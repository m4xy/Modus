---
# modus-0117
title: The spend ledger is tracked and has never been committed since it was created, so the audit log holds 2 of 274 records and the primary checkout is permanently dirty
status: todo
type: fix
priority: normal
created_at: 2026-09-04T00:00:00Z
---

# The spend ledger is tracked and has never been committed since it was created, so the audit log holds 2 of 274 records and the primary checkout is permanently dirty

`domains/modus/cost/0001.ndjson` is tracked. One commit reachable from `2b67b23` has ever
touched it, `8155b2a`, and it put two records there. The working tree of the primary checkout
holds 274 lines. The other 272 have never been committed, they span 2026-08-29 to 2026-09-04,
and they arrive from 24 distinct agent worktrees as well as from the primary checkout, because
the hook that writes them resolves one fixed path.

Three consequences, in the order they cost something:

1. `doc:40-durability` states that everything outside `.modus/` *"is intended to be committed,
   which is what makes `git` the audit log"* (`documentation/40-durability.md:147-148`, E7),
   and its own tree diagram puts `cost/0001.ndjson` outside `.modus/` (line 135). The audit log
   for spend therefore holds 2 records out of 274. Every figure `doc:60-cost-model` asks for is
   derivable only from one person's working tree.
2. `git status --porcelain` in the primary checkout is never empty and cannot be made empty by
   any act of the agent running there, because the hook fires on the run that would check it.
   `.claude/skills/modus-evidence/SKILL.md:27` makes an empty result a precondition of **every**
   invocation of the plant-observe-revert procedure, and its `## Validation` block at line 100
   makes empty output the pass condition. In the primary checkout both are unsatisfiable
   (E4, E8).
3. Nothing merges the file. Every branch that touches it conflicts at the last line, which
   `bean:0054` predicted and could not fix (E9).

## What was relayed, what was verified, and what was not

The finding arrived as five observations. Four survive; one is wrong as stated and one of the
worries attached to it is wrong outright. This table is the record of that check, not a
restatement of the claims.

| # | relayed | verdict | what was observed |
|---|---|---|---|
| 1 | tracked, not gitignored | **holds** | `git ls-files --error-unmatch` exits 0; `git check-ignore -v` prints nothing and exits 1 (E1) |
| 2 | touched by exactly one commit, `8155b2a` | **holds only when the query is scoped** | `git log 2b67b23 -- <path>` returns exactly `8155b2a`. The same query with `--all` returns 10, and the other 9 sit on `feat/cost-recorder` and `origin/feat/cost-recorder` — an unmerged branch carrying the pre-squash history. A reader who runs the unscoped form gets a different answer and neither is wrong (E2) |
| 3 | a large number of uncommitted entries, still growing | **holds, and the growth was observed inside this session** | 270 at `07:45:13Z`, 270 at `07:48:00Z`, 272 at `07:56:29Z`, all from `git diff --numstat` in the same session (E3, E10) |
| 4 | `.claude/settings.json` registers the recorder on `Stop` and `SubagentStop` | **holds** | both keys, both running `python3 "$CLAUDE_PROJECT_DIR/tools/cost-record.py"` (E5) |
| 5 | the fixed path makes worktree agents append to the primary checkout | **holds** | `tools/cost-record.py:38` and `:40`; 84 of the 272 uncommitted records carry a `cwd` under `.claude/worktrees/agent-*`, from 24 distinct worktrees, and one such worktree's own copy of the ledger is clean at 2 lines (E5, E6) |

Two claims attached to the finding were tested and did not survive.

- **"The mandated evidence procedure can destroy it."** It cannot, as the procedure is written.
  `.claude/skills/modus-evidence/SKILL.md:32` names the revert as `git checkout -- .beans`, a
  pathspec that cannot reach `domains/`. A search of `.beans`, `documentation`, `AGENTS.md` and
  `.claude` at `2b67b23` for a revert naming `domains/`, or a bare `git checkout -- .`, or a
  `git restore` over `domains/`, returns nothing and exits 1 (E8). The blast radius is `.beans`
  and the ledger is outside it. What the procedure does to the ledger is the *opposite* problem:
  it cannot start, because its precondition is already unmet.
