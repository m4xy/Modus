---
# modus-0087
title: Check 14 verifies the shape of an evidence record and not its content, so every evidence column shipped so far is weaker than its author believed
status: todo
type: fix
priority: high
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0063]
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

Every evidence cell check 14 inspects, across the `completed` beans:

```
cmd:      extract the evidence cells check 14 inspects from every `status: completed` bean,
          using check 14's own table logic, and test each against two candidate requirements
observed: cells=140 files=15 no-backtick=44 under-40=58 both=38
          cells reading exactly `met`: 27, in three beans —
            .beans/modus-0004  11
            .beans/modus-0005   8
            .beans/modus-0008   8
exit:     0
```

**The first version of this measurement said 104 cells across 12 files, and it was scoped
narrower than the thing it was measuring.** It counted only tables inside a criteria or
evidence region. Check 14's cell test — `if (evcol > 1 && evcol < nc)` — sits **outside** the
region conditional, so a table anywhere in a bean carrying an `evidence`, `observed`,
`output` or `result` column has its cells checked, including one under `## Options` or
`## Notes`. The 36 cells that scoping dropped are in region `NONE`, concentrated in
`modus-0001` (21), `modus-0010` (5), `modus-0046` (4), `modus-0009` (3) and `modus-0008` (3),
and dropping them silently lost three whole files. The fix agent is asked below to measure
the retroactive effect per file, and would have started from a corpus missing three of the
fifteen.

That error is better evidence for this bean's thesis than the corrected figure is. **The
author of a bean about what check 14 inspects did not know what check 14 inspects**, and
neither the check, nor the documentation, nor the first review round said otherwise. A
mechanism whose scope its own analyst cannot state from reading it is not one anybody should
be inferring soundness from.

Three beans — `modus-0004`, `modus-0005` and `modus-0008` — closed 27 criteria between them
with evidence cells reading nothing but the word `met`. All 27 are check-14-conformant today.
An earlier version of this bean reported "the ten shortest cells are all in `modus-0004`":
that was an artifact of sort order among 27 cells of equal length, and it understated the
finding by two thirds. The corrected figure is the plainest available statement of the point:
**conformance to the shape and conformance to `doc:00-constitution#evidence-rule` are
different things, and only the first is mechanised.**

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
measured above: 44 of 140 cells, across 10 of the 15 files, fail the first clause today.

### Does the cell contain a character sequence that could only have come from tool output?

**Not mechanisable. Recommend rejecting it.** There is no such sequence. Every candidate —
`exit 0`, `BUILD SUCCESSFUL`, a duration, a SHA-shaped token, a `file:line` — is a string an
author can type, and the plant above closed a criterion with a bare `BUILD SUCCESSFUL`. An
allowlist of output-shaped patterns fails exactly as a blocklist of junk strings does, in the
same direction, and it would additionally teach authors which strings to type.

### Is there a minimum information content below which a reviewer would also reject?

**Mechanisable as a proxy, and it measures typing rather than observation.** Length, distinct
token count, or the presence of a digit are all computable; 58 of 140 cells are under 40
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

## A third gate sits in front of both conditions, and it silences them

The two conditions in the table above only ever run on a table check 14 recognises as
carrying evidence. Recognition is a **closed vocabulary of four column headers** —
`evidence`, `observed`, `output`, `result` — and when a numbered table in an evidence section
matches none of them, `NOEVCOL` fires and the analyser suppresses the entire per-criterion
cascade:

```awk
if (C[i]) { nc++; if (!A[i] && !noevcol) { printf "UNANSWERED\t%d\n", i } }
```

`!noevcol` is the mask. One unrecognised column header silences the audit of every criterion
in the bean — not a wrong verdict, an **absent** one, reported as a single line about table
shape.

```
planted:  three numbered criteria, and an evidence table headed `| # | ground | verified |`
          answering only the first two. The third is genuinely unanswered.
observed: FAIL check 14 …: the table under 'Evidence' numbers criteria in an evidence
          section but carries no evidence column; 'evidence kind' states what will be
          produced, not what was observed
          docs-lint: 1 failure(s).
exit:     1

control:  the identical table, the one header renamed `verified` -> `observed`
observed: FAIL check 14 …: criterion 3 is not answered in the evidence; no evidence row
          bears its number and nothing cites it
          docs-lint: 1 failure(s).
