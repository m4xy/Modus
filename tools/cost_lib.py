"""Shared cost arithmetic over Claude Code transcripts. Tier 3 (adr:0006#classification).

Python, not bash and not jq, for one reason that is not taste: `doc:60-cost-model#spend-record`
requires `costUsd` as integer micros and `doc:20-ddd-practices` §3 forbids floating-point money.
jq has exactly one number type and it is an IEEE double, so jq cannot represent the required
type at all. bash has integers but no JSON reader, and the input is 61 nested-JSON files.
Python's int is arbitrary precision, and the stdlib reads JSON. No third-party package is used.

Enforcement gap: nothing in `./gradlew qualityCheck` lints or tests this file. ktlint, Detekt
and ESLint cover Kotlin and TypeScript; there is no Python gate and wiring one means editing
build.gradle.kts, which bean:0054 does not own. Recorded, not glossed.
"""

import hashlib
import json
import os
import re
from datetime import datetime

# --------------------------------------------------------------------- prices ---
# Rates are MICRO-DOLLARS PER MILLION TOKENS, so every figure below is an exact integer
# and `tokens * rate // 1_000_000` is exact integer arithmetic. $5.00/1M == 5_000_000.
#
# Base input/output: doc:60-cost-model#price-book, itself sourced from the `claude-api`
# skill (cached 2026-06-24).
#
# Cache multipliers: doc:60#price-book deliberately does NOT carry them ("they must not be
# written from memory"). Taken from the `claude-api` skill,
#   shared/prompt-caching.md:141 — "Cache reads cost ~0.1x base input price. Cache writes
#   cost 1.25x for 5-minute TTL, 2x for 1-hour TTL."
# read at CLI version 2.1.236 on 2026-08-29. This is a DERIVED cost, not a billed one; no
# authoritative per-run cost is emitted anywhere in the stored transcript (see
# `docs/`-free note in the baseline document).
CACHE_READ_MULT = (1, 10)  # 0.1x
CACHE_WRITE_5M_MULT = (5, 4)  # 1.25x
CACHE_WRITE_1H_MULT = (2, 1)  # 2x

BASE_RATES_UPM = {
    # modelId: (input, output) micro-dollars per million tokens
    "claude-opus-5": (5_000_000, 25_000_000),
    "claude-opus-4-8": (5_000_000, 25_000_000),
    "claude-opus-4-7": (5_000_000, 25_000_000),
    "claude-opus-4-6": (5_000_000, 25_000_000),
    "claude-fable-5": (10_000_000, 50_000_000),
    "claude-sonnet-4-6": (3_000_000, 15_000_000),
    "claude-haiku-4-5": (1_000_000, 5_000_000),
    # Sonnet 5's introductory rate lapses after 2026-08-31 (doc:60#price-book). Every run in
    # this corpus predates the lapse; a replay of a later run would need the standard rate
    # selected by date, which is why the price book proper carries effectiveFrom/effectiveTo
    # and this table does not pretend to be one.
    "claude-sonnet-5": (2_000_000, 10_000_000),
}

# A synthetic message is Claude Code's own placeholder (an interrupt, an API error). It has
# no usage block and no billed spend. Counted, never priced.
SYNTHETIC_MODEL = "<synthetic>"

# doc:60#price-book: "Model IDs are exact strings and are never constructed." The transcript
# may carry either the canonical id or a dated snapshot of it; normalisation strips exactly
# one trailing -YYYYMMDD and nothing else, and an id that still does not resolve raises.
_DATED = re.compile(r"^(.*)-\d{8}$")


def normalise_model(model_id):
    if model_id in BASE_RATES_UPM or model_id == SYNTHETIC_MODEL:
        return model_id
    m = _DATED.match(model_id or "")
    if m and m.group(1) in BASE_RATES_UPM:
        return m.group(1)
    raise KeyError("unpriced model id %r — add it to BASE_RATES_UPM from the price book" % (model_id,))


def rates_upm(model_id):
    """Return the five per-million rates in micro-dollars for one model."""
    inp, out = BASE_RATES_UPM[model_id]
    return {
        "input": inp,
        "output": out,
        "cacheRead": inp * CACHE_READ_MULT[0] // CACHE_READ_MULT[1],
        "cacheWrite5m": inp * CACHE_WRITE_5M_MULT[0] // CACHE_WRITE_5M_MULT[1],
        "cacheWrite1h": inp * CACHE_WRITE_1H_MULT[0] // CACHE_WRITE_1H_MULT[1],
    }


