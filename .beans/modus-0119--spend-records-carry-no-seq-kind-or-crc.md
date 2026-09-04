---
# modus-0119
title: Every spend record lacks seq, kind and crc, so the rule the log is written under already classifies each one as torn
status: todo
type: fix
priority: normal
created_at: 2026-09-04T00:00:00Z
---

# Every spend record lacks `seq`, `kind` and `crc`, so the rule the log is written under already classifies each one as torn

`domains/modus/cost/0001.ndjson` is an append-only log under `doc:40-durability#append-only-log`
— `doc:60-cost-model#spend-record` says so in its own first sentence, and §2.2's preamble names
*"cost events"* among the data the shape is for, so the spend ledger is inside the scope of the
rules below and not beside it (E1).

§2.2.3 requires *"a monotonic `seq` (per log), an ISO-8601 UTC `at`, and a `kind`"* of every
record. §2.2.5 requires *"`crc`, the CRC-32C of the record's canonical serialisation with the
`crc` field itself omitted"*, and adds *"`crc` is the last key."* Neither record committed at
`0e4324d` carries `seq`, `kind` or `crc`, and neither does any record in the primary checkout's
working tree at `2026-09-04T08:38:40Z` (E2). They are not computed and then dropped: the string
`crc` occurs zero times in `tools/cost-record.py` and zero times in `tools/cost_lib.py` (E3).

The consequence is not that a check has nothing to operate on. It is the opposite. §2.2.6:

> A record whose line does not parse, **lacks `crc`**, or whose recomputed `crc` does not match
> is **torn**: it is skipped on read, reported, and the log is marked `degraded` (§7). It is
> never repaired and never silently dropped.

Every line in the ledger lacks `crc`. By the rule the file is written under, the whole file is
already torn — so the first reader that implements §7 correctly will skip all of it and mark
the log `degraded`. That reader is `adapters/adapter-persistence-flatfile`, which §7 assigns
the recovery pass to and which is 17 lines of placeholder today; `bean:0017` carries it (E4).

## The reader that does exist, and what it does with a line it cannot parse

`tools/cost-record.py`'s `last_record_for` is the only code in the tree that reads this log. It
is the billing cursor: `Stop` fires per turn, each record is a delta, and the previous record's
`lastMessageId` is where the next one starts. On a line that does not parse it executes
`except ValueError: continue` — skipped, not reported, nothing marked `degraded` (E5). That is
the clause of §2.2.6 it violates, and `crc` is not needed to violate it.

What the skip costs is money. `advance_cursor`'s own docstring names the outcome it was rewritten
to prevent — a missing cursor falling back to billing every message, *"which silently re-bills
the run from its first message into an append-only money log"* — and says *"never that one"*.
But `previous is None` still returns `(messages, "full", None)`, and
`last_record_for` returns `None` for a run whose only record it silently skipped. Driven through
the real functions against a scratch log, a record truncated by one byte turns the cursor into
`None` and `advance_cursor` then bills every message it was handed (E6). The guard is in the
function that consumes the cursor; the hole is in the function that produces it.

Two paths reach a line that does not parse, and neither is hypothetical here. A crash between
`os.write` and `os.fsync` leaves the truncated tail §2.2.7 is written for. And `bean:0117`
records that every branch touching this file conflicts at its last line and that it is merged by
hand today — a conflict marker left in the file is a line that does not parse, in a log whose
only reader discards such lines without a word.

## What was relayed, what was verified, and what was not

