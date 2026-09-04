---
# modus-0123
title: An analyser that dies must make docs-lint go red, not print OK
status: in-progress
type: fix
priority: high
created_at: 2026-09-04T00:00:00Z
parent: modus-0118
---

# An analyser that dies must make `docs-lint` go red, not print `OK`

The first child of `bean:0118`, and the half of its remedy the other children stand on.
`tools/docs-lint.sh` runs `awk` twenty-two times and inspects the exit status of one of
them. An analyser destroyed by a syntax error writes nothing, the loop that reads it finds
nothing, no `fail` fires, and the gate prints its `OK` line at exit 0 — with stdout
**byte-identical** to the clean run, so nothing a caller reads distinguishes the two.

This work item closes the analyser rows of `bean:0118`'s boundary and nothing else. The
non-analyser rows are `bean:0124`, the per-check discrimination proof is `bean:0126`, the
test scripts' own boundary is `bean:0125`, and the counts line is `bean:0127`.

## The evidence at `9daff18`, before the change

`bean:0118`'s figures were taken at `13d8c27`. `tools/docs-lint.sh` is **byte-identical**
between those two heads — `git diff --quiet 13d8c27 9daff18 -- tools/docs-lint.sh` exits 0
— so every line number and every count in that bean that is about this file still holds,
and the re-derivation below returns the same twenty-two lines.

```
head:     9daff18, working tree clean
cmd:      awk '/^[ \t]*#/ { next } { n += gsub(/(^|[^A-Za-z_.-])awk[ \t]/, "&") } END { print "awk_invocations=" n }' tools/docs-lint.sh
observed: awk_invocations=22
cmd:      awk '/^[ \t]*#/ { next } /(^|[^A-Za-z_.-])awk[ \t]/ { printf "%d ", FNR } END { print "" }' tools/docs-lint.sh
observed: 53 89 91 94 190 198 215 217 221 261 395 443 444 460 484 487 521 524 544 566 583 664
cmd:      /usr/bin/grep -n 'awk_rc\|$?' tools/docs-lint.sh
observed: 668:    awk_rc=$?
          669:    if [ "$awk_rc" -ne 0 ]; then
          670:      fail 14 "$f: the check 14 analyser exited $awk_rc and examined nothing; tools/lib/docs-lint-fence.awk and tools/lib/docs-lint-c14.awk must both be present and parse"
```

`grep` there is `/usr/bin/grep`, BSD grep 2.6.0-FreeBSD. The two counting commands are
`awk`, which behaves the same under BSD awk and CI's.

The instance reproduces. The plant is a copy of the pristine `tools/docs-lint.sh` with
line 463 rewritten, placed at `tools/.docs-lint-unguarded.sh` and run from there —
`dirname "$0"/..` resolves to the repository either way, and nothing wrote to a tracked
file, so there was no restore step to skip and no `git` operation anywhere near `.beans`
or `tools` (`bean:0102`).

```
head:     9daff18, working tree clean
plant:    a copy of tools/docs-lint.sh at tools/.docs-lint-unguarded.sh, line 463,
          `    removed = 1` replaced by `    removed = = 1`
cmd:      /bin/bash tools/.docs-lint-unguarded.sh
stdout:   docs-lint: OK — 19 documents, 111 anchors, 1619 references, 105 beans, 37 graph edges, 48 selectable, 105 bean ids, 0 introduced, 105 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr:   awk: syntax error at source line 4
           context is
          	    removed = >>>  = <<<  1
          awk: illegal statement at source line 4
          awk: illegal statement at source line 4
exit:     0
cmd:      cmp baseline.out unguarded-plant.out    # the clean run against the planted one
observed: (no output)
exit:     0
```

The `105 beans` and `1619 references` figures are of a corpus this branch adds five beans
to, so they move (`doc:50-memory-and-evidence#corpus-figures`); the load-bearing
observations are `exit: 0` beside the word `OK` and the `cmp` exit, and no count affects
either.

## What changed

