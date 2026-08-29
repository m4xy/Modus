#!/usr/bin/env python3
"""Replay this repository's Claude Code transcripts into a cost baseline. bean:0054.

    tools/cost-replay.py                 regenerate domains/modus/cost/replay/* and the bean's
                                         generated figure block
    tools/cost-replay.py --check         fail if the committed output no longer matches
    tools/cost-replay.py --refresh-prs   re-fetch the pull-request snapshot from GitHub
    tools/cost-replay.py --transcripts D read transcripts from D instead of deriving the path
                                         from this checkout (also MODUS_COST_TRANSCRIPTS)

The output is committed because a baseline that must be regenerated to be read is not a
baseline: the transcripts live outside the repository, in ~/.claude, on one machine, and are
pruned. --check is what keeps the committed copy honest while those inputs still exist.

Every input file's sha256 goes into the run's `command` evidence record
(doc:50-memory-and-evidence#evidence-record), so the measurement is repeatable rather than
merely re-runnable.
"""

import json
import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import cost_lib as C  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(REPO, "domains", "modus", "cost", "replay")
# The pull-request metadata is a COMMITTED SNAPSHOT, not a live query. It used to be fetched
# at generation time and baked into both artifacts while not being one of the hashed inputs,
# so `--check` turned red the moment an open pull request merged — reporting a determinism bug
# in this file when nothing about this file had changed. Network state cannot be an unhashed
# input to a repeatability guarantee. `--refresh-prs` updates the snapshot deliberately.
PR_SNAPSHOT = os.path.join(OUT_DIR, "pull-requests.json")
BEAN = os.path.join(REPO, ".beans", "modus-0054--cost-baseline-and-run-recorder.md")
BEGIN = "<!-- cost-replay:begin -->"
END = "<!-- cost-replay:end -->"
PULL_RE = re.compile(r"/pull/(\d+)")


# ------------------------------------------------------------------ discovery ---
def discover(project_dir):
    """Every transcript belonging to this repository, root sessions and subagents alike.

    The subagent transcripts are the whole point. They live in `<sessionId>/subagents/` and
    are NOT matched by the `*.jsonl` glob at the top of the project directory, which is why
    delegated spend looks unattributable when you only read the parent transcript.
    """
    roots, subs = [], []
    if not os.path.isdir(project_dir):
        return roots, subs
    for name in sorted(os.listdir(project_dir)):
        if name.endswith(".jsonl"):
            roots.append(os.path.join(project_dir, name))
    for name in sorted(os.listdir(project_dir)):
        d = os.path.join(project_dir, name, "subagents")
        if not os.path.isdir(d):
            continue
        for f in sorted(os.listdir(d)):
            if f.endswith(".jsonl"):
                subs.append(os.path.join(d, f))
    return roots, subs


def load_meta(sub_path):
    meta_path = sub_path[: -len(".jsonl")] + ".meta.json"
    if not os.path.exists(meta_path):
        return {}, None
    with open(meta_path) as fh:
        return json.load(fh), meta_path


