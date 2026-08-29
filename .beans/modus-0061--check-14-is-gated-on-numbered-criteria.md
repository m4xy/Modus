---
# modus-0061
title: Every tooth of check 14 is gated on criteria being numbered
status: todo
type: task
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# Every tooth of check 14 is gated on criteria being numbered

`docs-lint` check 14 (`doc:05-authoring-for-agents#checks`) decides what a table row is by
matching `^[0-9]+$` against its first cell. `NOEVCOL`, `UNANSWERED` and the `EMPTYCELL` /
`HOLLOW` cascade all hang off that one predicate, so **a bean that numbers nothing is
examined by none of them**. Un-numbering is a single edit and it is reachable from a red
build as a deliberate escape.

## Observed

Each shape planted on `.beans/modus-0033` — a `status: todo` bean — by flipping its status to
`completed` and appending the shape, then reverted with `git checkout -- .beans`. Every plant
ran `bash tools/docs-lint.sh`. The baseline is the failure an author would be escaping from;
the three escapes are one edit away from it and each changes only the criteria numbering.

```
baseline: | # | criterion | evidence kind | with rows 1, 2, 3
observed: FAIL check 14 .beans/modus-0033-…: the table under 'Success criteria and
          evidence' numbers criteria in an evidence section but carries no evidence
          column; 'evidence kind' states what will be produced, not what was observed
          (adr:0005-evidence-lives-in-the-work-item#evidence-home)
exit:     1

escape 1: the same table with the `#` column deleted
observed: docs-lint: OK — … 1 closing transitions, 0 criteria checked, 3 unnumbered.
exit:     0

escape 2: the same table, the `#` cells reading `1.` `2.` `3.` instead of `1` `2` `3`
observed: docs-lint: OK — … 1 closing transitions, 0 criteria checked, 3 unnumbered.
exit:     0

escape 3: `## Success criteria` as three bullets, `## Evidence` as an unnumbered table
          whose only extra column is `evidence kind` holding bare `test-run`,
          `citation`, `command`
observed: docs-lint: OK — … 1 closing transitions, 0 criteria checked, 3 unnumbered.
exit:     0
```

A bean created already `completed` whose entire evidence is `| criterion | evidence kind |`
over bare kind cells therefore exits 0. Nothing in it was observed; nothing in it was checked.

## Why this is a residual and not a hole <a id="honest"></a>

The distinction is the whole reason this is `type: task` and not `type: fix`.

| input | `OK` line | what the line claims |
|---|---|---|
| the defect `bean:0055` found and fixed | `1 closing transitions, 3 criteria checked` | **false**: three criteria examined, no evidence anywhere |
| every escape above | `1 closing transitions, 0 criteria checked, 3 unnumbered` | **true**: nothing was examined, and the count says so |

The pre-fix check made a false claim of examination. This makes an honest report of
non-examination. A reader who reads the `OK` line is not misled — which is exactly the
property `bean:0055` criterion 11 exists to give, and it holds here.

## What is new against `bean:0055`

`bean:0055`'s residuals table already records that **eleven of the twenty-three beans
`completed` on `main` number no criteria**, and calls requiring numbered criteria a new rule
rather than that one. That much is not new.

What is new is the direction of travel. In the corpus, unnumbered criteria are an authoring
habit that predates the check. With check 14 merged, un-numbering is **a one-edit route out of
a failing build**, available to an author who has just been told their evidence column is
missing. The residual is no longer only "the check cannot see eleven old beans"; it is "the
check tells an author precisely which predicate to break".

## Absence of criteria entirely

The weaker form passes even more cleanly:

```
planted:  status: todo -> completed, plus `## Evidence`, one `### ` sub-heading, one
          line of prose, and no criteria section anywhere
observed: docs-lint: OK — … 1 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0
```

`0 unnumbered` is not a lie either — there were no criteria to number. But
`doc:00-constitution` §7.2 step 1 requires the work item to state its success criteria
**before** the work starts, so a bean closing with none is cheaply rejectable against a rule
that already exists, and nothing rejects it. (A reviewer cited this as §7.1; §7.1 is branch
protection. The criteria requirement is §7.2 step 1.)

## The citation matcher cannot tell a citation from a mention

Found while checking that this bean is itself visible to check 14. Flipped to `completed` as
written, with no `## Evidence` section at all:

```
planted:  .beans/modus-0061 status: todo -> completed, nothing appended
observed: FAIL check 14 …/modus-0061…: closes with no evidence section; …
          FAIL check 14 …/modus-0061…: criterion 3 is not answered …
          FAIL check 14 …/modus-0061…: criterion 4 is not answered …
          FAIL check 14 …/modus-0061…: criterion 5 is not answered …
exit:     1
```

Criteria 1 and 2 are absent from that list. They are "answered" by the sentence in
`#fence` above that reads *the words `criterion 1 and criterion 2` moved one line above the
fence* — prose **about** a citation, matched as a citation. A bean discussing criterion
numbering silently satisfies its own per-criterion condition.

