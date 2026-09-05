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

**The review round has its own head, `2bcd9aa`,** and every figure it produced names it: the
mutation table under criterion 2 and the whitespace pair beside it, the region measurement
under criterion 6, the two row-shape bypasses and the `modus-0055` probe under criterion 8, the
row-shape corpus differential under criterion 4, `PLANT 2`'s re-capture, and `qualityCheck`
under criterion 9. The figures taken at `5a625fc` are left where they are and not restated at
`2bcd9aa`: a figure is a record of a run, and re-labelling one is the failure this bean's own
evidence rules exist to prevent.

**The SECOND review round has its own head, `eabd009`,** and every figure it produced names it:
the seam measurement and the `SUBSEP` measurement under criterion 8, the whole mutation table
re-taken under criterion 2, the corpus differential under criterion 4, the portability figures
under criterion 8, and `qualityCheck` under criterion 9. It found one defect, in the evidence
cell mask this bean added — **the mask deleted the cell instead of replacing it**, which spliced
the cut cell's two neighbours together and made the mask ANSWER a criterion cited from no cell
of the file. The round is recorded under criterion 8 with the constraint it belongs to; the
figures the first round took are left where they are and not restated.

**All four residuals still reproduce at `3b02871`**, unchanged, over a corpus that has grown
to 110 beans. Two details of the `## Observed` section did not survive re-measurement and are
corrected under criterion 1 and criterion 8 below rather than quietly worked around.

The container residual is split out as `bean:0129`, which is **the second id this branch
allocated for it**. It was raised as `0128`, and PR #78 — opened before this one, closing
`bean:0123` — had allocated `0128` for a different finding. `.beans/` is the id allocator, it
is read at branch time and nothing serialises two readers, so both branches were right within
their own tree; the later allocator yields (`bean:0051`, `AGENTS.md`). Renamed here rather
than left for check 13 to find after one of the two merges.

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
=== r4-container     the residual this bean did NOT close, carried to `bean:0129`
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
[...]
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

**PLANT 2's five lines, elided above, re-taken as a capture.** In the run above they were the
one figure in this bean that was a characterised summary rather than the tool's bytes — "five
FAIL lines, one per criterion, identical in form to the two above" — which is a claim about
output and not output. Re-planted and re-captured at `2bcd9aa`, the head this bean's review
round produced. The plant is the same untracked bean, reverted with `rm`:

```
$ bash <scratch>/plant2/plant.sh
HEAD 2bcd9aa

########## PLANT 2 — emptiness
tree before the plant: []
FAIL check 14 .beans/modus-9902--plant-2-the-emptiness-residual.md: criterion 1 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
FAIL check 14 .beans/modus-9902--plant-2-the-emptiness-residual.md: criterion 2 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
FAIL check 14 .beans/modus-9902--plant-2-the-emptiness-residual.md: criterion 3 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
FAIL check 14 .beans/modus-9902--plant-2-the-emptiness-residual.md: criterion 4 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
FAIL check 14 .beans/modus-9902--plant-2-the-emptiness-residual.md: criterion 5 is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)
docs-lint: 5 failure(s).
exit: 1
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
`1 closing transitions, 6 criteria checked` and exit 0. The same shapes are asserted
individually in `tools/docs-lint-test.sh` — `control: prose under a citing heading is
content`, `control: a fenced block under a citing heading is content`, `control: a deeper
heading under a citing heading is content`, `control: an evidence sub-heading naming the
criterion answers it`, `control: an evidence row citing a range of criteria answers all of
them`, `control: the identical line under a delimiter row IS a row, and answers`, and
`accepted: a top-level section devoted to a criterion answers it`. Every one of them is in the
`citation scanner deleted` failure set under criterion 2, which is what makes them controls
rather than fixtures that merely look like controls.

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

**Four completed beans write that shape, not one**, and the difference matters because
"`bean:0038` is the evidence for the exemption" is the shorter sentence and it is the one that
gets repeated. At `2bcd9aa`, the completed beans carrying a `## ` heading that cites a criterion
BY NUMBER are `bean:0038`, `bean:0049`, `bean:0051` and `bean:0063`. Binding `## ` changes
exactly one verdict:

