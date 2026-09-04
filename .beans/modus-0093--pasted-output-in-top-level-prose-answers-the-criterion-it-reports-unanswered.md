---
# modus-0093
title: Pasted check 14 output standing in top-level prose answers the criterion it reports unanswered
status: in-progress
type: fix
priority: high
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0063]
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

`blocked_by: [modus-0061]` was carried for that reason: what a citation *is* is that bean's
decision, and fixing this first would pre-empt it with a narrower rule chosen against one
example.

### The edge is dropped, and the paragraph above is why

The rest of this bean falsifies its own premise, and the reversal is recorded here rather
than made silently. Three things had already happened by the time the edge was written down:

- The section above rules that *"accepting the looseness is not available"* for this shape.
  So `bean:0061`'s fifth criterion is not a decision with two outcomes any more — this bean
  fixed one of them shut. An edge whose downstream constrains its upstream to a single
  answer is a citation, not a dependency.
- The option was **measured**, twice, over the whole `completed` corpus, and stated as a
  property rather than as a rule chosen against one example. That is the specific harm the
  edge was written to prevent, and it did not happen.
- Option 1's own cost row says it catches *"this, and `bean:0061`'s mention problem, with
  one rule"*. Landing it first therefore makes `bean:0061` cheaper — its fifth criterion
  arrives already discharged — and blocking it makes that bean more expensive, not less.

The edge is **dropped, not inverted.** `bean:0061`'s subject is the three un-numbering
escapes and the absent-criteria case; none of them depends on where a citation may stand, so
an inverted edge would block that bean's real work behind this one for the sake of a
criterion this change answers on its way past. It stays selectable and gains a note.

`blocked_by: [modus-0063]` remains and is satisfied — that bean is `completed`.

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

`modus-0028`'s is the sharpest of them and it is worse than a benign mention. Verified by
applying check 14's own matcher to that file's non-fenced lines, which returns exactly one
line — and that one line closes **two** criteria:

```
cmd:      awk over .beans/modus-0028, skipping fenced lines, applying check 14's matcher
observed: 90: opened — so criterion 1 was false when written. Criterion 7 was added to make
              the sweep exhaustive rather than sampled.
          (no other line matches)
exit:     0
```

A `completed` bean on `main` closes its first criterion on a sentence stating that criterion
was false when written, and closes its seventh on the sentence explaining why that one had to
be added at all. That single line is why the structural-site measurement above shows
`modus-0028` gaining exactly two unanswered criteria and nothing else. Not planted, and live
today.

### The defect in one sentence

> **The citation matcher reads presence of a number, never the polarity of the claim around
> it.**

That is the whole of it, and it explains every instance at once — pasted failure output, a
benign mention, and a sentence asserting the thing it names was false. It is a better
formulation than "a mention answers a criterion" because it names what the mechanism *does*
rather than what it fails to do, and it makes the fix direction obvious: **no amount of
context-widening helps a matcher that never reads polarity.** A matcher given more
surrounding text still has no notion of negation, of past tense, or of a sentence being
*about* a numbered thing rather than answering it. That rules out the entire family of "read
more of the line" repairs, and leaves the structural-site option, which never needs to read
polarity because it never reads running prose at all.

A third is on `origin/main` and is not in this branch's base, which is why it is cited by
commit rather than by path. Verified against `origin/main` directly:

```
file:     origin/main:.beans/modus-0058--unwritten-working-conventions.md
line 183: `wc -l` at the two commits, which is what criterion 6 reads:
```

It has a property the others do not: **it was introduced by an author correcting a review
finding.** The act of responding to review is what closed the criterion it names.

A fourth is in `bean:0087`, now on `main` and citable rather than described — a sentence
naming a criterion by number while describing what that criterion decides — and a fifth is
recorded in `bean:0061`, which
was caught the same way while documenting the defect. Five instances, none planted, three by
authors who already knew about the defect.

**So the repair is stated here rather than left to the hand-off.** Whatever this bean adopts
MUST also reject the two shapes above; they are named in the criteria below. If `bean:0061`
closes by accepting the looseness, this bean does not inherit that acceptance.