This is the same widening `bean:0055` refused for a different reason: a matcher that accepts
"some section somewhere mentions this number" accepts too much. It is the counterpart to the
fence rule below — inside a fence the matcher reads nothing, and outside it the matcher reads
anything — and both are one decision about where a citation may be written.

## Two doc/code disagreements in what merged <a id="disagreements"></a>

### A `criterion N` citation inside a fence does not answer <a id="fence"></a>

`doc:05-authoring-for-agents#checks` said a criterion is answered by a `criterion N` citation
"anywhere in the bean", and the same list calls a fenced block an entry. The analyser
`next`s every fenced line before the citation scan, so the two statements disagree:

```
planted:  `## Success criteria` numbered 1 and 2; `## Evidence` holding one fenced
          transcript whose lines read `criterion 1: …` and `criterion 2: …`
observed: FAIL check 14 …: criterion 1 is not answered in the evidence; no evidence row
          bears its number and nothing cites it
          FAIL check 14 …: criterion 2 is not answered in the evidence; …
exit:     1

control:  the same bean with the words `criterion 1 and criterion 2` moved one line
          above the fence
observed: docs-lint: OK — … 1 closing transitions, 2 criteria checked, 0 unnumbered.
exit:     0
```

**Ruling: the code is right and the document was wrong.** A fence holds verbatim output, and
in this repository that output quotes this check's own messages. Six fenced lines in
`bean:0055` name a criterion by number, five of them being `criterion N is not answered in
the evidence`:

```
cmd:      awk over .beans/*.md — toggle a flag on every fence-marker line, and print
          each line INSIDE a fence matching /criteri(on|a)[^0-9a-z]*[0-9]/
observed: modus-0055: FAIL check 14 …: criterion 4 is not answered in the evidence;
          modus-0055:                  criterion 5 is not answered in the evidence;
          modus-0055: FAIL check 14 …: criterion 2 records 'test-run' — an evidence
          modus-0055: FAIL check 14 …: criterion 2 closes with an empty evidence
          modus-0055: FAIL check 14 …: criterion 1 is not answered in the evidence; …
          modus-0055:                  (identically for criteria 2 through 9)
exit:     0
```

A scan that read inside fences would let `bean:0055` answer criteria 1, 2, 4, 5 and the range
2–9 with pasted output stating that they are unanswered. Evidence would launder itself, in
the bean that built the check. This change therefore corrects
`doc:05-authoring-for-agents#checks` to say *outside a fenced block* and leaves
`tools/docs-lint.sh` alone. No bean in the corpus is affected: the failing shape is planted,
not live.

### `evidence` is exact-match, so a qualified header is rejected

`isevcol` compares the normalised header against the closed set `evidence`, `observed`,
`observed output`, `output`, `result`. A filled column headed `evidence (verbatim)` is not in
it:

```
planted:  | # | criterion | evidence (verbatim) | over two rows carrying real output
          (`BUILD FAILED` covered ratio 0.6; `reason: value class erasure`)
observed: FAIL check 14 …: the table under 'Success criteria and evidence' numbers
          criteria in an evidence section but carries no evidence column; …
exit:     1
```

The set is documented, so this is strictness rather than a defect, and it fails **closed** —
the author is told to rename a column, not let through. Recorded so the next author who hits
it finds it written down instead of rediscovering it against a red build.

## Options

| option | cost | what it buys |
|---|---|---|
| require numbered criteria in a closing bean — a new condition, `doc:00-constitution` §7.2 step 1 as its basis | one condition, and eleven grandfathered beans stay frozen under check 11 so none of them fails | closes all three escapes and the absent-criteria case at once |
| accept `1.` and `- ` as criterion numbering | smallest edit, one regex | closes escape 2 only; escapes 1 and 3 are untouched |
| reject a closing bean whose criteria count is zero | one condition, no numbering rule | closes the absent-criteria case; escapes 1–3 still report `3 unnumbered` and pass |
| leave it: the `OK` line already discloses the blind spot | none | the honest-report property above, and nothing more |

Deciding between them is this bean's work, not its premise.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | A closing bean whose criteria are unnumbered is either checked or refused; the chosen option is stated with its reason | citation |
| 2 | Each of the three escape routes above is observed against the changed check, not only against today's | test-run per route |
| 3 | A closing bean with no criteria section at all is decided the same way, explicitly | test-run |
| 4 | The eleven beans already `completed` on `main` still pass, or their exemption is stated | test-run over the corpus |
| 5 | A prose mention of a criterion number that is not a citation no longer answers it, or the looseness is stated as accepted | test-run |
| 6 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- `doc:05-authoring-for-agents#checks`'s fence wording and the `evidence (verbatim)` header.
  Both are recorded above and the first is corrected in the same change that raises this
  bean; neither waits on this work.
- Beans already `completed` on `main`. Check 11 freezes them and check 14 never re-reads them.
- The residuals `bean:0055` already owns: the qualified-kind cell, and zero-denominator
  citation.