exit:     1
```

The genuinely unanswered criterion is invisible until the header is renamed. An author
reading the first run learns that a column header is unrecognised and learns nothing about
whether their criteria are answered.

**So the sequence is header → numbering → cell condition, and a defect in the first hides the
state of the other two.** `bean:0061` owns the numbering gate and this bean owns the cell
conditions; neither runs at all when the vocabulary gate rejects the table. The check is most
silent exactly when a bean's structure is least conventional, which is when it would most
want to be talking.

### Is the vocabulary documented? Yes — so this is the smaller half of the finding

`doc:05-authoring-for-agents#checks` states it, in the definition list an author writing a
bean is already reading:

> An **evidence column** is one headed `evidence`, `observed`, `output` or `result`.

So nobody is guessing, and the failure is actionable from the documentation without reading
the implementation. What the failure **message** does not do is name the four accepted
headers, so acting on it means one hop to `doc:05`. That is an ergonomics gap, not a
discoverability one, and it is much smaller than the masking.

### Observed in flight, and it does not say what it was reported to say

`modus-0068` is `in-progress` on `main` and carries a `| # | ground | verified |` table in its
evidence section. Flipped to `completed` it reports exactly one failure and no per-criterion
line at all — the masking, on a real bean. Renaming that one header was reported to me as
yielding zero unanswered criteria. **It does not.**

```
cmd:      modus-0068 flipped to `completed`, header `verified` -> `observed`, nothing else
expected: the cascade unmasked, with nothing unanswered
observed: FAIL check 14 …modus-0068…: criterion 5 is not answered in the evidence; …
          FAIL check 14 …modus-0068…: criterion 6 is not answered in the evidence; …
          FAIL check 14 …modus-0068…: criterion 7 is not answered in the evidence; …
          FAIL check 14 …modus-0068…: criterion 8 is not answered in the evidence; …
          FAIL check 14 …modus-0068…: criterion 9 is not answered in the evidence; …
          FAIL check 14 …modus-0068…: criterion 10 is not answered in the evidence; …
          FAIL check 14 …modus-0068…: criterion 14 is not answered in the evidence; …
          docs-lint: 7 failure(s).
exit:     1
```

Seven, not zero. The header its owner is already fixing is masking seven unanswered criteria
today, and fixing the header alone will surface all seven at once.

And the unmasked reading is **also** unsound, which is the part that matters here. That
table's `#` column numbers *grounds*, not criteria — it is a list of supporting arguments that
happens to be numbered. Recognising it as an evidence table makes those row numbers answer
criteria they have nothing to do with, so the low-numbered criteria are marked answered by
grounds that were never about them, and only the criteria beyond the table's length are
reported. **Neither reading of that bean is correct**: masked, it audits nothing; unmasked, it
answers criteria from a table that is not about them. That is the numbering semantics
`bean:0061` owns, reached through the vocabulary gate this bean owns, on one table.

## This bean shipped with one of its own criteria pre-closed

Found by flipping it to `completed` and reading which criteria check 14 named:

```
cmd:      modus-0087 status: todo -> completed, nothing else changed
expected: NOEV, and all six criteria reported unanswered
observed: FAIL check 14 …: closes with no evidence section; …
          FAIL check 14 …: criterion 1 is not answered in the evidence; …
          FAIL check 14 …: criterion 2 is not answered in the evidence; …
          FAIL check 14 …: criterion 3 is not answered in the evidence; …
          FAIL check 14 …: criterion 5 is not answered in the evidence; …
          FAIL check 14 …: criterion 6 is not answered in the evidence; …
          docs-lint: 6 failure(s).
exit:     1
```

Five, not six. **The fourth is absent.** The `## Not in scope` section carried a sentence
naming that criterion by number, in top-level prose, while saying its decision was
constrained by check 11 — and the citation matcher marked it answered on that alone. A benign
mention, written by an author about his own criteria, with no pasted output and no fence
marker involved.

A bean about check 14's weakness shipped with one of its six criteria pre-closed by check
14's weakness. The sentence is rewritten above so it no longer names the number, which fixes
this instance and not the defect; the defect is the citation matcher's and is recorded in
`bean:0061` and in the beans raised from it.