```
$ bash <scratch>/h2scan.sh    # the strict variant vs shipped, over every bean
111 compared, 1 differing
=== modus-0038--evidence-in-the-work-item.md (status: completed)
  shipped: STATS	8	0
  strict:  UNANSWERED	7; STATS	8	0
```

So `bean:0038` is the bean that DEMONSTRATES the cost — the only one whose answered set moves —
and the other three are the corpus the exemption keeps working for. Naming one bean as "the
evidence" understates the reach of the exemption and overstates what that bean alone proves.

### Criterion 2

Each constraint mutated on its own, against a suite that carries both fixtures. The mutation
is a named one-line edit to a copy of the analyser, reapplicable by hand, and the figures were
re-measured rather than quoted:

```
# one named edit to a copy of tools/lib/docs-lint-c14.awk each time, then
# bash tools/docs-lint-test.sh; the mutation is applied to a MIRROR of tools/ under a
# private scratch root, so the tree under review is never written to and there is no
# pristine copy to restore from (bean:0102, and the commit that raised that rule here)
unmutated                    rc=0  docs-lint-test: 71 passed, 0 failed.
region off                   rc=1  docs-lint-test: 66 passed, 5 failed.
emptiness off                rc=1  docs-lint-test: 67 passed, 4 failed.
emptiness, whitespace-blind  rc=1  docs-lint-test: 70 passed, 1 failed.
cell off                     rc=1  docs-lint-test: 65 passed, 6 failed.
bean:0121 deleted whole      rc=1  docs-lint-test: 58 passed, 13 failed.
citation scanner deleted     rc=1  docs-lint-test: 54 passed, 17 failed.
```

The failure SETS, which are what the criterion asks for — neither constraint may be landed on
the other's evidence:

```
########## region off
  verdict: a citing sub-heading outside the evidence region answers nothing
  citation text: and the region is what refuses them, not their shape
  verdict: a citing sub-heading in the CRITERIA region answers nothing
  citation text: the evidence cell is cut out of the row, and the rest of it is not
  citation text: the escape is counted as cell content, not as a cell boundary
########## emptiness off
  verdict: a citing heading with nothing under it answers nothing
  verdict: a citing heading closed by a sibling heading still answers nothing
  verdict: a whitespace-only line under a citing heading is not content
  verdict: of two adjacent citing headings, only the second is answered
########## emptiness, whitespace-blind
  verdict: a whitespace-only line under a citing heading is not content
########## cell off
  verdict: pasted stdout in an evidence CELL no longer answers a criterion no row numbers
  verdict: sacrificed: a row's evidence cell no longer answers even when it cites honestly
  citation text: the evidence cell is cut out of the row, and the rest of it is not
  verdict: a row with no trailing pipe still has its evidence cell cut
  verdict: an escaped pipe in a cell does not misalign the cut
  citation text: the escape is counted as cell content, not as a cell boundary
```

The three sets as the measurement gives them, rather than rounded to "disjoint apart from the
probes" — which is not true of `emptiness off`, and was the wording in this file's first draft:

```
region off     2 region verdicts + ALL THREE citation-text probes
emptiness off  4 emptiness verdicts + NO probe at all
cell off       4 cell verdicts + the two `texts` probes (not the `reads` map)
```

So `region` and `emptiness` are disjoint, `emptiness` and `cell` are disjoint, and `region` and
`cell` meet in exactly the two `texts` probes — the assertions that print what survives a row's
mask, which both mechanisms decide between them. `emptiness` touches no probe because the
pending buffer is downstream of `citation_text()` and changes nothing that function returns.
That is what the criterion asks for: neither of the first two constraints is landed on the
other's evidence, and no pair is held up by a single shared assertion.

