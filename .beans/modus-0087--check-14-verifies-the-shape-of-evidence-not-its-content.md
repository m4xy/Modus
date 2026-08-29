---
# modus-0087
title: Check 14 verifies the shape of an evidence record and not its content, so every evidence column shipped so far is weaker than its author believed
status: todo
type: fix
priority: high
created_at: 2026-08-29T00:00:00Z
---

# Check 14 verifies the shape of an evidence record and not its content, so every evidence column shipped so far is weaker than its author believed

`doc:00-constitution#evidence-rule` requires the command, the exit code and the output.
`adr:0005-evidence-lives-in-the-work-item#evidence-home` puts that beside the criterion it
satisfies. `docs-lint` check 14 is the mechanism named against both, and what it actually
decides about an evidence cell is only this:

| condition | at | rejects |
|---|---|---|
| the cell is empty after normalisation | `tools/docs-lint.sh` `EMPTYCELL` | a blank cell |
| the cell is made **entirely** of names from `doc:50-memory-and-evidence#evidence-kinds` | `allkinds()` | `citation`, `test-run`, `diff and citation` |

Anything else non-empty passes. There is no third condition. **A green check 14 says the
record has the right shape, and says nothing about whether the cell is evidence** — and the
two have been read as the same thing, including by the author of this check.

This is a third gap in check 14, distinct from `bean:0056` (an evidence kind recorded as
evidence — that is the second row above, and it is the one that works) and from `bean:0061`
(the numbering gate).

## Observed

Planted on `.beans/modus-0033`, a `status: todo` bean, by flipping its status to `completed`
and appending a `## Success criteria and evidence` table with an `evidence` column, then
reverted with `git checkout -- .beans`. Every run is `bash tools/docs-lint.sh`.

### Six of eight junk cells close their criterion

```
planted:  eight numbered criteria, evidence cells reading
            1 `-`   2 `n/a`   3 `see above`   4 `TODO`
            5 `ArchUnit test`   6 <a single space>   7 `citation`   8 `BUILD SUCCESSFUL`
observed: FAIL check 14 .beans/modus-0033-…: criterion 6 closes with an empty evidence cell
          FAIL check 14 .beans/modus-0033-…: criterion 7 records 'citation' — an evidence
          KIND, not evidence; the cell must carry the command, the expectation and the
          verbatim observed output
          docs-lint: 2 failure(s).
exit:     1
```

Two of eight. The other six answered their criteria.

```
planted:  the six accepted cells alone — `-`, `n/a`, `see above`, `TODO`, `ArchUnit test`,
          `BUILD SUCCESSFUL` — as the whole evidence table of a closing bean
observed: docs-lint: OK — 19 documents, 106 anchors, 914 references, 64 beans, 28 graph
          edges, 18 selectable, 64 bean ids, 0 introduced, 64 on origin/main, 1 closing
          transitions, 6 criteria checked, 0 unnumbered.
exit:     0

control:  one criterion, its cell carrying a command, an expectation and output
observed: docs-lint: OK — … 1 closing transitions, 1 criteria checked, 0 unnumbered.
exit:     0
```

`6 criteria checked` and exit 0. The counts line — this check's own vacuity assertion —
cannot tell the two runs apart, because it counts criteria examined and not criteria
evidenced.

### The one condition that works is defeated by a plural

`allkinds()` normalises, splits on whitespace, and rejects only when **every** token is a
known kind. One extra token, or one letter, is enough.

```
planted:  eight cells — `citation`, `citations`, `a citation`, `citation.`,
          `test-run`, `test-runs`, `Test-Run`, `test-run of the gate`
observed: FAIL check 14 …: criterion 1 records 'citation' — an evidence KIND, not evidence…
          FAIL check 14 …: criterion 4 records 'citation.' — an evidence KIND, not evidence…
          FAIL check 14 …: criterion 5 records 'test-run' — an evidence KIND, not evidence…
          FAIL check 14 …: criterion 7 records 'test-run' — an evidence KIND, not evidence…
          docs-lint: 4 failure(s).
exit:     1
```

Rejected: `citation`, `citation.`, `test-run`, `Test-Run`. Accepted: `citations`,
`a citation`, `test-runs`, `test-run of the gate`. Four of eight, and the four that pass
differ from the four that fail by a plural, an article or a trailing phrase. **This is the
argument against fixing the problem with a longer list.** A blocklist of junk strings is the
allowlist mistake wearing different clothes: it fails on the first string nobody thought of,
and here it fails on the plural of a string that IS on the list.

### The corpus, measured

Every evidence cell check 14 inspects, across the 23 `completed` beans:

```
cmd:      extract the evidence cells check 14 inspects from every `status: completed` bean,
          using check 14's own table logic, and test each against two candidate requirements
observed: evidence cells in completed beans: 104, across 12 files
            no backtick-quoted span:       42
            under 40 characters:           50
            both:                          36
          the ten shortest cells are all in .beans/modus-0004, criteria 1-11, and each
          reads: met
exit:     0
```

