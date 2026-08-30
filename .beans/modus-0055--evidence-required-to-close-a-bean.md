---
# modus-0055
title: A bean may not close without evidence
status: completed
type: fix
priority: high
order: AN
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T22:00:00Z
---

# A bean may not close without evidence

`adr:0005-evidence-lives-in-the-work-item#evidence-home` makes the bean the evidence record:
every success criterion carries the command, the expectation and the verbatim observed
output, beside the criterion it satisfies. **Nothing reads that.**

`docs-lint` check 11 guards a bean once it is `completed`; check 13c guards its id. Neither
looks at whether the criteria are answered on the way in. `bean:0045` was found closable
with four criteria and zero evidence, and it was caught by an agent's conscience rather than
by a gate — which `doc:00-constitution#mechanical-enforcement` says is the definition of not
enforced.

## Scope

Owned: `tools/docs-lint.sh`, `documentation/05-authoring-for-agents.md` §6, this bean.

Not owned: the completed beans that record evidence kinds where `adr:0005` requires observed
output. `bean:0056` amends them, and the corpus run below widens its scope from two beans to
four. This bean is the mechanism.

## Success criteria

Each condition observed rejecting a planted violation before it is claimed
(`doc:00-constitution#observed-failing`).

| # | criterion | evidence kind |
|---|---|---|
| 1 | A bean that closes with no evidence section fails the build | planted violation, reverted |
| 2 | A bean that closes with an evidence section carrying no entry fails the build | planted violation, reverted |
| 3 | A numbered criterion that nothing in the bean answers fails the build | planted violation, reverted |
| 4 | An evidence cell holding only an evidence-kind name — `test-run` where the run's output belongs — fails the build | planted violation, reverted |
| 5 | An empty evidence cell fails the build | planted violation, reverted |
| 6 | A fully evidenced closure passes, and the two corpus shapes both pass | negative control over the ten closures in PR #35 |
| 7 | The check is not inert in CI, and its output distinguishes "ran and found nothing" from "could not run" | a CI run observed red on a planted closure, and the `OK` line's denominators |
| 8 | The check count lives in `doc:05-authoring-for-agents#checks` and nowhere else | `docs-lint` green with the row added |
| 9 | `./gradlew qualityCheck` green | test-run |
| 10 | A criteria-and-evidence table with **no evidence column** fails the build, and so does one whose only extra column is `evidence kind` | planted violation, reverted |
| 11 | The run discloses what it could not check: unnumbered criteria are counted on the `OK` line | `docs-lint` output on a bean with bullet criteria |

## What the check reads, and why it is shaped this way

**Scope is check 11's diff shape, one status earlier.** A bean *closes* in a change when it
is `completed` in the working tree and was not `completed` on the merge base. That covers
`in-progress` → `completed` (`doc:00-constitution#bean-lifecycle`), `todo` → `completed`, and
a bean created already `completed`. It never re-reads a bean that was already `completed` on
the base, which is what keeps it off the grandfathered corpus.

**The corpus writes evidence in two shapes and the check accepts both**, because eight of
the ten beans closing in PR #35 use the second:

| shape | form | completed beans on `main` |
|---|---|---|
| A | one `## Success criteria and evidence` table, one row per criterion, an `evidence` column | 13 |
| B | `## Success criteria` (or `## Restated criteria`) plus a separate `## Evidence` section, table-shaped or fenced-transcript-shaped | 10 |

An `evidence kind` column is a **plan** — what will be produced — and is deliberately not an
evidence column. A table carrying only that column records no observation, which is the
distinction `bean:0048` fell through.

## Decision — zero-denominator detection is a follow-up, not this bean

The brief asked whether a cited test run reporting `0 passed` and `0 failed` can be rejected
cheaply. **No, and the reason is specific to this corpus rather than general.**

The discriminator between *citing a vacuous run as proof of success* and *citing a vacuous
run as the defect being fixed* is not decidable from repository contents, and this repository
is unusual in how often it does the second: `doc:00-constitution` §9.1 is a table of ten
mechanisms that examined an empty set and reported success, and four completed beans quote
the vacuous output verbatim as their finding.

```
cmd:      grep -rn '0 beans\|0 graph edges\|- introduced\|0 passed\|no violation' \
            .beans/*.md    (excluding this bean)
observed: modus-0002:193  now fails … (30 passed, 1        <- a FALSE POSITIVE: '0 passed'
                                                              matched inside '30 passed'
          modus-0034:28   observed: BUILD SUCCESSFUL — no violation        (the defect)
          modus-0035:99   reports `0 beans, 0 graph edges, 0 selectable`   (the defect)
          modus-0051:274  … - introduced, - on origin/main.               (the defect)
          modus-0051:277  `- introduced, - on origin/main` against …      (the defect)
exit:     0
```