The probes are new: they read `citation_text()`'s own answer per line, because a verdict cannot
tell "the region refused this line" from "no citation was written there", and for a row it
cannot see the masked cell at all — the row still answers its own number through the
evidence-row path.

**`emptiness` has two halves, and only one of them was asserted when this bean was first
reviewed.** `Content is a non-blank line` is written `line !~ /^[ \t\r]*$/`, and both emptiness
fixtures terminated their section with a genuinely zero-length line — EOF in one, a sibling
heading in the other — so nothing exercised the character class. Relaxing the test to
`line != ""` scored `62 passed, 0 failed, rc=0` at `58210d6` against the suite as it then
stood, while a five-criterion bean whose whole `## Evidence` is `### Criteria 1-5` followed by
ONE SPACE closed green:

```
$ awk -v KINDS="…" -f tools/lib/docs-lint-fence.awk -f tools/lib/docs-lint-c14.awk ws.md
# shipped, at 2bcd9aa
UNANSWERED	1
UNANSWERED	2
UNANSWERED	3
UNANSWERED	4
UNANSWERED	5
STATS	5	0
# mutant, `line != ""`
STATS	5	0
```

That is the `intable sticky` blindness one level down — the same failure this bean's own file
names as "the blindness `bean:0093`'s blocker entered through" — and it was found the same way,
by mutating a clause nobody had aimed a fixture at. The fixture added for it is the only
assertion the fourteenth mutation kills.

The third state matters as much as the first two. A narrowing, its ABSENCE and its DELETION are
three things, and this file's own header records that it once scored the last two alike.
`bean:0121 deleted whole` kills thirteen assertions and `citation scanner deleted` kills
seventeen; both sets are ENUMERATED member by member in the header of `tools/docs-lint-test.sh`
rather than characterised, because the characterisation that stood there was wrong in three
places at once — the sets were said to meet in "the two citation-text probes" when there are
three probes and five members in the meet, and each set was said to be purely controls or
purely rejections when neither is. A count of a set is the claim that goes stale without
anyone noticing.

**The whole table above expired at `eabd009`, and is re-taken rather than adjusted.** The
second review round added five assertions, so every figure this bean recorded is a reading of a
71-assertion suite and the suite has 76. The re-measurement is the same named edits applied to
the same mirror, run at `eabd009`:

```
none                   rc=0  docs-lint-test: 76 passed, 0 failed.
classifier-only        rc=1  docs-lint-test: 64 passed, 12 failed.
citation-site-only     rc=1  docs-lint-test: 55 passed, 21 failed.
scanner-deleted        rc=1  docs-lint-test: 57 passed, 19 failed.
b0121-deleted-whole    rc=1  docs-lint-test: 62 passed, 14 failed.
region-off             rc=1  docs-lint-test: 70 passed,  6 failed.
emptiness-off          rc=1  docs-lint-test: 72 passed,  4 failed.
cell-off               rc=1  docs-lint-test: 69 passed,  7 failed.
whitespace-blind       rc=1  docs-lint-test: 75 passed,  1 failed.
no-intable             rc=1  docs-lint-test: 69 passed,  7 failed.
intable-sticky         rc=1  docs-lint-test: 75 passed,  1 failed.
allkinds-off           rc=1  docs-lint-test: 75 passed,  1 failed.
isevcol-true           rc=1  docs-lint-test: 74 passed,  2 failed.
isevcol-false          rc=1  docs-lint-test: 59 passed, 17 failed.
noevcol-forced         rc=1  docs-lint-test: 43 passed, 33 failed.
cut-deletes            rc=1  docs-lint-test: 72 passed,  4 failed.
```

Three things in that table are not arithmetic. **`allkinds-off` has moved off `rc=0`** — it was
the last mutation that could disable a mechanism outright behind a completely green suite, and
the row pin added this round kills it; `bean:0087` still owns the mechanism and the gap is
narrowed rather than closed, which is stated on those terms in `tools/docs-lint-test.sh`.
**`cut-deletes` is new** and is the defect this round found, described under criterion 8.
**`classifier-only` moved for a second reason**: the mutation as this bean described it has two
readings, and a toggle that maintains `FENCE_IN`/`FENCE_LINE` — so the kept
`fence_unterminated()` still reports — scores one failure fewer than one that does not. The
reading is now written down beside the figure instead of being re-derived each time.