| # | relayed | verdict | what was observed |
|---|---|---|---|
| 1 | the records carry neither `seq`, nor `kind`, nor `crc` | **holds, for both the committed records and the working-tree records** | the two key shapes at `0e4324d` and the two in the working tree are the same two, differing only by `parentRunId`; none of the three fields is in any of them (E2) |
| 2 | §2.2.3 requires the first two of every record in an append-only log | **holds** | quoted at E1. It requires `at` as well, and `at` is present on every working-tree record — but the writer emits `"at": None` on its error path, so the field is present rather than satisfied (E7) |
| 3 | §2.2.5 requires the third, naming it the last key | **holds** | quoted at E1 |
| 4 | §2.2.5 is *"the basis for detecting a torn record"* | **holds in substance, wrong in citation** | §2.2.5 defines `crc`; §2.2.6 is what makes a record torn. The distinction matters because §2.2.6 tears on *absence* as well as on mismatch, which is the sharper claim and is not in §2.2.5 |
| 5 | the requirement might be scoped to a kind of log the spend ledger sits outside | **does not hold — the finding is not weakened** | §2.2's preamble names *"cost events"*, §3's layout diagram names `cost/0001.ndjson` as the *"spend event log"*, and `doc:60-cost-model` §3.2 cites `40-durability.md` §2.2 for it (E1) |
| 6 | §2.2.6's torn-record detection has nothing to operate on | **wrong, and the truth is worse** | §2.2.6 needs no field to operate: absence of `crc` is the condition. The detection that is missing is the *reporting*, and the reader that exists skips silently (E5, E6) |
| 7 | §2.2.4's resume cursor has nothing to operate on | **holds, and is the weakest limb** | `Last-Event-ID`, `lastEventId`, `degraded` and `torn` occur nowhere in any `.kt`, `.ts`, `.tsx` or `.py` file in the tree (E4). Nothing streams this log, so the missing `seq` costs nothing *today*. It is a field no reader wants yet, which is a smaller problem than the two above and is recorded as such |

Two things the finding did not contain, found while checking it.

- **The log carries three record shapes and nothing distinguishes them.** One `append` call
  writes the spend record, one writes a `billingBasis: refused` line with `billed: false`, and
  one writes an error line whose `at` is `None` (E7). `kind` is exactly the discriminator
  §2.2.3 asks for; without it a consumer must sniff keys, and the rollups
  `doc:60-cost-model` §3.3 specifies would fold three different things together.
- **`crc` as the last key is incompatible with how the writer serialises.** `append` uses
  `json.dumps(record, sort_keys=True)`, under which `crc` sorts between `costUsdDisplay` and
  `cwd` (E8). §2.2.5's *"`crc` is the last key"* and §8's *"stable frontmatter key order"*
  canonicalisation therefore have to be reconciled before a `crc` can be both written and
  recomputed on read. That is a decision this work has to take, not a detail of it.

## Why this is a work item and not an observation

The bar is whether it changes what someone does.

- A torn line in this log silently re-bills a whole run, in a file that is append-only and
  therefore cannot be corrected in place. `bean:0054` built the refusal path precisely to stop
  that outcome, and the refusal path is reachable only through a cursor the same file discards
  without reporting.
- Every merge of this file is a hand merge today (`bean:0117`), and a hand merge is the cheapest
  way in the repository to produce a line that does not parse.
- `bean:0017` will build a reader that, applied to this file as it stands, marks the spend
  ledger `degraded` in its entirety on the first startup. Whether that is the right outcome, or
  whether the existing records are grandfathered, is a decision someone has to take, and taking
  it after the adapter ships is more expensive than taking it now.

## What is adjacent, and what is distinct

Checked at `0e4324d`. `git grep -l crc 0e4324d -- .beans` returns `modus-0001` and `modus-0117`,
and the control returns the two files known to contain `merge=union` (E9); the sweep names a
commit rather than a working tree, so this file being written does not change its result.

