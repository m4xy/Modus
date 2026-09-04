---
# modus-0116
title: The plant hazard recurs through the capture procedure, and the rule that would stop it is stated in a file no check reads
status: completed
type: fix
priority: high
created_at: 2026-09-04T00:00:00Z
updated_at: 2026-09-04T00:00:00Z
---

# The plant hazard recurs through the capture procedure, and the rule that would stop it is stated in a file no check reads

`bean:0102` records that a plant script's `git checkout -- .beans` destroys uncommitted edits
to **tracked** beans, and gives *"commit before you plant"* as the remedy. That remedy reads as
a precaution taken once, before a first plant.

It is not enough, and the reason is in the repository's own recommended workflow.
`doc:50-memory-and-evidence#corpus-figures` requires a figure whose subject is this repository
to be captured against a sentinel: **run, paste, re-run, diff**. The paste writes the figure
back into a tracked bean, so it re-creates precisely the uncommitted modification the commit
had just removed — and it does so *between* the two runs, where the second run's own revert
step then discards it. Committing before the plant protects the first run and nothing after it.

## What was already stated, and what was not

The half that is **not new** is the rule. `.claude/skills/modus-evidence/SKILL.md:27`, under
`## Preconditions`, has always required `git status --porcelain` to be empty, and a precondition
of a procedure named *plant, observe, revert* is a precondition of **every** invocation of it,
not of the first. `doc:70-skills#required-sections` §3.3 makes preconditions normative: each is
checkable by a command, and the skill "fails fast if a precondition is unmet; it never proceeds
hopefully."

The half that **is** new is the mechanism — that the recommended capture procedure is what
re-dirties the tree between runs — and the two defects that follow from it:

1. The skill states the wrong cost. *"a dirty tree makes the revert ambiguous"* understates it:
   the revert does not become ambiguous, it becomes destructive, silently and at exit 0.
2. **The skill's validation command cannot see the failure, and prefers it.** Its `validation`
   block runs `git status --porcelain` and requires empty output. A run that destroyed the
   author's uncommitted work leaves an empty tree and **passes**; a run that left that work
   intact leaves a dirty tree and **fails**. The check rewards the destructive outcome.

## How this was nearly missed, which is the part that generalises

The amendment this change writes was first drafted asserting that the recurrence rule was
stated nowhere. That claim was supported by a five-pattern sweep with a control, taken on the
merge base, which reproduces exactly and is **wrong**, because `SKILL.md` says *"revert the
plant"* and never `git checkout -- .beans`, and says *"dirty tree"* rather than *"dirties the
tree"*. Not one of the five patterns can reach it.

This is `bean:0112` — *a grep over chosen phrasings is evidence about wording, and is read as
evidence about a rule* — whose table records that a false negative of exactly this shape is
caught by **nothing**. The sweep's single near-hit was `.beans/modus-0051` line 107, which reads
*"Procedure: `modus-evidence` skill — plant, observe, revert"*: a pointer to the file that
decides the question, discarded as another bean describing its own procedure rather than
followed.

There is a structural reason the file is easy to miss. `tools/docs-lint.sh` contains **zero**
references to `.claude`, so no check reads any skill: not check 6 for its typed references, not
check 8 for a budget, not check 9. A normative precondition lives outside everything the build
can see. That is `bean:0062`, still open, and this change is another instance of its cost
rather than a fix for it.

## What "reconcile" means here

After this change the rule is stated in two places and they are not copies:

| file | what it owns | why there |
|---|---|---|
| `.claude/skills/modus-evidence/SKILL.md` `## Preconditions` | the rule and its cost — clean before every invocation, what the revert destroys, and why the capture procedure re-dirties the tree between runs | the procedure's own file, loaded at the moment the procedure runs, which is where `bean:0102` concludes a rule of this kind has to fire |
| `AGENTS.md` | the trap a **hand-written** script walks into, and a pointer naming the skill as the owner of the rule | every occurrence `bean:0102` records was a scratch script, not a skill invocation, so the population at risk is the one that never loads the skill |

`doc:05-authoring-for-agents#one-fact-one-place` forbids the second from carrying content the
first does not — *"a pointer that carries content its target does not is an unowned rule"* — and
after this change it does not: `AGENTS.md` states a strict subset and names its owner. The
division is stated here so a later reader can tell which sentence is the copy and which is the
source, rather than deducing it.

## Success criteria