The three constraint sets are re-enumerated in `tools/docs-lint-test.sh` at `eabd009`. What
changed in their SHAPE, which is what this criterion asks about rather than the counts: `region`
and `cell` now meet in THREE `texts` probes rather than two, because the round's probe is a
third one; `emptiness` still touches no probe; `region` and `emptiness` are still disjoint and
so are `emptiness` and `cell`. No pair is held up by a single shared assertion.

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
corpus is 111 beans, including the one this change raises.

**Re-taken at `eabd009`**, because the second review round changed what the mask writes and a
corpus figure is expired by default: `111 compared, 1 differing` against `494f174`, the same
single bean and no other, and `111 compared, 0 differing` against `5cf9c58` — the round moves
nothing in the corpus at all. Both runs are under criterion 8 with the change they measure. The
`5a625fc` figure below is left as it was taken:

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

**Where the risk actually lands, stated because this bean's PR body said otherwise.** It is not
that a too-aggressive narrowing "fails beans that were correctly closed and cannot be
corrected": check 14 cannot reach a bean that was `completed` on the merge base, which is the
two-gate argument above. The risk lands on beans *about to* close — a bean whose evidence is
written and whose closing pull request is open when the narrowing merges — and those are
correctable, by moving the citation to a site the narrowed rule reads. That is a real cost and
a smaller one, and it is the cost this differential measures.

**The row-shape fix found in review is verdict-neutral over the same corpus**, which is worth a
figure of its own because it changes what a row's cells ARE and could in principle have moved
any table in `.beans/`:

```
$ bash <scratch>/corpus.sh <58210d6 analyser> <2bcd9aa analyser>
111 compared, 0 differing
```

The corpus contains neither a data row without a trailing pipe nor an escaped pipe in an
evidence cell, so the fixtures are the whole of the evidence for that fix and the corpus says
nothing either way. Stated rather than left as a silent clean run.

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
the argument against building it is `bean:0129`'s, and the three conditions this bean adds do
not reach them — the container in each fixture stands inside `## Evidence`, under a heading
with content, which is where evidence belongs. The `pandoc 3.7.0.2` check of the second half
of that argument — a `#` heading inside `<details>` with blank lines around it is a heading —
was re-run and holds. Both pins name `bean:0129` and move when it does.

A **fourth** `ACCEPTED` assertion is added by this bean rather than inherited, and it is
labelled the same way for the same reason: an empty fenced block satisfies the emptiness
condition. That follows from an *entry* being a fenced block rather than the text inside one,
and from the OPEN delimiter being what commits a heading's pending citations. Nothing weaker
would be coherent — the analyser deliberately never reads inside a fence, so "is this fence
empty" is a question it has no business answering. It is documented in
`doc:05-authoring-for-agents#checks` and pinned as
`accepted: an EMPTY fenced block under a citing heading is still content`.

### Criterion 6

`doc:05-authoring-for-agents#checks` carries the three conditions as a table beside the
structural-site rule, and the paragraph this bean was named in is rewritten rather than left
beside them. `git diff --stat origin/main...HEAD -- documentation/`:

```
$ git diff --stat origin/main...HEAD -- documentation/
 documentation/05-authoring-for-agents.md | 95 ++++++++++++++++++++++----------
 1 file changed, 67 insertions(+), 28 deletions(-)
```

Re-taken at `eabd009`, after the second review round rewrote two of the document's claims:

```
$ git diff --stat origin/main...HEAD -- documentation/
 documentation/05-authoring-for-agents.md | 108 +++++++++++++++++++++++--------
 1 file changed, 80 insertions(+), 28 deletions(-)
```

