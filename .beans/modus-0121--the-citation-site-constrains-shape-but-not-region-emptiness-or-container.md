---
# modus-0121
title: A citing heading answers a criterion from anywhere in the file, with nothing under it, and from inside a raw HTML block; and an evidence row answers from its own cell
status: completed
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
nothing in the corpus at all. Both runs are under criterion 8 with the change they measure.

**`111` is this BRANCH's corpus and CI reports `112`, and the difference is not a stale count.**
The merge base is `3b02871`; `main` has moved to `494f174` since, which added
`.beans/modus-0128`. So the `push` run measures the branch and prints 111 beans, and the
`pull_request` run measures `refs/pull/79/merge` and prints 112. `modus-0128` is the whole of
the difference, and it gives a byte-identical verdict under the `494f174` analyser, under
`5cf9c58` and under `eabd009` — `NOEV`, eight `UNANSWERED` lines and `STATS 8 0` in all three —
so the post-merge corpus differential is the branch's with one unchanged row added. The
differentials below and under criterion 8 are figures about the branch, which is what a review
of the branch needs; the merged state is stated here rather than left as a discrepancy between
two numbers a reader can see.

The `5a625fc` figure below is left as it was taken:

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

**Why 71 assertions could not see it.** At `5cf9c58` every table in `tools/docs-lint-test.sh`
that carried an evidence-ish column — `evidence`, `observed`, `output` or `result` — had it as
the LAST column, without exception; the one table with no such column is the `evidence kind`
control. So no cut in the file had a right-hand neighbour, and a mask that deletes and one that
replaces were indistinguishable to all of them. That file already named this blind spot one
mechanism over, for `isevcol-TRUE`: *the probe fixture's `evidence` column is the LAST column*.
It was not carried across. The rejection added
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

Taken at `1556d02`, the last commit that changes any file this gate reads other than this
paragraph. The figure that stood here was taken at `5a625fc`, five commits behind the head
under review, which makes it a statement about a tree nobody would merge; it has been re-taken
at every head since that changed a file the gate reads — `2bcd9aa`, `112e201`, `cd8f3f9`,
`f65fb4a`, `27e7652` and here — because the bean's own prose is an input to `docs-lint` and its
reference count moves whenever this file does.

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

## Closing evidence — merged as PR #79, squashed onto `main` as `277c4d5`

A bean cannot close itself, so this is the next change (`doc:00-constitution#bean-lifecycle`).

**The criteria are not restated below, and none is reworded.** A close that rewrites its
criteria is indistinguishable from a close that met them (`bean:0113`), so the table below
indexes `## Success criteria` by number and records a verdict against the wording already
standing there. The `status:` line is the only edit this change makes outside this section;
where a figure taken above has expired, it is corrected in Block G rather than edited where it
stands.

**All nine close met, and the one place a reader might reach a different verdict is named
rather than left to be found.** It is the second half of the first criterion's wording, and it
is Block G's first entry. `doc:80-agent-operating-procedure#self-validate` step 6 forbids
weakening a criterion to reach green, so the reading that verdict rests on is written out where
it can be disagreed with, instead of being applied silently.

The heads in play. Every figure below was redirected to a file and pasted from that file,
carries `[...]` on every elision, and is stamped with the head and the interpreter that
produced it. Two bash interpreters are installed here and they are not interchangeable:
`/bin/bash` is 3.2.57(1)-release and is what `build.gradle.kts`'s `gateShell` invokes by
absolute path, and `bash` on `PATH` is 5.3.9(1)-release. Every shell figure below was produced
by `/bin/bash`.

- **`277c4d5`** — `origin/main`, this change's merge base, `.beans/` and `tools/` unmodified.
  Blocks A, B, C, D, and the first arm of Block E.
- **`277c4d5`+`status:`** — the merge base with this bean's `status:` line as the only edit and
  this section absent. Block E's second arm.
- **`277c4d5`+`status:`+this section, with Block F's fence replaced by a one-token placeholder
  line** — the gate run. Every other line of this section was present when it ran.