**The criteria are those of the work as shipped, not of work not yet begun.** This bean was
raised after the branch rather than before it (see the closing section), so 2, 5, 6 and 7 name
decisions the change had already taken — `SKILL.md:27` by name, keeping the failed sweep, and
the two reconciliation edits. Writing forward-looking substitutes for them would be inventing a
record, and a criteria table that cannot be told apart from one written before the work is
`bean:0113`'s subject. The handling is `bean:0105`'s, which states in its own criteria section
which work its criteria belong to rather than leaving a reader to infer it.

| # | criterion | evidence kind |
|---|---|---|
| 1 | `bean:0102` gains exactly one `## Amendments` entry and its body is unchanged, byte for byte, from the merge base | diff |
| 2 | The entry's `**Found:**` claims only the half that is new — the mechanism — and states that the recurrence rule was already stated at `SKILL.md:27` | diff |
| 3 | The paste-then-revert cycle is observed end to end, in a disposable worktree, with the commit taken first and the paste destroyed anyway | test-run |
| 4 | The `git checkout -b` / `git checkout -- <path>` asymmetry is observed on one tree, one command apart | test-run |
| 5 | The failed sweep is left standing in the entry beside what it missed, cited to `bean:0112`, rather than deleted and replaced with a correct one | diff |
| 6 | `SKILL.md` states the cost of a dirty tree and the limit of its own validation command | diff |
| 7 | `AGENTS.md` carries no rule the skill does not, and names the skill as owner, within check 8's 120-line budget | diff |
| 8 | `bash tools/docs-lint.sh` and `./gradlew qualityCheck` green | test-run |

## Not in scope

- **Making any check read `.claude/`.** That is `bean:0062` and it is a change to
  `tools/docs-lint.sh`, which PR #69 has open; this change would collide with it.
- **Closing `bean:0102`.** It is `completed` and frozen
  (`adr:0005-evidence-lives-in-the-work-item#finalisation`); it gains an amendment and nothing
  else.
- Re-taking the amendment's corpus sweep for PRs #69 and #70. Re-running a sweep belongs to the
  merge that falsifies it (`doc:50-memory-and-evidence#corpus-figures`), not to this author.
- A mechanism at the keystroke. `bean:0102` states it does not decide what that is, and neither
  does this.

## Why this bean was raised after the branch, which §7.2 step 1 forbids

`doc:00-constitution` §7.2 step 1 requires the work item first: *"Every branch has exactly one
work item in `beans/`. If none exists, create it before you create the branch."* This branch ran
without one for two review rounds. The only bean in its diff was `bean:0102`, which is
`completed` and frozen and therefore cannot be this change's work item, and the amendment named
`bean:0096` — a bean that supplied the finding and did not make the change.

The author raised the conflict in the pull request's `review_focus` and shipped anyway.
`bean:0102`'s own `## Why this bean exists at all` section had already settled that:
*"§7.2 step 1 admits no exception"*, and *"'Directed by the orchestrator' is not a waiver."* The
brief said to allocate no ids, which is a real constraint and is not one of the exceptions §7.2
does not have — the correct move was to stop and ask, which is what the brief itself instructed
for that case. The id was allocated by the orchestrator on review, and the amendment's heading
now names this bean. The `bean:0096` provenance stays in the entry's prose, where it is true.

## Closing evidence — merged as PR #71, squashed onto `main` as `2b67b23`

A bean cannot close itself, so this is the next change (`doc:00-constitution#bean-lifecycle`).
What it adds is the half a branch run cannot carry: that the artefacts the criteria name are on
`main`, and that they are the ones this bean asked for. Every command reads `2b67b23`
explicitly, so the outputs come from any checkout rather than from this one
(`doc:50-memory-and-evidence#corpus-figures`).

**All eight criteria are met.** Criterion 8 carries one residual, stated in its row rather than
glossed.