| bean | subject | why it is not this |
|---|---|---|
| `bean:0117` | the ledger is tracked and has never been committed, so git holds two records and the primary checkout is permanently dirty | about whether the **file reaches git**. This is about whether a **record can be validated once it does**. `bean:0117` observed this defect while checking that one, recorded it in its `## Not in scope` as E11, and said it needs an item of its own. This is that item |
| `bean:0111` | `doc:60-cost-model` §3.2's `Enforcement gap:` names no bean that closes it, and nothing compares the documented field list against the records written | about the **provenance of a gap line** and about a comparison between two lists that both already exist. Neither list contains `seq`, `kind` or `crc`, so `bean:0111`'s comparison passing would not add them. The two are complementary: `bean:0111` makes the §3.2 table and the writer agree, this makes the writer and `doc:40-durability` §2.2 agree |
| `bean:0054` | taking the cost baseline and building the harness-edge recorder | `completed`, and it never claimed these fields. Its `## Does the record match doc:60-cost-model#spend-record?` section compares against `doc:60` §3.2, which does not list `seq`, `kind` or `crc` either — so the recorder was checked against a field list that omits the requirement, which is why this went unseen |
| `bean:0017` | the flat-file durable store adapter — per-record CRC-32C, checksummed NDJSON, recovery-on-read that accounts for a degraded log | about the **Kotlin reader and writer inside the server process**. It will read this file and does not write it. If this bean is not done first, `bean:0017`'s first correct run reports the whole ledger torn |
| `bean:0060` | the cursor refusal is all-or-nothing on count, not proportional | about **how much** is re-billed once the cursor is known to be missing. This is about the cursor going missing without anyone being told |
| `bean:0016` | the cost bounded context | an empty placeholder module. The record shape it will eventually own is specified in `doc:60-cost-model`, not decided here |
| `bean:0039` | repository topology — what becomes its own repository, and when. `type: epic` | a superset that would move `domains/` wholesale. This is one record shape and does not wait on it |

None of the seven is blocked by this and this is blocked by none of them, so no `blocked_by`
edge is added. The sequencing note against `bean:0017` is a note, not an edge: `bean:0017` can
be built without this and would then be built against a ledger it must reject.

## Success criteria

Aimed at the work as it would be done. Which of the three answers below is right — bring the
records up to §2.2, amend §2.2 to state the exception, or move the ledger out of §2.2's scope —
is the first thing the work has to settle, so no criterion presumes one.

| # | criterion | evidence kind |
|---|---|---|
| 1 | The three candidate answers — add `seq`, `kind` and `crc` to the writer; amend `doc:40-durability` §2.2 to state the exception a harness-edge log gets and why; or take the spend ledger out of §2.2's scope — are each weighed, and the two rejected ones carry the reason, weighed before the decision rather than written after it | citation |
| 2 | §2.2.5's *"`crc` is the last key"* is reconciled with the writer's `json.dumps(sort_keys=True)`, in the document that owns the anchor, with no second copy of the serialisation rule created | diff |
| 3 | The `seq` a *per-log* monotonic counter means for a file appended to concurrently from many worktrees is settled, together with what `merge=union` would do to it — `bean:0117` is weighing that attribute for this same file and the two answers must not contradict | citation |
| 4 | Whatever computes or checks a record's integrity is observed rejecting a planted violation — a byte flipped, a line truncated, a `crc` recomputed against a mismatch — output recorded verbatim; if nothing can be made to fail, an `Enforcement gap:` naming a bean is written instead (`doc:00-constitution#observed-failing`) | test-run |
| 5 | `last_record_for`'s `except ValueError: continue` either reports and marks the log degraded per §2.2.6, or the bean records why the billing cursor is exempt. Silently skipping is not one of the two | diff |
| 6 | The re-bill that a skipped line causes is demonstrated once against the code as it stands and once against the change, on a scratch log, with both outputs recorded | test-run |
| 7 | The records that exist when the work starts are either migrated, grandfathered by a stated rule, or abandoned, with the count at that tree and the reason recorded. `bean:0017`'s recovery pass must have a defined answer for them | command |
| 8 | `doc:60-cost-model` §3.2's field table either gains the three fields or states that `doc:40-durability` §2.2 owns them and it does not restate them (`doc:05-authoring-for-agents#one-fact-one-place`) — one of the two, and the bean says which | diff |
| 9 | `bean:0054` and `bean:0068` are not edited; both are `completed` | diff |
| 10 | `bash tools/docs-lint.sh` and `./gradlew qualityCheck` green | test-run |