- **"An uncommitted ledger contradicts `doc:60-cost-model`."** `doc:60-cost-model` §3.2 says the
  record is *"appended to `domains/<domainId>/cost/NNNN.ndjson` — an append-only log
  (`40-durability.md` §2.2), fsynced per record because it is money"*, and every word of that is
  true of the file on disk. `doc:60` claims nothing about git. The document that does is
  `doc:40-durability`, at the two lines quoted above, and that is the property this contradicts.
  Citing `doc:60` for it would be a pointer carrying content its target does not
  (`doc:05-authoring-for-agents#one-fact-one-place`).

## Why this is a work item and not an observation

The bar is whether it changes what someone does.

- An agent in the primary checkout cannot run the `modus-evidence` skill without violating its
  stated precondition, and today the only available moves are to ignore the precondition or to
  take a worktree. Neither is written down. Every close in this repository needs that procedure.
- A `git checkout`, `git stash` or `git restore` over `domains/` in the primary checkout
  destroys 272 spend records and prints nothing. `bean:0102` records that exact class of loss
  happening five times under `.beans`, where the corpus is at least recoverable from a branch.
- The remedy already has a named candidate that no open item carries: `bean:0054` states *"The
  fix is a `merge=union` attribute, which lives in a root `.gitattributes` this work does not
  own"* (line 427). `bean:0054` is `completed` and therefore frozen, so it closes nothing
  further, and no root `.gitattributes` is tracked (E9). That is the same structural shape
  `bean:0111` records one level up: a follow-up recorded inside a bean that can no longer act
  on it belongs to nobody.

## What is adjacent, and what is distinct

Checked at `2b67b23`. `git grep -l 'domains/modus/cost/0001.ndjson' 2b67b23 -- .beans` returns
`modus-0054`, `modus-0068` and `modus-0069` (E9); the sweep names a commit rather than a
working tree, so this file being written does not change its result.

| bean | subject | why it is not this |
|---|---|---|
| `bean:0111` | `doc:60-cost-model` §3.2's `Enforcement gap:` line names beans that cannot close it, and nothing in `docs-lint` checks that a gap line names a live bean | about the **provenance of a gap line** and the missing field-list comparison. Says nothing about whether the file reaches git |
| `bean:0054` | taking the cost baseline and building the harness-edge recorder | `completed`. It **predicted** the git conflict and named `merge=union` as the fix it did not own. This bean is that unowned follow-up, raised where it can be acted on |
| `bean:0017` | the flat-file durable store adapter — atomic write, fsync, schema validation on write | about the **write path inside the process**. The ledger's writes already succeed; what fails is everything after the write |
| `bean:0039` | repository topology — what becomes its own repository, and when. `type: epic` | a superset that would move `domains/` wholesale. This is one file's git lifecycle and does not wait on that decision |
| `bean:0059` | the stale-figure guard covers the generated block, not the document | about a guard over generated cost figures, not over the log they derive from |
| `bean:0060` | the cursor refusal is all-or-nothing on count, not proportional | about **what the recorder bills**, inside a single record. Orthogonal to where the file ends up |

None of the six is blocked by this and this is blocked by none of them, so no `blocked_by` edge
is added.

## Success criteria

Aimed at the work as it would be done: a decision with its rejected alternatives, one document
that owns the answer, and a mechanism watched failing. Not at a preferred design — which of the
three options below is right is the first thing the work has to settle, and stating it here
would be deciding it without the investigation.