| # | verdict | observed |
|---|---|---|
| 1 | met | The three fixtures are rejected at the closing head and were accepted by the `3b02871` analyser on the same command: two `UNANSWERED` lines for the region fixture, five for the emptiness one, one for the cell one. The control this criterion names — a citing sub-heading with content under it, and an evidence row citing a range from the column that is not its evidence cell — closes at the head this branch merges as and closed before it, `STATS 3 0` in both arms. Block B, read with Block G's first entry, which states what the second half of this row's wording turns on |
| 2 | met | Each constraint mutated on its own and re-measured rather than inherited: `70 passed, 6 failed`, `72 passed, 4 failed`, `69 passed, 7 failed`, against `76 passed, 0 failed` unmutated. The failure sets are enumerated member by member, and neither of the first two is landed on the other's evidence. Block C |
| 3 | met | The choice — a non-blank line, and not an *entry* — is stated with its measured cost in `tools/lib/docs-lint-c14.awk` beside `pend_commit()` and in `doc:05-authoring-for-agents#checks`'s emptiness row, and that cost is a named bean rather than a count. Block D for the corpus half |
| 4 | met | Every bean in `.beans/` measured before and after, at the merged head and again at the rebased base: `112 compared, 1 differing`, then `112 compared, 2 differing` once PR #80 landed. Every differing bean is named, both are `in-progress`, and no `completed` bean moves in either run. Block D |
| 5 | met | The pasted-cell pin is a rejection at the closing head and keeps its position in the file; the two container pins are `ACCEPTED` with `bean:0129` named as their reason; a fourth is added for the empty fence. `cell-off` in Block C kills the flipped one, which is what makes it a pin and not a comment. Block C |
| 6 | met | `doc:05-authoring-for-agents#checks` carries the three conditions as a table beside the structural-site rule, states the `## ` exemption and names `bean:0038`, `bean:0049`, `bean:0051` and `bean:0063` as the beans that write it, and hands the container residual to `bean:0129`. Block G's second entry corrects the one figure that section leaves stale |
| 7 | met | No polarity is read. The mutation in Block C that deletes the region clause changes which SITES are read and not which CLAIMS are, and each of the three conditions is a positive property of where a citation stands. The literal set the change introduces is enumerated in this bean's seventh evidence section, and none of it is a word an author would write |
| 8 | met | The sacrificed case is not merely described, it is pinned: `cell-off` in Block C kills `sacrificed: a row's evidence cell no longer answers even when it cites honestly` alongside the laundering rejection, so both halves of the trade fail together and neither can be quietly dropped later. Block C |
| 9 | met | `./gradlew qualityCheck` green on this closing branch, re-run and not reused: an earlier run measured a different tree (`doc:80-agent-operating-procedure#self-validate`). PR #79's own `gate` job is `pass` on the pull request whose merge commit is `277c4d5`. Block F |

### Block A — what merged, read from the commit rather than from this bean

```
$ git show 277c4d5 --stat --format='%h %s' > [...]/merged-stat.txt   # /bin/bash 3.2.57
277c4d5 fix(docs-lint): narrow where a criterion citation counts (#79)

 ...-shape-but-not-region-emptiness-or-container.md | 900 ++++++++++++++++++-
 ...de-a-raw-html-block-is-still-a-citation-site.md |  96 ++
 AGENTS.md                                          |   6 +
 documentation/05-authoring-for-agents.md           | 108 ++-
 tools/docs-lint-test.sh                            | 997 +++++++++++++++++++--
 tools/lib/docs-lint-c14.awk                        | 220 ++++-
 6 files changed, 2201 insertions(+), 126 deletions(-)
```

```
$ GITHUB_TOKEN= gh pr view 79 --json number,title,mergeCommit,mergedAt,state \
    --jq '{number, title, state, mergedAt, merge: .mergeCommit.oid}'
{"merge":"277c4d57264d938b3be894400a038178f93c5761","mergedAt":"2026-09-05T00:16:33Z","number":79,"state":"MERGED","title":"fix(docs-lint): narrow where a criterion citation counts"}
```

The `--stat` is git's own abbreviation of the two bean paths and is not an elision of mine. The
six files are this bean, the bean the container residual was split into, the routing note in
`AGENTS.md`, the document, the assertion suite and the analyser.

**The merged tree is the reviewed tree**, which is what lets every figure below stand as a
figure about the reviewed change as well as about the merge. `git diff --stat 1556d02 277c4d5`
reports three bean files and nothing else: `.beans/modus-0123` and `.beans/modus-0128`, which
are PR #78's and arrived on `main` while this branch was open, and nine lines of this bean's
own ninth evidence section. No file any gate analyses differs between the last reviewed head
and the merge.

### Block B — the four residuals, at the merged head and at the analyser it replaced

Five fixtures, in one script, through two analysers. Fixtures and not bean files, so no plant,
no revert and no `git checkout -- .beans` is involved (`bean:0102`, `bean:0116`). The `before`
analyser is `3b02871`'s, extracted with `git show <ref>:<path> > <scratch-file>` — which reads
the index and never writes to the tree — and not restored from a copy taken earlier in the
session, which would restore the file as it was THEN (`AGENTS.md`). `KINDS` is the value
`tools/docs-lint.sh` passes.

