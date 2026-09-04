---
# modus-0123
title: An analyser that dies must make docs-lint go red, not print OK
status: completed
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
`tools/docs-lint-gate-test.sh` names it on every run rather than trusting it. That assertion
catches a second `command awk` and nothing else worth relying on: it is a lexical
enumeration, nine spellings walk past it, and two mutations that leave the guard covering
one call site out of twenty-two score a clean sheet against the whole suite. Criterion 1
carries both measurements, and `bean:0126` carries the fail-closed replacement.

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
| 1 | Every `awk` invocation in `tools/docs-lint.sh` is covered, and the covering mechanism cannot be bypassed unnoticed | **met in part** — see "criterion 1" below |
| 2 | An analyser destroyed by a syntax error makes the gate exit non-zero, observed for check 12 specifically | see "criterion 2" below |
| 3 | The guard is silent on the unmodified tree: `docs-lint` stays green and its stdout is unchanged | see "criterion 3" below |
| 4 | The proof is in `qualityCheck` and discriminates the fix's absence, its deletion, its neutering and its firing on every input | see "criterion 4" below |
| 5 | `./gradlew qualityCheck` is green | see "criterion 5" below |

Every capture below was taken on this branch's working tree with all five bean files
already present, so the corpus figures on the `OK` line do not move between them. The
figures were re-taken once, in full, when the review corrections to this bean and to
`bean:0125`, `bean:0126` and `bean:0127` added references the corpus did not carry:
`1654 references` became `1659` and the guard's invocation count `2134` became `2140`, both
in every transcript here. From this point the record is measurement-neutral — the sentences
added since add no reference to a file that did not already carry it, and no bean
(`doc:50-memory-and-evidence#corpus-figures`).

**Every `OK` line quoted here is this branch's, and CI's `pull_request` run will not match
it.** The two CI runs of head `fb29cc6` print different counts from the same commit: the
`push` run 33917588234 prints `1659 references, 47 selectable`, which is this branch's tree
and the figure in every transcript below, and the `pull_request` run 33917590984 prints
`1669 references, 48 selectable`, because that event checks out the branch merged into
`main` and `main` has moved. Named so the next reader does not read the difference as a
defect in a count (`doc:50-memory-and-evidence#corpus-figures`).

### Criterion 1 · every call site is covered; bypasses are named, not bounded

**Met in part, and the half that is not met is named here rather than reworded.** "Every
`awk` invocation is covered" holds: twenty-two bare calls, every one resolving to the shell
function, seventeen of them observed taking the guard's fail branch. "The covering mechanism
cannot be bypassed unnoticed" does **not** hold, and the measurements below are what says so:
the assertion that was supposed to carry it is a lexical enumeration, nine spellings walk
past it, and two mutations that leave twenty-one of the twenty-two call sites unguarded score
a clean sheet against the whole suite.

**Enforcement gap:** a second bypass of the `awk` shadow is not caught by anything in
`qualityCheck`. `bean:0126` closes it, with a behavioural assertion in place of the lexical
one (`doc:00-constitution#observed-failing`).

The counting command from `bean:0118`, re-run. It now returns **23**: the twenty-two call
sites, unchanged, plus the wrapper's own `command awk`, first in the list below. Every one
of the twenty-two is a bare `awk`, so every one resolves to the shell function.

```
head:     this branch's working tree
cmd:      awk '/^[ \t]*#/ { next } { n += gsub(/(^|[^A-Za-z_.-])awk[ \t]/, "&") } END { print "awk_invocations=" n }' tools/docs-lint.sh
observed: awk_invocations=23
cmd:      awk '/^[ \t]*#/ { next } /(^|[^A-Za-z_.-])awk[ \t]/ { printf "%d ", FNR } END { print "" }' tools/docs-lint.sh
observed: 70 92 128 130 133 229 237 254 256 260 300 434 482 483 499 523 526 560 563 583 605 622 709
cmd:      grep -v '^[[:space:]]*#' tools/docs-lint.sh | grep -E '(^|[^A-Za-z_.-])(command[[:space:]]+awk|env[[:space:]]+awk|[^[:space:]]*/awk)[[:space:]]'
observed:   command awk "$@"
exit:     0
```

`grep` there is `/usr/bin/grep`, `grep (BSD grep, GNU compatible) 2.6.0-FreeBSD`; the
`awk` is `/usr/bin/awk`, `awk version 20200816`. The harness's interactive `grep` is a
shell function running `ugrep 7.8.4` and returns the same single line, which matters
because `tools/docs-lint-gate-test.sh` runs the pipeline with whatever `grep` the machine
has and CI's is a third one.

**The bypass assertion bounds nothing, and this criterion no longer claims it does.** It is
an enumeration of spellings, which `doc:00-constitution#observed-failing` says at MUST
strength fails open. Measured against a fixture of one call per line, and separately against
a live shell function named `awk`, at this working tree:

The regex under test is the one **as it stood at `9fe411c`**, `\awk` included, because half
of what this measures is that that alternative was wrong to be there. Half (a) runs each
fixture line through the assertion's own pipeline; half (b) runs the spelling for real,
against `awk() { echo "SHADOW"; }`, and prints what answered.

```
head:     9fe411c's regex, this branch's working tree, under /bin/bash 3.2.57(1)-release
cmd:      /bin/bash spellings.sh, the fixture one call per line
observed: === (a) grep: grep (BSD grep, GNU compatible) 2.6.0-FreeBSD
          fails open    awk -v x=1 'BEGIN{}' /dev/null
          CAUGHT        command awk -v x=1 'BEGIN{}' /dev/null
          CAUGHT        env awk -v x=1 'BEGIN{}' /dev/null
          CAUGHT        /usr/bin/awk -v x=1 'BEGIN{}' /dev/null
          CAUGHT        \awk -v x=1 'BEGIN{}' /dev/null
          fails open    exec awk -v x=1 'BEGIN{}' /dev/null
          fails open    command -p awk -v x=1 'BEGIN{}' /dev/null
          fails open    printf '%s\n' /dev/null | xargs awk 'BEGIN{}'
          fails open    (unset -f awk; awk -v x=1 'BEGIN{}' /dev/null)
          fails open    find . -name x -exec awk 'BEGIN{}' {} +
          fails open    $AWK -v x=1 'BEGIN{}' /dev/null
          fails open    gawk -v x=1 'BEGIN{}' /dev/null
          fails open    nawk -v x=1 'BEGIN{}' /dev/null
          fails open    builtin :; awk -v x=1 'BEGIN{}' /dev/null

          === (b) does the spelling reach the binary, past a function named awk?
          awk            -> SHADOW
          command awk    -> BINARY
          env awk        -> BINARY
          /usr/bin/awk   -> BINARY
          \awk           -> SHADOW
          exec awk       -> BINARY
          command -p awk -> BINARY
          xargs awk      -> BINARY
          unset -f awk   -> BINARY
          find -exec awk -> BINARY
          $AWK           -> BINARY
          builtin :; awk -> SHADOW
exit:     0
```

The bare `awk` at the top of (a) is the control: the regex must not match it, and does not.
Nine spellings past that one are unmatched. Seven of the nine reach the binary;
`builtin :; awk` does **not** — it still runs the function, so it is a hole in the regex and
not a bypass of the guard — and `$AWK` reaches it only when it expands to something other
than `awk`, which is how it was run here, with `AWK=/usr/bin/awk`. `gawk` and `nawk` are
absent from (b) because they are different names, which is precisely why a function named
`awk` cannot shadow them.