| # | criterion | evidence kind |
|---|---|---|
| 1 | The three candidate answers — commit the ledger on a stated cadence, move it outside git, or make the recorder write per-worktree — are each weighed, and the two rejected ones carry the reason they were rejected, weighed before the decision rather than written after it | citation |
| 2 | `doc:40-durability`'s statement at `documentation/40-durability.md:147-148` is either satisfied for this file or amended to state the exception, in the document that owns the anchor, with no second copy of the rule created | diff |
| 3 | Whatever keeps the ledger's git state correct is observed rejecting a planted violation, output recorded verbatim; if nothing can be made to fail, an `Enforcement gap:` naming a bean is written instead | test-run |
| 4 | The records uncommitted when the work starts are either committed or explicitly abandoned, with the count at that tree and the reason recorded. A silent loss is not an outcome | command |
| 5 | After the change, either a hook run leaves the primary checkout's `git status --porcelain` empty, or `.claude/skills/modus-evidence/SKILL.md`'s precondition states what it excludes and why. One of the two, and the bean says which | observation |
| 6 | A root `.gitattributes` carrying `merge=union` for this path exists, or the reason it is not the answer for an append-only log with per-record ordering is recorded | diff |
| 7 | `bean:0054` is not edited. If its `merge=union` note needs correcting it gains an `## Amendments` entry and nothing else | diff |
| 8 | `bash tools/docs-lint.sh` and `./gradlew qualityCheck` green | test-run |

## Not in scope

- **The record shape.** The harness-edge records carry neither `seq`, nor `kind`, nor `crc`,
  which `doc:40-durability` §2.2.3 and §2.2.5 require of every record in an append-only log
  (E11). That is a real defect and it is a different one: it is about what is inside a line,
  this bean is about whether the file reaches git. It needs its own work item and does not have
  one.
- **`doc:60-cost-model` §3.2's field-list comparison.** `bean:0111`.
- **Moving `domains/` to its own repository.** `bean:0039`, and an epic.
- **Anything under `tools/`, `build.gradle.kts` or `documentation/05-authoring-for-agents`**
  while PRs #69 and #70 are open. The change that closes this bean will need `tools/cost-record.py`
  under at least one of the three options, so it is sequenced after those merge rather than
  scoped around them.

## Evidence — the finding, verified rather than relayed

Every corpus figure below names `2b67b23` as its subject explicitly rather than reading the
working tree, so this file being added to `.beans` cannot change any of them
(`doc:50-memory-and-evidence#corpus-figures`). That is the step the record is
measurement-neutral at, and it is stronger than a sentinel here because the subject is a commit
rather than a checkout. The figures that are *not* corpus figures — the working-tree line counts
in E3 and E10 — are stamped with the clock instead, because their subject is a tree that is
changing by design.

Two open pull requests, #69 and #70, will falsify E9's sweep and E8's search when they merge.
Re-running them belongs to that merge.

The script, verbatim as run. It writes nothing.

