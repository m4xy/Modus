---
# modus-0104
title: A scripted edit produces no reading of its own result
status: todo
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# A scripted edit produces no reading of its own result

Six defects this sprint, across two agents, share one shape: **a script edited a file, the
edit did something other than what was intended, and nobody looked at the result.** Every one
is invisible to every mechanical check this repository has, and every one is obvious within
seconds to a person reading the file.

That inversion is the point. Most of this sprint's findings were mechanisms failing to catch
what a careful reader would also have missed. **These are the opposite: the gate is green, the
document is well-formed, the code compiles — and a reader sees the defect immediately.**

## Not "scripts are bad"

Stated first, because the wrong lesson is the easy one. Two of the six were **the right tool**:
rewriting a whole section, and reverting a plant from a snapshot, are exactly what scripting is
for, and doing either by hand would have been slower and more error-prone.

The mechanism is narrower, and it is not about the correctness of the script. **A hand edit
forces a reading of the result as a side effect of making it** — you cannot type into a file
without seeing the lines around what you typed. **A scripted edit produces no reading of its own
result.** The author reads the *script*, which is a statement of intent, and then the exit
status, which reports only that the script ran. Neither is the file. The gap between "the script
did what I wrote" and "the file now says what I meant" is unobserved by construction.

That is why exit status misleads here. All six scripts exited 0. All six did exactly what they
were written to do.

## The six instances

Five are verified below by direct inspection. The third is recorded on report and marked as
such.

### 1. An anchor matched by the text the script had already inserted

A `str.replace(anchor, new_text + anchor)` where `new_text` itself contained the anchor, run
against `modus-0069` (cited by filename: it is unmerged, and check 6 resolves a typed
reference against this tree). The result was a duplicated H2 on one line:

```
observed: ## Two defects this bean committed, found by running the gate## Two defects this
          bean committed, found by running the gate
```

It survived **every revision two reviewers read**, and `docs-lint` was green on all of them —
nothing in this repository parses heading structure. A reader sees it instantly; it is a doubled
line of bold text.

### 2. A truncation that removed a whole section — the case that argues hardest

`patch65_seq.py` rewrote one section of `bean:0065` with `s[:s.index("## Sequencing")] + new`.
`## Evidence` sat **after** `## Sequencing`, so the slice discarded it:

```
cmd:      git show 777560c:.beans/modus-0065--ambient-capability-ports.md | grep -n "^## "
observed: 236:## Success criteria and evidence
          252:## Sequencing
          272:## Evidence                       <- 272..380, 109 lines
cmd:      git show 8f57a68:.beans/modus-0065--ambient-capability-ports.md | grep -n "^## "
observed: 281:## Success criteria and evidence
          297:## Sequencing                     <- file ends here; ## Evidence is gone
```

The author ran `docsLint` afterwards and it was green.

**Two independent blindnesses compose here, and that is what makes it the worst of the six.**

- The **criteria table survived**, because it sits *above* the cut. Every forward pointer into
  the evidence section remained intact, pointing at nothing. The document still asserts that its
  criteria are evidenced; the evidence is not there. A reader of the criteria table alone sees
  nothing wrong.
- The check that exists to catch exactly this **could not run**. `docs-lint` check 14 examines
  only beans that *close* in the change, and `bean:0065` was `status: in-progress` at both commits above — which fails check 14's scope exactly as `todo` would. `bean:0096` states
  the general invariant — check 14 never examines the bean whose work the pull request contains
  — so a green `docs-lint` line on that pull request said nothing whatever about its evidence.

Neither blindness is a defect on its own. The scripted edit was invisible because nothing reads
a document's structure; check 14 was silent because it is scoped to closures. **Composed, they
delete 109 lines of evidence from a document whose gate reports it well-formed.**

### 3. A revert that restored superseded code

A plant was reverted by `cp` from a snapshot taken **before** an intervening rewrite, silently
restoring the older implementation. The diff looked clean, because the restored code was
perfectly valid code that had been correct when the snapshot was taken.

*Recorded on report from the orchestrator; not independently verified here, and marked rather
than presented as inspected.* The shape is worth keeping regardless: **a snapshot is a claim
about a moment, and `cp` back asserts that nothing since that moment mattered.** Nothing checks
that assertion. What caught it was re-running the behaviour, not reading the diff — which is the
general remedy below.

### 4. A shared scratchpad, which inverts the pattern

An agent's scratch directory is keyed by **session** id, not agent id, so roughly fifty agents
share one directory that every brief describes as private:

```
cmd:      grep -rho "worktrees/agent-[a-z0-9]*" $SCRATCHPAD | sort | uniq -c | sort -rn
observed: 2250 worktrees/agent-aaae830b7d62a3f3d
          1658 worktrees/agent-a0896f9eb8e9d25b7
          [... 50 more distinct agent ids ...]
```

