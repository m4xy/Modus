---
# modus-0121
title: A citing heading answers a criterion from anywhere in the file, with nothing under it, and from inside a raw HTML block; and an evidence row answers from its own cell
status: in-progress
type: fix
priority: high
created_at: 2026-09-04T00:00:00Z
blocked_by: [modus-0093]
---

# A citing heading answers a criterion from anywhere in the file, with nothing under it, and from inside a raw HTML block; and an evidence row answers from its own cell

`bean:0093` narrowed check 14's citation site to a **shape**: an ATX heading, or a row of a
table the analyser has entered. That is the whole of the constraint. The site is not required
to stand inside the evidence region, it is not required to have anything under it, and the
analyser holds no state that would let it refuse a container. Nor is a citation read from any
particular PART of a site line: the whole row is scanned, evidence cell included. Four
residuals follow, and each lets a bean close green on evidence it never recorded.

The evidence-table path has `EMPTYCELL` and `HOLLOW` — a numbered row whose evidence cell is
blank, or holds only the NAME of an evidence kind, is a finding. **The heading path has no
analogue at all.** `doc:05-authoring-for-agents#checks` now recommends the sub-heading as the
way to cite, which promotes the cheapest hollow shape in the check from an accident into the
documented convention. That is why this is `high` and not an idle observation.

## Observed

Every run below is `awk -v KINDS="…" -f tools/lib/docs-lint-fence.awk -f
tools/lib/docs-lint-c14.awk <fixture>`, on a fixture and not on a bean file, so no plant, no
revert and no `git checkout -- .beans` is involved (`bean:0102`, `bean:0116`). `KINDS` is the
value `tools/docs-lint.sh` passes. The emptiness, region and container runs were taken at
`aa4e64f`; the cell run was found in review and taken at `d914eb5`. Each section states its
own head rather than inheriting one.

### No emptiness constraint: one heading closes a five-criterion bean

The whole of the bean's `## Evidence` is a single sub-heading citing a range. Nothing under
it, and nothing after it.

```
fixture:  a bean numbering criteria 1 to 5, whose `## Evidence` section is exactly the one
          line `### Criteria 1-5`
observed: STATS	5	0
exit:     0
```

No `UNANSWERED`, no `EMPTYEV` — the sub-heading is itself an entry, so the evidence section is
not empty either. Five criteria closed by eleven characters. The equivalent through the table
path is rejected twice over: an empty cell is `EMPTYCELL` and a cell naming a kind is
`HOLLOW`.

### No region constraint: a heading under `## Not in scope` answers

`region` is tracked by the analyser and is what decides whether a heading counts as an entry
and whether a table row is read as a criterion or as evidence. `citation_site()` does not read
it, so a citing heading answers from any section of the file.

```
fixture:  a bean numbering criteria 1 to 3, with `### Criterion 1` and real evidence under
          `## Evidence`, and `### Criterion 2 is deferred to another bean` and
          `### Criterion 3 was not attempted` under `## Not in scope`