A regex for a zero denominator would fire on every one of them — on the beans that are this
repository's best evidence, for quoting the thing they caught. Rejecting a vacuous citation
needs the citation to name its denominator in a machine-readable field, which is a change to
what a bean must contain rather than a check over what beans contain today.

Raised as a follow-up rather than half-built. The narrower half **is** built: check 14's own
`OK`-line denominators (`N closing transitions, N criteria checked`) make *this* check
non-vacuous, which is the property `doc:00-constitution#observed-failing` demands of it.

## Deliberately not in scope

- Beans already `completed` on `main`. `modus-0001` carries no evidence section at all and
  `modus-0028`'s is empty; both are frozen by check 11 and check 14 never reads them.
- Amending the beans `bean:0056` covers.
- Requiring every bean to number its criteria. Six completed beans state them as unnumbered
  bullets; the per-criterion condition simply does not apply to those, and the `criteria
  checked` count on the `OK` line is what says so.

## Evidence

Merged as PR #41, squashed onto `main` as `8db6ac7`. Each cell carries the command, what it
was expected to do, and the run's own output. The plants numbered here are the fenced
transcripts below; the closing runs are the ones taken at `8181726` while this bean was being
closed, against the change that closes it.

| # | criterion | command | expectation | observed |
|---|---|---|---|---|
| 1 | A bean that closes with no evidence section fails the build | `bash tools/docs-lint.sh` with plant 1 in place | exit 1, naming the bean and the missing evidence | `FAIL check 14 .beans/modus-0033-…: closes with no evidence section; a criterion's command, expectation and verbatim observed output live in the bean (adr:0005-evidence-lives-in-the-work-item#evidence-home)`, exit 1 — and again at the closing commit, on **this** change: see closing plant C1 |
| 2 | A bean that closes with an evidence section carrying no entry fails the build | the same, with plant 2 — a `## Evidence` heading followed by one sentence of prose | exit 1, distinguishing "no section" from "a section with nothing in it" | `FAIL check 14 .beans/modus-0033-…: closes with an evidence section carrying no entry — no table row, no sub-heading, no transcript`, exit 1; and closing plant C2 |
| 3 | A numbered criterion that nothing in the bean answers fails the build | the same, with plant 3 — criteria numbered 1-5, evidence rows 1, 2 and 3 | exit 1 once per unanswered number, naming the numbers | `FAIL check 14 …: criterion 4 is not answered in the evidence; no evidence row bears its number and nothing cites it` and the same for `criterion 5`, exit 1; and closing plant C3 |
| 4 | An evidence cell holding only an evidence-kind name fails the build | the same, with plant 4 — row 2's cell reading `test-run` | exit 1, quoting the cell back and naming it a kind | ``FAIL check 14 …: criterion 2 records 'test-run' — an evidence KIND, not evidence; the cell must carry the command, the expectation and the verbatim observed output``, exit 1; and closing plant C4 |
| 5 | An empty evidence cell fails the build | the same, with plant 5 | exit 1, naming the criterion whose cell is empty | `FAIL check 14 …: criterion 2 closes with an empty evidence cell (adr:0005-evidence-lives-in-the-work-item#evidence-home)`, exit 1; and closing plant C5 |
| 6 | A fully evidenced closure passes, and the two corpus shapes both pass | `bash tools/docs-lint.sh` on the control and on this change; then `grep -n "^## Success criteria and evidence$"` and `grep -n "^## Evidence$"` over the four closing beans, to count the shapes rather than assert them | exit 0 on both runs, and each closing bean classified by the headings this bean's own shape table defines | control: `docs-lint: OK — … 1 closing transitions, 3 criteria checked, 0 unnumbered.`, exit 0. The closing run is the stronger control — `docs-lint: OK — … 4 closing transitions, 31 criteria checked, 0 unnumbered.`, exit 0 — and the four split **1 shape A, 3 shape B**: `modus-0054:99` is the only `## Success criteria and evidence`, while `modus-0036:120`, `modus-0055:112` and `modus-0058:48` each carry a separate `## Evidence`. Both shapes pass in one run; they are not evenly split, and the count is below |
| 7 | The check is not inert in CI, and its output distinguishes "ran and found nothing" from "could not run" | `GITHUB_TOKEN= gh run view <id> --json conclusion,event,headBranch`, a clone with `refs/remotes/origin/main` deleted, and the CI log of the pull request that closes this bean | a planted closure turns CI red where the check is claimed to run; a run with no merge base prints `-` rather than `0`; and a real closure is examined in CI rather than skipped | the two planted-closure runs are still on record and still red: `{"conclusion":"failure","event":"pull_request","headBranch":"feat/docs-lint-evidence-check","url":"…/runs/33264964045"}` and the same for `…/runs/33263152489`; the no-base run prints `- closing transitions, - criteria checked, - unnumbered`, exit 0; and CI on the closing pull request prints `4 closing transitions, 31 criteria checked, 0 unnumbered` — not `0`, not `-` — in the fence below |
| 8 | The check count lives in `doc:05-authoring-for-agents#checks` and nowhere else | `grep -rn "checks" build.gradle.kts` and `sed -n '1,3p' tools/docs-lint.sh` | neither the build file nor the script states a number; both name the table that counts them | `build.gradle.kts:19:// The mechanical checks of doc:05-authoring-for-agents#checks — counted there and`; `# docs-lint — the mechanical checks of doc:05-authoring-for-agents#checks. That table` / `# is the one place the checks are counted; a count repeated here would drift, and did.` |
| 9 | `./gradlew qualityCheck` green | `./gradlew qualityCheck` | green with `docsLint` inside it, on the tree that closes these four beans | `BUILD SUCCESSFUL in 15s`, `158 actionable tasks: 4 executed, 154 up-to-date`, `> Task :docsLint` printing `docs-lint: OK — … 4 closing transitions, 31 criteria checked, 0 unnumbered.` — the same line the control in the plant block below carries, taken through the gate rather than through the script |
| 10 | A criteria-and-evidence table with no evidence column fails the build, and so does one whose only extra column is `evidence kind` | `bash tools/docs-lint.sh` with plants 6, 7 and 8 | exit 1 on the table rather than on the criteria it fails to answer, so the message names the root cause | `FAIL check 14 …: the table under 'Success criteria and evidence' numbers criteria in an evidence section but carries no evidence column; 'evidence kind' states what will be produced, not what was observed`, exit 1 — the same input that exited 0 with `3 criteria checked` before the fix; and closing plant C6 |
| 11 | The run discloses what it could not check: unnumbered criteria are counted on the `OK` line | `bash tools/docs-lint.sh` on a closure whose criteria are bullets | the line separates "checked" from "could not check", and both counts move | `docs-lint: OK — … 1 closing transitions, 0 criteria checked, 2 unnumbered.`, exit 0. Re-observed on this change as closing plant C7 — `bean:0058`'s seven criteria restated as bullets moves the line to `4 closing transitions, 24 criteria checked, 7 unnumbered`, exit 0, against `31 criteria checked, 0 unnumbered` for the same tree with the table in place. That is the count doing its job twice over: `bean:0036` stated its criteria as bullets, and **numbering them into a table** to close it is the behaviour this disclosure exists to provoke |