| # | criterion | observed |
|---|---|---|
| 1 | `bean:0102` gains exactly one `## Amendments` entry and its body is unchanged, byte for byte, from the merge base | `331	0` for that file — an append with nothing deleted; one `## Amendments` at line 519 and one entry heading, `### 2026-09-04 · bean:0116`, at 521; and `cmp` of the merged blob truncated to the merge base's 31143 bytes against the merge base exits 0. Blocks 1a, 1b, 1c |
| 2 | The entry's `**Found:**` claims only the half that is new — the mechanism — and states that the recurrence rule was already stated at `SKILL.md:27` | Line 529 opens `**Found:** two halves, and only one of them is this amendment's.` and line 530 names `.claude/skills/modus-evidence/SKILL.md:27`. Block 2 |
| 3 | The paste-then-revert cycle is observed end to end, in a disposable worktree, with the commit taken first and the paste destroyed anyway | F9 at line 574, run under `git worktree add --detach <path> f55de2a` per line 578. Block 3 |
| 4 | The `git checkout -b` / `git checkout -- <path>` asymmetry is observed on one tree, one command apart | Steps 4 and 5 of that same script — line 609 and line 614 in the script, line 640 and line 648 in its captured output. Block 3 |
| 5 | The failed sweep is left standing in the entry beside what it missed, cited to `bean:0112`, rather than deleted and replaced with a correct one | F10 at line 674; line 760 states it is kept rather than corrected; `git grep -c 'bean:0112'` returns 2 in that file. Block 4 |
| 6 | `SKILL.md` states the cost of a dirty tree and the limit of its own validation command | Line 27 now ends `the revert destroys what it finds`; line 31 opens the paragraph that states the cost; line 106 opens the paragraph that states the validation command's limit. Block 5 |
| 7 | `AGENTS.md` carries no rule the skill does not, and names the skill as owner, within check 8's 120-line budget | `AGENTS.md:52` hands the rule to the skill by name; the sentence it points at is `SKILL.md:31`, which states that rule and more; `AGENTS.md` is 96 lines against check 8's 120. Blocks 5 and 6 |
| 8 | `bash tools/docs-lint.sh` and `./gradlew qualityCheck` green | `bash tools/docs-lint.sh` in a clean detached worktree at `2b67b23` exits 0. PR #71's checks: `gate SUCCESS`, `build + mechanical gates SUCCESS`, `which halves SUCCESS`, `backoffice + e2e SKIPPED`, on the pull request whose merge commit is `2b67b23`. **Residual:** `./gradlew qualityCheck` was never run locally on `2b67b23` itself, and CI runs a per-path subset of it (`doc:00-constitution#workflow` §7.2.4), so the CI half is a weaker claim than the criterion's wording. The local `qualityCheck` that exists is the one on the branch carrying this close. Blocks 7 and 8 |

### Blocks 1a to 6 — one script, one capture

The script, verbatim as run. It is read-only: every command is a `git grep`, a `git show`, a
`wc` or a `cmp`, and nothing writes to the tree.

```
#!/bin/bash
# Read-only. Does bean:0116's shipped change satisfy bean:0116's criteria, on the merged tree?
cd /Users/maxholman/IdeaProjects/Modus || exit 9
B=.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md
S=.claude/skills/modus-evidence/SKILL.md

echo "=== 1a. criterion 1: what the merge commit changed, added and removed"
git show 2b67b23 --numstat --format=''

echo "=== 1b. criterion 1: the Amendments heading, and every entry heading under it"
git grep -n '^## Amendments$' 2b67b23 -- "$B"
git grep -nE '^### [0-9]{4}-[0-9]{2}-[0-9]{2} ' 2b67b23 -- "$B"

echo "=== 1c. criterion 1: the retained bytes are the merge base's bytes, not merely undeleted"
git show f55de2a:"$B" | wc -c
git show 2b67b23:"$B" | head -c "$(git show f55de2a:"$B" | wc -c)" | cmp - <(git show f55de2a:"$B")
echo "cmp exit=$?"

echo "=== 2. criterion 2: what the entry claims as new, and where it says the rule already was"
git grep -n 'Found:\*\* two halves' 2b67b23 -- "$B"
git grep -n 'stated\.\*\* `\.claude/skills/modus-evidence/SKILL.md:27`' 2b67b23 -- "$B"

echo "=== 3. criteria 3 and 4: the disposable-worktree probe, and the asymmetry inside it"
git grep -n 'F9 — the commit is taken, and the paste is destroyed anyway' 2b67b23 -- "$B"
git grep -n 'git worktree add --detach' 2b67b23 -- "$B"
git grep -n 'at that moment: git checkout -b, the OTHER checkout form' 2b67b23 -- "$B"
git grep -n 're-run: the same plant step the first run used' 2b67b23 -- "$B"

echo "=== 4. criterion 5: the failed sweep, kept rather than corrected, cited to bean:0112"
git grep -n 'F10 — a sweep that reproduces exactly and is wrong' 2b67b23 -- "$B"
git grep -n 'The failed sweep is left standing above rather than replaced' 2b67b23 -- "$B"
git grep -c 'bean:0112' 2b67b23 -- "$B"

echo "=== 5. criterion 6: SKILL.md states the cost, and the limit of its own validation command"
git grep -n 'must be empty before EVERY run' 2b67b23 -- "$S"
git grep -n 'Empty output proves the plant is gone' 2b67b23 -- "$S"
git grep -n 'Empty before every invocation' 2b67b23 -- "$S"

echo "=== 6. criterion 7: AGENTS.md hands the rule to the skill, and its length against check 8"
git grep -n 'Preconditions own the' 2b67b23 -- AGENTS.md
git show 2b67b23:AGENTS.md | wc -l
/usr/bin/grep -n 'exceeds 120 lines' documentation/05-authoring-for-agents.md
```

