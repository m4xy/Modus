---
# modus-0093
title: Pasted check 14 output standing in top-level prose answers the criterion it reports unanswered
status: todo
type: fix
priority: high
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0061, modus-0063]
---

# Pasted check 14 output standing in top-level prose answers the criterion it reports unanswered

A transcript quoting a fence marker an **even** number of times balances. The markers pair
off, the segment between the second and the third is top-level prose, and check 14's citation
matcher reads it. When that segment is this check's own output — the natural thing to paste
when documenting the check — the lines reading `criterion N is not answered in the evidence`
set `A[N]` and the criterion closes as answered.

## This is not a fence bug, and sending it to the fence code would be wrong

`bean:0063` fixed two escapes that look like this one and are not. Both were the analyser
**disagreeing with the renderer**: a marker that is content inverted the fence state, and a
block quote or an indented chunk rendered as code while a line-oriented reader saw prose. In
each case there was a perception divergence, and closing it was a matter of making the
analyser see what a reviewer sees.

**Here there is no perception divergence to close.** The analyser, every Markdown renderer,
GitHub's preview and a human reading the bean all agree: those lines are top-level prose. The
fence tracking is correct. The classifier is correct. What is wrong is the **citation rule** —
that any line outside a fence bearing `criterion N` answers criterion N. It admits a line
whose content is a machine-generated statement that the criterion is *not* answered.

So the fix is in `tools/lib/docs-lint-c14.awk`'s citation matcher and in the rule
`doc:05-authoring-for-agents#checks` states, not in `tools/lib/docs-lint-fence.awk`.
Widening, narrowing or refusing anything about fences cannot reach it. An agent that opens
the fence file to fix this has been sent to the wrong mechanism.

## Observed

Planted on `.beans/modus-0033`, a `status: todo` bean, by flipping its status to `completed`
and appending the shape, then reverted with `git checkout -- .beans`; `git status
--porcelain` is empty after each run. The control and the plant differ only in the two quoted
markers, which change nothing about what a reader sees rendered: both render as a code block,
a line of prose, and a code block.

```
control:  `## Success criteria` numbered 1 and 2; `## Evidence` holding ONE fence whose
          body is `cmd:` and the two `criterion N is not answered` lines
observed: FAIL check 14 .beans/modus-0033-…: criterion 1 is not answered in the evidence;
          no evidence row bears its number and nothing cites it (adr:0005-…)
          FAIL check 14 .beans/modus-0033-…: criterion 2 is not answered in the evidence;
          no evidence row bears its number and nothing cites it (adr:0005-…)
          docs-lint: 2 failure(s).
exit:     1

planted:  the same transcript quoting a fence marker TWICE, so the two `criterion N is not
          answered` lines fall between the pair and stand in top-level prose
observed: docs-lint: OK — 19 documents, 106 anchors, 914 references, 64 beans, 28 graph
          edges, 18 selectable, 64 bean ids, 0 introduced, 64 on origin/main, 1 closing
          transitions, 2 criteria checked, 0 unnumbered.
exit:     0
```

`2 criteria checked` and exit 0. Both criteria are answered by pasted output stating that
they are unanswered.

The plant was then run a third time against `bean:0063`'s analyser, to establish that this
does not wait on that work and is not closed by it:

```
cmd:      the identical plant, with tools/ taken from the bean:0063 branch
observed: docs-lint: OK — … 1 closing transitions, 2 criteria checked, 0 unnumbered.
exit:     0
```

Unchanged, as expected: that branch's CommonMark classifier reads the segment as prose for
the same reason every renderer does. It is asserted there as a pinned open defect
(`tools/docs-lint-test.sh`, "DEFECT (open): an EVEN number of quoted markers still answers
the criterion"), so the day the behaviour changes, that suite says so.

## How it was found, and why that matters more than the bug

It was shipped as a **residual** — a divergence recorded as acceptable — and it survived a
review round with that label on it. It became a defect the moment a **verdict** assertion was
demanded of it rather than a perception assertion: the verdict showed the outcome changes.

That is the rule catching a case which had already been read and accepted by a reviewer:

> Every residual needs a verdict assertion showing the divergence does not change the
> outcome. When it does change the outcome it is not a residual, it is a defect.

Two of `bean:0063`'s three residuals died the same way in the same round, and one of those
was a regression. Three for three. A residual asserted only at the parse layer is a claim
about acceptability that was never tested, and the label is what stops anyone looking — the
same shape `doc:00-constitution#observed-failing` records for an unfalsifiable gate.