**Two of the document's sentences were false and are corrected rather than softened.** *Each
failing closed* now reads *each written to fail closed*, because `cell` did not: the mask
manufactured an answer, which is under criterion 8. And *the rest of the row is read* now says
what is actually read — the rest of the row either side of a **barrier** standing where the cell
was — because the rest, spliced, is a different thing from the rest, and the difference was the
defect. A new paragraph states the replace-don't-delete rule and why the barrier has to be a
letter. The identical correction is made in `tools/lib/docs-lint-c14.awk`, which carried the
same flat `all three fail CLOSED` claim.

Four paragraphs changed on the first round, not one appended. The "converse is not checked" paragraph now reads
"is now checked" and says what the analogue is. The "at column zero is a qualifier and it has
a price" paragraph is rewritten in the past tense with the cell condition named as what closed
it. The "what the rule is not" paragraph hands the container residual to `bean:0129`. Check 14's
row in the table names the three conditions.

**The document promised a shape the gate refuses, and that half was found in review.** Two
places — check 14's row and the `region` row of the conditions table — said a citation is read
`inside a criteria or evidence section`. The code requires `EV` or `BOTH`; region `CRIT`, which
a bare `## Success criteria` heading sets, is refused exactly like `NONE`. Measured on the same
fixture at both heads — `### Criterion 2 cannot be met as written` with the ruling under it,
standing inside `## Success criteria`, with criterion 1 answered normally by an evidence row:

```
# base 3b028713b4c887cd0f2647c7dc12969cf5a2c68a
STATS	2	0
# head 58210d65f658b0ba61157184ef0d5bdba56d9a67, and unchanged at 2bcd9aa
UNANSWERED	2
STATS	2	0
```

This is the fail-open direction of a documentation error and it is the worse one: nothing in
the build goes red, an author follows the document, writes the sub-heading where it says to,
and the gate refuses their close with a message about evidence they believe they wrote. The
code is deliberate — `citation_text()`'s region clause aligns the citation path with the
numbered-row path beside it, which has required `EV` or `BOTH` since `bean:0045` — so **the
document is what is wrong**, and it now says "an evidence section, or a `## ` heading of its
own" in both places. The shape is pinned as a verdict, `a citing sub-heading in the CRITERIA
region answers nothing`, so the wording cannot drift back without the suite saying so.

Two smaller corrections in the same document. The `cell` row said the sacrificed span goes in
`the row's first cell`; in a table whose rows are numbered the first cell is the criterion
NUMBER, and a span written there stops the row being numbered — measured, and pinned in both
directions. And three edges of `emptiness` that were undocumented are now stated: a
whitespace-only line is blank, an empty fenced block is content, and of two adjacent citing
headings only the second is answered.

Line count after: 404, inside `adr:0003`'s 500.

### Criterion 7

No polarity is read and no rejected string is enumerated. The complete set of string and
pattern literals the change introduces into `tools/lib/docs-lint-c14.awk`:

```
/^## /            a heading level, not a word
/^#+ /            a heading, unchanged from bean:0093
"|"               the field separator a row is split on
/\\\|/            the CommonMark escape for a pipe INSIDE a cell, so that it is counted as
                  cell content and not as a cell boundary
/\|[ \t]*$/       a trailing pipe, so that the empty field after it is not counted as a cell
SUBSEP            awk's own separator, standing in for a masked escape
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

**The cut was one column wide and the row's columns were counted wrongly, twice.** The rule is
unconditional; `split(line, c, "|")` made it conditional on a row shape. Both bypasses were
found in review, both are the identical laundering string — this check's own stdout about
criteria no row numbers — in the evidence cell of a row that GFM and this analyser both read as
a row. Measured at `58210d6`, then again at `2bcd9aa` after the fix:

```
# | 3 | three | criterion 1 is not answered in the evidence; criterion 2 is not answered
#   a row with NO TRAILING PIPE: split() returns the evidence cell as the last field, so
#   `evcol < n` was false and the whole line was read
at 58210d6:   STATS	3	0
at 2bcd9aa:   UNANSWERED	1
              UNANSWERED	2
              STATS	3	0