## A further class: containers the rule does not model

The five instances above are citations standing in ordinary prose. There is a second class
reaching the same matcher from a container that **renders as code, as a container, or as
nothing** while its content sits at column zero with no fence marker and no `>`. Found by
review attacking `bean:0063`'s citation-site requirement, verified here, and pre-existing on
`main` in every case:

```
cmd:      the check 14 analyser over five fixtures, each a closing bean whose only evidence
          is the check's own failure message inside one container
observed: <pre>                          STATS 2 0   — both criteria answered, exit 0
          <!-- an HTML comment -->       STATS 1 0   — renders as NOTHING and still answers
          <details> wrapping a <pre>     STATS 1 0
          a line-initial ```json```      STATS 1 0
          a line-initial ```sh -c `date` STATS 1 0
exit:     0 for all five
```

`<pre>` is the sharpest: GitHub's sanitiser allows it and it renders **exactly** as a code
block, so a reader sees a transcript and the analyser sees prose. The HTML comment is the
strangest: it renders as nothing at all, and a criterion is closed by text no reader can see.
The reviewer also reached the same matcher through a link-reference definition, a footnote
definition and a table cell.

**This class is why the rule must be stated positively, and it is now.**
`doc:05-authoring-for-agents#checks` says a citation answers only from top-level Markdown
prose, inside no container of any kind. An enumeration of excluded containers is an allowlist
and fails on the first one nobody named — which is exactly how the three-container
enumeration was got past. Whatever this bean adopts MUST reject this class by construction
rather than by extending a list, and the third criterion below says so.

That bullet's `Enforcement gap:` line **names this bean**. It named `bean:0061` while this
one was unmerged, because a typed reference to an unmerged bean fails check 6; the obligation
to move it once this landed was recorded on two pull-request bodies and has been discharged.
The line now names both, with their roles: this bean closes the gap, and it is `blocked_by`
`bean:0061`, which decides what a citation is at all.

The five container shapes are pinned as verdict assertions in `tools/docs-lint-test.sh`, on
`main` since `bean:0063` merged, so the day any of them changes that suite says so.

## Options

| option | catches | cost |
|---|---|---|
| a citation answers only from a **structural** site — a `### Criterion N` heading, or a table row whose first cell is `N` — never from a line of running prose | this, and `bean:0061`'s mention problem, with one rule | **measured, not estimated: two `completed` beans change, re-measured at 27 and still the same two.** See `## Option 1's cost, measured` |
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

Measured rather than guessed. `bean:0063`'s analyser was run over every `status: completed`
bean twice — once as it stands, once with `citation_site()` additionally requiring the
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

Two files. `modus-0028` is already flagged today and gains detail rather than changing
state; `modus-0035` moves clean to flagged and is the one real new finding — a bean whose
criteria are cited only from running prose.

**Re-measured on `main` after the corpus grew, because a cost claim is bound by
`doc:00-constitution#observed-failing` like any other.** At 23 completed beans the answer was
two files; at 27 it is the same two files, with the same criterion numbers — `modus-0028`
gaining two, `modus-0035` gaining six. Only the denominator moved, and the conclusion is
unchanged: corpus totals would go from `clean=20 flagged=7 total=27` to
`clean=19 flagged=8 total=27`.

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
| 3 | Whatever is adopted names a property of where a citation may stand — not a set of rejected strings and not an enumeration of excluded containers — and rejects the wild instances above, the planted one, and the container class by construction rather than by being extended to cover it | diff |
| 4 | **Every** bean `completed` on `main` at the time of the fix, and the beans in flight, are measured before and after, and every bean whose answered-set changes is named. **Stated without a count deliberately, because a claim quantified over a growing set is stale on arrival while a claim about the set itself is not.** An earlier version of this cell said 23; the corpus reached 27 while the bean sat, and a criterion is a forward obligation, so a stale denominator sends its agent to measure the wrong corpus. Measured here rather than asserted: the `N references` figure on the `docs-lint` `OK` line is quoted at **28 distinct values** across `.beans/` and `documentation/`, none of them the current 1097 and none wrong when it was taken. Reported and not verifiable from this branch: `modus-0089` dropped a line count for the same reason, and the e2e spec's *"in every commit that ever touched"* is said to be the only one of four sites that has not rotted | analyser run over the corpus, before and after |
| 5 | **Every** `DEFECT (open)` assertion in `tools/docs-lint-test.sh` is flipped to a rejection, each with a verdict assertion and not only a perception one — there are six, and they are one class: pasted output at column zero answering its criterion | test-run |
| 6 | `doc:05-authoring-for-agents#checks` states the citation rule that results | diff |
| 7 | `./gradlew qualityCheck` green | test-run |

