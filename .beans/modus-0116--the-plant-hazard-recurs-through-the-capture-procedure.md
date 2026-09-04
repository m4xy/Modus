---
# modus-0116
title: The plant hazard recurs through the capture procedure, and the rule that would stop it is stated in a file no check reads
status: in-progress
type: fix
priority: high
created_at: 2026-09-04T00:00:00Z
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
