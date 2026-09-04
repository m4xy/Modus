---
# modus-0093
title: Pasted check 14 output standing in top-level prose answers the criterion it reports unanswered
status: completed
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

**`45 selectable` in the criterion 1 and 2 captures is the PLANT, not a difference between the
two heads.** The plant flips `.beans/modus-0033` `todo → completed`, and that bean is
selectable, so every planted run reports one fewer than an unplanted one. Both heads report
46, and anyone diffing the number across this bean would otherwise read a change that is not
there:

```
cmd:      /bin/bash tools/docs-lint.sh, over the merge base 1c19cf0 extracted to a
          scratch tree — no `.git`, so the diff-shaped checks report `-`
observed: docs-lint: OK — 19 documents, 111 anchors, 1552 references, 102 beans, 37 graph edges, 46 selectable, 102 bean ids, - introduced, - on origin/main, - closing transitions, - criteria checked, - unnumbered.
exit:     0

cmd:      the same tree with modus-0033 flipped to `completed`, which is the plant
observed: docs-lint: OK — 19 documents, 111 anchors, 1552 references, 102 beans, 37 graph edges, 45 selectable, 102 bean ids, - introduced, - on origin/main, - closing transitions, - criteria checked, - unnumbered.
exit:     0

cmd:      /bin/bash tools/docs-lint.sh at aa4e64f, in the worktree
observed: docs-lint: OK — 19 documents, 111 anchors, 1557 references, 102 beans, 36 graph edges, 46 selectable, 102 bean ids, 0 introduced, 102 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0
```

One flag moves and it is the one the plant sets. The scratch tree is a `git archive` extract
precisely so that no `git checkout -- .beans` runs anywhere near a worktree holding
uncommitted edits (`bean:0102`, `bean:0116`).

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
sites and names no container at all, so the escapes are refused without being enumerated: a
pasted transcript at column zero is not a heading and not a table row, in a raw `<pre>`, in an
HTML comment, in a `<details>` wrapper, in a lazy block-quote continuation, in a list item, in
the front matter, or in no container at all.

**This criterion is NOT met as worded, and the wording is left alone rather than adjusted to
fit what shipped** (`bean:0113`). It asks that the adopted rule reject *"the container class
by construction"*. It does not. `citation_site()` receives the line and one flag of the
analyser's own state; it holds no raw-HTML-block state and cannot refuse a container. What it
rejects is a SHAPE — running prose — and a container is refused only insofar as its contents
have that shape. Reproduced at `aa4e64f` on a four-criterion bean whose criterion 1 alone is
genuinely answered:

```
cmd:      awk -v KINDS=… -f tools/lib/docs-lint-fence.awk -f tools/lib/docs-lint-c14.awk
          <a bean carrying `# criterion 2 is not answered in the evidence` inside <pre>,
          `# criterion 3 …` inside <details><pre>, and `# criterion 4 …` inside an HTML
          comment>
observed: STATS	4	0
exit:     0
```

No `UNANSWERED`. The same bean with a Markdown table pasted inside a `<pre>` is entered too:
the delimiter row sets `intable` wherever it stands. The `citation_site()` probe says which
lines were read, and it is the direct measurement rather than an inference from the verdict:

```
cmd:      the citation-site probe over the same file, one decision per line
observed: sites=Y.Y......Y.Y...Y..Y....Y....Y....YY.
                        ^19    ^24    ^29      ^34^35
          19  # criterion 2 …   inside <pre>
          24  # criterion 3 …   inside <details><pre>
          29  # criterion 4 …   inside an HTML comment
          34  |---|---|---|     the delimiter row of a table inside <pre>
          35  | 4 | four | …    the row it admits
exit:     0
```

Two halves of the criterion ARE met and are worth separating from the half that is not. The
rule is a property and not a list: no container is named anywhere in `citation_site()`, and
adding one would not change any verdict. And every wild instance and the plant are rejected,
because all of them are prose. What is not met is the claim that the container class as such
falls out of it.

**Two resolutions were available and the claim was corrected rather than the mechanism
narrowed.** Refusing those lines needs a model of which HTML blocks hold literal content —
CommonMark §4.6's type 1, whose four tag names are the whole rule, and type 2's comment. That
is an enumeration of containers, which is the allowlist this bean's own argument rejects, and
it would be wrong in the other direction as well: a `#` heading inside `<details>` with blank
lines around it renders as a heading to CommonMark and to GitHub, so "inside a container" and
"not rendered as a heading" are different sets. A mechanism added under review pressure to
make a sentence true is the worse of the two trades. The limit is now stated in
`doc:05-authoring-for-agents#checks`, pinned as two `ACCEPTED` verdict assertions in
`tools/docs-lint-test.sh`, written into the analyser's own comment, and owned by `bean:0121`.

`intable` is kept, and the reason previously written here was false. It said the coupling is
why *"a `|`-leading line inside a raw HTML block is not a table row"*; the run above shows a
table inside a `<pre>` is entered exactly like any other. The true reason is narrower and
holds: a `|`-leading line with **no delimiter row above it** is not a table row to any
renderer either, and `line ~ /^\|/` alone would make one a citation site — a bean quoting a
single row out of a transcript would answer the criterion that row names.

```
cmd:      the analyser over a one-criterion bean whose evidence is the single line
          `| criterion 1 is not answered in the evidence`, shipped form then
          `line ~ /^#+ / || line ~ /^\|/`
observed: shipped:      UNANSWERED	1
                        STATS	1	0
          no intable:   STATS	1	0
exit:     0
```

The reviewer measured the coupling to be decorative — `citation_site()` reduced to
`line ~ /^#+ / || line ~ /^\|/` gives byte-identical verdicts over all 102 beans — and that
measurement reproduces here at `aa4e64f`:

```
cmd:      the check 14 analyser over every bean file, shipped citation_site() against the
          same function with the `intable` clause dropped, verdict sets diffed per file
observed: beans compared: 102
          files differing: 0
exit:     0
```

Both are true and they are not in tension: the coupling is decorative **over today's corpus**,
which contains no bare pipe-led line carrying a citation, and load-bearing over the shape
above. The suite no longer relies on the corpus to say so. `citation-site-no-intable` is now a
measured mutation in `tools/docs-lint-test.sh`'s header and scores `47 passed, 4 failed` at the
head this change ships; `intable-sticky`, added in review, covers the other half of the
coupling — that the flag is CLEARED — and scores `50 passed, 1 failed`.

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
remains a green fail-open; it is untouched by this bean and the suite header now names
`bean:0087` as the work item that closes it, which `doc:00-constitution#observed-failing`
requires of a demoted gap and which the header had omitted.