**`\awk` has been removed from the regex.** It was matched, as (a) shows — but (b) shows it
is not a bypass: backslash-quoting suppresses **alias** expansion, not function lookup, so
`\awk` runs the shadow function like any bare call. Matching it inflated the apparent reach
of a list that fails open on nine spellings, so it is gone rather than relabelled. Nothing
in `tools/docs-lint.sh` spells `awk` that way, so removing the alternative changes no
assertion result on this tree.

**And the suite cannot tell twenty-two covered sites from one.** Two mutations of the guard,
each run against `tools/docs-lint-gate-test.sh` unaltered, both scored a clean sheet, where
the author's own `command awk` at a second call site scores 10 passed, 1 failed:

```
head:     this branch's working tree
method:   the mutant is a copy at tools/.docs-lint-<tag>.sh and the harness is a copy of
          tools/docs-lint-gate-test.sh with its one `GATE=` line repointed at it, so no
          tracked file is written and there is no restore step to skip (bean:0102). The
          unmutated copy through the same harness scores 11 passed, 0 failed.
cmd:      diff tools/docs-lint.sh tools/.docs-lint-m1.sh
observed: 133c133
          < awk -F'\t' '$2 == "E" { print $1 "\t" $3 "\t" $4 }' "$TMP/fm.tsv" > "$TMP/parse.tsv"
          ---
          > (unset -f awk; awk -F'\t' '$2 == "E" { print $1 "\t" $3 "\t" $4 }' "$TMP/fm.tsv") > "$TMP/parse.tsv"
observed: docs-lint-gate-test: 11 passed, 0 failed.
exit:     0
cmd:      diff tools/docs-lint.sh tools/.docs-lint-m2.sh
observed: 71a72
          >   case "$*" in *bean-edges*) : ;; *) return "$awk_wrap_rc" ;; esac
observed: docs-lint-gate-test: 11 passed, 0 failed.
exit:     0
cmd:      diff tools/docs-lint.sh tools/.docs-lint-m3.sh
observed: 92c92
          <   awk -v file="$f" '
          ---
          >   command awk -v file="$f" '
observed: FAIL the guard's own call is the only site that bypasses it
          docs-lint-gate-test: 10 passed, 1 failed.
exit:     1
```

The three sites are 133, 92 and the wrapper itself, all in the listing at the top of this
criterion. The second mutation is the sharp one: it narrows the guard to the single call
site the plant reaches, leaving twenty-one of the twenty-two unguarded, and the suite reports
a clean sheet. `bean:0126` carries the fail-closed replacement.

**Seventeen of the twenty-two sites are observed reaching the guard**, which is more than
this bean first claimed and less than all of them. The wrapper's `-ne 0` was forced to
`-ge 0` in a copy, one line for one line so the numbering still holds, with
`${BASH_LINENO[0]}` recorded on every firing:

```
head:     this branch's working tree
cmd:      diff tools/docs-lint.sh tools/.docs-lint-sites.sh
observed: 72c72
          <   if [ "$awk_wrap_rc" -ne 0 ]; then
          ---
          >   if [ "$awk_wrap_rc" -ge 0 ]; then echo "${BASH_LINENO[0]}" >> [...]/sites.txt
cmd:      /bin/bash tools/.docs-lint-sites.sh, then sort -n sites.txt | uniq -c
observed: --- forced run STDOUT, in full:
          docs-lint: 2140 failure(s).
          --- forced run stdout line count: 1
          --- guard firings recorded: 2140
          --- call sites observed taking the fail branch (count, line):
            19 92
           760 128
           152 130
             1 133
            19 237
           132 254
           132 256
             9 264
           608 300
           110 434
            42 482
            42 483
             1 517
             1 523
             1 560
           110 587
             1 605
          --- distinct sites: 17
exit:     1
```

The per-site counts and the 2140 total are figures of this corpus and move with it
(`doc:50-memory-and-evidence#corpus-figures`); the seventeen line numbers and the five
absentees do not.

`${BASH_LINENO[0]}` reports the **last** line of a multi-line command, so 264, 517 and 587
are the sites listed above as 260, 499 and 583; the other fourteen are listed unchanged. The
five never reached are **229, 526, 563, 622 and 709**, and none of them is reached because
each sits inside a `while read` loop over a list that is empty on a clean corpus — duplicate
anchors, duplicate `order` values, duplicate bean ids, ids introduced twice — except 709,
which is check 14's analyser and needs a closing transition, of which this branch has none.

**And the guard's fail branch corrupts none of the seventeen.** The forced run's stdout is
one line. Every one of the 2140 records went to stderr, so no `$( )` capture took the
guard's words into its value and no check reported a spurious failure. Removing the `>&2` is
the control, and it is not subtle:

```
head:     this branch's working tree
cmd:      diff tools/docs-lint.sh tools/.docs-lint-corrupt.sh
observed: 72c72
          <   if [ "$awk_wrap_rc" -ne 0 ]; then
          ---
          >   if [ "$awk_wrap_rc" -ge 0 ]; then
          74c74
          <     fail - "an analyser exited $awk_wrap_rc and examined nothing; its last argument was '$awk_wrap_arg'" >&2
          ---
          >     fail - "an analyser exited $awk_wrap_rc and examined nothing; its last argument was '$awk_wrap_arg'"
cmd:      /bin/bash tools/.docs-lint-corrupt.sh
observed: --- stdout lines: 908
          --- stdout FAIL lines: 907
          --- first 6 stdout lines:
          FAIL check    FAIL check -  an analyser exited 0 and examined nothing; its last argument was '/var/folders/mg/c8xtgk197f74w3r78q7_9sfc0000gn/T/tmp.TmQyb1yhE9/fm.tsv': 
          FAIL check 2  documentation/00-constitution.md: unknown key 'FAIL'
          FAIL check 2  documentation/00-constitution.md: unknown key 'check'
          FAIL check 2  documentation/00-constitution.md: unknown key '-'
          FAIL check 2  documentation/00-constitution.md: unknown key 'an'
          FAIL check 2  documentation/00-constitution.md: unknown key 'analyser'
          --- last line:
          docs-lint: 3047 failure(s).
exit:     1
```

The first stdout line is the shape of the damage: check 1's front-matter analyser had the
guard's own sentence appended to its output, and check 2 then read the words of that
sentence back as front-matter keys, one `unknown key` per word per document. Both edits in
that mutant are matched by text rather than by line number, so the two `72`s and `74`s are
the file's, not the script's.

**The redirect has a cost, and it is not fixed here.** The guard is the only `fail` in the
file that does not reach stdout, so `./gradlew docsLint | tee log` leaves
`docs-lint: 1 failure(s).` in the log with no reason beside it. Gradle captures both
streams, so CI's log is complete and the `gate` job is unaffected. Replaying the guard's
records to stdout after the count line would close it and keep `$( )` safe — it is declined
here, not overlooked, because a change to what the gate prints that no assertion covers is
the shape this whole work item exists to close, and adding the assertion moves all four
mutation figures under criterion 4 and both transcripts that quote the `OK` line.

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

