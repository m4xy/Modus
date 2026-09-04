---
# modus-0125
title: docs-lint's own test suites fail open through their own analysers
status: todo
type: fix
priority: medium
created_at: 2026-09-04T00:00:00Z
parent: modus-0118
blocked_by: [modus-0123]
---

# `docs-lint`'s own test suites fail open through their own analysers

`bean:0118` measured `tools/docs-lint-test.sh` against the same boundary as the gate and
found the same shape: `set -uo pipefail` and no `set -e`, `$?` occurring nowhere in the
file, and neither of its `awk` invocations status-checked. `bean:0123` added a third
script, `tools/docs-lint-gate-test.sh`, which runs the gate and reads its output through
`grep`, `head`, `diff` and `cmp` — none of them status-checked either.

The consequence is the same one level up. An analyser that dies inside a test suite
produces empty output, the assertion compares empty against expected and fails — which is
the benign case — or, where the assertion's expected value is itself derived from a
command, both sides go empty together and the assertion passes. The second shape is the
one worth closing, and it is not hypothetical: `docs-lint-test.sh`'s `perceives`,
`decides` and `sites` helpers each build the actual side from an `awk` run, and its
`check` compares two strings.

## Scope

Both test scripts, against `bean:0118`'s boundary. Whether the answer is `bean:0123`'s
shadowing wrapper a second and third time, a shared file the three scripts source, or a
per-site guard is the first question of the work and is not decided here. A shared file
is the obvious move and is not obviously right: `tools/docs-lint.sh` has no dependency on
anything under `tools/lib/` that it does not pass to `awk` by path, and adding one makes
a missing file a new way for the gate to die.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | An analyser destroyed by a syntax error in `tools/docs-lint-test.sh` makes that suite exit non-zero rather than report a pass | `test-run`, plant / observe / revert per `doc:00-constitution#observed-failing` |
| 2 | The same, for `tools/docs-lint-gate-test.sh` | `test-run`, plant / observe / revert |
| 3 | Neither suite's assertion count or mutation figures change unrecorded: every figure in `tools/docs-lint-test.sh`'s header is re-measured if an assertion is added, by the edit named beside it | `test-run`, per figure |
| 4 | Both suites are green on the unmodified tree, so the guard is observed silent as well as firing | `test-run`, `doc:50-memory-and-evidence#evidence-kinds` |
| 5 | `./gradlew qualityCheck` is green | `test-run` |

## References

`bean:0118` — the parent; its closing paragraphs are where `tools/docs-lint-test.sh` was
assessed against the boundary and left as a residual.
`bean:0123` — the wrapper this may or may not reuse, and the third script it added.
`doc:00-constitution#observed-failing`.