## Not in scope

- **Whether the ledger reaches git at all.** `bean:0117`.
- **The `doc:60-cost-model` §3.2 field-list comparison and its `Enforcement gap:` line.**
  `bean:0111`.
- **How much is re-billed once a cursor is known missing.** `bean:0060`.
- **Building `adapters/adapter-persistence-flatfile`.** `bean:0017`. This bean settles what that
  adapter will find; it does not write it.
- **Anything under `tools/`, `build.gradle.kts` or `documentation/05-authoring-for-agents`**
  while PR #69 is open. Criterion 5 needs `tools/cost-record.py`, so the work is sequenced after
  that merges rather than scoped around it.

## Evidence — the finding, verified rather than relayed

Corpus figures name `0e4324d` explicitly rather than reading the working tree, so this file
being added to `.beans` cannot change any of them (`doc:50-memory-and-evidence#corpus-figures`).
The one figure that is not a corpus figure — step 5's record count in the primary checkout — is
stamped with the clock instead, because its subject is a tree that is changing by design;
`bean:0117` E3 is the record of that growth being watched, and it is not re-measured here.

PR #69 owns `tools/` and will falsify every line number in steps 6 to 9 and 12 when it merges.
Re-running them belongs to that merge.

The script, verbatim as run. It writes nothing inside the repository; step 15's only writes are
under `$SCRATCH`, and the assertion `LOG is under SCRATCH: True` is what proves the real
`domains/modus/cost/0001.ndjson` was not touched.