## Evidence

Branch `fix/0093-citation-structural-site`, based on `main` at `1c19cf0`. Every figure below
was redirected to a file and pasted from it. Two heads are in play and each run says which it
was taken at, because the corpus measurement must be taken on a tree this bean has not yet
edited:

- **`1c19cf0`** — the merge base, `.beans/` and `tools/` unmodified. The corpus differential.
- **`1c19cf0`+`tools/`** — the merge base with only this change's `tools/` edits applied and
  `.beans/` unmodified, so that `git checkout -- .beans` after a plant restores the base.
  Every planted run.
- **the branch head** — everything, including this section. The re-measurement at the end.

### Criterion 1 — the plant is observed rejected

Planted on `.beans/modus-0033`, a `status: todo` bean, by flipping its status to `completed`
and appending a numbered criteria table and an `## Evidence` section holding two fences with
the check's own failure lines standing between them, at column zero. Reverted with
`git checkout -- .beans`; `git status --porcelain` was empty before every run and showed only
this change's three modified files after each.

First against the analyser as it stands on `main`, at `1c19cf0`+`tools/` with
`tools/lib/docs-lint-c14.awk` restored to its base content — the defect, reproduced here
rather than quoted from the section above:

```
cmd:      /bin/bash tools/docs-lint.sh
observed: docs-lint: OK — 19 documents, 111 anchors, 1552 references, 102 beans, 37 graph edges, 45 selectable, 102 bean ids, 0 introduced, 102 on origin/main, 1 closing transitions, 2 criteria checked, 4 unnumbered.
exit:     0
```

`2 criteria checked` and exit 0: both criteria answered by the two lines reporting them
unanswered. Then the identical plant with the narrowed `citation_site()`:

```
cmd:      /bin/bash tools/docs-lint.sh
observed: FAIL check 14 .beans/modus-0033--baseline-writer-erases-regression-provenance.md: criterion 1 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0033--baseline-writer-erases-regression-provenance.md: criterion 2 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          docs-lint: 2 failure(s).
exit:     1
```

The two runs differ in one file and in nothing else. `4 unnumbered` in the first is
`.beans/modus-0033`'s own four bullet criteria, which the plant leaves in place; the plant
adds the numbered table beneath them.

### Criterion 2 — the control still fails for its own reason, and a structural citation still closes

The same bean, the same two lines, moved inside one fence — the original control, which must
still fail because a fence is an entry and not a citation site. At `1c19cf0`+`tools/`:

```
cmd:      /bin/bash tools/docs-lint.sh
observed: FAIL check 14 .beans/modus-0033--baseline-writer-erases-regression-provenance.md: criterion 1 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0033--baseline-writer-erases-regression-provenance.md: criterion 2 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          docs-lint: 2 failure(s).
exit:     1
```

And the negative control that makes the two rejections above mean something — the same bean
with its evidence filed under `### Criterion 1` and `### Criterion 2` sub-headings, which is
the shape the new rule asks for:

```
cmd:      /bin/bash tools/docs-lint.sh
observed: docs-lint: OK — 19 documents, 111 anchors, 1552 references, 102 beans, 37 graph edges, 45 selectable, 102 bean ids, 0 introduced, 102 on origin/main, 1 closing transitions, 2 criteria checked, 4 unnumbered.
exit:     0
```