def cost_micros(model_id, usage):
    """Integer micro-dollars for one message's usage. Never a float (doc:20 §3).

    Priced per message and per token kind, then floored, so a total is the sum of floors
    rather than the floor of a sum. That biases every total DOWN by at most one micro-dollar
    per (message x token kind) — on the current corpus, 2,399 micro-dollars against a total
    near 4x10^8, or 0.0006%. Pricing the aggregate instead would be marginally closer to the
    real bill but would stop a run total being the sum of its message totals, and a rollup
    that does not add up is worse than a bias six orders of magnitude below the figure
    (doc:60-cost-model §3.3: sums are folded, never stored).
    """
    if model_id == SYNTHETIC_MODEL:
        return 0
    r = rates_upm(model_id)
    total = 0
    for kind, tokens in (
        ("input", usage["inputTokens"]),
        ("output", usage["outputTokens"]),
        ("cacheRead", usage["cacheReadTokens"]),
        ("cacheWrite5m", usage["cacheWrite5mTokens"]),
        ("cacheWrite1h", usage["cacheWrite1hTokens"]),
    ):
        total += tokens * r[kind] // 1_000_000
    return total


# ----------------------------------------------------------------- transcripts ---
USAGE_KINDS = (
    "inputTokens",
    "outputTokens",
    "cacheReadTokens",
    "cacheWrite5mTokens",
    "cacheWrite1hTokens",
)


def zero_usage():
    return dict((k, 0) for k in USAGE_KINDS)


def add_usage(acc, other):
    for k in USAGE_KINDS:
        acc[k] += other[k]
    return acc


def _usage_of(u):
    """Project one API usage block onto the five token kinds doc:60#spend-record names.

    `cache_creation_input_tokens` is split into its 5m and 1h halves because they are billed
    at DIFFERENT multipliers (1.25x vs 2x). Folding them together, or folding either into an
    input total, misprices the dominant term. When the split sub-object is absent the whole
    creation total is charged at the 5m rate and that is stated rather than silently assumed.
    """
    creation = u.get("cache_creation_input_tokens", 0) or 0
    split = u.get("cache_creation") or {}
    h1 = split.get("ephemeral_1h_input_tokens")
    m5 = split.get("ephemeral_5m_input_tokens")
    if h1 is None and m5 is None:
        h1, m5 = 0, creation
    else:
        h1, m5 = h1 or 0, m5 or 0
    return {
        "inputTokens": u.get("input_tokens", 0) or 0,
        "outputTokens": u.get("output_tokens", 0) or 0,
        "cacheReadTokens": u.get("cache_read_input_tokens", 0) or 0,
        "cacheWrite5mTokens": m5,
        "cacheWrite1hTokens": h1,
    }


def read_messages(path):
    """Deduplicated assistant messages from one transcript, oldest first.

    Claude Code writes MORE THAN ONE LINE PER ASSISTANT MESSAGE — one per content block, and
    in subagent transcripts also a partial line written before the message finished. Summing
    lines overcounts by that multiplicity; on this corpus it is 1.83x. So dedupe on
    `message.id`.

    But the frames are NOT interchangeable. `input_tokens`, `cache_read_input_tokens` and
    `cache_creation_input_tokens` are byte-identical across every frame of a message; the
    replay asserts that on every run and reports the count, so no figure is quoted here — a
    number written into a docstring over a growing corpus is stale by the next session.
    `output_tokens` is NOT: a partial frame
    carries a mid-stream count (3, 2, 5 …) that the final frame supersedes (345, 160, 120 …).
    Keeping the first frame therefore undercounts output. The authoritative frame is the one
    with the largest `output_tokens`; that is what this returns, and
    `frame_disagreements` reports any message where the *other* four kinds disagreed,
    which would mean this rule is wrong.
    """
    seen = {}
    order = []
    for line in open(path, "r"):
        line = line.strip()
        if not line:
            continue
        d = json.loads(line)
        if d.get("type") != "assistant":
            continue
        msg = d.get("message") or {}
        mid = msg.get("id")
        if mid is None:
            continue
        if mid in seen:
            rec = seen[mid]
            rec["frames"] += 1
            rec["frameUsages"].append(msg.get("usage") or {})
            cand = _usage_of(msg.get("usage") or {})
            if cand["outputTokens"] > rec["usage"]["outputTokens"]:
                rec["staleOutputTokens"] = rec["usage"]["outputTokens"]
                rec["usage"] = cand
            continue
        rec = {
            "messageId": mid,
            "model": normalise_model(msg.get("model")),
            "effort": d.get("effort"),
            "speed": (msg.get("usage") or {}).get("speed") or "standard",
            "serviceTier": (msg.get("usage") or {}).get("service_tier"),
            "at": d.get("timestamp"),
            "gitBranch": d.get("gitBranch"),
            "sessionId": d.get("sessionId"),
            "agentId": d.get("agentId"),
            "usage": _usage_of(msg.get("usage") or {}),
            "staleOutputTokens": None,
            "toolUseIds": [],
            "frames": 1,
            "frameUsages": [msg.get("usage") or {}],
        }
        content = msg.get("content")
        if isinstance(content, list):
            for b in content:
                if isinstance(b, dict) and b.get("type") == "tool_use":
                    rec["toolUseIds"].append(b["id"])
        seen[mid] = rec
        order.append(mid)
    # A later frame of the same message may carry tool_use blocks the first frame did not.
    for line in open(path, "r"):
        line = line.strip()
        if not line:
            continue
        d = json.loads(line)
        if d.get("type") != "assistant":
            continue
        msg = d.get("message") or {}
        rec = seen.get(msg.get("id"))
        if rec is None:
            continue
        content = msg.get("content")
        if isinstance(content, list):
            for b in content:
                if isinstance(b, dict) and b.get("type") == "tool_use" and b["id"] not in rec["toolUseIds"]:
                    rec["toolUseIds"].append(b["id"])
    return [seen[m] for m in order]