```
$ /bin/bash [...]/run-fixtures.sh <this worktree> > [...]/fixtures-at-277c4d5.txt
head 277c4d5
interpreter 3.2.57(1)-release
=== fx-region
UNANSWERED	2
UNANSWERED	3
STATS	3	0
exit: 0
=== fx-emptiness
UNANSWERED	1
UNANSWERED	2
UNANSWERED	3
UNANSWERED	4
UNANSWERED	5
STATS	5	0
exit: 0
=== fx-cell
UNANSWERED	3
STATS	3	0
exit: 0
=== fx-cell-control
UNANSWERED	3
STATS	3	0
exit: 0
=== fx-container
STATS	4	0
exit: 0
```

```
$ /bin/bash [...]/run-fixtures.sh <the 3b02871 analyser, extracted to scratch>
fatal: not a git repository (or any of the parent directories): .git
head 
interpreter 3.2.57(1)-release
=== fx-region
STATS	3	0
exit: 0
=== fx-emptiness
STATS	5	0
exit: 0
=== fx-cell
STATS	3	0
exit: 0
=== fx-cell-control
UNANSWERED	3
STATS	3	0
exit: 0
=== fx-container
STATS	4	0
exit: 0
```

The `fatal:` line and the empty `head` are the script's own head stamp failing: the scratch
directory holding the extracted analyser is not a git repository. It is left in rather than
tidied away, because a stamp that could not be taken is worth more visible than hidden — the
head is `3b02871`, named by the `git show` that produced the files and not by the run.

Read the pair rather than either half. Three fixtures move from accepted to rejected, and the
`STATS N 0` line is present in both runs of each, so the criteria were counted either way and
the rejections come from the citation rule rather than from a fixture the analyser could not
parse.

**`fx-cell-control` says something sharper after the change than before it.** At `3b02871` the
control differs from `fx-cell` — the pasted string was answering the third criterion, and
removing it left that criterion unanswered. At `277c4d5` the two runs are byte-identical, which
is the claim in its strongest form: the evidence cell now contributes nothing in either
direction.

**`fx-container` is unchanged in both runs, and that is why it is here.** The container model —
the fourth residual this bean recorded — is not closed by what merged, and it was never one of
this bean's criteria; it is `bean:0129`, which carries these fixtures in its own `## Observed`.
A heading-shaped line inside `<pre>`, inside `<details><pre>` and inside an HTML comment still
answers at the merged head, because the three conditions this bean shipped do not reach it: the
container in that fixture stands inside `## Evidence`, under a heading that has content, which
is where evidence belongs.

**The control this criterion names, and why Block B did not carry one until review said so.**
Neither fixture above is it. `fx-cell-control` reports `UNANSWERED 3` in BOTH arms — it is a
control that the evidence cell contributes nothing, not a control that a bean closes — and
`fx-container` closes but is the residual this bean did not fix. So Block B evidenced the
rejections and left the second half of this criterion's wording resting on the named assertions
in the first evidence section above, which were taken at `5a625fc`: a different head, and
reusing it is what `doc:80-agent-operating-procedure#self-validate` says not to do. The control
is therefore re-taken here, at the head this branch merges as. It carries both shapes the
criterion names — a citing sub-heading inside the evidence region with a paragraph under it,
and an evidence row citing a range from the column that is NOT its evidence cell — over a bean
numbering three criteria:

```
$ /bin/bash [...]/run-control.sh <this worktree> > [...]/control-at-7731d13.txt
head 83c64bf, base 7731d13
interpreter 3.2.57(1)-release
=== fx-control  (shipped analyser)
STATS	3	0
exit: 0
=== fx-control  (3b02871 analyser)
STATS	3	0
exit: 0
```

No `UNANSWERED` line in either arm: the bean closes under the narrowed rule and closed under the
rule it replaced. **Closing identically in both arms is what makes it a control rather than a
fixture** — the narrowing cost it nothing, which is the claim, and a shape that only closed
after the change would be evidence of something else. The head differs from the rest of Block B
by design and is stamped rather than inherited; `git diff --stat 277c4d5 7731d13 -- tools/lib/`
is empty, so the analyser under this run is the one the rest of the block measured.

### Block C — each constraint mutated on its own, and the fail-open that closed

One named edit to a copy of `tools/lib/docs-lint-c14.awk` each time, applied to a MIRROR of
`tools/` under a private scratch root, so the tree under review is never written to and there
is no pristine copy to restore from. The diff against the tree is printed for each, so the edit
is not taken on trust, and the failing assertions are NAMED and never counted — a count of a
set is the claim that goes stale without anyone noticing, and this suite's own header records
three sentences that went stale that way.

