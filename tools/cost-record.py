#!/usr/bin/env python3
"""Append one spend record per agent run, from a Claude Code hook. bean:0054.

Registered on `Stop` and `SubagentStop` in `.claude/settings.json`. Reads the hook payload on
stdin and appends to `domains/modus/cost/0001.ndjson` — the append-only log
`doc:60-cost-model#spend-record` specifies, fsynced per record because it is money
(`doc:40-durability#append-only-log`).

    tools/cost-record.py            read a hook payload on stdin, append a record
    tools/cost-record.py --self-test  replay the newest run through the same code path

**No hook event carries token usage or cost.** Verified against the 2.1.236 binary: the shared
payload builder emits `session_id, transcript_path, cwd, prompt_id, permission_mode, agent_id,
agent_type, effort` and the per-event assemblers add nothing numeric. So the recorder reads the
transcript the payload points at and does the arithmetic itself, with `tools/cost_lib.py` —
the same code the baseline replay uses, so the two cannot disagree.

`Stop` fires at the end of every turn, not once per session. Records are therefore DELTAS: each
one covers the messages after the previous record's `lastMessageId` for the same `runId`. The
log is its own cursor; there is no side state to lose.
"""

import json
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import cost_lib as C  # noqa: E402

REPO = os.environ.get("CLAUDE_PROJECT_DIR") or os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOMAIN_ID = "modus"
LOG = os.path.join(REPO, "domains", DOMAIN_ID, "cost", "0001.ndjson")
SOURCE = "claude-code-hook"

# doc:60-cost-model#spend-record fields this vantage point cannot supply, and why. They are
# OMITTED from the record rather than nulled or guessed: a null is indistinguishable from a
# measured zero, and doc:60 §3.2 forbids writing an estimate into a spend record at all.
UNAVAILABLE = {
    "stage": "no hook payload names a stage; it is a property of the work, not of the process",
    "workItemId": "the harness does not know which bean a run is for",
    "epicId": "follows workItemId",
    "rationale": "doc:60 requires it non-empty; a hook cannot know why a model was chosen",
    "priceBookEntryId": "domains/modus/cost/price-book.md does not exist yet (bean:0001)",
    "skillId": "no hook payload names the skill a run executed under",
}

# `outcome` is a PARTIAL mapping, not a measurement. Stop and SubagentStop mean the turn ended;
# neither says whether it ended well. `failed` and `retried` are unreachable from a hook and
# need the runner (bean:0020). Recorded with its derivation so a reader cannot mistake it for
# an observation.
OUTCOME_BY_EVENT = {"Stop": "succeeded", "SubagentStop": "succeeded"}


def git(args, cwd):
    try:
        return subprocess.check_output(["git"] + args, cwd=cwd, stderr=subprocess.DEVNULL).decode().strip()
    except Exception:
        return None


def resolve_parent(session_transcript, agent_transcript):
    """The run that spawned this one, via the subagent sidecar's `toolUseId`.

    The sidecar records the id of the `Task` tool_use block in the SPAWNING run. The hook's own
    `transcript_path` is always the session transcript even for a subagent, which covers depth
    1; a depth-2 subagent was spawned by another subagent, so the siblings are searched only if
    the session transcript does not own the id. A `fork` agent inherits its parent's context
    verbatim, so it owns a copy of its own spawning tool_use — the self-edge is discarded.
    """
    meta_path = agent_transcript[: -len(".jsonl")] + ".meta.json"
    if not os.path.exists(meta_path):
        return None, None
    with open(meta_path) as fh:
        meta = json.load(fh)
    tool_use_id = meta.get("toolUseId")
    if not tool_use_id:
        return None, meta
    me = os.path.basename(agent_transcript)[: -len(".jsonl")]
    candidates = [session_transcript]
    subdir = os.path.dirname(agent_transcript)
    if os.path.isdir(subdir):
        candidates += [os.path.join(subdir, f) for f in sorted(os.listdir(subdir))
                       if f.endswith(".jsonl") and os.path.join(subdir, f) != agent_transcript]
    for cand in candidates:
        if not cand or not os.path.exists(cand):
            continue
        owner = os.path.basename(cand)[: -len(".jsonl")]
        if owner == me:
            continue
        for m in C.read_messages(cand):
            if tool_use_id in m["toolUseIds"]:
                return owner, meta
    return None, meta


def last_recorded_message_id(run_id):
    if not os.path.exists(LOG):
        return None
    seen = None
    with open(LOG) as fh:
        for line in fh:
            line = line.strip()
            if not line:
                continue
            try:
                rec = json.loads(line)
            except ValueError:
                continue
            if rec.get("runId") == run_id and rec.get("lastMessageId"):
                seen = rec["lastMessageId"]
    return seen