# ---------------------------------------------------------------------- runs ---
def build_runs(roots, subs):
    runs = {}
    owner_of_tool_use = {}

    def ingest(path, run_id, role, parent_hint, meta):
        messages = C.read_messages(path)
        if not messages:
            return
        usage = C.zero_usage()
        micros = 0
        models, efforts, branches = set(), set(), set()
        synthetic = 0
        for m in messages:
            if m["model"] == C.SYNTHETIC_MODEL:
                synthetic += 1
            else:
                C.add_usage(usage, m["usage"])
                micros += C.cost_micros(m["model"], m["usage"])
                models.add(m["model"])
                if m["effort"]:
                    efforts.add(m["effort"])
            branches.add(m["gitBranch"])
            for tid in m["toolUseIds"]:
                owner_of_tool_use.setdefault(tid, []).append(run_id)
        times = [C.parse_ts(m["at"]) for m in messages if m["at"]]
        runs[run_id] = {
            "runId": run_id,
            "parentRunId": parent_hint,
            "role": role,
            "transcript": path,
            "messages": len(messages),
            "frames": sum(m["frames"] for m in messages),
            "frameDisagreements": C.frame_disagreements(messages),
            "staleOutputTokensAvoided": C.stale_output_undercount(messages),
            "syntheticMessages": synthetic,
            "models": sorted(models),
            "efforts": sorted(efforts),
            "gitBranches": sorted(b for b in branches if b is not None),
            "usage": usage,
            "costMicros": micros,
            "peakContextTokens": C.peak_context_tokens(messages),
            "startedAt": C.iso(min(times)) if times else None,
            "endedAt": C.iso(max(times)) if times else None,
            "wallClockSeconds": int((max(times) - min(times)).total_seconds()) if times else 0,
            "spawnDepth": meta.get("spawnDepth", 0),
            "description": meta.get("description"),
            "worktreeBranch": meta.get("worktreeBranch"),
            "_toolUseId": meta.get("toolUseId"),
            "prsCreated": prs_created(path),
            "prTimeline": pr_timeline(path),
        }

    for p in roots:
        ingest(p, os.path.basename(p)[: -len(".jsonl")], "root", None, {})
    for p in subs:
        meta, _ = load_meta(p)
        ingest(p, os.path.basename(p)[: -len(".jsonl")], meta.get("agentType") or "subagent", None, meta)

    # parentRunId: the meta sidecar's `toolUseId` is the id of the Task tool_use block in the
    # SPAWNING run's transcript. Resolving it gives a real parent edge, at every depth. A
    # `fork` agent inherits its parent's context verbatim, so the same tool_use id appears in
    # the child's own transcript too; the self-edge is discarded rather than followed.
    unresolved = []
    for run in runs.values():
        tid = run.pop("_toolUseId")
        if tid is None:
            continue
        owners = [o for o in owner_of_tool_use.get(tid, []) if o != run["runId"]]
        if owners:
            run["parentRunId"] = owners[0]
        else:
            unresolved.append(run["runId"])
    return runs, unresolved


def prs_created(path):
    """PR numbers this run opened, read from the tool_result of its own `gh pr create`.

    This is an exact run-to-pull-request edge and it does not depend on `gitBranch`, which
    this corpus does not carry (see the baseline document).
    """
    creates, out = {}, []
    lines = []
    for line in open(path):
        line = line.strip()
        if line:
            lines.append(json.loads(line))
    for d in lines:
        if d.get("type") != "assistant":
            continue
        content = (d.get("message") or {}).get("content")
        if not isinstance(content, list):
            continue
        for b in content:
            if isinstance(b, dict) and b.get("type") == "tool_use" and b.get("name") == "Bash":
                if "pr create" in (b.get("input") or {}).get("command", ""):
                    creates[b["id"]] = C.parse_ts(d.get("timestamp"))
    for d in lines:
        if d.get("type") != "user":
            continue
        content = (d.get("message") or {}).get("content")
        if not isinstance(content, list):
            continue
        for b in content:
            if isinstance(b, dict) and b.get("type") == "tool_result" and b.get("tool_use_id") in creates:
                for n in PULL_RE.findall(json.dumps(b.get("content"))):
                    out.append({"pr": int(n), "at": C.iso(creates[b["tool_use_id"]])})
    return out


def pr_timeline(path):
    """`pr-link` records: Claude Code's own note of a pull request it touched, with a time."""
    out = []
    for line in open(path):
        line = line.strip()
        if not line:
            continue
        d = json.loads(line)
        if d.get("type") == "pr-link" and d.get("prNumber"):
            out.append({"pr": int(d["prNumber"]), "at": d.get("timestamp")})
    return out