**Re-measured in review, because five assertions were added.** Two pin the container limit
under criterion 3, two pin the `intable` coupling, and one is the second half of the raw-HTML
citation-site fixture, whose name claimed a property its `<pre>` did not contain: it held one
line of plain prose, so the `.` in its map was the prose rule answering and the fixture read
identically with the `<pre>` tags deleted. Taken at `aa4e64f` plus this change's `tools/` and
`documentation/` edits, with this block absent; the run at the head that carries it is on the
pull request body.

```
cmd:      each mutation applied to a copy of tools/, then /bin/bash tools/docs-lint-test.sh
observed: none                       rc=0  docs-lint-test: 48 passed, 0 failed.
          classifier                 rc=1  docs-lint-test: 36 passed, 12 failed.
          citation-site-off          rc=1  docs-lint-test: 33 passed, 15 failed.
          citation-site-no-intable   rc=1  docs-lint-test: 45 passed, 3 failed.
          citation-scanner-deleted   rc=1  docs-lint-test: 43 passed, 5 failed.
          isevcol-true               rc=1  docs-lint-test: 47 passed, 1 failed.
          isevcol-false              rc=1  docs-lint-test: 46 passed, 2 failed.
          allkinds-off               rc=0  docs-lint-test: 48 passed, 0 failed.
```

`citation-site-no-intable` is a new mutation and it is the answer to the review finding that
the coupling is decorative. It scored `45 passed, 3 failed` when it was added, so the coupling is now covered by
assertion rather than by the corpus happening not to contain the shape. `isevcol-false` moved
from one failure to two, and the second is `ACCEPTED: a Markdown table pasted inside <pre> is
entered like any other`: its row is NUMBERED and stands in an evidence section, so a mutation
that stops recognising the `evidence` header makes `NOEVCOL Evidence` fire on it. The
`intable` control table also carries an `evidence` header but its row is not numbered, so
`NOEVCOL` cannot fire there and it gives `STATS 1 0` under both forms — it does not fail, and
an earlier draft of this paragraph named it as the second failure. Incidental again, and
recorded as incidental. The five that fail under `citation-scanner-deleted` are named in the suite header;
`ACCEPTED: a Markdown table pasted inside <pre> is entered like any other` is deliberately not
one of them, because its row is numbered and the evidence-row path answers it.

**Re-measured a second time in review, and every figure moved again.** Three assertions were
added at `d914eb5`: one pinning a FOURTH residual, which `bean:0121` now carries — this check's
own stdout pasted into an evidence CELL of a numbered row, where the row around it is the site
and nothing is written by hand — one control for it, and one covering the `intable` RESETS,
which `citation-site-no-intable` does not reach. A fifth mutation, `intable-sticky`, was added
with that last assertion; against the 48-assertion suite at `d914eb5` it scored
`48 passed, 0 failed`, a real weakening nothing in the suite could see. The block below is
measurement-neutral by construction: every figure in it is a function of `tools/` alone, and
this block lives in `.beans/`, which no mutation reads.

```
cmd:      each mutation applied to a fresh copy of tools/ from the tree this change ships,
          then /bin/bash <copy>/tools/docs-lint-test.sh
observed: none                       rc=0  docs-lint-test: 51 passed, 0 failed.
          classifier                 rc=1  docs-lint-test: 39 passed, 12 failed.
          citation-site-off          rc=1  docs-lint-test: 35 passed, 16 failed.
          citation-site-no-intable   rc=1  docs-lint-test: 47 passed, 4 failed.
          intable-sticky             rc=1  docs-lint-test: 50 passed, 1 failed.
          citation-scanner-deleted   rc=1  docs-lint-test: 45 passed, 6 failed.
          isevcol-true               rc=1  docs-lint-test: 50 passed, 1 failed.
          isevcol-false              rc=1  docs-lint-test: 46 passed, 5 failed.
          allkinds-off               rc=0  docs-lint-test: 51 passed, 0 failed.
```

`intable-sticky` deletes the three `intable = 0` resets — the `## ` branch, the `#+ ` branch
and the else branch — so the flag is sticky once any table has been seen, and a stray row
quoted out of a transcript two paragraphs below a table that has ended is read as a row of it.
`citation-site-no-intable` proves the flag is READ and proves nothing about its being CLEARED;
the pair covers both halves. `isevcol-false` goes from two failures to five because all three
new fixtures number their rows in an evidence section. `allkinds-off` is still the one green
fail-open and is still `bean:0087`'s.

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

**Corrected in review, because the first version stated enforcement the check does not have.**
It said the excluded containers *"are deliberately not enumerated"* and that a raw HTML block,
an HTML comment and a `<details>` wrapper *"all fail it by construction"*. They do not; see
criterion 3. What stands now says the rule tests a line's SHAPE and models no container,
names the three residuals, and points at `bean:0121`. The `Enforced by:` paragraph is
qualified in the same edit: every plant it lists put the citation on a line of PROSE inside a
container, and prose is what was rejected. Two further corrections in the same pass, neither
of them about this rule: a heading here means an ATX heading, so a **bold line** and a
**Setext** heading are not sites; and the recommendation to file the citation as a sub-heading
now carries the emptiness and region residuals beside it, since recommending the shape is what
promotes them from accident to convention.

Bold is named because it is what authors actually write, and that is measured rather than
supposed. Over `.beans/` at `1c19cf0` — the merge base, so the narrowing's live cost and not
this branch's:

```
cmd:      every line of every bean file that was a citation site under the 1c19cf0 rule, is
          not one under this change's rule, and carries a check 14 matcher hit, classified
          by shape
observed: lost citation sites carrying a matcher hit: 143
            prose    131
            bold      10
            ordered    2
            bullet     0
            setext     0
          bold     .beans/modus-0030--domainmgmt-domain-aggregate.md:223
          bold     .beans/modus-0049--bash-32-claim-is-unenforced.md:487
          bold     .beans/modus-0049--bash-32-claim-is-unenforced.md:491
          bold     .beans/modus-0049--bash-32-claim-is-unenforced.md:519
          bold     .beans/modus-0055--evidence-required-to-close-a-bean.md:310
          bold     .beans/modus-0063--fence-state-inversion-in-the-check-14-analyser.md:311
          bold     .beans/modus-0065--ambient-capability-ports.md:122
          bold     .beans/modus-0068--encode-sprint-1-findings.md:351
          bold     .beans/modus-0069--per-request-usage-is-the-published-vocabulary.md:169
          bold     .beans/modus-0116--the-plant-hazard-recurs-through-the-capture-procedure.md:136
          ordered  .beans/modus-0049--bash-32-claim-is-unenforced.md:848
          ordered  .beans/modus-0096--check-14-contributes-nothing-to-an-implementation-pull-request.md:214
exit:     0
```

