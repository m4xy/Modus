---
# modus-0124
title: docs-lint still fails open on every runtime error that is not an analyser
status: todo
type: fix
priority: high
created_at: 2026-09-04T00:00:00Z
parent: modus-0118
blocked_by: [modus-0123]
---

# `docs-lint` still fails open on every runtime error that is not an analyser

`bean:0123` closes the analyser rows of `bean:0118`'s measured boundary and nothing else.
The rest of that table is untouched: a `false`, a missing file, a failed `cd`, a failed
pipeline element and an unbound variable expanded inside `$( )` or inside a pipeline
element all still reach the `OK` line at exit 0. Three of them — `false`,
`/usr/bin/false` and `false | cat` — reach it having written nothing at all to stderr, so
there is no diagnostic a caller could notice even if it were reading stderr.

`set -u` remains the only fail-closed mechanism and remains fail-closed only in the
top-level shell. `bean:0123` did not add `set -e`, and the reason is measured rather than
assumed: `tools/docs-lint.sh` uses commands whose non-zero exit is the normal case on a
green run, `n_fail="$(grep -c . "$TMP/fails.txt")"` among them, where `grep -c` prints `0`
and exits 1 on an empty file. A blanket `set -e`, or a blanket `ERR` trap, therefore fires
on the success path and would have to be preceded by an audit of every command in the
file. That audit is this work item.

## Scope

`bean:0118`'s boundary table, re-run at the head this closes, with each row either made
non-zero or recorded with a stated reason why it may not be. `doc:00-constitution#observed-failing`'s
negative half applies with unusual force here: a mechanism that fires on `grep -c` finding
nothing is worse than the gap, because it makes the gate red on a clean tree and the fix
will be to remove it.

The probe harness is itself the thing that can close vacuously — a harness that stopped
planting would report every row passing — so it closes on being observed producing a red
run and a green one on the same tree, not on a list of exit statuses.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | Each non-analyser row of `bean:0118`'s boundary table makes the gate exit non-zero, or is recorded here with a stated reason why it may not | `test-run`, the probe harness re-run, each row's exit status pasted from the capture |
| 2 | The probe harness is observed producing both a red run and a green run on the same tree | `test-run`, both halves |
| 3 | `tools/docs-lint.sh` is green on the unmodified tree, with stdout byte-identical to the run before this change | `command`, `cmp` against a capture taken before |
| 4 | `./gradlew qualityCheck` is green | `test-run` |

## References

`bean:0118` — the parent, and the measured boundary table this closes.
`bean:0123` — the analyser rows, closed first because they are the ones `bean:0118`
observed producing a byte-identical `OK`.
`doc:00-constitution#observed-failing` — and `doc:50-memory-and-evidence#evidence-kinds`
for the half that says firing on every input is also firing.