**A shadowing wrapper, not a per-site guard and not a trap.** `tools/docs-lint.sh` now
defines a shell function named `awk` that calls `command awk "$@"`, records a non-zero
exit into `$TMP/fails.txt` through the existing `fail`, and returns the status unchanged.
Every one of the twenty-two call sites is covered without any of them opting in.

Three alternatives were weighed and the reasons are measurements, not preferences.

**A per-site `rc=$?` after every analyser**, generalising check 14's, does not reach most
of the sites. Of the twenty-two, most are inside `$( )` or are pipeline elements, where
there is no statement after the analyser for `$?` to be read at. `bean:0118` states the
obstacle differently — that in `x="$(awk …)"` the status "is the substitution's and is
discarded by the assignment" — and that is **not** what bash 3.2.57 does. A plain
assignment takes the substitution's status; only the `local`/`declare` form discards it,
because the status is then the builtin's:

```
head:     9daff18, working tree clean, under /bin/bash 3.2.57(1)-release
cmd:      v="$(command awk 'BEGIN { x = = 1 }' /dev/null 2>/dev/null)"; echo "rc=$?"
observed: plain assignment rc=2
cmd:      f() { local w; w="$(command awk 'BEGIN { x = = 1 }' /dev/null 2>/dev/null)"; echo "rc=$?"; }; f
observed: local-then-assign rc=2
cmd:      g() { local u="$(command awk 'BEGIN { x = = 1 }' /dev/null 2>/dev/null)"; echo "rc=$?"; }; g
observed: local u=... rc=0
cmd:      p="$(command awk 'BEGIN { x = = 1 }' /dev/null 2>/dev/null | cat)"; echo "rc=$?"
observed: assignment of a pipeline (pipefail) rc=2
```

So the real obstacle to a per-site guard is the pipeline elements and the readability of
twenty-two copies of the same four lines, not the assignments. Recorded because a reviewer
reading `bean:0118` alone would reach the wrong conclusion about which sites are reachable.

**`set -e`, or a blanket `ERR` trap**, fires on the success path. `tools/docs-lint.sh`
ends with `n_fail="$(grep -c . "$TMP/fails.txt")"`, and on a green run that file is empty:

```
head:     9daff18, working tree clean, under /bin/bash 3.2.57(1)-release
cmd:      q="$(command grep -c . /dev/null)"; echo "rc=$? q=[$q]"
observed: grep -c on empty rc=1 q=[0]
```

An `ERR` trap is still the right answer for the rows `bean:0124` covers, and closing those
needs the audit of every command in the file that this measurement is the first line of.

**One bypass exists, deliberately**: the wrapper's own `command awk`. A second one would be
a call site the guard silently stops covering, which is this defect one level up, so
`tools/docs-lint-gate-test.sh` names the bypasses on every run rather than trusting them.

## The proof runs in `qualityCheck`

`tools/docs-lint-gate-test.sh` is new, registered as `docsLintGateTest` and added to
`qualityCheck`'s dependencies. It plants the syntax error into a **copy** of the gate,
requires the copy to exit non-zero, requires the *unmutated* copy at the same path to exit
0 and write nothing to stderr, and asserts before either run that the plant landed and
changed exactly one line.

It is a separate file from `tools/docs-lint-test.sh` because the subject differs — that
file feeds fixtures to the two `awk` libraries, this one can only observe the shell script
by running the whole gate — and because that file's header requires every one of its
mutation figures to be re-measured whenever an assertion is added to it, which an assertion
about the gate has no use for.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | Every `awk` invocation in `tools/docs-lint.sh` is covered, and the covering mechanism cannot be bypassed unnoticed | see "criterion 1" below |
| 2 | An analyser destroyed by a syntax error makes the gate exit non-zero, observed for check 12 specifically | see "criterion 2" below |
| 3 | The guard is silent on the unmodified tree: `docs-lint` stays green and its stdout is unchanged | see "criterion 3" below |
| 4 | The proof is in `qualityCheck` and discriminates the fix's absence, its deletion, its neutering and its firing on every input | see "criterion 4" below |
| 5 | `./gradlew qualityCheck` is green | see "criterion 5" below |