131 of 143 are running prose, which is the narrowing working. **Ten are bold pseudo-headings**,
named rather than counted above, and they are the cost an author pays without being told —
which is why `doc:05` now says so. Setext is a latent regression only: the analyser never
tracked it and no bean in the corpus uses it, so nothing is lost today and the day one is
written it is unanswered silently. The same run over `documentation/**` as well adds one
bullet, `documentation/70-skills.md:357`, which check 14 never reads; the figure above is
`.beans/` alone because that is the corpus the check examines.

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
- The composition of fence parity with the citation matcher (`bean:0099`). That bean's
  **matcher half is closed outright** by this change — a range citation in a prose transcript
  no longer answers anything, and the row in its options table reading *"its citation-site
  requirement alone … HOLE OPEN"* was measured against `bean:0063`'s exclusion rule and
  inverts under this one. Both are recorded there with the runs, in the section it gained
  here, beside the note `bean:0061` gained. What that bean still owns — the composed fixture,
  the upstream `NOEVCOL` masking, the reached-not-only-correct property — is untouched.
- The three residuals of the rule this bean adopts, all of them raised as `bean:0121` and none
  of them closed here: a heading- or row-shaped line inside a raw HTML block is a citation
  site, a citing heading is not required to stand inside the evidence region, and it is not
  required to have anything under it. The third criterion above is not met as worded because
  of the first; the section under it says so and does not reword the criterion.
- The matcher's polarity blindness, which is unchanged and is **kept deliberately**. The
  defect this bean closed was *laundering* — a machine-generated string flowing out of the
  tool's stdout and back into the tool with nobody deciding anything. A heading an author
  types is *asserting*, under `doc:00-constitution` §7.4's mandatory independent review.
  Reading polarity would be a blocklist of output patterns, which is the second option in the
  table above and the shape `doc:00-constitution#mechanical-enforcement` records as failing
  open on the first string nobody thought of.

## Closing evidence — merged as PR #75, squashed onto `main` as `9daff18`

A bean cannot close itself, so this is the next change (`doc:00-constitution#bean-lifecycle`).

**The criteria are not restated below.** A close that rewrites its criteria is
indistinguishable from a close that met them (`bean:0113`), so the table indexes
`## Success criteria` by number and records a verdict against the wording already standing
there. Nothing above this heading is edited by this change.

**All but one criterion are met. Criterion 3 is NOT MET AS WORDED**, which its own section above
already records; that section is not amended, the criterion is not reworded, and the residual
is `bean:0121`'s. `doc:80-agent-operating-procedure` step 6 forbids weakening a criterion to
reach green, and changing one is a separate work item and a human decision.

**Answered is not met**, and this close is the sharpest instance of that distinction the
corpus holds. Check 14 reports `7 criteria checked` on this change because every criterion is
cited from a structural site; criterion 3 is among them and is not met. A green check 14 is a
statement about the shape of the evidence and never about the verdict recorded in it
(`doc:05-authoring-for-agents#checks`, `bean:0096`).

Every figure names the head it was taken at, and the heads in play are these. Every figure was
redirected to a file and pasted from that file; the elisions are marked `[...]` and none
carries a count a verdict rests on.

- **`9daff18`** — `origin/main` and this change's merge base, `.beans/` and `tools/`
  unmodified. Blocks A, B and C, and Block D's `git show`.
- **`9daff18`+`status:`** — the merge base with this bean's `status:` line as the only edit
  and this section absent. Block F's second arm, the selectable probe's second arm, and the
  graph plant, which runs on a copy of that tree and never on the worktree.
- **`9daff18`+this section, with Block G absent** — Block G's two check 14 plants and the
  run that restored them.
- **`9daff18`+this section, with the gate block absent** — the gate run at the end.

Block E is not on this list: PR #75's checks are GitHub's record of a head that is already
`main`, and no tree of this branch produces them.

| # | verdict | observed |
|---|---|---|
| 1 | met | The plant is a verdict rejection pinned in `tools/docs-lint-test.sh` and green on `main`, twice over: `the planted defect: pasted output at top level cannot answer its criterion`, and the bean's own shape, `an EVEN number of quoted markers no longer answers the criterion`. Block A |
| 2 | met | Block A. The fenced control still fails for its own reason, and both structural sites have a passing negative control beside the rejections — an evidence sub-heading naming the criterion, and an evidence row citing a range |
| 3 | **NOT MET AS WORDED** | The adopted rule is a property and names no container, and it rejects every wild instance and the plant. It does not reject the container class by construction: `citation_site()` receives a line of text and one flag of the analyser's own state, holds no raw-HTML-block model, and refuses a container only insofar as that container's contents are neither heading- nor row-shaped. The claim was corrected rather than the mechanism narrowed. Block B |
| 4 | met | The differential over the whole corpus, re-run at the head the fix landed on rather than at the merge base it was measured on, and every changed bean named. Three beans entered the corpus between the measurement and the merge and are measured here; one bean drops out of the eleven and the cause is established. Block C |
| 5 | met | Zero `decides "DEFECT` assertions remain, and all six are green verdict rejections rather than perception assertions. Block A |
| 6 | met | `doc:05-authoring-for-agents#checks` carries the rule on `main`, with the container limit, the ATX-heading boundary and the evidence-cell qualifier stated beside it. Block D |
| 7 | met | PR #75's `gate` is `SUCCESS` on the pull request whose merge commit is `9daff18`, and `./gradlew qualityCheck` is green on this closing branch. Block E and the gate section below |

### Block A — criterion 1, criterion 2 and criterion 5, off the suite `main` carries

The word is repeated in that heading on purpose, and the purpose is this check. An evidence
sub-heading is a structural site, so check 14 reads it; its matcher takes at most two numbers
per `criteri(on|a)` token, separated by at most three non-alphanumeric characters. `criteria 1,
2 and 5` therefore sets `A[1]` and `A[2]` and drops the ` and 5`, and `criteria 1 and 2` sets
`A[1]` alone, because ` and ` is letters and cannot be the separator. Criterion 5 is answered
here regardless — by `### Criterion 5` above and by the closing table's row — so no verdict
moves either way. What the rewording buys is that the heading's claim and the analyser's
reading of it are now the same claim, in the one bean whose subject is figures that say more
than their capture supports.

```
cmd:      a five-criterion fixture whose ONLY citation site is the heading under test, run
          through `awk -f tools/lib/docs-lint-fence.awk -f tools/lib/docs-lint-c14.awk` at
          9e46fff; `answered` is the complement of the analyser's UNANSWERED lines
observed: heading:  ### Block A — criteria 1, 2 and 5, off the suite `main` carries
          answered: 1 2
          heading:  ### Block A — criterion 1, criterion 2 and criterion 5, off the suite `main` carries
          answered: 1 2 5
          heading:  ### Block A — criteria 1 and 2, and criterion 5
          answered: 1 5
          heading:  ### Block A — criteria 1, 2, 5
          answered: 1 2
exit:     0
```