# --------------------------------------------------------------- attribution ---
def attribute(runs):
    """Attribute every run's spend to a pull request, exactly where possible.

    EXACT — a run that ran `gh pr create` and got a `/pull/N` back owns PR N, and so do all
    of its descendants. No time window, no branch name, no guess.

    SEGMENTED — a run that opened several pull requests (an orchestrator does) is split on
    its own creation timestamps: a message belongs to the first pull request created at or
    after it, because you do the work and then open the pull request. This IS a heuristic and
    the report says how much of the total rides on it.

    UNATTRIBUTED — spend after the last creation in a run. Reported, never folded into a
    total, for the same reason doc:60-cost-model §3.1 makes `overhead` a first-class stage.
    """
    root_of, parent = {}, {}
    for rid, r in runs.items():
        parent[rid] = r["parentRunId"]
    for rid in runs:
        seen, cur = set(), rid
        while parent.get(cur) and cur not in seen:
            seen.add(cur)
            cur = parent[cur]
        root_of[rid] = cur

    exact, segmented, inherited, unattributed = {}, {}, {}, 0
    for rid, r in runs.items():
        creations = sorted(r["prsCreated"], key=lambda x: x["at"] or "")
        if len(creations) > 1:
            segmented[rid] = creations
            continue
        if len(creations) == 1:
            exact.setdefault(creations[0]["pr"], []).append(rid)
            continue
        # No pull request of its own. Walk up to the nearest ancestor that opened one.
        cur, hop, placed = r["parentRunId"], 0, False
        while cur in runs and hop < 16:
            pc = sorted(runs[cur]["prsCreated"], key=lambda x: x["at"] or "")
            if len(pc) == 1:
                exact.setdefault(pc[0]["pr"], []).append(rid)
                placed = True
                break
            if pc:
                # The ancestor opened several. Place this run by the same timestamp rule the
                # ancestor's own spend is segmented by: the first pull request opened at or
                # after the run started. Heuristic, and counted as such.
                pr = next((b["pr"] for b in pc if r["startedAt"] and r["startedAt"] <= b["at"]), None)
                if pr is not None:
                    inherited.setdefault(pr, []).append(rid)
                    placed = True
                break
            cur, hop = runs[cur]["parentRunId"], hop + 1
        if not placed:
            unattributed += r["costMicros"]
    return root_of, exact, segmented, inherited, unattributed


def segment_run(path, creations):
    """Split one run's per-message spend across the pull requests it opened."""
    bounds = sorted(creations, key=lambda x: x["at"])
    per_pr, tail = {}, {"usage": C.zero_usage(), "costMicros": 0, "messages": 0}
    for m in C.read_messages(path):
        if m["model"] == C.SYNTHETIC_MODEL:
            continue
        at = m["at"]
        target = None
        for b in bounds:
            if at is not None and at <= b["at"]:
                target = b["pr"]
                break
        bucket = per_pr.setdefault(target, {"usage": C.zero_usage(), "costMicros": 0, "messages": 0}) if target else tail
        C.add_usage(bucket["usage"], m["usage"])
        bucket["costMicros"] += C.cost_micros(m["model"], m["usage"])
        bucket["messages"] += 1
    return per_pr, tail


# -------------------------------------------------------------------- output ---
def refresh_pr_snapshot():
    """Re-fetch pull-request metadata and rewrite the committed snapshot. Deliberate only."""
    raw = subprocess.check_output(
        ["gh", "pr", "list", "--state", "merged", "--limit", "200",
         "--json", "number,title,headRefName,mergedAt,additions,deletions"],
        cwd=REPO, stderr=subprocess.DEVNULL, env=dict(os.environ, GITHUB_TOKEN=""),
    )
    prs = sorted(json.loads(raw), key=lambda p: p["number"])
    if not os.path.isdir(OUT_DIR):
        os.makedirs(OUT_DIR)
    with open(PR_SNAPSHOT, "w") as fh:
        fh.write(json.dumps(prs, indent=1, sort_keys=True) + "\n")
    print("refreshed %s — %d merged pull requests" % (os.path.relpath(PR_SNAPSHOT, REPO), len(prs)))


def merged_prs():
    if not os.path.exists(PR_SNAPSHOT):
        return {}
    with open(PR_SNAPSHOT) as fh:
        return dict((p["number"], p) for p in json.load(fh))


def ratio(part, whole):
    return 0.0 if not whole else round(100.0 * part / whole, 2)