Without this run the two above would also be produced by a check that answers nothing at all.
The same control exists in `tools/docs-lint-test.sh` for both structural sites, and the
mutation table under criterion 5 measures what it is worth.

### Criterion 3 — a property, not a list

`tools/lib/docs-lint-c14.awk`, before:

```
function citation_site(line,   lead, body) {
  lead = fence_lead(line)
  if (fence_cols(line, lead) >= 4) { return 0 }
  body = substr(line, lead + 1)
  if (substr(body, 1, 1) == ">") { return 0 }
  return 1
}
```

after:

```
function citation_site(line) {
  return (line ~ /^#+ / || (intable && line ~ /^\|/))
}
```

The before accepts every line and subtracts two containers by name. The after accepts two
sites and names no container at all, so the escapes are refused without being enumerated: the
raw `<pre>`, the HTML comment, the `<details>` wrapper, the lazy block-quote continuation, the
list item, the front matter and the pasted transcript at column zero are all simply not
headings and not table rows. `intable` is the analyser's own table state, which is why a
`|`-leading line inside a raw HTML block is not a table row either — the plain
`line ~ /^\|/` form was measured too and gives byte-identical verdicts over all 102 beans, so
the stricter form was taken at no cost.

The rule as adopted is stated at `doc:05-authoring-for-agents#checks`; see criterion 6.

### Criterion 4 — every bean measured before and after, and every changed bean named

The check 14 analyser run over every bean file twice at `1c19cf0`, once as-is and once with
the narrowed `citation_site()`, and the verdict sets diffed per file. Named, not counted.

Over the `status: completed` corpus — the beans check 11 has frozen:

```
cmd:      the check 14 analyser over every `status: completed` bean, before and after,
          diffed per file
observed: CHANGED modus-0028--normative-gate-commands.md
              >  UNANSWERED	1
              >  UNANSWERED	7
          CHANGED modus-0035--beans-graph-check.md
              >  UNANSWERED	1
              >  UNANSWERED	2
              >  UNANSWERED	3
              >  UNANSWERED	4
              >  UNANSWERED	5
              >  UNANSWERED	6
          (no other file differs)
exit:     0
```

Two files, the same two the section above measured at 23 completed beans and re-measured at
27. The corpus is now 102 beans of which 35 are `completed`, and the answer is unchanged.
Neither bean fails: check 11 freezes both and check 14 never re-reads a bean the merge base
already closed.

Over **all** beans, which is the forward cost — a bean in flight that closes after this lands
must file its citations structurally:

```
cmd:      the same differential over all 102 bean files
observed: CHANGED modus-0028--normative-gate-commands.md          >  UNANSWERED 1, 7
          CHANGED modus-0035--beans-graph-check.md                >  UNANSWERED 1..6
          CHANGED modus-0049--bash-32-claim-is-unenforced.md      >  UNANSWERED 1, 3
          CHANGED modus-0061--check-14-is-gated-on-numbered-criteria.md
                                                                  >  UNANSWERED 1, 2, 3
          CHANGED modus-0062--docs-lint-does-not-scan-claude-skills.md
                                                                  >  UNANSWERED 1, 2
          CHANGED modus-0086--check-6-resolves-references-through-a-naive-fence-toggle.md
                                                                  >  UNANSWERED 1, 4
          CHANGED modus-0091--transcript-discipline-in-evidence.md
                                                                  >  UNANSWERED 3, 6
          CHANGED modus-0098--pull-request-bodies-restate-evidence.md
                                                                  >  UNANSWERED 1
          CHANGED modus-0101--a-stacked-bean-is-unauditable-until-the-stack-lands.md
                                                                  >  UNANSWERED 1, 5
          CHANGED modus-0118--docs-lint-reports-ok-through-almost-every-runtime-failure.md
                                                                  >  UNANSWERED 2, 3
          CHANGED modus-0119--spend-records-carry-no-seq-kind-or-crc.md
                                                                  >  UNANSWERED 1, 2, 3, 5, 6, 7, 9
          (no other file differs)
exit:     0
```

