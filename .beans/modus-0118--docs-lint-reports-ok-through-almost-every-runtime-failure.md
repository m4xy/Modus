---
# modus-0118
title: docs-lint reports OK at exit 0 through almost every runtime failure
status: todo
type: fix
priority: high
created_at: 2026-09-04T00:00:00Z
---

# `docs-lint` reports `OK` at exit 0 through almost every runtime failure

`tools/docs-lint.sh` runs `awk` twenty-two times. Exactly one of those invocations has its
exit status inspected.

```
cmd:      awk '/^[ \t]*#/ { next } { n += gsub(/(^|[^A-Za-z_.-])awk[ \t]/, "&") } END { print "awk_invocations=" n }' tools/docs-lint.sh
observed: awk_invocations=22
cmd:      awk '/^[ \t]*#/ { next } /(^|[^A-Za-z_.-])awk[ \t]/ { printf "%d ", FNR } END { print "" }' tools/docs-lint.sh
observed: 53 89 91 94 190 198 215 217 221 261 395 443 444 460 484 487 521 524 544 566 583 664
cmd:      grep -n 'awk_rc' tools/docs-lint.sh
observed: 668:    awk_rc=$?
          669:    if [ "$awk_rc" -ne 0 ]; then
          670:      fail 14 "$f: the check 14 analyser exited $awk_rc and examined nothing; tools/lib/docs-lint-fence.awk and tools/lib/docs-lint-c14.awk must both be present and parse"
```

Taken at `13d8c27`. `tools/docs-lint.sh` is not identical to `origin/main`'s there —
`git diff origin/main --stat -- tools/docs-lint.sh` reports `6 insertions(+), 3 deletions(-)`,
all in the header comment that pull request added — but no invocation is among them. The
comment-skipping term in the counting command matters: line 660 contains the words `awk exits
2` in prose and is not an invocation. `grep` here was the harness's `ugrep 7.8.4`; the two
counting commands are `awk`, which behaves the same under BSD awk and CI's, and are the ones
the figures rest on.

The invocation at line 664 is check 14's, guarded at 668-670. **For the other twenty-one, an
analyser that dies produces empty output, the loop that reads it finds nothing, no `fail`
fires, and the gate prints its `OK` line at exit 0.**

## The instance: check 12's analyser destroyed, and nothing in the output moves

`tools/docs-lint.sh:460-479` is the acyclicity analyser for the bean dependency graph — the
check `bean:0035` added because a backlog could deadlock with every file individually
well-formed. Replacing line 463 with an awk syntax error destroys it. The gate's stdout is
**byte-identical** to the unmodified run:

```
plant:    tools/docs-lint.sh:463, `    removed = 1` replaced by `    removed = = 1`
cmd:      /bin/bash tools/docs-lint.sh
observed: docs-lint: OK — 19 documents, 111 anchors, 1471 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 99 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr:   awk: syntax error at source line 4
           context is
          	    removed = >>>  = <<<  1
          awk: illegal statement at source line 4
          awk: illegal statement at source line 4
exit:     0

cmd:      cmp out.baseline out.awk-syntaxerr-c12    # the unmodified run against the planted one
observed: (no output)
exit:     0
```

`tools/docs-lint.sh:707-708` calls the counts line the gate's vacuity assertion — "a check
that silently examined nothing reports zero here, where check 11 shipping inert went
unnoticed for four plants". It carries twelve figures and **not one of them is derived from
the cycle check**. `37 graph edges` comes from `$TMP/bean-edges.uniq`, which `sort -u` wrote
at line 459, one line before the analyser. So the assertion that exists to catch an inert
check cannot see this one, and an entirely inert check 12 is invisible in the gate's output.
Both counts above are of a corpus this bean belongs to and move with it
(`doc:50-memory-and-evidence#corpus-figures`); the load-bearing observation is `exit: 0`
beside the word `OK`, and the byte-identity, neither of which any count affects.

## The measured fail-open boundary

Each row is one line planted into `tools/docs-lint.sh` at line 29, immediately after
`set -uo pipefail` at line 28, then run as `/bin/bash tools/docs-lint.sh`. `set -u` is the
only fail-closed mechanism in the file and it is fail-closed only in the top-level shell.

| planted at line 29 | exit | reached `docs-lint: OK` |
|---|---|---|
| `echo "$__probe_unbound_top"` | 1 | no — **fail-closed** |
| `;;` | 2 | no — **fail-closed** |
| `echo x \| ;;` | 2 | no — **fail-closed** |
| `false` | 0 | yes |
| `/usr/bin/false` | 0 | yes |
| `cat /no/such/file/anywhere` | 0 | yes |
| `cd /no/such/dir/anywhere` | 0 | yes |
| `false \| cat` | 0 | yes |
| `probe_x="$(echo "$__probe_unbound_sub")"` | 0 | yes |
| `echo "$__probe_unbound_pipe" \| cat` | 0 | yes |
| `probe_y="$( ;; )"` | 0 | yes |
| `awk "BEGIN { x = = 1 }" /dev/null \| cat` | 0 | yes |
| `probe_z="$(awk "BEGIN { x = = 1 }" /dev/null)"` | 0 | yes |

