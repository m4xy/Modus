---
# modus-0126
title: No docs-lint check is proved to discriminate, per check, on every run
status: todo
type: fix
priority: high
created_at: 2026-09-04T00:00:00Z
parent: modus-0118
blocked_by: [modus-0123, modus-0125]
---

# No `docs-lint` check is proved to discriminate, per check, on every run

`bean:0123` catches an analyser that **dies**. It says nothing about an analyser that
runs, exits 0 and matches nothing — which is how check 11 shipped inert through four
plants (`bean:0051`), and is the failure `doc:50-memory-and-evidence#evidence-kinds`
describes as a mechanism observed firing and never observed silent.

The mechanism that closes this already exists in this repository and is simply not
pointed at `docs-lint`. `tools/bash-compat-lint.sh` plants each rule's own sample
violation on **every invocation**, asserts the scan finds it exactly once, asserts the
finding is attributed to that rule, and asserts a fixture of legal near-misses produces
nothing. The range is written with the command that finds it rather than as a line range,
because a bare range does not survive the next edit to the file it points into:
`/usr/bin/grep -n 'n_planted=0' tools/bash-compat-lint.sh` names its start.

Every analyser in `docs-lint` reads a named file, so every one of them can be fed a
fixture the same way. Which of the checks are worth a fixture and which are better served
by `bean:0123`'s failure path alone is the first task of this work item, not a finding of
`bean:0118`.

## The cost is the design problem

One full gate run over the corpus takes on the order of fifteen seconds, and
`bean:0123` already spends two of them per `qualityCheck`. A plant-and-run per check is
not affordable at that shape, so this work item is a harness design as much as a proof:
a reduced corpus, a per-check entry point, or a single run that plants into every check
at once and reads the failure list. The last is the cheapest and the weakest — one run
cannot tell which plant produced which failure unless each plant's message is distinct,
which is exactly the attribution assertion `tools/bash-compat-lint.sh` makes.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | Each check's analyser, neutered in turn, makes the gate go red, and the failure is attributed to that check | `test-run`, per check |
| 2 | The unmodified tree is green, so the mechanism is observed both firing and silent | `test-run`, both halves, `doc:50-memory-and-evidence#evidence-kinds` |
| 3 | The count of checks covered is derived from `doc:05-authoring-for-agents#checks`, not written down a second time, so a check added there without a fixture here fails the gate | `command` |
| 4 | The harness runs inside `qualityCheck`, and its added wall time is measured and stated | `test-run`, timed |
| 5 | The harness is observed producing a red run and a green run on the same tree, so it cannot close by having stopped planting | `test-run`, both halves |
| 6 | `./gradlew qualityCheck` is green | `test-run` |

## References

`bean:0118` — the parent; its remedy's second half is this work item.
`bean:0123` — the failure path, which catches a dead analyser and not an inert one. The
two do not subsume each other.
`bean:0051` — check 11 inert in CI through four plants.
`bean:0035` — check 12's six rejections and its negative control, the shape this
generalises.
`doc:50-memory-and-evidence#evidence-kinds`, `doc:00-constitution#observed-failing`.
