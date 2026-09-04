---
# modus-0102
title: A plant script's revert step destroys uncommitted edits to tracked beans, and the convention is written nowhere
status: completed
type: task
priority: normal
created_at: 2026-08-30T00:00:00Z
updated_at: 2026-09-04T00:00:00Z
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

Reading `git diff --name-only origin/main` before a push is a widespread agent habit here.
**The repository does state it — in the two-dot form, recommended as "the safe form" — and an
earlier version of this bean said it did not.** That claim came from
`grep -rn "diff --name-only" documentation/ AGENTS.md CLAUDE.md`, three paths that exclude
`.beans/`, which is where it lives:

```
cmd:      git grep -n "diff --name-only" origin/main        # unscoped
observed: .beans/modus-0036--defensive-copy-rule.md:890
            "The safe form is `git rebase -i origin/main`, or reading
             `git diff --name-only origin/main` **before** pushing and refusing any path
             the bean does not own."
exit:     0
```

In its **two-dot** form the command compares two endpoints, so once `main` has moved ahead of
the branch it lists what **`main`** changed as well as what you changed, with nothing to
distinguish them.

Observed on this sprint's own refs — a branch at `67219cc` carrying two files of work, and
`origin/main` at `2c958e4` one commit ahead of its base, that commit touching five bean
files:

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
pre-push review, and the one place the repository states it recommends that form, so an agent
who checks is confirmed in it rather than corrected.

### Its provenance, which is the part that generalises

Reported by the orchestrator on being shown the measurement, and recorded here as reported
rather than as something this bean verified — the incident predates it and left no artefact
this branch can reach:

- The instruction is issued in **every orchestrator brief this sprint, in the two-dot form**.
  That part is reported and not verifiable from this branch.
- **The incident behind it is on disk and verifiable.** `.beans/modus-0036` lines 886–892
  record it: a branch `reset --soft` and force-pushed before its file list was read, where
  "`reset --soft` keeps the working tree and moves the base, so the staged diff is against a
  tree the author never looked at". The instruction is that bean's remedy for it.
- **In the form recommended, it would not have caught it.** Against a branch whose base has
  moved, the two-dot output is dominated by files the branch never touched — precisely the
  noise a staged diff against an unexamined tree would hide inside.

So the correction to the story, which is worse than the version it replaces: a safety
instruction was recommended on `main` and propagated through briefs for a sprint, in a form
that could not perform the function it was added for. **There was an artefact to compare
against, and nobody compared it.** The earlier claim here — that the defect survived because
the repository never stated the rule — was itself the product of a grep scoped past the
directory the rule lives in.

`modus-0036` is `completed`, and `adr:0005-evidence-lives-in-the-work-item#finalisation`
makes it append-only, so **the wrong recommendation stays on `main` until it is amended**.
That is not attempted here: it is an amendment to a frozen bean and belongs to its own change.
It is recorded as this bean's outstanding consequence, because a reader who follows
`AGENTS.md` to the three-dot form and then follows `modus-0036` will find a completed bean
recommending the two-dot one.

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
being bitten.

They were **not** both added by `bean:0058`, as an earlier version of this bean said twice:

```
cmd:      trace each convention to the commit that added it
observed: GITHUB_TOKEN trap   f39f100 (#37)   carries .beans/modus-0058
          sandbox refusals    52fd07e (#42)   carries no bean file at all
exit:     0
```

So the practice here is **inconsistent**, and saying so is more useful than the tidy version.

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
the branch.

**The precedent does not settle it, and the honest version cuts both ways.** Of the two
conventions already in that block, one shipped with a bean and one shipped with none — so
there is a merged precedent for doing exactly what this change first did. That does not make
it right: §7.2 step 1 is the rule, `doc:05-authoring-for-agents` line 168 makes the document
the rule when a bean and a document disagree, and an unchallenged instance is not an
exemption. But it does mean the error was to follow a real local practice rather than to
invent one, and that the practice itself wants correcting.

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
- **Amending `bean:0036`.** Its line 890 recommends the two-dot form as "the safe form", it is
  `completed` and append-only, and correcting it means an `## Amendments` entry with
  `**Claimed:**`, `**Found:**` and `**Evidence:**` — a real correction to a real claim, so the
  entry would be well-formed, unlike the one this bean declined to write against `bean:0058`.
  It is deferred rather than refused, and it is this bean's outstanding consequence.
- `bean:0058`, which is `completed` and frozen, and which this bean deliberately does not
  amend.
- A mechanical check for the hazard. Nothing here can see a scratch script.

## Evidence