`false`, `/usr/bin/false` and `false | cat` reached the `OK` line having written nothing at
all to stderr — no diagnostic a caller could notice even if it were reading stderr. The rest
did write one, which is the more insidious case in a gate whose output nobody reads on a green
run: `set -u` reported `__probe_unbound_sub: unbound variable` from inside the command
substitution and the script carried on regardless.

**Where the boundary is not where it was first stated.** The finding that produced this bean
described "unbound variables *or* syntax errors occurring inside a command substitution or a
pipeline element" as fail-open. That is true of unbound variables in both positions and of a
bash syntax error inside `$( )`, which bash parses when the word is expanded. It is **not**
true of a bash syntax error in a pipeline element: bash parses the whole script before
executing any of it, so `echo x | ;;` is a parse error at line 29 and exits 2 before check 1
runs. What is fail-open in a pipeline element is an **analyser's** syntax error — awk's — and
that is the shape check 12 actually has. The distinction is the whole point: the file's own
`bash -n` parse and `set -u` cover bash's errors; nothing covers the analysers'.

```
cmd:      /bin/bash tools/docs-lint.sh      # with `echo x | ;;` planted at line 29
observed: /Users/[...]/tools/docs-lint.sh: line 29: syntax error near unexpected token `;;'
          /Users/[...]/tools/docs-lint.sh: line 29: `echo x | ;;'
exit:     2
```

Every plant above was made against a pristine copy taken before the first one and restored
from that copy after each, with no `git` operation involved, so nothing else in the tree
could be discarded (`bean:0102`). `diff` against the pristine copy after the last restore
reported the files identical.

## What is not in scope

This is not a bash 3.2 problem and it is not `bean:0049`'s. `bean:0049` pinned the interpreter
and made the compatibility claim falsifiable. `bean:0049`'s criterion 2 was reaching for this —
it asked for an interpreter to reject a planted construct, and no interpreter can, because the
script has no failure path — but the concern is larger than that criterion and outlives it.

**Could not verify: the boundary on the CI image.** Every probe above ran under `/bin/bash`
3.2.57 on macOS, which is what `build.gradle.kts` pins. On the Linux runner `/bin/bash` is
bash 5, and no run of these probes there exists. Nothing in the mechanism is version-specific
— the missing option is `set -e`, and what goes unchecked is `awk`'s exit status — but that is
reasoning, not a measurement, and it is the figure in this bean most likely to be wrong. Take
the probes on the runner before relying on the boundary there.

It reaches beyond `tools/docs-lint.sh`, measured at **`07ace1c`** — re-run and unchanged at
`ea4185f` and at the third review round's head. This block was stamped `13d8c27` when it was
written, and that stamp was wrong: at `13d8c27` the first command returns **one** hit, not two,
because `tools/bash-compat-lint.sh:23` does not exist there. `07ace1c` introduced it, and is
the earliest head at which this capture reproduces. The counter-evidence is below the block.
`grep` throughout is `/usr/bin/grep`, BSD grep 2.6.0-FreeBSD — the same grep CI has, and not
the harness's `ugrep` shim:

```
cmd:      /usr/bin/grep -n 'set -e' tools/docs-lint.sh tools/docs-lint-test.sh tools/bash-compat-lint.sh
observed: tools/bash-compat-lint.sh:19:# `mapfile` writes `command not found` and exits 0 from the script, which has no `set -e`.
          tools/bash-compat-lint.sh:23:# table is the six families that diverge silently or behind a diagnostic `set -e` would
exit:     0
cmd:      /usr/bin/grep -c '$?' tools/docs-lint-test.sh
observed: 0
exit:     1
cmd:      /usr/bin/grep -n '$?' tools/bash-compat-lint.sh
observed: (no output)
exit:     1
```

And the counter-evidence for the stamp, which is why the head above is `07ace1c`:

```
cmd:      git show 13d8c27:tools/bash-compat-lint.sh | /usr/bin/grep -n 'set -e'
observed: 19:# `mapfile` writes `command not found` and exits 0 from the script, which has no `set -e`.
cmd:      git show 07ace1c:tools/bash-compat-lint.sh | /usr/bin/grep -n 'set -e'
observed: 19:# `mapfile` writes `command not found` and exits 0 from the script, which has no `set -e`.
          23:# table is the six families that diverge silently or behind a diagnostic `set -e` would
```

Nothing in this bean rests on the difference — the conclusion holds at every head on the
branch — but a stamp exists so a reader can re-run at it, and one that cannot be re-run at is
not a stamp. The general form is `bean:0102` and `doc:50-memory-and-evidence#capturing`: a
capture takes the head it was taken at, not the head the surrounding prose is about.