Every capture below was taken on this branch's working tree with all five bean files
already present, so the corpus figures on the `OK` line do not move between them. Pasting
these transcripts adds no reference the file did not already carry, and no bean, so the
record is measurement-neutral from this point (`doc:50-memory-and-evidence#corpus-figures`).

### Criterion 1 · every call site is covered, and bypasses are named

The counting command from `bean:0118`, re-run. It now returns **23**: the twenty-two call
sites, unchanged, plus the wrapper's own `command awk` at line 61. Every one of the
twenty-two is a bare `awk`, so every one resolves to the shell function.

```
head:     this branch's working tree
cmd:      awk '/^[ \t]*#/ { next } { n += gsub(/(^|[^A-Za-z_.-])awk[ \t]/, "&") } END { print "awk_invocations=" n }' tools/docs-lint.sh
observed: awk_invocations=23
cmd:      awk '/^[ \t]*#/ { next } /(^|[^A-Za-z_.-])awk[ \t]/ { printf "%d ", FNR } END { print "" }' tools/docs-lint.sh
observed: 61 83 119 121 124 220 228 245 247 251 291 425 473 474 490 514 517 551 554 574 596 613 694
cmd:      grep -v '^[[:space:]]*#' tools/docs-lint.sh | grep -E '(^|[^A-Za-z_.-])(command[[:space:]]+awk|env[[:space:]]+awk|\\awk|[^[:space:]]*/awk)[[:space:]]'
observed:   command awk "$@"
```

`grep` there is the harness's `ugrep 7.8.4`. The same pipeline under `/usr/bin/grep`
(`grep (BSD grep, GNU compatible) 2.6.0-FreeBSD`) returns the same single line, which
matters because `tools/docs-lint-gate-test.sh` runs it with whatever `grep` the machine
has and CI's is neither of these two.

The bypass list is an **enumeration** of spellings — `command awk`, `env awk`, `\awk`, and
any path-qualified form — and therefore fails open on a spelling nobody has named, exactly
as `tools/lib/bash32-forbidden.tsv` does and for the reason
`doc:00-constitution#mechanical-enforcement` gives. It was checked against a fixture
carrying all four spellings plus a bare call and a comment, and returned the four and not
the other two.

### Criterion 2 · a destroyed analyser makes the gate exit non-zero

The same plant as the `9daff18` capture above — check 12's cycle analyser, one line
replaced by a syntax error, in a copy — now run against the guarded gate. Run here by hand
so the transcript could be taken; `tools/docs-lint-gate-test.sh` makes the identical plant
at a `tools/.docs-lint-probe-<pid>-mutant.sh` of its own and asserts the same exit status,
so this is a capture of what `qualityCheck` re-runs rather than a one-off:

```
head:     this branch's working tree
plant:    a copy of tools/docs-lint.sh at tools/.docs-lint-c2.sh, with
          `    removed = 1` replaced by `    removed = = 1`
cmd:      /bin/bash tools/.docs-lint-c2.sh
stdout:   docs-lint: 1 failure(s).
stderr:   awk: syntax error at source line 4
           context is
          	    removed = >>>  = <<<  1
          awk: illegal statement at source line 4
          awk: illegal statement at source line 4
          FAIL check -  an analyser exited 2 and examined nothing; its last argument was '/var/folders/mg/c8xtgk197f74w3r78q7_9sfc0000gn/T/tmp.9mUDLQqsDa/bean-edges.uniq'
exit:     1
```

### Criterion 3 · the guard is silent on the unmodified tree

The control is the guarded gate against the **unguarded** gate on the **same** tree, not
against the capture taken at `9daff18`: the corpus has five more beans, so a comparison
across the two trees would fail for a reason that has nothing to do with the guard.

```
head:     this branch's working tree
cmd:      /bin/bash tools/docs-lint.sh
observed: docs-lint: OK — 19 documents, 111 anchors, 1654 references, 110 beans, 42 graph edges, 47 selectable, 110 bean ids, 5 introduced, 105 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0
cmd:      wc -c < v2-guarded.err      # everything the guarded run wrote to stderr
observed:        0
cmd:      cmp v2-noguard.out v2-guarded.out   # the wrapper deleted, against the wrapper present
observed: (no output)
exit:     0
```