The fixture is written outside `.beans/`, which is the only directory check 14 reads, and the
four headings are quoted above from inside a fence — never asked for a citation, which is what
the run below asserts at its line 21. Neither the probe nor this record can answer a criterion
of this bean by accident.

The suite is the mechanism the two plants and the six flipped pins live in, so criteria 1, 2
and 5 are read off one run rather than replanted. Both runs are at `9daff18` with `tools/`
unmodified — `git status --porcelain` names this bean and nothing else, and the suite runs on
its own fixtures and reads no bean file.

```
cmd:      git rev-parse HEAD && git status --porcelain && /usr/bin/grep -c 'decides "DEFECT' tools/docs-lint-test.sh
observed: 9daff18c55ac1fa727da7906652b3bce337fbef9
           M .beans/modus-0093--pasted-output-in-top-level-prose-answers-the-criterion-it-reports-unanswered.md
          0
exit:     1
```

`grep` here is `/usr/bin/grep`, the BSD grep 2.6.0-FreeBSD that CI also runs, named because
the interactive shell's `grep` is a harness-installed `ugrep 7.8.4`. `exit: 1` is `grep -c`
finding nothing, which is the result and not a failure. The pattern is anchored on
`decides "DEFECT` rather than on the bare label, because that label still stands in the comment
heading the flipped block and in the `ACCEPTED` note beside the container residual, and a count
including those would measure the wrong thing.

```
cmd:      /bin/bash tools/docs-lint-test.sh
observed: [...]
          ok   verdict: a quoted fence marker is refused, not laundered into an answer
          ok   verdict: quoted correctly, the pasted output stays inside the fence and answers nothing
          [...]
          ok   verdict: the planted defect: pasted output at top level cannot answer its criterion
          ok   verdict: control: an evidence sub-heading naming the criterion answers it
          ok   verdict: control: an evidence row citing a range of criteria answers all of them
          [...]
          ok   verdict: an EVEN number of quoted markers no longer answers the criterion
          ok   verdict: a raw HTML <pre> block no longer answers its criteria
          ok   verdict: an HTML comment renders as nothing and no longer answers
          ok   verdict: <details> wrapping a <pre> does not answer either
          ok   verdict: a line-initial inline code span leaves the next line unable to answer
          ok   verdict: a backtick in the info string does the same
          [...]
          docs-lint-test: 51 passed, 0 failed.
exit:     0
```

Four elisions, each a run of whole lines from the same capture and never a partial one. What
each one covers is derived below rather than recalled: the run is 60 lines, the twelve lines
quoted above are 26–27, 36–38, 44–49 and 60, and the elided runs are therefore 1–25, 28–35,
39–43 and 50–59.

```
cmd:      /bin/bash tools/docs-lint-test.sh redirected to `suite.txt`, then
          /usr/bin/grep -c '' suite.txt
observed: 60
exit:     0

cmd:      the twelve kept lines of the fence above, dedented out of this bean into
          `kept.txt`, matched back against the run:
          /usr/bin/grep -n -F -f kept.txt suite.txt
observed: 26:ok   verdict: a quoted fence marker is refused, not laundered into an answer
          27:ok   verdict: quoted correctly, the pasted output stays inside the fence and answers nothing
          36:ok   verdict: the planted defect: pasted output at top level cannot answer its criterion
          37:ok   verdict: control: an evidence sub-heading naming the criterion answers it
          38:ok   verdict: control: an evidence row citing a range of criteria answers all of them
          44:ok   verdict: an EVEN number of quoted markers no longer answers the criterion
          45:ok   verdict: a raw HTML <pre> block no longer answers its criteria
          46:ok   verdict: an HTML comment renders as nothing and no longer answers
          47:ok   verdict: <details> wrapping a <pre> does not answer either
          48:ok   verdict: a line-initial inline code span leaves the next line unable to answer
          49:ok   verdict: a backtick in the info string does the same
          60:docs-lint-test: 51 passed, 0 failed.
exit:     0

cmd:      the complement, every line the fence above elides:
          /usr/bin/grep -n -v -F -f kept.txt suite.txt
observed: 1:--- perception: where the analyser believes the fences are
          2:ok   perception: a balanced fence
          3:ok   perception: a three-backtick marker inside a four-backtick fence is content
          4:ok   perception: an odd number of markers leaves a block open, and says so
          5:ok   perception: a tilde fence is a fence, and a backtick marker inside it is content
          6:ok   perception: a tilde marker does not close a backtick fence
          7:ok   perception: an info string opens a fence
          8:ok   perception: a backtick in the info string is an inline code span, not a fence
          9:ok   perception: a marker indented four columns is not a delimiter
          10:ok   perception: a tab-indented marker is not a delimiter
          11:ok   perception: three columns of indent still opens and closes
          12:ok   perception: a marker in a table cell is not line-initial and cannot be a delimiter
          13:ok   perception: a longer marker closes a shorter fence
          14:ok   perception: a closing marker may carry nothing but whitespace
          15:ok   perception: CRLF line endings still close a fence
          16:
          17:--- citation site: where the analyser will read a criterion citation
          18:ok   citation site: a heading and a row of an entered table are sites; prose is not, in or out of raw HTML
          19:ok   citation site: raw HTML is NOT modelled: a heading-shaped and a row-shaped line inside <pre> are sites
          20:ok   citation site: no container is a site, and none of them had to be named to be refused
          21:ok   citation site: a fence's inside and its delimiters are never asked, and the answer is not '.'
          22:
          23:--- verdict: what the analyser decides about a bean
          24:ok   verdict: a filled evidence table answers its criteria
          25:ok   verdict: a transcript that cites no criterion answers none
          28:ok   verdict: a stray marker above a filled table is named, not reported as missing evidence
          29:
          30:--- adversarial: attempts to defeat the fence tracking
          31:ok   verdict: a tilde-fenced transcript cannot answer its own criteria
          32:ok   verdict: a nested fence does not release the outer block
          33:ok   verdict: a fenced transcript indented into a list item cannot answer its criteria
          34:ok   verdict: a block-quoted transcript cannot answer its criteria
          35:ok   verdict: an indented chunk with no marker at all cannot answer its criterion
          39:ok   verdict: accepted: a heading that denies its criterion still answers it
          40:ok   verdict: a bean whose only evidence is inside a container has no entry
          41:ok   verdict: control: the same transcript unquoted IS an entry
          42:ok   perception: RESIDUAL: a fence inside a block quote is not seen
          43:ok   perception: RESIDUAL: a fence indented into a list item is not seen
          50:ok   verdict: ACCEPTED: a heading-shaped line inside <pre>, <details> or an HTML comment answers
          51:ok   verdict: ACCEPTED: a Markdown table pasted inside <pre> is entered like any other
          52:ok   verdict: ACCEPTED: pasted stdout in an evidence CELL answers a criterion no row numbers
          53:ok   verdict: control: the same table without the pasted cell leaves criterion 3 unanswered
          54:ok   verdict: a pipe-led line that is not a row of an entered table is not a site
          55:ok   verdict: control: the identical line under a delimiter row IS a row, and answers
          56:ok   verdict: a table the analyser has LEFT is not entered, so a later stray row is not a site
          57:ok   perception: the length rule applies to tilde fences too
          58:ok   perception: a shorter tilde marker does not close a longer tilde fence
          59:
exit:     0
```