```
#!/bin/bash
# Read-only. Every corpus figure names 2b67b23 explicitly, so this record cannot change its
# own subject by existing (doc:50-memory-and-evidence#corpus-figures).
cd /Users/maxholman/IdeaProjects/Modus || exit 9
G=/usr/bin/grep
L=domains/modus/cost/0001.ndjson

echo "=== 0. tree, clock, grep"
git rev-parse HEAD
date -u +%Y-%m-%dT%H:%M:%SZ
$G --version | head -1

echo "=== 1. the ledger is tracked and is not ignored"
git ls-files --error-unmatch "$L"
echo "ls-files exit=$?"
git check-ignore -v "$L"
echo "check-ignore exit=$?"

echo "=== 2. commits reachable from 2b67b23 that touch it"
git log --oneline 2b67b23 -- "$L"
echo "=== 2b. the same query over every ref, and where the extras live"
git log --oneline --all -- "$L" | wc -l
git branch -a --contains e300048

echo "=== 3. what is committed, what is in the working tree, and the shape of the difference"
git show 2b67b23:"$L" | wc -l
wc -l < "$L"
git diff --numstat -- "$L"

echo "=== 4. the age of the uncommitted block"
git diff -U0 -- "$L" | $G '^+' | $G -v '^+++' | python3 -c '
import sys, json
ats = sorted(json.loads(l[1:])["at"] for l in sys.stdin)
print("uncommitted records:", len(ats))
print("oldest at:", ats[0])
print("newest at:", ats[-1])
'

echo "=== 5. the hook, on both events"
$G -n '"Stop"\|"SubagentStop"\|cost-record.py' .claude/settings.json

echo "=== 6. the log path the hook writes to"
$G -n '^REPO = \|^DOMAIN_ID = \|^LOG = ' tools/cost-record.py

echo "=== 7. where the uncommitted records were produced"
git diff -U0 -- "$L" | $G '^+' | $G -v '^+++' | python3 -c '
import sys, json
p = w = 0; names = set()
for l in sys.stdin:
    cwd = json.loads(l[1:]).get("cwd", "")
    if "/.claude/worktrees/" in cwd:
        w += 1; names.add(cwd.rsplit("/", 1)[-1])
    else:
        p += 1
print("cwd is the primary checkout:", p)
print("cwd is a .claude/worktrees/agent-* worktree:", w)
print("distinct such worktrees:", len(names))
'

echo "=== 8. one of those worktrees, and its own copy of the ledger"
git -C .claude/worktrees/agent-sp3-0102 status --porcelain -- "$L"
echo "worktree porcelain exit=$? (nothing printed above means clean)"
wc -l < .claude/worktrees/agent-sp3-0102/"$L"
git -C .claude/worktrees/agent-sp3-0102 log --oneline -1 -- "$L"

echo "=== 9. the primary checkout's whole status"
git status --porcelain

echo "=== 10. the property doc:40 states about everything outside .modus/"
$G -n 'cost/0001.ndjson\|is git-ignored in its entirety\|which is what makes' documentation/40-durability.md

echo "=== 11. the revert the skill actually names, and every revert form the corpus records"
$G -n 'checkout -- ' .claude/skills/modus-evidence/SKILL.md
git grep -c 'checkout -- ' 2b67b23 -- .beans documentation AGENTS.md
echo "--- of those, the ones whose pathspec is not .beans"
git grep -n 'checkout -- ' 2b67b23 -- .beans documentation AGENTS.md | $G -v 'checkout -- \.beans'
echo "--- and any that name domains/ at all"
git grep -n 'checkout -- domains\|checkout -- \.$\|restore.*domains' 2b67b23 -- .beans documentation AGENTS.md .claude
echo "last grep exit=$?"

echo "=== 12. the precondition and the validation command the skill states"
$G -n 'porcelain' .claude/skills/modus-evidence/SKILL.md

echo "=== 13. the remedy bean:0054 named and did not own, and its status"
$G -n 'The spend log will conflict in git' .beans/modus-0054--cost-baseline-and-run-recorder.md
$G -n 'merge=union' .beans/modus-0054--cost-baseline-and-run-recorder.md
$G -n '^status:' .beans/modus-0054--cost-baseline-and-run-recorder.md
git ls-files .gitattributes | $G -c .
echo "tracked .gitattributes files: (count above; grep exit=$?)"

echo "=== 14. corpus sweep, subject named as a commit so this record is not in it"
git grep -l 'domains/modus/cost/0001.ndjson' 2b67b23 -- .beans | sort
echo "--- and the control: a string known to be in .beans at that commit"
git grep -l 'merge=union' 2b67b23 -- .beans | sort

echo "=== 15. the backlog this bean joins, at 2b67b23"
git grep -c '^status: todo$' 2b67b23 -- .beans | wc -l
git grep -l '^type: epic$' 2b67b23 -- .beans | wc -l
```

Its output, verbatim. The `=== n` lines are the script's own `echo`s. Nothing below was edited
after capture.

