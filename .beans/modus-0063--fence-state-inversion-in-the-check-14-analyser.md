---
# modus-0063
title: A quoted fence marker inverts the check 14 analyser's fence state for the rest of the file
status: todo
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
