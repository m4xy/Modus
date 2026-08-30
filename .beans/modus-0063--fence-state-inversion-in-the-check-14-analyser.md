---
# modus-0063
title: A quoted fence marker inverts the check 14 analyser's fence state for the rest of the file
status: in-progress
type: fix
priority: high
created_at: 2026-08-29T00:00:00Z
---

# A quoted fence marker inverts the check 14 analyser's fence state for the rest of the file

Check 14's analyser tracks fenced blocks with a single toggle — every line matching
`^[ \t]*` followed by three backticks flips it, and while it is set the line is skipped
before any other rule sees it. The toggle has no notion of nesting, of an info string, or of
a fence marker that is *content*. A bean whose transcript quotes a fence marker on its own
line therefore contributes an **odd** flip, and every rule downstream reads the rest of the
file with its inside/outside sense reversed.

This is a defect, not a residual. `bean:0061`'s fence ruling says a `criterion N` citation
inside a fence must not answer a criterion, because in this repository fenced output quotes
this check's own `criterion N is not answered` message and would otherwise launder itself.
The same rule is what this bug breaks, in both directions.

## Observed

Planted on `.beans/modus-0033` — a `status: todo` bean — by flipping its status to
`completed` and appending the shape, then reverted with `git checkout -- .beans`. Every plant
ran `bash tools/docs-lint.sh`. Each pair is a plant and its own negative control, differing
only in the stray marker.

### Fails OPEN — the direction `bean:0061`'s fence ruling forbids

One `## Evidence` transcript that quotes a bare fence marker mid-way. A human reads one
fenced block; the analyser reads two, with the middle segment as prose.

```
control:  `## Success criteria` numbered 1 and 2, `## Evidence` holding one balanced
          fenced transcript that cites no criterion
observed: FAIL check 14 .beans/modus-0033-…: criterion 1 is not answered in the evidence; …
          FAIL check 14 .beans/modus-0033-…: criterion 2 is not answered in the evidence; …
          docs-lint: 2 failure(s).
exit:     1

planted:  the same, the transcript now quoting a fence marker on its own line, with the
          two lines below it reading
            FAIL check 14 …: criterion 1 is not answered in the evidence
            FAIL check 14 …: criterion 2 is not answered in the evidence
observed: docs-lint: OK — … 1 closing transitions, 2 criteria checked, 0 unnumbered.
exit:     0
```

`2 criteria checked` and exit 0. Both criteria are answered by pasted output stating that
they are unanswered — the exact laundering `bean:0061`'s fence ruling says can never be
allowed, reached without touching `doc:05-authoring-for-agents#checks`' rule.

### Fails CLOSED — a correct bean rejected

One stray marker above a filled evidence table hides the table.

```
planted:  `## Evidence`, one lone fence marker, then
            | # | criterion | observed |
            |---|---|---|
            | 1 | rejected | `docs-lint: 1 failure(s).` exit 1 |
            | 2 | clean | `git status --porcelain` empty |
observed: FAIL check 14 .beans/modus-0033-…: criterion 1 is not answered in the evidence; …
          FAIL check 14 .beans/modus-0033-…: criterion 2 is not answered in the evidence; …
          docs-lint: 2 failure(s).
exit:     1