**Neither listing elides anything.** The two are disjoint and exhaust the file — twelve kept
plus forty-eight elided is the whole 60 — and both were taken at `9e46fff`, this branch's head
before this correction, with `tools/` byte-identical to `9daff18`: `git diff --numstat 9daff18
HEAD -- tools/` prints nothing, so the run mapped here is the run the fence quotes.

- **1–25** is the perception section with its header, the citation-site section with its
  header, the blank line between them — and then the `--- verdict:` header and that section's
  first two assertions. It does not stop where the citation-site section stops; it reaches
  three lines into the verdict section.
- **28–35** is one further verdict assertion, then the `--- adversarial:` header and all five
  adversarial assertions. It is not the remainder of the verdict section: the adversarial
  section is elided here, entire.
- **39–43** is the `accepted:` boundary assertion, the container-entry assertion with its
  control, and the two `RESIDUAL` perception assertions. It holds no adversarial assertion at
  all, and it sits below the planted defect and its two controls rather than above them.
- **50–59** is the three `ACCEPTED` assertions and the control that closes them, the three
  table-state assertions, the two tilde perception assertions, and the blank line before the
  total.

Nothing between the `ok` and the name on any kept line was touched, and the middle listing is
what shows it: twelve patterns lifted out of this bean, twelve exact `-F` matches in the run,
in the order the bean quotes them.

The two controls at 37 and 38 are what stop the six rejections below them — 44–49 — meaning
nothing: without a run asserting that something DOES answer, the same failure set is produced
by deleting the citation scanner outright, which the suite header names as a measured mutation.

### Block B — criterion 3, the shape of what shipped

```
cmd:      git show 9daff18:tools/lib/docs-lint-c14.awk | /usr/bin/grep -n -A2 '^function citation_site'
observed: 86:function citation_site(line) {
          87-  return (line ~ /^#+ / || (intable && line ~ /^\|/))
          88-}
exit:     0
```

That is the whole of the adopted rule. It takes the line and reads `intable`, the analyser's
own table state, and nothing else.

The criterion asks for three things and two of them hold. The rule is a property and not a
list: no container is named anywhere in `citation_site()`, and adding a name to it would move
no verdict, because there is nowhere in the function for a name to go. And every wild instance
and the plant are rejected, all of them for one reason — they are running prose.

The third does not hold. The container class is not refused *by construction*, because there
is no construction here that could refuse a container: the function is given a line of text
and one boolean, and a container is not a property of either. What it rejects is a shape, and
a container is refused only when its contents happen not to have the accepted shapes.

`### Criterion 3 — a property, not a list` above carries the measurement, the probe output
naming which lines were read, and the reason the claim was corrected rather than the mechanism
narrowed. It is unchanged. The limit is stated on `main` at
`doc:05-authoring-for-agents#checks` and in the analyser's own comment, pinned as `ACCEPTED`
verdict assertions in the suite, and owned by `bean:0121`.

### Block C — criterion 4, re-measured at the head the fix landed on

The measurement above was taken over 102 bean files at `1c19cf0`, the branch's merge base.
The fix did not land there. `9daff18`'s parent is `4d75cc6`, which closed `bean:0049` and
`bean:0096` and added two beans; `9daff18` added one more. So the corpus at the time of the
fix is 105 beans, three of which no run recorded above could have covered, and the criterion
quantifies over the corpus rather than over a count deliberately.

Re-run at `9daff18`, whole corpus, shipped `citation_site()` against the exclusion rule at
`1c19cf0` substituted into the same analyser, verdict sets diffed per file:

```
cmd:      one call: `git rev-parse HEAD`, then `git status --porcelain`, then the check 14
          analyser over every bean file with the shipped `citation_site()` against the
          `1c19cf0` rule substituted into the same analyser, verdict sets diffed per file
observed: 9daff18c55ac1fa727da7906652b3bce337fbef9
          CHANGED modus-0028--normative-gate-commands.md
              1a2
              > UNANSWERED	1
              6a8
              > UNANSWERED	7
          CHANGED modus-0035--beans-graph-check.md
              0a1,6
              > UNANSWERED	1
              > UNANSWERED	2
              > UNANSWERED	3
              > UNANSWERED	4
              > UNANSWERED	5
              > UNANSWERED	6
          CHANGED modus-0061--check-14-is-gated-on-numbered-criteria.md
              1a2,4
              > UNANSWERED	1
              > UNANSWERED	2
              > UNANSWERED	3
          CHANGED modus-0062--docs-lint-does-not-scan-claude-skills.md
              1a2,3
              > UNANSWERED	1
              > UNANSWERED	2
          CHANGED modus-0086--check-6-resolves-references-through-a-naive-fence-toggle.md
              1a2
              > UNANSWERED	1
              3a5
              > UNANSWERED	4
          CHANGED modus-0091--transcript-discipline-in-evidence.md
              3a4
              > UNANSWERED	3
              5a7
              > UNANSWERED	6
          CHANGED modus-0098--pull-request-bodies-restate-evidence.md
              1a2
              > UNANSWERED	1
          CHANGED modus-0101--a-stacked-bean-is-unauditable-until-the-stack-lands.md
              1a2
              > UNANSWERED	1
              4a6
              > UNANSWERED	5
          CHANGED modus-0118--docs-lint-reports-ok-through-almost-every-runtime-failure.md
              2a3,4
              > UNANSWERED	2
              > UNANSWERED	3
          CHANGED modus-0119--spend-records-carry-no-seq-kind-or-crc.md
              0a1,3
              > UNANSWERED	1
              > UNANSWERED	2
              > UNANSWERED	3
              1a5,7
              > UNANSWERED	5
              > UNANSWERED	6
              > UNANSWERED	7
              2a9
              > UNANSWERED	9
          CHANGED modus-0120--nothing-closes-a-bean-after-its-work-merges.md
              2a3
              > UNANSWERED	3
          CHANGED modus-0121--the-citation-site-constrains-shape-but-not-region-emptiness-or-container.md
              1a2,6
              > UNANSWERED	1
              > UNANSWERED	2
              > UNANSWERED	3
              > UNANSWERED	4
              > UNANSWERED	5
          CHANGED modus-0122--the-constitution-misstates-the-ruleset-it-says-it-states-once.md
              1a2
              > UNANSWERED	2
              2a4
              > UNANSWERED	4
          beans compared: 105
          files differing: 13
exit:     0
```

