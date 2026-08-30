---
# modus-0102
title: A plant script's revert step destroys uncommitted edits to tracked beans, and the convention is written nowhere
status: in-progress
type: task
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# A plant script's revert step destroys uncommitted edits to tracked beans, and the convention is written nowhere

`doc:00-constitution#observed-failing` requires a gate be observed rejecting a planted
violation, and the procedure is plant, observe, revert. In this repository the plant is
almost always a bean and the revert step is `git checkout -- .beans`.

That command does not only revert the plant. It reverts **every uncommitted modification to
a tracked file under that path**, including the edits the plant exists to prove. Nothing in
`AGENTS.md`, `doc:80-agent-operating-procedure` or `doc:00-constitution` says so.

## Observed

```
cmd:      modify a tracked bean in the working tree, create an untracked file beside it,
          run `git checkout -- .beans`
observed: before:
            tracked edit present:   1
            untracked file present: 1
            git status --porcelain -- .beans:
               M .beans/modus-0033--baseline-writer-erases-regression-provenance.md
              ?? .beans/zz-untracked-probe.md
          after `git checkout -- .beans`:
            tracked edit present:   0
            untracked file present: 1
exit:     0
```

**The boundary is tracked versus untracked, and it is exactly the wrong way round for this
sprint's workflow.**

| what the branch is doing | files involved | survives a plant? |
|---|---|---|
| raising a new bean, before its first commit | untracked | **yes** |
| closing beans — flipping `status:` on merged beans | tracked, modified | **no** |
| appending an `## Amendments` entry | tracked, modified | **no** |
| correcting a bean under review | tracked, modified | **no** |
| editing `tools/` or `documentation/` beside a bean plant | tracked, modified | **no**, when the revert step names those paths too |

So the one shape that is safe is a bean nobody has committed yet, and every shape that
carries this sprint's actual work is destroyed. The loss is silent: the plant's own output is
unaffected and looks correct, because the plant ran against the file it planted.

### The instance

Recorded because `doc:00-constitution#evidence-rule` applies to a claim about work as much as
to a claim about code. While applying a review round to `.beans/modus-0087`, a plant script
ending in `git checkout -- .beans` was run against the same tree. Every correction in that
round — a corpus re-measurement, a reversed blast-radius section, a rewritten criteria row
and a `blocked_by` edge — was reverted in one step. The plant printed what it was expected to
print. The loss was found only by re-reading the file afterwards, and the round was redone
from scratch.

## A second command of the same class: `git diff` against a branch point that moved

Reading `git diff --name-only origin/main` before a push is a widespread agent habit here —
it arrives in task briefs rather than from the repository, and **`grep -rn "diff --name-only"
documentation/ AGENTS.md CLAUDE.md` returns nothing**, so nothing on disk says which form is
meant. In its **two-dot** form the command compares two endpoints, so once `main` has moved
ahead of the branch it lists what **`main`** changed as well as what you changed, with
nothing to distinguish them.

Observed on this sprint's own refs — a branch at `67219cc` carrying two files of work, and
`origin/main` at `2c958e4` five bean commits ahead of its base:

```
cmd:      git diff --name-only origin/main 67219cc          # two-dot
observed: 7 files — the two the branch changed, mixed with five it never touched
exit:     0

cmd:      git diff --name-only origin/main...67219cc        # three-dot
observed: .beans/modus-0093--pasted-output-in-top-level-prose-answers-the-criterion-…md
          documentation/05-authoring-for-agents.md
exit:     0
```

Two files, not seven. The three-dot form diffs the **merge base** against the branch tip,
which is what "what did I change" means and what a reviewer will see. `@{u}` reads the same
way against the branch's own upstream.

It is the same class as the plant-script hazard above: a command whose obvious reading is
wrong only in a condition that is invisible when it happens, and which produces a plausible
answer rather than an error. It nearly misled the author of this bean into believing a commit
had touched five bean files it had not — the check that is supposed to catch a mistake became
the thing reporting one.

The two-dot form is not *wrong*; it answers a different question. It is the wrong tool for a
pre-push review, and since the repository never states the instruction at all, every agent
given it in a brief inherits whichever form the brief happened to use.

### Its provenance, which is the part that generalises

Reported by the orchestrator on being shown the measurement, and recorded here as reported
rather than as something this bean verified — the incident predates it and left no artefact
this branch can reach:

- The instruction originates in **an orchestrator brief, not the documentation**, and has
  been issued in every brief this sprint in the two-dot form.
- It was added in response to a real incident: an agent force-pushed a staged revert of
  `main`'s entire history. The command was the check meant to stop that recurring.
- **In the form it was issued, it would not have caught it.** Against a branch whose base has
  moved, the two-dot form's output is dominated by files the branch never touched, which is
  precisely the noise a staged revert of history would hide inside.

So a safety instruction was propagated to every agent for a sprint, in a form that could not
perform the function it was added for, and nothing detected that — because the repository
never stated the rule, so there was no artefact for a check or a reviewer to compare against.
**A convention that lives only in briefs has no place a defect in it can be found.** That is
the argument for this bean's whole shape: the fix is not a better brief, it is a line on disk
where the next reader can disagree with it.

## A third convention: cite a sibling PR's bean by filename until it merges

`docs-lint` check 6 resolves a typed `bean:NNNN` reference against the files in the tree. A
bean raised on a sibling's open pull request is not in your tree, so a typed reference to it
**fails the build** — while the same bean named as `modus-NNNN`, or by its path, passes,
because that form is not a reference at all.