Merged in two parts. PR #60, squashed onto `main` as `157a57a`, carried the plant-script
convention and the filename-citation convention; PR #65, squashed as `bd9da18`, carried the
three-dot pre-push form. This bean was `status: in-progress` on `main` through both, so the
close is a separate change (`doc:00-constitution` §7.2.1) and its evidence includes those two
merges — which is why it could not have been written from either branch.

Every `observed` below is taken against `9adb8af`, the commit `main` carries as this is
written, rather than against either branch: a line that was on a branch and has since been
edited by another change is not a line that merged
(`doc:50-memory-and-evidence#corpus-figures`). Criteria 3 and 5 are the two the bean's body
already observed during implementation; both are **re-observed here** on today's tree rather
than cited from that body, because both are claims about how a command behaves and a
recollection of a run is not a run.

| # | criterion | evidence |
|---|---|---|
| 1 | the convention is in the Commands block, inside check 8's budget | F1 — the three paragraphs are lines 49 to 60, and F2's second command puts `## Commands` at 25 and the next heading, `## Workflow`, at 66, so all three fall inside the Commands section; each ends in a `bean:0102` citation. F2 — `AGENTS.md` is 95 lines against the `-le 120` ceiling at `tools/docs-lint.sh:278`, read out of the script rather than from memory of it, so 25 lines of headroom |
| 2 | the wording distinguishes tracked from untracked | F1, first paragraph — `reverts uncommitted edits to **tracked** files under that path`, then `a new bean is untracked and survives, a bean you are closing, amending or correcting does not`. The distinction is stated in both directions, which is what makes it usable: a reader learns which of the two states their own work is in |
| 3 | the boundary is observed, not asserted | F3 — one tracked bean modified and one untracked file created; `git checkout -- .beans` exits 0; afterwards `git status --porcelain -- .beans` reports only `?? .beans/zz-untracked-probe.md` and the ` M` line is gone, while `ls` still finds the untracked probe. The tracked edit was destroyed and the untracked file survived, in one run, on `9adb8af`. F7 records the same boundary meeting live work during this close, and what the other `git checkout` form does instead |
| 4 | the three-dot form, or `@{u}`, is the pre-push review command | F1, second paragraph — the sentence opening *Review what you changed with* names the three-dot form and `@{u}` as the alternatives, and states the two-dot failure mode beside them: it *gives a plausible answer rather than an error*. The command itself is not re-quoted in this cell, because a backtick span cannot nest and the quoted text contains one |
| 5 | the difference is observed on real refs | F4 — a branch cut at `6fbf0e0` with one commit on it, against `origin/main` at `9adb8af`, which `git merge-base --is-ancestor` confirms is ahead of that base. Two-dot lists **21** paths, three-dot lists **1**, and the one is the only file the branch touched. The 20 extra are `main`'s own merges since the base |
| 6 | the provenance is stated, and that the form issued could not do its job | F5 — `bean:0036` still recommends the two-dot form on `main`, at `.beans/modus-0036--defensive-copy-rule.md:889`, line number derived by the `grep` in that fence and not quoted from a reading. Read against F4: against a base that has moved, the recommended form buries the branch's one file among 21, which is the noise the instruction was added to remove. The half that is **reported and not verified here** is that the instruction was issued in every brief this sprint; that is the orchestrator's report, is marked as such in the body above, and no artefact in this repository carries it |
| 7 | the filename-citation convention is recorded, with the reason and the obligation to convert | F1, third paragraph — `check 6 resolves against your tree, so a typed reference to a bean you do not have fails the build. Convert it once the bean merges` |
| 8 | `bash tools/docs-lint.sh` green, check 8 included | F6 — the closing run on this branch's tree. Check 8 is inside it: F2 reads the ceiling out of the same script |

### F1 — the three paragraphs on `main`

```
cmd:      git show 9adb8af:AGENTS.md | sed -n '49,60p'
observed: Planting a violation and reverting it (`doc:00-constitution#observed-failing`) usually means a
          script ending in `git checkout -- .beans`, which reverts uncommitted edits to **tracked**
          files under that path as well as the plant — a new bean is untracked and survives, a bean you
          are closing, amending or correcting does not. Commit before you plant (`bean:0102`).

          Review what you changed with `git diff --name-only origin/main...HEAD` — three dots, or
          `@{u}`. The two-dot form compares endpoints, so once `main` moves ahead it lists what **main**
          changed beside what you did, and gives a plausible answer rather than an error (`bean:0102`).

          Cite a bean raised on a sibling's open pull request by filename (`modus-NNNN`), not as
          `bean:NNNN`: check 6 resolves against your tree, so a typed reference to a bean you do not
          have fails the build. Convert it once the bean merges (`bean:0102`).
