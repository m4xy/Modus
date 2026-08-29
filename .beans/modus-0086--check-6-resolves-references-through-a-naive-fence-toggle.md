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
| 10 no bare bean paths | **none** — a `grep -noE` over whole files | a `beans/NNNN` path inside a fenced block is reported as prose. Fails CLOSED only: it can spuriously flag an example, never miss a real one. No corpus file trips it today |
| 11 completed beans are final | **none** — it diffs bytes and greps for `^## Amendments` and `**Claimed:**` | correct by construction. Immutability is a property of the byte sequence, and a fence has no bearing on it. A fenced block quoting `## Amendments` inside a completed bean's appended text would be miscounted against the amendment headings, which is a narrower and separate question |
| 12, 13 bean graph and ids | **none** — front-matter and filenames only | front-matter is above the first fence by construction |

So check 6 is the only remaining consumer of fenced blocks that decides anything, and check 10
is the only other place a fence is relevant at all.

## Options

| option | catches | cost |
|---|---|---|
| use `tools/lib/docs-lint-fence.awk`, which `bean:0063` already added, and refuse a file whose block never closes | both directions above, as an explicit error naming the file and the line | the fence file exists and is tested; the work is the refusal path and the blast radius below |
| leave check 6 and disclose the parity on the `OK` line | nothing, but stops it being silent | rejected in `bean:0063` for check 14 for the same reason |
| make the `OK` line's reference count per-file rather than a total | the fails-OPEN direction, as a diff a reviewer must read | a count nobody diffs is not a check |

## Blast radius — read this before starting

A correct fix **changes which lines check 6 reads across the entire document set**, and it is
not a refactor:

- **The `references` figure on the `OK` line will move.** It is 921 on this branch. It is asserted
  verbatim in the evidence of beans that are already `completed`, which `docs-lint` check 11
  makes append-only: those transcripts cannot be corrected in place, only amended
  (`adr:0005-evidence-lives-in-the-work-item#amendments`). Expect to write amendments, or to
  argue that a historical transcript records what was observed then and is not made false by a
  later change.
- **New check 6 failures may appear in `documentation/`.** Any file whose fence markers are
  currently mis-parsed has references that are being skipped and will start resolving. Each is
  a real finding and each needs a fix in a tree that other agents hold.
- **References inside fenced blocks stop resolving where they currently do.** The reverse
  direction: any file that is currently reading a fenced template as live prose loses those
  references from the count, and any of them that were resolving by accident are now invisible.
- Measure all three before changing anything: run the current check 6 and a fence-aware check 6
  over the corpus, and diff the reference sets per file. That diff **is** the evidence, and any
  file in it is a finding.

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