```
=== 0. tree, clock, grep
2b67b23fa3ef74173ccba511da319dad81a40ffc
2026-09-04T07:56:29Z
grep (BSD grep, GNU compatible) 2.6.0-FreeBSD
=== 1. the ledger is tracked and is not ignored
domains/modus/cost/0001.ndjson
ls-files exit=0
check-ignore exit=1
=== 2. commits reachable from 2b67b23 that touch it
8155b2a feat(cost): record spend at the harness edge (#39)
=== 2b. the same query over every ref, and where the extras live
      10
  feat/cost-recorder
  remotes/origin/feat/cost-recorder
=== 3. what is committed, what is in the working tree, and the shape of the difference
       2
     274
272	0	domains/modus/cost/0001.ndjson
=== 4. the age of the uncommitted block
uncommitted records: 272
oldest at: 2026-08-29T21:55:53.108Z
newest at: 2026-09-04T07:53:59.799Z
=== 5. the hook, on both events
3:    "Stop": [
8:            "command": "python3 \"$CLAUDE_PROJECT_DIR/tools/cost-record.py\"",
14:    "SubagentStop": [
19:            "command": "python3 \"$CLAUDE_PROJECT_DIR/tools/cost-record.py\"",
=== 6. the log path the hook writes to
38:REPO = os.environ.get("CLAUDE_PROJECT_DIR") or os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
39:DOMAIN_ID = "modus"
40:LOG = os.path.join(REPO, "domains", DOMAIN_ID, "cost", "0001.ndjson")
=== 7. where the uncommitted records were produced
cwd is the primary checkout: 188
cwd is a .claude/worktrees/agent-* worktree: 84
distinct such worktrees: 24
=== 8. one of those worktrees, and its own copy of the ledger
worktree porcelain exit=0 (nothing printed above means clean)
       2
8155b2a feat(cost): record spend at the harness edge (#39)
=== 9. the primary checkout's whole status
 M .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md
 M domains/modus/cost/0001.ndjson
=== 10. the property doc:40 states about everything outside .modus/
101:| 2.2.5 | Every record carries `crc`, the CRC-32C of the record's canonical serialisation with the `crc` field itself omitted (§8 makes serialisation deterministic, which is what makes this reproducible on read). `crc` is the last key. |
135:      cost/0001.ndjson                spend event log
147:`.modus/` is git-ignored in its entirety. Everything outside `.modus/` is intended to be
148:committed, which is what makes `git` the audit log.
=== 11. the revert the skill actually names, and every revert form the corpus records
32:`git checkout -- .beans`, which discards uncommitted edits to *tracked* files under that path
2b67b23:.beans/modus-0035--beans-graph-check.md:1
2b67b23:.beans/modus-0051--parallel-bean-id-allocation.md:1
2b67b23:.beans/modus-0055--evidence-required-to-close-a-bean.md:2
2b67b23:.beans/modus-0061--check-14-is-gated-on-numbered-criteria.md:1
2b67b23:.beans/modus-0063--fence-state-inversion-in-the-check-14-analyser.md:2
2b67b23:.beans/modus-0086--check-6-resolves-references-through-a-naive-fence-toggle.md:1
2b67b23:.beans/modus-0087--check-14-verifies-the-shape-of-evidence-not-its-content.md:1
2b67b23:.beans/modus-0093--pasted-output-in-top-level-prose-answers-the-criterion-it-reports-unanswered.md:1
2b67b23:.beans/modus-0099--fence-parity-and-the-citation-matcher-compose-into-a-hole-neither-owns.md:1
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:16
2b67b23:.beans/modus-0104--a-scripted-edit-produces-no-reading-of-its-result.md:1
2b67b23:.beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md:3
2b67b23:AGENTS.md:1
--- of those, the ones whose pathspec is not .beans
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:488:**`git checkout -b` fails closed and `git checkout -- <path>` fails open, and that asymmetry
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:695:grep -rl 'checkout -- \.beans' --include='*.md' . | sort
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:699:grep -rl 'checkout -- \.beans' --include='*.md' . | sort > /tmp/sp3b.txt
2b67b23:.beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md:93:| 4 | The `git checkout -b` / `git checkout -- <path>` asymmetry is observed on one tree, one command apart | test-run |
--- and any that name domains/ at all
last grep exit=1
=== 12. the precondition and the validation command the skill states
27:git status --porcelain          # must be empty before EVERY run; the revert destroys what it finds
91:- [ ] The tree is clean afterwards — `git status --porcelain` is empty. Empty proves the
100:  argv: ["git", "status", "--porcelain"]
=== 13. the remedy bean:0054 named and did not own, and its status
425:- **The spend log will conflict in git.** `domains/modus/cost/0001.ndjson` is append-only and
427:  conflict at the last line. The fix is a `merge=union` attribute, which lives in a root
4:status: completed
0
tracked .gitattributes files: (count above; grep exit=1)
=== 14. corpus sweep, subject named as a commit so this record is not in it
2b67b23:.beans/modus-0054--cost-baseline-and-run-recorder.md
2b67b23:.beans/modus-0068--encode-sprint-1-findings.md
2b67b23:.beans/modus-0069--per-request-usage-is-the-published-vocabulary.md
--- and the control: a string known to be in .beans at that commit
2b67b23:.beans/modus-0054--cost-baseline-and-run-recorder.md
=== 15. the backlog this bean joins, at 2b67b23
      64
       7
```