The head line is the `git rev-parse` at the front of that call; `git status --porcelain`
printed nothing between the two, which is what makes this a measurement of `9daff18` and not
of a tree that happens to sit on it — it was taken before this bean's `status:` line was
edited. Everything below the head is the differential's stdout, redirected to a file and
pasted whole from it. **No elision.**
The `1a2` and `0a1,6` lines are `diff`'s hunk headers and the `>` lines are its marker for a
line present only in the second verdict set, which is the shipped rule; so every `>` line is
one criterion the narrowing newly reports unanswered, and the number after each is separated
by a tab.

**Two things moved against the eleven named at `1c19cf0`, and both are established rather
than assumed.**

`bean:0049` drops out. `### Criterion 4` above recorded it as the one bean needing action and
not this bean's to act on, and predicted the failure if the parallel branch closed it after
this landed. It closed *before* — in `4d75cc6`, `9daff18`'s parent — so it is `completed` on
this change's merge base and check 14 never re-reads it. It also no longer differs at all,
and that is not because its evidence sub-headings were renamed: they still read `### 1 — …`
and `### 3 — …`. Its closing sections cite the numbers from block headings, which are
structural sites:

```
cmd:      /usr/bin/grep -n '^#.*[Cc]riteri' .beans/modus-0049--bash-32-claim-is-unenforced.md
observed: 35:## Success criteria
          58:## Criterion 2 cannot be met as written, and this bean stays open
          147:### 3 — the mechanism that was built, observed rejecting both (a substitute, not criterion 2)
          289:### Block A — criteria 1 and 3, read off `1c19cf0`
          334:### Block B — criterion 2's concern has a home on `main`, and it is not this bean
          349:### Block C — criterion 1's live half, on this tree
          461:### Block E — criteria 1 and 3 under the narrowing `bean:0093` carries, checked rather than assumed
exit:     0

cmd:      the check 14 analyser over .beans/modus-0049--bash-32-claim-is-unenforced.md at 9daff18
observed: STATS	3	0
exit:     0
```

Its second criterion is answered by the heading that denies it, which is the accepted boundary
this bean records and not an accident.

Three beans enter. `bean:0120` and `bean:0122` arrived in `4d75cc6` and `bean:0121` in
`9daff18` itself, all three after the corpus measurement, and all three change. None fails
today — every one is `todo` — and the cost is the forward one the criterion is about: each
must file its citations structurally when it closes.

The instrument is validated by its own agreement with the recorded measurement. Ten of the
eleven files named at `1c19cf0` reproduce here with the same criterion numbers, and the
eleventh has the cause above. A script matching nothing does not reproduce ten files.

### Block D — criterion 6, the rule as it stands on `main`

```
cmd:      git show 9daff18:documentation/05-authoring-for-agents.md redirected to a scratch
          file, then /usr/bin/grep -n over that file for the five alternatives
          'A criterion is \*\*answered\*\*', 'Running prose is not a citation site',
          'What the rule is not', 'At column zero' and 'A \*\*heading\*\* here is an ATX'
observed: 283:- A criterion is **answered** by an evidence row bearing its number, or by a `criterion N` or
          285:  Running prose is not a citation site, whatever it renders as. Write the citation as an
          296:  **What the rule is not.** It is a test of a line's SHAPE, not a model of containers, and
          309:  **`At column zero` is a qualifier and it has a price.** A citation is read from the whole of
          320:  A **heading** here is an ATX heading: `#` characters at the start of the line. A
exit:     0
```

The locators carry the command that produced them, and the command is a match rather than a
range, so it cannot be misread later as a section extent
(`doc:50-memory-and-evidence#capturing`). Line 283 is the rule; 296, 309 and 320 are the three
things the first version of that section claimed and the check does not have — the container
limit, the evidence-cell qualifier, and the ATX boundary — each corrected in review and each
now on `main`.

The emptiness residual `bean:0121` records is described here and not reproduced. Its fixture
closes a five-criterion bean from a single range-citing sub-heading, and that heading is a
citation site: pasting it into this bean would answer five of this bean's criteria from an
example. That is the same laundering shape one layer up, and it is why the fixture stays in
`tools/docs-lint-test.sh`, a file check 14 never reads because it reads only `.beans/*.md`.

### Block E — criterion 7, the gate

PR #75's checks, on the pull request whose merge commit is `9daff18`:

```
cmd:      GITHUB_TOKEN= gh pr view 75 --json statusCheckRollup \
            -q '.statusCheckRollup[] | "\(.name)\t\(.conclusion)\t\(.detailsUrl)"'
observed: which halves	SUCCESS	[...]/runs/33901590752/job/101116701849
          which halves	SUCCESS	[...]/runs/33901585342/job/101116685207
          build + mechanical gates	SUCCESS	[...]/runs/33901590752/job/101116756121
          build + mechanical gates	SUCCESS	[...]/runs/33901585342/job/101116719671
          backoffice + e2e	SKIPPED	[...]/runs/33901590752/job/101116757224
          backoffice + e2e	SKIPPED	[...]/runs/33901585342/job/101116720739
          gate	SUCCESS	[...]/runs/33901590752/job/101117024195
          gate	SUCCESS	[...]/runs/33901585342/job/101117011697
exit:     0
```

`[...]` elides the `https://github.com/m4xy/Modus/actions` prefix each URL carries and nothing
else; the run and job ids are what the columns are for. Every check appears twice because two
workflow runs answered for this pull request, and the pair is printed rather than collapsed.
`backoffice + e2e` is `SKIPPED` in both, which is the per-path job declining a change that
touches neither directory. `gate` — the job `bean:0047` is holding a required-status rule back
for — is `SUCCESS` in both.

The local gate on this closing branch is under **The gate on this branch** below, taken after
this block was written, because a run taken here could not cover the text beneath it.

### Block F — the counters moved, and check 12 is alive

`main` at `9daff18` reports `0 closing transitions, 0 criteria checked`, which is what a change
that closes nothing prints and what a check that examined nothing prints
(`doc:00-constitution#observed-failing`). The pair below is the same tree differing in one
line — this bean's `status:` — with this section absent from both arms, so the movement is
attributable to the close and to nothing else.