**At one site, and only one, a dead analyser makes two records.** Check 14 already had a
per-site `awk_rc` guard before this change, so when its analyser dies the shadow guard and
that check both fire and the run says `docs-lint: 2 failure(s).` for one defect. Observed —
the mutation points one of check 14's two `-f` files at a path that does not exist, in a
copy, and an untracked probe bean supplies the one closing transition the check needs (this
branch has none of its own). The same tree with the analyser alive is green, so both records
are the dead analyser's:

```
head:     this branch's working tree, plus one untracked probe bean, deleted on exit
cmd:      diff tools/docs-lint.sh tools/.docs-lint-c14.sh
observed: 711c711
          <       -f "$ROOT/tools/lib/docs-lint-c14.awk" \
          ---
          >       -f "$ROOT/tools/lib/docs-lint-c14-ABSENT.awk" \
cmd:      /bin/bash tools/docs-lint.sh            # the analyser alive, probe bean present
observed: docs-lint: OK — 19 documents, 111 anchors, 1659 references, 111 beans, 42 graph edges, 47 selectable, 111 bean ids, 6 introduced, 105 on origin/main, 1 closing transitions, 1 criteria checked, 0 unnumbered.
exit:     0
cmd:      /bin/bash tools/.docs-lint-c14.sh       # the same tree, the analyser dead
observed: docs-lint: 2 failure(s).
          stdout records mentioning the probe bean:
          FAIL check 14 .beans/modus-0199--a-throwaway-closing-candidate.md: the check 14 analyser exited 2 and examined nothing; tools/lib/docs-lint-fence.awk and tools/lib/docs-lint-c14.awk must both be present and parse
          stderr, in full:
          awk: can't open file [...]/tools/lib/docs-lint-c14-ABSENT.awk
           source line number 99 source file [...]/tools/lib/docs-lint-c14-ABSENT.awk
           context is
          	} >>> 
           <<< awk: can't open file [...]/tools/lib/docs-lint-c14-ABSENT.awk
           source line number 99 source file [...]/tools/lib/docs-lint-c14-ABSENT.awk
          FAIL check -  an analyser exited 2 and examined nothing; its last argument was '.beans/modus-0199--a-throwaway-closing-candidate.md'
exit:     1
```

Each `[...]` in that stderr elides the same absolute path to this worktree and nothing else.

The gate is red and both messages are true, so this is presentation. **The per-site `fail 14`
stays.** The two records are not duplicates: the guard's names the bean the analyser was
reading and cannot name the two files that must be present and parse, and check 14's names
those two files and cannot name the bean. Dropping either loses half the diagnosis of the
exact failure the comment above that call site was written for. What was wrong was the
reading, not the code — `docs-lint: N failure(s).` counts **records**, not defects, at every
check in the file, and one malformed document has always been able to produce several. The
line the `diff` rewrites, 711, is one of the two `-f` arguments; the call site criterion 1
lists is the 709 two lines above it.

### Criterion 3 · the guard is silent on the unmodified tree

The control is the guarded gate against the **unguarded** gate on the **same** tree, not
against the capture taken at `9daff18`: the corpus has five more beans, so a comparison
across the two trees would fail for a reason that has nothing to do with the guard.

```
head:     this branch's working tree
cmd:      /bin/bash tools/docs-lint.sh
observed: docs-lint: OK — 19 documents, 111 anchors, 1659 references, 110 beans, 42 graph edges, 47 selectable, 110 bean ids, 5 introduced, 105 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0
cmd:      wc -c < v2-guarded.err      # everything the guarded run wrote to stderr
observed:        0
cmd:      cmp v2-noguard.out v2-guarded.out   # the wrapper deleted, against the wrapper present
observed: (no output)
exit:     0
```

Twenty-two call sites, and the guard fired zero times. The over-firing mutation below puts
a figure on how many invocations those twenty-two sites make over one run: **2140**, a
figure of this corpus and one that moves with it
(`doc:50-memory-and-evidence#corpus-figures`).

### Criterion 4 · the proof discriminates its own absence, deletion, neutering and over-firing

Four mutations of the fix, each run against `tools/docs-lint-gate-test.sh` unchanged. Each
mutant is a copy at `tools/.docs-lint-<tag>.sh` and each is run by a copy of the gate test
with its one `GATE=` line repointed at that copy, so nothing tracked is written, there is no
restore step to skip, and no `git` operation runs near `.beans` or `tools` (`bean:0102`). The
unmutated copy through the same harness scores 11 passed, 0 failed, which is what says the
copying is not what any of the four rows below is measuring. The suite is 11 assertions and
green on the fix as shipped.

| mutation | edit | result |
|---|---|---|
| the fix is absent / deleted | the wrapper and its comment removed | 7 passed, 4 failed |
| the guard is neutered | `-ne 0` → `-lt 0`, so it never records | 8 passed, 3 failed |
| the guard fires on every input | `-ne 0` → `-ge 0` | 6 passed, 5 failed |
| one call site bypasses the guard | the front-matter parser's `awk` → `command awk` | 10 passed, 1 failed |

The four are not redundant. Deletion and neutering are told apart by the bypass assertion,
which fails on the first and passes on the second — a structural check catching what the
single behavioural probe cannot. And the bypass mutation is caught only by the structural
check, because it moves a call site the mutated run never reaches — which is the honest
statement of this file's limit, and is why `bean:0126` exists.

**The negative control is not what catches over-firing**, and an earlier revision of this
paragraph said it was. Read the over-firing transcript below: five assertions fail, and two
of them are positive-half rows — `expected: docs-lint: 1 failure(s).` against
`actual: docs-lint: 2140 failure(s).`, and `expected: 1` against `actual: 2140`. Against
these four mutations the three negative-control rows are therefore **strictly redundant**:
no mutation here is caught by them alone. What is true is the converse — over-firing is the
only one of the four the control catches at all — and the control stays for the reason
`doc:50-memory-and-evidence#evidence-kinds` gives rather than because a mutation forces it:
a mechanism observed firing and never observed silent is not discrimination, and no mutation
in a set of four can supply that.

The four failure transcripts. Each is elided to what differs from the green run pasted under
criterion 5. `[same]` marks a run of lines byte-identical to that green run, which is quoted
in full there (`doc:50-memory-and-evidence#capturing`); `[...]` marks an elision that is
**not** identical, and says what it drops. The `[same]` opening each block always covers at
least the two-line header, the plant section's banner and its four `ok` rows, and the
`--- the runs` banner; in the fourth it runs on to the last banner, since that mutation
changes only one line of the output:

```
head:     this branch's working tree
mutation: the wrapper and its comment deleted, lines 44-77, 34 lines removed
observed: [same]
          FAIL a destroyed analyser makes the gate exit non-zero
                 expected: rc=1
                 actual:   rc=0
          FAIL and the gate says it failed rather than printing OK
                 expected: docs-lint: 1 failure(s).
                 actual:   docs-lint: OK — 19 documents, 111 anchors, 1659 references, 110 beans, 42 graph edges, 47 selectable, 110 bean ids, 5 introduced, 105 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          FAIL and attributes it to an analyser that examined nothing
                 expected: 1
                 actual:   0
               (this awk exited  on the planted syntax error)
          ok   the negative control: the same copy unmutated exits 0
          ok   and prints the OK line
          ok   and writes nothing at all to stderr

          --- the mutated run's stderr: 5 line(s), at most 20 shown
          [...] awk's own five-line diagnostic, the same five the green run shows, WITHOUT
                the sixth line the green run has — the guard's `FAIL check -` record

          --- the guard covers every call site, because no call site opts in
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
                 actual:   docs-lint: OK — 19 documents, 111 anchors, 1659 references, 110 beans, 42 graph edges, 47 selectable, 110 bean ids, 5 introduced, 105 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          FAIL and attributes it to an analyser that examined nothing
                 expected: 1
                 actual:   0
               (this awk exited  on the planted syntax error)
          ok   the negative control: the same copy unmutated exits 0
          ok   and prints the OK line
          ok   and writes nothing at all to stderr

          --- the mutated run's stderr: 5 line(s), at most 20 shown
          [...] the same five lines as the deleted run above

          --- the guard covers every call site, because no call site opts in
          ok   the guard's own call is the only site that bypasses it

          docs-lint-gate-test: 8 passed, 3 failed.
exit:     1

mutation: `-ne 0` → `-ge 0`, the guard recording every invocation
observed: [same]
          ok   a destroyed analyser makes the gate exit non-zero
          FAIL and the gate says it failed rather than printing OK
                 expected: docs-lint: 1 failure(s).
                 actual:   docs-lint: 2140 failure(s).
          FAIL and attributes it to an analyser that examined nothing
                 expected: 1
                 actual:   2140
               (this awk exited 0 on the planted syntax error)
          FAIL the negative control: the same copy unmutated exits 0
                 expected: rc=0
                 actual:   rc=1
          FAIL and prints the OK line
                 expected: docs-lint: OK
                 actual:   docs-lint: 21
          FAIL and writes nothing at all to stderr
                 expected: 0
                 actual:   2140

          --- the mutated run's stderr: 2145 line(s), at most 20 shown
               FAIL check -  an analyser exited 0 and examined nothing; its last argument was 'documentation/00-constitution.md'
          [...] eighteen more records of exactly that shape, one per document, in the
                order docs-lint reads them
               FAIL check -  an analyser exited 0 and examined nothing; its last argument was '/var/folders/mg/c8xtgk197f74w3r78q7_9sfc0000gn/T/tmp.qA29dq4owg/fm.tsv'

          --- the guard covers every call site, because no call site opts in
          ok   the guard's own call is the only site that bypasses it

          docs-lint-gate-test: 6 passed, 5 failed.
exit:     1

mutation: the front-matter parser's `awk`, line 92, rewritten as `command awk`
observed: [same]
          FAIL the guard's own call is the only site that bypasses it
                 expected:   command awk "$@"
                 actual:     command awk "$@"
            command awk -v file="$f" '
          [same]
          docs-lint-gate-test: 10 passed, 1 failed.
exit:     1
```

Two lines in there are not truncations of this record. `docs-lint: 21` in the third block is
what the assertion prints: it compares the first thirteen characters of the first stdout
line, and the failing run's is `docs-lint: 2140 failure(s).`. And
`(this awk exited  on the planted syntax error)` in the first two blocks has an empty status
because those runs write no guard record for the reporting `sed` to take one from — that
line reports and is not asserted on, which is the point of `bean:0123`'s CI fix.

`2140` and `2145` are figures of this corpus and move with it
(`doc:50-memory-and-evidence#corpus-figures`); the four pass/fail pairs do not.

### Criterion 5 · `./gradlew qualityCheck`

```
head:     this branch's working tree
cmd:      ./gradlew qualityCheck
observed: bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 4 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [...]
          docs-lint-test: 51 passed, 0 failed.
          [...]
          docs-lint: OK — 19 documents, 111 anchors, 1659 references, 110 beans, 42 graph edges, 47 selectable, 110 bean ids, 5 introduced, 105 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          [...]
          docs-lint-gate-test: 11 passed, 0 failed.
          [...]
          BUILD SUCCESSFUL in 27s
          161 actionable tasks: 7 executed, 154 up-to-date
exit:     0
```

Each `[...]` there elides Gradle's own task lines and the output of the tasks between the
four this criterion is about; the five quoted lines are lines 347, 348, 410, 459 and 481 of
the 487-line capture, in that order, and the last two are its final two lines. The elapsed
time is one sample of a task graph that is mostly cached — the same command on the same tree
measured 44s a run earlier — and the figure that is not a sample is under "what the second
gate run costs" below.

`docs-lint-test: 51 passed, 0 failed` is the same figure as at `9daff18`: no assertion was
added to that file, so none of its mutation figures is restated by this change. The
`4 scripts parsed` on the `bash-compat` line was 3 before — `tools/docs-lint-gate-test.sh`
is covered by that glob the day it lands.

**What the second gate run costs.** Both `Exec` tasks declare no inputs, so neither is ever
up to date and every figure here is a real run. `org.gradle.parallel=true` and the
configuration cache overlap them, and the gate test's own two gate runs are backgrounded
against each other, so two full runs of the gate cost about one:

```
head:     this branch's working tree
cmd:      ./gradlew <shape>, each shape twice, wall clock around the whole invocation
observed: pass 1  rc=0  docsLint                   19.00 s
          pass 1  rc=0  docsLintGateTest           21.21 s
          pass 1  rc=0  docsLint docsLintGateTest  21.72 s
          pass 2  rc=0  docsLint                   17.67 s
          pass 2  rc=0  docsLintGateTest           20.93 s
          pass 2  rc=0  docsLint docsLintGateTest  21.72 s
cmd:      /bin/bash tools/docs-lint.sh and /bin/bash tools/docs-lint-gate-test.sh, timed
observed: rc=0  tools/docs-lint.sh               16.77 s
          rc=0  tools/docs-lint-gate-test.sh     19.24 s
```

So `docsLintGateTest` adds **2.7 to 4.1 seconds** to a build that already runs `docsLint`,
against the ~17 seconds a single gate run takes, and the two full gate runs inside it cost
2.5 seconds more than one. That is the figure to use: it is wall clock around the whole
invocation, measured twice per shape.

On the runner it is smaller, and the evidence there is weaker in kind — Gradle's log
timestamps are when it flushed a task's output, not when the task ran. What they do show, in
both CI runs of this branch, is that `:docsLint` finishes *inside* `:docsLintGateTest`'s
span, so the gate test is not a serial second gate run:

```
head:     9fe411c, run 33906992159 / 464a3b0, run 33917147569, both ubuntu-latest
cmd:      the `> Task :` lines and the docs-lint OK line of each job log, with timestamps
observed: 33906992159  18:39:52.5743941Z  > Task :docsLintGateTest   (first line)
          33906992159  18:40:02.1414422Z  > Task :docsLint           (and its OK line)
          33906992159  18:40:02.4423572Z  docs-lint-gate-test: 11 passed, 0 failed.
          33917147569  20:38:18.5883650Z  > Task :docsLintGateTest   (first line)
          33917147569  20:38:36.1353088Z  > Task :docsLint
          33917147569  20:38:37.0348120Z  docs-lint: OK — [...] the counts line
          33917147569  20:38:37.0360738Z  docs-lint-gate-test: 11 passed, 0 failed.
```

The gate test's last line lands 0.30 s after `:docsLint`'s output in the first run and
0.0013 s after it in the second. Neither number is a task duration and neither should be
quoted as one; what they bound is the serial tail, and it is well under a second.

The whole green run, in full:

```
head:     this branch's working tree
cmd:      /bin/bash tools/docs-lint-gate-test.sh
observed: docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)
          docs-lint-gate-test: analyser awk — awk version 20200816

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
               FAIL check -  an analyser exited 2 and examined nothing; its last argument was '/var/folders/mg/c8xtgk197f74w3r78q7_9sfc0000gn/T/tmp.diN2issxTo/bean-edges.uniq'

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

The CI run of the fix prints the number, and it is **1**. The runner's `awk` is a **gawk**,
not the mawk an earlier revision of this bean named. Both the diagnostic and the status say
so, and they were compared against real builds of all three rather than recalled:

```
head:     9fe411c, working tree clean
plant:    the check 12 program of tools/docs-lint.sh, lines 490-507 at this head, with
          `    removed = 1` replaced by `    removed = = 1`, passed as an ARGUMENT (which
          is what makes gawk say `cmd. line` rather than a filename), over /dev/null
cmd:      gawk -F'\t' "$P" /dev/null
observed: === /opt/homebrew/bin/gawk   [GNU Awk 5.4.1, API 4.1, PMA Avon 8-g1, (GNU MPFR 4.2.2, GNU MP 6.3.0)]
          gawk: cmd. line:4:     removed = = 1
          gawk: cmd. line:4:               ^ syntax error
          exit=1
cmd:      mawk -F'\t' "$P" /dev/null
observed: === /opt/homebrew/bin/mawk   [mawk 1.3.4 20260302]
          mawk: line 4: syntax error at or near =
          exit=2
cmd:      /usr/bin/awk -F'\t' "$P" /dev/null
observed: === /usr/bin/awk   [awk version 20200816]
          /usr/bin/awk: syntax error at source line 4
           context is
          	    removed = >>>  = <<<  1
          /usr/bin/awk: illegal statement at source line 4
          /usr/bin/awk: illegal statement at source line 4
          exit=2
```

Two lines with a caret, and exit **1**, is gawk and only gawk of the three: mawk prints one
line, names the offending token, and exits 2. The prefix on each line is the name the binary
was invoked by, which on the runner is `awk` — Ubuntu's `update-alternatives` points `awk`
at gawk when both are installed. The runner's own two lines, at the head this bean closes:

```
head:     9fe411c, GitHub Actions run 33906992159, job 101134169366, ubuntu-latest
cmd:      ./gradlew qualityCheck --stacktrace -x backofficeTypecheck -x backofficeLint -x backofficeFormatCheck
observed: docs-lint-gate-test: interpreter /bin/bash (bash 5.2.21(1)-release)

          --- the plant: check 12's acyclicity analyser, destroyed
          [...] the plant's four `ok` rows, identical to the local run under criterion 5

          --- the runs: both halves, over the whole corpus
          [...] Gradle's output for the other tasks of the run, interleaved by the
                parallel executor, and `> Task :docsLint` with its OK line
          > Task :docsLintGateTest
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
               FAIL check -  an analyser exited 1 and examined nothing; its last argument was '/tmp/tmp.7DwW3g6QsQ/bean-edges.uniq'

          --- the guard covers every call site, because no call site opts in
          ok   the guard's own call is the only site that bypasses it

          docs-lint-gate-test: 11 passed, 0 failed.
exit:     0, and the `gate` job passed
```

Inference, not observation, is what the three fences above settle it by: nothing on the
runner was asked what it was. `tools/docs-lint-gate-test.sh` now prints the awk's own
version line beside the interpreter's, so the run after this one answers the question
directly instead — the fence is under "the runner names its own awk" below.

The guard itself is not written against a status — it tests `-ne 0` — so it works under
both, and the failed run was a defect in the test rather than in the fix.

What this does not establish is that the two `awk`s agree on a program that **parses**.
That question is not untouched: `bean:0049`'s amendment "The anchored regexes under CI's
awk, which is not the awk they were written against" took exactly that measurement for
`tools/lib/bash32-scan.awk`'s patterns, on the runner, and found its planted samples still
caught exactly once. That is a measurement of one analyser's regexes, not of the analysers
in `tools/docs-lint.sh`, and this work item adds nothing to it. Named so the next reader
does not mistake a difference in exit status for a difference in parsing.

### The runner names its own awk

Everything above settles the runner's `awk` by inference from a diagnostic's shape, which is
how it was got wrong the first time. `tools/docs-lint-gate-test.sh` now prints the awk's own
version line beside the interpreter's, so the first CI run after that change answers it
without inference:

```
head:     464a3b0, GitHub Actions run 33917147569, job 101166858927, ubuntu-latest
cmd:      ./gradlew qualityCheck --stacktrace -x backofficeTypecheck -x backofficeLint -x backofficeFormatCheck
observed: docs-lint-gate-test: interpreter /bin/bash (bash 5.2.21(1)-release)
          docs-lint-gate-test: analyser awk — GNU Awk 5.2.1, API 3.2, PMA Avon 8-g1, (GNU MPFR 4.2.1, GNU MP 6.3.0)
          [...] the eleven assertion rows and the two banners between them, all `ok`, and
                identical to the local green run under criterion 5 except for this stderr
               awk: cmd. line:4:     removed = = 1
               awk: cmd. line:4:               ^ syntax error
               FAIL check -  an analyser exited 1 and examined nothing; its last argument was '/tmp/tmp.5Ce3WF2ev0/bean-edges.uniq'
          docs-lint-gate-test: 11 passed, 0 failed.
exit:     0, and the `gate` job passed
```

A gawk, as the diagnostic said: 5.2.1 on the runner against the 5.4.1 the local comparison
used, agreeing with it on both the format and the status. Nothing about mawk was ever true
of this image.

## Not verified here

**The rest of `bean:0118`'s boundary on the CI image.** The plant above is now observed on
the runner, under bash 5.2.21 — one row of that table, and the row this work item is about.
The other twelve rows have still never been run there. `bean:0124`.

**That every one of the twenty-two analysers reaches the guard.** Seventeen are observed
doing so, under criterion 1, and the five that are not are named there with the reason each
is unreachable on a clean corpus. What no run here establishes is that those five reach the
guard when their loops do iterate; that rests on the guard's shape. `bean:0126`.

**That `qualityCheck` would catch a second bypass.** Its assertion is a lexical enumeration
and two mutations walk past it with a clean sheet, both under criterion 1. `bean:0126`.

## References

`bean:0118` — the parent, and the measurement this closes the first part of.
`bean:0124`, `bean:0125`, `bean:0126`, `bean:0127` — the rest of the split.
`bean:0102` — a plant reverted with `git checkout` discards uncommitted work; nothing here
writes to a tracked file at all.
`doc:00-constitution#observed-failing` — and `doc:50-memory-and-evidence#evidence-kinds`
for the negative half, which is why the control run is here beside the planted one.
`doc:50-memory-and-evidence#corpus-figures` — why the counts on the `OK` line move.

## Closing evidence — merged as PR #77, squashed onto `main` as `3b02871`

A bean cannot close itself, so this is the next change (`doc:00-constitution#bean-lifecycle`).

**The criteria are not restated below, and none is reworded.** A close that rewrites its
criteria is indistinguishable from a close that met them, so the table below indexes
`## Success criteria and evidence` by number and records a verdict against the wording
already standing there. The `status:` line is the only edit this change makes outside this
section.

**Criterion 1 closes `met in part`, which is the verdict its own section above already
records**, and its `Enforcement gap:` naming `bean:0126` stands unaltered.
`doc:80-agent-operating-procedure#self-validate` step 6 forbids weakening a criterion to reach
green, and changing one is a separate work item and a human decision. Block B re-measures both
halves of it at the closing head rather than inheriting either.