This one **damaged nothing** — worktree isolation held, and every script involved hardcoded its
own worktree root. It still cost a reviewer's attention and nearly sent an author debugging
scripts that were correct.

It belongs here because it sharpens the shared property rather than repeating it: **the only
thing that noticed was a reader who happened to be looking at that exact file.** Had the file
landed in a scratchpad nobody was reading, the collision would have stayed invisible while the
privacy guarantee stayed false. The reviewer's response — flag it, refuse to absorb it, do not
act on it — is the behaviour that made the diagnosis possible, and is worth stating as correct.

The serious near-miss sits underneath it and is not about sharing at all. A bean was backed up
to a **guessable path in shared `/tmp`**, edited, and restored from that backup — and committed.
Had anything clobbered the path in between, a stranger's file would have been committed as
`modus-0090` with nothing to flag it. Verified after the fact that it did not happen. That was
luck, not design.

### 5. A region anchor invalidated by the document it reads

The same shape as #2, found in the tool built to catch this class. A script extracts every
`` `cmd`: `` from a bean's criteria table and runs it, bounding the table with a **named
heading**. Prose added later, above that heading, fell inside the scanned region, and the script
tried to execute an example command quoted in that prose.

Harmless, and instructive: **an anchor chosen from a document's current shape is a claim about
that shape, falsified by editing the document.** #2 is the same error with a destructive slice
instead of a search bound.

### 6. Two cross-references inverted by the sections moved around them

`modus-0103` — the sibling bean, cited by filename because it is unmerged — carried two
internal pointers that both pointed the wrong way:

```
observed: line 146: "The first is used above."      the usage is at 168 and 172, below
          line 183: "see the recording hazard below" that section starts at 122, above
```

Neither is a typed reference, so check 6 cannot see either; nothing mechanical resolves a
prose pointer. And **both inverted for the same reason**: a scripted edit inserted two new
sections in the middle of the document, moving everything after them, and the result was not
read. Each pointer had been correct when written.

This is the sixth instance and the cheapest to state: **a cross-reference by direction is a
claim about layout, and a scripted edit that moves a section falsifies every such claim in the
document at once, silently.** The fix generalises past scripts — name the section rather than
its position — and both pointers are now by name, which survives any reordering.

## Not the same as `bean:0102`

`bean:0102` landed on `main` while this was being written and is adjacent, not overlapping.
It is about a **revert command's scope**: `git checkout -- .beans` reverts every uncommitted
modification under that path, not only the plant, so it destroys the work the plant existed to
prove. The damage is to files the author never asked the command to touch.

This bean is about a **script's result going unread**. The scripts here did exactly what they
were told, to exactly the file they were aimed at. Instance 3 sits closest to `bean:0102` and is
still distinct: a `cp` from a stale snapshot restores *superseded but valid* content to its own
target, rather than destroying a bystander's.

The two do compose, and the composition is worse than either: a revert with a wider scope than
intended, whose result nobody reads, leaves no trace at all.

## The remedy, and what it actually costs

**"Read the file after a scripted edit" is trivially stated and genuinely expensive**, which is
why nobody does it. On a 500-line bean it is a full re-read for a three-line change, every time,
and an agent that does it will run out of context before it runs out of edits. Any remedy that
ignores that cost will be ignored in turn.

Four cheaper forms. The two adopted below were **measured against instances 1 and 2 rather
than reasoned about**, because an earlier revision of this table adopted a form that catches
neither and credited it with both.

| form | catches | cost | verdict |
|---|---|---|---|
| Read the **diff hunks either side of the edit boundary** | #1, #2, #5 | seconds; the diff is already produced | **Adopt.** The strongest of the four |
| Compare the **heading sequence** — `grep -o "^## "` either side, diffed | #1, #2 | one command each side | **Adopt.** Catches the two worst |
| **Re-run the behaviour**, not the diff | #3 | a test run | **Adopt where behaviour changed.** The only thing that caught #3 |
| Compare the **heading count** — `grep -c "^## "` either side | **nothing** | one command each side | **Rejected — measured, not guessed.** See below |
| Read the **whole file**, rendered | everything | a full re-read per edit | **Reject as a routine rule.** Correct and unaffordable; a final pass before a pull request only |

**The heading-count form catches 0 of 2, and this bean credited it with both.**

```
cmd:      git show 777560c:.beans/modus-0065…md | grep -c "^## "     # before instance 2
observed: 10
cmd:      git show 8f57a68:.beans/modus-0065…md | grep -c "^## "     # after
observed: 10
```

Unchanged across a 109-line deletion, because the same edit removed `## Evidence` and added
`## Why this port is in core-domain…`. One out, one in, net zero: **the invariant reports the
file healthy at the exact moment it is at its least healthy.** On instance 1 it reads 7 then 8,
which is precisely the delta that edit intended — an author checking it sees +1, matches intent,
and concludes it held. It could not have caught that duplicate in any case, because the doubled
heading shares a line with its original and `grep -c` counts lines:

