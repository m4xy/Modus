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
`bean:0123` already spends two of them per `qualityCheck` — two runs, not twice the time,
because they run concurrently: `bean:0123` measured `docsLintGateTest` at 21s against
`docsLint`'s 18s and both together at 22s, and on the CI runner the gate test's whole span
overlaps `docsLint`'s. So what a per-check plant-and-run would spend is not the seconds this
one does; it is a run per check where this is one run for all of them, and that is
not affordable, so this work item is a harness design as much as a proof:
a reduced corpus, a per-check entry point, or a single run that plants into every check
at once and reads the failure list. The last is the cheapest and the weakest — one run
cannot tell which plant produced which failure unless each plant's message is distinct,
which is exactly the attribution assertion `tools/bash-compat-lint.sh` makes.

## The second thing this closes: the bypass assertion `bean:0123` could not make fail closed

`bean:0123` shipped its shadow guard with a `qualityCheck` assertion that the wrapper's own
`command awk` is the only call site bypassing it. That assertion is a **lexical
enumeration** of bypass spellings, which `doc:00-constitution#mechanical-enforcement` says
fails open, and `bean:0123`'s criterion 1 records it doing so: nine spellings the regex does
not match, seven of which reach the binary, and two mutations that leave twenty-one of the
twenty-two call sites unguarded while the suite reports 11 passed, 0 failed. `bean:0123`
carries that as a named enforcement gap pointing here.

**A candidate design, recorded and not chosen.** The wrapper already knows how many times it
ran — `bean:0123` measured **2140** invocations over one run of a clean corpus, a figure of
this corpus and one that moves with it (`doc:50-memory-and-evidence#corpus-figures`). Print
that count on the `OK` line and have the gate-test assert it is non-zero and equal between
the mutant and the control except for the plant, and the assertion becomes **behavioural
rather than lexical**: any bypass, however spelled, lowers the count, and a spelling nobody
has named is caught by the same assertion as one that has. That is the fail-closed shape
`doc:00-constitution#observed-failing` asks for, in place of an allowlist.

Two things it collides with, both to be settled here rather than assumed. The `OK` line is
`bean:0127`'s subject and it is blocked on this work item, so a figure added to that line
belongs to whichever of the two lands second. And a count is a corpus figure: it moves with
every document and bean added, so the assertion has to be "equal between two runs of the same
tree", never a literal.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | Each check's analyser, neutered in turn, makes the gate go red, and the failure is attributed to that check | `test-run`, per check |
| 2 | The unmodified tree is green, so the mechanism is observed both firing and silent | `test-run`, both halves, `doc:50-memory-and-evidence#evidence-kinds` |
| 3 | The count of checks covered is derived from `doc:05-authoring-for-agents#checks`, not written down a second time, so a check added there without a fixture here fails the gate | `command` |
| 4 | The harness runs inside `qualityCheck`, and its added wall time is measured and stated | `test-run`, timed |
| 5 | The harness is observed producing a red run and a green run on the same tree, so it cannot close by having stopped planting | `test-run`, both halves |
| 6 | A bypass of `bean:0123`'s `awk` shadow makes `qualityCheck` go red whatever the spelling, demonstrated on at least the two mutations that walk past the lexical assertion today — `unset -f awk` at one call site, and the guard narrowed to one site of twenty-two | `test-run`, both mutations |
| 7 | `./gradlew qualityCheck` is green | `test-run` |

## References

`bean:0118` — the parent; its remedy's second half is this work item.
`bean:0123` — the failure path, which catches a dead analyser and not an inert one. The
two do not subsume each other.
`bean:0051` — check 11 inert in CI through four plants.
`bean:0035` — check 12's six rejections and its negative control, the shape this
generalises.
`doc:50-memory-and-evidence#evidence-kinds`, `doc:00-constitution#observed-failing`.
