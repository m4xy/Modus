---
# modus-0129
title: A heading-shaped or row-shaped line inside a raw HTML block is still a citation site
status: todo
type: fix
priority: medium
created_at: 2026-09-04T00:00:00Z
blocked_by: [modus-0121]
---

# A heading-shaped or row-shaped line inside a raw HTML block is still a citation site

Split out of `bean:0121`, which recorded four residuals of check 14's citation rule and
closed three. This is the fourth. `bean:0121` names it splittable in its own text and states
the reason: the three it closed are extra CONDITIONS on a site the analyser already
recognises, each decided from state it already holds — `region`, the heading level, `evcol`.
This one needs a perception layer the analyser does not have, so it is a different mechanism
and a different change.

`citation_site()` in `tools/lib/docs-lint-c14.awk` receives the line text and one flag of the
analyser's own state. It has no raw-HTML-block state and cannot refuse a container. A
container is refused only insofar as its contents are neither heading-shaped nor row-shaped.

## Observed

At `3b02871`, and again unchanged at the head that closes `bean:0121`. Every run is
`awk -v KINDS="…" -f tools/lib/docs-lint-fence.awk -f tools/lib/docs-lint-c14.awk <fixture>`
on a fixture and not on a bean file, so no plant, no revert and no `git checkout -- .beans`
is involved (`bean:0102`, `bean:0116`). `KINDS` is the value `tools/docs-lint.sh` passes.

```
fixture:  a bean numbering four criteria, criterion 1 genuinely answered under
          `### Criterion 1`, and a heading-shaped `# criterion 2 …` line inside <pre>,
          `# criterion 3 …` inside <details><pre>, and `# criterion 4 …` inside an HTML
          comment, all under a `### The run` heading inside `## Evidence`
observed: STATS	4	0
exit:     0
```

```
fixture:  a bean numbering one criterion, whose `## Evidence` holds a Markdown table pasted
          inside a <pre>; the delimiter row sets `intable` wherever it stands, so the row
          that follows is read as a table row and answers its criterion
observed: STATS	1	0
exit:     0
```

Both shapes are pinned as `ACCEPTED` verdicts in `tools/docs-lint-test.sh`, restated at
`bean:0121` with the reason they remain accepted rather than left standing by omission.

**`bean:0121`'s three conditions do not reach this.** The container in each fixture stands
inside `## Evidence`, under a heading that has content, which is where evidence belongs; and
the second fixture's citation is not in an evidence cell. Narrowing region, emptiness or the
cell further would not close this and would cost beans that closed correctly.

## Why this is hard, and why it may be right to close it as WONTFIX

The argument against building it is on the record twice — in `bean:0093`, which chose the
positive shape rule over the enumeration it replaced, and in `bean:0121`, which re-checked it
against `pandoc 3.7.0.2`. Both halves must be answered by whoever selects this bean:

1. **Refusing container contents is an enumeration.** Which HTML blocks hold literal content
   is CommonMark §4.6's type 1, whose four tag names are the whole rule, and type 2's
   comment. That is a list of containers — the allowlist `doc:00-constitution#mechanical-enforcement`
   records as failing open on the first string nobody thought of, and the exact shape the
   positive rule was written to replace.
2. **It is wrong in the other direction too.** A `#` heading inside `<details>` with blank
   lines around it renders as a heading to CommonMark and to GitHub alike. "Inside a
   container" and "not rendered as a heading" are different sets, so a rule that refuses the
   first refuses headings that genuinely are headings.

A closure that states this and demotes the residual to a recorded `Enforcement gap:` is a
legitimate outcome. `doc:00-constitution#observed-failing` prefers an admitted gap to an
unfalsifiable gate, and a bean is allowed to conclude that the cost of closing exceeds the
cost of the residual — `bean:0093`'s criterion 3 closed NOT MET AS WORDED for this exact
reason. What is not allowed is silence.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | Both fixtures above are re-measured at the head the work starts from, and any that no longer reproduces is named | test-run |
| 2 | The change either refuses a citation from inside a raw HTML block — observed rejected on each fixture, against a negative control showing a heading and a row outside one still answer — or states a refusal with the reason, and the two arguments above are each answered rather than restated | planted violation, reverted |
| 3 | If it is closed, the rule is decidable without enumerating container tag names, or the enumeration is defended against `doc:00-constitution#mechanical-enforcement` with what makes it exhaustive | diff |
| 4 | If it is refused, `doc:05-authoring-for-agents#checks` carries an `Enforcement gap:` naming this bean, and the two `ACCEPTED` pins in `tools/docs-lint-test.sh` are restated with the refusal as their reason | diff |
| 5 | Every bean in `.beans/` is measured before and after and every bean whose answered set changes is named, without a count because the corpus grows | analyser run over the corpus, before and after |
| 6 | The matcher still never reads polarity | diff |
| 7 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- The region, emptiness and evidence-cell conditions (`bean:0121`), which are shipped and
  which this bean must not weaken.
- The fence classifier (`bean:0063`). A fence is the one container the analyser does model.
- The composition of fence parity with the citation matcher (`bean:0099`).
- Polarity blindness, accepted at `bean:0093` with its reasoning.
