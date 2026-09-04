---
# modus-0121
title: A citing heading answers a criterion from anywhere in the file, with nothing under it, and from inside a raw HTML block
status: todo
type: fix
priority: high
created_at: 2026-09-04T00:00:00Z
blocked_by: [modus-0093]
---

# A citing heading answers a criterion from anywhere in the file, with nothing under it, and from inside a raw HTML block

`bean:0093` narrowed check 14's citation site to a **shape**: an ATX heading, or a row of a
table the analyser has entered. That is the whole of the constraint. The site is not required
to stand inside the evidence region, it is not required to have anything under it, and the
analyser holds no state that would let it refuse a container. Three residuals follow, and each
lets a bean close green on evidence it never recorded.

The evidence-table path has `EMPTYCELL` and `HOLLOW` — a numbered row whose evidence cell is
blank, or holds only the NAME of an evidence kind, is a finding. **The heading path has no
analogue at all.** `doc:05-authoring-for-agents#checks` now recommends the sub-heading as the
way to cite, which promotes the cheapest hollow shape in the check from an accident into the
documented convention. That is why this is `high` and not an idle observation.

## Observed

Every run below is `awk -v KINDS="…" -f tools/lib/docs-lint-fence.awk -f
tools/lib/docs-lint-c14.awk <fixture>` at `aa4e64f`, on a fixture and not on a bean file, so
no plant, no revert and no `git checkout -- .beans` is involved (`bean:0102`, `bean:0116`).
`KINDS` is the value `tools/docs-lint.sh` passes.

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

**This one is different in kind from the two above and may be split out.** The first two are
extra conditions on a site the analyser already recognises, and both are decidable from state
it already tracks. This one needs a new perception layer: which HTML blocks hold literal
content is CommonMark §4.6's type 1, whose four tag names are the whole rule, and type 2's
comment — an enumeration of containers, which is the allowlist `bean:0093`'s argument
replaced. It is also wrong in the other direction: a `#` heading inside `<details>` with blank
lines around it renders as a heading to CommonMark and to GitHub alike, so "inside a
container" and "not rendered as a heading" are different sets. An agent selecting this bean
should decide whether the third belongs in it (`doc:05-authoring-for-agents#bean-split`).

## Why the two constraints are one bean

Either alone is walked past by the other. Requiring the citing heading to be inside the
evidence region leaves `### Criteria 1-5` as the whole of that region. Requiring content under
the heading leaves `### Criterion 3 was not attempted` under `## Not in scope`, followed by a
paragraph. Together they cover both without reading meaning, and both fail closed: a heading
that satisfies neither reports the criterion unanswered, which is the direction
`doc:00-constitution#observed-failing` asks for.

"Non-empty content beneath it" needs defining rather than assuming, and the definition already
exists: `doc:05-authoring-for-agents#checks` defines an **entry** as a table row, a sub-heading
or a fenced block, and states that a section of prose alone is not one. Whether the citing
heading requires an entry under it or merely a non-blank line is the decision this bean makes,
and it has a measurable cost over the corpus either way.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The two fixtures above — the whole-region sub-heading, and the two headings under `## Not in scope` — are observed rejected, not merely no longer accepted, and negative controls show a heading inside the evidence region with content under it still closes | planted violation, reverted |
| 2 | The region constraint and the emptiness constraint are each observed to fail on their own against the other's fixture, so that neither is landed on the other's evidence | test-run |
| 3 | What counts as content under a citing heading is decided against `doc:05-authoring-for-agents#checks`'s existing definition of an entry, and the choice is stated with its cost rather than assumed | diff |
| 4 | **Every** bean in `.beans/` is measured before and after and every bean whose answered-set changes is named, stated without a count because the corpus grows (`bean:0093`'s fourth criterion is the precedent) | analyser run over the corpus, before and after |
| 5 | The two `ACCEPTED` verdict assertions in `tools/docs-lint-test.sh` that pin the container limit are either flipped to rejections or restated with the reason they remain accepted; a decision either way is recorded, and silence is not one | test-run |
| 6 | `doc:05-authoring-for-agents#checks` states the constraints that result, and the paragraph this bean is named in is corrected rather than left beside them | diff |
| 7 | The matcher still never reads polarity, and no rejected string is enumerated anywhere in the change | diff |
| 8 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- The polarity blindness itself. It is accepted at `bean:0093` with its reasoning, and both
  constraints above are chosen precisely because neither requires reading polarity.
- The fence classifier (`bean:0063`), which is a separate perception layer and is not what
  decides any of the three shapes above.
- The composition of fence parity with the citation matcher (`bean:0099`). That bean owns two
  mechanisms interacting; this one owns three under-constraints of a single mechanism.
- What an evidence *cell* must hold (`bean:0087`). `EMPTYCELL` and `HOLLOW` are named above
  only as the analogue the heading path lacks.