## Relationship to `bean:0061`

`bean:0061` owns the citation matcher and already records that it cannot tell a citation from
a mention: prose *about* criterion numbers is read as citing them, and its own text
suppressed three of its own failures. Its fifth criterion reads

> A prose mention of a criterion number that is not a citation no longer answers it, **or the
> looseness is stated as accepted**

— and that second clause is why this bean exists as well as that one. **This is the case where
accepting the looseness is not available.** A benign mention that answers a criterion is a
loose check; a pasted `criterion N is not answered` that answers criterion N is a check
accepting its own failure output as evidence, which is the defect class
`doc:00-constitution#observed-failing` is written against. Whichever option `bean:0061`
takes, it must reject this shape, and if it closes by accepting the looseness this bean is
what remains open.

`blocked_by: [modus-0061]` for that reason: what a citation *is* is that bean's decision, and
fixing this first would pre-empt it with a narrower rule chosen against one example.

## The boundary, and the two instances observed in the wild

This bean hands **benign mentions** — prose about a criterion number that was never meant to
answer it — back to `bean:0061`. That hand-off has a gap, and the gap is reachable:
`bean:0061`'s fifth criterion may be closed by *stating the looseness as accepted*, and if it
is, while this bean adopts a narrow option aimed only at pasted output, then the shape that
actually occurs in this repository is owned by nobody.

It does occur. Two instances, neither planted, both found by running the check rather than by
reading:

```
cmd:      grep the completed corpus for a criterion citation standing in top-level prose
observed: .beans/modus-0028--normative-gate-commands.md:90
            "…opened — so criterion 1 was false when written. Criterion 7 was added to
             make the sweep…"
          .beans/modus-0035--beans-graph-check.md:98
            "The three trailing counts are criterion 6. They exist because docs-lint
             check 11 shipped…"
exit:     0
```

`modus-0028`'s is the sharper of the two and it is worse than a benign mention: the sentence
says the criterion **was false when written**, and the matcher reads it as answering that
criterion. A completed bean closed a criterion on a sentence declaring that criterion false.

A third is on `origin/main` and is not in this branch's base, which is why it is cited by
commit rather than by path: `origin/main:.beans/modus-0058--unwritten-working-conventions.md`
line 183 reads "`wc -l` at the two commits, which is what criterion 6 reads:". It arrived
after this branch was cut and was verified against `origin/main` directly. It has a property
the others do not: **it was introduced by an author correcting a review finding.** The act of
responding to review is what closed the criterion.

A fourth was found on an unmerged bean this sprint — a sentence naming a criterion by number
while describing what that criterion decides — and a fifth is recorded in `bean:0061`, which
was caught the same way while documenting the defect. Five instances, none planted, three by
authors who already knew about the defect.

**So the repair is stated here rather than left to the hand-off.** Whatever this bean adopts
MUST also reject the two shapes above; they are named in the criteria below. If `bean:0061`
closes by accepting the looseness, this bean does not inherit that acceptance.

## Options

| option | catches | cost |
|---|---|---|
| a citation answers only from a **structural** site — a `### Criterion N` heading, or a table row whose first cell is `N` — never from a line of running prose | this, and `bean:0061`'s mention problem, with one rule | **measured, not estimated: two of the 23 `completed` beans change.** See `## Option 1's cost, measured` |
| a line that both cites a criterion and states a check verdict about it does not answer | this shape exactly | a blocklist of output patterns; fails on the first message nobody thought of, and teaches authors which strings to avoid. The open bean on evidence-cell strength rejects the same shape for the same reason |
| require the citing line to be inside the `## Evidence` region and outside any transcript-shaped run of lines | narrows the window | "transcript-shaped" is a guess about content, which is what the option above fails on |
| leave it, and disclose on the `OK` line | nothing, but stops it being silent | the parity is no longer visible after `bean:0063` makes an odd count an error, so there is nothing left to disclose |