`modus-0004` closed eleven criteria with an evidence column whose every cell is the word
`met`. It is check-14-conformant today, and it is the plainest available statement of the
finding: **conformance to the shape and conformance to `doc:00-constitution#evidence-rule`
are different things, and only the first is mechanised.**

It applies ahead of the fact as well as behind it. PR #44 raises three beans whose criteria
tables were demonstrated check-14-conformant; those beans will be **closed** against the two
conditions in the table above, and nothing in this check will look at what their evidence
cells then say. Nothing about that PR is impugned. What is impugned is the inference from
"check 14 is green" to "the evidence is sound", which is an inference this repository has
been making.

## Can a requirement be built? Each candidate, evaluated

A **requirement** — something structural that evidence has and prose does not — is the right
shape, for the reason the plural defeat above demonstrates. Three were considered.

### Does the cell reference a command that appears elsewhere in the bean?

**Partly mechanisable, and the best of the three.** As a requirement: the cell MUST contain a
backtick-quoted span, and at least one such span MUST also occur elsewhere in the bean —
in a fenced transcript, or in a command line. It rejects `met`, `-`, `n/a`, `see above`,
`TODO`, `ArchUnit test` and `BUILD SUCCESSFUL` on the first clause alone, and it is not a
blocklist: it names a property, not a set of bad strings.

What it does not do is decide whether the quoted span is *observed output* rather than a
plausible-looking string an author typed. It raises the floor; it is not a gate. Cost,
measured above: 42 of 104 cells across 10 completed beans fail the first clause today.

### Does the cell contain a character sequence that could only have come from tool output?

**Not mechanisable. Recommend rejecting it.** There is no such sequence. Every candidate —
`exit 0`, `BUILD SUCCESSFUL`, a duration, a SHA-shaped token, a `file:line` — is a string an
author can type, and the plant above closed a criterion with a bare `BUILD SUCCESSFUL`. An
allowlist of output-shaped patterns fails exactly as a blocklist of junk strings does, in the
same direction, and it would additionally teach authors which strings to type.

### Is there a minimum information content below which a reviewer would also reject?

**Mechanisable as a proxy, and it measures typing rather than observation.** Length, distinct
token count, or the presence of a digit are all computable; 50 of 104 cells are under 40
characters. But a fluent sentence of unevidenced prose passes any such threshold while `met`
fails it, which means the threshold catches laziness and not dishonesty. Useful only as a
second clause under the first candidate, never alone.

### The conclusion, stated plainly

**No mechanism decides whether a cell is evidence, and this one should stop implying that it
does.** What separates evidence from prose is that a third party can re-run the command and
compare the output — a judgement about the world, not a property of a string.
`doc:00-constitution#independent-review` already puts an independent reviewer on every
change, and that reviewer is the mechanism for this; §7.4 is not a fallback here, it is the
answer. The machine's honest job is the floor (the first candidate) plus a statement in
`doc:05-authoring-for-agents#checks` that a green check 14 asserts the record's shape and not
its truth, so that nobody reads it the way it has been read.

## Blast radius

- **A strict fix retroactively flags completed beans.** At least 10 of the 23, on the first
  candidate's first clause alone.
- **`docs-lint` check 11 makes a `completed` bean append-only**, so those cells cannot be
  filled in place; the only route is an `## Amendments` entry per bean
  (`adr:0005-evidence-lives-in-the-work-item#amendments`). Twelve beans' worth of amendments
  written to satisfy a check is itself a shape worth refusing.
- Check 14 is therefore likely to need the same grandfathering it already has for the closing
  transition: judge what CLOSES in this change, and leave the corpus check 11 has frozen
  alone. That is a decision this bean owes, not an assumption.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The six junk cells above are each observed either rejected, or accepted with the reason stated and argued | planted violation, reverted |
| 2 | Whatever is adopted is a requirement naming a property, not a list of rejected strings, and the plural/article/trailing-word variations above are observed against it | planted violation, reverted |
| 3 | `doc:05-authoring-for-agents#checks` states what a green check 14 does and does not assert | diff |
| 4 | The retroactive effect on the 23 `completed` beans is measured and named per file, and the grandfathering decision is recorded either way | analyser run over the corpus, before and after |
| 5 | What the analyser perceives about a cell is asserted separately from what it decides | test-run |
| 6 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- An evidence kind recorded as evidence (`bean:0056`) and the numbering gate (`bean:0061`).
- Fence tracking (`bean:0063`) and check 6's copy of it.
- The set of accepted evidence kinds. That is
  `doc:50-memory-and-evidence#evidence-kinds`' closed set and this bean does not touch it.
- Rewriting the evidence of any `completed` bean. Whether that is required at all is
  criterion 4's decision, and it is constrained by check 11.