```
#!/bin/bash
# Read-only against the repository. Corpus figures name 0e4324d explicitly, so this file
# being added to .beans cannot change its own subject (doc:50-memory-and-evidence#corpus-figures).
# Step 5's figure is the primary checkout's ledger, which is changing by design, so it is
# stamped with the clock instead. Step 15 writes only under $SCRATCH.
cd /Users/maxholman/IdeaProjects/Modus/.claude/worktrees/agent-a64e9c6bab4b26433 || exit 9
G=/usr/bin/grep
P=/Users/maxholman/IdeaProjects/Modus/domains/modus/cost/0001.ndjson
SCRATCH=/private/tmp/claude-501/-Users-maxholman-IdeaProjects-Modus/ffd4977c-3d34-41e9-a3ae-f60919540688/scratchpad/fakerepo

echo "=== 0. tree, clock, grep"
git rev-parse HEAD
date -u +%Y-%m-%dT%H:%M:%SZ
$G --version | head -1

echo "=== 1. what doc:40 §2.2 requires, and of which logs"
sed -n '84,86p;97,105p' documentation/40-durability.md

echo "=== 2. the layout entry that names this file"
$G -n 'cost/0001.ndjson' documentation/40-durability.md

echo "=== 3. what doc:60 §3.2 calls it"
sed -n '148,151p' documentation/60-cost-model.md

echo "=== 4. keys of every distinct record shape, committed at 0e4324d"
git show 0e4324d:domains/modus/cost/0001.ndjson | python3 -c '
import sys, json, collections
s = collections.Counter()
n = 0
for l in sys.stdin:
    l = l.strip()
    if not l: continue
    n += 1
    s[tuple(sorted(json.loads(l)))] += 1
print("records:", n)
for k, v in s.items():
    print(v, "record(s):", list(k))
for f in ("seq", "kind", "crc"):
    print("any record carrying %-4s:" % f, any(f in k for k in s))
'

echo "=== 5. the same, over the primary checkout working tree"
python3 -c '
import json, collections
s = collections.Counter(); n = 0
for l in open("'"$P"'"):
    l = l.strip()
    if not l: continue
    n += 1
    s[tuple(sorted(json.loads(l)))] += 1
print("records:", n)
print("distinct key shapes:", len(s))
for f in ("seq", "kind", "crc", "at"):
    print("every record carries %-4s:" % f, all(f in k for k in s))
'

echo "=== 6. are seq/kind/crc computed and dropped, or never computed"
$G -nc 'crc\|CRC\|crc32' tools/cost-record.py tools/cost_lib.py
echo "grep exit=$?"
$G -n '"seq"\|"kind"\|"crc"' tools/cost-record.py tools/cost_lib.py
echo "grep exit=$?"

echo "=== 7. the three record shapes this one writer emits, with nothing to tell them apart"
$G -n 'append({' tools/cost-record.py
$G -n '"billingBasis": "refused"' tools/cost-record.py
$G -n '"error": repr(exc)' tools/cost-record.py

echo "=== 8. the only reader of the log that exists today, and what it does with a bad line"
sed -n '134,151p' tools/cost-record.py

echo "=== 9. what the writer does when that reader returns None"
sed -n '173,175p' tools/cost-record.py

echo "=== 10. does anything in the tree implement §2.2.4, §2.2.6 or §7's degraded state"
$G -rn 'Last-Event-ID\|lastEventId\|degraded\|torn' --include=*.kt --include=*.ts --include=*.tsx --include=*.py . 2>/dev/null
echo "grep exit=$?"
echo "--- and the adapter §7 assigns the recovery pass to"
wc -l adapters/adapter-persistence-flatfile/src/main/kotlin/uk/m4xy/modus/adapter/persistence/flatfile/FlatFilePersistenceAdapter.kt

echo "=== 11. §7's recovery rows for a bad line and a seq gap, and who owns the gap"
sed -n '363,365p;375,377p' documentation/40-durability.md

echo "=== 12. crc as the last key, against how the writer serialises"
$G -n 'json.dumps(record' tools/cost-record.py
python3 -c '
import json
ks = ["costUsdDisplay", "crc", "cwd", "runId", "at"]
print("sort_keys=True puts crc at index",
      sorted(ks).index("crc"), "of", len(ks), "->", json.dumps({k: 1 for k in ks}, sort_keys=True))
'

echo "=== 13. corpus sweep at 0e4324d: which beans already say crc"
git grep -l 'crc' 0e4324d -- .beans | sort
echo "--- and the control, a string known to be under .beans at that commit"
git grep -l 'merge=union' 0e4324d -- .beans | sort

echo "=== 14. the backlog this joins, at 0e4324d"
git grep -c '^status: todo$' 0e4324d -- .beans | wc -l
git grep -c '^status: completed$' 0e4324d -- .beans | wc -l

echo "=== 15. the skip, driven through the real function against a scratch log under \$SCRATCH"
python3 - "$SCRATCH" <<'PY'
import importlib.util, json, os, sys
SCRATCH = sys.argv[1]
TOOLS = "/Users/maxholman/IdeaProjects/Modus/.claude/worktrees/agent-a64e9c6bab4b26433/tools"
os.makedirs(os.path.join(SCRATCH, "domains", "modus", "cost"), exist_ok=True)
os.environ["CLAUDE_PROJECT_DIR"] = SCRATCH
sys.path.insert(0, TOOLS)
spec = importlib.util.spec_from_file_location("costrecord", os.path.join(TOOLS, "cost-record.py"))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
print("LOG is under SCRATCH:", m.LOG.startswith(SCRATCH))

rec = {"at": "2026-09-04T10:00:00.000Z", "runId": "RUN-A", "lastMessageId": "msg-42",
       "endedAt": "2026-09-04T10:00:00.000Z", "costUsd": 1234}
line = json.dumps(rec, sort_keys=True)

def put(text):
    with open(m.LOG, "w") as fh:
        fh.write(text)

put(line + "\n")
print("intact     -> last_record_for('RUN-A') is None:", m.last_record_for("RUN-A") is None)
put(line[:-1] + "\n")
print("torn       -> last_record_for('RUN-A') is None:", m.last_record_for("RUN-A") is None)
put("<<<<<<< HEAD\n" + line + "\n=======\n" + line + "\n>>>>>>> other\n")
print("conflicted -> last_record_for('RUN-A') is None:", m.last_record_for("RUN-A") is None)

msgs = [{"messageId": "m1"}, {"messageId": "m2"}]
sel, basis, note = m.advance_cursor(msgs, None)
print("advance_cursor(previous=None) -> basis %r, messages billed %d of %d"
      % (basis, len(sel), len(msgs)))
PY
echo "exit=$?"
```