def main(argv):
    check = "--check" in argv
    if "--refresh-prs" in argv:
        refresh_pr_snapshot()
        if len(argv) == 1:
            return 0
    override = None
    if "--transcripts" in argv:
        override = argv[argv.index("--transcripts") + 1]
    project_dir = C.project_dir_for(REPO, override=override)
    roots, subs = discover(project_dir)
    if not roots:
        sys.stderr.write(
            "no transcripts under %s\n"
            "Pass --transcripts DIR or set %s to point at the Claude Code project directory\n"
            "holding this repository's sessions.\n" % (project_dir, C.TRANSCRIPTS_ENV))
        return 2

    def rel(path):
        """Input paths are relative to $HOME when they are under it, absolute otherwise."""
        home = os.path.expanduser("~")
        return os.path.relpath(path, home) if os.path.abspath(path).startswith(home + os.sep) else os.path.abspath(path)

    inputs = []
    for p in roots + subs:
        inputs.append({"path": rel(p), "sha256": C.sha256_file(p), "bytes": os.path.getsize(p)})
        meta, mp = load_meta(p)
        if mp:
            inputs.append({"path": rel(mp), "sha256": C.sha256_file(mp), "bytes": os.path.getsize(mp)})
    # The pull-request snapshot is an input like any other, and is hashed like any other.
    if os.path.exists(PR_SNAPSHOT):
        inputs.append({"path": os.path.relpath(PR_SNAPSHOT, REPO), "sha256": C.sha256_file(PR_SNAPSHOT),
                       "bytes": os.path.getsize(PR_SNAPSHOT), "inRepo": True})

    runs, unresolved_parents = build_runs(roots, subs)
    root_of, exact, segmented, inherited, unattributed_micros = attribute(runs)

    total = C.zero_usage()
    total_micros = 0
    delegated = C.zero_usage()
    delegated_micros = 0
    frame_disagreements = 0
    stale_output_avoided = 0
    for rid, r in runs.items():
        C.add_usage(total, r["usage"])
        total_micros += r["costMicros"]
        frame_disagreements += r["frameDisagreements"]
        stale_output_avoided += r["staleOutputTokensAvoided"]
        if r["role"] != "root":
            C.add_usage(delegated, r["usage"])
            delegated_micros += r["costMicros"]

    per_pr = {}

    def bump(pr, usage, micros, kind, nruns=0):
        b = per_pr.setdefault(pr, {"usage": C.zero_usage(), "costMicros": 0, "runs": 0, "exactMicros": 0, "segmentedMicros": 0})
        C.add_usage(b["usage"], usage)
        b["costMicros"] += micros
        b[kind] += micros
        b["runs"] += nruns

    for pr, rids in exact.items():
        for rid in rids:
            bump(pr, runs[rid]["usage"], runs[rid]["costMicros"], "exactMicros", 1)
    segmented_micros = 0
    for pr, rids in inherited.items():
        for rid in rids:
            bump(pr, runs[rid]["usage"], runs[rid]["costMicros"], "segmentedMicros", 1)
            segmented_micros += runs[rid]["costMicros"]
    tail_micros = 0
    for rid, creations in segmented.items():
        buckets, tail = segment_run(runs[rid]["transcript"], creations)
        for pr, b in buckets.items():
            bump(pr, b["usage"], b["costMicros"], "segmentedMicros", 1)
            segmented_micros += b["costMicros"]
        tail_micros += tail["costMicros"]

    merged = merged_prs()
    tokens_total = sum(total[k] for k in C.USAGE_KINDS)
    cache_read = total["cacheReadTokens"]

    summary = {
        "generatedBy": "tools/cost-replay.py",
        "bean": "bean:0054",
        "repoSha": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=REPO).decode().strip(),
        "projectDir": os.path.relpath(project_dir, os.path.expanduser("~")),
        "rootSessions": len(roots),
        "subagentRuns": len(subs),
        "runs": len(runs),
        "messagesTotal": sum(r["messages"] for r in runs.values()),
        "framesTotal": sum(r["frames"] for r in runs.values()),
        "frameMultiplicity": round(
            sum(r["frames"] for r in runs.values()) / float(sum(r["messages"] for r in runs.values())), 3),
        "unresolvedParentEdges": unresolved_parents,
        "frameDisagreements": frame_disagreements,
        "staleOutputTokensAvoided": stale_output_avoided,
        "usage": total,
        "tokensTotal": tokens_total,
        "cacheReadRatioPct": ratio(cache_read, tokens_total),
        "costUsd": total_micros,
        "costUsdDisplay": C.usd(total_micros),
        "delegated": {
            "runs": len(subs),
            "usage": delegated,
            "costMicros": delegated_micros,
            "shareOfCostPct": ratio(delegated_micros, total_micros),
        },
        "attribution": {
            "exactMicros": sum(b["exactMicros"] for b in per_pr.values()),
            "segmentedMicros": segmented_micros,
            "unattributedMicros": unattributed_micros + tail_micros,
        },
        "gitBranchHeadMessages": sum(r["messages"] for r in runs.values() if r["gitBranches"] == ["HEAD"]),
        "gitBranchTotalMessages": sum(r["messages"] for r in runs.values()),
        "peakContextTokensMax": max(r["peakContextTokens"] for r in runs.values()),
        "rates": dict((m, C.rates_upm(m)) for m in sorted(set(m for r in runs.values() for m in r["models"]))),
        "inputs": inputs,
    }

    lines = [json.dumps({"type": "summary", **summary}, sort_keys=True)]
    for rid in sorted(runs, key=lambda r: (runs[r]["startedAt"] or "", r)):
        r = dict(runs[rid])
        r["transcript"] = rel(r["transcript"])
        r["rootRunId"] = root_of[rid]
        r["costUsd"] = r["costMicros"]
        r["costUsdDisplay"] = C.usd(r["costMicros"])
        lines.append(json.dumps({"type": "run", **r}, sort_keys=True))
    for pr in sorted(per_pr):
        b = per_pr[pr]
        m = merged.get(pr) or {}
        lines.append(json.dumps({
            "type": "pull-request", "pr": pr, "runs": b["runs"],
            "usage": b["usage"], "costMicros": b["costMicros"], "costUsd": b["costMicros"],
            "costUsdDisplay": C.usd(b["costMicros"]),
            "exactMicros": b["exactMicros"], "segmentedMicros": b["segmentedMicros"],
            "merged": bool(m), "title": m.get("title"), "headRefName": m.get("headRefName"),
            "mergedAt": m.get("mergedAt"), "additions": m.get("additions"), "deletions": m.get("deletions"),
        }, sort_keys=True))
    ndjson = "\n".join(lines) + "\n"

    doc = render(summary, runs, per_pr, merged, root_of)

    targets = [
        (os.path.join(OUT_DIR, "runs.ndjson"), ndjson),
        (os.path.join(OUT_DIR, "baseline.md"), doc),
        (BEAN, splice_bean(summary, per_pr)),
    ]
    if check:
        return run_check(targets)
    if not os.path.isdir(OUT_DIR):
        os.makedirs(OUT_DIR)
    for path, want in targets:
        with open(path, "w") as fh:
            fh.write(want)
        print("wrote %s" % os.path.relpath(path, REPO))
    return 0


