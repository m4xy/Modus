---
# modus-0086
title: Check 6 decides which references are live through the same naive fence toggle bean:0063 removed from check 14
status: todo
type: fix
priority: high
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0063]
---

# Check 6 decides which references are live through the same naive fence toggle bean:0063 removed from check 14

`tools/docs-lint.sh:205` reads every `documentation/*.md`, every ADR, every bean, `AGENTS.md`,
`CLAUDE.md` and the pull-request template through

```
awk '/^```/ { fence = !fence; next } !fence { print }' "$f"
```

and resolves the typed `doc:` / `bean:` / `adr:` / `rule:` references in what survives. That is
the toggle `bean:0063` replaced in check 14, unchanged, in the check that decides whether a
pointer resolves. Every line matching a line-initial run of three backticks flips it; a marker
that is **content** contributes an odd flip, and every line after it is read with its
inside/outside sense reversed.

The consequence is the same class and a wider blast radius. Check 14 governs one bean at the
moment it closes. Check 6 governs every typed reference in the whole document set, which is
what `doc:05-authoring-for-agents#one-fact-one-place` relies on to make a reference safer than
a copy.

## Observed

Planted on `.beans/modus-0033`, a `status: todo` bean, by appending the shape and reverting
with `git checkout -- .beans`. Every run is `bash tools/docs-lint.sh`, filtered to check 6 and
the summary line. Each pair is a plant and its own negative control, differing only in the
stray marker.

### Fails OPEN — an unresolvable reference is not seen

```
control:  one line of prose carrying `doc:99-not-a-document`
observed: FAIL check 6  .beans/modus-0033--baseline-writer-erases-regression-provenance.md:
          'doc:99-not-a-document' resolves to 0 files, expected exactly 1
          docs-lint: 1 failure(s).
exit:     1

planted:  the identical line, with a transcript above it quoting one fence marker
observed: docs-lint: OK — 19 documents, 106 anchors, 921 references, 65 beans, 29 graph
          edges, 19 selectable, 65 bean ids, 1 introduced, 64 on origin/main, 0 closing
          transitions, 0 criteria checked, 0 unnumbered.
exit:     0
```

The reference count on the `OK` line is the vacuity assertion `doc:00-constitution#observed-failing`
asks for, and it does not fire: 921 in both runs, because the plant hides one reference and
the count is a total rather than a per-file figure.

### Fails CLOSED — a fenced template's reference is resolved

Fenced blocks are skipped deliberately: they hold the literal templates an author copies, and
`bean:NNNN` in a template is not a reference. An odd marker earlier in the file makes those
templates live.

```
control:  a balanced fenced block holding `doc:98-also-not-a-document`
observed: docs-lint: OK — … 921 references, …
exit:     0

planted:  the identical block, with one quoted fence marker above it
observed: FAIL check 6  .beans/modus-0033--baseline-writer-erases-regression-provenance.md:
          'doc:98-also-not-a-document' resolves to 0 files, expected exactly 1
          docs-lint: 1 failure(s).
exit:     1
```

The author is told a reference does not resolve when the only occurrence of it in the file is
inside a code block that the check is documented as skipping.

## Which checks track fences, and what that means for each

| check | fence handling | consequence |
|---|---|---|
| 6 references resolve | the naive toggle, `tools/docs-lint.sh:205` | this bean |
| 14 a bean closes without evidence | a CommonMark state machine, `tools/lib/docs-lint-fence.awk` (`bean:0063`) | fixed |
| 10 no bare bean paths | **none** — a `grep -noE` over whole files | a `beans/NNNN` path inside a fenced block is reported as prose. Within the file set it reads — `documentation/*.md`, `AGENTS.md`, `CLAUDE.md` — it can spuriously flag an example but cannot miss a real one. That file set is narrower than check 6's: `.beans/*.md` is outside it, and bare `beans/0…` paths do sit there unseen. `doc:05-authoring-for-agents#checks` defines that scope, so it is a spec choice and not a defect of this kind. No file in scope trips it today |
| 11 completed beans are final | **none** — it diffs bytes and greps for `^## Amendments` and `**Claimed:**` | correct by construction. Immutability is a property of the byte sequence, and a fence has no bearing on it. A fenced block quoting `## Amendments` inside a completed bean's appended text would be miscounted against the amendment headings, which is a narrower and separate question |
| 12, 13 bean graph and ids | **none** — front-matter and filenames only | front-matter is above the first fence by construction |

So check 6 is the only remaining consumer of fenced blocks that decides anything, and check 10
is the only other place a fence is relevant at all.

## Options

| option | catches | cost |
|---|---|---|
| use `tools/lib/docs-lint-fence.awk`, which `bean:0063` already added, and refuse a file whose block never closes | both directions above — the fails-OPEN one via the refusal, not via the reference reappearing | the fence file exists and is tested, and the parser swap is measured below as a zero-diff drop-in. The work is the refusal path and its message |
| leave check 6 and disclose the parity on the `OK` line | nothing, but stops it being silent | rejected in `bean:0063` for check 14 for the same reason |
| make the `OK` line's reference count per-file rather than a total | the fails-OPEN direction, as a diff a reviewer must read | a count nobody diffs is not a check |