```
cmd:      git status --porcelain; git rev-parse HEAD; /bin/bash tools/docs-lint.sh
observed: 9daff18c55ac1fa727da7906652b3bce337fbef9
          docs-lint: OK — 19 documents, 111 anchors, 1619 references, 105 beans, 37 graph edges, 48 selectable, 105 bean ids, 0 introduced, 105 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0

cmd:      git rev-parse HEAD; git diff --stat; /bin/bash tools/docs-lint.sh
observed: 9daff18c55ac1fa727da7906652b3bce337fbef9
           ...ut-in-top-level-prose-answers-the-criterion-it-reports-unanswered.md | 2 +-
           1 file changed, 1 insertion(+), 1 deletion(-)
          docs-lint: OK — 19 documents, 111 anchors, 1619 references, 105 beans, 37 graph edges, 49 selectable, 105 bean ids, 0 introduced, 105 on origin/main, 1 closing transitions, 7 criteria checked, 0 unnumbered.
exit:     0
```

The leading `...` on the second arm's stat line is `git diff --stat`'s own abbreviation of a
path too long for its column, not an elision of mine; the file is this one and it is the only
one changed. `git status --porcelain` printed no line at all before the first run, which is
why the head is the first thing under `observed:` there and is what makes that arm a
measurement of `9daff18` rather than of a working tree that happens to sit on it. Exactly
three fields move — `48 selectable` to `49`, `0 closing transitions` to `1`, `0 criteria
checked` to `7` — and `1619 references` is identical across the pair, because a status flip
adds no reference. Seven is this bean's seven numbered criteria, all of them cited from the
`### Criterion N` sub-headings above, which are structural sites under the rule this bean
adopted. The check reads its own subject.

**`bean:0121` is selectable after this change, and it is the only bean whose selectability
moves.** The counter says one bean joined; it does not say which. A probe reimplementing
`AGENTS.md` step 1 independently of `tools/docs-lint.sh` — `status: todo`, `type` not `epic`,
every `blocked_by` id resolving to a `completed` bean — was run on both arms and its two
outputs diffed. It agrees with check 12's counter at 48 and at 49, which is what makes it an
instrument rather than an assertion:

```
cmd:      the step 1 probe over `.beans`, on the `9daff18` arm, output redirected and piped
          to `wc -l`
observed:       48
exit:     0

cmd:      the same probe on the `9daff18`+`status:` arm, redirected and piped to `wc -l`
observed:       49
exit:     0

cmd:      diff <the probe's output on the first arm> <its output on the second>
observed: 47a48
          > modus-0121
exit:     1
```

The two counts are `wc -l`'s own column padding, pasted as printed. `exit: 1` on the third is
`diff` reporting a difference, which is the result. One line added, none removed, none
reordered: `bean:0121`'s only blocker is this bean, and closing it clears the edge. The probe
is not check 12 and does not share a line of code with it, so its agreement with `48
selectable` and `49 selectable` above is a cross-check of two independent readings of the
same rule, not one reading quoted twice.

**Check 12's analyser is alive on this tree, planted and reverted.** `bean:0118` records that
this gate never inspects the exit status of twenty-one of the programs it runs, check 12's
acyclicity `awk` among them, so a broken check 12 prints the same `OK` line as a passing one
and `37 graph edges` on a green line is not by itself evidence the cycle detector ran.

The plant runs on a **copy** of the tree, made with `cp -R` and with the worktree's `.git`
file removed, so no `git` command runs anywhere near uncommitted work and no
`git checkout -- .beans` exists to discard it (`bean:0102`, `bean:0116`). With no `.git` the
diff-shaped checks report `-` rather than a count, which is how an inert run is distinguished
from a clean one; check 12 needs no ref and runs either way.

```
cmd:      /bin/bash tools/docs-lint.sh, in a copy of the `9daff18`+`status:` tree with no `.git`
observed: docs-lint: OK — 19 documents, 111 anchors, 1619 references, 105 beans, 37 graph edges, 49 selectable, 105 bean ids, - introduced, - on origin/main, - closing transitions, - criteria checked, - unnumbered.
exit:     0

cmd:      the same copy with `blocked_by: [modus-0063, modus-0121]` on this bean, so the
          edge this bean's close clears points back at the bean that carries it
observed: FAIL check 12 blocked_by graph has a cycle: modus-0093 -> modus-0121, modus-0121 -> modus-0093
          docs-lint: 1 failure(s).
exit:     1

cmd:      the planted line restored by the inverse `sed`, then
          `diff <the copy's bean file> <the worktree's> && echo identical`
observed: identical
exit:     0

cmd:      /bin/bash tools/docs-lint.sh, in the restored copy
observed: docs-lint: OK — 19 documents, 111 anchors, 1619 references, 105 beans, 37 graph edges, 49 selectable, 105 bean ids, - introduced, - on origin/main, - closing transitions, - criteria checked, - unnumbered.
exit:     0
```

`identical` is `echo`'s, reached only because `diff` printed nothing and exited 0; the
restored copy is byte-identical to the worktree's bean, so the plant left nothing behind.

The cycle the plant makes is the exact edge this close clears, run backwards, so what the
detector is watched rejecting is this change's own graph and not an unrelated one. Both the
`OK` lines and the `FAIL` come from the same copy of the same tree.

### Block G — check 14 examined this close rather than passing over it

`1 closing transitions, 7 criteria checked` says the check had a candidate and counted its
criteria. It does not say the check would have rejected anything here, and
`doc:00-constitution#observed-failing` is explicit that a mechanism observed firing and never
observed silent — or the reverse — is a claim. Two plants against the closing table this
change adds, each restored from a copy taken before it, are the observation.

Neither plant runs a `git` command. The bean is copied to a scratch file with `cp`, edited in
place with `sed`, and restored by copying the scratch file back; there is no
`git checkout -- .beans` anywhere in the procedure to discard the uncommitted work this whole
change consists of (`bean:0102`, `bean:0116`). Both were taken with this closing section
present and this block absent.

```
plant:    the closing table's row numbered 3, with its verdict cell kept and its evidence
          cell emptied
observed: FAIL check 14 .beans/modus-0093--pasted-output-in-top-level-prose-answers-the-criterion-it-reports-unanswered.md: criterion 3 closes with an empty evidence cell (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          docs-lint: 1 failure(s).
exit:     1

plant:    the same row renumbered from `| 3 |` to `| three |`, and the two headings that
          cite the number by it — the `### Criterion 3 — …` sub-heading under `## Evidence`
          and the `### Block B — criterion 3, …` heading above — reworded to name it in
          words, so that nothing in the file bears or cites the number any more
observed: FAIL check 14 .beans/modus-0093--pasted-output-in-top-level-prose-answers-the-criterion-it-reports-unanswered.md: criterion 3 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          docs-lint: 1 failure(s).
exit:     1

cmd:      cp <the scratch copy> <the bean> && diff <the scratch copy> <the bean> && echo identical
          /bin/bash tools/docs-lint.sh
observed: identical
          docs-lint: OK — 19 documents, 111 anchors, 1627 references, 105 beans, 37 graph edges, 49 selectable, 105 bean ids, 0 introduced, 105 on origin/main, 1 closing transitions, 7 criteria checked, 0 unnumbered.