The `UNANSWERED N, M` form on the right is this bean's transcription of one `UNANSWERED` line
per number; the per-file diff prints them one to a line, as the `completed` block above shows
in full. No other elision.

**`.beans/modus-0049` is the one that needs acting on and it is not this bean's to act on.**
It is `status: in-progress` and is being closed on a parallel branch. If that branch closes it
after this merges, check 14 will report its first and third criteria unanswered, because both
are cited only from running prose. Its second is not affected: it is headed
`## Criterion 2 cannot be met as written`, and a heading is a structural site. The fix is one
edit — its evidence sub-headings read `### 1 — …` and `### 3 — …`, and naming the criterion in
them (`### Criterion 1 — …`) answers them. Recorded here rather than done here, because that
bean belongs to another branch.

The instrument was validated against a known positive before the result was believed: a
fixture answering its one numbered criterion only from a running-prose line reports nothing
before and `UNANSWERED 1` after. It is `tools/docs-lint-test.sh`'s "the planted defect: pasted
output at top level cannot answer its criterion", and it fails if the differential is a script
matching nothing.

### Criterion 5 — every `DEFECT (open)` assertion flipped, as a verdict

All six are now rejections, and each is a verdict assertion:

```
cmd:      /usr/bin/grep -c 'decides "DEFECT' tools/docs-lint-test.sh
observed: 0
exit:     1
```

`grep` here is `/usr/bin/grep`, the BSD grep 2.6.0-FreeBSD that CI also runs, named because
the interactive shell's `grep` is a harness-installed `ugrep 7.8.4`. The pattern is anchored
on `decides "DEFECT` and not on `DEFECT (open)`: the latter still matches once, in the comment
that records what those six pins used to assert, and a count that included it would be
measuring the wrong thing. `exit: 1` is `grep -c` finding nothing, which is the result.

```
cmd:      /bin/bash tools/docs-lint-test.sh
observed: [...]
          ok   verdict: an EVEN number of quoted markers no longer answers the criterion
          ok   verdict: a raw HTML <pre> block no longer answers its criteria
          ok   verdict: an HTML comment renders as nothing and no longer answers
          ok   verdict: <details> wrapping a <pre> does not answer either
          ok   verdict: a line-initial inline code span leaves the next line unable to answer
          ok   verdict: a backtick in the info string does the same
          [...]
          docs-lint-test: 43 passed, 0 failed.
exit:     0
```

The suite gained a third assertion layer, and the reason is `doc:00-constitution#observed-failing`'s
negative half. The old suite could not distinguish this narrowing from **deleting the citation
scanner outright**: both produced the same failure set, because its only assertion that
required something to BE answered was a line of top-level prose, which is exactly what the
narrowing refuses. So a `citation_site()` probe was added beside the fence probe, asserting
the function's own answer per line, and two negative controls replaced the one that was
inverted. Re-measured, at the 43-assertion suite:

```
cmd:      each mutation applied to a copy of tools/, then /bin/bash tools/docs-lint-test.sh
observed: none                       rc=0  docs-lint-test: 43 passed, 0 failed.
          classifier                 rc=1  docs-lint-test: 31 passed, 12 failed.
          citation-site-off          rc=1  docs-lint-test: 30 passed, 13 failed.
          citation-scanner-deleted   rc=1  docs-lint-test: 40 passed, 3 failed.
          isevcol-true               rc=1  docs-lint-test: 42 passed, 1 failed.
          isevcol-false              rc=1  docs-lint-test: 42 passed, 1 failed.
          allkinds-off               rc=0  docs-lint-test: 43 passed, 0 failed.
```

`citation-scanner-deleted` now fails, which it could not before. `isevcol-true` was one of two
mutations that made check 14 accept beans it should reject with the suite completely green;
it is now caught, incidentally rather than by design, and the file says so. `allkinds-off`
remains a green fail-open and is untouched by this bean.