# | 3 | three | a \| b criterion 1 is not answered; criterion 2 is not answered |
#   an ESCAPED PIPE, the documented way to put a pipe in a cell: split() counted it as a
#   delimiter, every field after it shifted, and the mask cut the column to its left
at 58210d6:   STATS	3	0
at 2bcd9aa:   UNANSWERED	1
              UNANSWERED	2
              STATS	3	0
```

Each row is still numbered 3, so criterion 3 is answered through the evidence-row path in every
run above — that is what stops the rejections being produced by a scanner that read nothing.
Both are fixed rather than pinned as residuals, because the document states the cell condition
unconditionally and a residual would make it conditional in prose too. `rowcells()` is the one
place a row's cells are decided, and the header scan and the `EMPTYCELL`/`HOLLOW` path use it
as well, so the two halves cannot drift apart. What survives the cut is asserted directly, not
inferred from an absence: `citation text: the escape is counted as cell content, not as a cell
boundary` reads `| 3 | three |z` and nothing else — at `2bcd9aa` it read `| 3 | three `, and the
`|z` is the barrier described in the next section, which did not exist then.

**`rowcells()` has three call sites and only one of them was pinned.** The cut is one caller;
the other two are the HEADER, where `evcol` is chosen, and the `EMPTYCELL`/`HOLLOW` bound below
it. Both moved from `split()` with the bound `i < n` to `rowcells()` with `i <= last` in this
bean, and no assertion saw either. Two fixtures pin them at `eabd009`, each stated as a verdict
DIFFERENCE against `494f174` because that is what a pin on a changed path is for:

```
# | # | criterion | evidence      <- header with NO TRAILING PIPE, then `| 3 | three |  |`
at 494f174:   NOEVCOL	Evidence
              STATS	3	0
at eabd009:   EMPTYCELL	3
              UNANSWERED	1
              UNANSWERED	2
              STATS	3	0

# | 3 | three | command           <- row with NO TRAILING PIPE, cell holding only a kind name
at 494f174:   UNANSWERED	1
              UNANSWERED	2
              STATS	3	0
at eabd009:   HOLLOW	3	command
              UNANSWERED	1
              UNANSWERED	2
              STATS	3	0