def build_record(payload):
    event = payload.get("hook_event_name")
    cwd = payload.get("cwd") or REPO
    session_transcript = payload.get("transcript_path")
    agent_transcript = payload.get("agent_transcript_path")

    if event == "SubagentStop" and agent_transcript:
        run_id, transcript, role = payload.get("agent_id"), agent_transcript, payload.get("agent_type") or "subagent"
        parent_run_id, meta = resolve_parent(session_transcript, agent_transcript)
    else:
        run_id, transcript, role = payload.get("session_id"), session_transcript, "root"
        parent_run_id, meta = None, {}

    if not transcript or not os.path.exists(transcript):
        return None, "no transcript at %r" % (transcript,)

    messages = C.read_messages(transcript)
    cursor = last_recorded_message_id(run_id)
    if cursor is not None:
        ids = [m["messageId"] for m in messages]
        messages = messages[ids.index(cursor) + 1:] if cursor in ids else messages
    messages = [m for m in messages if m["model"] != C.SYNTHETIC_MODEL]
    if not messages:
        return None, "nothing new since %s" % (cursor,)

    usage = C.zero_usage()
    micros = 0
    models, efforts, speeds = [], [], []
    for m in messages:
        C.add_usage(usage, m["usage"])
        micros += C.cost_micros(m["model"], m["usage"])
        models.append(m["model"])
        if m["effort"]:
            efforts.append(m["effort"])
        speeds.append(m["speed"])
    times = [C.parse_ts(m["at"]) for m in messages if m["at"]]

    def dominant(xs):
        return max(set(xs), key=xs.count) if xs else None

    record = {
        "at": C.iso(C.parse_ts(messages[-1]["at"])) if times else None,
        "domainId": DOMAIN_ID,
        "runId": run_id,
        "parentRunId": parent_run_id,
        "role": role,
        "modelId": dominant(models),
        "modelIds": sorted(set(models)),
        "effort": dominant(efforts),
        "speed": dominant(speeds) or "standard",
        "channel": "interactive",
        "inputTokens": usage["inputTokens"],
        "outputTokens": usage["outputTokens"],
        "cacheReadTokens": usage["cacheReadTokens"],
        "cacheWriteTokens": usage["cacheWrite5mTokens"] + usage["cacheWrite1hTokens"],
        "cacheWrite5mTokens": usage["cacheWrite5mTokens"],
        "cacheWrite1hTokens": usage["cacheWrite1hTokens"],
        "costUsd": micros,  # integer micros, per doc:60 §3.2. Never a float.
        "costBasis": "derived",
        "peakContextTokens": C.peak_context_tokens(messages),
        "outcome": OUTCOME_BY_EVENT.get(event),
        "outcomeBasis": "hook event %s means the turn ended, not that it succeeded" % event,
        "startedAt": C.iso(min(times)) if times else None,
        "endedAt": C.iso(max(times)) if times else None,
        # The transcript's own `gitBranch` is the literal string "HEAD" on every line this
        # repository has produced. Resolved live from the hook's cwd instead, which is the one
        # moment the real branch is knowable.
        "gitBranch": git(["rev-parse", "--abbrev-ref", "HEAD"], cwd),
        "repoSha": git(["rev-parse", "HEAD"], cwd),
        "cwd": cwd,
        "messages": len(messages),
        "lastMessageId": messages[-1]["messageId"],
        "source": SOURCE,
        "sourceVersion": payload.get("_cliVersion"),
        "agentDescription": meta.get("description") if meta else None,
        "spawnDepth": meta.get("spawnDepth") if meta else 0,
        "unavailable": UNAVAILABLE,
    }
    return record, None


def append(record):
    d = os.path.dirname(LOG)
    if not os.path.isdir(d):
        os.makedirs(d)
    line = (json.dumps(record, sort_keys=True) + "\n").encode("utf-8")
    fd = os.open(LOG, os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o644)
    try:
        os.write(fd, line)
        os.fsync(fd)
    finally:
        os.close(fd)


def self_test():
    """Drive the same code path from a synthesised payload, so the hook is testable offline."""
    project_dir = C.project_dir_for(REPO)
    subs = []
    for name in sorted(os.listdir(project_dir)):
        d = os.path.join(project_dir, name, "subagents")
        if os.path.isdir(d):
            subs += [(os.path.join(project_dir, name + ".jsonl"), os.path.join(d, f))
                     for f in sorted(os.listdir(d)) if f.endswith(".jsonl")]
    if not subs:
        sys.stderr.write("no subagent transcripts to test against\n")
        return 2
    session, agent = max(subs, key=lambda p: os.path.getmtime(p[1]))
    payload = {
        "hook_event_name": "SubagentStop",
        "session_id": os.path.basename(session)[: -len(".jsonl")],
        "transcript_path": session,
        "agent_transcript_path": agent,
        "agent_id": os.path.basename(agent)[len("agent-"):-len(".jsonl")],
        "agent_type": "general-purpose",
        "cwd": REPO,
    }
    record, why = build_record(payload)
    if record is None:
        sys.stderr.write("self-test produced no record: %s\n" % why)
        return 1
    print(json.dumps(record, indent=2, sort_keys=True))
    missing = [k for k in ("runId", "parentRunId", "role", "modelId", "effort", "inputTokens",
                           "outputTokens", "cacheReadTokens", "cacheWriteTokens",
                           "peakContextTokens", "outcome", "startedAt", "endedAt", "gitBranch")
               if record.get(k) in (None, "")]
    if missing:
        sys.stderr.write("self-test: unpopulated required fields: %s\n" % ", ".join(missing))
        return 1
    print("\nself-test OK — every required field populated", file=sys.stderr)
    return 0


def main(argv):
    if "--self-test" in argv:
        return self_test()
    raw = sys.stdin.read()
    if not raw.strip():
        return 0
    try:
        payload = json.loads(raw)
    except ValueError:
        return 0
    # A hook must never break the session it observes. Any failure here is silent to the agent
    # and visible only in the log's own error line.
    try:
        record, why = build_record(payload)
        if record is None:
            return 0
        append(record)
    except Exception as exc:  # noqa: BLE001 - a recorder that kills the run is worse than a gap
        try:
            append({"at": None, "domainId": DOMAIN_ID, "source": SOURCE, "error": repr(exc),
                    "hookEvent": payload.get("hook_event_name")})
        except Exception:
            pass
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