## Option 1's cost, measured

**A cost written into an options table is a claim, and it should be measured before it is
written.** That is a new rule, stated here and attributed to nobody. An earlier version of
this section cited `doc:00-constitution#observed-failing` for it, and that citation was
wrong: §9.1 binds `Enforced by:` lines and the gates they name, and says nothing about the
cost column of an options table. The principle is sound and the authority was invented, which
is the shape this repository is least willing to accept from a bean.

It needs encoding somewhere real rather than asserting here. `doc:00-constitution` is at
500/500 lines and cannot take it, so it is routed to `modus-0089` — named by filename because
it is unmerged, and a typed reference to an unmerged bean fails check 6 — as a third instance
of a rule that could not land because of that ceiling.

The argument for the rule: an unmeasured "several beans would break" deters a cheap fix
indefinitely, and the deterrence is invisible, so nobody ever discovers the claim was wrong.
An overstated cost is not the safe direction to be wrong in.

Measured rather than guessed. `bean:0063`'s analyser was run over the 23 `status: completed`
beans twice — once as it stands, once with `citation_site()` additionally requiring the
citing line to be a heading or a table row — and the verdict sets diffed per file.

```
cmd:      the check 14 analyser over every `status: completed` bean, as-is and with
          citations restricted to structural sites, diffed per file
observed: CHANGED .beans/modus-0028--normative-gate-commands.md
              > UNANSWERED 1
              > UNANSWERED 7
          CHANGED .beans/modus-0035--beans-graph-check.md
              > UNANSWERED 1 .. UNANSWERED 6
          (no other file differs)
exit:     0
```

Two files of 23. `modus-0028` is already flagged today and gains detail rather than changing
state; `modus-0035` moves clean to flagged and is the one real new finding — a bean whose
criteria are cited only from running prose. Corpus totals would go `clean=16 flagged=7` to
`clean=15 flagged=8`.

The instrument was validated against a known positive before the result was believed: a
fixture answering its one numbered criterion only from a running-prose line reports nothing as-is and
`UNANSWERED 1` under the restriction. So two is a measurement, not a script matching nothing.

Both changed beans are frozen by check 11, so the grandfathering question raised by the fourth
criterion below is the one check 14 already answers — judge what closes in the change, leave the frozen corpus alone —
and neither needs an amendment for this change to be possible.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The plant above is observed rejected, not merely no longer accepted | planted violation, reverted |
| 2 | The control above still fails for its own reason, and a criterion cited from a **structural** site — a sub-heading naming it, or an evidence row bearing its number — still closes | planted violation, reverted |
| 3 | Whatever is adopted names a property of where a citation may stand, not a set of rejected strings, and rejects the two wild instances named above as well as the planted one | diff |
| 4 | The 23 beans `completed` on `main` and the beans in flight are measured before and after, and every bean whose answered-set changes is named | analyser run over the corpus, before and after |
| 5 | The pinned assertion in `tools/docs-lint-test.sh` is flipped from DEFECT to a rejection, with a verdict assertion and not only a perception one | test-run |
| 6 | `doc:05-authoring-for-agents#checks` states the citation rule that results | diff |
| 7 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- Fence tracking (`bean:0063`). There is no perception divergence here and that code is not
  the mechanism; the second section of this bean exists to keep the next agent out of it.
- Check 6's copy of the old fence toggle, which is raised separately and unmerged.
- Whether an evidence cell's *contents* are evidence at all, which is raised separately and
  unmerged. This bean is about where a citation may stand, not what a cell must hold.
- The numbering gate itself (`bean:0061`), and the three escape routes it records.