Its output, verbatim. The `=== n` lines are the script's own `echo`s. Nothing below was edited
after capture.

```
=== 0. tree, clock, grep
0e4324d3be556136e16e4c05d779207cea09e697
2026-09-04T08:38:40Z
grep (BSD grep, GNU compatible) 2.6.0-FreeBSD
=== 1. what doc:40 §2.2 requires, and of which logs
### 2.2 Logs — newline-delimited JSON, append-only <a id="append-only-log"></a>

For high-volume machine data: agent-run output, domain events, cost events, audit trail.
| 2.2.1 | One JSON object per line. No pretty-printing, no embedded newlines (escape them). |
| 2.2.2 | Append-only. A record is **never** modified or deleted in place. A correction is a new record that supersedes an earlier one by `seq`. |
| 2.2.3 | Every record carries a monotonic `seq` (per log), an ISO-8601 UTC `at`, and a `kind`. |
| 2.2.4 | `seq` is the resume cursor for streaming. An SSE `Last-Event-ID` is a `seq`. |
| 2.2.5 | Every record carries `crc`, the CRC-32C of the record's canonical serialisation with the `crc` field itself omitted (§8 makes serialisation deterministic, which is what makes this reproducible on read). `crc` is the last key. |
| 2.2.6 | A record whose line does not parse, lacks `crc`, or whose recomputed `crc` does not match is **torn**: it is skipped on read, reported, and the log is marked `degraded` (§7). It is never repaired and never silently dropped. |
| 2.2.7 | A truncated final line — no trailing newline, at the end of the file — is discarded on read and truncated away before the next append. This is the **only** permitted repair, it applies only to the last line, and it is logged. |
| 2.2.8 | Logs roll at a size threshold into `NNNN.ndjson` segments. Segments are immutable once rolled. |

=== 2. the layout entry that names this file
135:      cost/0001.ndjson                spend event log
=== 3. what doc:60 §3.2 calls it
### 3.2 The spend record <a id="spend-record"></a>

Appended to `domains/<domainId>/cost/NNNN.ndjson` — an append-only log
(`40-durability.md` §2.2), fsynced per record because it is money.
=== 4. keys of every distinct record shape, committed at 0e4324d
records: 2
1 record(s): ['agentDescription', 'at', 'billingBasis', 'cacheReadTokens', 'cacheWrite1hTokens', 'cacheWrite5mTokens', 'cacheWriteTokens', 'channel', 'costBasis', 'costUsd', 'costUsdDisplay', 'cwd', 'domainId', 'effort', 'endedAt', 'gitBranch', 'inputTokens', 'lastMessageId', 'messages', 'modelId', 'modelIds', 'outcome', 'outcomeBasis', 'outputTokens', 'parentRunId', 'peakContextTokens', 'repoSha', 'role', 'runId', 'source', 'spawnDepth', 'speed', 'startedAt', 'unavailable']
1 record(s): ['at', 'billingBasis', 'cacheReadTokens', 'cacheWrite1hTokens', 'cacheWrite5mTokens', 'cacheWriteTokens', 'channel', 'costBasis', 'costUsd', 'costUsdDisplay', 'cwd', 'domainId', 'effort', 'endedAt', 'gitBranch', 'inputTokens', 'lastMessageId', 'messages', 'modelId', 'modelIds', 'outcome', 'outcomeBasis', 'outputTokens', 'peakContextTokens', 'repoSha', 'role', 'runId', 'source', 'spawnDepth', 'speed', 'startedAt', 'unavailable']
any record carrying seq : False
any record carrying kind: False
any record carrying crc : False
=== 5. the same, over the primary checkout working tree
records: 282
distinct key shapes: 2
every record carries seq : False
every record carries kind: False
every record carries crc : False
every record carries at  : True
=== 6. are seq/kind/crc computed and dropped, or never computed
tools/cost-record.py:0
tools/cost_lib.py:0
grep exit=1
grep exit=1
=== 7. the three record shapes this one writer emits, with nothing to tell them apart
217:        append({"at": C.iso(C.parse_ts(payload.get("_now"))) if payload.get("_now") else None,
385:            append({"at": None, "domainId": DOMAIN_ID, "source": SOURCE, "error": repr(exc),
219:                "billingBasis": "refused", "billed": False, "error": billing_note,
385:            append({"at": None, "domainId": DOMAIN_ID, "source": SOURCE, "error": repr(exc),
=== 8. the only reader of the log that exists today, and what it does with a bad line
def last_record_for(run_id):
    """The newest record already written for this run, or None."""
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
                seen = rec
    return seen

=== 9. what the writer does when that reader returns None
    if previous is None:
        return messages, "full", None
    cursor = previous.get("lastMessageId")
=== 10. does anything in the tree implement §2.2.4, §2.2.6 or §7's degraded state
grep exit=1
--- and the adapter §7 assigns the recovery pass to
      17 adapters/adapter-persistence-flatfile/src/main/kotlin/uk/m4xy/modus/adapter/persistence/flatfile/FlatFilePersistenceAdapter.kt
=== 11. §7's recovery rows for a bad line and a seq gap, and who owns the gap
| Log file whose final line is truncated (no trailing newline) | Truncate to the last complete newline; log at WARN with byte count discarded. Only the final line qualifies (§2.2.7). |
| Log line that fails to parse, or whose `crc` does not match | Skip it, count it, mark the log `degraded`, log at ERROR with the byte offset. **Never repair, never renumber.** A torn record can sit anywhere in the file, not only at the end, so the reader checks every line — a fragment of one record can be followed by a complete later record and then the fragment's tail. |
| Log with a `seq` gap | Log at ERROR; mark the log degraded; do not silently renumber |
**Enforcement gap:** the recovery test suite that would construct each condition above on
disk and assert the exact action taken does not exist. `bean:0017` carries it.

=== 12. crc as the last key, against how the writer serialises
305:    line = (json.dumps(record, sort_keys=True) + "\n").encode("utf-8")
342:    print(json.dumps(record, indent=2, sort_keys=True))
sort_keys=True puts crc at index 2 of 5 -> {"at": 1, "costUsdDisplay": 1, "crc": 1, "cwd": 1, "runId": 1}
=== 13. corpus sweep at 0e4324d: which beans already say crc
0e4324d:.beans/modus-0001--foundation-documentation-package.md
0e4324d:.beans/modus-0117--the-spend-ledger-is-tracked-and-has-never-been-committed.md
--- and the control, a string known to be under .beans at that commit
0e4324d:.beans/modus-0054--cost-baseline-and-run-recorder.md
0e4324d:.beans/modus-0117--the-spend-ledger-is-tracked-and-has-never-been-committed.md
=== 14. the backlog this joins, at 0e4324d
      64
      35
=== 15. the skip, driven through the real function against a scratch log under $SCRATCH
LOG is under SCRATCH: True
intact     -> last_record_for('RUN-A') is None: False
torn       -> last_record_for('RUN-A') is None: True
conflicted -> last_record_for('RUN-A') is None: False
advance_cursor(previous=None) -> basis 'full', messages billed 2 of 2
exit=0
```