```
$ /bin/bash [...]/mutate.sh <this worktree> > [...]/mutations-at-277c4d5.txt
head 277c4d5
interpreter 3.2.57(1)-release
########## none
rc=0 docs-lint-test: 76 passed, 0 failed.
########## region-off
192c192
<   if (line !~ /^## / && region != "EV" && region != "BOTH") { return "" }
---
>   if (0) { return "" }
rc=1 docs-lint-test: 70 passed, 6 failed.
  FAIL verdict: a citing sub-heading outside the evidence region answers nothing
  FAIL citation text: and the region is what refuses them, not their shape
  FAIL verdict: a citing sub-heading in the CRITERIA region answers nothing
  FAIL citation text: the evidence cell is cut out of the row, and the rest of it is not
  FAIL citation text: the barrier stands where the cut cell was, keeping its neighbours apart
  FAIL citation text: the escape is counted as cell content, not as a cell boundary
########## emptiness-off
335c335
<     if (scan(citation_text(line), P)) { pendlvl = lvl }
---
>     scan(citation_text(line), A)
rc=1 docs-lint-test: 72 passed, 4 failed.
  FAIL verdict: a citing heading with nothing under it answers nothing
  FAIL verdict: a citing heading closed by a sibling heading still answers nothing
  FAIL verdict: a whitespace-only line under a citing heading is not content
  FAIL verdict: of two adjacent citing headings, only the second is answered
########## cell-off
197c197
<     for (i = 2; i <= last; i++) { t = t "|" (i == evcol ? CUTCHAR : c[i]) }
---
>     for (i = 2; i <= last; i++) { t = t "|" c[i] }
rc=1 docs-lint-test: 69 passed, 7 failed.
  FAIL verdict: pasted stdout in an evidence CELL no longer answers a criterion no row numbers
  FAIL verdict: sacrificed: a row's evidence cell no longer answers even when it cites honestly
  FAIL citation text: the evidence cell is cut out of the row, and the rest of it is not
  FAIL citation text: the barrier stands where the cut cell was, keeping its neighbours apart
  FAIL verdict: a row with no trailing pipe still has its evidence cell cut
  FAIL verdict: an escaped pipe in a cell does not misalign the cut
  FAIL citation text: the escape is counted as cell content, not as a cell boundary
########## allkinds-off
244c244
<   return 1
---
>   return 0
rc=1 docs-lint-test: 75 passed, 1 failed.
  FAIL verdict: a row with no trailing pipe has its evidence cell examined, so a bare kind name is HOLLOW
```

The three constraint sets, as the run gives them rather than rounded. `region-off` kills two
region verdicts and all four citation-text probes; `emptiness-off` kills four emptiness
verdicts and no probe at all; `cell-off` kills four cell verdicts and three of the four probes.
So the first two are disjoint, the second and third are disjoint, and the first and third meet
in exactly three probes — the assertions that print what survives a row's mask, which both
mechanisms decide between them. No pair is held up by a single shared assertion.

**`allkinds-off` still scores `rc=1`, and that is confirmed here rather than inherited.** It
scored `rc=0`, `71 passed, 0 failed` against every state of this suite before this bean's second
review round, and was the last mutation in `tools/docs-lint-test.sh`'s table that could delete a
mechanism outright behind a completely green run. It holds at the merged head, killing the row
pin and nothing else. The gap is narrowed and not closed, and it is `bean:0087`'s: one kind name
in one cell shape is a pin on WHICH CELL is examined, not a test of what reading it decides.

The `none` arm is the mirror's own negative control. Without it a mutation table says only that
the suite can fail, not that these edits are what failed it.

### Block D — every bean in `.beans/`, before and after, at the closing head

The corpus differential re-taken, because a per-corpus figure is expired by default and this one
had already been taken three times on the branch. It is not a re-labelling: the corpus has grown
since the branch could measure it, and this is the first run of it that includes
`.beans/modus-0128`, which arrived on `main` from PR #78 after this bean's merge base was cut.

```
$ /bin/bash [...]/corpus.sh <this worktree> <3b02871 analyser> <277c4d5 analyser>
=== modus-0118--docs-lint-reports-ok-through-almost-every-runtime-failure.md (status: in-progress)
  1a2
  > UNANSWERED	1
  2a4,7
  > UNANSWERED	3
  > UNANSWERED	4
  > UNANSWERED	5
  > UNANSWERED	6
112 compared, 1 differing
```

`modus-0118` is named and not counted, and it is the same bean the branch measurement named. It
has no evidence section — its first output line is `NOEV`, before and after — so its criteria
were being answered by rows of its `## Success criteria` table, in region `CRIT`, and the region
constraint now refuses them. A bean with no evidence home answers nothing, which is the correct
reading; it is `in-progress` and already could not close. No `completed` bean moves.

The branch predicted this run and the prediction held: this bean's fourth evidence section
states that `modus-0128` gives a byte-identical verdict under all three analysers, so the
post-merge differential should be the branch's with one unchanged bean added. It is.