control:  the identical table with the stray marker removed
observed: docs-lint: OK — … 1 closing transitions, 2 criteria checked, 0 unnumbered.
exit:     0
```

The evidence is present, verbatim and correctly numbered, and the check reports it absent.
The author is told to answer criteria that are already answered, with no indication that a
fence marker three lines earlier is the cause.

## Why it is intermittent

The parity, not the presence, is what decides. An even number of quoted markers balances
back and the file parses correctly; an odd number inverts everything after the last one. So
the same authoring habit produces a green run, a spurious failure or a spurious pass
depending on how many times the transcript happens to quote a marker, and the failure moves
as the bean is edited. No corpus bean is affected today:

```
cmd:      one awk over .beans/*.md counting line-initial fence markers per file and
          reporting any file with an odd count
observed: (no output — every bean is even)   7614 lines scanned
exit:     0
```

`bean:0055` is the one to watch. It is still `in-progress` on `main`, so its closing pull
request is the first place this can bite: it carries 22 fence markers, all balanced today,
and it is the bean most likely to quote one, because its subject **is** this analyser and its
evidence is transcripts of the analyser's own output. `bean:0061` (12) and `bean:0062` (2) are
likewise even. A single added transcript changes that for any of them.

## Options

| option | catches | cost |
|---|---|---|
| refuse to guess: fail the check when a bean's line-initial fence-marker count is odd | every case above, as an explicit error naming the file | one condition; rejects a legal Markdown file that renders correctly, so the message must say why |
| track the opening fence's marker length and close only on a marker at least as long, per CommonMark | a transcript quoting three backticks inside a four-backtick fence | correct by the spec, and it makes the fix a documented convention rather than a rule of this repository's own |
| require indented transcripts rather than fenced ones in beans | the class, by removing fences | a change to what a bean must contain, against the whole corpus |
| leave it, and disclose the parity on the `OK` line | nothing, but stops it being silent | the smallest honest move; `bean:0061` argues disclosure is what makes a blind spot tolerable |

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | A bean whose transcript quotes a fence marker an odd number of times no longer changes how any other line is classified, or is refused outright with a message naming the cause | test-run per direction |
| 2 | The fails-OPEN plant above is observed rejected, not merely no longer accepted | planted violation, reverted |
| 3 | The fails-CLOSED plant above is observed **refused**, with a message that names the unterminated fence and tells the author how to disambiguate — amended, see below | planted violation, reverted |
| 4 | The 23 beans `completed` on `main` and the beans in flight still parse as they do today | analyser run over the corpus, before and after |
| 5 | `./gradlew qualityCheck` green | test-run |

Every criterion here is binary, unlike `bean:0061`'s and `bean:0062`'s: this bean owes a code
change, not a decision.

## Criterion 3, amended

**This criterion was changed after the work was done, and this section exists so that a
reader sees the change rather than a criterion that quietly fits what was built.** A
criterion edited to match its own implementation is decorative; the only defence is that the
edit is visible and argued.

**Was:** "The fails-CLOSED plant above is observed passing."

**Is:** "The fails-CLOSED plant above is observed refused, with a message that names the
unterminated fence and tells the author how to disambiguate."

**Decided by:** the orchestrator, on the implementing agent's report, before review closed.
Not by the author, and not for the author's convenience. `doc:00-constitution#bean-lifecycle`
keeps this bean `in-progress` through its own pull request, so check 11's append-only rule
does not bind it yet and the criteria table is edited in place rather than amended.

**Argument.** The fails-CLOSED plant is one unterminated marker above a filled evidence
table. CommonMark, and every renderer, put that table inside a code block. Making the plant
pass therefore means deciding that this particular marker is content — a guess, and exactly
the guess the option table above rejects, because an allowlist of contexts in which a marker
is "really" content can always be written around. Refusing the file and naming the fence is
the better outcome and it is what criterion 1's second clause already admits: *"or is refused
outright with a message naming the cause"*. What the author is told changed from two false
`criterion N is not answered` lines naming the wrong problem to one line naming the fence and
the remedy.

## Not in scope

- The fence **rule** — that a citation inside a fence does not answer a criterion. That is
  settled in `doc:05-authoring-for-agents#checks` and in `bean:0061`; this bean is about the
  analyser failing to agree with itself on where a fence is.
- Anchored references into a bean. This bean cites `bean:0061` whole rather than one of its
  sections: a reference of the form `bean:NNNN` plus an `#anchor` fails check 6 however real
  the target's `<a id>` is, because check 6 resolves an anchor against a document's
  front-matter `provides` and a bean has no `provides` field. Observed here while writing this
  bean — the reference was replaced rather than the check changed. The `<a id>` markers in
  `bean:0061` are navigation for a human reader, not citable targets. Recorded, not fixed: it
  belongs to check 6, not to check 14.
- Every other consumer of fenced blocks in `tools/docs-lint.sh`. Only check 14 was measured;
  whether checks 6 and 10 share the toggle is this bean's first question.
- The numbering gate and the citation matcher (`bean:0061`).

## The fix

The single toggle is replaced by a CommonMark §4.5 fence state machine in
`tools/lib/docs-lint-fence.awk`, and the check 14 analyser moves beside it into
`tools/lib/docs-lint-c14.awk`. The split is not tidying: what the analyser **perceives** is
a separate concern from what it **decides**, and a verdict test cannot tell "read the file
correctly and judged it right" from "read it wrongly and judged it wrongly twice". Both
files are loaded with one `awk -f … -f …`, so there is one implementation and two
consumers.

Four properties replace the toggle, and none of them guesses which markers are content:

| property | what it stops |
|---|---|
| a fence closes only on a marker of the same character, at least as long as the one that opened it | a transcript quoting three backticks inside a four-backtick fence, which is the documented way to quote a marker |
| a backtick fence's info string may not contain a backtick | a line-initial inline code span opening a block |
| a marker indented four or more columns is not a delimiter | an indented chunk inverting the state of everything after it |
| a block that never closes fails check 14, naming the line | the whole ambiguous class, as an explicit refusal rather than a guess |

The second option in the table above, taken with the first. The **requirement** is the last
row: the file must say unambiguously where its fences are, and what stays ambiguous is
refused. An allowlist of prose contexts in which a marker is "really" content was rejected
without being tried — someone can always write a line it does not name, which is how it
fails open.

Generalised, the last row is also the first option in the table above: an odd count of
line-initial markers is exactly an unterminated block, so the parity rule is subsumed rather
than added beside.

## Evidence

Every plant below is on `.beans/modus-0033`, a `status: todo` bean, closed by flipping its
status and appending the shape, then reverted with `git checkout -- .beans`; `git status
--porcelain` is empty after each run. Each planted shape is reproduced as a fixture in
`tools/docs-lint-test.sh`, where it is asserted rather than narrated.

### Criterion 1 — a quoted marker no longer changes how any other line is classified

`tools/docs-lint-test.sh` asserts the classification of individual lines directly, as tests
distinct from any verdict, and every residual carries a verdict assertion as well as a
perception one — the rule F4 below states. The perception half is not the half that matters
on its own: F1 and F2 are perception divergences that were only visible as verdicts.

The tests were observed failing against the toggle they replace. The state machine was
swapped for a stand-in reproducing the old behaviour behind the same three function names,
the suite was run, and the stand-in was reverted:

```
cmd:      cp <classifier-only stand-in> tools/lib/docs-lint-fence.awk && bash tools/docs-lint-test.sh
expected: the perception assertions fail; the decision assertions largely pass. The stand-in
          keeps the real measurement helpers and replaces only fence_classify, so the
          mutation isolates the classifier
observed: FAIL perception: a three-backtick marker inside a four-backtick fence is content
          FAIL perception: an odd number of markers leaves a block open, and says so
          FAIL perception: a tilde fence is a fence, and a backtick marker inside it is content
          FAIL perception: a backtick in the info string is an inline code span, not a fence
          FAIL perception: a marker indented four columns is not a delimiter
          FAIL perception: a tab-indented marker is not a delimiter
          FAIL perception: a closing marker may carry nothing but whitespace
          FAIL verdict: a quoted fence marker is refused, not laundered into an answer
          FAIL verdict: quoted correctly, the pasted output stays inside the fence and answers nothing
          FAIL verdict: a stray marker above a filled table is named, not reported as missing evidence
          FAIL verdict: a tilde-fenced transcript cannot answer its own criteria
          FAIL perception: RESIDUAL: a fence indented into a list item is not seen
          FAIL perception: the length rule applies to tilde fences too
          FAIL perception: a shorter tilde marker does not close a longer tilde fence
          docs-lint-test: 21 passed, 16 failed.
exit:     1

cmd:      <the citation-site guard reverted to `s = tolower(line)`> && bash tools/docs-lint-test.sh
expected: the three container-block verdicts fail and nothing else does
observed: FAIL verdict: a fenced transcript indented into a list item cannot answer its criteria
          FAIL verdict: a block-quoted transcript cannot answer its criteria
          FAIL verdict: an indented chunk with no marker at all cannot answer its criterion
          docs-lint-test: 34 passed, 3 failed.
exit:     1

cmd:      <both mutations reverted> && bash tools/docs-lint-test.sh
observed: docs-lint-test: 37 passed, 0 failed.
exit:     0
```

Two mutations, because there are two mechanisms: the classifier decides where a fence is,
and the citation-site requirement decides where a citation counts. A single mutation would
have left one of them untested.

### Criterion 2 — the fails-OPEN plant is observed rejected

Before, reproducing this bean's `## Observed` exactly. The control and the plant differ only
in the stray marker:

```
cmd:      <fails-OPEN control planted> ; bash tools/docs-lint.sh
observed: FAIL check 14 .beans/modus-0033-…: criterion 1 is not answered in the evidence; …
          FAIL check 14 .beans/modus-0033-…: criterion 2 is not answered in the evidence; …
          docs-lint: 2 failure(s).
exit:     1

cmd:      <fails-OPEN plant> ; bash tools/docs-lint.sh
observed: docs-lint: OK — 19 documents, … 1 closing transitions, 2 criteria checked, 0 unnumbered.
exit:     0
```

After. The plant is refused, and the message names the line rather than the criteria:

```
cmd:      <fails-OPEN control planted> ; bash tools/docs-lint.sh
observed: FAIL check 14 .beans/modus-0033-…: criterion 1 is not answered in the evidence; …
          FAIL check 14 .beans/modus-0033-…: criterion 2 is not answered in the evidence; …
          docs-lint: 2 failure(s).
exit:     1

cmd:      <fails-OPEN plant> ; bash tools/docs-lint.sh
observed: FAIL check 14 .beans/modus-0033-…: a fenced block opened at line 71 is never
          closed, so every line after it is read as code and no absence of evidence below it
          can be observed; close the fence, or — when the marker is part of a transcript's
          verbatim output — wrap that transcript in a longer fence so the quoted marker is
          content (doc:05-authoring-for-agents#checks)
          docs-lint: 1 failure(s).
exit:     1
```

Rejected, not merely no longer accepted. The same evidence rewritten the way the message
directs — the transcript wrapped in a four-backtick fence — is read with the pasted output
inside the fence, where it answers nothing, and check 14 reports both criteria unanswered
(`tools/docs-lint-test.sh`, "quoted correctly, the pasted output stays inside the fence and
answers nothing").

### Criterion 3 — the fails-CLOSED plant is observed refused, naming the fence

```
cmd:      <fails-CLOSED plant: one lone marker above a filled table> ; bash tools/docs-lint.sh
expected: refusal, naming the unterminated fence and the remedy
observed: FAIL check 14 .beans/modus-0033-…: a fenced block opened at line 65 is never
          closed, so every line after it is read as code and no absence of evidence below it
          can be observed; close the fence, or — when the marker is part of a transcript's
          verbatim output — wrap that transcript in a longer fence so the quoted marker is
          content (doc:05-authoring-for-agents#checks)
          docs-lint: 1 failure(s).
exit:     1

cmd:      <fails-CLOSED control: the identical table, marker removed> ; bash tools/docs-lint.sh
observed: docs-lint: OK — 19 documents, … 1 closing transitions, 2 criteria checked, 0 unnumbered.
exit:     0
```

The criterion this answers is the amended one, and `## Criterion 3, amended` above records
what it said before, who changed it and on what argument. Against the original wording —
"observed passing" — this is a refusal, not a pass. The message names the line, the cause and
the two ways to disambiguate; the two false `criterion N is not answered` lines naming the
wrong problem are gone.

### Criterion 4 — the corpus parses as it did

The analyser was run over every `status: completed` bean, before and after, and a bean is
clean when it emits nothing but its `STATS` line:

```
cmd:      the check 14 analyser over the 23 `status: completed` beans, before the change
observed: clean=16 flagged=7 total=23
          flagged: modus-0001 (NOEV + 13 UNANSWERED), modus-0028 (EMPTYEV + 5 UNANSWERED),
          modus-0030 (NOEVCOL), modus-0032 (NOEVCOL), modus-0048 (6 HOLLOW),
          modus-0051 (2 UNANSWERED), modus-0052 (1 HOLLOW)
exit:     0

cmd:      the same, after the change
observed: clean=16 flagged=7 total=23
          the same seven files with byte-identical findings
exit:     0

cmd:      bash tools/docs-lint.sh, on the tree, before and after
observed: docs-lint: OK — 19 documents, 106 anchors, 914 references, 64 beans, 28 graph
          edges, 19 selectable, 64 bean ids, 0 introduced, 64 on origin/main, 0 closing
          transitions, 0 criteria checked, 0 unnumbered.
exit:     0
```

No corpus bean changes classification, so nothing newly flagged and nothing newly excused.
The beans in flight are unaffected for the same reason: every one of them is fence-balanced
today, and a balanced file that never nests is read identically by both implementations.

### Criterion 5 — `./gradlew qualityCheck` green

```
cmd:      ./gradlew qualityCheck
observed: > Task :docsLintTest
          docs-lint-test: 37 passed, 0 failed.
          > Task :docsLint
          docs-lint: OK — 19 documents, 106 anchors, 914 references, 64 beans, 28 graph
          edges, 19 selectable, 64 bean ids, 0 introduced, 64 on origin/main, 0 closing
          transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 14s
          159 actionable tasks: 5 executed, 154 up-to-date
exit:     0
```

`docsLintTest` is a dependency of `qualityCheck` rather than a command someone remembers,
because a test outside the aggregate is not run — the failure
`doc:00-constitution#observed-failing` records against check 11.

## Attacks tried against the fix

Each is a fixture in `tools/docs-lint-test.sh`, so a later change that reopens one fails the
build rather than being rediscovered.

| attack | outcome |
|---|---|
| a fenced transcript indented into a list item | **F1 — regression, found in review, now closed** by the citation-site requirement |
| a `>`-prefixed transcript, and an indented chunk with no marker at all | **F2/F3 — escapes older than this bean, now closed** by the same requirement |
| deleting one of the analyser's two files | **F5 — created by this bean's refactor, now closed**: a non-zero exit and a missing `STATS` line each fail the file |
| nested fences — three backticks inside a four-backtick block | survives; the inner marker is content and the outer block is not released |
| tildes as fence markers | survives; tilde fences are tracked, and the length rule applies to them. This was a *new* hole closed: the old toggle knew only backticks, so a tilde-fenced transcript was prose and every citation in it answered its criterion |
| a fence marker inside an inline-code span at line start | survives; a backtick in the info string means the line is not an opening fence |
| a fence marker in a table cell | survives; the line begins with a pipe and can never be a delimiter |
| an unterminated fence at end of file | refused by design, naming the opening line |
| a marker indented four columns, or by a tab | survives in the sense that matters — the marker is inert and can no longer invert the state of the lines after it |
| a marker at three columns of indent | still a delimiter, per CommonMark |
| a closing marker carrying an info string | does not close; only whitespace may follow |
| a closing marker longer than the opening one | closes, per CommonMark |
| CRLF line endings | close their fences; the carriage return is whitespace to the closing rule |

## What review found, and what it cost

The first version of this fix shipped three residuals asserted at the **perception** layer
only. Independent review turned two of them into verdicts and both changed the outcome; one
was a regression against the very toggle being replaced. The findings and their answers:

### F1 — a regression: a fenced transcript indented into a list item

```
cmd:      a closing bean whose `## Evidence` holds a `### ` sub-heading and, inside a list
          item, a four-column-indented fenced transcript reading
            FAIL check 14 …: criterion 1 is not answered in the evidence
            FAIL check 14 …: criterion 2 is not answered in the evidence
observed: this branch, first version:   docs-lint: OK — … 2 criteria checked   exit 0
          the parent commit's docs-lint.sh: FAIL check 14 …: criterion 1 is not answered…
                                            FAIL check 14 …: criterion 2 is not answered…
                                            docs-lint: 2 failure(s).   exit 1
```

Strictly weaker than the code it replaced. The old toggle's `^[ \t]*` opened a fence at any
indent and skipped the pasted output; the CommonMark rule correctly declines to treat a
four-column marker as a delimiter, and nothing else stopped the lines being citation-scanned.

### F2, F3 — block quotes and indented chunks, no marker required

The same escape needs no fence marker at all. A `>`-prefixed transcript and an
eight-column-indented one both closed the bean on the parent commit and on the first version
of this fix — `2 criteria checked`, exit 0, both criteria answered by output stating they are
unanswered. `> ` is how `.beans/modus-0033` already quotes text.

### The answer, and why it is not a loosening

The classifier is **not** widened. A marker indented four columns still does not open a
fence: making it open one puts back a delimiter whose meaning the file does not fix, which is
the inversion this bean exists to remove. The fix is in the **citation scanner**, as a
requirement on where a citation may stand:

> A `criterion N` citation answers its criterion only from top-level prose — not inside a
> fenced block, not inside a block quote, not on a line indented four or more columns.

CommonMark §4.4 and §5.1 render all three as code. A line-oriented reader sees prose. The
requirement makes the disagreement fail closed: a criterion cited only from inside a
container is unanswered. After it, all three plants are rejected, and F2 and F3 are rejected
where the parent commit accepted them — so the change is now strictly stronger in both
directions rather than weaker in one.

```
cmd:      the F1, F2 and F3 fixtures, after the citation-site requirement
observed: F1  FAIL … criterion 1 is not answered …  FAIL … criterion 2 …   exit 1
          F2  FAIL … criterion 1 is not answered …  FAIL … criterion 2 …   exit 1
          F3  FAIL … criterion 1 is not answered …                          exit 1
          parent commit on the same three: F1 exit 1, F2 exit 0, F3 exit 0
```

The corpus is unchanged by it: `clean=16 flagged=7 total=23`, the same seven files with
byte-identical findings, so no completed bean cites a criterion from inside a container.

### F4 — the rule this bean owes the next one

**My argument for residual 3 was wrong, and the way it was wrong is the finding.** I wrote
that an unrecognised marker is inert and therefore strictly better than one that inverts. The
**marker** is inert. The **content it delimits** is not: it becomes prose, and prose is
citation-scanned. F1 and F2 are that sentence failing.

Worse, the test named for residual 2 was **vacuous** — its fixture already answered criterion
1 from a filled evidence row, so its expected output held with or without the indented line,
and it survived the classifier mutation. And the two highest-blast-radius residuals were
asserted as `OUT OUT OUT OUT OUT` and nothing else.

> **Every residual needs a verdict assertion showing the divergence does not change the
> outcome. When it does change the outcome it is not a residual, it is a defect.**

A residual's whole claim is that a divergence is acceptable, and acceptability is a claim
about the outcome, not about the parse. Asserting the classification documents the divergence
without ever asking what it costs. The rule is now in `tools/docs-lint-test.sh`'s header,
where the next author of a residual will read it.

A second rule is in that header for the same reason. **A mutation suite proves only that the
tests can detect the mutation that was made, so it needs one mutation per MECHANISM, not per
file.** This analyser is two mechanisms — the classifier decides where a fence is, the
citation-site requirement decides where a citation counts — and the single classifier
mutation this bean shipped first left the second one entirely unexercised while the suite
reported green. Measured, all three on the same 31 assertions:

```
cmd:      bash tools/docs-lint-test.sh, against each mutation in turn
observed: classifier only (fence_classify -> the pre-bean:0063 toggle)   21 passed, 16 failed
          citation site only (citation_site bypassed)                    34 passed,  3 failed
          citation scanner deleted (s = "")                              30 passed,  7 failed
          unmutated                                                      37 passed,  0 failed
exit:     1, 1, 1, 0
```

The third is the complement of the second and is why the negative control exists: narrowing
the scanner and deleting it are different faults, and the container assertions pass under
both. What catches a deleted scanner is the top-level-prose control together with the six
open-defect pins, each of which asserts that something DOES answer.

**These figures went stale once and it is worth saying how.** They were measured at a
31-assertion suite; four assertions were added in a later round and all four numbers were
wrong at once, inside the comment block whose entire purpose is to state what the suite can
detect. A recorded measurement of a suite is invalidated by growing the suite, and nothing
mechanical says so. They are regenerated by one script rather than edited by hand.

### F5 — the coupling the refactor created

Moving the analyser to two files on disk made it possible for it to go **missing**, which an
inline program could not.

```
cmd:      one bean closing; delete tools/lib/docs-lint-fence.awk; bash tools/docs-lint.sh
expected: before the fix — awk exits 2, writes nothing, the read loop finds nothing, no
          failure fires, and the run reports OK
observed: FAIL check 14 .beans/modus-0033-…: the check 14 analyser exited 2 and examined
          nothing; tools/lib/docs-lint-fence.awk and tools/lib/docs-lint-c14.awk must both
          be present and parse
          docs-lint: 1 failure(s).
exit:     1

control:  the same bean with both files present
observed: docs-lint: OK — … 1 closing transitions, 2 criteria checked, 0 unnumbered.
exit:     0
```

Two conditions now stand between the analyser and a green run: a non-zero exit fails the
file, and a run producing no `STATS` line fails it. The counts line called itself the vacuity
assertion; until this it described rather than asserted.

## Residuals — each with the verdict assertion that makes it one

- **A fence inside a block quote, or indented into a list item, is not seen.** True at the
  perception layer and asserted there. It no longer changes the verdict, and that is asserted
  too: the transcript inside the container answers nothing, and a bean whose only evidence is
  quoted or indented has no entry and cannot close. Both directions fail closed.

## The second review round: the rule was written as an enumeration, and that was the defect

The citation-site rule shipped in the first round as three named non-sites — fence, block
quote, four-column indent. Review got past it with a **raw HTML block**. `<pre>` is allowed by
GitHub's sanitiser and renders exactly as a code block, but its content stands at column zero
with no marker and no `>`, so `citation_site()` returns 1. Verified here, and the class is
wider than one tag:

```
cmd:      the analyser over five fixtures, each a closing bean whose only evidence is the
          check's own failure message inside a container
observed: <pre>                        STATS 2 0   — both criteria answered
          <!-- HTML comment -->        STATS 1 0   — renders as NOTHING and still answers
          <details> wrapping a <pre>   STATS 1 0
          a line-initial ```json```    STATS 1 0
          a line-initial ```sh -c `date`   STATS 1 0
exit:     0 for all five
```

All five escape on `main` too, so none is a regression. **The lesson is about the shape of the
rule, not the count of containers.** An enumeration of excluded containers is an allowlist
wearing different clothes, and it fails on the first container nobody named — which is the
argument this bean already made against allowlists for the fence classifier, unapplied one
section away from where it was written.

That last clause is the transferable part, and it is not a lapse in reasoning. **A principle
gets applied where it was learned and not one function over.** The anti-allowlist argument
was written against the fence classifier, at the time the classifier was the question; the
citation rule was written later, against a different question, and nothing in the file
connects them. Both are mine, in one change, a few lines apart, and the second contradicts the
first. Neither review round caught it by reading — it took an attack that walked through the
enumeration.

So the generalisation is not "remember the principle". It is that **a principle stated in
prose beside one mechanism does not propagate to the next mechanism by being true**, and
nothing in this repository makes it propagate: no check reads it, and a reviewer would have to
hold two sections in mind at once to notice. The countermeasure available today is the cheap
one — when a rule is written as a list, ask what makes the list complete, and if the answer is
"the cases we thought of", rewrite it positively. A second instance of the same shape was
reported to me this round in the architecture tests, where one rule is scrupulously honest
about the limit of what it enforces while its neighbour, written in the same commit,
overclaims. I have not read that code and record it as reported, not verified.

`doc:05-authoring-for-agents#checks` now states the rule **positively**: a citation answers
only from top-level Markdown prose, inside no container of any kind, so anything unlisted is
excluded by construction rather than by omission. It carries an `Enforcement gap:` naming
`bean:0061`, because the rule states a property while the check does not.

**The gap line's first draft enumerated containers, one line below the sentence rewritten to
stop enumerating.** Review got past that too, with three more shapes, each verified here:

```
cmd:      the analyser over three further fixtures
observed: a lazy block-quote continuation (`> transcript follows`, then a bare citation
          line, which CommonMark 5.1 puts inside the quote)          STATS 1 0
          a bean's own YAML front matter (`title: criterion 1 …`)    STATS 1 0
          a list-item paragraph at one to three columns              STATS 1 0
exit:     0 for all three
```

So the gap line now names the **mechanism**: `citation_site()` blocks exactly two things —
four or more columns of indent, and a `>` on the citation's own line. That is complete, it
cannot go stale as containers are discovered, and it is the same requirement-over-allowlist
move as the sentence above it. Which is the third time in this change that the move was made
in one place and not in the next; see the paragraph above on propagation.

The two info-string shapes are the reading being **correct** and having an uncovered
consequence. `main` holds them only because its toggle flips ON and hides the rest of the
file — the defect this bean removes, which would equally hide a real evidence table. All six
open shapes are pinned as verdict assertions in `tools/docs-lint-test.sh` — five containers
and the even-parity case — which is the rule this bean encoded after the first round, applied
to itself. `<details>` was named in this bean's transcript for a round with no assertion
behind it; it has one now.

## Defect, open, and owed a bean

- **An EVEN number of quoted markers still answers the criterion.** Two quoted markers
  balance, so the segment between them is top-level prose — to this analyser and to every
  renderer alike, so unlike F1 and F2 there is no perception divergence to close and the
  citation-site requirement does not reach it. The verdict assertion shows the outcome
  changes, and by the rule above that makes it a **defect, not a residual**. It belongs to
  the citation matcher (`bean:0061`), not to fence tracking. `tools/docs-lint-test.sh` pins
  today's behaviour so the day it changes is visible, and it is reported for its own bean.

## Findings for the orchestrator

- **Check 6 still carries the original toggle** — `awk '/^```/ { fence = !fence; next }'` —
  and so still inverts on a quoted marker, deciding which `doc:`/`bean:`/`rule:` references
  are live. This bean put it out of scope, and it is left out of scope: changing it moves the
  `references` count on the `OK` line and can raise new check 6 failures across
  `documentation/`, which this branch does not own. It is a real defect and wants its own
  bean. Checks 10 and 11 do not track fences at all.
- **The repository had no test location for `tools/docs-lint.sh`.** `tools/docs-lint-test.sh`
  is new, with its fixtures as heredocs beside their assertions rather than in a fixture
  directory; a fixture whose expected output lives in another file is read twice and updated
  once. `build.gradle.kts` gains the `docsLintTest` task and the `qualityCheck` dependency —
  the one edit outside this bean's stated scope, made because the alternative is a test that
  never runs.
- **`doc:05-authoring-for-agents#checks` now understates check 14.** Its check 14 row lists
  the conditions that fail the check and does not name the unterminated fence, and the prose
  below it defines "answered … outside a fenced block" without saying what makes a fence.
  `documentation/` is owned by a sibling on this round, so the change is reported and not
  made.