No script has `set -e` — the two hits are prose about its absence. `tools/docs-lint-test.sh`
has the same shape as `docs-lint`: `set -uo pipefail` at line 91, and `$?` does not occur in
it at all, so neither of its two `awk` invocations is status-checked.
`tools/bash-compat-lint.sh` has a much smaller exposure for a different reason: it never
inspects `$?` either, but it accumulates into an explicit `rc` that gates its own `OK` line,
and it exits 2 rather than continuing when a required file is missing, when no rule loads, or
when no script matches its glob. Its four `awk` invocations are still unguarded; what saves it
is that three of them feed assertions that would fail loudly on empty output.

## The remedy, and the half of it that already exists

**A failure path for runtime errors.** Either an `ERR` trap appending to `$TMP/fails.txt` —
the file that already exists so that a `fail` inside a pipeline subshell still changes the
exit status — or an `awk_rc` guard after every analyser, generalising `tools/docs-lint.sh:668-670`
from check 14 to all twenty-two. The trap is the smaller diff and the guard is the more
legible one; whichever is chosen has to answer what happens to the invocations inside `$( )`,
where the failing command's status is the substitution's and is discarded by the assignment.

**Per-check discrimination proof.** Neuter each check's analyser in turn and assert the gate
goes red — the three observations `doc:50-memory-and-evidence#evidence-kinds` requires of a
mechanism claiming to discriminate, applied per check rather than to the gate as a whole.
**This mechanism already exists in this repository and is simply not pointed at `docs-lint`.**
`tools/bash-compat-lint.sh:117-227` does exactly this for its own rules on every invocation:
it plants each rule's sample violation, asserts the scan finds it exactly once and attributes
it to that rule, and asserts a fixture of legal near-misses produces nothing. That range is
`n_planted=0` through the `fi` closing the negative-control assertion, at the third review
round's head; it was `116-194` before that round extended the fixture, and it is written with
its command because a bare line range does not survive the next edit to the file it points into
(`doc:50-memory-and-evidence#unverified-shapes`):
`/usr/bin/grep -n 'n_planted=0' tools/bash-compat-lint.sh` prints `117`, and
`awk 'NR >= 224 && NR <= 227' tools/bash-compat-lint.sh` prints the closing `if`/`fi`.

Every analyser in `docs-lint` reads a named file, so every one of them can be fed a fixture the
same way —
check 12's cycle analyser at line 460 reads `$TMP/bean-edges.uniq`, check 14's at 664 reads
the bean under test. Which of the twenty-two are worth a fixture and which are better served
by the failure path alone has not been decided here; that is the first task of the work, not a
finding of this bean.

The two halves do not subsume each other. A failure path catches an analyser that dies; a
discrimination proof catches one that runs, returns 0, and matches nothing —
`doc:00-constitution#observed-failing`, and the reason check 11 shipping inert went unnoticed
for four plants.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | Every `awk` invocation in `tools/docs-lint.sh` either has its exit status inspected or is covered by a trap that records the failure, and the count of unguarded invocations is zero by the same command that printed 22 here | `command`, the counting command re-run, with the tree it was taken at |
| 2 | An analyser destroyed by a syntax error makes the gate exit non-zero. Observed for check 12 specifically, by re-planting the syntax error at `tools/docs-lint.sh:463` that this bean records producing a byte-identical `OK` | `test-run`, plant / observe / revert per `doc:00-constitution#observed-failing` |
| 3 | Each of the fail-open rows in the boundary table above makes the gate exit non-zero, or is recorded here with a stated reason why it may not | `test-run`, the probe harness re-run, each row's exit status pasted from the capture |
| 4 | Each check's analyser, neutered in turn, makes the gate go red — and the unmodified tree is green, so the mechanism is observed both firing and silent | `test-run`, per check, both halves, `doc:50-memory-and-evidence#evidence-kinds` |
| 5 | The counts line at `tools/docs-lint.sh:707-708` carries a figure derived from every check that can be inert, or the checks it cannot cover are named there | `citation` at the head sha, plus criterion 4's red runs |
| 6 | `tools/docs-lint-test.sh` is assessed against the same boundary, and either fixed or recorded as a residual naming the work item that closes it | `command`, or a citation to the residual |
| 7 | `./gradlew qualityCheck` is green | `test-run` |

Criterion 3 is the one that can be closed vacuously: a probe harness that stopped planting
would report every row passing. It closes on the harness being observed producing a red run
and a green one on the same tree, not on a list of exit statuses.

## References

`bean:0049` — pinned the interpreter, built `tools/bash-compat-lint.sh`, and recorded
criterion 2 NOT MET because no interpreter can reject a construct in a script with no failure
path. That measurement is the entry point to this one.
`doc:00-constitution#observed-failing` — a mechanism nobody has watched reject a real
violation is not enforcement, it is a claim.
`doc:50-memory-and-evidence#evidence-kinds` — firing on every input is also firing.
`doc:05-authoring-for-agents#checks` — the table that counts the checks this gate runs.