**This section is worded to avoid naming that criterion by number, and its first draft was
not.** Written plainly — quoting the offending sentence and stating which one was missing —
it re-answered the criterion three times over and the check reported five findings again.
`bean:0061` recorded the same thing happening to itself and wrote the warning this section
had to rediscover: in a bean about criterion numbering, plain prose is unsafe. Two authors,
in two beans, both of whom knew about the defect, both caught by it while documenting it.
That is the argument that this cannot be delegated to authors being careful.

## Blast radius, measured: nothing breaks unless the fix chooses to widen scope

**The first version of this section had it backwards, and gave three different numbers for
one quantity two bullets apart.** It said a strict fix retroactively flags "at least 10 of
the 23" completed beans, built an amendments deadlock on that, priced it at "twelve beans'
worth", and only then conceded the grandfathering that makes the whole tension impossible.

The mechanism decides it, and it is not a matter of degree. Check 14's candidate set is
`git diff --name-only $BASE -- .beans` plus untracked files, and each candidate is then
dropped by `[ "$was" = "completed" ] && continue`. **A completed bean the branch does not
touch is never a candidate; one the branch does touch is skipped.** There is no path by which
a stricter cell condition reaches the frozen corpus.

```
cmd:      bash tools/docs-lint.sh on a clean tree, with the completed corpus present and 44
          backtick-free evidence cells among them
observed: 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0

cmd:      the same, with one completed bean TOUCHED in the working tree
observed: FAIL check 11 .beans/modus-0004-…: appended 'A line appended to a completed
          bean.'; a completed bean may only gain a '## Amendments' section
          (adr:0005#amendments)
          docs-lint: 1 failure(s).
exit:     1
```

Check 11 fires; check 14 says nothing about it, in either direction. So:

- **A stricter cell condition flags nothing retroactively.** The 44 backtick-free cells and
  the 27 `met` cells stay exactly as they are, and no amendment is owed for any of them.
- **The only way to reach them is for the fix to deliberately widen check 14's scope** beyond
  the closing transition, which would be a separate decision with its own argument, and this
  bean does not ask for it.
- The grandfathering is therefore not a decision this bean owes. It is already the shape of
  the check.

### The habit this is the second instance of

This is the second bean this sprint in which I stated a blast radius as certainty without
measuring it, and both errors ran the same way: **the cost was estimated from the shape of
the change rather than from the mechanism.** "This condition is stricter, therefore more
things fail" is a plausible inference about a linter and a false one about this linter,
because what the check examines is decided by a diff and not by the condition.

An overstated cost is not the safe direction to be wrong in. It has the worse failure mode of
the two: an understated cost is discovered the moment somebody acts on it, and an overstated
one is discovered by nobody, because it stops anybody acting at all. A blast-radius estimate
is a claim and needs the same treatment as any other claim here. The general rule belongs in
a document rather than in this bean, and is routed to `modus-0089` — named by filename
because it is unmerged, and a typed reference to an unmerged bean fails check 6.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The six junk cells above are each observed either rejected, or accepted with the reason stated and argued | planted violation, reverted |
| 2 | Whatever is adopted is a requirement naming a property, not a list of rejected strings, and the plural/article/trailing-word variations above are observed against it | planted violation, reverted |
| 3 | `doc:05-authoring-for-agents#checks` states what a green check 14 does and does not assert, and its check 14 row carries an `Enforcement gap:` line naming the bean that closes the unmechanised half, per `doc:00-constitution#observed-failing` | diff |
| 4 | The retroactive effect on the `completed` corpus is measured and named per file at check 14's real cell scope — every table carrying an evidence column, not only those in a criteria or evidence region | analyser run over the corpus, before and after |
| 5 | What the analyser perceives about a cell is asserted separately from what it decides, and the NOEVCOL mask is either removed or its suppression of the per-criterion cascade is stated as deliberate with its reason | test-run |
| 6 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- An evidence kind recorded as evidence (`bean:0056`) and the numbering gate (`bean:0061`).
- Fence tracking (`bean:0063`) and check 6's copy of it.
- The set of accepted evidence kinds. That is
  `doc:50-memory-and-evidence#evidence-kinds`' closed set and this bean does not touch it.
- Rewriting the evidence of any `completed` bean. The measured blast radius above settles
  that it is not required, and check 11 forbids it in place regardless.
