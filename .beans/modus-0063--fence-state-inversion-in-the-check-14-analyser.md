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
| 3 | The fails-CLOSED plant above is observed passing | planted violation, reverted |
| 4 | The 23 beans `completed` on `main` and the beans in flight still parse as they do today | analyser run over the corpus, before and after |
| 5 | `./gradlew qualityCheck` green | test-run |

Every criterion here is binary, unlike `bean:0061`'s and `bean:0062`'s: this bean owes a code
change, not a decision.

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
distinct from any verdict: 18 on perception, 9 on the verdict. The perception half is the
half that matters here, because every escape from this gate so far entered through the parse
while the decision tests passed.

The tests were observed failing against the toggle they replace. The state machine was
swapped for a stand-in reproducing the old behaviour behind the same three function names,
the suite was run, and the stand-in was reverted:

```
cmd:      cp <old-toggle stand-in> tools/lib/docs-lint-fence.awk && bash tools/docs-lint-test.sh
expected: the perception assertions fail; the decision assertions largely pass
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
          docs-lint-test: 13 passed, 14 failed.
exit:     1

cmd:      <stand-in reverted> && bash tools/docs-lint-test.sh
observed: docs-lint-test: 27 passed, 0 failed.
exit:     0
```

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

### Criterion 3 — the fails-CLOSED plant, and where this criterion was not met literally

```
cmd:      <fails-CLOSED plant: one lone marker above a filled table> ; bash tools/docs-lint.sh
expected: OK, exit 0 — the criterion as written
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

**This criterion is met by criterion 1's second clause and not by its own wording, and the
difference is the whole design.** A lone unterminated marker above a table is not a
misreading to be corrected: CommonMark and every renderer put that table inside a code
block, so making it "pass" means deciding that this particular marker is content — the guess
the option table rejects, and the shape three rounds of allowlists failed on the last gate.
What did change is what the author is told: two false `criterion N is not answered` lines
naming the wrong problem became one line naming the fence and the remedy. Criterion 3 is
recorded here as amended in substance rather than silently reinterpreted; the orchestrator
owns whether that is acceptable.

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
          docs-lint-test: 27 passed, 0 failed.
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

## Residuals, found by looking

None of these is fixed here. Each is asserted in `tools/docs-lint-test.sh` so it is a
recorded fact rather than an unknown.

- **An EVEN number of quoted markers still reads the segment between them as prose**, and so
  can still answer a criterion with pasted output. This is not a misreading: CommonMark and
  every renderer read that segment as prose too. The endpoint the fix reaches is that the
  analyser now perceives what a reviewer sees rendered, so laundering by this route means
  writing the false output as visible prose in the bean rather than hiding it in a
  transcript. Closing it further belongs to the citation matcher (`bean:0061`), not to fence
  tracking.
- **A four-column indented chunk is code to a renderer and is not a fence**, and
  `doc:05-authoring-for-agents#checks`' rule is written about fences. A citation inside one
  is honoured.
- **A fence inside a block quote, or indented into a list item, is not seen.** This analyser
  reads lines, not a block structure. Widening the indent tolerance to cover it is what the
  old toggle did; an unrecognised marker is inert, which is strictly better than one that
  inverts.

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