### The two shapes, counted rather than asserted

An earlier draft of criterion 6's cell said the four closures were "two in shape A and two in
shape B". That was a classification claim with no command behind it, and it was **wrong** —
in the bean whose whole subject is that a cell must record what was observed. Review caught
it. The count, by the definitions in the shape table above:

```
cmd:      grep -n "^## Success criteria and evidence$\|^## Success criteria$\|^## Evidence$" \
            .beans/modus-0036--defensive-copy-rule.md \
            .beans/modus-0054--cost-baseline-and-run-recorder.md \
            .beans/modus-0055--evidence-required-to-close-a-bean.md \
            .beans/modus-0058--unwritten-working-conventions.md
observed: .beans/modus-0058-…:36:## Success criteria
          .beans/modus-0058-…:48:## Evidence
          .beans/modus-0054-…:99:## Success criteria and evidence
          .beans/modus-0036-…:30:## Success criteria
          .beans/modus-0036-…:120:## Evidence
          .beans/modus-0055-…:32:## Success criteria
          .beans/modus-0055-…:112:## Evidence
exit:     0
```

One shape A — `modus-0054` — and three shape B. `modus-0036` is shape B on this test even
though its closing evidence lives under a combined `## Criteria met — the closing evidence`
heading, because the section that names its criteria and the section that holds the
transcripts are two, which is what shape B is. Review confirmed the reading independently by
renaming that heading to force a pure split: `docs-lint` still printed
`4 closing transitions, 31 criteria checked, 0 unnumbered`, so nothing about the pass depends
on how that one heading is worded.