```

Both are pins on the CELL SELECTION and neither is a test of what `allkinds()` decides or of
what `isevcol()` recognises; `bean:0087` still owns those. The second one has a consequence
worth naming on its own: `allkinds-off` — HOLLOW detection disabled outright — scored `rc=0`,
`71 passed, 0 failed` against every earlier state of this suite and now scores `rc=1`,
`75 passed, 1 failed`. That was the last mutation in `tools/docs-lint-test.sh`'s table that
could delete a mechanism behind a completely green run, and it is `bean:0087`'s to hear about.

**THE MASK MANUFACTURED A CITATION, which is the defect the second review round found.** The
mask DELETED the evidence cell from the reconstructed row. Deleting it makes the cut cell's two
neighbours adjacent, and the matcher spans the seam:
`criteri(on|a)[^0-9a-z]*[0-9]+` skips any run of characters that are neither digit nor letter,
and the `|` the mask writes between surviving cells is one of them. The fixture is a
three-criterion bean whose evidence column is in the MIDDLE:

```
| # | claim | evidence | runs |
|---|---|---|---|
| 1 | covers both criteria | ran the suite | 3 runs |
```

```
########## awk
### 494f174 (base)
UNANSWERED	2
UNANSWERED	3
STATS	3	0
### 5cf9c58 (the branch as reviewed)
UNANSWERED	2
STATS	3	0
### 5cf9c58 with the cell replaced by SUBSEP
UNANSWERED	2
STATS	3	0
### eabd009 (CUTCHAR)
UNANSWERED	2
UNANSWERED	3
STATS	3	0
[... gawk and mawk agree line for line with awk on all four states ...]
```

The deleting form read `| 1 | covers both criteria | 3 runs `, matched `criteria | 3`, and
answered **the third criterion from a citation standing in no cell of the file**: the word is in
the claim column, the digit is in the runs column, and the evidence cell that separated them is
the one thing this condition says not to read. Fail-OPEN, in the mechanism whose stated property
is the opposite. Row 1 is numbered, so the first criterion is answered through the evidence-row
path in every run above — that is what stops the rejection being produced by a mask that reads
nothing.

**The barrier is a lowercase letter, and `SUBSEP` was measured not to do.** `SUBSEP` is what
`rowcells()` already uses for an escaped pipe and is the obvious choice for a character no
author wrote; the run above is why it is the wrong one. `\034` is neither a digit nor a
lowercase letter, so `[^0-9a-z]*` swallows it exactly as it swallows the `|` and the seam stays
open. The barrier has to be a character the matcher's own gap class EXCLUDES, and that class is
`[^0-9a-z]`, so it must come from `[0-9a-z]`; and it must not be a digit, or the cut would
supply the criterion NUMBER itself. That leaves the lowercase letters, and `CUTCHAR` is one. It
cannot cite on its own account — it holds no digit, and `criteri(on|a)` is a contiguous literal,
so a letter standing behind a `|` cannot complete one.

**Why 71 assertions could not see it.** Every table in `tools/docs-lint-test.sh` — seventeen
delimiter rows — put its evidence-ish column LAST, so no cut in the file had a right-hand
neighbour and a mask that deletes and one that replaces were indistinguishable to all of them.
That file already named this blind spot one mechanism over, for `isevcol-TRUE`: *the probe
fixture's `evidence` column is the LAST column*. It was not carried across. The rejection added
this round puts the column in the middle, with a control that the rest of a middle-cut row is
still read and a probe that prints the barrier standing between the neighbours; the same fixture
incidentally makes `isevcol-TRUE` move the mask for the first time, which is recorded as
incidental in that file rather than claimed as coverage.

**The fix moves no bean.** The whole corpus, `5cf9c58` against `eabd009`, same command:

```
111 compared, 0 differing  (5cf9c58 vs CUTCHAR)
```

So the defect was latent and not live, and the barrier is not a behaviour change to anything in
`.beans/` — which is the reason it needed a fixture rather than a corpus run to find. Against
`494f174` the branch still moves exactly the one bean named under criterion 4 and no other:

```
DIFFERS modus-0118--docs-lint-reports-ok-through-almost-every-runtime-failure.md
111 compared, 1 differing  (494f174 vs CUTCHAR)
```

Portability, at `eabd009`, because the barrier is a new character in a string every awk builds:

```
awk                          docs-lint-test: 76 passed, 0 failed.
gawk                         docs-lint-test: 76 passed, 0 failed.
mawk                         docs-lint-test: 76 passed, 0 failed.

111 beans, 0 differing across awk/gawk/mawk
```

**The case sacrificed, and it is a real one.** A row that records a run and names, in its
evidence cell, a span of criteria that run genuinely covers no longer answers that span. It is
pinned as a verdict in `tools/docs-lint-test.sh` under the name `sacrificed: a row's evidence
cell no longer answers even when it cites honestly`, on a fixture whose only evidence row
numbers itself 3 and whose cell honestly names the other two; both come back `UNANSWERED`. The
example is described here and written out only inside that file, for the reason this bean's
own eighth criterion gives.

What the author does instead is any **other** column: the cut is one column wide, so the span
goes wherever the row says what it is about and the evidence cell holds the run. Not
necessarily the first cell — in a table whose rows are numbered, the first cell is the
criterion NUMBER, and a span written there stops the row being numbered and costs it its own
criterion. Both halves are pinned as verdicts in `tools/docs-lint-test.sh`, under
`control: an honest span in a non-evidence column answers, and the row keeps its own number`
and `the same span in the FIRST cell costs the row its own criterion`. The asymmetry is the
whole of the argument — the evidence cell is where output is PASTED, and this check's own
stdout is what gets pasted there; every other column is written by an author deciding
something.

**A correction in this bean was itself wrong, and this paragraph replaces it rather than
deleting it.** This bean's `## Observed` section names the corpus instance at
`.beans/modus-0055--evidence-required-to-close-a-bean.md:123` and says the row is harmless
because rows 4 and 5 of that table answer their own criteria. A later paragraph here called
that reading "right about the outcome and imprecise about the mechanism", and asserted that
the surviving numbers on row 123 are in its **command** column, so the row keeps answering a
span even with the cell cut out. Both halves of that assertion are false. Probing the row with
the shipped `evcol`, mask and matcher, at `2bcd9aa` — `citation_text()` and `scan()` loaded
after the analyser, so `intable` and `evcol` are the values the real scan held on that line:

