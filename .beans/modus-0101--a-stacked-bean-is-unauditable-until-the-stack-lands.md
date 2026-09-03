---
# modus-0101
title: A bean whose evidence ships across a stack is unauditable until the last merge
status: todo
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# A bean whose evidence ships across a stack is unauditable until the last merge

`docs-lint` check 14 asks whether every numbered criterion in a closing bean is answered by
evidence. When one bean's evidence is written across several pull requests, there is **no
tree on which that question is both runnable and meaningful** until the last of them merges.

`bean:0068` is the worked case. Its evidence is in five sections, one per branch of a stack
of five.

## The measurement

The same two commands, on every head in the stack, in stack order. Each row was run rather
than reasoned about; the first version of this section reported a run that **no tree in this
stack produces**, and the correction is the reason the section is now a table.

```
cmd:      sed -i '' 's/^status: in-progress$/status: completed/' .beans/modus-0068--…md
          bash tools/docs-lint.sh
observed: the table below, one row per tree
tree:     each row's own named head; origin/main at 9c9940d, which check 13c reads
```

| tree | result |
|---|---|
| `97f13b0` — `main` when the base merged | 1 failure, naming **no criterion**: `the table under 'Evidence' numbers criteria in an evidence section but carries no evidence column` |
| `docs/encode-sprint-1-agent-loop` head | 1 failure, the same, naming no criterion |
| `docs/encode-sprint-1-claims` head | 1 failure, the same, naming no criterion |
| `docs/encode-sprint-1-gate-design` head | `criterion 14 is not answered in the evidence; no evidence row bears its number and nothing cites it`, and a check 13c id collision that says nothing about criteria |
| `docs/spend-record-behind-its-recorder` head — all sections present | `OK — … 1 closing transitions, 17 criteria checked, 0 unnumbered` |

Row 4 grew a second failure while this stack was open, which is why it names its failures
rather than counting them: `bean:0089` merged, check 13c resolves an introduced id against
`origin/main`, and the id that head introduces now collides with the copy `main` carries. That
is `bean:0051`'s allocation check firing on an allocation its own success has spent, and it is
not about criteria at all.

The four trees named by **branch** rather than by commit are named that way for a reason, and
the history of that choice is the argument for it.

Every squash-merge of a stack member puts a **new** commit on `main`. The branch's own commits
never become ancestors of it, so every branch still based on them is based on something `main`
does not contain, and the merge forces a rebase of everything below it. A stack of N members
costs N−1 rebases **by construction**, not through anyone's mistake: this repository offers no
other kind of merge (`allow_merge_commit: false`, `allow_rebase_merge: false`), so the cascade
is a repository setting, and no discipline on the author's part avoids it.

This table was first written after #51 — row 1 cites `97f13b0`, which #51's squash created —
and it gave shas for the four unmerged heads. **#48's squash is the one that killed the sha
version**: the rebase it forced orphaned all four, and rewriting them as branch names was the
fix. **#52's squash orphaned the two remaining heads and the table survived it unchanged**,
which is the whole return on that one edit. A branch name is invariant under rebase, so no
later squash can require that column to be rewritten — the edit goes on paying without
anything here having to record that it did.

A commit id for an unmerged branch head is rebase-dependent — invalidated by an operation that
changes no content — which is the same class as the line numbers this stack removed from its
own evidence cells. `97f13b0` keeps its sha in row 1 because a squash's own commit lands on
`main`: it is reachable and permanent, and it is the one id in this section that no rebase can
invalidate.

**How this paragraph got here is worth as much as what it says.** Before the last revision it
carried one wrong number: "twice", where three squashes had happened. Adding the enumeration
to fix it left three statements — "twice more", "survived all three", "paid for itself three
times" — disagreeing with each other and with the sentence two lines above them. One low
figure became three mutually inconsistent ones, and it was the *added precision* that exposed
them: the enumeration made checkable a set of claims that had been vague enough to coexist.
**Adding precision to part of a paragraph makes the rest of it falsifiable, so expect the
correction to surface more than it fixed** — and re-read the whole paragraph, not the clause
you came to change.