**The differing set GREW when this branch was rebased, and this criterion is the one that
said it would.** Its wording asks for every bean whose answered set changes to be named and
`stated without a count because the corpus grows`. The capture above is stamped at `277c4d5`
and stays as taken; re-run against the rebased base it reports a second bean:

```
$ /bin/bash [...]/corpus.sh <this worktree> <3b02871 analyser> <7731d13 analyser>
=== modus-0118--docs-lint-reports-ok-through-almost-every-runtime-failure.md (status: in-progress)
  1a2
  > UNANSWERED	1
  2a4,7
  > UNANSWERED	3
  > UNANSWERED	4
  > UNANSWERED	5
  > UNANSWERED	6
=== modus-0124--the-non-analyser-fail-open-boundary-in-docs-lint.md (status: in-progress)
  1a2,5
  > UNANSWERED	1
  > UNANSWERED	2
  > UNANSWERED	3
  > UNANSWERED	4
112 compared, 2 differing
```

`modus-0124` is PR #80's own bean, which grew by two thousand-odd lines in that pull request.
It is the same mechanism as `modus-0118` and not a new one: it prints `NOEV` in BOTH arms —
no evidence section at all — so its criteria were being answered by rows of its
`## Success criteria` table, in region `CRIT`, and the region constraint refuses them. A bean
with no evidence home answers nothing, which is the correct reading.

**No `completed` bean moves, and that is what the criterion turns on.** Both beans are
`in-progress`; neither can close, and check 14 never reads a bean that is not transitioning
into `completed`, so nothing goes red on either. What this does is land a real cost on
`bean:0124`'s eventual close — it will have to file its evidence under an evidence section
rather than cite from its criteria table. That is exactly the cost this bean's fourth evidence
section predicted and priced: the risk lands on beans *about to* close, and those are
correctable. It is named here rather than left for that author to discover.

**A count would have hidden this and a name did not.** Had the figure been carried as `1
differing` rather than re-derived, the rebase would have made it silently wrong; had the
criterion asked for a count instead of names, the second bean would have been a number moving
from 1 to 2 with nothing to check it against.

### Block E — `bean:0129` becomes selectable, and check 12 still sees an acyclic graph

Two arms of one command, and then the question of whether the green line under them means
anything.

```
$ /bin/bash tools/docs-lint.sh > [...]/base-lint.txt     # 277c4d5, working tree clean
docs-lint: OK — 19 documents, 111 anchors, 1729 references, 112 beans, 43 graph edges, 50 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit: 0
```

```
$ /bin/bash tools/docs-lint.sh > [...]/bare-flip.txt      # 277c4d5 + the `status:` line alone
docs-lint: OK — 19 documents, 111 anchors, 1729 references, 112 beans, 43 graph edges, 51 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 1 closing transitions, 9 criteria checked, 0 unnumbered.
exit: 0
```

**Three figures move and each says a different thing.** `selectable` moves 50 to 51, which is
this bean's blocked successor becoming reachable. `closing transitions` moves 0 to 1 and
`criteria checked` 0 to 9, which is check 14's vacuity assertion working: the merge base reports
zero because a bean is never a candidate on its own pull request
(`doc:00-constitution#bean-lifecycle`, `bean:0096`), and the pair is non-zero here only because
this change closes a bean whose implementation merged earlier and was reviewed elsewhere. A run
that examined nothing and a run that passed both print `OK`; these counts are what tells them
apart, and nine is the number of criteria this bean numbers.

The nine also answers a question the bare flip asks on its own: the evidence sections already
standing above satisfy check 14 unaided, with this closing section absent. Nothing below was
written in order to make the check pass.

Which bean became selectable, named rather than deduced from the arithmetic. The probe
implements `AGENTS.md` step 1, and its count is cross-checked against the gate's own
`selectable` figure at both heads — 50 against 50, then 51 against 51 — so it is validated by
the gate rather than believed on its own:

```
$ diff [...]/sel-before.txt [...]/sel-after.txt          # /bin/bash 3.2.57(1)-release
51c51,52
< probe selectable count: 50
---
> SELECTABLE	modus-0129
> probe selectable count: 51
```

`.beans/modus-0129` is `status: todo`, `type: fix`, `priority: medium`,
`blocked_by: [modus-0121]`, and it is the only bean in the tree with an edge onto this one:
`/usr/bin/grep -rn '^blocked_by:.*modus-0121' .beans/` returns that file and no other. The
interpreter matters for that figure as much as for a shell one — `/usr/bin/grep` is BSD grep
2.6.0-FreeBSD, and the bare name `grep` in this harness's interactive shell is a function
wrapping `ugrep 7.8.4`. So the delta of one is that bean and nothing else.