The heads in play. Every figure below is stamped with the one it was taken at, was redirected
to a file and pasted from that file, and carries `[...]` on every elision.

- **`3b02871`** — `origin/main`, this change's merge base, `.beans/` and `tools/` unmodified.
  Block A, Block B, and the first arm of Block E.
- **`3b02871`+`status:`** — the merge base with this bean's `status:` line as the only edit and
  this section absent. Block E's second arm, and the bare-flip probe below it.
- **`3b02871`+`status:`+this section, with Block C's and Block D's fences absent** — the two
  gate runs. Every other line of this section was present when they ran.
- **`870be5c`** — this change's first commit, which is the three heads above with Block C's and
  Block D's fences filled in. Block F, taken from its CI run rather than from this machine.

Block F is out of alphabetical order on purpose: it belongs beside Block D's gate and was
written after Block E, and renumbering would have edited a block whose figures were already
taken (`doc:50-memory-and-evidence#capturing`).

| # | verdict | observed |
|---|---|---|
| 1 | **met in part** | The covered half holds and is re-measured: the same seventeen call sites take the guard's fail branch at the closing head, and the five that do not are the five named above, for the reasons named above. The uncovered half does not hold — the bypass assertion is still a lexical enumeration and `bean:0126` still owns it. Block B |
| 2 | met | `docsLintGateTest` plants the syntax error into check 12's acyclicity analyser in a copy and the gate exits non-zero, saying it failed rather than printing `OK`, and attributing it to an analyser that examined nothing. Block C |
| 3 | met | The negative control in the same run: the same copy unmutated exits 0, prints the `OK` line, and writes nothing at all to stderr. Without it a guard firing on every input would score identically (`doc:50-memory-and-evidence#evidence-kinds`). Block C |
| 4 | met | The proof is a `qualityCheck` dependency and ran as one here, not by hand. Its four structural assertions — the mutation site occurs exactly once, the copy differs on exactly one line, that line is the plant, and the control copy is identical to the gate — all pass, which is what makes the plant a plant. Block C, and Block D for the task graph it ran under |
| 5 | met | `./gradlew qualityCheck` green on this closing branch, re-run rather than reused: an earlier run measured a different tree (`doc:80-agent-operating-procedure#self-validate`). PR #77's own `gate` job is `pass` on the pull request whose merge commit is `3b02871`. Block D |

### Block A — what merged, read from the commit rather than from this bean

```
head:     3b02871
cmd:      git show 3b02871 --stat --format='%h %s' > [...]/merged-stat.txt
observed: 3b02871 fix(docs-lint): make a dead analyser fail the gate (#77)

           ...orts-ok-through-almost-every-runtime-failure.md |  41 +-
           ...analyser-that-dies-must-make-the-gate-go-red.md | 914 +++++++++++++++++++++
           ...non-analyser-fail-open-boundary-in-docs-lint.md |  56 ++
           ...nt-test-fails-open-through-its-own-analysers.md | 105 +++
           ...nt-check-is-proved-to-discriminate-per-check.md |  91 ++
           ...ounts-line-carries-no-figure-for-most-checks.md |  59 ++
           build.gradle.kts                                   |  17 +-
           tools/docs-lint-gate-test.sh                       | 186 +++++
           tools/docs-lint-test.sh                            |   8 +
           tools/docs-lint.sh                                 |  45 +
           10 files changed, 1520 insertions(+), 2 deletions(-)
exit:     0

cmd:      GITHUB_TOKEN= gh pr view 77 --json number,title,mergeCommit,mergedAt,state \
            --jq '{number, title, state, mergedAt, merge: .mergeCommit.oid}'
observed: {"merge":"3b028713b4c887cd0f2647c7dc12969cf5a2c68a","mergedAt":"2026-09-04T21:07:18Z","number":77,"state":"MERGED","title":"fix(docs-lint): make a dead analyser fail the gate"}
exit:     0
```

The `--stat` is git's own abbreviation of the six bean paths and is not an elision of mine.
The three files this bean's prose claims as its subject are all in it: the wrapper's 45 lines
into `tools/docs-lint.sh`, the new `tools/docs-lint-gate-test.sh`, and the 17 lines into
`build.gradle.kts` that register `docsLintGateTest` and add it to `qualityCheck`'s
dependencies. `tools/docs-lint-test.sh` is `bean:0121`'s file and gained 8 lines here.

### Block B — criterion 1's two halves, re-measured at the closing head

The forced-guard measurement from criterion 1, re-run at `3b02871` on a corpus that has moved.
The mutant is a copy at `tools/.docs-lint-sites.sh`, one line for one line so every line number
in the gate still holds; it is deleted by the same script, and `git status --porcelain` is
printed after the deletion and is empty, so no tracked file was written and there is no restore
step to skip (`bean:0102`).

```
head:     3b02871, working tree clean
cmd:      /bin/bash [...]/sites.sh [...] > [...]/sites.out
observed: --- diff tools/docs-lint.sh tools/.docs-lint-sites.sh
          72c72
          <   if [ "$awk_wrap_rc" -ne 0 ]; then
          ---
          >   if [ "$awk_wrap_rc" -ge 0 ]; then echo "${BASH_LINENO[0]}" >> [...]/sites.txt
          --- forced run STDOUT, in full:
          forced run exit: 1
          docs-lint: 2146 failure(s).
          --- forced run stdout line count: 1
          --- guard firings recorded: 2146
          --- call sites observed taking the fail branch (count, line):
            19 92
           763 128
           152 130
             1 133
            19 237
           132 254
           132 256
             9 264
           611 300
           110 434
            42 482
            42 483
             1 517
             1 523
             1 560
           110 587
             1 605
          --- distinct sites: 17
          --- mutant removed; git status:
          --- (end)
exit:     0
```

**The seventeen sites hold; the total does not, and that is the figure's own prediction.**
Criterion 1 recorded `2140` firings on this branch's tree at PR #77; this run records
**2146** on `main` at `3b02871`, off the same seventeen line numbers with three of the
per-site counts moved — 128, 300 and the total. That is what the sentence under the original
capture says will happen: the per-site counts and the total are figures of this corpus and
move with it, and the seventeen line numbers and the five absentees do not
(`doc:50-memory-and-evidence#corpus-figures`). The `git status` line after the deletion is
empty because the mutant was the only file written and it is gone.

Nothing here re-measures the bypass half, because nothing has changed about it: the assertion
`tools/docs-lint-gate-test.sh` carries is still the lexical enumeration criterion 1 records
nine spellings walking past, and the two mutations that leave twenty-one of the twenty-two
call sites unguarded still score a clean sheet against it. That residual is `bean:0126`'s and
is why this criterion does not close met.

### Block C — criterion 2, criterion 3 and criterion 4, off the suite `main` now carries

The word is repeated in that heading on purpose. Check 14's matcher takes at most two numbers
per `criteri(on|a)` token separated by at most three non-alphanumeric characters, so a heading
reading `criteria 2, 3 and 4` would set only the first two and silently drop the third, ` and `
being letters and not a separator. Written as three tokens it says what it reads as.