Twenty-two call sites, and the guard fired zero times. The over-firing mutation below puts
a figure on how many invocations those twenty-two sites make over one run: **2134**, a
figure of this corpus and one that moves with it
(`doc:50-memory-and-evidence#corpus-figures`).

### Criterion 4 · the proof discriminates its own absence, deletion, neutering and over-firing

Four mutations of the fix, each run against `tools/docs-lint-gate-test.sh` unchanged, each
made in a copy and restored from a pristine copy taken beforehand with no `git` operation
involved (`bean:0102`). The suite is 11 assertions and green on the fix as shipped.

| mutation | edit | result |
|---|---|---|
| the fix is absent / deleted | the wrapper and its comment removed | 7 passed, 4 failed |
| the guard is neutered | `-ne 0` → `-lt 0`, so it never records | 8 passed, 3 failed |
| the guard fires on every input | `-ne 0` → `-ge 0` | 6 passed, 5 failed |
| one call site bypasses the guard | the front-matter parser's `awk` → `command awk` | 10 passed, 1 failed |

The four are not redundant. Deletion and neutering are told apart by the bypass assertion,
which fails on the first and passes on the second — a structural check catching what the
single behavioural probe cannot. Over-firing is caught only by the negative-control half,
which is `doc:50-memory-and-evidence#evidence-kinds`'s point exactly: without the green run
beside the red one, a guard that reports every analyser as dead scores identically to the
fix. And the bypass mutation is caught only by the structural check, because it moves a
call site the mutated run never reaches — which is the honest statement of this file's
limit, and is why `bean:0126` exists.

The four failure transcripts, elided to the lines that differ from the green run pasted
under criterion 5; every `ok` line and the header are identical in all four and are marked
`[same]`:

```
head:     this branch's working tree
mutation: the wrapper and its comment deleted (lines 43-68)
observed: [same]
          FAIL a destroyed analyser makes the gate exit non-zero
                 expected: rc=1
                 actual:   rc=0
          FAIL and the gate says it failed rather than printing OK
                 expected: docs-lint: 1 failure(s).
                 actual:   docs-lint: OK — 19 documents, 111 anchors, 1654 references, 110 beans, 42 graph edges, 47 selectable, 110 bean ids, 5 introduced, 105 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          FAIL and attributes it to an analyser that examined nothing
                 expected: 1
                 actual:   0
          [same]
          FAIL the guard's own call is the only site that bypasses it
                 expected:   command awk "$@"
                 actual:   
          [same]
          docs-lint-gate-test: 7 passed, 4 failed.
exit:     1

mutation: `-lt 0`, the guard present and never recording
observed: [same]
          FAIL a destroyed analyser makes the gate exit non-zero
                 expected: rc=1
                 actual:   rc=0
          FAIL and the gate says it failed rather than printing OK
                 expected: docs-lint: 1 failure(s).
                 actual:   docs-lint: OK — 19 documents, 111 anchors, 1654 references, 110 beans, 42 graph edges, 47 selectable, 110 bean ids, 5 introduced, 105 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          FAIL and attributes it to an analyser that examined nothing
                 expected: 1
                 actual:   0
          [same]
          docs-lint-gate-test: 8 passed, 3 failed.
exit:     1

mutation: `-ge 0`, the guard recording every invocation
observed: [same]
          FAIL and the gate says it failed rather than printing OK
                 expected: docs-lint: 1 failure(s).
                 actual:   docs-lint: 2134 failure(s).
          FAIL and attributes it to an analyser that examined nothing
                 expected: 1
                 actual:   2134
               (this awk exited 0 on the planted syntax error)
          FAIL the negative control: the same copy unmutated exits 0
                 expected: rc=0
                 actual:   rc=1
          FAIL and prints the OK line
                 expected: docs-lint: OK
                 actual:   docs-lint: 21
          FAIL and writes nothing at all to stderr
                 expected: 0
                 actual:   2134
          [same]
          --- the mutated run's stderr: 2139 line(s), at most 20 shown
               FAIL check -  an analyser exited 0 and examined nothing; its last argument was 'documentation/00-constitution.md'
               [...] eighteen more of the same shape
               FAIL check -  an analyser exited 0 and examined nothing; its last argument was '/var/folders/mg/c8xtgk197f74w3r78q7_9sfc0000gn/T/tmp.0TzmnUUVld/fm.tsv'
          [same]
          docs-lint-gate-test: 6 passed, 5 failed.
exit:     1

mutation: the front-matter parser's `awk` rewritten as `command awk`
observed: [same]
          FAIL the guard's own call is the only site that bypasses it
                 expected:   command awk "$@"
                 actual:     command awk "$@"
            command awk -v file="$f" '
          [same]
          docs-lint-gate-test: 10 passed, 1 failed.
exit:     1
```