```
cmd:      git show e8fba3a:.beans/modus-0069…md | grep -o "^## Two defects.*"
observed: ## Two defects this bean committed, found by running the gate## Two defects this
          bean committed, found by running the gate
```

**A count is a lossy projection, and the loss is exactly where the defects live.** Both are
compositions — a removal plus an addition, and a heading plus a heading — and summing destroys
composition by construction. The sequence survives it:

```
cmd:      diff <(git show 777560c:…| grep -o "^## .*") <(git show 8f57a68:…| grep -o "^## .*")
observed: 5a6
          > ## Why this port is in `core-domain` and `bean:0066`'s is in `core-application`
          10d10
          < ## Evidence
```

And the seam hunk shows it as a single pure deletion, unmissable:

```
cmd:      git diff -U0 777560c 8f57a68 -- .beans/modus-0065…md
observed: @@ -272,109 +338,0 @@
```

**The uncomfortable part is that the right command was already in this bean.** Every
`observed:` block above is a `grep -n "^## "` sequence — that is how instance 2 was diagnosed
at all. The remedy section then wrote up a *count*. So this bean ran the right command, drew
the right conclusion, and recommended a different and weaker one — which is its own mechanism
at one remove: the write-up was not checked against the work it describes.

**What cannot exist is a repository-wide gate**, and that is the sharper form of the earlier
"no gate" refusal rather than a change of position. An assertion taken **either side of one
edit** needs no model of intent, because the person running it supplies the intent — that is
why the two adopted forms work. A gate in `qualityCheck` sees only the merged result, with no
before-state and no author, so it cannot distinguish a deliberate section removal from an
accidental one. The refusal is about scope, not about mechanisation.

Check 14 cannot be extended to cover instance 2 either: `bean:0065` is shape A — one table with
an evidence column — so deleting a separate `## Evidence` section breaks no invariant it knows
about, and the forward pointers into it are the prose word "below".

### One mechanism that did work, as precedent

The extractor in #5, despite its own bug, caught two defects **that reading would not have** — a
`grep` alternation which a Markdown table cell escapes to `\|`, silently turning one intended
match into eleven, and phrases wrapping across two lines so their greps can never match. Both
are invisible to a careful reader *by construction*, because the fault is in how the cell
renders the command rather than in the command's logic.

That is the shape worth generalising: **where a document makes a claim that can be executed,
execute it.** It converts an unfalsifiable assertion about diligence into a check. The script is
32 lines, general to any bean using the `cmd:`/`observed:` convention
(`adr:0005-evidence-lives-in-the-work-item`), and is **offered rather than installed** — placing
it in `tools/` and wiring it into `qualityCheck` is a review cycle this bean does not open. It currently sits at `/tmp/a09c2c894e8ac7eea-runcells.py`, which is **not a home** — a
guessable path in shared `/tmp` is the near-miss this bean records two sections above, and
leaving a deliverable there would be the same mistake with the write-up attached. Criterion 2
is its home or its deletion; until one or the other, it is a draft and not an artefact.

## Success criteria and evidence

Numbered and tabled rather than bulleted, because `docs-lint` check 14b answers **per numbered
criterion**: against unnumbered bullets it collapses to one aggregate judgement, so a single
prose paragraph would satisfy the whole bean at closure. Verified on the sibling bean by
flipping it to `completed` — numbered, it reports one failure per criterion; unnumbered it
reported one for the lot.

Evidence is empty by design while this is `todo`: these criteria describe follow-up work that
has not been done, and a cell filled now would be a plan rather than an observation
(`adr:0005-evidence-lives-in-the-work-item`).

| # | criterion | evidence |
|---|---|---|
| 1 | The three adopted forms — read the seam hunks, compare the heading **sequence**, re-run the behaviour where behaviour changed — are written wherever agent editing practice is owned, as a habit with its cost stated. Not a repository-wide gate, for the reason given above. The rejected heading-**count** form is recorded with it, so it is not re-adopted by someone reasoning about it rather than measuring it | |
| 2 | The offered extractor is either placed in `tools/` with a home in the gate, or this bean records why it was not | |
| 3 | If any mechanical check is proposed, it is **observed failing** against a planted truncation and a planted duplicated heading before it is claimed (`doc:00-constitution#observed-failing`), and observed **not** firing on a deliberate section removal | |

## On generality

Six instances, two agents, one sprint, five verified by inspection. The mechanism is not
disputed by any of them, and the sample is small and local: all six come from Markdown and
TypeScript edits in one repository by two agents working the same way. Whether it generalises
past that is untested, and the remedy is pitched at the strength the evidence supports — a habit
with a cheap form, not a rule with a count behind it.