```
head:     3b02871 + this bean's `status:` line + this section + `bean:0128`, with this fence
          and Block D's replaced by a one-token placeholder line
cmd:      ./gradlew qualityCheck > [...]/quality.txt
observed: > Task :docsLintGateTest
          docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)
          docs-lint-gate-test: analyser awk — awk version 20200816

          --- the plant: check 12's acyclicity analyser, destroyed
          ok   the mutation site occurs exactly once in the gate
          ok   the copy differs from the gate on exactly one line (one '<', one '>')
          ok   and the line it differs on is the planted syntax error
          ok   the control copy is identical to the gate

          --- the runs: both halves, over the whole corpus
          [...] the three backoffice tasks' output, interleaved by the parallel executor:
                `backofficeFormatCheck`, `backofficeLint` and `backofficeTypecheck`, each
                clean, and `> Task :docsLint` with the OK line quoted in Block D
          > Task :docsLintGateTest
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
               FAIL check -  an analyser exited 2 and examined nothing; its last argument was '/var/folders/[...]/T/tmp.zFznD8TKaP/bean-edges.uniq'

          --- the guard covers every call site, because no call site opts in
          ok   the guard's own call is the only site that bypasses it

          docs-lint-gate-test: 11 passed, 0 failed.
exit:     0
```

`docsLintGateTest` is the mechanism criterion 4 names and it ran here as a `qualityCheck`
dependency, not by hand. Its first four rows are the assertions that make the plant a plant —
the mutation site occurs exactly once in the gate, the copy differs from it on exactly one
line, that line is the planted syntax error, and the control copy is byte-identical to the gate
— and criterion 2 and criterion 3 are the two runs under them, the mutated one red and the
unmutated one green, silent and printing the `OK` line.

**The analyser's exit status is reported and not required, and this run is why that was the
right call.** It reads `2` here against the `1` the runner printed at `9fe411c`: this machine's
`awk` is `awk version 20200816`, the BSD awk macOS ships, and the runner's is a gawk. The
guard tests `-ne 0` and is written against neither. The version line beside the interpreter's
is what makes that readable without inference, and the CI arm of this close reads it directly.

### Block D — criterion 5, the gate on this branch

The gate is `doc:00-constitution#workflow` §7.2.4's block, run whole rather than as the three
tasks this bean's own subject would have made it tempting to run alone. It is the same run
Block C reads `docsLintGateTest` out of.

```
head:     3b02871 + this bean's `status:` line + this section + `bean:0128`, with this fence
          and Block C's replaced by a one-token placeholder line
cmd:      ./gradlew ktlintFormat > [...]/ktlint.txt
exit:     0
cmd:      ./gradlew qualityCheck > [...]/quality.txt
observed: [...] lines 1-354: the Gradle task banners for every module — the `build-logic`
                tasks, the Kotlin compilations, ktlint, Detekt, both test suites, the
                ArchUnit run, `coverageAggregateReport` and `> Task :check`
          bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 4 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [...] line 357, blank, and lines 358-417: `> Task :docsLintTest` and its 51 `ok`
                rows under their four `---` banners
          docs-lint-test: 51 passed, 0 failed.
          [...] lines 419-443: `> Task :e2eInstall` and `> Task :backofficeInstall` with npm's
                deprecation warning, package counts, funding notice and `found 0
                vulnerabilities` for each, and `> Task :architecture-tests:test` and `:check`
          [...] lines 444-491: the `docsLintGateTest` banner and its four plant rows, quoted
                whole in Block C, then the three backoffice tasks' clean output
          docs-lint: OK — 19 documents, 111 anchors, 1701 references, 111 beans, 42 graph edges, 51 selectable, 111 bean ids, 1 introduced, 110 on origin/main, 1 closing transitions, 5 criteria checked, 4 unnumbered.
          [...] lines 493-513: the six `docsLintGateTest` run rows and the mutated run's
                6-line stderr, quoted whole in Block C
          docs-lint-gate-test: 11 passed, 0 failed.
          [...] lines 515-525: a blank line, the `> Task :qualityCheck` banner, the
                incubating problems-report notice and the Gradle 10 deprecation warning
          BUILD SUCCESSFUL in 28s
          170 actionable tasks: 54 executed, 96 from cache, 20 up-to-date
          Configuration cache entry stored.
exit:     0
```

The elisions above are line ranges of the redirect, so each is checkable against it rather than
merely described — which is the failure mode this bean was reviewed for. The redirect is 528
lines; the eight quoted lines are 355, 356, 418, 492, 514, 526, 527 and 528, and the six ranges
cover 1-354, 357-417, 419-443, 444-491, 493-513 and 515-525, which is every remaining line.

**Nothing in the redirect is a failure.** `/usr/bin/grep -niE 'fail|error'` over it returns 18
lines: eleven `checkKotlinGradlePluginConfigurationErrors SKIPPED` task banners, the two
`N passed, 0 failed` summary lines, and five lines inside `docsLintGateTest`'s deliberate plant
— including the one `FAIL check -` line, which is the guard firing on the destroyed analyser and
is the thing this bean shipped.

**This `OK` line is not Block E's, and the difference is not a defect in a count.** It reads
`1701 references, 111 beans, 51 selectable, 1 introduced` where Block E's second arm reads
`1669`, `110`, `50` and `0`, because this tree also introduces `bean:0128` — the bean this
change raises. A record that measures a corpus it belongs to changes that corpus
(`doc:50-memory-and-evidence#corpus-figures`), and the closing transition and criteria counts,
which are the two this close is about, are `1` and `5` on both.

PR #77's own checks, which are criterion 5's merged half and are GitHub's record of a head that
is already `main`:

```
head:     3b02871, the merge commit of PR #77
cmd:      GITHUB_TOKEN= gh pr checks 77 > [...]/pr77-checks.txt
observed: build + mechanical gates	pass	1m17s	https://github.com/m4xy/Modus/actions/runs/33917931026/job/101169364224	
          gate	pass	4s	https://github.com/m4xy/Modus/actions/runs/33917925889/job/101169654777	
          gate	pass	2s	https://github.com/m4xy/Modus/actions/runs/33917931026/job/101169720656	
          backoffice + e2e	skipping	0	https://github.com/m4xy/Modus/actions/runs/33917931026/job/101169366504	
          build + mechanical gates	pass	1m8s	https://github.com/m4xy/Modus/actions/runs/33917925889/job/101169337504	
          backoffice + e2e	skipping	0	https://github.com/m4xy/Modus/actions/runs/33917925889/job/101169338947	
          which halves	pass	4s	https://github.com/m4xy/Modus/actions/runs/33917925889/job/101169307543	
          which halves	pass	6s	https://github.com/m4xy/Modus/actions/runs/33917931026/job/101169325433
exit:     0
```

### Block F — the runner, re-read at the closing head rather than inherited

"The runner names its own awk" above settles the image's `awk` at `464a3b0`, a head that no
longer exists on any branch. The closing change gets its own CI run, so the figure is re-read
rather than carried forward, which is what `doc:50-memory-and-evidence#primary-sources` asks of
a citation relied on again.