## Blast radius, measured: there is none

**Every claim in the first version of this section was wrong, and it was wrong in the
direction that does harm.** It asserted, in bold and without measuring, that the
`references` figure would move, that new check 6 failures would appear in `documentation/`,
that references would stop resolving, and that amendments to frozen beans should be
expected. It then told the next agent to measure — after four paragraphs telling them what
they would find.

Measured. PR #46's `tools/lib/docs-lint-fence.awk`, the file criterion 4 mandates, was run
over check 6's exact `REF_FILES` set with check 6's exact `REF_RE`, against today's naive
toggle, and the two reference sets diffed per file:

```
cmd:      check 6's REF_RE over check 6's REF_FILES, once through the naive toggle and
          once through PR #46's fence-aware parser, diffed per file
observed: naive (check 6 today) unique refs: 921
          PR46-fence-aware unique refs:      921
          --- APPEAR only when fence-aware ---   (empty)
          --- DISAPPEAR when fence-aware ---     (empty)
          --- files whose fences never close --- (empty)
exit:     0
```

**It is a zero-diff drop-in.** No file changes, the counter does not move, no new failure
appears, nothing stops resolving. The corpus is fence-balanced and none of its fences nest,
which is exactly the condition under which the two parsers agree.

The instrument was validated in both directions before the zero was believed, because a
diffing script that matches nothing and a corpus with nothing to match print the same
result:

```
cmd:      the same measurement, with two planted shapes that separate the parsers — a
          line-initial inline code span (which check 6's /^```/ toggles on and the
          fence-aware parser does not open, because a backtick fence's info string may not
          contain a backtick), and a three-column-indented fence (which check 6's /^```/
          never sees, because it allows no leading whitespace, and the fence-aware parser
          opens)
observed: naive (check 6 today) unique refs: 922
          PR46-fence-aware unique refs:      922
          --- APPEAR only when fence-aware ---
          .beans/modus-0033-…	doc:97-appears
          --- DISAPPEAR when fence-aware ---
          .beans/modus-0033-…	doc:96-disappears
exit:     0
```

So the zero is a measurement, not a broken script.

### The deadlock this section warned about could not have existed

`adr:0005-evidence-lives-in-the-work-item#finalisation` makes a bean's evidence the durable
record of what was required and **what was observed**, and `#amendments` is for what the
original claimed against what was found to be true. A point-in-time counter that has since
moved is not a claim found untrue; it is an observation that remains true of the run that
produced it. No amendment is owed for one, ever.

And the premise was false as well as the reasoning. No `completed` bean quotes 921. The
`N references` figures the corpus does quote are already stale by up to several hundred, and
nothing re-verifies a quoted counter against a current run. The proposed remedy also
contradicts the sibling bean on evidence-cell strength, which argues that amendments written
to satisfy a check are themselves a shape worth refusing. That bean has it right.

### The general defect, because this one is not about check 6

**A blast-radius estimate is a claim, and `doc:00-constitution#observed-failing` binds it
like any other.** Stated as certainty without measurement it does a specific harm: a future
agent reads "expect to write amendments" and defers a zero-diff change indefinitely. An
overstated cost is not the safe direction to be wrong in. It is exactly as false as an
understated one and it has the worse failure mode, because an understated cost is discovered
the moment someone acts on it, and an overstated one is discovered by nobody, because it
stops anyone acting at all.

### One thing the measurement changed about the fix itself

The fails-OPEN plant in `## Observed` is **not** closed by making check 6 fence-aware. Run
against the fence-aware parser, the escaped reference stays hidden: the stray marker opens a
block that never closes, so the reference is inside it under both parsers, for different
reasons. What closes the plant is the **refusal** — the parser reports the unterminated
block and the check fails the file, naming it. Criterion 1 is satisfied by that route and
not by the reference becoming visible, and an implementation that adds fence-awareness
without the refusal will observe the plant still passing.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The fails-OPEN plant above is observed rejected, not merely no longer accepted | planted violation, reverted |
| 2 | The fails-CLOSED plant above is observed passing, or refused with a message naming the unterminated fence | planted violation, reverted |
| 3 | The reference set check 6 resolves is diffed per file, before and after, and every file that changes is named with the reason | analyser run over the corpus, before and after |
| 4 | Check 6 shares `tools/lib/docs-lint-fence.awk` with check 14 rather than carrying a second implementation | diff |
| 5 | What check 6 perceives is asserted in `tools/docs-lint-test.sh` separately from what it decides | test-run |
| 6 | `./gradlew qualityCheck` green | test-run |

## Not in scope

- Check 14. `bean:0063` fixed it and its evidence is there.
- Check 10's lack of fence tracking. Recorded in the table above; it fails closed, and a
  spuriously flagged example is a different and much smaller problem.
- The strength of what an evidence cell contains, the numbering gate (`bean:0061`) and the
  evidence-kind matcher (`bean:0056`). This bean is about where a fence is, in one more place.
- Widening `REF_RE`. Which strings are references is settled in
  `doc:05-authoring-for-agents#reference-syntax`.