**The accepted boundary, asserted rather than left to be discovered.** The matcher still reads
the presence of a number and never the polarity of the claim around it — this change removed
running prose from its reach, not the polarity blindness. A heading therefore answers the
criterion it names whatever it says about it, and `.beans/modus-0049`'s `## Criterion 2 cannot
be met as written` is the live instance. That is intended: a heading is an author filing a
section under a criterion, and the section under it is that criterion's evidence home
(`adr:0005-evidence-lives-in-the-work-item#evidence-home`). It is pinned as a verdict
assertion — "accepted: a heading that denies its criterion still answers it" — and stated in
`doc:05-authoring-for-agents#checks`, so the day it stops being acceptable it is visible.

### Criterion 6 — the rule is stated

`documentation/05-authoring-for-agents.md`. The `Enforcement gap:` paragraph is deleted rather
than reworded: it existed because the rule was a property and the check was a list, and that
is no longer true. What replaces it names the two structural sites, keeps the containers
un-enumerated, records the measured cost of refusing mentions along with pasted output, and
states the heading boundary above. The `Enforced by:` paragraph gains this bean's observed
rejections beside `bean:0055`'s and `bean:0063`'s.

### Criterion 7 — `./gradlew qualityCheck` green

Taken at `47e032a`, which is this branch with everything above it and this block absent:

```
cmd:      ./gradlew qualityCheck
observed: bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 3 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [... gradle task lines ...]
          docs-lint-test: 43 passed, 0 failed.
          [... gradle task lines ...]
          docs-lint: OK — 19 documents, 111 anchors, 1555 references, 102 beans, 36 graph edges, 46 selectable, 102 bean ids, 0 introduced, 102 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 21s
exit:     0
```

The `1555 references` figure moves the moment this block is added to the bean, which is why
the head it was taken at is named rather than assumed; the run at the head that carries this
block is recorded on the pull request body. The two elisions are Gradle task lines and
deprecation notices, and neither carries a count this criterion rests on.

### The graph edge, verified rather than assumed

`docs-lint` check 12 is the acyclicity analyser over `blocked_by`, and `bean:0118` records
that its `awk` exit status is one of twenty-one this gate never inspects — so a broken check
12 prints the same `OK` line as a passing one. Dropping an edge is therefore not something to
land on a green line alone. Two observations, both at `47e032a`:

The count moved with the edit, which says the parse read it: the run before the edge was
dropped reports `37 graph edges` (see the plants under criterion 1) and every run after it
reports `36`.

And the analyser is alive, planted and reverted:

```
cmd:      restore `blocked_by: [modus-0061, modus-0063]` on this bean, add
          `blocked_by: [modus-0093]` to .beans/modus-0061, then /bin/bash tools/docs-lint.sh
observed: FAIL check 12 blocked_by graph has a cycle: modus-0061 -> modus-0093, modus-0093 -> modus-0061
          docs-lint: 1 failure(s).
exit:     1
cmd:      git checkout -- .beans && git status --porcelain
observed: (no output)
exit:     0
```

That is also the direct measurement of the inverted edge: inverting rather than dropping
would have required removing this bean from `.beans/modus-0061`'s blockers at the same time,
and the cycle above is what the half-done inversion looks like.

### The record is measurement-neutral, checked rather than assumed

`doc:50-memory-and-evidence#corpus-figures` warns that a record measuring a corpus it belongs
to changes that corpus, and this bean's evidence is 250-odd lines added to a bean the
differential reads. Re-run at `47e032a`, after every section above was written:

```
cmd:      the all-beans differential, re-run at the branch head
observed: the same eleven files, with the same criterion numbers on each
exit:     0
```

Unchanged from the run at `1c19cf0`. This bean does not appear in the differential in either
run: its citations are filed under the `### Criterion N` sub-headings above, which are
structural sites, so the change does not move its own verdict.

## Not in scope

- Fence tracking (`bean:0063`). There is no perception divergence here and that code is not
  the mechanism; the second section of this bean exists to keep the next agent out of it.
- Check 6's copy of the old fence toggle (`bean:0086`).
- Whether an evidence cell's *contents* are evidence at all (`bean:0087`). This bean is about
  where a citation may stand, not what a cell must hold.
- The numbering gate itself (`bean:0061`), and the three escape routes it records.