### E1 to E9

**E1 — the rules, and that this log is inside them.** Steps 1, 2 and 3. §2.2's preamble names
*"agent-run output, domain events, cost events, audit trail"*; the §3 layout diagram at line 135
names `cost/0001.ndjson` as the *"spend event log"*; `doc:60-cost-model` §3.2 opens by citing
`40-durability.md` §2.2 for this exact path. Three independent statements, none of which admits
an exception for a harness-edge writer. The relayed worry that §2.2 might be scoped narrowly
enough to exclude the ledger is answered by the preamble alone.

**E2 — the key lists, committed and working-tree.** Steps 4 and 5. Two distinct key shapes at
`0e4324d`, differing only in `parentRunId`; the same two shapes across the working tree's 282
records, so the two corpora do **not** differ in the way that mattered. `any record carrying`
is `False` for all three fields at the commit, and `every record carries` is `False` for all
three over the working tree — the two questions are asked in the two directions because a
finding of this shape is falsified by one counter-example either way.

**E3 — never computed, not computed and dropped.** Step 6. `grep -c` returns `0` for both tool
files on `crc\|CRC\|crc32`, and the search for the three quoted key strings prints nothing and
exits 1. There is no code path that produces these values, so this is not a serialisation bug.

**E4 — nothing implements §2.2.4, §2.2.6 or §7's degraded state.** Step 10's recursive search
over every `.kt`, `.ts`, `.tsx` and `.py` file in the tree exits 1 having printed nothing, and
the adapter §7 names is 17 lines. So the resume cursor of §2.2.4 has no consumer, which is the
honest reading of the weakest limb of this finding — and §7 already carries its own
`Enforcement gap:` naming `bean:0017` for the recovery suite that would exercise the rest
(step 11).

