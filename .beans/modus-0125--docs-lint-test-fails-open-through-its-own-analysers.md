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

## Two hazards `bean:0123`'s wrapper carries into this work item

**A non-zero exit is a signal in `tools/lib/bash32-scan.awk`, and the wrapper cannot tell
that from a death.** Line 51 `exit 2`s deliberately, on its own designed control path — a
scan that loaded no patterns matches nothing and would report every file clean, so it
refuses to report success (`doc:00-constitution#observed-failing`). Reuse `bean:0123`'s
wrapper around `tools/bash-compat-lint.sh` unaltered and that path becomes
`an analyser exited 2 and examined nothing`, a red run on a mechanism working as designed.
`tools/docs-lint.sh` has no such case, which is why the wrapper works there unconditionally
and not because the question was settled:

```
head:     this branch's working tree
cmd:      /usr/bin/grep -n exit tools/docs-lint.sh tools/lib/*.awk
observed: tools/docs-lint.sh:12:# still changes the exit status.
          tools/docs-lint.sh:35:cd "$ROOT" || exit 2
          tools/docs-lint.sh:47:# fires, and this script printed its `OK` line at exit 0 through the failure. Destroying
          tools/docs-lint.sh:55:# guard's own words back as front-matter keys (bean:0123). The record that changes the exit
          tools/docs-lint.sh:74:    fail - "an analyser exited $awk_wrap_rc and examined nothing; its last argument was '$awk_wrap_arg'" >&2
          tools/docs-lint.sh:99:      if ($0 != "---") { emit("E", "1", "no front-matter block"); exit }
          tools/docs-lint.sh:435:    NR == 1 { if ($0 != "---") { exit } ; fm = 1; next }
          tools/docs-lint.sh:436:    fm && $0 == "---" { exit }
          tools/docs-lint.sh:443:      if (!fm) { exit }
          tools/docs-lint.sh:584:    NR == 1 { if ($0 != "---") exit; next }
          tools/docs-lint.sh:585:    $0 == "---" { exit }
          tools/docs-lint.sh:630:# claims and must say so rather than exit 0 (doc:00-constitution#observed-failing).
          tools/docs-lint.sh:699:    # program could not: awk exits 2, writes nothing, the read loop below finds nothing,
          tools/docs-lint.sh:701:    # transitions` at exit 0. The counts line calls itself the vacuity assertion; these
          tools/docs-lint.sh:715:      fail 14 "$f: the check 14 analyser exited $awk_rc and examined nothing; tools/lib/docs-lint-fence.awk and tools/lib/docs-lint-c14.awk must both be present and parse"
          tools/docs-lint.sh:750:  exit 1
          tools/lib/bash32-scan.awk:51:        exit 2
exit:     0
```

Seventeen lines, in full. Six are `exit` statements inside an awk program in
`tools/docs-lint.sh` — 99, 435, 436, 443, 584, 585 — and every one is **bare**, which in awk
is status 0. Six are comments, two are the word inside a `fail` message, two are the shell's
own (`cd "$ROOT" || exit 2` and the final `exit 1`), and the seventeenth is
`bash32-scan.awk`'s. So the one non-zero analyser exit in the tree is exactly the one this
work item would newly cover. A shared guard therefore needs a way for a caller to declare an
expected non-zero status, or `bash-compat-lint` needs to signal "no patterns loaded" some
other way; choosing between those is part of the scope above.

**The guard's message is a `fail` record, and one call site could make it several.** The
wrapper reports the analyser's **last argument**, which at twenty-two of the twenty-three
`awk` invocations in `tools/docs-lint.sh` is a filename. At exactly one — line 256, the
reference extractor, whose input arrives on a pipe — the last argument is the awk **program**
itself. It is one line today, so the record is one line. A multi-line program written there
later would make `fail` emit a multi-line record, and the gate's
`n_fail="$(grep -c . "$TMP/fails.txt")"` counts lines, so one defect would be reported as
several. Established by reading all twenty-three sites listed under `bean:0123`'s criterion 1,
not by a search for a phrasing.

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