```
head:     870be5c, GitHub Actions run 33921251544, job 101179886255, ubuntu-latest
cmd:      GITHUB_TOKEN= gh run view 33921251544 --job 101179886255 --log \
            | /usr/bin/grep -E 'docs-lint-gate-test:|analyser awk|docs-lint: OK|an analyser exited' \
            > [...]/ci-awk.txt
observed: [...] each line below is preceded in the capture by the job name, the step name and
                an ISO-8601 timestamp, all three of GitHub's making and stripped here
          docs-lint-gate-test: interpreter /bin/bash (bash 5.2.21(1)-release)
          docs-lint-gate-test: analyser awk — GNU Awk 5.2.1, API 3.2, PMA Avon 8-g1, (GNU MPFR 4.2.1, GNU MP 6.3.0)
          docs-lint: OK — 19 documents, 111 anchors, 1702 references, 111 beans, 42 graph edges, 51 selectable, 111 bean ids, 1 introduced, 110 on origin/main, 1 closing transitions, 5 criteria checked, 4 unnumbered.
               FAIL check -  an analyser exited 1 and examined nothing; its last argument was '/tmp/tmp.2Akq34hO7q/bean-edges.uniq'
          docs-lint-gate-test: 11 passed, 0 failed.
exit:     0
```

**A gawk, again, and a different one: 5.2.1 here against the 5.2.1 at `464a3b0` and the 5.4.1
the local three-way comparison used.** Nothing about mawk was ever true of this image, and
this run says so directly rather than by inference from a diagnostic's shape.

The two figures that differ from every local capture in this bean are the interpreter and the
status. `/bin/bash` is 5.2.21 on the runner against 3.2.57 here, and the destroyed analyser
exits **1** there against **2** here, because this machine's `awk` is `awk version 20200816`.
The guard tests `-ne 0` and both runs take its fail branch, which is the whole reason the
assertion reports the status instead of requiring it.

The closing-transition counters are the runner's too: `1 closing transitions, 5 criteria
checked`, on the tree this change proposes, so check 14 examined this bean where it is claimed
to run and not only where that was convenient (`doc:00-constitution#observed-failing`).

### Block E — the counters moved, and `bean:0124` and `bean:0125` became selectable

`main` at `3b02871` reports `0 closing transitions, 0 criteria checked`, which is what a tree
with no bean closing in it prints and also what a run that examined nothing prints
(`bean:0096`). Both counters move here, which is the statement that check 14 examined this
bean rather than passing over it.

```
head:     3b02871, working tree clean
cmd:      /bin/bash tools/docs-lint.sh > [...]/baseline-3b02871.txt
observed: docs-lint: OK — 19 documents, 111 anchors, 1669 references, 110 beans, 42 graph edges, 48 selectable, 110 bean ids, 0 introduced, 110 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0

head:     3b02871 + this bean's `status:` line, this section absent
cmd:      /bin/bash tools/docs-lint.sh > [...]/bareflip.txt
observed: docs-lint: OK — 19 documents, 111 anchors, 1669 references, 110 beans, 42 graph edges, 50 selectable, 110 bean ids, 0 introduced, 110 on origin/main, 1 closing transitions, 5 criteria checked, 4 unnumbered.
exit:     0
```

`0 closing transitions, 0 criteria checked` becomes `1 closing transitions, 5 criteria checked`,
and `48 selectable` becomes `50`. **Check 12 reports an acyclic graph on both**, at 42 edges
across 110 beans; the `OK` line is the check's own vacuity assertion and a run that parsed
nothing reports zero rather than success (`doc:05-authoring-for-agents#checks`).

**And that green line means more here than it did before `3b02871`, which is checked rather
than assumed.** Before the wrapper, an analyser destroyed mid-run wrote nothing, no `fail`
fired, and this same line printed at exit 0 with stdout byte-identical to a clean run — so
`42 graph edges, 48 selectable` was consistent with check 12's analyser never having parsed.
Block C is that claim under test on this tree: check 12's acyclicity analyser destroyed makes
this gate exit non-zero and say so.

The two counts the `OK` line does not name are the two beans, so the difference is named rather
than inferred. The set is `AGENTS.md` step 1's: `status: todo`, not `type: epic`, every
`blocked_by` id resolving to a `completed` bean.

```
head:     3b02871, and the same tree with the `status:` line flipped
cmd:      /usr/bin/awk -f [...]/selectable.awk .beans/*.md > [...]/sel-before.txt
          /usr/bin/awk -f [...]/selectable.awk .beans/*.md > [...]/sel-after.txt
          diff [...]/sel-before.txt [...]/sel-after.txt
observed: 49c49,51
          < selectable=48
          ---
          > modus-0124
          > modus-0125
          > selectable=50
exit:     1
```

`modus-0124` and `modus-0125` are the whole of the difference, and both are `blocked_by:
[modus-0123]` alone. `bean:0126` is `blocked_by: [modus-0123, modus-0125]` and stays
unselectable, correctly: `completed` means `completed`, and `bean:0125` is still `todo`.
`bean:0127` is `blocked_by: [modus-0126]` and stays unselectable behind it. `bean:0118` is
`in-progress` and is not in the set in either arm — it is the parent of five children and only
this one closes here.

The enumerator above is this closing change's own script and not the gate's, so its answer
needs a check that is not itself: it returns **48** on the tree whose `OK` line reads
`48 selectable`, and **50** on the tree whose `OK` line reads `50 selectable`. Two mechanisms,
one written for this record and one already in `qualityCheck`, agreeing on both arms.

### The gate applied no pressure to write any block above

The second arm of Block E is a **bare `status:` flip** — one changed line, this whole section
absent — and it is green at exit 0, reporting `1 closing transitions, 5 criteria checked`.
The five `### Criterion N` sub-headings this bean already carried from PR #77 answer all five
criteria on their own, so check 14 was satisfied before a word of this close was written.

Everything above is author discipline, not gate pressure, and the same was true of
`bean:0093`'s close. That is not a defect in check 14: a green check 14 is a statement about
the shape of a record and never about the verdict recorded in it, which
`doc:05-authoring-for-agents#checks` already states. What has no home anywhere is the verdict
vocabulary itself — this bean closes a criterion `met in part`, `bean:0093` closed one
`NOT MET AS WORDED`, and `bean:0049` closed one `NOT MET`, three phrasings for one practice
that no document permits, forbids or names. Raised as `bean:0128` by the change that closes
this bean.

### This close is itself an instance of `bean:0120`

`doc:00-constitution#bean-lifecycle` ends by saying the close "is the first act of the session
after a merge", and nothing establishes that the session happens. This close happened because
it was dispatched, which is a fact about dispatch and not about a rule. The backlog it clears
is one tree deep, and that is the honest size rather than a supporting one:

```
head:     3b02871
cmd:      git log --format='COMMIT %h %ad %s' --date=short -p origin/main \
            -- '.beans/modus-0123--an-analyser-that-dies-must-make-the-gate-go-red.md' \
            | /usr/bin/grep -E '^COMMIT |^\+status:|^-status:' > [...]/backlog.txt
observed: COMMIT 3b02871 2026-09-04 fix(docs-lint): make a dead analyser fail the gate (#77)
          +status: in-progress
exit:     0
```

One commit and one `+status:` line with no `-status:` beside it. `3b02871` is the only tree
`main` has ever held this bean `in-progress` on, which is the shortest such run the corpus can
produce and is not evidence that `bean:0120`'s gap is small. The absent `-status:` is that
bean's E4 finding on a ninth instance: this bean was **created** `in-progress` rather than
flipped there, so `doc:00-constitution#bean-lifecycle`'s `todo` → `in-progress` transition is
not what put it on `main` and a mechanism resting on that transition would rest on an act the
record shows did not occur. `/usr/bin/grep` is BSD grep 2.6.0-FreeBSD, named because this
harness's interactive `grep` is a shell function running `ugrep 7.8.4` and CI's is a third one.