**E5 — the reader that does exist.** Step 8 is `last_record_for` whole. `except ValueError:
continue` is the §2.2.6 violation, and it is independent of `crc`: the rule requires a skipped
line to be **reported** and the log **marked degraded**, and this does neither.

**E6 — what the skip costs, driven through the real functions.** Step 15 imports
`tools/cost-record.py` with `CLAUDE_PROJECT_DIR` pointed at a scratch directory, so `LOG`
resolves under `$SCRATCH` — asserted in the output rather than assumed. Three states of the
same one-record log, then the branch the cursor lands in:

- intact: `last_record_for('RUN-A') is None: False` — the cursor is found.
- truncated by one byte: `last_record_for('RUN-A') is None: True` — the cursor is gone, silently.
- wrapped in git conflict markers: `False`. The marker lines are discarded without a word and a
  record is still returned. This one is the *milder* outcome and is reported because it did not
  go the way the reasoning above expected: conflict markers around whole records do not lose the
  cursor, they only prove the silent discard.

Then `advance_cursor(previous=None) -> basis 'full', messages billed 2 of 2`. The truncated
case therefore produces a `full` re-bill of the run, which is the outcome `advance_cursor`'s
docstring says can no longer happen.

**E7 — three shapes, one of them with `at: None`.** Step 7's three `grep` hits are the three
`append` call sites: the spend record at line 217, the refusal at 219, and the error record at
385 whose `"at"` is the literal `None`. `json.dumps` writes that as `null`, so a record exists
in this log whose `at` is null while §2.2.3 requires an ISO-8601 UTC `at`. Step 5 reports
`every record carries at : True` — key presence, not value validity, and the two are stated
separately here because the first does not imply the second.

**E8 — `crc` cannot be the last key under this serialiser.** Step 12. `append` calls
`json.dumps(record, sort_keys=True)`; over a five-key sample including the two keys `crc` would
fall between, `crc` lands at index 2 of 5. Any implementation of §2.2.5 here has to either drop
`sort_keys` for a canonical order that ends in `crc`, or amend §2.2.5. Criterion 2 is that
decision.

**E9 — the corpus sweep and its control.** Step 13 names a commit as its subject, so adding this
file to `.beans` does not change it. It returns `modus-0001`, whose line 186 already
describes a per-record CRC-32C as *"(`crc`, last key, computed over the canonical serialisation
with `crc` omitted)"*, and `modus-0117`, which recorded this finding and deferred it. The control returns the two files
known to contain `merge=union` at that commit, so the sweep can reach the corpus it claims to
search. Step 14 puts the backlog this joins at 64 `todo` and 35 `completed` beans at `0e4324d`;
this makes the first number 65 on the tree that merges it.