**Check 12's acyclicity result is a real result, and that is checked rather than assumed.**
`43 graph edges` on the `OK` line is the check's own vacuity assertion — a run that parsed
nothing reports zero. What makes the rest of the line mean more than it used to is `bean:0123`:
before it, an analyser that died mid-check left `docs-lint` printing `OK`. The guard it added
covers check 12's acyclicity analyser specifically, and `docsLintGateTest` plants a syntax error
into a copy of exactly that analyser and observes the gate go red:

```
$ /bin/bash tools/docs-lint-gate-test.sh > [...]/base-gate.txt   # 277c4d5, tree clean
docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)
docs-lint-gate-test: analyser awk — awk version 20200816

--- the plant: check 12's acyclicity analyser, destroyed
ok   the mutation site occurs exactly once in the gate
ok   the copy differs from the gate on exactly one line (one '<', one '>')
ok   and the line it differs on is the planted syntax error
ok   the control copy is identical to the gate

--- the runs: both halves, over the whole corpus
ok   a destroyed analyser makes the gate exit non-zero
ok   and the gate says it failed rather than printing OK
ok   and attributes it to an analyser that examined nothing
     (this awk exited 2 on the planted syntax error)
ok   the negative control: the same copy unmutated exits 0
ok   and prints the OK line
ok   and writes nothing at all to stderr

--- the mutated run's stderr: 6 line(s), at most 20 shown
     awk: syntax error at source line 4
      context is
     	    removed = >>>  = <<<  1
     awk: illegal statement at source line 4
     awk: illegal statement at source line 4
     FAIL check -  an analyser exited 2 and examined nothing; its last argument was '/var/folders/mg/c8xtgk197f74w3r78q7_9sfc0000gn/T/tmp.ezeGJTySr7/bean-edges.uniq'

--- the guard covers every call site, because no call site opts in
ok   the guard's own call is the only site that bypasses it

docs-lint-gate-test: 11 passed, 0 failed.
exit: 0
```

The last argument in that `FAIL` line is `bean-edges.uniq`, which is the acyclicity analyser's
own input — so the plant landed where the assertion above it says it landed. The `OK` line in
the two runs at the top of this block is therefore a green line from an analyser that ran, and
the acyclicity verdict under it is a verdict rather than a silence.

**The two arms above are stamped at `277c4d5` and both are still true there; the arithmetic
at the head this branch MERGES as is different, and it is re-derived rather than restated.**
This branch was rebased onto `7731d13` in review — PR #80, which merged after this close was
first pushed. The figures above are left exactly as taken, because re-labelling a figure with a
head it was not measured at is the failure this bean's own evidence rules exist to prevent. The
same two arms at the rebased base, taken the same way:

```
$ /bin/bash tools/docs-lint.sh          # 7731d13 + this bean's `status:` line reverted
docs-lint: OK — 19 documents, 111 anchors, 1746 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.

$ /bin/bash tools/docs-lint.sh          # the same tree with this close applied
docs-lint: OK — 19 documents, 111 anchors, 1746 references, 112 beans, 43 graph edges, 50 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 1 closing transitions, 9 criteria checked, 0 unnumbered.
```

`49` to `50`, not `50` to `51`. **The baseline moved and the delta did not**, which is the
distinction that matters: `closing transitions` and `criteria checked` still move 0 to 1 and 0
to 9, and the probe still names exactly one bean entering the selectable set:

```
$ diff [...]/sel-base-rebase.txt [...]/sel-after-rebase.txt
50c50,51
< probe selectable count: 49
---
> SELECTABLE	modus-0129
> probe selectable count: 50
```

What moved the baseline is `.beans/modus-0124`, which PR #80 took out of the selectable set by
starting it: `status: todo` at `277c4d5`, `status: in-progress` at `7731d13`. Its own probe
diff says so and names no other bean:

```
$ diff [...]/sel-before.txt [...]/sel-base-rebase.txt
48d47
< SELECTABLE	modus-0124
51c50
< probe selectable count: 50
---
> probe selectable count: 49
```

So `selectable` is a figure of the corpus at a moment and moves under this bean without this
bean touching it (`doc:50-memory-and-evidence#corpus-figures`), while *which* bean this change
makes selectable is a property of the change and is unchanged. A close that had restated `50 to
51` at the new head would have been wrong in the count and right in the claim, which is the
worst of both.

**The gate-test figure quoted above moved the same way and for the same kind of reason**, and
it is worth separating stale from invalidated. `11 passed, 0 failed` is what that suite reported
at `277c4d5` and the capture is left saying so. PR #80 rewrote it; at `7731d13` it reports
`168 passed, 0 failed, over 2 bash major version(s)`. Nothing in that rewrite reads bean content
or touches `tools/lib/`, and what this block relies on is a PROPERTY — that a destroyed check 12
acyclicity analyser makes the gate exit non-zero instead of printing `OK` — not a count of
assertions. The property is re-derived green at the rebased head, so the argument above is
unchanged and strictly better evidenced than when it was written.