def splice_bean(s, per_pr):
    """Rewrite the bean's generated figure block from the artifact, in place.

    Item 1 of the review, and its lesson. Six figures were hand-copied out of this artifact
    into the bean, a pull-request body and a normative document, and every one of them was
    stale within a day because the corpus is live and appending. Prose that quotes a total
    over a growing corpus rots; the only fix that holds is to stop hand-writing it. Everything
    volatile now lives between the markers and is generated here; `--check` fails if the block
    drifts from the artifact, exactly as it does for the artifact itself.
    """
    with open(BEAN) as fh:
        text = fh.read()
    u = s["usage"]
    costs = sorted(b["costMicros"] for b in per_pr.values())
    mid = costs[len(costs) // 2] if costs else 0
    body = [
        BEGIN,
        "<!-- generated by tools/cost-replay.py from runs.ndjson; do not hand-edit -->",
        "",
        "| | |",
        "|---|---:|",
        "| runs replayed | %d (%d root session(s), %d subagent run(s)) |" % (s["runs"], s["rootSessions"], s["subagentRuns"]),
        "| assistant messages / transcript frames | %s / %s (**%sx** overcount if frames are summed) |"
        % ("{:,}".format(s["messagesTotal"]), "{:,}".format(s["framesTotal"]), s["frameMultiplicity"]),
        "| tokens | %s |" % "{:,}".format(s["tokensTotal"]),
        "| cache-read ratio | **%s%%** |" % s["cacheReadRatioPct"],
        "| fresh input + output | %s%% of all tokens |" % ratio(u["inputTokens"] + u["outputTokens"], s["tokensTotal"]),
        "| derived cost | **%s** |" % s["costUsdDisplay"],
        "| delegated share of cost | **%s%%** — included, not excluded |" % s["delegated"]["shareOfCostPct"],
        "| largest peak context | %s tokens, %sx the 300k ceiling (`doc:00-constitution#context-budget`) |"
        % ("{:,}".format(s["peakContextTokensMax"]), round(s["peakContextTokensMax"] / 300000.0, 1)),
        "| pull requests attributed | %d — min %s, median %s, max %s |"
        % (len(per_pr), C.usd(costs[0]) if costs else "-", C.usd(mid), C.usd(costs[-1]) if costs else "-"),
        "| attributed exactly / by timestamp / not at all | %s%% / %s%% / %s%% of dollars |"
        % (ratio(s["attribution"]["exactMicros"], s["costUsd"]),
           ratio(s["attribution"]["segmentedMicros"], s["costUsd"]),
           ratio(s["attribution"]["unattributedMicros"], s["costUsd"])),
        "| `gitBranch` == the literal `HEAD` | %s of %s messages |"
        % ("{:,}".format(s["gitBranchHeadMessages"]), "{:,}".format(s["gitBranchTotalMessages"])),
        "| output tokens recovered by taking the largest frame, not the first | %s (%s%% of all output) |"
        % ("{:,}".format(s["staleOutputTokensAvoided"]), ratio(s["staleOutputTokensAvoided"], u["outputTokens"])),
        "| frames disagreeing on input or cache tokens | %d |" % s["frameDisagreements"],
        "| subagent parent edges unresolved | %d of %d |" % (len(s["unresolvedParentEdges"]), s["subagentRuns"]),
        "",
        "Verbatim, for criteria 2 and 3 (`doc:00-constitution#evidence-rule`):",
        "",
        "```",
        "cmd:      python3 tools/cost-replay.py",
        "observed: Delegated spend is INCLUDED: %s%% of the dollar total is subagent runs" % s["delegated"]["shareOfCostPct"],
        "            (%d of %d runs)" % (s["delegated"]["runs"], s["runs"]),
        "          repeated frames of one `message.id` agree on input and cache tokens",
        "            | %d disagreement(s)" % s["frameDisagreements"],
        "          output tokens recovered by taking the largest frame, not the first",
        "            | %s (%s%% of all output)" % ("{:,}".format(s["staleOutputTokensAvoided"]),
                                                   ratio(s["staleOutputTokensAvoided"], u["outputTokens"])),
        "exit:     0",
        "```",
        "",
        END,
    ]
    start, stop = text.find(BEGIN), text.find(END)
    if start < 0 or stop < 0:
        raise SystemExit("%s has no cost-replay block; add the BEGIN/END markers" % BEAN)
    return text[:start] + "\n".join(body) + text[stop + len(END):]


def run_check(targets):
    """Verify the committed baseline against the inputs it records, not against today's.

    Repeatability is the property being checked (doc:50-memory-and-evidence#evidence-record),
    and it is a property of a fixed input set. Comparing against whatever is in ~/.claude now
    would fail every time a session appends a line, which says nothing about whether the
    committed numbers were right. So: hash the recorded inputs first. Drift there is reported
    as drift, not as a wrong baseline.
    """
    ndjson_path = targets[0][0]
    if not os.path.exists(ndjson_path):
        sys.stderr.write("MISSING %s\n" % os.path.relpath(ndjson_path, REPO))
        return 1
    with open(ndjson_path) as fh:
        summary = json.loads(fh.readline())
    home = os.path.expanduser("~")
    drift, missing = [], []
    for i in summary["inputs"]:
        # An input is either in this repository (the pull-request snapshot) or under $HOME
        # (a transcript); an absolute path passes through os.path.join unchanged.
        p = os.path.join(REPO if i.get("inRepo") else home, i["path"])
        if not os.path.exists(p):
            missing.append(i["path"])
        elif C.sha256_file(p) != i["sha256"]:
            drift.append(i["path"])
    if missing or drift:
        sys.stderr.write("baseline inputs have moved on: %d changed, %d gone. The committed\n"
                         "figures describe the %d files hashed at generation and are not\n"
                         "reproducible from today's transcripts. Regenerate to re-baseline.\n"
                         % (len(drift), len(missing), len(summary["inputs"])))
        return 1
    bad = 0
    for path, want in targets:
        have = open(path).read() if os.path.exists(path) else None
        if have != want:
            sys.stderr.write(
                "STALE %s — every hashed input is byte-identical, so the difference came from\n"
                "this repository, not from the transcripts: an edit inside the generated block,\n"
                "or a change to tools/cost-replay.py or tools/cost_lib.py that moves a number.\n"
                "Re-run `python3 tools/cost-replay.py` and review the diff before committing.\n"
                % os.path.relpath(path, REPO))
            bad = 1
    return bad


def render(s, runs, per_pr, merged, root_of):
    u = s["usage"]
    out = []
    w = out.append
    w("# Cost baseline — the `modus` domain's own SDLC")
    w("")
    w("Generated by `tools/cost-replay.py` (`bean:0054`); do not hand-edit. `--check` fails if")
    w("this file no longer matches a replay of the same transcripts.")
    w("")
    w("| | |")
    w("|---|---|")
    w("| repo sha at generation | `%s` |" % s["repoSha"])
    w("| transcripts | %d root session(s), %d subagent run(s) |" % (s["rootSessions"], s["subagentRuns"]))
    w("| input files hashed | %d |" % len(s["inputs"]))
    w("")
    w("## Headline")
    w("")
    w("| | tokens | share |")
    w("|---|---:|---:|")
    for label, key in (("input (fresh)", "inputTokens"), ("output", "outputTokens"),
                       ("cache read", "cacheReadTokens"), ("cache write 5m", "cacheWrite5mTokens"),
                       ("cache write 1h", "cacheWrite1hTokens")):
        w("| %s | %s | %s%% |" % (label, "{:,}".format(u[key]), ratio(u[key], s["tokensTotal"])))
    w("| **total** | **%s** | |" % "{:,}".format(s["tokensTotal"]))
    w("")
    w("- **Cache-read ratio: %s%%** of all tokens.  " % s["cacheReadRatioPct"])
    w("- **Cost: %s** (derived — see *Prices* below; no billed figure exists in the transcript).  " % s["costUsdDisplay"])
    w("- **Delegated spend is INCLUDED**: %s%% of the dollar total is subagent runs (%d of %d runs)."
      % (s["delegated"]["shareOfCostPct"], s["delegated"]["runs"], s["runs"]))
    w("")
    w("Fresh input plus output is %s%% of all tokens. A baseline reporting only those two would"
      % ratio(u["inputTokens"] + u["outputTokens"], s["tokensTotal"]))
    w("describe almost none of the spend; the cache classes are the bill.")
    w("")
    w("## What these transcripts cannot tell you")
    w("")
    w("| asked for | obtainable | why |")
    w("|---|---|---|")
    w("| per-branch totals | **no** | `gitBranch` is the literal string `HEAD` on %d of %d assistant messages in this corpus. Not a detached checkout — the repository was on named branches throughout. The field is unusable as a join key, and the pull-request join above uses `gh pr create` results instead. |"
      % (s["gitBranchHeadMessages"], s["gitBranchTotalMessages"]))
    w("| `durationMs` per message | **no** | the field does not exist on an assistant line. Wall clock above is `max(timestamp) - min(timestamp)` per run, so it includes think time, tool time and idle time, and for a root session it includes the human being away. |")
    w("| a billed dollar figure | **no** | the stored transcript carries no cost field. Every dollar here is derived from token counts and the rate table below. |")
    w("| delegated spend | **yes** | in `<sessionId>/subagents/agent-*.jsonl`, one transcript per subagent, with an `agent-*.meta.json` sidecar. It is absent from the parent transcript, so reading only `*.jsonl` at the top of the project directory hides %s%% of the spend. |"
      % s["delegated"]["shareOfCostPct"])
    w("| `parentRunId` | **yes** | the sidecar's `toolUseId` is the id of the `Task` tool_use block in the spawning run. All %d subagent edges resolved, at depth 1 and depth 2. There is no `parent_tool_use_id` field on a stored assistant line. |"
      % s["subagentRuns"])
    w("| `peakContextTokens` | **yes** | `input + cache_read + cache_creation` on the largest single request of the run. The largest here is **%s tokens**, %sx `doc:00-constitution` §6's 300k ceiling. |"
      % ("{:,}".format(s["peakContextTokensMax"]), round(s["peakContextTokensMax"] / 300000.0, 1)))
    w("")
    w("## Per pull request")
    w("")
    w("`exact` is a run that opened the pull request itself, plus its descendants — no heuristic.")
    w("`seg` is an orchestrator's own spend, split on its pull-request-creation timestamps.")
    w("")
    w("| PR | merged | title | runs | tokens | $ | exact $ | seg $ |")
    w("|---:|---|---|---:|---:|---:|---:|---:|")
    for pr in sorted(per_pr):
        b = per_pr[pr]
        m = merged.get(pr) or {}
        toks = sum(b["usage"][k] for k in C.USAGE_KINDS)
        w("| %d | %s | %s | %d | %s | %s | %s | %s |" % (
            pr, "yes" if m else "no", (m.get("title") or "—")[:52], b["runs"],
            "{:,}".format(toks), C.usd(b["costMicros"]), C.usd(b["exactMicros"]), C.usd(b["segmentedMicros"])))
    a = s["attribution"]
    w("")
    w("| attribution | micro-dollars | share |")
    w("|---|---:|---:|")
    for label, key in (("exact (run opened the PR)", "exactMicros"), ("segmented (orchestrator, by timestamp)", "segmentedMicros"), ("unattributed", "unattributedMicros")):
        w("| %s | %s | %s%% |" % (label, "{:,}".format(a[key]), ratio(a[key], s["costUsd"])))
    w("")
    w("## Per run")
    w("")
    w("| run | role | parent | msgs | tokens | $ | peak ctx | wall |")
    w("|---|---|---|---:|---:|---:|---:|---:|")
    for rid in sorted(runs, key=lambda r: -runs[r]["costMicros"]):
        r = runs[rid]
        toks = sum(r["usage"][k] for k in C.USAGE_KINDS)
        w("| `%s` | %s | %s | %d | %s | %s | %s | %ds |" % (
            rid[:17], r["role"], ("`%s`" % r["parentRunId"][:17]) if r["parentRunId"] else "—",
            r["messages"], "{:,}".format(toks), C.usd(r["costMicros"]),
            "{:,}".format(r["peakContextTokens"]), r["wallClockSeconds"]))
    w("")
    w("## Prices used (derived, not billed)")
    w("")
    w("Micro-dollars per million tokens. Base rates: `doc:60-cost-model#price-book`. Cache")
    w("multipliers (read 0.1x, write 1.25x at 5m TTL, 2x at 1h TTL) are from the `claude-api`")
    w("skill's `shared/prompt-caching.md`, which `doc:60#price-book` names as their source.")
    w("")
    w("| model | input | output | cache read | cache write 5m | cache write 1h |")
    w("|---|---:|---:|---:|---:|---:|")
    for m, r in sorted(s["rates"].items()):
        w("| `%s` | %s | %s | %s | %s | %s |" % (m, "{:,}".format(r["input"]), "{:,}".format(r["output"]),
                                                 "{:,}".format(r["cacheRead"]), "{:,}".format(r["cacheWrite5m"]),
                                                 "{:,}".format(r["cacheWrite1h"])))
    w("")
    w("## Self-checks on this replay")
    w("")
    w("| assertion | result |")
    w("|---|---|")
    w("| repeated frames of one `message.id` agree on input and cache tokens | %d disagreement(s) |" % s["frameDisagreements"])
    w("| output tokens recovered by taking the largest frame, not the first | %s (%s%% of all output) |"
      % ("{:,}".format(s["staleOutputTokensAvoided"]), ratio(s["staleOutputTokensAvoided"], s["usage"]["outputTokens"])))
    w("| every subagent's parent edge resolved | %d unresolved |" % len(s["unresolvedParentEdges"]))
    w("")
    w("## Input hashes")
    w("")
    w("Paths are relative to `$HOME`. `doc:50-memory-and-evidence#evidence-record`: a `command`")
    w("record is repeatable by running it again, which requires knowing what it read.")
    w("")
    w("```")
    for i in s["inputs"]:
        w("%s  %s" % (i["sha256"], i["path"]))
    w("```")
    return "\n".join(out) + "\n"


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