NON_OUTPUT_KINDS = tuple(k for k in USAGE_KINDS if k != "outputTokens")


def frame_disagreements(messages):
    """Frames of one message that disagree on something other than `output_tokens`.

    Non-zero invalidates the max-output selection rule in `read_messages`: it would mean the
    frames are independent charges rather than snapshots of one, and that deduplicating them
    undercounts. Asserted on every run, never assumed.
    """
    bad = 0
    for m in messages:
        if m["frames"] < 2:
            continue
        projected = set(
            json.dumps(dict((k, _usage_of(u)[k]) for k in NON_OUTPUT_KINDS), sort_keys=True)
            for u in m["frameUsages"]
        )
        if len(projected) > 1:
            bad += 1
    return bad


def stale_output_undercount(messages):
    """Output tokens that keeping the FIRST frame instead of the largest would have lost."""
    lost = 0
    for m in messages:
        if m["staleOutputTokens"] is not None:
            lost += m["usage"]["outputTokens"] - m["staleOutputTokens"]
    return lost


def peak_context_tokens(messages):
    """Largest prompt any single request in this run carried.

    The prompt is what was billed as input on that request: fresh input + cache read + cache
    creation. This is the figure doc:00-constitution §6's 300k ceiling is about.
    """
    peak = 0
    for m in messages:
        u = m["usage"]
        n = u["inputTokens"] + u["cacheReadTokens"] + u["cacheWrite5mTokens"] + u["cacheWrite1hTokens"]
        peak = max(peak, n)
    return peak


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def parse_ts(s):
    if not s:
        return None
    return datetime.strptime(s.replace("Z", "+0000"), "%Y-%m-%dT%H:%M:%S.%f%z")


def iso(dt):
    return None if dt is None else dt.strftime("%Y-%m-%dT%H:%M:%S.") + "%03dZ" % (dt.microsecond // 1000)


TRANSCRIPTS_ENV = "MODUS_COST_TRANSCRIPTS"


def project_dir_for(repo_root, home=None, override=None):
    """The directory Claude Code stores this repository's sessions in.

    Derived from the checkout path by default, which is what Claude Code itself does. That
    derivation is wrong in a worktree, a second clone, or anyone else's machine, and a
    baseline a stranger cannot regenerate is not a baseline — so an explicit override wins,
    from `--transcripts` or the MODUS_COST_TRANSCRIPTS environment variable.
    """
    if override:
        return os.path.abspath(os.path.expanduser(override))
    env = os.environ.get(TRANSCRIPTS_ENV)
    if env:
        return os.path.abspath(os.path.expanduser(env))
    home = home or os.path.expanduser("~")
    slug = os.path.abspath(repo_root).replace("/", "-")
    return os.path.join(home, ".claude", "projects", slug)


def tool_use_ids(path):
    """Every tool_use id emitted in one transcript, without building message records.

    `read_messages` walks the file twice and allocates a record per message; the parent-edge
    search only needs the ids, and runs inside a hook's timeout against every sibling
    transcript. This is the cheap path for that.
    """
    ids = set()
    for line in open(path, "r"):
        line = line.strip()
        if not line or '"tool_use"' not in line:
            continue
        d = json.loads(line)
        if d.get("type") != "assistant":
            continue
        content = (d.get("message") or {}).get("content")
        if isinstance(content, list):
            for b in content:
                if isinstance(b, dict) and b.get("type") == "tool_use":
                    ids.add(b["id"])
    return ids


def usd(micros):
    return "$%d.%06d" % (micros // 1_000_000, micros % 1_000_000)