observed: STATS	3	0
exit:     0
```

Both headings close their criteria. **Nothing here requires the check to read polarity**, and
that is the point of pairing the two constraints: `## Criterion 3 was not attempted` is
refused for standing outside the evidence region and for having nothing under it, not for what
it says. The polarity blindness is deliberate and stays (`bean:0093`'s `## Not in scope`): a
heading an author types is asserting under `doc:00-constitution` §7.4's mandatory independent
review, where the defect `bean:0093` closed was a machine-generated string flowing out of the
tool's stdout and back into the tool with nobody deciding anything. Reading polarity would be
the blocklist `doc:00-constitution#mechanical-enforcement` records as failing open on the first
string nobody thought of.

### No cell constraint: a row answers a criterion from its own evidence cell

The other three residuals are about WHICH LINES are sites. This one is about how much of a site
line is read: all of it. Once `intable` and a `|` make the line a row, the citation scan runs
over the whole row, so a criterion number appearing in the **evidence cell** of one row answers
that criterion as surely as the row's own number would. Paste this check's own stdout into a
cell and it answers the criterion it reports unanswered.

This run is at `d914eb5`, not `aa4e64f`, and its control is the identical fixture with the
pasted string removed:

```
fixture:  a bean numbering criteria 1 to 3, whose `## Evidence` table numbers rows 1 and 2
          only, row 2's evidence cell reading
            FAIL check 14 .beans/modus-9901--rr-probe.md: criterion 3 is not answered in the
            evidence; exit 1
observed: STATS	3	0
exit:     0

fixture:  the identical bean with row 2's cell reading `ran it too`
observed: UNANSWERED	3
          STATS	3	0
exit:     0
```

**This is the one residual of the four where nothing has to be written by hand.** The three
above all need an author to type a `#` or a `|`; the pasted stdout that `bean:0093` was raised
for arrives here verbatim, as a cell, in a table an author filled in for an entirely different
reason. It is therefore laundering by the definition `bean:0093` adopted — a machine-generated
string out of the tool's stdout and back into the tool, with nobody deciding anything about
criterion 3 — reaching the analyser through the site the narrowing KEPT rather than through the
prose it refused. The shape already stands in the corpus at
`.beans/modus-0055--evidence-required-to-close-a-bean.md:123`, harmless only because rows 4 and
5 of that table exist and answer their own criteria.

Like the region and emptiness residuals and unlike the container one, this needs **no new
perception layer**: `evcol` is already tracked, so "do not read a citation out of the evidence
column of a row" is decidable from state the analyser holds. Whether that is the right
constraint is this bean's to decide — a row legitimately reading `| 3 | … | criteria 1-5 are
answered by the run above | …` is the case that argues against it, and `doc:05-authoring-for-agents#checks`
now tells authors to quote transcripts inside a fence rather than inside a cell.

### No container model: a raw HTML block is entered

`citation_site()` receives the line text and one flag of the analyser's own state. It has no
raw-HTML-block state and cannot refuse a container; a container is refused only insofar as its
contents are neither heading-shaped nor row-shaped.

```
fixture:  a bean numbering criteria 1 to 4, criterion 1 genuinely answered, and
          `# criterion 2 is not answered in the evidence` inside <pre>,
          `# criterion 3 …` inside <details><pre>, and `# criterion 4 …` inside an
          HTML comment
observed: STATS	4	0
exit:     0
```

```
fixture:  the same, with a Markdown table pasted inside a <pre>
observed: the delimiter row sets `intable` wherever it stands, so the row that follows is
          read as a table row and answers its criterion
exit:     0
```

**This one is different in kind from the three above and may be split out.** The other three
are extra conditions on a site the analyser already recognises, and all three are decidable
from state it already tracks — `region`, the entry rule, `evcol`. This one needs a new
perception layer: which HTML blocks hold literal
content is CommonMark §4.6's type 1, whose four tag names are the whole rule, and type 2's
comment — an enumeration of containers, which is the allowlist `bean:0093`'s argument
replaced. It is also wrong in the other direction: a `#` heading inside `<details>` with blank
lines around it renders as a heading to CommonMark and to GitHub alike, so "inside a
container" and "not rendered as a heading" are different sets. An agent selecting this bean
should decide whether the container residual belongs in it
(`doc:05-authoring-for-agents#bean-split`).

## Why the region, emptiness and cell constraints are one bean

Either of the first two alone is walked past by the other. Requiring the citing heading to be
inside the evidence region leaves `### Criteria 1-5` as the whole of that region. Requiring
content under the heading leaves `### Criterion 3 was not attempted` under `## Not in scope`,
followed by a paragraph. Together they cover both without reading meaning, and both fail
closed: a heading that satisfies neither reports the criterion unanswered, which is the
direction `doc:00-constitution#observed-failing` asks for.

The cell constraint belongs with them and not with the container residual. It is the same kind
of thing — an extra condition on a site the analyser already recognises, decidable from `evcol`,
which it already computes — and it fails closed the same way: a criterion cited only from an
evidence cell is reported unanswered. It is also the residual that most needs deciding TOGETHER
with the other two, because the cheapest way to satisfy a rule that refuses the cell is to move
the citation into a sub-heading, which is exactly what the emptiness constraint has to catch.

"Non-empty content beneath it" needs defining rather than assuming, and the definition already
exists: `doc:05-authoring-for-agents#checks` defines an **entry** as a table row, a sub-heading
or a fenced block, and states that a section of prose alone is not one. Whether the citing
heading requires an entry under it or merely a non-blank line is the decision this bean makes,
and it has a measurable cost over the corpus either way.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The three fixtures above that this bean closes — the whole-region sub-heading, the two headings under `## Not in scope`, and the pasted stdout in row 2's evidence cell — are observed rejected, not merely no longer accepted, and negative controls show that a heading inside the evidence region with content under it, and a legitimately citing evidence row, both still close | planted violation, reverted |
| 2 | The region constraint and the emptiness constraint are each observed to fail on their own against the other's fixture, so that neither is landed on the other's evidence | test-run |
| 3 | What counts as content under a citing heading is decided against `doc:05-authoring-for-agents#checks`'s existing definition of an entry, and the choice is stated with its cost rather than assumed | diff |
| 4 | **Every** bean in `.beans/` is measured before and after and every bean whose answered-set changes is named, stated without a count because the corpus grows (`bean:0093`'s fourth criterion is the precedent) | analyser run over the corpus, before and after |
| 5 | The three `ACCEPTED` verdict assertions in `tools/docs-lint-test.sh` — the two that pin the container limit and the one that pins the pasted cell — are each either flipped to a rejection or restated with the reason it remains accepted; a decision either way is recorded, and silence is not one | test-run |
| 6 | `doc:05-authoring-for-agents#checks` states the constraints that result, and the paragraph this bean is named in is corrected rather than left beside them | diff |
| 7 | The matcher still never reads polarity, and no rejected string is enumerated anywhere in the change | diff |
| 8 | The cell constraint is decided against a row that cites legitimately — an evidence row whose cell names, by number, a span the run recorded in it genuinely covers — and the decision states which of the two cases it sacrifices, rather than being written as if only the laundering case existed. The example is deliberately described here and not written out: writing it out in this table would answer those numbers in this bean | diff |
| 9 | `./gradlew qualityCheck` green | test-run |

## Evidence

Head: every figure below was taken at `5a625fc`, the head of
`fix/0121-citation-region-emptiness-and-cell`, unless the figure names another. The `before`
half of each pair is the analyser as it stood at `3b02871`, extracted with `git show` into a
scratch directory and run against the same fixture in the same command — the head this bean's
`## Observed` section should be re-read at, since its own figures were taken at `aa4e64f` and
`d914eb5`.

**All four residuals still reproduce at `3b02871`**, unchanged, over a corpus that has grown
to 110 beans. Two details of the `## Observed` section did not survive re-measurement and are
corrected under criterion 1 and criterion 8 below rather than quietly worked around.

### Criterion 1

The three fixtures this bean closes, each run through the analyser before and after. Fixtures
and not bean files, so no plant, no revert and no `git checkout -- .beans` is involved
(`bean:0102`, `bean:0116`). `KINDS` is the value `tools/docs-lint.sh` passes.

```
$ awk -v KINDS=" command test-run diff citation fetch observation " \
    -f tools/lib/docs-lint-fence.awk -f tools/lib/docs-lint-c14.awk <fixture>

=== r1-region        two citing sub-headings under `## Not in scope`, each with a
                     paragraph under it; criterion 1 answered normally
--- before
STATS	3	0
exit: 0
--- after
UNANSWERED	2
UNANSWERED	3
STATS	3	0
exit: 0
=== r2-empty         one citing sub-heading, naming a range covering all five, as the
                     whole of a five-criterion bean's `## Evidence`
--- before
STATS	5	0
exit: 0
--- after
UNANSWERED	1
UNANSWERED	2
UNANSWERED	3
UNANSWERED	4
UNANSWERED	5
STATS	5	0
exit: 0
=== r3-cell          three criteria, rows 1 and 2 only, row 2's evidence cell holding this
                     check's own stdout about the third
--- before
STATS	3	0
exit: 0
--- after
UNANSWERED	3
STATS	3	0
exit: 0
=== r3-cell-control  the identical bean with the pasted string replaced by `ran it too`
--- before
UNANSWERED	3
STATS	3	0
exit: 0
--- after
UNANSWERED	3
STATS	3	0
exit: 0
=== r4-container     the residual this bean did NOT close, carried to `bean:0128`
--- before
STATS	4	0
exit: 0
--- after
STATS	4	0
exit: 0
=== r4-table-in-pre  the second half of the same residual
--- before
STATS	1	0
exit: 0
--- after
STATS	1	0
exit: 0
```

**Observed rejected, not merely no longer accepted, and at the GATE and not only in the
analyser.** Each residual was planted as a bean that closes in this change — an UNTRACKED
file, so reverting is `rm` and never `git checkout -- .beans`, which would discard the
uncommitted edits to tracked beans this branch carries. The tree was checked clean before
each plant and after each revert, not once before the first (`bean:0102`).

```
$ bash <scratch>/plant.sh   # cp a plant into .beans/, run tools/docs-lint.sh, rm the plant
HEAD 5a625fc

########## PLANT 1 — region
tree before the plant: []
FAIL check 14 .beans/modus-9901--plant-1-the-region-residual.md: criterion 2 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
FAIL check 14 .beans/modus-9901--plant-1-the-region-residual.md: criterion 3 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
docs-lint: 2 failure(s).
exit: 1
tree after the revert: []

########## PLANT 2 — emptiness
tree before the plant: []
[... five FAIL lines, one per criterion, identical in form to the two above ...]
docs-lint: 5 failure(s).
exit: 1
tree after the revert: []

########## PLANT 3 — evidence cell
tree before the plant: []
FAIL check 14 .beans/modus-9903--plant-3-the-evidence-cell-residual.md: criterion 3 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
docs-lint: 1 failure(s).
exit: 1
tree after the revert: []

########## CONTROL — every accepted shape
tree before the plant: []
docs-lint: OK — 19 documents, 111 anchors, 1680 references, 112 beans, 43 graph edges, 47 selectable, 112 bean ids, 2 introduced, 110 on origin/main, 1 closing transitions, 6 criteria checked, 0 unnumbered.
exit: 0
tree after the revert: []
```

The same three plants at the `3b02871` analyser, swapped in by copy and restored from a
pristine copy taken first, so the rejections above are shown to come from the change and not
from the plants being malformed:

```
$ bash <scratch>/plant-before.sh
HEAD 5a625fc
analyser swapped to the 3b02871 form; tree: [ M tools/lib/docs-lint-c14.awk;]

########## PLANT 1 — region, at 3b02871
docs-lint: OK — [...] 1 closing transitions, 3 criteria checked, 0 unnumbered.
exit: 0

########## PLANT 2 — emptiness, at 3b02871
docs-lint: OK — [...] 1 closing transitions, 5 criteria checked, 0 unnumbered.
exit: 0

########## PLANT 3 — evidence cell, at 3b02871
docs-lint: OK — [...] 1 closing transitions, 3 criteria checked, 0 unnumbered.
exit: 0

analyser restored; tree: []
docs-lint: OK — 19 documents, 111 anchors, 1679 references, 111 beans, 43 graph edges, 47 selectable, 111 bean ids, 1 introduced, 110 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
```

The negative controls are the fourth plant above — one bean carrying every shape that must
still close: a citing sub-heading inside the evidence region with prose under it, one with a
fenced transcript under it, an unnumbered evidence row citing a range from its FIRST cell, a
numbered evidence row, and a top-level section devoted to one criterion. It reports
`1 closing transitions, 6 criteria checked` and exit 0. Five further controls are asserted in
`tools/docs-lint-test.sh` and are named under criterion 2.

**What this bean's `## Observed` section got wrong, corrected rather than worked around.** Its
region section writes the refused shape once as a `##` heading — `## Criterion 3 was not
attempted` — and says it is refused *for standing outside the evidence region*. A `##`
heading cannot stand outside the evidence region in that sense, because `region` is what a
`##` heading SETS. The constraint that binds `##` headings too was built and measured, and it
takes criterion 7 off `bean:0038`, which is `completed` on `main` and closed on a
`## Criterion 7 is dropped, deliberately` section with the ruling and the reason under it.
That is a legitimately closed bean, so the narrowing is wrong and not the bean. The shipped
constraint exempts `## ` headings and binds everything else; the fixture this bean actually
measured — `###` sub-headings under `## Not in scope` — is refused either way.

### Criterion 2

Each constraint mutated on its own, against a suite that carries both fixtures. The mutation
is a named one-line edit to a copy of the analyser, reapplicable by hand, and the figures were
re-measured rather than quoted:

```
# one named edit to a copy of tools/lib/docs-lint-c14.awk each time, then
# bash tools/docs-lint-test.sh; the analyser is restored from a pristine cp after each
unmutated                    rc=0  docs-lint-test: 62 passed, 0 failed.
region off                   rc=1  docs-lint-test: 59 passed, 3 failed.
emptiness off                rc=1  docs-lint-test: 60 passed, 2 failed.
cell off                     rc=1  docs-lint-test: 59 passed, 3 failed.
bean:0121 deleted whole      rc=1  docs-lint-test: 55 passed, 7 failed.
citation scanner deleted     rc=1  docs-lint-test: 50 passed, 12 failed.
```

The failure SETS, which are what the criterion asks for — neither constraint may be landed on
the other's evidence:

```
########## region off
  verdict: a citing sub-heading outside the evidence region answers nothing
  citation text: and the region is what refuses them, not their shape
  citation text: the evidence cell is cut out of the row, and the rest of it is not
########## emptiness off
  verdict: a citing heading with nothing under it answers nothing
  verdict: a citing heading closed by a sibling heading still answers nothing
########## cell off
  verdict: pasted stdout in an evidence CELL no longer answers a criterion no row numbers
  verdict: sacrificed: a row's evidence cell no longer answers even when it cites honestly
  citation text: the evidence cell is cut out of the row, and the rest of it is not
```

`region off` kills neither emptiness assertion and `emptiness off` kills neither region one.
They meet only in the citation-text probes, which assert the mechanism directly. The two
probes are new: they read `citation_text()`'s own answer per line, because a verdict cannot
tell "the region refused this line" from "no citation was written there", and for a row it
cannot see the masked cell at all — the row still answers its own number through the
evidence-row path.

The third state matters as much as the first two. A narrowing, its ABSENCE and its DELETION
are three things, and this file's own header records that it once scored the last two alike.
`bean:0121 deleted whole` kills seven assertions and every one is a REJECTION; `citation
scanner deleted` kills twelve and every one is a CONTROL. Both figures are in the header of
`tools/docs-lint-test.sh` with the sets named.

### Criterion 3

What counts as content under a citing heading is **a non-blank line**, and the alternative
considered was `doc:05-authoring-for-agents#checks`'s ENTRY — a table row, a sub-heading or a
fenced block, a section of prose explicitly not one. Entry is the stricter reading of the same
document and it is rejected on measured cost, not on preference. The whole corpus, both forms
against `3b02871`:

```
########## v-entry vs shipped
110 compared, 1 differing
=== modus-0038--evidence-in-the-work-item.md (status: completed)
0a1
> UNANSWERED	7
```

`bean:0038` is `completed` on `main`. Its seventh criterion is answered by a top-level section
that rules the criterion dropped and gives the reason, in prose. An entry rule refuses that
section, and `doc:05-authoring-for-agents#checks` accepts it in as many words — a ruling with
its reason is an answer. A ruling in prose IS the evidence for a criterion that cannot be met;
a run's transcript is not, which is why the entry definition is right for `EMPTYEV` and wrong
here. The constraint shipped is `EMPTYCELL`'s analogue — nothing at all under the heading —
and not `HOLLOW`'s.

The choice costs something in the other direction and it is stated rather than hidden: a
citing heading over one line of hand-waving satisfies it. That is the same strength the
evidence-cell path has had since `bean:0055`, where a cell holding one word that is not an
evidence-kind name passes. Cell strength is `bean:0087` and this bean does not reach it.

### Criterion 4

Every bean in `.beans/` measured before and after, named and not counted. At `5a625fc` the
corpus is 111 beans, including the one this change raises:

```
$ bash <scratch>/corpus.sh <3b02871 analyser> <HEAD analyser>
111 compared, 1 differing
=== modus-0118--docs-lint-reports-ok-through-almost-every-runtime-failure.md (status: in-progress)
1a2
> UNANSWERED	1
2a4,7
> UNANSWERED	3
> UNANSWERED	4
> UNANSWERED	5
> UNANSWERED	6
```

**`modus-0118` is the only bean whose answered set changes, and no `completed` bean changes at
all.** `modus-0118` has no evidence section — its first output line is `NOEV`, before and
after — so its criteria were being answered by rows of its `## Success criteria` table, in
region `CRIT`, and the region constraint now refuses them. A bean with no evidence home
answers nothing, which is the correct reading; it is `in-progress`, it already could not
close, and it is being closed and split by another agent.

**What structurally protects the `completed` beans, rather than the observation that today's
corpus happens to be fine.** `tools/docs-lint.sh`'s check 14 does not iterate the corpus. It
iterates the beans this change TOUCHES — `git diff --name-only "$BASE" -- .beans` plus
`git ls-files --others` — and for each one it reads the status twice:

```
    now="$(sed -n 's/^status:[[:space:]]*//p' "$f" | head -1)"
    [ "$now" = "completed" ] || continue
    [...]
      was="$(git show "$BASE:$f" [...])"
    [...]
    [ "$was" = "completed" ] && continue
```

Two gates in series. A bean this change does not touch is never a candidate at all. A bean
that IS touched and was `completed` on the merge base is skipped by the second line, so the
analyser never runs on it — and check 11 independently forbids any change to such a bean other
than an appended `## Amendments` entry, so the only edits that can reach the second gate are
ones that cannot alter a criteria or evidence section. The set check 14 can judge is therefore
exactly *beans transitioning into `completed` in the change under review*, and a bean already
closed on `main` cannot re-enter it. That is the protection: not that the 38 `completed` beans
at `3b02871` pass today, but that they are not read.

The measurement above is still worth having, and `bean:0038` is why. It is protected and would
not fail — and it is the bean that showed two candidate forms of this change to be wrong. The
corpus differential is how a narrowing is judged, not how a build is kept green.

### Criterion 5

The three `ACCEPTED` verdict assertions in `tools/docs-lint-test.sh` at `3b02871`, and what
happened to each:

```
$ git show 3b02871:tools/docs-lint-test.sh | grep -n 'ACCEPTED:'
75:# `ACCEPTED: a Markdown table pasted inside <pre> is entered like any other` deliberately
1009:decides "ACCEPTED: a heading-shaped line inside <pre>, <details> or an HTML comment answers" \
1031:decides "ACCEPTED: a Markdown table pasted inside <pre> is entered like any other" \
1054:decides "ACCEPTED: pasted stdout in an evidence CELL answers a criterion no row numbers" \
```

The third is **flipped to a rejection**. Its fixture is byte-identical to the one that stood
there and only the expectation changed, so the pin moved with the rule rather than being
deleted beside it. It keeps its position in the file, under a comment saying it was a pin.

The first two are **restated with the reason they remain accepted**, in the comment block above
them: they are the container residual, they need a perception layer the analyser does not have,
the argument against building it is `bean:0128`'s, and the three conditions this bean adds do
not reach them — the container in each fixture stands inside `## Evidence`, under a heading
with content, which is where evidence belongs. The `pandoc 3.7.0.2` check of the second half
of that argument — a `#` heading inside `<details>` with blank lines around it is a heading —
was re-run and holds. Both pins name `bean:0128` and move when it does.

### Criterion 6

`doc:05-authoring-for-agents#checks` carries the three conditions as a table beside the
structural-site rule, and the paragraph this bean was named in is rewritten rather than left
beside them. `git diff --stat origin/main...HEAD -- documentation/`:

```
$ git diff --stat origin/main...HEAD -- documentation/
 documentation/05-authoring-for-agents.md | 82 +++++++++++++++++++++-----------
 1 file changed, 54 insertions(+), 28 deletions(-)
```

Four paragraphs changed, not one appended. The "converse is not checked" paragraph now reads
"is now checked" and says what the analogue is. The "at column zero is a qualifier and it has
a price" paragraph is rewritten in the past tense with the cell condition named as what closed
it. The "what the rule is not" paragraph hands the container residual to `bean:0128`. Check 14's
row in the table names the three conditions. Line count after: 391, inside `adr:0003`'s 500.

### Criterion 7

No polarity is read and no rejected string is enumerated. The complete set of string and
pattern literals the change introduces into `tools/lib/docs-lint-c14.awk`:

```
/^## /            a heading level, not a word
/^#+ /            a heading, unchanged from bean:0093
"|"               the field separator a row is split on
/^[ \t\r]*$/      blankness
"EV", "BOTH"      the analyser's own region values, already tracked
```

None is a word that could appear in a bean, so there is nothing for an author to avoid saying
and nothing for the next unfamiliar phrasing to walk past. The matcher itself — the `criteri`
regex and its range arithmetic — moved into `scan()` unchanged; the only difference is which
array it writes into.

The three conditions are each stated as a positive property of where a citation stands rather
than as a subtraction of shapes it may not stand in, which is the direction
`doc:00-constitution#mechanical-enforcement` requires. All three fail closed: a citation that
does not satisfy them is not read and its criterion is reported `UNANSWERED`.

### Criterion 8

The cell condition is `do not read a citation out of the evidence column of a row`, and it
applies to **every** row, not only to one that numbers itself. Both forms were built and
measured over the whole corpus:

```
########## v-numbered vs shipped
110 compared, 0 differing
```

The corpus does not choose between them, so the reasoning has to. The narrow form closes the
shape this bean measured — a numbered row's cell — and leaves the identical laundering one
column over, in the evidence cell of an UNNUMBERED row: the same machine-generated string, the
same site, nothing written by hand. That is the case that decides it.

**The case sacrificed, and it is a real one.** A row that records a run and names, in its
evidence cell, a span of criteria that run genuinely covers no longer answers that span. It is
pinned as a verdict in `tools/docs-lint-test.sh` under the name `sacrificed: a row's evidence
cell no longer answers even when it cites honestly`, on a fixture whose only evidence row
numbers itself 3 and whose cell honestly names the other two; both come back `UNANSWERED`. The
example is described here and written out only inside that file, for the reason this bean's
own eighth criterion gives.

What the author does instead is one cell to the left: the span goes in the row's first cell,
where a row says what it is about, and the evidence cell holds the run. That shape is a
control in the same file and it still closes. The asymmetry is the whole of the argument — the
evidence cell is where output is PASTED, and this check's own stdout is what gets pasted there;
the first cell is written by an author deciding something.

This bean's `## Observed` section names the corpus instance at
`.beans/modus-0055--evidence-required-to-close-a-bean.md:123` and says the row is harmless
because the rows below it answer their own criteria. That reading is right about the outcome
and imprecise about the mechanism: the numbers that survive the mask on that row are in its
COMMAND column, not its evidence column, so the row keeps answering a span even with the cell
cut out. `modus-0055` is unchanged in the corpus differential above for both reasons at once.

### Criterion 9

```
$ ./gradlew qualityCheck
[...]
docs-lint: OK — 19 documents, 111 anchors, 1679 references, 111 beans, 43 graph edges, 47 selectable, 111 bean ids, 1 introduced, 110 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
[...]
docs-lint-test: 62 passed, 0 failed.
[...]
docs-lint-gate-test: 11 passed, 0 failed.
[...]
BUILD SUCCESSFUL in 29s
170 actionable tasks: 57 executed, 113 from cache
```

`0 closing transitions` is expected and is not a gap: `doc:00-constitution#bean-lifecycle`
holds a bean `in-progress` for the whole life of its own pull request, so this bean is not a
candidate on its own change and check 14 examines no bean's evidence here (`bean:0096`). The
plants above are what exercise it, and the fourth of them is a closure that passes.

## Not in scope

- The polarity blindness itself. It is accepted at `bean:0093` with its reasoning, and both
  constraints above are chosen precisely because neither requires reading polarity.
- The fence classifier (`bean:0063`), which is a separate perception layer and is not what
  decides any of the three shapes above.
- The composition of fence parity with the citation matcher (`bean:0099`). That bean owns two
  mechanisms interacting; this one owns three under-constraints of a single mechanism.
- What an evidence *cell* must hold (`bean:0087`). `EMPTYCELL` and `HOLLOW` are named above
  only as the analogue the heading path lacks.