`docs-lint: 21` in the third block is not a truncation of this record: the assertion
compares the first thirteen characters of the first stdout line, and the failing run's is
`docs-lint: 2134 failure(s).`

### Criterion 5 · `./gradlew qualityCheck`

```
head:     this branch's working tree
cmd:      ./gradlew qualityCheck
observed: bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 4 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [...]
          docs-lint-test: 51 passed, 0 failed.
          [...]
          docs-lint: OK — 19 documents, 111 anchors, 1654 references, 110 beans, 42 graph edges, 47 selectable, 110 bean ids, 5 introduced, 105 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          [...]
          docs-lint-gate-test: 11 passed, 0 failed.
          [...]
          BUILD SUCCESSFUL in 37s
          161 actionable tasks: 7 executed, 154 up-to-date
exit:     0
```

`docs-lint-test: 51 passed, 0 failed` is the same figure as at `9daff18`: no assertion was
added to that file, so none of its mutation figures is restated by this change. The
`4 scripts parsed` on the `bash-compat` line was 3 before — `tools/docs-lint-gate-test.sh`
is covered by that glob the day it lands.

The whole green run, in full:

```
head:     this branch's working tree
cmd:      /bin/bash tools/docs-lint-gate-test.sh
observed: docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)

          --- the plant: check 12's acyclicity analyser, destroyed
          ok   the mutation site occurs exactly once in the gate
          ok   the copy differs from the gate on exactly one line (one '<', one '>')
          ok   and the line it differs on is the planted syntax error
          ok   the control copy is identical to the gate

          --- the runs: both halves, over the whole corpus
          ok   a destroyed analyser makes the gate exit non-zero
          ok   and the gate says it failed rather than printing OK
          ok   and attributes it to an analyser that examined nothing
               (this awk exited 2 on the planted syntax error)
          ok   the negative control: the same copy unmutated exits 0
          ok   and prints the OK line
          ok   and writes nothing at all to stderr

          --- the mutated run's stderr: 6 line(s), at most 20 shown
               awk: syntax error at source line 4
                context is
               	    removed = >>>  = <<<  1
               awk: illegal statement at source line 4
               awk: illegal statement at source line 4
               FAIL check -  an analyser exited 2 and examined nothing; its last argument was '/var/folders/mg/c8xtgk197f74w3r78q7_9sfc0000gn/T/tmp.e7k149j7Lj/bean-edges.uniq'

          --- the guard covers every call site, because no call site opts in
          ok   the guard's own call is the only site that bypasses it

          docs-lint-gate-test: 11 passed, 0 failed.
exit:     0
```

### The CI observation, and what it changed

`bean:0118` recorded "could not verify: the boundary on the CI image" and marked it the
figure most likely to be wrong. The first CI run of this branch is that measurement, and it
found something: on the runner the gate went red **correctly** and one assertion in this
file failed, because the assertion had the analyser's exit **status** written into it and
that status is the interpreter's, not the gate's.

```
head:     7d3892c, GitHub Actions run 33905404724, job 101129080076, ubuntu-latest
cmd:      ./gradlew qualityCheck --stacktrace -x backofficeTypecheck -x backofficeLint -x backofficeFormatCheck
observed: docs-lint-gate-test: interpreter /bin/bash (bash 5.2.21(1)-release)
          [...]
          ok   a destroyed analyser makes the gate exit non-zero
          ok   and the gate says it failed rather than printing OK
          FAIL and attributes it to an analyser that examined nothing
                 expected: 1
                 actual:   0
          ok   the negative control: the same copy unmutated exits 0
          ok   and prints the OK line
          ok   and writes nothing at all to stderr
          [...]
          docs-lint-gate-test: 10 passed, 1 failed.
exit:     1 (Execution failed for task ':docsLintGateTest')
```