So while a bean is in flight, cite it by filename; convert to `bean:NNNN` once it merges.
The conversion is not optional tidying — a filename citation is invisible to check 6, so it
is never validated, and it will not be noticed when the target's id or slug changes.

Measured on this tree rather than counted from memory:

```
cmd:      grep -rl "typed reference to an unmerged bean\|named by filename" .beans/
observed: modus-0087, modus-0093, modus-0096, modus-0102 — four beans carrying the
          workaround in their own text
exit:     0
```

Four, in one sprint, in one family, and each was found the same way: by writing the natural
typed reference, watching check 6 fail, and working around it. `doc:05-authoring-for-agents`
does not mention the constraint, so every agent meets it by tripping over it — the same shape
as the two conventions above, and the third instance in this bean of a rule that exists only
in the space between briefs and the build.

## Why it belongs in `AGENTS.md` rather than in a document

`AGENTS.md`'s Commands block already carries two conventions of exactly this class: the stale
`GITHUB_TOKEN` credential trap, and the sandbox's refusal of certain command shapes. Both are
working conventions that cost an agent real work when unknown and are only ever discovered by
being bitten. Both were added by `bean:0058`.

`AGENTS.md` states that it never restates a rule. The line added here states a **convention**
and cites `doc:00-constitution#observed-failing` rather than restating it, which is the same
form as the two beside it.

## Why this bean exists at all

The change shipped once without a bean, on the reasoning that `bean:0058` owns unwritten
working conventions but is `completed`, so `docs-lint` check 11 makes it append-only and
`adr:0005-evidence-lives-in-the-work-item#amendments` would require `**Claimed:**`,
`**Found:**` and `**Evidence:**` lines. That half is sound and is verified: this is an
**addition**, not a correction, so there is no original claim for those lines to carry, and
forcing them would produce a malformed amendment written to satisfy a shape — which
`.beans/modus-0087` argues is itself worth refusing.

The half that failed was the jump from "`bean:0058` cannot take it" to "no bean at all",
which never considered raising a new one. `doc:00-constitution` §7.2 step 1 admits no
exception: every branch has exactly one work item, and if none exists it is created before
the branch. The precedent runs the same way — the pull request that added the first two
conventions to this same block carried `bean:0058`.

**"Directed by the orchestrator" is not a waiver.** `doc:00-constitution#independent-review`
gives the orchestrator merge authority, not authority over §7.2 step 1. The instruction to
raise a bean *only if the line would not fit the budget* treated the requirement as a
fallback for a space problem. It is not one, and following it was still the author's error to
catch.

## The bean did not stop its own author repeating the hazard

The plant-script hazard above was written into this bean and into `AGENTS.md`. Its author
then hit it **twice more** while doing the work of this sprint — once mid-way through a
review round on `bean:0087`, losing a corpus re-measurement, a reversed blast-radius section,
a rewritten criteria row and a `blocked_by` edge; and once more during a later round on
`bean:0093`, after the `AGENTS.md` line had already merged.

Three occurrences, by the person who documented it, with the documentation in front of him.
That is this bean's own evidence and it points somewhere specific:

> **A convention its own author cannot follow while writing it down is not a convention
> problem.**

The failure is not insufficient knowledge of the rule — knowledge was maximal, and it still
did not fire at the moment of use, because the hazard's trigger is a command that looks
routine and whose damage is silent and immediate. Restating it more forcefully has been tried
by construction: the third occurrence happened with the rule already on `main`.

It is the same shape `modus-0100` records for verification — named by filename because it is
still unmerged, and a typed reference to an unmerged bean fails check 6. "Instruct agents to
verify" was in every brief all sprint and was still insufficient. Both point at the same conclusion: where a
rule must fire at a specific keystroke, the fix is a mechanism at that keystroke, not a
sentence somewhere a reader has already agreed with. What that mechanism is, this bean does
not decide; it is not reachable from a repository that cannot see a scratch script.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `AGENTS.md` carries the convention in its Commands block, beside the two of the same class, within the 120-line budget check 8 enforces | diff |
| 2 | The wording distinguishes tracked from untracked, because the distinction decides whether an agent's current work is at risk | diff |
| 3 | The tracked/untracked boundary is observed rather than asserted | test-run |
| 4 | `AGENTS.md` carries the three-dot form, or `@{u}`, as the pre-push review command — an addition, since the repository states no such instruction today and the two-dot habit arrives from task briefs | diff |
| 5 | The difference between the two forms is observed on real refs, not asserted | test-run |
| 6 | The convention's provenance is stated — that it originates in briefs rather than in the repository, and that the form issued could not perform the function it was added for | citation |
| 7 | The filename-citation convention for a bean raised on a sibling's open pull request is recorded, with the reason a typed reference fails check 6 and the obligation to convert once it merges | diff |
| 8 | `bash tools/docs-lint.sh` green, check 8 included | test-run |

## Not in scope

- The plant-and-revert procedure itself (`doc:00-constitution#observed-failing`). The
  procedure is correct; what is missing is a warning about how it is usually implemented.
- Changing any plant script in `tools/`. The scripts that carry this hazard are scratch
  files outside the repository.
- `bean:0058`, which is `completed` and frozen, and which this bean deliberately does not
  amend.
- A mechanical check for the hazard. Nothing here can see a scratch script.