### Block F — the gate, on this closing branch

Re-run rather than reused, after the last edit to this file: this bean's own prose is an input
to `docs-lint`, and its reference count moves whenever this section does.

```
$ ./gradlew qualityCheck > [...]/quality-with-placeholder.txt
[...]
> Task :bashCompatLint
bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
bash-compat: OK — 4 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
[...]
docs-lint-test: 76 passed, 0 failed.
[...]
> Task :docsLintGateTest
docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)
docs-lint-gate-test: analyser awk — awk version 20200816
[...]
> Task :docsLint
docs-lint: OK — 19 documents, 111 anchors, 1732 references, 112 beans, 43 graph edges, 51 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 1 closing transitions, 9 criteria checked, 0 unnumbered.
[...]
docs-lint-gate-test: 11 passed, 0 failed.
[...]
BUILD SUCCESSFUL in 53s
170 actionable tasks: 54 executed, 96 from cache, 20 up-to-date
exit: 0
```

Every `[...]` in that capture elides `> Task :…` banners, the per-module compile, ktlint,
Detekt, ArchUnit and test output, `docsLintTest`'s and `docsLintGateTest`'s own assertion
listings — the second is quoted whole in Block E, where that script is run on its own — the
`:e2eInstall` and `:backofficeInstall` npm output, the three backoffice tasks
(`backofficeTypecheck`, `backofficeLint`, `backofficeFormatCheck`), Gradle's problems-report
path and its Gradle 10 deprecation notice, and the blank lines between all of them.

**The head that run measured is stated exactly, because it is not this file as it now
stands.** `docs-lint` reads this bean, so a gate run cannot contain its own output. That run
measured this section with Block F's fence holding a one-token placeholder line, and without
everything written into this section after it: Block B's closing control, Block D's
re-derivation of the corpus differential, Block E's re-derivation at the rebased base, the
PR #79 capture in this block, and every paragraph of this block standing below the fence —
this one and the ones under it included, which is what stops this list going stale the next
time a paragraph is added to it. Every one of those changed this file, so the gate was run
again after the last of them, because
`doc:80-agent-operating-procedure#self-validate` requires the last change to be the one the
gate saw. The final run's `OK` line is quoted in the closing pull request's body rather than
here, for the same reason this fence cannot hold it.

**That list omitted Block D when it was first written, and named its own paragraphs
individually so that it went stale again the moment this paragraph was added. The omission is
worth more than either correction.** Block D's re-derivation was added in the same commit as
the list, and the list was itself produced by the sweep for the count-of-a-growable-set defect
one round earlier — the remedy for that defect being to NAME a set rather than count it. So a
named enumeration, written by the sweep against counting, was already incomplete over a set its
own commit was changing, and then went stale a second time under its own author.
**Naming does not prevent staleness; it only makes staleness legible.** A count moving from one
value to another is invisible, and that is the case for naming; an enumeration missing a member
is visible to a reader who checks it against the thing it enumerates, which is why review caught
this one. Both fail, and they fail differently.

What that widens, recorded here because this paragraph is the instance and not a description of
one: a check for this class cannot be only *a prose numeral standing before an enumerable set*.
It has to reach *an enumeration asserted to be exhaustive over a set the same commit changes* —
which is decidable, since the commit's own diff is what says which sets moved. That is not this
bean's work and no bean claims it yet.

`1732 references` inside the fence is the count as it stood at that run, and **this paragraph
deliberately does not say what it reads now.** A reference count is a figure of the corpus at a
moment (`doc:50-memory-and-evidence#corpus-figures`), never a property of this change, and this
bean is part of the corpus it counts — so any live count written here is invalidated by the
sentence written to explain it. That is not hypothetical: the clause standing here carried a
number and a cause, went stale on the rebase, was corrected to a new number and a disclaimer,
and went stale again on the very edit that added the paragraph above. Two rounds of review
reached the verdict table and Block G and did not reach this block. The count that matters is
the one on the `OK` line of the run in the closing pull request's body, which is re-taken after
the last edit by construction; this fence holds the run it holds.

**The ninth criterion's second half is PR #79's own CI, and it is observed here rather than
asserted.** It was written into the verdict table above before it had been looked at, which is
the failure `doc:00-constitution#evidence-rule` names — an unevidenced statement is a
hypothesis — and the fix is the capture, not a softer sentence. Four check runs, deduplicated
by name because the two workflow runs on that pull request report the same four:

```
$ GITHUB_TOKEN= gh pr checks 79 --json name,bucket,state \
    --jq '.[] | "\(.name)\t\(.bucket)"' | sort -u
backoffice + e2e	skipping
build + mechanical gates	pass
gate	pass
which halves	pass
```

`gate` is `pass`. `backoffice + e2e` reports `skipping` and not a failure: PR #79 changed no
file under `backoffice/` or `e2e/`, and `doc:00-constitution#workflow` §7.2.4 records that the
`gate` job exists precisely because a skipped half reports neither success nor failure. That
is why `gate` and not `build + mechanical gates` is the job this row cites.

### Block G — figures taken above that have expired, corrected here and not edited there

An observation is amended, never edited. Nothing below changes a verdict. Each entry is a
figure whose head moved out from under it, a reading a verdict rests on that a reader should be
able to check, or a constraint this record hit while being written. The entries are named and
not counted, because a set that can grow is named and never counted
(`doc:05-authoring-for-agents#one-fact-one-place`) — this one already grew by one while this
paragraph stood above it saying `three`.

**The first criterion's `a legitimately citing evidence row`, and the reading the verdict rests
on.** The negative control that stands at the merged head is a row citing from a column that is
not the evidence cell. A row citing from its evidence CELL no longer answers, and that is not an
oversight — it is the case the eighth criterion was written to decide, and `cell-off` in Block C
kills the sacrifice and the laundering rejection together. Both readings of the phrase are live:
under the wider one, the class of evidence rows that cite legitimately is non-empty and still
closes, and the criterion is met; under the narrower one it is not. The verdict recorded is
`met`, on the wider reading, because this bean's own eighth criterion is what settles which
reading was intended and it settles it in that direction. A reader who disagrees should
disagree with this paragraph, which is why it is here rather than folded into the table.

**The sixth criterion's `Line count after: 404`, expired.**
`wc -l documentation/05-authoring-for-agents.md` reports `417` at `277c4d5`, and
`git show <head>:documentation/05-authoring-for-agents.md | wc -l` reports 391 at `5a625fc`,
404 at `2bcd9aa`, and 417 at `eabd009` and at every head after it. The figure was taken at
`2bcd9aa`; the section around it was re-taken at `eabd009` and this line was not carried with
it. 417 is inside `adr:0003`'s 500, so the budget claim the figure was making still holds — the
figure is stale, not wrong about what it was for.

**`seventeen delimiter rows, seventeen evidence-ish columns, all of them last`, in
`tools/docs-lint-test.sh` at `eabd009` — wrong when written, and already corrected before the
merge.** `1556d02` replaced it with an uncounted statement, which is why the merged tree carries
no count there. Measured with an `awk` that applies `rowcells()`'s own rule for what a row's
cells are, `/bin/bash` 3.2.57:

```
$ awk -f [...]/delims.awk <tools/docs-lint-test.sh at 5cf9c58, extracted with git show>
NOEVCOL	line 887	| # | criterion | evidence kind |
delimiter rows: 18; with an evidence-ish column: 17 (last: 17, not last: 0); without: 1

$ awk -f [...]/delims.awk tools/docs-lint-test.sh          # at 277c4d5
NOEVCOL	line 959	| # | criterion | evidence kind |
NOTLAST	line 1747	| # | claim | evidence | runs |
NOTLAST	line 1775	| # | claim | evidence | runs |
delimiter rows: 22; with an evidence-ish column: 21 (last: 19, not last: 2); without: 1
```

The comment undercounted the delimiter rows by one, by leaving out of its total the single table
the sentence's own next clause is about — so it made the blind spot sound smaller and tidier
than it was. The two `NOTLAST` rows at the merged head are the fixtures added for the seam, so
the structural blindness that hid the mask-seam defect is measurably gone rather than merely
described as gone. This is `doc:05-authoring-for-agents#one-fact-one-place`'s drift generator in
its purest form: a count, in a comment, of another file's shape. The comment that replaced it
names the property and no number.

**This section could not state the entries above as a numbered table, and that is the
sharpest demonstration available that the narrowing bites.** The count that stood in this
sentence is the fourth instance of the defect its own lead names, found in review after the
lead was written to name the third — which is the argument for naming a set rather than
counting it, made once more by the paragraph making it. A table row inside an evidence
region is a citation site, so a row reading `| 2 | criterion 6's line count | … |` would answer
the sixth criterion from the row that reports its figure stale — the shape
`doc:05-authoring-for-agents#checks` warns about, reached while writing the closing record for
the change that created it. Worse, a table with no evidence-ish column is read whole, so every
criterion number in every cell would land. The entries are therefore prose under bolded leads:
a bold line is running prose to the analyser and to CommonMark alike, which the same section
states in as many words. The change this bean closes binds its own paperwork, and nothing in
the corpus had done that before.