The load-bearing half held on the runner: `a destroyed analyser makes the gate exit
non-zero` and `the gate says it failed rather than printing OK` both passed under bash
5.2.21, so the fix works there and the defect is closed there. What did not hold was
`exited 2`, which was written from the BSD awk macOS ships. The assertion now requires the
attribution — one failure, named as an analyser that examined nothing — and **reports** the
status beside it instead of fixing it, because a number that differs per image is a
measurement and not a requirement. The mutated run's stderr is now printed in full (capped
at twenty lines), so the next difference of this kind is visible in the log rather than
only in an assertion's `actual:`.

The CI run of the fix prints the number, and it is **1**. The runner's `awk` is a mawk —
its diagnostic is `awk: cmd. line:4:` rather than BSD awk's `awk: syntax error at source
line 4` — and it exits 1 where the BSD awk this was written against exits 2:

```
head:     41ad94c, GitHub Actions run 33906282727, job 101131893027, ubuntu-latest
cmd:      ./gradlew qualityCheck --stacktrace -x backofficeTypecheck -x backofficeLint -x backofficeFormatCheck
observed: docs-lint-gate-test: interpreter /bin/bash (bash 5.2.21(1)-release)
          [...]
          ok   a destroyed analyser makes the gate exit non-zero
          ok   and the gate says it failed rather than printing OK
          ok   and attributes it to an analyser that examined nothing
               (this awk exited 1 on the planted syntax error)
          ok   the negative control: the same copy unmutated exits 0
          ok   and prints the OK line
          ok   and writes nothing at all to stderr

          --- the mutated run's stderr: 3 line(s), at most 20 shown
               awk: cmd. line:4:     removed = = 1
               awk: cmd. line:4:               ^ syntax error
               FAIL check -  an analyser exited 1 and examined nothing; its last argument was '/tmp/tmp.iACxzUYSvB/bean-edges.uniq'

          --- the guard covers every call site, because no call site opts in
          ok   the guard's own call is the only site that bypasses it

          docs-lint-gate-test: 11 passed, 0 failed.
exit:     0, and the `gate` job passed
```

The guard itself is not written against a status — it tests `-ne 0` — so it works under
both, and the failed run was a defect in the test rather than in the fix.

What this does not establish is that the two `awk`s agree on a program that **parses**.
That question is not untouched: `bean:0049`'s amendment "The anchored regexes under CI's
awk, which is not the awk they were written against" took exactly that measurement for
`tools/lib/bash32-scan.awk`'s patterns, on the runner, and found its planted samples still
caught exactly once. That is a measurement of one analyser's regexes, not of the analysers
in `tools/docs-lint.sh`, and this work item adds nothing to it. Named so the next reader
does not mistake a difference in exit status for a difference in parsing.

## Not verified here

**The rest of `bean:0118`'s boundary on the CI image.** The plant above is now observed on
the runner, under bash 5.2.21 — one row of that table, and the row this work item is about.
The other twelve rows have still never been run there. `bean:0124`.

**That every one of the twenty-two analysers reaches the guard.** One is observed doing so.
The other twenty-one rest on the guard's shape and on the bypass assertion, neither of which
is a run. `bean:0126`.

## References

`bean:0118` — the parent, and the measurement this closes the first part of.
`bean:0124`, `bean:0125`, `bean:0126`, `bean:0127` — the rest of the split.
`bean:0102` — a plant reverted with `git checkout` discards uncommitted work; nothing here
writes to a tracked file at all.
`doc:00-constitution#observed-failing` — and `doc:50-memory-and-evidence#evidence-kinds`
for the negative half, which is why the control run is here beside the planted one.
`doc:50-memory-and-evidence#corpus-figures` — why the counts on the `OK` line move.
