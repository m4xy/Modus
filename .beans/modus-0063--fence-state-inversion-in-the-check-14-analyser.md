---
# modus-0063
title: A quoted fence marker inverts the check 14 analyser's fence state for the rest of the file
status: completed
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

**Every transcript in this section was re-taken against `main` at `2c958e4`, on the tree
this closure is cut from — not quoted from the branch that implemented the change.** The
figures therefore differ from the ones in the implementing pull request wherever the corpus
has grown since, and where they do, the difference is stated rather than smoothed over.

Every plant is on `.beans/modus-0033`, a `status: todo` bean, closed by flipping its status
and appending the shape, then reverted with `git checkout -- .beans`; `git status
--porcelain` is empty after each run. Each planted shape is also a fixture in
`tools/docs-lint-test.sh`, where it is asserted rather than narrated.

### Criterion 1 — a quoted marker no longer changes how any other line is classified

`tools/docs-lint-test.sh` asserts the classification of individual lines directly, as tests
distinct from any verdict, and every residual carries a verdict assertion as well as a
perception one.

```
cmd:      bash tools/docs-lint-test.sh
expected: green on main
observed: docs-lint-test: 37 passed, 0 failed.
exit:     0
```

The assertions were then observed failing, once per mechanism. A suite proves only that it
can detect the mutation that was made, so each mechanism is mutated on its own:

```
cmd:      bash tools/docs-lint-test.sh against each mutation in turn, on main
observed: unmutated                                  rc=0  37 passed,  0 failed
          classifier only (pre-0063 toggle)          rc=1  21 passed, 16 failed
          citation site only (guard bypassed)        rc=1  34 passed,  3 failed
          citation scanner deleted                   rc=1  30 passed,  7 failed
exit:     0, 1, 1, 1
```

The classifier mutation replaces `fence_classify` with the toggle this bean removes, keeping
the real measurement helpers, so it isolates the classifier. The second bypasses
`citation_site()`; the third deletes the citation scanner outright, and is the complement of
the second — narrowing the scanner and deleting it are different faults, and the container
assertions pass under both, which is why the top-level-prose control exists.

The same run also measures the mechanisms this change did **not** touch, and two of them fail
open with the suite completely green:

```
cmd:      the same, mutating the mechanisms docs-lint-c14.awk inherited unchanged
observed: isevcol-true  (every column is an evidence column)  rc=0  37 passed, 0 failed
          allkinds-off  (HOLLOW detection disabled)           rc=0  37 passed, 0 failed
          isevcol-false (no column is ever evidence)          rc=1  36 passed, 1 failed
          noevcol forced on (cascade suppressed)              rc=1  28 passed, 9 failed
exit:     0, 0, 1, 1
```

Recorded because it bounds what this closure claims: the two green rows make check 14 accept
beans it should reject and nothing in the suite notices. They are inherited untested, not
newly untested — the analyser is moved-verbatim code — and the test file's header says so.

The two green rows are not merely uncovered — here is what each lets through, on real
completed beans and on the shape check 14 was built to catch:

```
cmd:      allkinds-off, then the analyser over .beans/modus-0048
observed: unmutated  6 HOLLOW findings
          mutated    STATS 8 0  — no findings at all
exit:     0

cmd:      isevcol-true, then the analyser over a fixture in the bean:0045 shape — a
          numbered table in an evidence section whose only extra column is `evidence kind`,
          with cells that are not bare kind names
observed: unmutated  NOEVCOL  Success criteria and evidence
          mutated    STATS 2 0  — no findings at all
exit:     0
```

`allkinds-off` would accept a `completed` bean whose evidence cells are bare kind names —
the defect `bean:0056` exists for. `isevcol-true` removes the `NOEVCOL` guard, which is the
`bean:0045` defect check 14 was built to close.

On `modus-0030` and `modus-0032` that second loss is **masked** rather than clean: their
`NOEVCOL` disappears, but both stay flagged because their restated cells are bare kind names
and `HOLLOW` catches them instead. The fixture above is what shows the guard itself is gone,
since a bean whose cells are ordinary prose has nothing left to catch it. Stated precisely
because "NOEVCOL → gone" alone would overstate it on those two files and understate it in
general.


### Criterion 2 — the fails-OPEN plant is observed rejected

```
cmd:      the fails-OPEN control planted on modus-0033; bash tools/docs-lint.sh
expected: both criteria reported unanswered
observed: FAIL check 14 .beans/modus-0033-…: criterion 1 is not answered in the evidence;
          no evidence row bears its number and nothing cites it (adr:0005-…)
          FAIL check 14 .beans/modus-0033-…: criterion 2 is not answered in the evidence;
          no evidence row bears its number and nothing cites it (adr:0005-…)
          docs-lint: 2 failure(s).
exit:     1

cmd:      the fails-OPEN plant — the same file, its transcript quoting one fence marker
expected: refusal naming the fence, NOT the OK line the defect produced
observed: FAIL check 14 .beans/modus-0033-…: a fenced block opened at line 71 is never
          closed, so every line after it is read as code and no absence of evidence below
          it can be observed; close the fence, or — when the marker is part of a
          transcript's verbatim output — wrap that transcript in a longer fence so the
          quoted marker is content (doc:05-authoring-for-agents#checks)
          docs-lint: 1 failure(s).
exit:     1
```