exit:     0
```

The block's two blank lines are output, not layout: they are the paragraph breaks in
`AGENTS.md` itself, which is why the excerpt is three paragraphs and not one.

`49,60` bounds the excerpt. It is **not** a claim about where the Commands section ends —
that is the third command in F2, which derives the boundaries rather than leaving a range
chosen for an excerpt to be reread as a section's extent (`doc:50-memory-and-evidence#capturing`,
and `bean:0105` criterion 2, where exactly that reread shipped).

### F2 — the budget, and the ceiling read out of the check that enforces it

```
cmd:      git show 9adb8af:AGENTS.md | grep -c ''
observed: 95
exit:     0

cmd:      git grep -n 'agents_lines' 9adb8af -- tools/docs-lint.sh
observed: 9adb8af:tools/docs-lint.sh:277:agents_lines="$(grep -c '' AGENTS.md)"
          9adb8af:tools/docs-lint.sh:278:[ "$agents_lines" -le 120 ] || fail 8 "AGENTS.md: $agents_lines lines, over the 120 ceiling"
exit:     0

cmd:      git grep -n '^## ' 9adb8af -- AGENTS.md
observed: 9adb8af:AGENTS.md:12:## Routing
          9adb8af:AGENTS.md:25:## Commands
          9adb8af:AGENTS.md:66:## Workflow
          9adb8af:AGENTS.md:86:## Context budget
exit:     0
```

### F3 — the tracked/untracked boundary, re-observed

Run in a detached worktree cut at `9adb8af` and destroyed afterwards, so nothing this branch
was carrying was in reach of the plant. That precaution is this bean's own rule applied to
the run that evidences it.

```
cmd:      printf '\nzz probe line\n' >> .beans/modus-0033--baseline-writer-erases-regression-provenance.md
          printf 'untracked probe\n' > .beans/zz-untracked-probe.md
          git status --porcelain -- .beans
observed:  M .beans/modus-0033--baseline-writer-erases-regression-provenance.md
          ?? .beans/zz-untracked-probe.md
exit:     0

cmd:      git checkout -- .beans
observed: (no output)
exit:     0

cmd:      git status --porcelain -- .beans
observed: ?? .beans/zz-untracked-probe.md
exit:     0

cmd:      ls .beans/zz-untracked-probe.md
observed: .beans/zz-untracked-probe.md
exit:     0
```

The tracked modification is gone from `git status` and the untracked file is still on disk.
Both halves are asserted: a run that only showed the loss would not show that the plant
itself — the untracked file — survives, and it is that asymmetry, not the loss, that makes
the hazard silent.

### F4 — two-dot against a base that has moved, on real refs

Same throwaway worktree. `zz-probe-branch` is cut at `6fbf0e0` — PR #54's squash, which
`git rev-list --count 6fbf0e0..origin/main` puts **2** commits below `main` — and carries one
commit adding one file. The setup is in the fence so
the whole thing is re-runnable; the worktree and the branch were destroyed afterwards, so
nothing here survives to be read instead of re-run.

```
cmd:      git checkout -b zz-probe-branch 6fbf0e0
          printf 'probe\n' > zz-probe-file.txt
          git add zz-probe-file.txt
          git commit -m 'probe: one file'
observed: (setup; the three commands below are the measurement)
exit:     0

cmd:      git merge-base --is-ancestor 6fbf0e0 origin/main; echo $?
observed: 0
exit:     0

cmd:      git diff --name-only origin/main zz-probe-branch
observed: .beans/modus-0014--execution-bounded-context.md
          .beans/modus-0020--claude-code-runner.md
          .beans/modus-0047--require-the-gate-check.md
          .beans/modus-0069--per-request-usage-is-the-published-vocabulary.md
          .beans/modus-0090--constants-that-must-match-an-authority.md
          .beans/modus-0105--the-negative-half-of-observed-failing-is-normative-nowhere.md
          .beans/modus-0106--the-evidence-extractor-reads-only-table-cells.md
          .beans/modus-0107--bean-0103-states-a-count-where-its-own-paragraph-argues-for-a-quantifier.md
          .beans/modus-0112--a-sweep-for-a-wording-read-as-a-sweep-for-a-rule.md
          .beans/modus-0113--a-close-that-rewrites-its-criteria-is-indistinguishable.md
          .beans/modus-0114--nothing-checks-that-a-pull-requests-refs-are-complete.md
          .beans/modus-0115--encode-sprint-2-findings-and-hand-off-to-sprint-3.md
          backoffice/src/agent/mockTransport.ts
          backoffice/src/agent/transport.ts
          backoffice/src/agent/useAgentSession.ts
          backoffice/src/api/types.ts
          backoffice/src/routes/AgentConsole.tsx
          documentation/50-memory-and-evidence.md
          documentation/80-agent-operating-procedure.md
          e2e/tests/agent-console.spec.ts
          zz-probe-file.txt
exit:     0

cmd:      git diff --name-only origin/main...zz-probe-branch
observed: zz-probe-file.txt
exit:     0
```

