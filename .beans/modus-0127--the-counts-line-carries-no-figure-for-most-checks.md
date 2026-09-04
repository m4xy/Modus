---
# modus-0127
title: The counts line calls itself a vacuity assertion but covers only some checks
status: todo
type: fix
priority: medium
created_at: 2026-09-04T00:00:00Z
parent: modus-0118
blocked_by: [modus-0126]
---

# The counts line calls itself a vacuity assertion but covers only some checks

`tools/docs-lint.sh`'s comment above its `OK` line calls the twelve counts the gate's
vacuity assertion — "a check that silently examined nothing reports zero here, where
check 11 shipping inert went unnoticed for four plants". `bean:0118` measured what the
twelve figures are actually derived from and found no figure derived from the cycle
check: the `graph edges` figure comes from `$TMP/bean-edges.uniq`, which `sort -u` writes
one line before the analyser runs, so an entirely inert check 12 leaves every figure on
the line unchanged. The numeral is deliberately not written here. It is a figure of a
corpus this repository is still growing — `bean:0118` read 37 and the same field read 42
at `9fe411c` — and what this work item is about is which check each field is derived from,
which does not move (`doc:50-memory-and-evidence#corpus-figures`).

That is the general case, not check 12's peculiarity. The line carries figures for
documents, anchors, references, beans, edges, selectable beans, bean ids, introduced ids,
ids on `origin/main`, closing transitions, criteria and unnumbered criteria. Checks 4, 5,
6, 8, 9, 10 and 11 have no figure of their own on it, and a reader who takes the comment
at its word believes otherwise.

## Scope

Either every check that can be inert carries a figure derived from its own analyser's
output, or the checks the line cannot cover are named in the comment. Naming them is the
weaker answer and is acceptable, because `bean:0126` covers the same ground with a
stronger mechanism; what is not acceptable is a comment claiming a coverage the line does
not have.

Blocked on `bean:0126` deliberately. The question "which checks can be inert" is answered
by neutering each one and watching, and that is `bean:0126`'s harness. Writing the figures
first would mean guessing the answer and then building the thing that could have told us.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The counts line carries a figure derived from every check that can be inert, or the checks it cannot cover are named in the comment above it | `citation` at the head sha |
| 2 | For each new figure, the check's analyser neutered makes that figure move, observed | `test-run`, per figure, reusing `bean:0126`'s harness |
| 3 | The unmodified tree still prints every figure non-zero where it was non-zero before, so the line has not been widened into noise | `command`, the `OK` line before and after |
| 4 | `./gradlew qualityCheck` is green | `test-run` |

## References

`bean:0118` — the parent; the measurement that the `graph edges` figure predates the cycle
analyser is there, stamped at the head it was taken on.
`bean:0126` — the harness this uses, and the reason this is blocked on it.
`bean:0051` — check 11's inert CI runs differed from real ones by exactly one character,
`- introduced` rather than `0 introduced`, which is the whole argument for the line.
`doc:00-constitution#observed-failing`.