### E1 to E15, and the four readings that are not self-evident

**E1, E2 — tracked, and touched once.** Step 2 and step 2b answer different questions and both
are true. `git log 2b67b23` walks the history the merged tree has; `git log --all` walks every
ref this clone holds, including `feat/cost-recorder`, whose nine commits are the pre-squash
originals of `8155b2a`. The claim is about `main`'s history, so step 2 is the figure and step 2b
is the control that says why an unscoped run disagrees.

**E3, E10 — the growth, in one session.** Step 3's `272	0` is `git diff --numstat`: 272 lines
added, none removed, so the working-tree file is the committed file plus an append. The counts
below are the same command at three moments in this one session, and are what turns "large" into
"growing".

```
cmd:      git diff --numstat -- domains/modus/cost/0001.ndjson
observed: 270	0	domains/modus/cost/0001.ndjson
at:       2026-09-04T07:45:13Z, from date -u in the same script
exit:     0

cmd:      git diff --numstat -- domains/modus/cost/0001.ndjson
observed: 270	0	domains/modus/cost/0001.ndjson
at:       2026-09-04T07:48:00Z
exit:     0

cmd:      git diff --numstat -- domains/modus/cost/0001.ndjson
observed: 272	0	domains/modus/cost/0001.ndjson
at:       2026-09-04T07:56:29Z
exit:     0
```

Eleven minutes, two records, one session — and the session that took the readings is one of the
writers, which is the property that makes the file unfixable by the agent standing in front of
it. The first draft of this block carried `2026-07-04` for the first reading and a paragraph
reasoning about the two-month gap. There was no gap: the capture file says `2026-09-04T07:45:13Z`
and the digit was introduced while transcribing, not by any run. It is recorded here because a
figure invented while explaining a figure is the failure mode this section exists to guard
against, and it got past one author inside one bean.

**E4, E7 — the property, and what breaks it.** `documentation/40-durability.md:135` puts
`cost/0001.ndjson` in the tree diagram outside `.modus/`; lines 147 and 148 say everything
outside `.modus/` is intended to be committed and that this is what makes git the audit log. 2
of 274.

**E5, E6 — one path, many trees.** `tools/cost-record.py:38` takes `REPO` from
`$CLAUDE_PROJECT_DIR`, which the harness sets to the project directory rather than to the
running agent's worktree, and line 40 joins a fixed suffix onto it. Step 7 is the consequence
measured rather than inferred: 84 records whose own `cwd` field is a worktree path, from 24
distinct worktrees, sitting in the primary checkout's copy. Step 8 is the other half — that
worktree's copy of the same file is clean, at the 2 committed lines.