Nothing is elided. The two-dot list is quoted whole because its length **is** the finding:
an agent scanning it for paths its bean does not own has twenty to acquit before reaching
the one that is theirs, and every one of the twenty is a real path with a plausible reason to
be in a diff. The output is not wrong, which is what makes it dangerous.

### F5 — the recommendation still on `main`, in a bean that may not be edited

```
cmd:      git grep -n 'The safe form is' 9adb8af -- .beans/modus-0036--defensive-copy-rule.md
observed: 9adb8af:.beans/modus-0036--defensive-copy-rule.md:889:  never looked at. The safe form is `git rebase -i origin/main`, or reading
exit:     0
```

The sentence continues onto line 890 with `git diff --name-only origin/main` — the two-dot
form — and `bean:0036` is `completed`, so `adr:0005-evidence-lives-in-the-work-item#finalisation`
makes it append-only. This close does not amend it. It is recorded above as this bean's
outstanding consequence and it stays one.

### F6 — the gate

Captured by the sentinel method (`doc:50-memory-and-evidence#corpus-figures`): the counts
below describe the tree that carries them, so the first run was taken with `@@sp3lint@@`
standing where the line now is, making the asserted string absent from the corpus it
measures; the result was inserted, the command re-run and the two diffed. The substitution is
measurement-neutral by inspection of what the twelve counts read — it lives inside a fenced
block, which check 6 does not scan for references, and it adds no document, anchor, bean,
graph edge or bean id. **The two runs are byte-identical**, which is the step that would
have caught a figure agreeing with itself, so the line below describes the tree that
carries it and not the one before it.

```
cmd:      bash tools/docs-lint.sh
observed: docs-lint: OK — 19 documents, 111 anchors, 1467 references, 98 beans, 37 graph edges, 45 selectable, 98 bean ids, 0 introduced, 98 on origin/main, 5 closing transitions, 50 criteria checked, 4 unnumbered.
exit:     0
```

### F7 — a fourth occurrence, during this close, and what stopped it

The bean above records three occurrences by the author who documented the hazard. This close
produced a fourth, and it is recorded because it is the first one that did **not** cost
anything, and the reason is not that anyone remembered the rule.

A script was written to re-run every `cmd` fence this close adds and diff the output against
the recorded `observed`, which is `doc:50-memory-and-evidence#capturing` applied mechanically.
F4's fence carries its setup, so the script ran the setup too — against the live worktree,
which at that moment held four beans' worth of uncommitted evidence:

```
cmd:      git checkout -b zz-probe-branch 6fbf0e0
observed: error: Your local changes to the following files would be overwritten by checkout:
          	.beans/modus-0068--encode-sprint-1-findings.md
          	.beans/modus-0069--per-request-usage-is-the-published-vocabulary.md
          	.beans/modus-0102--plant-scripts-destroy-uncommitted-work-on-tracked-beans.md
          	.beans/modus-0115--encode-sprint-2-findings-and-hand-off-to-sprint-3.md
          Please commit your changes or stash them before you switch branches.
          Aborting
exit:     1
```

**`git checkout -b` fails closed and `git checkout -- <path>` fails open, and that asymmetry
is the whole finding.** Both are `git checkout`. One refuses when uncommitted work is in
reach, names every file at risk, and prints this bean's own rule — *commit your changes or
stash them* — as its remedy. The other silently discards exactly the same files and exits 0,
which is the transcript in `## Observed` at the top of this bean.

So the hazard is narrower and more fixable than "reverting is dangerous": git already has the
guard, and the pathspec form is where it does not run. Nothing here proposes the fix — a
mechanism at the keystroke is what the section above says is needed and what this bean cannot
reach — but it names where such a mechanism would have to sit, which the earlier three
occurrences did not.

The four files this refusal names are the four this close was carrying. Had the script's first
command been the pathspec form instead, this section would be a report of the loss rather than
of the refusal, written after redoing the work for the fourth time in this bean's history.