Its output, verbatim. Nothing below was edited after capture; the `=== n` lines are the
script's own `echo`s.

```
=== 1a. criterion 1: what the merge commit changed, added and removed
331	0	.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md
125	0	.beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md
17	2	.claude/skills/modus-evidence/SKILL.md
4	3	AGENTS.md
=== 1b. criterion 1: the Amendments heading, and every entry heading under it
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:519:## Amendments
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:521:### 2026-09-04 · bean:0116
=== 1c. criterion 1: the retained bytes are the merge base's bytes, not merely undeleted
   31143
cmp exit=0
=== 2. criterion 2: what the entry claims as new, and where it says the rule already was
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:529:**Found:** two halves, and only one of them is this amendment's. **The rule is already
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:530:stated.** `.claude/skills/modus-evidence/SKILL.md:27`, tracked at `f55de2a` and quoted in F11,
=== 3. criteria 3 and 4: the disposable-worktree probe, and the asymmetry inside it
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:574:#### F9 — the commit is taken, and the paste is destroyed anyway
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:578:Run it only in a disposable worktree — `git worktree add --detach <path> f55de2a` — which is
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:609:echo "=== 4. at that moment: git checkout -b, the OTHER checkout form"
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:640:=== 4. at that moment: git checkout -b, the OTHER checkout form
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:614:echo "=== 5. re-run: the same plant step the first run used"
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:648:=== 5. re-run: the same plant step the first run used
=== 4. criterion 5: the failed sweep, kept rather than corrected, cited to bean:0112
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:674:#### F10 — a sweep that reproduces exactly and is wrong about the one file that decides it
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:760:The failed sweep is left standing above rather than replaced by a corrected one. A corrected
2b67b23:.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md:2
=== 5. criterion 6: SKILL.md states the cost, and the limit of its own validation command
2b67b23:.claude/skills/modus-evidence/SKILL.md:27:git status --porcelain          # must be empty before EVERY run; the revert destroys what it finds
2b67b23:.claude/skills/modus-evidence/SKILL.md:106:Empty output proves the plant is gone. It does **not** prove your own work survived: the
2b67b23:.claude/skills/modus-evidence/SKILL.md:31:**Empty before every invocation, not once before the first.** Step 5's revert is usually
=== 6. criterion 7: AGENTS.md hands the rule to the skill, and its length against check 8
2b67b23:AGENTS.md:52:closing, amending or correcting does not. The `modus-evidence` skill's Preconditions own the
      96
211:| 8 | line budget | a `documentation/*.md` is outside the line range `documentation/README.md` states, or `AGENTS.md` exceeds 120 lines |
```

Three readings that are not obvious from the lines themselves.

- Block 1a's `331	0` is check 11's condition and says only that nothing was deleted. Block 1c
  is what says the retained bytes are the same bytes: the merged blob truncated to the merge
  base's length, compared against the merge base, `cmp` silent at exit 0.
- Block 3 returns two hits per pattern by design. The lower line number is the script's own
  `echo` inside the fenced script, the higher is that `echo` reappearing in the fenced
  transcript beneath it — so the script and the run of that script are both on `main`, which
  one hit could not establish.
- Block 4's third command is `git grep -c`, so its output line ends in a count rather than in a
  line of file text — the trailing `:2` on that path. Two occurrences of `bean:0112`, at the
  lines the two commands above it print.

### Block 7 — criterion 8, the mechanical half, on a tree that is only `2b67b23`

The first run of this figure was taken in the primary checkout with this bean's `status:`
already flipped, and it measured the working tree rather than the commit — the failure it
reported is recorded below under `### The run that proves this one examined something`. This
capture is taken in a worktree detached at `2b67b23` and destroyed afterwards, so the tree
under `docs-lint` is that commit and nothing else.

```
cmd:      git worktree add --detach <scratch>/at2b67b23 2b67b23
          cd <scratch>/at2b67b23 && git rev-parse HEAD
          git status --porcelain -- .beans documentation AGENTS.md | wc -l
          bash tools/docs-lint.sh
observed: 2b67b23fa3ef74173ccba511da319dad81a40ffc
                 0
          docs-lint: OK — 19 documents, 111 anchors, 1486 references, 99 beans, 37 graph edges, 45 selectable, 99 bean ids, 0 introduced, 99 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0
```

The `0` is the vacuity guard on the tree rather than on the check: no modification under the
three paths `docs-lint` reads, so the `OK` line describes `2b67b23`. `0 closing transitions`
and `0 criteria checked` are correct there — no bean closes at that commit, and check 14
reports `0` rather than `-` because `origin/main` is present.

### Block 8 — criterion 8, the CI half

```
cmd:      GITHUB_TOKEN= gh pr view 71 --json statusCheckRollup -q '.statusCheckRollup[] | "\(.name)\t\(.conclusion)"' | sort -u
observed: backoffice + e2e	SKIPPED
          build + mechanical gates	SUCCESS
          gate	SUCCESS
          which halves	SUCCESS
exit:     0

cmd:      GITHUB_TOKEN= gh pr view 71 --json mergeCommit,mergedAt -q '.mergeCommit.oid, .mergedAt'
observed: 2b67b23fa3ef74173ccba511da319dad81a40ffc
          2026-09-04T07:05:23Z
exit:     0
```

The second command is what ties the first to this close: the checks are green on the pull
request whose merge commit is the tree every other block here reads. `backoffice + e2e SKIPPED`
is the per-path subset behaving as `doc:00-constitution#workflow` §7.2.4 describes, and is why
`gate` exists as a check a skipped half cannot leave unreported.

### Block 9 — the local `qualityCheck` criterion 8's residual names

Run on the branch carrying this close, not on `2b67b23`, and recorded here rather than left as
an assertion in the row above.

```
cmd:      ./gradlew qualityCheck
observed: > Task :docsLint
          docs-lint: OK — 19 documents, 111 anchors, 1504 references, 100 beans, 37 graph edges, 46 selectable, 100 bean ids, 1 introduced, 99 on origin/main, 1 closing transitions, 8 criteria checked, 0 unnumbered.

          > Task :qualityCheck
          [...]
          BUILD SUCCESSFUL in 22s
          168 actionable tasks: 5 executed, 2 from cache, 161 up-to-date
tree:     chore/close-0116-raise-0117, this close and bean:0117 present, uncommitted
exit:     0
```

The `[...]` between `> Task :qualityCheck` and `BUILD SUCCESSFUL` elides Gradle's incubating
problems-report line and its Gradle 10 deprecation notice, neither of which the claim rests on;
the exit code and `BUILD SUCCESSFUL` do, and both are present. `1 closing transitions, 8
criteria checked` is check 14 examining this section rather than skipping it, which the run at
`2b67b23` in Block 7 reports as `0` and `0`.

### The run that proves this one examined something

`docs-lint: OK` and a `docs-lint` that examined nothing print differently here — the counts on
the `OK` line are the difference — but neither says the check would have rejected *this* close.
The run below does. It was taken in the primary checkout after `status:` was set to `completed`
and before this section existed, and it is not a plant: it is the intermediate state of this
change, kept because `doc:00-constitution#observed-failing` values a mechanism watched
rejecting a real violation over one watched passing.

```
cmd:      bash tools/docs-lint.sh
observed: FAIL check 14 .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md: closes with no evidence section; a criterion's command, expectation and verbatim observed output live in the bean (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md: criterion 1 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md: criterion 2 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md: criterion 3 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md: criterion 4 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md: criterion 5 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md: criterion 6 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md: criterion 7 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md: criterion 8 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          docs-lint: 9 failure(s).
tree:     the working tree at `2b67b23` with only this bean's front-matter edited
exit:     1
```

Nine failures, one per numbered criterion plus the missing home, on a bean whose
`## Success criteria` table carries an `evidence kind` column — which check 14 does not count
as an evidence column, by design. So the table this bean already had answers nothing, and the
section above it is what answers the eight.