Then it went wrong the same way a second time: #53 merged, and the enumeration that had said
three was one short again. **An enumeration of squashes is a count over a set that keeps
growing until the stack lands**, so the roll-call is gone rather than corrected a third time.
The mechanism above says what any number of squashes does, and leaves nothing for the next
merge to falsify. #48's squash and #52's are still named, because each carries an argument
about the table rather than a place in a tally: one killed a version of it, the other failed
to.

**Three of the five are masked, and by a different defect than the one this bean is about.**
A numbered table in the evidence region whose column header is outside check 14's accepted
vocabulary trips `NOEVCOL`, and `NOEVCOL` suppresses the whole per-criterion cascade by
design. So on the first three trees the check reports one line about a table header and says
nothing whatever about criteria. The un-numbering that lifts the suppression arrives in the
gates branch, which is why the fourth row is the first that speaks.

**The fourth row is the finding.** The gates head is the one tree where check 14 is *runnable and
misleading*: it names criterion 14, the statement is true of that tree, and it is false of the
work — the evidence answering criterion 14 is written and sitting on the next branch. Nothing
in the output distinguishes "not written" from "written, not merged". A reader learns nothing
from it, and cannot tell that they have learned nothing.

**What the first version of this section claimed, and why it could not be true.** It reported
`97f13b0` yielding seven failures, criteria 5-10 and 14. That tree yields one, and the seven
belong to a different head: at `97f13b0` the unanswered set is **nine** criteria, and the
seven are that set minus the agent-loop section's **two**. The figure was assembled from the
argument rather than read off a run, which is the failure `doc:50-memory-and-evidence` §2.2
names as a figure with no command, committed in the bean raised to record a measurement.

**Neither number is obtainable by running anything committed, and that is why they were
invented.** `NOEVCOL` suppresses the whole per-criterion cascade on both trees, so the
unanswered set can only be read off a linter patched to lift the suppression. A figure that
needs a rig built before it can be observed is exactly the figure that gets reasoned out
instead. The rig, and what it produces:

```
cmd:      grep -rl '!noevcol' tools | xargs sed -i '' 's/ && !noevcol//'
          sed -i '' 's/^status: in-progress$/status: completed/' .beans/modus-0068--encode-sprint-1-findings.md
          bash tools/docs-lint.sh | sed -n 's/.*criterion \([0-9]*\) is not answered.*/\1/p' | paste -sd' ' -
observed: 1 2 5 6 7 8 9 10 14
tree:     97f13b0 — `main` when the base merged; check 14 lived in tools/docs-lint.sh
exit:     0

cmd:      grep -rl '!noevcol' tools | xargs sed -i '' 's/ && !noevcol//'
          sed -i '' 's/^status: in-progress$/status: completed/' .beans/modus-0068--encode-sprint-1-findings.md
          bash tools/docs-lint.sh | sed -n 's/.*criterion \([0-9]*\) is not answered.*/\1/p' | paste -sd' ' -
observed: 5 6 7 8 9 10 14
tree:     89faba2 — the agent-loop head; check 14 had moved into
          tools/lib/docs-lint-c14.awk, which is why the patch greps for its own target
exit:     0
```

`{1, 2}` is the difference: the agent-loop section answers criteria 1 and 2 and no others, so
9 − 2 = 7 is the subtraction the first version described with both numerals wrong by the same
offset, which is what let it balance.

The rig also shows how near the miss was. `89faba2`'s unanswered set **is** the seven that was
reported — what no commit in this stack does is *print* them. On that head `NOEVCOL` still
suppresses the cascade, and the un-numbering that lifts it arrives on the gates branch, where
the unanswered set is already down to criterion 14 alone (row 4). The reported figure named a
tree assembled from parts of two.

**The conclusion is stronger on the real data than on the invented data.** "There is no tree
on which the question is both runnable and meaningful until the last merge" was written
against five trees on which the check was assumed to run. On three of them it does not run at
all, and on the fourth it runs and misleads. Only the last is both runnable and correct.