**E8 — the blast radius, which is narrower than feared.** Step 11's third command searches
`.beans`, `documentation`, `AGENTS.md` and `.claude` at `2b67b23` for `checkout -- domains`, for
a bare `git checkout -- .`, and for a `git restore` naming `domains`, and exits 1 having printed
nothing. Its second command lists the four lines whose pathspec is not `.beans`, and none of the
four is a revert: two are `grep` patterns inside `bean:0102`'s own sweep script, one is prose
about `<path>`, one is a criterion row. So the documented revert cannot reach the ledger. The
exposure is `git stash`, `git checkout <branch>` and `git restore` run by hand over a tree the
agent has been told should be clean — not the skill.

**E9 — the unowned remedy.** `bean:0054:425` and `:427` state the conflict and name
`merge=union` in a root `.gitattributes` as the fix it does not own; `bean:0054:4` is
`status: completed`; `git ls-files .gitattributes` prints nothing, so the count is `0` and the
`grep -c` exits 1. Step 14's sweep names a commit as its subject, so adding this file to
`.beans` does not change it; its control returns `modus-0054`, the file known to contain
`merge=union`, so the sweep can reach the corpus it claims to search.

**E11 — the record shape, recorded and out of scope.** The keys of the last committed record and
of the last uncommitted one:

```
cmd:      git show 2b67b23:domains/modus/cost/0001.ndjson | tail -1 | python3 -c "import sys,json;print(sorted(json.loads(sys.stdin.read()).keys()))"
observed: ['at', 'billingBasis', 'cacheReadTokens', 'cacheWrite1hTokens', 'cacheWrite5mTokens', 'cacheWriteTokens', 'channel', 'costBasis', 'costUsd', 'costUsdDisplay', 'cwd', 'domainId', 'effort', 'endedAt', 'gitBranch', 'inputTokens', 'lastMessageId', 'messages', 'modelId', 'modelIds', 'outcome', 'outcomeBasis', 'outputTokens', 'peakContextTokens', 'repoSha', 'role', 'runId', 'source', 'spawnDepth', 'speed', 'startedAt', 'unavailable']
exit:     0

cmd:      tail -1 domains/modus/cost/0001.ndjson | python3 -c "import sys,json;print(sorted(json.loads(sys.stdin.read()).keys()))"
observed: ['agentDescription', 'at', 'billingBasis', 'cacheReadTokens', 'cacheWrite1hTokens', 'cacheWrite5mTokens', 'cacheWriteTokens', 'channel', 'costBasis', 'costUsd', 'costUsdDisplay', 'cwd', 'domainId', 'effort', 'endedAt', 'gitBranch', 'inputTokens', 'lastMessageId', 'messages', 'modelId', 'modelIds', 'outcome', 'outcomeBasis', 'outputTokens', 'parentRunId', 'peakContextTokens', 'repoSha', 'role', 'runId', 'source', 'spawnDepth', 'speed', 'startedAt', 'unavailable']
tree:     the working tree at 2b67b23; the second command reads the file rather than the commit, because the uncommitted records are the subject
exit:     0
```

Neither list contains `seq`, `kind` or `crc`. `doc:40-durability` §2.2.3 requires the first two
of every record in an append-only log and §2.2.5 requires the third, naming it the last key.
This is recorded here because it was observed while checking something else and would otherwise
be lost; it is in `## Not in scope` and needs an item of its own.

**E12 — the backlog.** Step 15 counts 64 files under `.beans` at `2b67b23` carrying
`status: todo`, and 7 carrying `type: epic`. This bean makes the first number 65 on the tree
that merges it, which is the cost of raising it and is stated rather than left for a reader to
work out.