The criterion is met either way — shape A is exercised by `modus-0054` and shape B by the
other three, in the same run — and it is met by a smaller margin than the sentence claimed.
Recording the smaller margin is the point.

### The closing change, planted seven ways

The plants below were made **at the closing commit, on the change that closes this bean**,
not on the branch that built the check. Each one goes into `.beans/modus-0058` — a bean
closing in this same pull request — and each was restored from a copy taken before the plant.
`doc:00-constitution` §9.1 asks for a mechanism observed where it is claimed to run; the
input check 14 is claimed to read is a closure, and this is one.

Every plant was **re-run after review**, against the final state of this change, and one of
them stopped failing — see C3a. An earlier revision of this block quoted the plants as run
against an earlier revision of the same change; those transcripts had gone stale under
edits made to `modus-0058` in between, which is the defect `bean:0091` now carries.

```
control:  the four closures and the new bean, as this change stands
observed: docs-lint: OK — [... nine corpus counts, elided: they describe the tree at the
          moment of the run and move whenever a word is added to this change, including
          by this transcript. The elision is marked because that is the rule; the full
          line for an immutable tree is in the CI block below ...]
          4 closing transitions, 31 criteria checked, 0 unnumbered.
exit:     0

C1        `## Evidence` renamed `## Notes`, so the closure has no evidence home
observed: FAIL check 14 .beans/modus-0058-…: closes with no evidence section; a criterion's
          command, expectation and verbatim observed output live in the bean
          (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0058-…: criterion 3 is not answered in the evidence; no
          evidence row bears its number and nothing cites it (…#evidence-home)
          FAIL check 14 .beans/modus-0058-…: criterion 5 is not answered in the evidence; …
          docs-lint: 3 failure(s).
exit:     1

C2        the same, plus a `## Evidence` section holding one sentence of prose
observed: FAIL check 14 .beans/modus-0058-…: closes with an evidence section carrying no
          entry — no table row, no sub-heading, no transcript (…#evidence-home)
          (and the same two unanswered-criterion lines as C1)
          docs-lint: 3 failure(s).
exit:     1

C3a       evidence row 6 deleted, and the sub-heading that cites it renamed
observed: docs-lint: OK — … 4 closing transitions, 31 criteria checked, 0 unnumbered.
exit:     0        <- PASSES. This plant failed before review and passes after it: a
                      correction added the words "which is what criterion 6 reads" to a
                      line of prose, and that prose now answers the criterion whose
                      evidence row was just deleted.

C3b       the same, plus that one prose citation neutralised
observed: FAIL check 14 .beans/modus-0058-…: criterion 6 is not answered in the evidence; no
          evidence row bears its number and nothing cites it (…#evidence-home)
          docs-lint: 1 failure(s).
exit:     1

C4        row 7's observed cell replaced by `test-run`
observed: FAIL check 14 .beans/modus-0058-…: criterion 7 records 'test-run' — an evidence
          KIND, not evidence; the cell must carry the command, the expectation and the
          verbatim observed output (…#evidence-home, doc:50-memory-and-evidence#evidence-kinds)
          docs-lint: 1 failure(s).
exit:     1

C5        row 7's observed cell emptied
observed: FAIL check 14 .beans/modus-0058-…: criterion 7 closes with an empty evidence cell
          (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          docs-lint: 1 failure(s).
exit:     1

C6        the evidence table's `observed` column renamed `evidence kind`
observed: FAIL check 14 .beans/modus-0058-…: the table under 'Evidence' numbers criteria in
          an evidence section but carries no evidence column; 'evidence kind' states what
          will be produced, not what was observed (…#evidence-home)
          docs-lint: 1 failure(s).
exit:     1

C7        the criteria table restated as seven bullets (the disclosure, not a failure)
observed: docs-lint: OK — … 4 closing transitions, 24 criteria checked, 7 unnumbered.
exit:     0

restored: `modus-0058` copied back from the pre-plant copy; `git status --short` lists the
          four modified beans and the one new one, and nothing else
observed: docs-lint: OK — … 4 closing transitions, 31 criteria checked, 0 unnumbered.
exit:     0
```

**C3a is the finding, and it is worth more than the six rejections.** The plant was not
weakened; the bean around it changed. A correction elsewhere in `modus-0058` added the phrase
"which is what criterion 6 reads" to ordinary prose, and `doc:05-authoring-for-agents#checks`
says a citation by number answers a criterion — so a sentence written for a human reader
silently took over the job of an evidence row, and deleting that row stopped being detectable.
Nothing about the check changed between the two runs. This is `bean:0061`'s
"the citation matcher cannot tell a citation from a mention", reproduced by accident, in a
live closure, by an author who was fixing something else. It is one more reason the
per-criterion condition is weaker than the `OK` line makes it look, and it is why C3b exists.

And the same closure through the gate in CI, where the check is claimed to run. This is the
first time check 14 has examined a real closing transition in CI: every prior CI observation
was of a plant, or of a branch that closed nothing.

```
run:      33278170814 (pull_request, https://github.com/m4xy/Modus/actions/runs/33278170814)
head:     b643f08 — the commit that closes these four beans
observed: build + mechanical gates
            docs-lint: OK — 19 documents, 106 anchors, 918 references, 64 beans,
            28 graph edges, 21 selectable, 64 bean ids, 0 introduced, 64 on origin/main,
            4 closing transitions, 31 criteria checked, 0 unnumbered.
            BUILD SUCCESSFUL in 27s
            162 actionable tasks: 48 executed, 114 from cache
jobs:     which halves             pass
          build + mechanical gates pass
          backoffice + e2e         skipping
          gate                     pass
exit:     0
```

At that commit the CI line was byte-identical to the local one — review confirmed it
independently by extracting the line from the run's raw log and comparing digests, both
`e2f1607cf90c9e46ac6c677f4fcefc71`. That is the property `bean:0045`'s escape and check 11's
two inert runs both lacked: the same denominators locally and in CI, on the same input.

That line is quoted in full because `b643f08` is immutable: anyone can check out that commit
and reproduce it exactly. The controls above are not, which is why their corpus counts are
elided rather than quoted — this change has since grown by review's corrections and by one
new bean, and every one of those edits moved the `references` figure, including the sentence
you are reading.

**The counts check 14 owns did not move through any of it**:
`4 closing transitions, 31 criteria checked, 0 unnumbered`, locally and in CI, before and
after the corrections, with and without `bean:0091`. That is the split worth knowing. The
corpus counts describe the tree at the instant of the run and are self-referential in a
document that reports them; the check-14 counts describe the four closures, and only a change
to a closure moves them. Quote the second verbatim, elide the first with a marker, and pin
anything that must be exact to a commit.

The `references` count in that control is self-referential and is left as observed rather
than quietly corrected: writing a transcript into a bean adds typed references, so the figure
moved from `917` to `918` when this block was first written and to `924` once review's
corrections and `bean:0091` were in. The control above is the last of those runs and the
plants were re-run against it, so the block is internally consistent — and it will be wrong
again the moment anything else is added to this change. The counts check 14 owns do not have
that property. The useful discipline is to say which numbers are self-referential and re-take
the transcript when the tree it describes changes materially, not to chase a figure that moves
because it is being written down.

C1 and C2 are worth reading past the headline. Both report criteria 3 and 5 unanswered and
**not** criteria 1, 2, 4, 6 and 7 — because those five are cited by number in prose or in a
sub-heading elsewhere in that bean, and the citation rule stated at
`doc:05-authoring-for-agents#checks` says a citation answers a criterion. So the check is not
counting table rows; it is applying the definition, and gutting the table left exactly the
two criteria that had nothing else naming them. The renaming in C6 is the escape hatch
`bean:0045` shipped with and the one an author reaches for first; it is rejected by name.

Criteria 1-5, each planted against `.beans/modus-0033` — a `status: todo` bean — by flipping
its status to `completed` and appending the shape under test, then reverted with
`git checkout -- .beans`. Every plant ran `bash tools/docs-lint.sh` and exited 1.

```
planted:  status: todo -> completed, nothing else
observed: FAIL check 14 .beans/modus-0033-…: closes with no evidence section; a
          criterion's command, expectation and verbatim observed output live in the bean
          (adr:0005-evidence-lives-in-the-work-item#evidence-home)
exit:     1

planted:  the same, plus "## Evidence" followed by "It all worked when I ran it."
observed: FAIL check 14 .beans/modus-0033-…: closes with an evidence section carrying no
          entry — no table row, no sub-heading, no transcript
          (adr:0005-evidence-lives-in-the-work-item#evidence-home)
exit:     1

planted:  the same, plus a criteria table numbered 1-5 and an evidence table with rows
          1, 2 and 3
observed: FAIL check 14 .beans/modus-0033-…: criterion 4 is not answered in the evidence;
          no evidence row bears its number and nothing cites it
          (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0033-…: criterion 5 is not answered in the evidence;
          no evidence row bears its number and nothing cites it
          (adr:0005-evidence-lives-in-the-work-item#evidence-home)
exit:     1

planted:  the same, plus "## Success criteria and evidence" whose row 2 evidence cell is
          `test-run`
observed: FAIL check 14 .beans/modus-0033-…: criterion 2 records 'test-run' — an evidence
          KIND, not evidence; the cell must carry the command, the expectation and the
          verbatim observed output (adr:0005-evidence-lives-in-the-work-item#evidence-home,
          doc:50-memory-and-evidence#evidence-kinds)
exit:     1

planted:  the same, with row 2's evidence cell empty
observed: FAIL check 14 .beans/modus-0033-…: criterion 2 closes with an empty evidence
          cell (adr:0005-evidence-lives-in-the-work-item#evidence-home)
exit:     1

control:  the same closure, row 2's cell reading
          `BUILD SUCCESSFUL in 26s`, `167 actionable tasks`
observed: docs-lint: OK — 19 documents, 105 anchors, 844 references, 56 beans,
          26 graph edges, 15 selectable, 56 bean ids, 1 introduced, 55 on origin/main,
          1 closing transitions, 3 criteria checked, 0 unnumbered.
exit:     0

reverted: git checkout -- .beans
observed: docs-lint: OK — … 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0
```

The control is the criterion-6 half that matters: the same bean, the same transition, the
same two criteria — one cell's content is the whole difference between exit 1 and exit 0,
and the denominator moves from `0 closing transitions` to `1`.

### Criterion 10 — the check had `bean:0045`'s own defect, and it was invisible

Review found it. The first version keyed every content condition on `evcol`, the index of a
column headed `evidence` or `observed`, and in the combined region a numbered row set both
`C[n]` and `A[n]`. So a `## Success criteria and evidence` table with **no evidence column at
all** satisfied every criterion by restating it, and the fix was to rename a column header.

That is the defect this bean exists to close, inside the check that closes it, and neither
the bean nor `doc:05` disclosed it — both read as enforcement.

```
before:   status: todo -> completed, plus

            ## Success criteria and evidence
            | # | criterion |
            |---|---|
            | 1 | the writer refuses to lower a covered count |
            | 2 | the recorded reason survives a rewrite |
            | 3 | `./gradlew qualityCheck` green |

observed: docs-lint: OK — … 1 closing transitions, 3 criteria checked.
exit:     0        <- three criteria examined, no evidence anywhere, green

before:   the same, third column headed `evidence kind` holding bare `test-run`
observed: docs-lint: OK — … 1 closing transitions, 3 criteria checked.
exit:     0
```

After: a numbered row answers its criterion only when the table carries an evidence column,
and a numbered table in an evidence section without one is rejected outright.

```
planted:  the first shape above
observed: FAIL check 14 .beans/modus-0033-…: the table under 'Success criteria and
          evidence' numbers criteria in an evidence section but carries no evidence
          column; 'evidence kind' states what will be produced, not what was observed
          (adr:0005-evidence-lives-in-the-work-item#evidence-home)
exit:     1

planted:  the second shape above (`evidence kind` holding bare `test-run`)
observed: FAIL check 14 .beans/modus-0033-…: the table under 'Success criteria and
          evidence' numbers criteria in an evidence section but carries no evidence column;
          …
exit:     1

planted:  shape B — `## Success criteria` with an `evidence kind` column, and a
          `## Evidence` table headed `| # | criterion |`
observed: FAIL check 14 .beans/modus-0033-…: the table under 'Evidence' numbers criteria
          in an evidence section but carries no evidence column; …
exit:     1

control:  the same table, the column renamed `evidence` and filled with observed output
observed: docs-lint: OK — … 1 closing transitions, 3 criteria checked, 0 unnumbered.
exit:     0
```

The lesson is not that the check had a bug. It is that **planting violations of the rule
proves the gate fires; only planting the shape you claim it catches proves the claim.** Five
plants all used a table that carried an evidence column, so five plants all missed the one
input where the column is absent.

Observed in CI on the fixed check, not only locally — the escape hatch itself, planted on
this bean and reverted:

```
run:      33264964045 (pull_request, https://github.com/m4xy/Modus/actions/runs/33264964045)
planted:  .beans/modus-0055 status: in-progress -> completed, its `## Evidence` section
          replaced by a `## Success criteria and evidence` table headed `| # | criterion |`
observed: > Task :docsLint FAILED
          FAIL check 14 .beans/modus-0055-…: the table under 'Success criteria and evidence'
          numbers criteria in an evidence section but carries no evidence column; 'evidence
          kind' states what will be produced, not what was observed
          (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          docs-lint: 1 failure(s).
          Execution failed for task ':docsLint' (registered in build file 'build.gradle.kts')
          BUILD FAILED in 26s
jobs:     which halves            success
          build + mechanical gates failure
          backoffice + e2e         success
          gate                     failure
exit:     1
reverted: git reset --hard <the commit before the plant>, force-pushed
```

That is the same input that exited 0 with `3 criteria checked` before the fix, run through
the gate where it is claimed to run.

### Criterion 11 — the run discloses what it could not check

`n_c14_unnum` was accumulated and never printed, which made the blind spot invisible: eleven
of the twenty-three completed beans on `main` state their criteria as bullets, and the check
reports `0 criteria checked` for each without saying why.

```
planted:  a closure whose criteria are two bullets under `## Success criteria`, with a
          filled `## Evidence` table
observed: docs-lint: OK — … 1 closing transitions, 0 criteria checked, 2 unnumbered.
exit:     0
```

`0 criteria checked, 2 unnumbered` is the honest report: the per-criterion condition did not
apply, and the line says so rather than implying a clean pass.

### Criterion 7 — the check is not inert, and an inert run says so

`bean:0038`'s check 11 shipped inert twice and `bean:0051`'s check 13 went inert in CI, both
printing `OK`. Three things separate check 14 from that.

**It shares `BASE` with checks 11 and 13c.** One merge-base resolution serves all three, so
check 14 cannot be inert while they are live, and `.github/workflows/ci.yml`'s `build` job
already carries `fetch-depth: 0` for exactly that reason (`bean:0051`).

**`.beans/` classifies as the Kotlin half.** `ci.yml`'s `filter` step sends every path that
is not under `backoffice/` or `e2e/` to `kotlin=true`, so a bean change runs `build`, which
runs `qualityCheck`, which runs `docsLint`.

**An inert run reports `-`, not `0`.** Against a clone with `refs/remotes/origin/main`
deleted:

```
cmd:      git clone --no-hardlinks <worktree> inert && git update-ref -d refs/remotes/origin/main
          bash tools/docs-lint.sh
observed: docs-lint: OK — … 56 bean ids, - introduced, - on origin/main,
          - closing transitions, - criteria checked, - unnumbered.
exit:     0
```

`- closing transitions` is "could not run"; `0 closing transitions` is "ran and found
nothing"; `1 closing transitions` is the control above. Every count check 14 owns reads `-`
when there is no base, so no half of the line can look clean while the other half is inert. Check 11's inert CI runs were
distinguishable from real ones by one character and nobody was looking; this line makes the
same distinction for check 14 and prints it every run.

**Observed rejecting a planted violation in CI, not only locally.** `doc:00-constitution`
§9.1's sharpest clause: observe it where it is claimed to run. A commit planting a closure
of this bean with its evidence section gutted was pushed to `feat/docs-lint-evidence-check`
and reverted immediately after.

```
run:      33263152489 (pull_request, https://github.com/m4xy/Modus/actions/runs/33263152489)
planted:  .beans/modus-0055 status: in-progress -> completed, evidence section emptied
observed: > Task :docsLint FAILED
          FAIL check 14 .beans/modus-0055-…: closes with no evidence section; a criterion's
          command, expectation and verbatim observed output live in the bean
          (adr:0005-evidence-lives-in-the-work-item#evidence-home)
          FAIL check 14 .beans/modus-0055-…: criterion 1 is not answered in the evidence; …
          (identically for criteria 2 through 9)
          docs-lint: 10 failure(s).
          Execution failed for task ':docsLint' (registered in build file 'build.gradle.kts')
          BUILD FAILED in 31s
jobs:     which halves            success
          build + mechanical gates failure
          backoffice + e2e         skipped
          gate                     failure
exit:     1
reverted: git reset --hard <the commit before the plant>, force-pushed
```

`gate` going red is the second half: check 14 does not merely print, it fails the
aggregating job that `bean:0047` will make required.

The reverted branch, same workflow, same job:

```
run:      33263283039 (pull_request)
observed: > Task :docsLint
          docs-lint: OK — 19 documents, 103 anchors, 807 references, 53 beans,
          25 graph edges, 12 selectable, 53 bean ids, 1 introduced, 52 on origin/main,
          0 closing transitions, 0 criteria checked.
jobs:     which halves            success
          build + mechanical gates success
          backoffice + e2e         skipped
          gate                     success
exit:     0
```

`0 closing transitions` in CI, not `-`. The merge base resolved, the check ran, and it
found nothing to examine on a branch that closes no bean — which is the distinction check
11 could not make for four plants and check 13 could not make for a whole life in CI.


### Criterion 4, and what it finds on the live corpus

PR #35 merged while this was in review, so its ten closures are now `completed` on `main` and
check 14 will never read them again. The finding is preserved because it is the measurement
of what the check does to real beans, and because two of the ten are why `bean:0056` exists.

Every one of the twenty-three beans `completed` on `main`, run through check 14's analyser
directly — the diff scope skips them all, so this is the analyser, not the gate:

```
cmd:      for each completed bean on origin/main: awk -f <check 14's analyser> <bean>
observed: clean=16 flagged=7 total=23
          modus-0001  NOEV, then UNANSWERED 1..13   — no evidence section at all
          modus-0028  EMPTYEV, then UNANSWERED 2..6 — an evidence section with no entry
          modus-0030  NOEVCOL 'Success criteria and evidence'
          modus-0032  NOEVCOL 'Success criteria and evidence'
          modus-0048  HOLLOW 3 citation; 4 citation; 5 citation; 6 citation; 7 diff;
                      8 test-run
          modus-0051  UNANSWERED 5; UNANSWERED 6    — see below
          modus-0052  HOLLOW 8 test-run
exit:     n/a (analyser run, not the gate — the diff scope skips all 23)
```

`modus-0030` and `modus-0032` are new information: `bean:0056` named two beans that record
evidence kinds where `adr:0005` requires observed output, and there are **four**. Both are
frozen by check 11, so the finding is recorded in `bean:0056` — widened by this change from
two beans to four, with the corpus run above as its basis — and nothing here edits either
bean. `modus-0001` and `modus-0028` are a different defect (no evidence at all rather than a
kind in place of one) and are left where the run put them.

### The `modus-0051` report is correct, and the rule it applies is now written down

`modus-0051`'s criteria 5 and 6 are answered at length under `## Decision — detect, not
prevent` and `## Decision — where the allocation rule lives`. Neither section names the
criterion it answers, so check 14 reports both unanswered.

Review called this a false positive on the ground that "a criterion is answered by a
`criterion N` citation" was a convention invented here and stated in no normative document.
That half was right and is fixed: `doc:05-authoring-for-agents#checks` now states the three
definitions check 14 depends on — what an entry is, what an evidence column is, and what
answers a criterion — so the matcher enforces a rule instead of an assumption.

The report itself stands. `adr:0005-evidence-lives-in-the-work-item#evidence-home` puts the
evidence *beside the criterion it satisfies*; a decision section that never names the
criterion is not beside it, and a reader cannot recover the pairing. Widening the matcher to
accept "some section somewhere discusses this" would accept every bean, which is the
`bean:0045` shape again. A decision-shaped criterion is answered by writing `criterion 5`
into the heading that answers it — one word, and the pairing survives.

### What was deliberately not made retroactive

`modus-0001` carries no evidence section at all, `modus-0028`'s carries no entry, and four
more record kinds where output belongs. All six are `completed` on `main` and frozen by
check 11, and check 14 skips any bean the base already closed, so none fails the build. A
rule applying from adoption is the same call `bean:0038` made for `bean:0010`'s in-place
corrections.

### Residuals, each disclosed rather than fixed

| residual | why it is left | where it shows |
|---|---|---|
| a cell reading `` `citation`, per skill `` escapes the bare-kind condition — only a cell whose every token is a kind name is rejected | widening it needs a rule for what a qualifier is, which is a change to what a bean must contain | `modus-0048` criterion 2 |
| eleven of twenty-three completed beans number no criteria, so the per-criterion condition cannot apply | requiring numbered criteria is a new rule, not this one | the `unnumbered` count on every `OK` line |
| zero-denominator citation | the decision above | `doc:00-constitution` §9.1 |

The first two are the reason criterion 11 exists: the check now reports the size of its own
blind spot on every run, so the residual is a number a reader sees rather than a paragraph
in a bean nobody reopens.

### Bash 3.2

`docs-lint.sh` claims bash 3.2 compatibility and the gate never exercises it (`bean:0049`).
Verified directly:

```
cmd:      /bin/bash --version
observed: GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25)

cmd:      /bin/bash tools/docs-lint.sh
observed: docs-lint: OK — … 0 closing transitions, 0 criteria checked.

cmd:      /bin/bash tools/docs-lint.sh, with plant 1 in place
observed: FAIL check 14 .beans/modus-0033-…: closes with no evidence section; …
exit:     1
```