## The trade, which is not a recommendation

`doc:00-constitution#context-budget` §6.2 requires a work item too large for one agent to be
split. Applied to a single bean, that produces exactly the shape above.

A second influence is worth separating from it, because it is **not** a repository rule: the
preference for several atomic pull requests over one large one. `grep -rn "atomic"
documentation/*.md` returns eighteen hits across four documents — `doc:40-durability`,
`doc:00-constitution`, `doc:05-authoring-for-agents` and `doc:50-memory-and-evidence` — and
every one of them is about atomic **file writes**: `rename` within a filesystem, `O_APPEND`,
`PIPE_BUF`, a memory and its evidence as one unit. None is about pull requests. Its only trace
in the tree is the `atomic:` field in `.github/pull_request_template.md`, which records
whether a change is atomic and does not say to prefer it. As a stated preference it lives in orchestrator briefs
and nowhere else — another convention this sprint found to be brief-resident rather than
written down. This bean does not adopt it as a rule; it names it as what it is. Both resolutions cost something real:

| shape | gains | costs |
|---|---|---|
| one bean, N pull requests | one coherent record of one piece of work; one set of criteria; one place a reader looks | an interval, from the first merge to the last, in which check 14 cannot be run meaningfully on it |
| one bean per pull request | every bean auditable at its own closing change | N beans describing one piece of work, N criteria sets to keep consistent, and the cross-references between them are `bean:` links that resolve only after each merges |

Which is correct is **not settled here**. What is assertable today is that the cost of the
first is undocumented: an author choosing to split a bean across a stack — following the
project's own guidance — has no way to learn they are buying an audit blind spot until they
hit it.

The first version of criterion 1 also cited `doc:05-authoring-for-agents#bean-split` as a
place the trade might be stated. That anchor sits on *Documentation, beans and ADRs*, whose
subject is which of `documentation/`, `.beans/` and `documentation/adr/` owns which question.
The "split" in its name is that three-way split of responsibility, not the splitting of a bean
across pull requests, and nothing in the section concerns sizing. A citation that resolves and
does not carry its claim, in the stack that encodes that shape.

## Its sibling, and what the two mean together

`bean:0096`, raised by the agent that owns `tools/docs-lint.sh`, records that check 14 never
runs on an implementation pull request at all, because `doc:00-constitution#bean-lifecycle`
keeps the bean `in-progress` through its own review.

It is written here as a typed reference, and the earlier refusal to write one was wrong twice
over. `bean:0096` merged as #59 before this branch's base, so it was on `origin/main` the
whole time. And `origin/main` was never the test: check 6 resolves a bean by
`ls .beans/<prefix><id>*.md` over the **working tree** (`tools/docs-lint.sh`), so what governs
is whether the file is in the tree you are linting. Resolution against `origin/main` is
check 13's business, not check 6's, and the two were conflated. A bean on your own branch has
always resolved; only a bean living solely on a *sibling's* branch does not.

Together the two say more than either does alone: **check 14 is meaningful only on the final
closing pull request of a stack, and silent everywhere else in the workflow that produced the
work.** It is not that the check is weak; it is that the workflow gives it one moment to speak,
and that moment is after every decision it might have influenced has been made.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The trade is stated where an author chooses the split — `doc:00-constitution#context-budget` §6.2, which is the only anchor that carries bean sizing — with both costs named and neither presented as the default | citation |
| 2 | A reader of a check 14 failure can tell "not written" from "written, not merged yet", or it is recorded that they cannot and why | citation |
| 3 | The combined statement with 0096 is recorded once, in whichever of the two beans closes last, and not in both | citation |
| 4 | Re-measured at the time of the fix rather than reused from here: the five rows above are a property of one stack at one moment, and this bean has already had to re-derive them once | command |
| 5 | The atomic-pull-request preference is either written into `documentation/` as a rule or left attributed to briefs — not cited as a repository rule while sourced nowhere in it | citation |