exit:     0
```

The second plant is the sharper of the two, because the criterion it unanswers is the one this
bean closes **unmet**. Check 14 accepts this bean with criterion 3 recorded NOT MET AS WORDED
and rejects it the moment nothing bears or cites the number — which is the whole distinction
between *answered* and *met*, and the reason a green check 14 is a statement about shape and
not about a verdict.

**The `FAIL` lines above are this check's own stdout, pasted into this bean, and they are
inert.** That sentence is what this bean was raised to make true. They are inside a fence, so
they are skipped whole; and under the rule this change adopted they would be inert at column
zero as well, because a pasted transcript is neither a heading nor a row of an entered table.
Before `9daff18` the second of them would have set `A[3]` and closed as answered the criterion
it reports unanswered.

### This close is itself an instance of `bean:0120`

`doc:00-constitution#bean-lifecycle` holds a bean `in-progress` through its own pull request
and ends by saying the close "is the first act of the session after a merge". Nothing
establishes that the session happens, and `bean:0120` is that gap. This close is the act the
rule describes and does not assign; it happened because it was dispatched, which is a fact
about dispatch and not about a rule.

The backlog it clears is one tree deep, and that is the honest size rather than a supporting
one:

```
cmd:      git log --format='COMMIT %h %ad %s' --date=short -p origin/main \
            -- .beans/modus-0093--…md | /usr/bin/grep -E '^COMMIT |^\+status:|^-status:'
observed: COMMIT 9daff18 2026-09-04 fix(docs-lint): answer a criterion only from a structural site (#75)
          -status: todo
          +status: in-progress
          COMMIT 63f367e 2026-08-30 docs(05): point the citation gap line at the bean that closes it (#62)
          COMMIT 74cb201 2026-08-30 docs(beans): raise bean:0093 — pasted output in top-level prose answers its own criterion (#56)
          +status: todo
exit:     0
```

`…` elides the rest of this bean's filename in the pathspec, which was given in full. So
`origin/main` has held this bean `in-progress` in exactly one tree, `9daff18`, its own head.
That is not shorter than anything `bean:0120`'s E1 records; it **equals** the shortest run
there. `bean:0116`'s row in that capture reads `1` in the tree that shipped it, `2b67b23`, and
`0` in the very next, `8c3fd82`. What it is nothing like is the twenty-three consecutive trees
E1 records for `bean:0068`. Two observations follow and only the first is about this bean.

`status: in-progress` reached `main` in the same commit that shipped the work, not when the
branch was cut: §7.2.1's first arrow is a rule about a branch, and `main` never sees it.
`bean:0120` records that five of the nine beans in its window were created at `in-progress`
and never flipped; this is the other route to the same conclusion — the status on `main` is
not evidence of when the work started, so a backlog metric built on it measures merges.

And a one-tree run is not evidence that the gap is small. The signal and its absence print
the same characters, so the run is short here and would look identical at any length.

### The record is measurement-neutral, stated rather than assumed

`doc:50-memory-and-evidence#corpus-figures` warns that a record measuring a corpus it belongs
to changes that corpus. This section is the bulk of what this change adds, appended to a bean
that Blocks C, F and G all read. No line count is given for it, here or in a heading, because
a count restated outside the thing it counts is a drift generator and this one moves with
every review fix (`doc:05-authoring-for-agents#one-fact-one-place`); `git diff --stat` on the
pull request is the figure and it is derived rather than copied.

Block C's differential is measurement-neutral over this section by construction: this bean
does not appear in the 13 under either rule, because its citations stand at `### Criterion N`
sub-headings, which are structural sites under the shipped rule and under the one it replaced
alike. So no amount of writing here can move that result — which is the property, not a
result of having checked afterwards.

Block F's `1619 references` and `105 beans` are not neutral over it, and are stamped
`9daff18`+`status:` for that reason: both arms were captured before a line of this section
existed. Block G's `1627 references` is the run that restored its two plants, taken with the
closing section present and Block G itself not yet written. The gate below reports `1629`, and
the two it gained after Block G's run are both this section's own first citations of an anchor
no other line of this bean names: `doc:05-authoring-for-agents#one-fact-one-place`, in the
paragraph above about the line count, and `bean:0068`, in the paragraph about E1's longest
run. `refs.uniq` is a set of file-and-reference pairs, so a second mention of `bean:0102` in
this file adds nothing to it and a first mention of anything adds one. Every figure moves only
in the direction its own text accounts for, and each was re-read after the edit that moved it
rather than assumed to have held.

### The gate on this branch

```
cmd:      git rev-parse HEAD; ./gradlew qualityCheck
observed: 9daff18c55ac1fa727da7906652b3bce337fbef9
          [...]
          > Task :bashCompatLint
          bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 3 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [...]
          docs-lint-test: 51 passed, 0 failed.
          [...]
          > Task :docsLint
          docs-lint: OK — 19 documents, 111 anchors, 1629 references, 105 beans, 37 graph edges, 49 selectable, 105 bean ids, 0 introduced, 105 on origin/main, 1 closing transitions, 7 criteria checked, 0 unnumbered.

          > Task :qualityCheck

          BUILD SUCCESSFUL in 21s
          160 actionable tasks: 6 executed, 154 up-to-date
          Configuration cache entry reused.
exit:     0
tree:     `chore/close-0093`, uncommitted, at `9daff18` with this change's single modified
          file present and this block absent. `git rev-parse HEAD` names the merge base
          because nothing was committed yet, not because the tree is `main`.
```

Three elisions, all Gradle task lines and the tool output between them: the Kotlin, ktlint,
Detekt, ArchUnit and test tasks before the first, the `docs-lint-test` assertion lines and
their section headers before the second — the total they sum to is the line kept — and the
backoffice typecheck, lint and format-check output before the third. Nothing this criterion
rests on was trimmed: `exit:` is stated separately, every line carrying a count is kept, and
`BUILD SUCCESSFUL` is present.

**This block cannot cover its own transcript**, which is the one place this record is not
self-checking, and the gap is narrowed rather than argued away: the run above was taken on the
tree that carries everything up to this heading, and the gate was then run again after the
block was appended. `docs-lint` inside it reports the same twelve counts, because a fence adds
no reference and this block cites nothing new. That second run is not pasted — it would be the
block above with Gradle's wall clock changed, and `doc:50-memory-and-evidence#capturing` is
against a fence that is a composition. CI runs the same gate over the committed tree, and its
result is on the pull request.

`1629 references` differs from Block F's `1619` and Block G's `1627` for the reason the
section above gives, and the three fields this close moves — `49 selectable`,
`1 closing transitions`, `7 criteria checked` — are identical across all of them. The gate
reaches `docsLint` through Gradle rather than through `/bin/bash`, so this is the same verdict
from the runner CI uses.