Rejected, not merely no longer accepted. On the tree this bean was raised against the same
plant produced `docs-lint: OK — … 2 criteria checked`, exit 0.

### Criterion 3 — the fails-CLOSED plant is observed refused, naming the fence

**This criterion was amended by the orchestrator before review closed**; `## Criterion 3,
amended` above records what it said, who changed it and the argument. It asked for the plant
to be observed *passing*; it asks now for it to be observed **refused, with a message naming
the unterminated fence and telling the author how to disambiguate**.

```
cmd:      the fails-CLOSED plant — one lone fence marker above a filled evidence table
expected: refusal naming the fence and the remedy
observed: FAIL check 14 .beans/modus-0033-…: a fenced block opened at line 65 is never
          closed, so every line after it is read as code and no absence of evidence below
          it can be observed; close the fence, or — when the marker is part of a
          transcript's verbatim output — wrap that transcript in a longer fence so the
          quoted marker is content (doc:05-authoring-for-agents#checks)
          docs-lint: 1 failure(s).
exit:     1

cmd:      the fails-CLOSED control — the identical table, the stray marker removed
expected: clean
observed: docs-lint: OK — 19 documents, 107 anchors, 1094 references, 77 beans, 37 graph
          edges, 24 selectable, 77 bean ids, 0 introduced, 77 on origin/main, 1 closing
          transitions, 2 criteria checked, 0 unnumbered.
exit:     0
```

Against the original wording this is a refusal and not a pass. What changed for the author is
that two false `criterion N is not answered` lines naming the wrong problem became one line
naming the fence and the remedy.

### Criterion 4 — the corpus parses as it did

Both arms are run here, over the same corpus, with the pre-change analyser reconstructed
from the commit before the fix merged. **An earlier draft of this closure replaced the pair
with a name list and the prose claim "byte-identical"; that made the claim uncheckable
without redoing the reconstruction, and it is restored below.** Closure is the last moment
evidence can be added freely — `docs-lint` check 11 makes a `completed` bean append-only —
so it is the worst point in the workflow at which to trim.

```
cmd:      git show e756042^:tools/docs-lint.sh, its inline check 14 analyser extracted to
          pre-c14.awk, run over every `status: completed` bean on this branch
observed: BEFORE (analyser at e756042^): clean=21 flagged=7 total=28

cmd:      the analyser on this tree, over the same beans
observed: AFTER  (analyser on this tree): clean=21 flagged=7 total=28

cmd:      diff of the two full outputs, flagged files and per-file codes included
observed: IDENTICAL
          pre lines: 39   post lines: 39
exit:     0
```

The per-file codes, identical in both arms:

```
FLAGGED modus-0001--foundation-documentation-package
    NOEV
    UNANSWERED 1 .. UNANSWERED 13
FLAGGED modus-0028--normative-gate-commands
    EMPTYEV
    UNANSWERED 2, 3, 4, 5, 6
FLAGGED modus-0030--domainmgmt-domain-aggregate
    NOEVCOL  Success criteria and evidence
FLAGGED modus-0032--domain-id-shared-kernel
    NOEVCOL  Success criteria and evidence
FLAGGED modus-0048--extract-the-first-skills
    HOLLOW 3 citation, 4 citation, 5 citation, 6 citation, 7 diff, 8 test-run
FLAGGED modus-0051--parallel-bean-id-allocation
    UNANSWERED 5, 6
FLAGGED modus-0052--orchestrator-role
    HOLLOW 8 test-run
clean=21 flagged=7 total=28
```

**The denominator is 28 on this branch and 27 on `main`**, because this bean becomes
`completed` in this change and is itself clean. The criterion as written names 23, which is
what the corpus held when it was raised; the 23 are a subset of the 27, their findings are
unchanged, and the beans completed since — `modus-0036`, `0054`, `0055`, `0058`, closed in
`08936ee` — appear in neither arm's flagged list. The criterion is satisfied on its own
terms, on `main`'s corpus, and on this branch's.

### Criterion 5 — `./gradlew qualityCheck` green

```
cmd:      ./gradlew qualityCheck
expected: green, with docsLint and docsLintTest inside it
observed: > Task :docsLintTest
          docs-lint-test: 37 passed, 0 failed.
          > Task :docsLint
          docs-lint: OK — 19 documents, 107 anchors, 1094 references, 77 beans, 37 graph
          edges, 25 selectable, 77 bean ids, 0 introduced, 77 on origin/main, 0 closing
          transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 20s
          159 actionable tasks: 5 executed, 154 up-to-date
exit:     0
```

`0 closing transitions, 0 criteria checked` on that run is the point `bean:0096` records:
check 14 never examines the bean whose work the change contains. **The pull request carrying
this closure is the first run in which it examines this bean at all.**

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