```
$ awk -v KINDS="…" -f tools/lib/docs-lint-fence.awk -f tools/lib/docs-lint-c14.awk \
    -f probe0055.awk .beans/modus-0055--evidence-required-to-close-a-bean.md
n=7  evcol=6  intable=1
whole line             hits: 4 5
masked (citation_text) hits:  <none>
col 2                  hits:  <none>
col 3                  hits:  <none>
col 4                  hits:  <none>
col 5                  hits:  <none>
col 6                  hits: 4 5
STATS	11	0
```

Column 4 is the command column and it hits nothing: its text reads `criteria numbered 1-5`,
and ` numbered ` sits between the word and the digits, which the matcher's
`criteri(on|a)[^0-9a-z]*[0-9]+` does not cross. Column 6 is the evidence column, it carries
both numbers, and the mask cuts it — so **nothing at all** survives on that row. The row
answers only its own criterion 3, through the evidence-row path.

The **original** `## Observed` wording was therefore correct exactly as written: `modus-0055`
is unchanged in the corpus differential above because rows 4 and 5 are numbered and answer
themselves, and for no other reason. The amendment was not a refinement of it; it was a
second, wrong mechanism asserted beside a right conclusion, and it survived because the
conclusion it sat next to was true. This corpus has now produced several self-corrections that
were themselves defects, which is why the wrong one is recorded here instead of quietly
reverted: a correction is a claim, and `doc:00-constitution#observed-failing` binds a claim to
a measurement whoever makes it and whichever direction it points.

### Criterion 9

Taken at `cd8f3f9`, the last commit that changes any file this gate reads other than this
paragraph. The figure that stood here was taken at `5a625fc`, five commits behind the head
under review, which makes it a statement about a tree nobody would merge; it was re-taken at
`2bcd9aa`, at `112e201`, and again here, because the bean's own prose is an input to `docs-lint`
and its reference count moves whenever this file does.

```
$ ./gradlew qualityCheck
[...]
bash-compat: OK — 4 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
[...]
docs-lint-test: 76 passed, 0 failed.
[...]
docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)
docs-lint-gate-test: analyser awk — awk version 20200816
[...]
docs-lint: OK — 19 documents, 111 anchors, 1692 references, 111 beans, 43 graph edges, 47 selectable, 111 bean ids, 1 introduced, 111 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
[...]
docs-lint-gate-test: 11 passed, 0 failed.
[...]
BUILD SUCCESSFUL in 26s
161 actionable tasks: 7 executed, 154 up-to-date
```

The analyser is the same under all three awks a CI runner might supply, which is worth a figure
of its own because `rowcells()` introduces this file's first `\\` inside a regex — and now
because `CUTCHAR` puts a character of the check's own into every row it reads. Re-taken at
`cd8f3f9`; the file count moved because the corpus and the document set did, not the verdicts:

```
$ bash <scratch>/awkport.sh    # same analyser, three awks, every bean and document
126 files compared across bsd awk / gawk / mawk, 0 differing
$ bash <scratch>/suite-awks.sh # the whole assertion suite under each, via a PATH shim
awk                          docs-lint-test: 76 passed, 0 failed.
gawk                         docs-lint-test: 76 passed, 0 failed.
mawk                         docs-lint-test: 76 passed, 0 failed.
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
