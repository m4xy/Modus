---
# modus-0124
title: docs-lint still fails open on every runtime error that is not an analyser
status: in-progress
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

## The boundary re-derived, and the plant point that stopped measuring the gate

`bean:0118` measured its table by planting one line **immediately after `set -uo pipefail`** —
line 28 at the head it was taken at, line 32 on `main` now. Since `bean:0123` that point sits
**above the mechanism**: the `awk` wrapper is a shell function defined further down the file,
and this change's `ERR` trap is armed further down still. A plant there measures the prefix of
the script where nothing is armed yet. So the table's two analyser rows still read `exit 0`
and `reached OK`, and that is a fact about the plant point rather than about the guard
`bean:0123` shipped — **the first figure in `bean:0118` that no longer means what it says.**
The correction is carried in `bean:0118` itself, which is `in-progress` and therefore not
frozen by check 11; it is not left as a note in a child bean nobody reading that table will
open.

**And the harness that produces these tables is committed**, as
`tools/docs-lint-boundary-probe.sh`. Every earlier version lived in a scratch directory and
was deleted, so this table could be read and not re-run — a figure whose measurement needs
apparatus nobody has built, which `doc:50-memory-and-evidence#evidence-kinds` names as the
shape most likely to be fabricated. It takes an interpreter argument, so the row of "Not
verified here" that says the boundary table has never been taken under bash 5 is now closable
with one command rather than with a rebuild. It is deliberately not in `qualityCheck`: one
pass is forty-two runs of the whole gate over the whole corpus.

Re-derived at that anchor, unchanged:

```
head:     494f174, working tree clean. tools/docs-lint.sh is byte-identical at 277c4d5:
          `git diff --stat 494f174 277c4d5 -- tools/docs-lint.sh` prints nothing at exit 0.
cmd:      /bin/bash [...]/probe.sh — each row planted into a COPY at tools/.docs-lint-probe-rowN.sh
          immediately after `set -uo pipefail`, the copy run, the copy deleted
gate under probe: [...]/tools/docs-lint.sh
row planted after set -uo pipefail                 exit OK?  stderr bytes
1   echo "$__probe_unbound_top"                    1    no   157
2   ;;                                             2    no   283
3   echo x | ;;                                    2    no   292
4   false                                          0    yes  0
5   /usr/bin/false                                 0    yes  0
6   cat /no/such/file/anywhere                     0    yes  55
7   cd /no/such/dir/anywhere                       0    yes  172
8   false | cat                                    0    yes  0
9   probe_x="$(echo "$__probe_unbound_sub")"       0    yes  157
10  echo "$__probe_unbound_pipe" | cat             0    yes  159
11  probe_y="$( ;; )"                              0    yes  331
12  awk "BEGIN { x = = 1 }" /dev/null | cat        0    yes  116
13  probe_z="$(awk "BEGIN { x = = 1 }" /dev/null)" 0    yes  116
--- git status --porcelain after every probe was removed:
--- (end)
```

The same thirteen rows, at the same head, planted **after the mechanisms are armed** instead —
immediately after `FM_FILES=…`, which is the first statement below both. Same tree, same gate,
same rows; only the plant point moves:

```
head:     this branch's working tree; the gate under probe is `git show 277c4d5:tools/docs-lint.sh`
method:   each row planted into a COPY at tools/.docs-lint-b-N.sh, the copy run, the copy
          deleted; six copies at a time; no tracked file written (bean:0102)
gate under probe: [...]/gate-277c4d5.sh (tools/docs-lint.sh at 277c4d5)
plant point: immediately after: FM_FILES="$(ls documentation/*.md documentation/adr/*.md)"
occurrences of that line in that gate: 1
planted line                                   exit OK?  stderr lines
echo "$__probe_unbound_top"                    1    no   1
;;                                             2    no   2
echo x | ;;                                    2    no   2
false                                          0    yes  0
/usr/bin/false                                 0    yes  0
cat /no/such/file/anywhere                     0    yes  1
cd /no/such/dir/anywhere                       0    yes  1
false | cat                                    0    yes  0
probe_x="$(echo "$__probe_unbound_sub")"       0    yes  1
echo "$__probe_unbound_pipe" | cat             0    no   1978
probe_y="$( ;; )"                              0    yes  2
awk "BEGIN { x = = 1 }" /dev/null | cat        1    no   5
probe_z="$(awk "BEGIN { x = = 1 }" /dev/null)" 1    no   5

--- git status --porcelain:
 M tools/docs-lint.sh
--- (end)
```

**Two analyser rows go from `exit 0` to `exit 1`, and eight rows do not move.** That is
`bean:0123` working, measured where it is armed, and it is the whole of the difference between
the two blocks. `echo x | ;;` at exit 2 is bash parsing the file before it runs any of it, so
it is fail-closed at either point. The eight that do not move are this work item.

**One row is worse than the table records.** `echo "$__probe_unbound_pipe" | cat` does not
merely go unrecorded: `set -u` firing inside a pipeline element **exits that element's
subshell**, and a subshell exiting runs the EXIT trap it inherited — `rm -rf "$TMP"`. The
gate's scratch directory goes with it, every record after that point is written into a
directory that is not there, and the run ends `docs-lint: OK` at exit 0 — on 288 stdout lines
of which 287 are `FAIL check`, with every count between the commas of that `OK` line empty.
`bean:0118` recorded that row as a plain fail-open because at its plant point `$TMP` has not
been created yet. The same shape, reached the other way, is the scratch-directory row below.


## The opt-out, re-derived from the semantics — and the two clean trees it went red on

The first version of this opt-out held three commands, and each was there because a run had
been **seen** firing at it. That is the wrong warrant, and it produced the wrong list twice:
once when the runner found a site this machine could not see, and once — recorded here —
when two trees that break no documented rule turned the gate red.

**Blocker 1: a `status: draft` document that declares no anchor.**
`doc:05-authoring-for-agents` §2's front-matter table says `provides` "May be empty only when
`status: draft`", so a draft with no anchors is legal in as many words; check 2 permits it,
and check 5's `grep -oE '^#+ .*<a id="…">'` then finds nothing and exits 1 for saying so.

```
head:     688f3ba, tools/docs-lint.sh as that commit has it
tree:     that worktree plus ONE UNTRACKED file, documentation/98-a-draft-with-no-anchors.md,
          carrying all seven required keys, `status: draft`, an empty `provides:` and no
          `<a id=` anywhere; removed by the same script, `git status --porcelain` empty after
cmd:      /bin/bash tools/docs-lint.sh   and   /opt/homebrew/bin/bash tools/docs-lint.sh
--- /bin/bash (GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25))
exit: 1
stdout: docs-lint: 1 failure(s).
stderr: FAIL check -  line 301: a command exited 1 and nothing checked it: 'sort -u > "$TMP/declared.txt"' (pipeline exited 1 0 0, left to right)
--- /opt/homebrew/bin/bash (GNU bash, version 5.3.9(1)-release (aarch64-apple-darwin25.1.0))
exit: 1
stdout: docs-lint: 1 failure(s).
stderr: FAIL check -  line 303: a command exited 1 and nothing checked it: 'sort -u > "$TMP/declared.txt"' (pipeline exited 1 0 0, left to right)
--- the same tree against the gate this SHIPS, both interpreters, byte-identical, stderr empty:
exit: 0
docs-lint: OK — 20 documents, 111 anchors, 1738 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
--- git status --porcelain after the probe document was removed: empty
--- (end)
```

Twenty documents rather than nineteen, and the reference count moves with the probe file's own
two references: the plant is visible in the counts line, which is what says the run examined it
rather than skipping it.

**The inference that let it through is in this bean, and it is false at that site.** The
red-corpus audit below concluded that "every one of them is a line whose next few lines report
the same defect on stdout". Check 5's site fires on **zero anchors declared**, and check 5
reports only a **promised** anchor that no heading declares. Those are different conditions:
the first is legal, the second is the defect. One is not evidence about the other.

**Blocker 2: an `AGENTS.md` with no `derived` row.** Nothing requires one. Check 9's
`grep -nE '^\|.*derived' AGENTS.md` is structurally the same command as check 10's
`grep -noE '\bbeans/[0-9]'`, which *was* opted out; the two differ only in what today's
corpus happens to contain.

```
head:     688f3ba, tools/docs-lint.sh as that commit has it
tree:     AGENTS.md is TRACKED, so the plant is into a COPY of the corpus — AGENTS.md,
          CLAUDE.md, .beans.yml, .beans, documentation, tools, .github, config and
          architecture-tests copied to a scratch directory and the gate run there. No `.git`
          there, so the five diff-shaped counts report `-`. The edit is one word:
          `derived, not restated here` -> `not restated here`
--- the copy BEFORE the edit, with the 688f3ba gate, /bin/bash 3.2.57 (the control that says
    the copying is not what turns it red):
exit: 0
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, - introduced, - on origin/main, - closing transitions, - criteria checked, - unnumbered.
--- after the edit; /usr/bin/grep -cE '^\|.*derived' AGENTS.md now prints 0.
    /bin/bash 3.2.57 and /opt/homebrew/bin/bash 5.3.9, byte-identical:
exit: 1
stdout: docs-lint: 1 failure(s).
stderr: FAIL check -  line 390: a command exited 1 and nothing checked it: 'grep -nE '^\|.*derived' AGENTS.md > "$TMP/derived.txt"'
--- the same edited copy against the gate this SHIPS, both interpreters, byte-identical:
exit: 0
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, - introduced, - on origin/main, - closing transitions, - criteria checked, - unnumbered.
--- (end)
```

### The rule the list is derived from now

> `grep` exits 1 to say the pattern is ABSENT, and 2 or more to say it could not look.
> `grep -c` says the same thing twice: it exits 1 whenever the count it has just printed is 0.
> **Wherever the gate reads the OUTPUT — the count, the matching lines, the file they were
> written into — the status restates something the gate already holds.** Such a site is opted
> out. It stays armed only where "nothing found" means the tree is broken and no check says so.

That is a rule about the command, not about a corpus, which is what makes it re-derivable by
the next reader. Applied to `tools/docs-lint.sh` it names every `grep` in the file except one,
and it retires the `|| true` and `|| :` blanket tolerances beside five of them — those swallow
every status, including the "could not look" this opt-out exists to keep. The sites are named
rather than counted, since the count moves with the next check anyone writes.

| site | why |
|---|---|
| check 2's `keys \| grep -cx "$k"` | 0 is the count line 221 reports as `key '$k' appears 0 times` |
| check 5's anchor `grep -oE` | **blocker 1** — a draft may declare no anchor |
| check 6's `rule:` counts, three of them | 0 is the count line 351 reports |
| check 6's `printf \| grep -c .` and `awk \| grep -c .` | 0 is the count lines 362 and 368 report |
| check 8's two budget `grep -oE`s | absence is what line 376 reports |
| check 8's two `grep -c ''` | a file of 0 lines has a count of 0; check 1 reports an empty file |
| check 9's `grep -nE '…derived'` | **blocker 2** — nothing requires a `derived` row |
| check 11's `grep -c ''`, `grep -m1`, `grep '^### '`, two `grep -c` | all five read the output; three were `\|\| true`, which is worse |
| check 12's two `\| grep -c .` after a glob | 0 is the count lines 532 and 548 report |
| check 12's `n_ready` | **the sharpest**: `grep -c .` returns 1 on exactly the condition line 602 reports — "no bean is selectable" cost two records |
| check 13's `id_length` grep | absence is what line 626 reports |
| check 13's `n_marker` | 0 is the count line 661 reports |
| check 13c's `n_bean_ids`, `n_introduced`, `n_main_ids`, `n_beans`, the `ls-tree \| grep` | all read the output; four were `\|\| true` |
| the four `grep -c` of the **counts line** | a record written there is written AFTER `n_fail` has been read, so an armed site there records into a number nobody inspects again: fail-open, not enforcement |
| `n_fail` itself | already opted out; unchanged |
| check 10's bare-`beans/` grep, check 6's reference `grep -oE` | already opted out; unchanged |

**The one site left armed** is `BEAN_PREFIX="$(grep -E '^ *prefix:' .beans.yml \| …)"`. A
`.beans.yml` with no `prefix:` is not a legal tree — every bean path checks 6, 12 and 13 build
comes from that value — and, unlike `id_length:` twelve lines below it, **no check reports its
absence**. The trap's record is the only signal there is, so it keeps it. That asymmetry is the
rule doing work rather than a list being copied.

**`ls` is deliberately not routed through `absent_ok`.** Its "no such file" is status 1 on the
BSD `ls` macOS ships and 2 on GNU coreutils, so tolerating it would mean tolerating 2 — exactly
the widening the suite now plants a `grep` that could not look to catch. The six `ls <glob>
2>/dev/null` sites are answered by `glob_lines` instead, which is the shell's own `[ -e ]` over
the expanded glob and cannot fail, so there is no status left to tolerate.

**And `absent_ok` now names the command it refuses.** With `set -E` off, the ERR trap fires at
the CALLER for the function's non-zero return, and `$BASH_COMMAND` there holds `return "$ec"` —
the last command `absent_ok` ran — under both interpreters:

```
head:     this branch's working tree
cmd:      bash <scratch>/optprobe.sh, a five-line file defining absent_ok, a recording ERR
          trap, and one call: `absent_ok grep -c . /no/such/file/__probe_cannot_look__`
--- /bin/bash 3.2.57:            rec: status=2 cmd=[return "$ec"]
--- /opt/homebrew/bin/bash 5.3.9: rec: status=2 cmd=[return "$ec"]
```

So a `grep` that could not look would have been recorded as a `return`. `absent_ok` writes its
own `fail` naming the argv, and the trap's record stands beside it — one failure, two records,
exactly as one dead analyser is two.

## The audit, which is what made the trap safe to arm — and which had to be run four times

`bean:0124` names the audit of every command in the file as the work, and
`doc:00-constitution#observed-failing`'s negative half is why: a mechanism that fires when
nothing is wrong is worse than the gap, because it will be removed rather than fixed. The
audit is a measurement, not a reading. A copy of the gate gets `set -E` and an `ERR` trap that
records — errtrace ON here, so a firing inside a function or a subshell cannot be missed — and
the green run says which commands return non-zero when nothing is wrong.

**The two audits BELOW are of `main`'s gate, and they are sound.** `main`'s gate arms no `ERR`
trap of its own, so a recorder inserted after `set -uo pipefail` there is live for the whole
file and nothing replaces it. The two audits of the gate this SHIPS were a different matter,
and they are corrected below the CI section.

```
head:     this branch's working tree; the gate under audit is main's at 277c4d5
method:   a COPY of that gate with `set -E` and a RECORDING ERR trap inserted after
          `set -uo pipefail`; the copy deleted and `git status` printed after
gate under audit: [...]/gate-277c4d5.sh
interpreter:      /bin/bash (GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25))
--- the two lines inserted:
32a33,34
> set -E
> trap 'printf "%s\t%s\n" "$?" "$BASH_COMMAND" >> [...]/errsites-before.txt' ERR
--- the run:
exit: 0
stdout:
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr byte count: 0
--- ERR firings on a green run, by (status, command), most frequent first:
   4 1	awk -v f="$f" '{ print f "\t" $0 }' >> "$TMP/refs.tsv"
   1 1	n_fail="$(grep -c . "$TMP/fails.txt")"
--- total firings: 5
--- distinct commands: 2
--- git status --porcelain:
 M tools/docs-lint.sh
--- (end)
```

**Two commands, and `bean:0118` predicted one of them.** `n_fail="$(grep -c . …)"` on an empty
record file is the one that bean names; the other is check 6's reference extraction, where
`grep -oE` finds no reference in a file that carries none.

**And that audit was incomplete, which the runner said and no run on this machine had.** The
first CI run of this branch did the thing this section exists to prevent: it went red on a
clean corpus. The site is check 10's, `grep -noE '\bbeans/[0-9]' … | while read`, which finds
no bare `beans/` path on a tree that has none and exits 1 for saying so.

```
head:     33939770456, job 101234783208, ubuntu-latest, of head dd64b8f
cmd:      ./gradlew qualityCheck --stacktrace -x backofficeTypecheck -x backofficeLint -x backofficeFormatCheck
observed: docs-lint-gate-test: interpreter /bin/bash (bash 5.2.21(1)-release)
          docs-lint-gate-test: analyser awk — GNU Awk 5.2.1, API 3.2, PMA Avon 8-g1, (GNU MPFR 4.2.1, GNU MP 6.3.0)
          FAIL check -  line 406: a command exited 1 and nothing checked it: 'grep -noE '\bbeans/[0-9]' documentation/*.md AGENTS.md CLAUDE.md 2> /dev/null' (pipeline exited 1 0, left to right)
          docs-lint: 1 failure(s).
          [...] the six assertion rows that then failed, every one of them downstream of that
                single record: `and the gate says it failed rather than printing OK`, at
                `docs-lint: 3 failure(s).` against the 2 the analyser plant alone makes; the
                three negative-control rows, since the unmutated copy was red, printed no OK
                line and wrote to stderr; `and the pipeline record carries the statuses`; and
                `and records nothing else: five plants, five distinct records`, which found six
          docs-lint-gate-test: 43 passed, 6 failed.
exit:     1, and the `gate` job failed
```

**It is not the two greps disagreeing.** `/usr/bin/grep` exits 1 on that command here too, so
the site was always a success-path site; what differs is the **interpreter**. Under bash
3.2.57 a pipeline whose last element is a compound command does not reach the `ERR` trap, and
under bash 5 it does. An audit run under the interpreter `build.gradle.kts` pins therefore
cannot see a site the runner will fire on, and this one did not.

```
head:     this branch's working tree
cmd:      /usr/bin/grep -noE '\bbeans/[0-9]' documentation/*.md AGENTS.md CLAUDE.md 2>/dev/null > /dev/null; echo "status alone=$?"
observed: === /usr/bin/grep — grep (BSD grep, GNU compatible) 2.6.0-FreeBSD
          status alone=1
```

So the audit is re-run under Homebrew's bash 5.3.9, which is not the runner's 5.2.21 but is
on the same side of the difference, and it finds **three**:

```
head:     this branch's working tree; the gate under audit is main's at 277c4d5
method:   the same recording trap, at the same insertion point, under bash 5.3.9
gate under audit: [...]/gate-277c4d5.sh
interpreter:      /opt/homebrew/bin/bash (GNU bash, version 5.3.9(1)-release (aarch64-apple-darwin25.1.0))
--- the two lines inserted:
32a33,34
> set -E
> trap 'printf "%s\t%s\n" "$?" "$BASH_COMMAND" >> [...]/errsites-before5.txt' ERR
--- the run:
exit: 0
stdout:
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr byte count: 0
--- ERR firings on a green run, by (status, command), most frequent first:
   4 1	awk -v f="$f" '{ print f "\t" $0 }' >> "$TMP/refs.tsv"
   1 1	n_fail="$(grep -c . "$TMP/fails.txt")"
   1 1	grep -noE '\bbeans/[0-9]' documentation/*.md AGENTS.md CLAUDE.md 2> /dev/null
   1 1	grep -c . "$TMP/fails.txt"
--- total firings: 7
--- distinct commands: 4
--- git status --porcelain:
 M tools/docs-lint.sh
--- (end)
```

The fourth line of that listing is the same site as the second: with errtrace on, `grep -c .`
inside the command substitution and the assignment that contains it are both recorded, which
is the doubling the shipped trap does not have because it does not set errtrace.

All three then went through `absent_ok`. **And the two "after" audits that said so were run
with the recorder somewhere it could not see anything.**

### The instrument was blind, and its zero was a coincidence

Both "after" audits inserted `set -E` and a recording `ERR` trap at line 33 — immediately
after `set -uo pipefail`. `tools/docs-lint.sh` arms **its own** `ERR` trap further down the
file, and a second `trap … ERR` REPLACES the first. So the recorder was live for the 59 lines
between the two arming points and dead for the remaining 789, and "0 firings" was a fact about
those 59 lines. On the unmodified corpus the number happens to be right, which makes it worse
rather than better: it is a figure that would have read the same had the instrument been
disconnected, and it was.

Demonstrated on the tree that made blocker 1 red. The gate goes red; the recorder at
`bean:0124`'s own insertion point logs **nothing**:

```
head:     688f3ba; the gate under audit is `git show 688f3ba:tools/docs-lint.sh`, copied to
          tools/.docs-lint-audit-$$.sh and deleted after
tree:     this worktree plus the untracked documentation/98-a-draft-with-no-anchors.md of
          blocker 1, removed by the same script; `git status --porcelain` after shows it gone
method:   `set -E` and a RECORDING ERR trap inserted immediately after `set -uo pipefail`
--- /bin/bash 3.2.57 and /opt/homebrew/bin/bash 5.3.9, identical:
exit: 1
stdout: docs-lint: 1 failure(s).
stderr byte count: 138
--- ERR firings, by (status, line, command), most frequent first:
--- total firings: 0
--- distinct commands: 0
--- (end)
```

The same recorder moved BELOW the gate's own `trap … ERR`, on the same tree, logs blocker 1
itself and nothing else:

```
head:     688f3ba; same gate, same tree, same script
method:   the recorder inserted immediately after the gate's own `trap … ERR` line, and
          CHAINED rather than replacing it — it records and then calls `docs_lint_err` with
          the same four fields, so the run is identical to an uninstrumented one and the
          gate's end-of-run check still finds the failure path working. A record-only
          recorder trips that check — under the `trap -p ERR` reading this audit was taken
          against, and under the live fire that replaced it, for the same reason: it stops
          `docs_lint_err` from producing the record. That is the instrument changing what it
          measures. errtrace ON, so a firing inside a function or a subshell cannot be missed
--- /bin/bash 3.2.57
exit: 1
stdout: docs-lint: 1 failure(s).
--- ERR firings, by (status, line, command), most frequent first:
   1 1	304	sort -u > "$TMP/declared.txt"
--- total firings: 1   distinct commands: 1
--- /opt/homebrew/bin/bash 5.3.9
exit: 1
stdout: docs-lint: 1 failure(s).
--- ERR firings, by (status, line, command), most frequent first:
   1 1	306	sort -u > "$TMP/declared.txt"
--- total firings: 1   distinct commands: 1
--- (end)
```

(The line numbers are three higher than the gate's own records — 301 and 303 — because the
recorder's three lines sit above them. Same site, same two interpreters, same difference of
two between them, for the reason `$LINENO` differs inside a `for` body under 3.2.57.)

### The audit redone, where the recorder can observe

Three lines inserted immediately after the gate's own `trap … ERR`, chaining into it, on the
clean tree, under both interpreters:

```
head:     688f3ba, working tree; the gate under audit is the one this ships
method:   a COPY at tools/.docs-lint-audit-$$.sh with `set -E` and a CHAINING recording ERR
          trap inserted after the gate's own; the copy deleted and `git status` printed after
occurrences of that anchor in the gate: 1
--- the lines inserted:
113a114,116
> set -E
> __audit_rec=[...]/rec-shipped-below-32.txt
> trap '__ec=$?; __ps="${PIPESTATUS[*]}"; __cmd="$BASH_COMMAND"; __ln=$LINENO; printf "%s\t%s\t%s\n" "$__ec" "$__ln" "$__cmd" >> "$__audit_rec"; docs_lint_err "$__ec" "$__cmd" "$__ln" "$__ps"' ERR

=== /bin/bash (GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25))
exit: 0
stdout:
docs-lint: OK — 19 documents, 111 anchors, 1737 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr byte count: 0
--- ERR firings on this run, by (status, line, command), most frequent first:
--- total firings: 0
--- distinct commands: 0

=== /opt/homebrew/bin/bash (GNU bash, version 5.3.9(1)-release (aarch64-apple-darwin25.1.0))
exit: 0
stdout:
docs-lint: OK — 19 documents, 111 anchors, 1737 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr byte count: 0
--- ERR firings on this run, by (status, line, command), most frequent first:
--- total firings: 0
--- distinct commands: 0

--- git status --porcelain:
 M .beans/modus-0118--docs-lint-reports-ok-through-almost-every-runtime-failure.md
 M .beans/modus-0124--the-non-analyser-fail-open-boundary-in-docs-lint.md
 M tools/docs-lint-gate-test.sh
 M tools/docs-lint.sh
?? tools/docs-lint-boundary-probe.sh
--- (end)
```

**This zero is a different zero from the one it replaces**, and the difference is the pair of
runs above: the same instrument, on a tree with one legal-tree defect, logs one firing and
names it. A mechanism observed silent and never observed firing is not discrimination
(`doc:50-memory-and-evidence#evidence-kinds`); the control is what makes the silence mean
something.

**A note on the `references` count, which is not constant across this bean's fences and cannot
be.** `.beans/modus-0124--*.md` is one of the files check 6 reads, so writing this bean changes
the number the gate prints while it is being written: `1736` in the fences taken before this
review round, `1737` in the ones taken during it, `1738` in the last ones — `qualityCheck`,
criterion 3, and the run this paragraph was checked against. Each is what its command printed
when it was run, and none has been retyped to agree with the others. That is the
measurement-neutrality question `doc:50-memory-and-evidence#corpus-figures` says to state
rather than assume: this record is neutral at no step, and the stamp on each fence is what
distinguishes them. The `documents`, `anchors`, `beans`, `graph edges`, `selectable` and
`bean ids` counts are unmoved throughout.

For completeness, the recorder at the OLD insertion point on the CLEAN tree also logs 0 under
both interpreters — the same number the old fences carry, arrived at by an instrument that
cannot see. That is the coincidence, stated as one.

**And the sites where a non-zero status is an answer on a RED run are not opted out, because
each is followed by the check that reports it.** That is a claim about paths, so it is
measured on a red corpus rather than read off the source: two untracked probe files, one bean
with unresolvable `parent` and `blocked_by` edges and one document with a key missing and two
references that resolve to nothing.

```
head:     this branch's working tree, plus two UNTRACKED probe files removed by the same
          script; `git status --porcelain` after the removal is printed and is empty
method:   the gate as it ships, run over a corpus with two planted defects
--- the planted corpus:
[...]/.beans/modus-9901--a-probe-bean-with-unresolvable-edges.md
[...]/documentation/98-a-probe-document.md

--- the run:
exit: 1
first stdout line: FAIL check 2  documentation/98-a-probe-document.md: key 'depends_on' appears 0 times, expected once
last stdout line:  docs-lint: 15 failure(s).
stdout FAIL check lines, by check:
   2 12
   2 2
   1 5
   2 6

--- every runtime-failure record the trap wrote, distinct:
FAIL check -  line 219: a command exited 1 and nothing checked it: 'n="$(keys "$f" | grep -cx "$k")"'
FAIL check -  line 301: a command exited 1 and nothing checked it: 'sort -u > "$TMP/declared.txt"' (pipeline exited 1 0 0, left to right)
FAIL check -  line 354: a command exited 1 and nothing checked it: 'target="$(ls .beans/"${BEAN_PREFIX}${rest}"*.md 2>/dev/null)"'
FAIL check -  line 354: a command exited 1 and nothing checked it: 'target="$(ls documentation/"$rest"*.md 2>/dev/null)"'
FAIL check -  line 369: a command exited 1 and nothing checked it: 'n="$(printf '%s' "$target" | grep -c .)"'
FAIL check -  line 545: a command exited 1 and nothing checked it: 'dn="$(ls .beans/"$dep"--*.md 2>/dev/null | grep -c .)"'
FAIL check -  line 564: a command exited 1 and nothing checked it: 'n="$(ls .beans/"$parent"--*.md 2>/dev/null | grep -c .)"'
--- firings: 8   distinct: 7

--- the checks that reported the same two defects on stdout:
FAIL check 2  documentation/98-a-probe-document.md: key 'depends_on' appears 0 times, expected once
FAIL check 2  documentation/98-a-probe-document.md: depends_on '' is not a flow list
FAIL check 5  documentation/98-a-probe-document.md: provides '#nowhere' but no heading declares <a id="nowhere">
FAIL check 6  documentation/98-a-probe-document.md: 'bean:9902' resolves to 0 files, expected exactly 1
FAIL check 6  documentation/98-a-probe-document.md: 'doc:97-not-a-document#nowhere' resolves to 0 files, expected exactly 1
FAIL check 12 .beans/modus-9901--a-probe-bean-with-unresolvable-edges.md: parent 'modus-9997' resolves to 0 bean files, expected exactly 1
FAIL check 12 .beans/modus-9901--a-probe-bean-with-unresolvable-edges.md: blocked_by 'modus-9998' resolves to 0 bean files, expected exactly 1

--- git status --porcelain after the probe corpus was removed:
 M tools/docs-lint.sh
--- (end)
```

Seven distinct sites fire, and on THIS corpus each is a line whose next few lines report the
same defect on stdout — `key 'depends_on' appears 0 times`, the check 6 record for a `doc:`
reference to a document that is not there, and `blocked_by 'modus-9998' resolves to 0 bean
files`; all three are in the fence above, where a fenced block keeps the reference itself from
resolving. The run is red either way; what the trap adds there is a second record for a defect
already named, which is noise on a red run and not a red run of its own.

**The sentence that followed here was the mistake this whole change turns on, and it is struck
rather than dropped.** It read: *every one of them is a line whose next few lines report the
same defect on stdout*, and it was then generalised from these seven sites to every site not
in the opt-out. It is **false at check 5's site**, which is one of the seven: line 301 fires
when a document declares **no anchor at all**, and check 5 reports only a **promised** anchor
that no heading declares. Those are different conditions, and one of them is legal. The
generalisation was an inference from what one planted corpus happened to exercise, offered
where a statement about the command's semantics was needed. Blockers 1 and 2 are what it cost,
and the rule the opt-out is derived from now is above.

**If one of those sites ever fires on a green corpus the gate goes red on a clean tree**,
which is the risk this audit exists to bound — and the CI run above is what that risk looks
like when the bound is taken on one interpreter only, as the two clean trees are what it looks
like when the bound is taken from one corpus only.

## What bounds the trap's EXTENT — the one-line disarm the suite could not see

Every plant this bean shipped with sat between lines 147 and 681 of an 848-line file, so
check 14, the record-file guard, `n_fail` and the counts line were covered by nothing. The
mutation that shows it is one line, `trap - ERR`, and it is **not a no-op**:

```
head:     688f3ba; the gate under probe is `git show 688f3ba:tools/docs-lint.sh`, copied to
          tools/.docs-lint-d1.sh and tools/.docs-lint-d2.sh, both deleted after
method:   one `false __probe_below_the_disarm__` planted after the `KINDS=` line in BOTH
          copies; d1 additionally has `trap - ERR` planted immediately BEFORE that line
interpreter: /bin/bash (GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25))
--- d2, trap ARMED:
diff:  742a743
       > false __probe_below_the_disarm__
exit:   1
stdout: docs-lint: 1 failure(s).
stderr: FAIL check -  line 743: a command exited 1 and nothing checked it: 'false __probe_below_the_disarm__'
--- d1, trap DISARMED one line above the plant:
diff:  741a742
       > trap - ERR
       742a744
       > false __probe_below_the_disarm__
exit:   0
stdout: docs-lint: OK — 19 documents, 111 anchors, 1737 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr: (empty)
--- (end)
```

**A plant per region is the wrong shape for the answer.** It is an enumeration of the places
somebody thought to look, which is what `bean:0123` learned bounds nothing — a lexical list of
`awk` bypass spellings that left twenty-one of twenty-two call sites unguarded and scored a
clean sheet. Regions are the same list with different words in it.

**What bounds it is behavioural, and it lives in the gate rather than in the suite.** At its
last statement before the count, `tools/docs-lint.sh` FIRES its own failure path and requires
the record:

```sh
{ false __docs_lint_armed_probe__; } 2>/dev/null
n_armed="$(absent_ok grep -cF '__docs_lint_armed_probe__' "$TMP/fails.txt")"
[ "$n_armed" = 1 ] || fail - "the failure path was not working at the end of the run: …"
```

### The reading this replaced, and why review was right to refuse it

The first version of this bound asked the shell what handler string it was holding and matched
a token in it:

```sh
case "$(trap -p ERR)" in
  *docs_lint_err*) ;;
  *) fail - "the ERR trap was not armed at the end of the run; …" ;;
esac
```

Its comment said "no list of spellings, which would fail open
(`doc:00-constitution#observed-failing`)". **A one-token list is a list**, and §9.1's
"enumerating the shapes a gate accepts fails open" binds it exactly as it bound the three
defensive-copy allowlists. Two ONE-EDIT escapes walked past it. Both were planted into a copy
of the gate immediately after the `KINDS=` line — the same anchor the disarm uses, below every
other plant — and run under both interpreters:

```
head:     0982b71; each row planted into a COPY at .scratch-probe/esc-<row>.sh, run, deleted
cmd:      [...]/escapes.sh <interpreter> [...]  (the plant/run/report harness)
--- interpreter: /bin/bash (bash 3.2.57(1)-release)
--- disarm  (planted 'trap - ERR'; copy differs on 1 line(s))  rc=1
    OK lines on stdout: 0
    FAIL check -  the failure path was not working at the end of the run: a top-level 'false' produced 0 record(s) in the failure file instead of exactly 1. [...]
    last stdout line: docs-lint: 1 failure(s).
--- retrap-token  (planted 'trap ': docs_lint_err' ERR'; copy differs on 1 line(s))  rc=1
    OK lines on stdout: 0
    FAIL check -  the failure path was not working at the end of the run: a top-level 'false' produced 0 record(s) in the failure file instead of exactly 1. [...]
    last stdout line: docs-lint: 1 failure(s).
--- redefine  (planted 'docs_lint_err() { :; }'; copy differs on 1 line(s))  rc=1
    OK lines on stdout: 0
    FAIL check -  the failure path was not working at the end of the run: a top-level 'false' produced 0 record(s) in the failure file instead of exactly 1. [...]
    last stdout line: docs-lint: 1 failure(s).
--- (end; the elisions above are the rest of one long record line, identical in all three rows)
```

The bash 5.3.9 run of the same harness is byte-identical in these columns except for the
interpreter banner. **That is the FIXED gate catching all three.** What says the reading did
not is the suite's own score with the reading restored in place of the live fire and
everything else left alone:

```
head:     0982b71 with the live fire replaced by the `case "$(trap -p ERR)"` reading
cmd:      /bin/bash tools/docs-lint-gate-test.sh
[...] the disarm rows still pass; the retrap and redefine rows do not, 8 rows × 2 interpreters
FAIL a gate with the ERR trap re-trapped to text containing the handler's name exits non-zero [bash 3.2.57(1)-release]
FAIL and names the broken failure path rather than reporting OK (retrap) [bash 3.2.57(1)-release]
FAIL a gate with the handler redefined to do nothing exits non-zero [bash 3.2.57(1)-release]
FAIL and names the broken failure path rather than reporting OK (redef) [bash 3.2.57(1)-release]
[...] the same four rows again under bash 5.3.9(1)-release
docs-lint-gate-test: 120 passed, 23 failed, over 2 bash major version(s).
```

`trap ': docs_lint_err' ERR` is the one whose *text* merely contains the token; the `case`
matches and the handler that runs records nothing. `docs_lint_err() { :; }` is the more
plausible accident of the two — `trap -p ERR` is byte-identical after it, so every artefact a
reader would consult says the mechanism is armed, and only the plant says otherwise. That is
§9.1's `import/no-cycle` row, in this file.

**Firing the path answers all three, and shapes nobody has named.** The question the records
depend on is whether a failing command at the top level of this file becomes a LINE OF
`$TMP/fails.txt`; the live fire asks that question rather than a proxy for it. The probe's own
record is filtered out of `n_fail` with `grep -cvF` rather than deleted from the file, so a
failure to remove it cannot lose a real record, and the marker is the only line that can carry
it — if a document ever does, the count reads 2 and the gate goes red rather than quiet.

`{ … } 2>/dev/null` is what keeps the probe silent on a clean run while the record still
reaches the file, and that is measured per interpreter by the suite rather than remembered: a
redirect on the SIMPLE COMMAND (`false __probe__ 2>/dev/null`) does **not** reach the handler
under either interpreter, and the group's does.

### Two shapes that look like escapes and are not

Recorded so nobody plants them expecting red. Same harness, same anchor, both interpreters:

```
head:     0982b71
--- subshell (planted '( trap - ERR; true )'; copy differs on 1 line(s))  rc=0
    stdout: docs-lint: OK — 19 documents, 111 anchors, 1738 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
    stderr lines: 0
--- in-function (planted '__probe_fn() { trap - ERR; }; __probe_fn'; copy differs on 1 line(s))  rc=0
    stdout: docs-lint: OK — 19 documents, 111 anchors, 1738 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
    stderr lines: 0
--- (end; identical under /opt/homebrew/bin/bash 5.3.9 — same two rows, rc=0, 0 stderr lines)
```

A disarm inside `( … )` leaves the parent's trap intact, and `trap - ERR` inside a FUNCTION
does not disarm the caller: bash restores the function-local ERR trap on return. **These are
green because the failure path still works, not because the check missed them** — which is the
difference between a behavioural bound and a token match, in two rows.

**And the eighth plant was stopping short while claiming to reach the end.** Its anchor was the
`# ------- done ---` banner, twenty-seven lines and two guards above the `n_fail` read, and
neither the suite nor the gate's own end-of-run check could tell. It is anchored now on the
statement immediately preceding the count. What remains below it is one `[ … ] || fail` list,
and an `||` list is a context the ERR trap is exempt from BY CONSTRUCTION — it can produce no
record to lose — so this is the last trap-visible statement in the file rather than the last
one somebody measured.

The residual is stated rather than implied: **a break that undoes itself before the last line
passes**, and that needs two edits.

## The detection regression this branch introduced, and the two counts it lost

Two defects found in review at `160e1ff`. The first is the one that matters most, because
merging as it stood would have made the gate **strictly worse than `main`**.

### `glob_lines` dropped what it could not stat, silently, at status 0

`glob_lines` replaced `ls <glob> 2>/dev/null` at **eight** sites — not six, as
`review_focus` said. The count is a command:

```
head:     277c4d5 (base), via `git show 277c4d5:tools/docs-lint.sh > [...]/docs-lint-base.sh`
cmd:      grep -n 'ls .*2>/dev/null' [...]/docs-lint-base.sh   (ugrep 7.8.4, interactive shell)
80:REF_FILES="$FM_FILES $(ls .beans/*.md 2>/dev/null) AGENTS.md CLAUDE.md .github/pull_request_template.md"
288:    doc) target="$(ls documentation/"$rest"*.md 2>/dev/null)" ;;
289:    adr) target="$(ls documentation/adr/"$rest"*.md 2>/dev/null)" ;;
290:    bean) target="$(ls .beans/"${BEAN_PREFIX}${rest}"*.md 2>/dev/null)" ;;
431:for f in $(ls .beans/*.md 2>/dev/null); do
460:    n="$(ls .beans/"$parent"--*.md 2>/dev/null | grep -c .)"
475:    dn="$(ls .beans/"$dep"--*.md 2>/dev/null | grep -c .)"
573:for f in $(ls .beans/*.md 2>/dev/null); do
--- count: 8
```

The helper's whole claim was that it **cannot fail**, and for the RETURN STATUS that holds:
review could not break it under either interpreter with closed stdout, 3000 arguments, zero
arguments, or names containing spaces, newlines or a leading `-`. It does not hold for the
**answer**. `[ -e "$p" ]` FOLLOWS SYMLINKS, so a path the shell globbed and the helper could
not stat was dropped — and a helper with no status has nothing left to say so with.

Planted `.beans/modus-0199--a-broken-symlink-bean.md -> /no/such/target`, one untracked
symlink, on an otherwise clean tree:

```
head:     160e1ff, working tree + the planted symlink; interpreter /opt/homebrew/bin/bash 5.3.9
cmd:      /opt/homebrew/bin/bash tools/docs-lint.sh
docs-lint: OK — 19 documents, 111 anchors, 1738 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
rc=0
--- the SAME tree and the SAME interpreter, against the base gate at .scratch-probe/docs-lint-base.sh:
cmd:      /opt/homebrew/bin/bash .scratch-probe/docs-lint-base.sh
awk: can't open file .beans/modus-0199--a-broken-symlink-bean.md
 source line number 1
FAIL check -  an analyser exited 2 and examined nothing; its last argument was '.beans/modus-0199--a-broken-symlink-bean.md'
[...] two more analyser records, at source line numbers 14 and 5, same argument
FAIL check 13 .beans/modus-0199--a-broken-symlink-bean.md: front-matter carries 0 '# modus-…' id markers, expected exactly 1
FAIL check 13 113 bean file(s) on disk but 112 parsed; a bean with no front-matter block is invisible to checks 12 and 13
docs-lint: 5 failure(s).
rc=1
```

**The counts line could not see it either**, and that is the sharper half. `n_bean_files` and
`n_beans` both derive from this one function's output, so a dropped member shrinks the count
AND the thing it is compared against, and the vacuity assertion reads a clean `112 / 112`. A
vacuity assertion over two figures with one source is not one.

`ls` is what kept the detection, and `:204`'s `FM_FILES="$(ls documentation/*.md
documentation/adr/*.md)"` still uses it — so `documentation/` never lost what the eight
`glob_lines` sites did. That asymmetry is why the regression was invisible from inside.

The fix is `[ -e "$p" ] || [ -L "$p" ]`: an unstattable name is emitted and then fails LOUDLY
at whatever tries to read it, which is what `ls` did. Same plant, same tree, fixed gate:

```
head:     0982b71 (the fix), working tree + the planted symlink
cmd:      /opt/homebrew/bin/bash tools/docs-lint.sh
[...] three analyser records and three trap records, one pair per site that opened it
FAIL check 13 .beans/modus-0199--a-broken-symlink-bean.md: front-matter carries 0 '# modus-…' id markers, expected exactly 1
FAIL check 13 113 bean file(s) on disk but 112 parsed; a bean with no front-matter block is invisible to checks 12 and 13
docs-lint: 8 failure(s).
rc=1
--- /bin/bash 3.2.57, same tree: docs-lint: 8 failure(s).  rc=1
--- the symlink removed, /opt/homebrew/bin/bash 5.3.9:
docs-lint: OK — 19 documents, 111 anchors, 1738 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
rc=0
```

Eight rather than base's five: the three extra are this branch's own trap records, one per
analyser that could not open the file — the same "one dead analyser is two records" trade the
awk wrapper already makes.

**What pins it is an assertion, because no test would have caught this one.** The suite now
cuts `glob_lines` out of the gate and runs it, per interpreter, against a REAL broken symlink
in the suite's own scratch directory — not in `.beans/`, so a crash cannot leave the corpus in
a state that turns everyone's gate red. Negative control, with `[ -e "$p" ]` restored and
nothing else changed:

```
cmd:      /bin/bash tools/docs-lint-gate-test.sh
FAIL and the helper emits it rather than dropping it, so something downstream can fail [bash 3.2.57(1)-release]
FAIL and the helper emits it rather than dropping it, so something downstream can fail [bash 5.3.9(1)-release]
docs-lint-gate-test: 141 passed, 2 failed, over 2 bash major version(s).
```

### Four counts were written AFTER the exit decision, into a file nothing reads again

`n_fail` was read, the `if` took the exit status, and only then did the `printf` on the OK
branch evaluate four `$( )` arguments. A "could not look" at one of those four was recorded by
`absent_ok` into `$TMP/fails.txt` — **and nothing reads that file again**. Their statuses
cannot reach the trap either, being command substitutions inside a `printf` argument list.

The tell was a count that disagreed with itself: **31 statements but 36 call sites**. The five
that collapse are two multi-line pipelines and — the four that matter — the `printf`'s four
arguments, which are exactly the four whose records are never read.

```
head:     160e1ff, with `rm -f "$TMP/provides.tsv"` planted immediately above the `n_fail` read
cmd:      /opt/homebrew/bin/bash .scratch-probe/b2-probe.sh
stdout: docs-lint: OK — 19 documents,  anchors, 1738 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr: grep: [...]/provides.tsv: No such file or directory
        FAIL check -  an opted-out command exited 2 and could not look, which is not 'no match': grep -c . [...]/provides.tsv
rc=0
```

`OK` at exit 0, a blank count between two commas, and an uncounted record. That blank is this
bean's own `before:` text one character over from check 11's `- introduced` (`bean:0051`).

All twelve counts are now taken above the `n_fail` read. At the fixed head, the same plant
moved above the four counts:

```
head:     0982b71, with `rm -f "$TMP/provides.tsv"` planted immediately above `n_documents=`
cmd:      /opt/homebrew/bin/bash .scratch-probe/esc-counts.sh
first stdout line: docs-lint: 2 failure(s).
OK lines on stdout: 0
stderr: FAIL check -  an opted-out command exited 2 and could not look, which is not 'no match': grep -c . [...]/provides.tsv
rc=1
--- identical under /bin/bash 3.2.57: 2 failure(s), 0 OK lines, rc=1
```

**Pinned structurally, not by a plant**, because a plant shows one of the four failing while
the property wanted is that no count can be computed there at all — including the thirteenth
somebody adds next year. The suite reads the `printf` whole and requires every argument to be
a plain variable, and requires `n_edges` to be read above `n_fail`. Negative control, with one
count moved back into the `printf` and nothing else changed:

```
cmd:      /bin/bash tools/docs-lint-gate-test.sh
FAIL and not one of its arguments is a command substitution
docs-lint-gate-test: 145 passed, 1 failed, over 2 bash major version(s).
```

### The site the derivation sent to the opt-out and nobody read the answer of

Check 11's `sed -n '/^## Amendments/,$p' "$f" | absent_ok grep '^### '`. "Nothing found" here
means a bean gained an `## Amendments` section with no amendment heading: `n_amend` and every
`n_k` become 0, `[ 0 != 0 ]` is false, and no `fail 11` fires. Pre-existing — it was `|| :` on
base, so this branch neither introduced nor widened it — and **arming that `grep` would be the
wrong fix**: "no `### ` heading" is a legal answer about the input and the opt-out is correctly
derived. What was missing is a check that READS the answer.

```
head:     160e1ff, with `## Amendments` plus one line of bare prose appended to
          .beans/modus-0001--foundation-documentation-package.md
cmd:      /opt/homebrew/bin/bash tools/docs-lint.sh
docs-lint: OK — 19 documents, 111 anchors, 1738 references, 112 beans, [...] 0 unnumbered.
rc=0
--- POSITIVE CONTROL, the same bean with `### not-a-valid-heading` appended as well, so the
--- loop is proved live on this file rather than assumed:
FAIL check 11 .beans/modus-0001--...: amendment heading '### not-a-valid-heading' is not '### YYYY-MM-DD · bean:NNNN'
FAIL check 11 .beans/modus-0001--...: 1 amendment(s) but 0 '**Claimed:**' line(s) (adr:0005#amendments)
[...] the same row for '**Found:**' and '**Evidence:**'
docs-lint: 4 failure(s).
rc=1
--- both plants reverted with `git checkout -- .beans/modus-0001--...`; `git status --short`
--- then lists only the two tool files.
```

A `fail 11` is now beside it. The corpus is clean for it — two beans carry `## Amendments`,
`modus-0102` with 1 heading and `modus-0049` with 11 — so it is a gate over the tree as it is,
not a change to it. The derivation comment said `.beans.yml`'s `prefix:` was "the only such
site left in this file"; that sentence had **two** outcomes where the rule has three, and it
now says so. A site the derivation sends to the opt-out can still be a site whose result
nobody checks, and those are two questions.

## Both interpreters, in the suite — not in a transcript

`tools/docs-lint-gate-test.sh` ran `SHELL_BIN="${BASH:-/bin/bash}"` — one interpreter,
whichever invoked the file. That is 3.2.57 here and bash 5 on the runner, and **nothing in the
repository ever ran both**, while this change's `after:` clause claimed the opt-out had been
read off a green run under both. The claim was true of two hand-run transcripts pasted into
this bean and false of anything that re-runs.

The suite now discovers every distinct bash MAJOR version on the host — the pinned `$BASH`
first, then a candidate list of paths — and repeats every gate run and every run assertion
once per interpreter, tagging each assertion with the version that produced it. The candidate
list is a list of PATHS, so a bash it does not name costs coverage and never correctness; what
would fail open is asserting a bash-5-only property while running 3.2, and the banner refuses
to let that pass in silence:

```
head:     this branch's working tree
cmd:      /bin/bash tools/docs-lint-gate-test.sh
docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)
docs-lint-gate-test: analyser awk — awk version 20200816
docs-lint-gate-test: exercising /bin/bash (bash 3.2.57(1)-release)
docs-lint-gate-test: exercising /opt/homebrew/bin/bash (bash 5.3.9(1)-release)
[...] the ten-shape ERR-trap table under each interpreter, quoted in full above; then every
      plant assertion once, and every run assertion twice — once per interpreter, each row
      tagged `[bash 3.2.57(1)-release]` or `[bash 5.3.9(1)-release]`
docs-lint-gate-test: 110 passed, 0 failed, over 2 bash major version(s).
exit:     0
```

On a host with one bash major version — which the CI runner is — the suite prints, in five
lines it cannot be read past, that it exercised one, and names the claims that are therefore
unverified there: which pipeline shapes reach the trap, which element `$BASH_COMMAND` names,
and what `$LINENO` holds inside a loop body. The cost is one extra set of six backgrounded
gate runs per interpreter; on the runner there is one interpreter and the cost is nothing.

## What changed, and why this mechanism and not the other three

**An `ERR` trap that records through `fail`, plus one named opt-out.** `tools/docs-lint.sh`
now arms `trap 'docs_lint_err $? "$BASH_COMMAND" "$LINENO" "${PIPESTATUS[*]}"' ERR` as soon
as the file every record is written into exists, and the handler appends one line through
the same `fail` every check uses. The run continues; the exit status changes, because
`docs-lint: N failure(s).` is a count of that file's lines. Commands whose non-zero status is
an **answer** rather than a failure — `grep` reporting no match, `grep -c` restating a count
of 0 — go through an `absent_ok` wrapper that tolerates status 1 and nothing above it, and
names the command when it refuses. **Which commands those are is settled by the rule stated
above and not by a list of sites a run was seen firing at**; the list version was wrong three
times — twice on trees that break no rule, once on the runner.

**Plus two things the first version did not have.** `glob_lines` replaces `ls <glob>
2>/dev/null` where "no such file" is an answer, because `ls` says it with a status that
differs by implementation and so cannot be tolerated by a single number — and it emits a name
it cannot STAT rather than dropping it, which is the correction review forced and which the
section on that regression records. And at its last trap-visible statement the gate FIRES its
own failure path and requires the record, which is what bounds the RANGE that path works over
rather than the shapes it catches. A reading of `trap -p ERR` stood there first and was
replaced: it matched one token, and a one-token list is a list.

Three alternatives were weighed, and the reasons are measurements.

**`set -e` abandons the run.** errexit exits at the first non-zero status, so a gate whose
whole purpose is to report every defect in one pass would report one and stop — and stop
before printing the counts line that `tools/docs-lint.sh` calls its own vacuity assertion.
It also buys nothing in safety: errexit exits and the `ERR` trap fires under exactly the
same conditions, so the audit below would have been needed either way.

**Shadowing the commands that matter, as `bean:0123` shadowed `awk`, cannot reach five of
the eight rows.** That trick worked because `awk` is one name, with twenty-two call sites,
and any non-zero status from it is a failure. Of the eight rows still open here, only three
— `false`, `/usr/bin/false` and `cat` on a missing file — are an external command failing at
all. A failed `cd` is a builtin, an unbound variable expanded inside `$( )` or inside a
pipeline element is the shell, and a bash syntax error inside `$( )` is the parser. No
shadow of any command name reaches them. And for the two commands a shadow would most want —
`grep` and `ls` — a non-zero status is frequently the answer the gate asked for, so the
shadow would have to carry a per-command table of which statuses mean *no* and which mean
*broken*, which is an enumeration, which fails open (`doc:00-constitution#observed-failing`).

**A per-site `rc=$?`, which is what check 14 does once and what `bean:0123` replaced for
`awk`, fails open by omission.** It cannot be written after a command that is a pipeline
element or that sits inside `$( )` — where `bean:0123` measured most analyser sites to be —
and every line added to the file afterwards is unguarded with nothing to say so. The trap
covers the lines nobody thought about, which is the half of the failure path that matters.

**Not `set -E` (errtrace), and that is the one place this mechanism is deliberately
narrower than it could be.** Without errtrace the trap is not inherited by functions,
subshells or command substitutions, so a failure inside one is recorded once, at the
enclosing statement whose status it makes non-zero. With errtrace the same failure records
three and four times, at a depth that is bash's business — and `bean:0123` has already had
one assertion fail on the runner because it required a number the interpreter chose. What
errtrace would buy is named under "what it does not catch" and measured there.

**The record is flattened to one line, and that is not cosmetic.** `docs-lint: N
failure(s).` counts the LINES of the record file. `$BASH_COMMAND` for check 12's analyser
call is a nineteen-line awk program, and the first working version of this change reported
`docs-lint: 20 failure(s).` for two records. The handler now replaces newlines with spaces
and clips at 120 characters.

## What it catches, and what it does not

**Catches**, and each is observed below rather than argued: any command whose non-zero status
is not consumed by an enclosing construct — in the top-level shell and in every `for`, `while`
and `if` body that runs there. That is a plain `false` that writes nothing at all, a missing
file, a failed `cd`, a pipeline that fails at any element under `pipefail`, a command
substitution that fails, an unbound variable expanded inside `$( )` or inside a pipeline
element, and a bash syntax error inside `$( )`. It also adds a second record at an analyser's
call site, which `bean:0123`'s wrapper cannot name; a `grep` at an opted-out site that could
not look, which `absent_ok` names because the trap there can only name a `return`; and its own
absence, if anything below the arming disarms it.

**Does not catch**, named because the assertions would otherwise imply it:

- **A failure whose status an enclosing subshell discards.** In this file that is a command
  inside a `while read` body whose enclosing loop is a pipeline element: the loop runs in a
  subshell of its own and its status is the last command's. The loops of that shape are check
  7's over `read_when` and check 2's over `provides`, both fed by `printf`; check 5's two, fed
  by `comm`; and check 10's, fed by a `grep` — named rather than counted, since the count moves
  with the next check anyone writes. **Two of the five are `comm`-fed, and an earlier revision
  of this bullet and of the gate's own comment said `printf … | while read` as though all of
  them were.** Measured as the fourth plant point of the table below, where every row reaches
  `OK` at exit 0 exactly as it did before the change. `set -E` closes it, at the price stated
  above; that price is the one thing in this work item a reviewer could reasonably reverse.
- **Under the pinned bash 3.2.57 the residual is bigger than that, and this bean stated the
  two halves separately without ever joining them.** 3.2 does not reach the trap AT ALL for a
  pipeline whose last element is a compound command, so what is unseen there is not the loop
  body but the WHOLE PIPELINE, its final command included. Under bash 5 the pipeline is seen
  and the body is not. The eight shapes, distinguished only by their last element:

  ```
  head:     this branch's working tree
  cmd:      /bin/bash <scratch>/shapes.sh   and   /opt/homebrew/bin/bash <scratch>/shapes.sh
            — one file, `set -uo pipefail`, a counting ERR trap, ten pipelines `false | <shape>`
  last element of the pipeline        3.2.57  5.3.9   $BASH_COMMAND under 5.3.9
  while                                0       1      false   (the FIRST element)
  for                                  0       1      false
  until                                0       1      false
  if                                   0       1      false
  case                                 0       1      false
  brace group  { …; }                  0       1      false
  redirected group  { …; } > /dev/null 0       1      false
  subshell  ( … )                      0       2      ( : )
  simple command                       1       1      cat     (the LAST element)
  function                             1       1      fn      (the LAST element)
  ```

  Three consequences this bean did not carry. The subshell case fires **twice** under bash 5.
  `$BASH_COMMAND` holds the **first** element under bash 5 for every compound shape, where
  `tools/docs-lint.sh`'s comment said "the LAST element" unconditionally; the comment now says
  which, and why the statuses beside it are what the record is read on. And the divergence is
  not a curiosity — one line, one tree, two verdicts:

  ```
  head:     this branch's working tree; the gate under probe is a COPY at
            tools/.docs-lint-last.sh, deleted after; `git status --porcelain` shows it gone
  plant:    `      false __probe_last__` inserted as the LAST command of check 7's `while`
            body, whose enclosing loop is the last element of `printf … | while read`
  diff:     335a336
            >       false __probe_last__
  --- /bin/bash 3.2.57
  exit: 0
  docs-lint: OK — 19 documents, 111 anchors, 1737 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
  records naming __probe_last__: 0
  --- /opt/homebrew/bin/bash 5.3.9
  exit: 1
  docs-lint: 16 failure(s).
  records: 16, all one line:
  FAIL check -  line 323: a command exited 1 and nothing checked it: 'printf '%s\n' "$rw_items"' (pipeline exited 0 1, left to right)
  ```

  Note what the bash 5 record NAMES: `printf`, the element that succeeded, with `0 1` beside it
  saying the failure was at the other end. That is the `$BASH_COMMAND` row of the table above,
  in the gate rather than in a probe. It is a difference in what the trap SEES, not in
  what the gate does, and it means the audit is only as complete as the set of interpreters it
  was run under. `tools/docs-lint-gate-test.sh` now prints this table under every interpreter
  it finds, on every run, so the premise is measured rather than remembered.
- **A failure in a tested context** — `if cmd`, `while cmd`, `cmd && …`, `cmd || …`. The
  status is consumed there, which is the same rule errexit uses, so a `grep` that could not
  look, at a site where the gate reads non-zero as "no match", is still read as an answer.
  The sites are enumerated by the red-corpus audit below rather than by reading.
- **A command that runs, exits 0, and examines nothing.** That is `bean:0126`, and no trap
  can see it.
- **`ROOT` coming out empty.** `cd ""` exits **0** under `/bin/bash` 3.2.57 and **1** under
  bash 5.3.9, which writes `cd: null directory` — both measured here, and the one-interpreter
  version of this bullet stated the 3.2 half as a property of `cd`. So `cd "$ROOT" || exit 2`
  would not fire on the pinned interpreter if `ROOT="$(cd "$(dirname "$0")/.." && pwd)"`
  produced nothing, and would fire on the runner. The unreachability holds in practice — it
  cannot be empty while the directory the running script sits in exists — but it holds *in
  practice* and not by construction, which is a weaker statement than the one this bullet made.

  ```
  head:     this branch's working tree
  cmd:      /bin/bash -c 'cd ""; echo "3.2.57 cd empty -> $?"'
            /opt/homebrew/bin/bash -c 'cd ""; echo "5.3.9 cd empty -> $?"'
  observed: 3.2.57 cd empty -> 0
            /opt/homebrew/bin/bash: line 1: cd: null directory
            5.3.9 cd empty -> 1
  ```
- **The diagnosis is on stderr only.** The trap's record does not reach stdout, for the
  reason `bean:0123`'s wrapper does not: a call site inside `$( )` has its stdout captured,
  and `bean:0123` measured what happens when it is not redirected — 908 stdout lines, 907 of
  them `FAIL`, as check 2 read the guard's own words back as front-matter keys. So
  `./gradlew docsLint | tee log` leaves a count with no reason beside it. Gradle and CI
  capture both streams. Inherited, not introduced.

## What was measured against each criterion

The criteria are the four already standing in this bean's `## Success criteria` table and no
wording of any of them is changed here: a close that rewrites its criteria is
indistinguishable from a close that met them (`bean:0113`). This bean stays `in-progress`
through its own pull request (`doc:00-constitution#bean-lifecycle`); closing it is a separate
change.

### Criterion 1 · the boundary rows, at four plant points

Thirteen rows at two plant points and eight rows at two more, against the gate as it ships.
`a1` is `bean:0118`'s own anchor, above everything; `a2` is the first statement below the
mechanism; `a3` is inside check 2's per-document loop, mid-file; `a4` is the first command of
a `printf … | while read` body, which runs in a subshell of its own. `records` counts the
trap's lines and `distinct` counts them after `sort -u`, because a plant inside a loop fires
once per document and that figure is the corpus's
(`doc:50-memory-and-evidence#corpus-figures`).

```
head:     688f3ba, working tree; the gate under probe is tools/docs-lint.sh itself
cmd:      tools/docs-lint-boundary-probe.sh /bin/bash — the rig, committed, so this
          table is re-derivable rather than re-created (doc:50-memory-and-evidence#evidence-kinds)
interpreter:      /bin/bash (GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25))
method:   each row planted into a COPY at tools/.docs-lint-bp-<pid>-N.sh, the copy run,
          the copy deleted; the rows of one plant point run concurrently; no tracked file
          written (bean:0102). The anchor and the planted line reach awk through the
          ENVIRONMENT and not `-v`, which processes escapes: the a4 anchor contains a
          backslash and an n, and with `-v` it silently matched nothing — which is what
          the `lines added` column is for


=== plant point a1, immediately after: set -uo pipefail
occurrences of that anchor in the gate: 1
planted line                                   exit OK?  records distinct lines added
echo "$__probe_unbound_top"                    1    no   0       0        1
;;                                             2    no   0       0        1
echo x | ;;                                    2    no   0       0        1
awk "BEGIN { x = = 1 }" /dev/null | cat        0    yes  0       0        1
probe_z="$(awk "BEGIN { x = = 1 }" /dev/null)" 0    yes  0       0        1
false                                          0    yes  0       0        1
/usr/bin/false                                 0    yes  0       0        1
cat /no/such/file/anywhere                     0    yes  0       0        1
cd /no/such/dir/anywhere                       0    yes  0       0        1
false | cat                                    0    yes  0       0        1
probe_x="$(echo "$__probe_unbound_sub")"       0    yes  0       0        1
echo "$__probe_unbound_pipe" | cat             0    yes  0       0        1
probe_y="$( ;; )"                              0    yes  0       0        1

=== plant point a2, immediately after: FM_FILES="$(ls documentation/*.md documentation/adr/*.md)"
occurrences of that anchor in the gate: 1
planted line                                   exit OK?  records distinct lines added
echo "$__probe_unbound_top"                    1    no   0       0        1
;;                                             2    no   0       0        1
echo x | ;;                                    2    no   0       0        1
awk "BEGIN { x = = 1 }" /dev/null | cat        1    no   2       2        1
probe_z="$(awk "BEGIN { x = = 1 }" /dev/null)" 1    no   2       2        1
false                                          1    no   1       1        1
/usr/bin/false                                 1    no   1       1        1
cat /no/such/file/anywhere                     1    no   1       1        1
cd /no/such/dir/anywhere                       1    no   1       1        1
false | cat                                    1    no   1       1        1
probe_x="$(echo "$__probe_unbound_sub")"       1    no   1       1        1
echo "$__probe_unbound_pipe" | cat             2    no   1322    59       1
probe_y="$( ;; )"                              1    no   1       1        1

=== plant point a3, immediately after:   id="$(field "$f" S id)"
occurrences of that anchor in the gate: 1
planted line                                   exit OK?  records distinct lines added
false                                          1    no   19      1        1
/usr/bin/false                                 1    no   19      1        1
cat /no/such/file/anywhere                     1    no   19      1        1
cd /no/such/dir/anywhere                       1    no   19      1        1
false | cat                                    1    no   19      1        1
probe_x="$(echo "$__probe_unbound_sub")"       1    no   19      1        1
echo "$__probe_unbound_pipe" | cat             2    no   1289    55       1
probe_y="$( ;; )"                              1    no   19      1        1

=== plant point a4, immediately after:     printf '%s\n' "$prov" | while IFS= read -r a; do
occurrences of that anchor in the gate: 1
planted line                                   exit OK?  records distinct lines added
false                                          0    yes  0       0        1
/usr/bin/false                                 0    yes  0       0        1
cat /no/such/file/anywhere                     0    yes  0       0        1
cd /no/such/dir/anywhere                       0    yes  0       0        1
false | cat                                    0    yes  0       0        1
probe_x="$(echo "$__probe_unbound_sub")"       0    yes  0       0        1
echo "$__probe_unbound_pipe" | cat             0    yes  0       0        1
probe_y="$( ;; )"                              0    yes  0       0        1

--- git status --porcelain after every probe was removed:
 M .beans/modus-0118--docs-lint-reports-ok-through-almost-every-runtime-failure.md
 M .beans/modus-0124--the-non-analyser-fail-open-boundary-in-docs-lint.md
 M build.gradle.kts
 M tools/docs-lint-gate-test.sh
 M tools/docs-lint.sh
?? tools/docs-lint-boundary-probe.sh
--- (end)
```

**Every one of the eight rows exits non-zero at `a2` and at `a3`, and none of them does at
`a1` or `a4`.** The two that do not are not two failures of the mechanism but one, stated
twice:

- **`a1` is above the trap**, which cannot be armed before the file it records into exists.
  What sits above it is `ROOT=`, `cd "$ROOT" || exit 2`, `TMP=`, the EXIT trap that removes
  `$TMP`, and `: > "$TMP/fails.txt"`. `TMP=` and `: >` now carry `|| exit 2` of their own,
  which is the scratch-directory row below; `cd` already carried one; `ROOT=` is the empty
  case named above; and a `trap` builtin setting a handler does not fail.
- **`a4` is a subshell whose status is discarded**, which is the errtrace residual named
  above. This is the row where a reviewer could reasonably ask for `set -E` and the price
  paid instead.

**And the same table under bash 5.3.9**, which "Not verified here" carried as unmeasured until
the rig was committed.

**The block that stood here was hand-edited and its elision said something the measurement
denies.** It read `[...] a1 identical to the 3.2.57 table` while listing three differing rows,
and the prose above it said "every row identical … except two". The rig says **four**: at every
one of the four plant points, `probe_y="$( ;; )"` differs — `a1` included, which the elision
explicitly claimed was identical. `doc:50-memory-and-evidence#capturing` requires a block a
reader cannot check to be recaptured whole rather than patched, so both runs were re-taken and
what is pasted below is a `diff` of the two redirect files, produced by a command rather than
composed:

```
head:     0982b71, working tree; the gate under probe is tools/docs-lint.sh itself
cmd:      tools/docs-lint-boundary-probe.sh /bin/bash                > [...]/bp-b3.txt 2>&1
          tools/docs-lint-boundary-probe.sh /opt/homebrew/bin/bash   > [...]/bp-b5.txt 2>&1
          diff [...]/bp-b3.txt [...]/bp-b5.txt        ('<' is 3.2.57, '>' is 5.3.9)
3,4c3,4
< interpreter:      /bin/bash (GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25))
< method:   each row planted into a COPY at tools/.docs-lint-bp-28210-N.sh, the copy run,
---
> interpreter:      /opt/homebrew/bin/bash (GNU bash, version 5.3.9(1)-release (aarch64-apple-darwin25.1.0))
> method:   each row planted into a COPY at tools/.docs-lint-bp-13726-N.sh, the copy run,
22c22
< probe_y="$( ;; )"                              0    yes  0       0        1
---
> probe_y="$( ;; )"                              2    no   0       0        1
38,39c38,39
< echo "$__probe_unbound_pipe" | cat             2    no   1323    59       1
< probe_y="$( ;; )"                              1    no   1       1        1
---
> echo "$__probe_unbound_pipe" | cat             2    no   1375    66       1
> probe_y="$( ;; )"                              2    no   0       0        1
50,51c50,51
< echo "$__probe_unbound_pipe" | cat             2    no   1285    55       1
< probe_y="$( ;; )"                              1    no   19      1        1
---
> echo "$__probe_unbound_pipe" | cat             2    no   1338    62       1
> probe_y="$( ;; )"                              2    no   0       0        1
63c63
< probe_y="$( ;; )"                              0    yes  0       0        1
---
> probe_y="$( ;; )"                              2    no   0       0        1
70,77d69
< ?? tools/.docs-lint-bp-13726-1.sh
[...] seven more `?? tools/.docs-lint-bp-13726-N.sh` lines, N = 2 to 8
--- (end of diff; line 22 is a1, 39 is a2, 51 is a3, 63 is a4)
```

Lines 22, 39, 51 and 63 are `probe_y="$( ;; )"` at `a1`, `a2`, `a3` and `a4`: **all four**, and
`a1` is `0 yes` under 3.2.57 against `2 no` under 5.3.9, which is the row the old elision said
was identical. Lines 38 and 50 are the record COUNTS of the `echo "$__probe_unbound_pipe" | cat`
row, which are figures of the corpus and move with it
(`doc:50-memory-and-evidence#corpus-figures`); those two rows agree on exit status and on
whether the run reached `OK`, and differ only in how far each got before its scratch directory
went.

**Lines 70–77 are an artefact of how these two runs were taken and not a fact about the gate.**
The two probes were run CONCURRENTLY, so the 3.2.57 run's closing `git status --porcelain`
observed the 5.3.9 run's copies mid-flight. Both runs removed their own; `git status --porcelain`
immediately afterwards lists only the three files this change edits, and `ls -a tools/` matches
`docs-lint-bp|docs-lint-probe|docs-lint-dbg` zero times. Run them one after the other and that
hunk is not there — recorded because a reader would otherwise read it as the rig leaking.

This block strengthens the conclusion below rather than weakening it, which is exactly why it
had to be corrected: an elision that overstates agreement between two interpreters is an
elision in favour of the argument it appears in.

**A bash syntax error inside `$( )` is FAIL-CLOSED under bash 5 and is not under 3.2.57**, and
at `a4` that is the one row of the residual bash 5 closes on its own: the plant that reaches
`docs-lint: OK` at exit 0 on the pinned interpreter stops the run at exit 2 on the runner's.
It is recorded as a difference, not as coverage — the residual is what the PINNED interpreter
does, because that is what `build.gradle.kts` runs the gate under.

The scratch-directory row is not in `bean:0118`'s table and is new here. It is the same class
— a runtime failure the gate reports `OK` through — and it is the sharpest instance in the
file, because the record file is what the exit status is computed from:

```
head:     this branch's working tree; `main` is `git show 277c4d5:tools/docs-lint.sh`
method:   mktemp pointed at a directory that does not exist, in a COPY of each gate
=== main  ([...]/gate-277c4d5.sh)
--- diff:
37c37
< TMP="$(mktemp -d)"
---
> TMP="$(mktemp -d /no/such/dir/anywhere/XXXXXX)"
--- exit: 0
stdout lines: 288   of them 'FAIL check': 287
last stdout line: docs-lint: OK — 19 documents,  anchors,  references,  beans,  graph edges,  selectable,  bean ids,  introduced,  on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr lines: 1979   first: mktemp: mkdtemp failed on /no/such/dir/anywhere/bks6tv: No such file or directory

=== shipped  ([...]/tools/docs-lint.sh)
--- diff:
43c43
< TMP="$(mktemp -d)" || exit 2
---
> TMP="$(mktemp -d /no/such/dir/anywhere/XXXXXX)" || exit 2
--- exit: 2
stdout lines: 0   of them 'FAIL check': 0
last stdout line: 
stderr lines: 1   first: mktemp: mkdtemp failed on /no/such/dir/anywhere/e0edac: No such file or directory

=== git status --porcelain:
 M tools/docs-lint.sh
=== (end)
```

And the analyser row, which `bean:0123` closed, now carries a second record naming the call
site that `bean:0123`'s wrapper cannot name. One dead analyser, two records, both true. That
capture is not repeated here: it is `tools/docs-lint-gate-test.sh`'s first plant, which runs
on every build and prints the mutated run's stderr verbatim, and it is quoted under criterion
4 below from a run of the suite rather than from a hand-driven copy. A capture that a
committed harness re-takes on every run belongs where the harness prints it, and a second
transcript of it here would be the one that goes stale (`doc:05-authoring-for-agents#one-fact-one-place`).

### Criterion 2 · the harness produces a red run and a green run on the same tree

The four-plant-point table under criterion 1 is both halves, from one harness invocation on
one tree: the `a1` rows reach
`docs-lint: OK` at exit 0 and the `a2` rows do not, and `a4`'s eight rows reach it too. A
harness that had stopped planting would report every row `exit 0 / OK`, and `a2` and `a3` say
it did not stop. The second, independent pair is `tools/docs-lint-gate-test.sh`'s own control,
which runs an unmutated copy of the gate at the same path as every mutant and requires it to
exit 0, print the `OK` line and write nothing at all to stderr — quoted whole under criterion
4 below. The harness is `tools/docs-lint-boundary-probe.sh` and is committed, so both halves
re-run from the repository rather than from a scratch directory that no longer exists.

The third pair is the audit and its control, in the audit section above: the same recording
trap, at the same insertion point, logs nothing on the clean tree and logs exactly one firing —
naming blocker 1 — on a tree carrying one legal-tree defect. An instrument observed silent and
never observed firing measures nothing (`doc:50-memory-and-evidence#evidence-kinds`), and that
is what the pair of "after" audits this change shipped with amounted to.

### Criterion 3 · silent on the unmodified tree, byte-identical stdout

The control is `tools/docs-lint.sh` **as `main` has it**, run on **this** tree — not the
capture taken on `main`'s tree, whose corpus differs by this bean's own text
(`doc:50-memory-and-evidence#corpus-figures`) — and now under both interpreters, since the
opt-out this change re-derives is exercised by every check on a green run and 3.2.57 and 5.3.9
do not reach the trap at the same places.

Re-taken at `0982b71`, because the gate changed in review: four counts moved above the exit
decision, a live fire was added at the end, and check 11 gained a `fail`. All three are
silent on a tree that breaks no rule, and `cmp` is what says so.

```
head:     0982b71ae3d229dc2c25ed272df446e7440210ec plus the working-tree edits
method:   main's gate at 277c4d5 copied to tools/.docs-lint-baseline.sh and run there,
          then the gate this ships; `cmp` on the two stdouts, under each interpreter.
          Copy deleted after
before:   tools/docs-lint.sh at 277c4d5
after:    tools/docs-lint.sh in the working tree

=== /bin/bash (GNU bash, version 3.2.57(1)-release)
--- before: exit 0, stderr bytes 0
docs-lint: OK — 19 documents, 111 anchors, 1740 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
--- after:  exit 0, stderr bytes 0
docs-lint: OK — 19 documents, 111 anchors, 1740 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
--- cmp before.out after.out: exit 0

=== /opt/homebrew/bin/bash (GNU bash, version 5.3.9(1)-release)
--- before: exit 0, stderr bytes 0
docs-lint: OK — 19 documents, 111 anchors, 1740 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
--- after:  exit 0, stderr bytes 0
docs-lint: OK — 19 documents, 111 anchors, 1740 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
--- cmp before.out after.out: exit 0

--- git status --porcelain:
 M .beans/modus-0124--the-non-analyser-fail-open-boundary-in-docs-lint.md
 M tools/docs-lint-gate-test.sh
 M tools/docs-lint.sh
?? .scratch-probe/
--- (end)
```

**`stderr bytes 0` is the half the live fire could have broken and did not.** The probe is a
`false` at the top level of the gate, and its record goes through the same `fail` every check
uses — which `tee`s to the handler's stdout, redirected to stderr. `{ … } 2>/dev/null` is what
keeps that off a clean run, and the four zeros above are the measurement, not the intention.
`1740` rather than `1738` is this bean's own text: it gained references while being written,
which is why the control is `main`'s gate on THIS tree and never a capture taken on `main`'s
(`doc:50-memory-and-evidence#corpus-figures`).

### Criterion 4 · the proof runs in `qualityCheck`, and `qualityCheck` is green

`tools/docs-lint-gate-test.sh` gains four plants and keeps the one it had, and repeats every
run assertion once per bash major version on the host. It is registered as `docsLintGateTest`
and is already a `qualityCheck` dependency; the only change in `build.gradle.kts` is the
comment above that registration, which said the suite runs the gate twice.

```
head:     688f3ba, working tree
cmd:      /bin/bash tools/docs-lint-gate-test.sh > [...]/gate-test-final.txt 2>&1
          (the redirect is 220 lines; every line below is from it)
docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)
docs-lint-gate-test: analyser awk — awk version 20200816
docs-lint-gate-test: exercising /bin/bash (bash 3.2.57(1)-release)
docs-lint-gate-test: exercising /opt/homebrew/bin/bash (bash 5.3.9(1)-release)

--- what an ERR trap can see, per interpreter
     /bin/bash (bash 3.2.57(1)-release) — firings per shape
       while             	0
       for               	0
       until             	0
       if                	0
       case              	0
       brace group       	0
       redirected group  	0
       subshell          	0
       simple command    	1
       function          	1
ok   a pipeline ending in a simple command reaches the trap under bash 3.2.57(1)-release
ok   and one ending in a function does too, under bash 3.2.57(1)-release
     /opt/homebrew/bin/bash (bash 5.3.9(1)-release) — firings per shape
       while             	1
       for               	1
       until             	1
       if                	1
       case              	1
       brace group       	1
       redirected group  	1
       subshell          	2
       simple command    	1
       function          	1
ok   a pipeline ending in a simple command reaches the trap under bash 5.3.9(1)-release
ok   and one ending in a function does too, under bash 5.3.9(1)-release

--- the plant: check 12's acyclicity analyser, destroyed
ok   the mutation site occurs exactly once in the gate
ok   the copy differs from the gate on exactly one line (one '<', one '>')
ok   and the line it differs on is the planted syntax error
ok   the control copy is identical to the gate

--- the second plant: eight runtime failures that are not an analyser
ok   the plant point for a silent non-zero exit occurs exactly once in the gate
ok   and a silent non-zero exit is planted exactly once in the copy
ok   and a silent non-zero exit's marker occurs nowhere in the gate itself
ok   the plant point for a grep that could not look, at an opted-out site occurs exactly once in the gate
ok   and a grep that could not look, at an opted-out site is planted exactly once in the copy
ok   and a grep that could not look, at an opted-out site's marker occurs nowhere in the gate itself
ok   the plant point for a missing file occurs exactly once in the gate
ok   and a missing file is planted exactly once in the copy
ok   and a missing file's marker occurs nowhere in the gate itself
ok   the plant point for a failed pipeline element occurs exactly once in the gate
ok   and a failed pipeline element is planted exactly once in the copy
ok   and a failed pipeline element's marker occurs nowhere in the gate itself
ok   the plant point for an unbound variable inside $( ) occurs exactly once in the gate
ok   and an unbound variable inside $( ) is planted exactly once in the copy
ok   and an unbound variable inside $( )'s marker occurs nowhere in the gate itself
ok   the plant point for a failed cd occurs exactly once in the gate
ok   and a failed cd is planted exactly once in the copy
ok   and a failed cd's marker occurs nowhere in the gate itself
ok   the plant point for a silent non-zero exit in check 14's preamble occurs exactly once in the gate
ok   and a silent non-zero exit in check 14's preamble is planted exactly once in the copy
ok   and a silent non-zero exit in check 14's preamble's marker occurs nowhere in the gate itself
ok   the plant point for a missing file at the last statement before the count occurs exactly once in the gate
ok   and a missing file at the last statement before the count is planted exactly once in the copy
ok   and a missing file at the last statement before the count's marker occurs nowhere in the gate itself
ok   eight points were planted, one line each

--- the third plant: the gate's own scratch directory, which every record is written into
ok   the scratch-directory line occurs exactly once in the gate
ok   and the copy differs from the gate on exactly one line (one '<', one '>')

--- the fourth plant: the scratch directory removed MID-RUN, which one row of the boundary does
ok   the plant point for the vanishing record file occurs exactly once in the gate
ok   and the copy differs from the gate on exactly one line (one '>')

--- the fifth plant: the ERR trap disarmed mid-file, which nothing below it can see
ok   the plant point for the disarmed trap occurs exactly once in the gate
ok   and the copy differs from the gate on exactly one line (one '>')
ok   and the disarm is planted exactly once in the copy
ok   and the gate itself disarms nothing

--- the runs under /bin/bash (bash 3.2.57(1)-release): both halves, over the whole corpus
ok   a destroyed analyser makes the gate exit non-zero [bash 3.2.57(1)-release]
ok   and the gate says it failed rather than printing OK [bash 3.2.57(1)-release]
ok   and the trap names the call site the analyser died at [bash 3.2.57(1)-release]
ok   and attributes it to an analyser that examined nothing [bash 3.2.57(1)-release]
     (this awk exited 2 on the planted syntax error)
ok   the negative control: the same copy unmutated exits 0 [bash 3.2.57(1)-release]
ok   and prints the OK line [bash 3.2.57(1)-release]
ok   and writes nothing at all to stderr [bash 3.2.57(1)-release]

--- the mutated run's stderr under bash 3.2.57(1)-release: 7 line(s), at most 20 shown
     awk: syntax error at source line 4
      context is
     	    removed = >>>  = <<<  1
     awk: illegal statement at source line 4
     awk: illegal statement at source line 4
     FAIL check -  an analyser exited 2 and examined nothing; its last argument was '/var/folders/mg/c8xtgk197f74w3r78q7_9sfc0000gn/T/tmp.2IH9aPyhEs/bean-edges.uniq'
     FAIL check -  line 655: a command exited 2 and nothing checked it: 'cycle="$(awk -F'\t' '   { from[NR] = $1; to[NR] = $2; n = NR }   END {     removed = = 1     while (removed) {       ...'

--- the runtime failure path under bash 3.2.57(1)-release: one record per plant, all eight in one run
ok   an unchecked non-zero exit makes the gate exit non-zero [bash 3.2.57(1)-release]
ok   and the gate says it failed rather than printing OK [bash 3.2.57(1)-release]
ok   and records a silent non-zero exit, once and distinctly [bash 3.2.57(1)-release]
ok   and records a grep that could not look, at an opted-out site, once and distinctly [bash 3.2.57(1)-release]
ok   and records a missing file, once and distinctly [bash 3.2.57(1)-release]
ok   and records a failed pipeline element, once and distinctly [bash 3.2.57(1)-release]
ok   and records an unbound variable inside $( ), once and distinctly [bash 3.2.57(1)-release]
ok   and records a failed cd, once and distinctly [bash 3.2.57(1)-release]
ok   and records a silent non-zero exit in check 14's preamble, once and distinctly [bash 3.2.57(1)-release]
ok   and records a missing file at the last statement before the count, once and distinctly [bash 3.2.57(1)-release]
ok   and the pipeline record carries the statuses that say which end failed [bash 3.2.57(1)-release]
ok   and records nothing else: eight plants, nine distinct records [bash 3.2.57(1)-release]

--- the records under bash 3.2.57(1)-release, and the 27 firings they came from
     FAIL check -  an opted-out command exited 2 and could not look, which is not 'no match': grep -c . /no/such/file/__probe_cannot_look__
     FAIL check -  line 205: a command exited 1 and nothing checked it: 'false __probe_silent__'
     FAIL check -  line 208: a command exited 2 and nothing checked it: 'return "$ec"'
     FAIL check -  line 271: a command exited 1 and nothing checked it: 'cat /no/such/file/__probe_missing_file__'
     FAIL check -  line 401: a command exited 1 and nothing checked it: 'sed -n 's/__probe_pipeline__//p'' (pipeline exited 1 0, left to right)
     FAIL check -  line 573: a command exited 1 and nothing checked it: 'probe="$(echo "${__probe_unbound_subst}")"'
     FAIL check -  line 750: a command exited 1 and nothing checked it: 'cd /no/such/dir/__probe_failed_cd__'
     FAIL check -  line 818: a command exited 1 and nothing checked it: 'false __probe_check14__'
     FAIL check -  line 894: a command exited 1 and nothing checked it: 'cat /no/such/file/__probe_last_statement__'

--- and with no scratch directory, the gate stops instead of reporting [bash 3.2.57(1)-release]
ok   a gate that cannot create its record file exits 2 [bash 3.2.57(1)-release]
ok   and prints nothing at all on stdout, so there is no OK line to misread [bash 3.2.57(1)-release]
     (it wrote 1 line(s) to stderr; the first is: mktemp: mkdtemp failed on /no/such/dir/__probe_no_tmpdir__/UX4b3N: No such file or directory)

--- and with the record file removed under it mid-run, likewise [bash 3.2.57(1)-release]
ok   a gate whose record file vanished exits 2 [bash 3.2.57(1)-release]
ok   and says so on the line it stops at [bash 3.2.57(1)-release]
ok   and never reaches the OK line [bash 3.2.57(1)-release]

--- and with the ERR trap disarmed below every plant, the gate says so [bash 3.2.57(1)-release]
ok   a gate whose ERR trap was disarmed mid-file exits non-zero [bash 3.2.57(1)-release]
ok   and names the disarm rather than reporting OK [bash 3.2.57(1)-release]
ok   and never reaches the OK line [bash 3.2.57(1)-release]

--- the opt-out under bash 3.2.57(1)-release: what the trap is told not to look at
ok   the opt-out is one function, cut whole out of the gate [bash 3.2.57(1)-release]
ok   and it tolerates grep's 'no match', which is an answer [bash 3.2.57(1)-release]
ok   and does not tolerate a grep that could not look, which is a failure [bash 3.2.57(1)-release]
ok   and names the command it refused to tolerate [bash 3.2.57(1)-release]
ok   the glob helper is one function, cut whole out of the gate [bash 3.2.57(1)-release]
ok   and it prints nothing, at exit 0, for a glob that matches nothing [bash 3.2.57(1)-release]
ok   and prints the one file a glob that matches one does [bash 3.2.57(1)-release]

[...] the same rows again under /opt/homebrew/bin/bash (bash 5.3.9(1)-release), every one
      tagged [bash 5.3.9(1)-release] and every one `ok`. Two things differ and neither is
      asserted on: the $LINENO in the record for the plant inside check 2's loop reads 290
      there and 271 here, which is the loop-header difference measured elsewhere in this
      bean; and the scratch directory names, which mktemp chooses per run. The analyser's
      exit status is 2 under both, because it is the same awk either way — the interpreter
      that differs is the shell.

--- the guard covers every call site, because no call site opts in
ok   the guard's own call is the only site that bypasses it

docs-lint-gate-test: 110 passed, 0 failed, over 2 bash major version(s).
gate test exit: 0
```

What that costs. Both `Exec` tasks declare no inputs, so neither is ever up to date and every
figure here is a real run. The gate test's six gate runs are backgrounded against each other
within an interpreter and the interpreters run one after another, so on this machine it is
twelve full passes over the corpus; on the CI runner, which has one bash major version, it is
six and the second interpreter costs nothing. Measured here, alone on the machine:

```
head:     688f3ba
cmd:      /bin/bash tools/docs-lint.sh and /bin/bash tools/docs-lint-gate-test.sh, timed
          with `date +%s` around each, twice per shape, nothing else running
pass 1  rc=0  tools/docs-lint.sh             17 s
pass 1  rc=0  tools/docs-lint-gate-test.sh   52 s
pass 2  rc=0  tools/docs-lint.sh             16 s
pass 2  rc=0  tools/docs-lint-gate-test.sh   62 s
```

```
head:     688f3ba, working tree
cmd:      ./gradlew ktlintFormat, then ./gradlew qualityCheck > [...]/quality.txt 2>&1
[...] lines 1-266: Gradle's own task banners and the output of the tasks between
bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
bash-compat: OK — 5 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
[...] lines 268-439: Gradle's own task banners and the output of the tasks between
docs-lint-test: 76 passed, 0 failed.
[...] lines 441-442: Gradle's own task banners and the output of the tasks between
docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)
docs-lint-gate-test: analyser awk — awk version 20200816
[...] lines 445-600: the gate test's own output, quoted whole above, and the banners between
docs-lint: OK — 19 documents, 111 anchors, 1738 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
[...] lines 602-741: the rest of the gate test's output
docs-lint-gate-test: 110 passed, 0 failed, over 2 bash major version(s).
[...] lines 743-753: Gradle's own task banners and its deprecation notice
BUILD SUCCESSFUL in 3m 39s
170 actionable tasks: 56 executed, 94 from cache, 20 up-to-date
Configuration cache entry stored.
qualityCheck exit: 0
(the redirect is 756 lines; the quoted lines are 266, 267, 440, 443, 444, 601, 742, 754, 755, 756)
```

### The runner, where `bean:0118` said the boundary had never been measured

`bean:0118` recorded "could not verify: the boundary on the CI image" and called it the figure
in that bean most likely to be wrong. It was wrong, twice over: the first CI run of this branch
found a success-path site no run here could see, quoted in the audit section above, and this
one is the same tree with the opt-out re-derived. Eight planted points, nine records, the
analyser's two, and the one-interpreter banner, under bash 5.2.21 and a gawk:

```
head:     2efeb1c, GitHub Actions run 33950230043, job 101263499409, ubuntu-latest
cmd:      GITHUB_TOKEN= gh run view 33950230043 --job 101263499409 --log \
            | /usr/bin/grep -E 'docs-lint-gate-test:|analyser awk|ONE bash MAJOR|docs-lint: OK|nothing checked it|could not look|an analyser exited|BUILD |bash-compat: |docs-lint-test:'
observed: [...] each line below is preceded in the capture by the job name, the step name and
                an ISO-8601 timestamp, all three of GitHub's making and stripped here
bash-compat: interpreter /bin/bash (bash 5.2.21(1)-release)
bash-compat: OK — 5 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
docs-lint-gate-test: interpreter /bin/bash (bash 5.2.21(1)-release)
docs-lint-gate-test: analyser awk — GNU Awk 5.2.1, API 3.2, PMA Avon 8-g1, (GNU MPFR 4.2.1, GNU MP 6.3.0)
docs-lint-gate-test: exercising /bin/bash (bash 5.2.21(1)-release)
docs-lint-gate-test: ONE bash MAJOR VERSION ONLY on this host. The claims in
docs-lint-gate-test: tools/docs-lint.sh's trap comment that differ BY interpreter —
docs-lint-gate-test: which pipeline shapes reach the ERR trap, which element
docs-lint-gate-test: $BASH_COMMAND names, and what $LINENO holds inside a loop body —
docs-lint-gate-test: are exercised here for bash 5 and for no other.
docs-lint-test: 76 passed, 0 failed.
docs-lint: OK — 19 documents, 111 anchors, 1738 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
     FAIL check -  an analyser exited 1 and examined nothing; its last argument was '/tmp/tmp.BdfRtFLfb1/bean-edges.uniq'
     FAIL check -  line 655: a command exited 1 and nothing checked it: 'cycle="$(awk -F'\t' '   { from[NR] = $1; to[NR] = $2; n = NR }   END {     removed = = 1     while (removed) {       ...'
     FAIL check -  an opted-out command exited 2 and could not look, which is not 'no match': grep -c . /no/such/file/__probe_cannot_look__
     FAIL check -  line 205: a command exited 1 and nothing checked it: 'false __probe_silent__'
     FAIL check -  line 208: a command exited 2 and nothing checked it: 'return "$ec"'
     FAIL check -  line 290: a command exited 1 and nothing checked it: 'cat /no/such/file/__probe_missing_file__'
     FAIL check -  line 401: a command exited 1 and nothing checked it: 'sed -n 's/__probe_pipeline__//p'' (pipeline exited 1 0, left to right)
     FAIL check -  line 573: a command exited 1 and nothing checked it: 'probe="$(echo "${__probe_unbound_subst}")"'
     FAIL check -  line 750: a command exited 1 and nothing checked it: 'cd /no/such/dir/__probe_failed_cd__'
     FAIL check -  line 818: a command exited 1 and nothing checked it: 'false __probe_check14__'
     FAIL check -  line 894: a command exited 1 and nothing checked it: 'cat /no/such/file/__probe_last_statement__'
docs-lint-gate-test: 74 passed, 0 failed, over 1 bash major version(s).
BUILD SUCCESSFUL in 56s
exit:     0, and the `gate` job passed
```

And the shape table the suite now prints on every run, taken there — the runner's own answer
to the question the gate's trap comment turns on, rather than this machine's reasoning about it:

```
head:     2efeb1c, the same job; the same capture, `grep -A 14 'what an ERR trap can see'`
     /bin/bash (bash 5.2.21(1)-release) — firings per shape
       while             	1
       for               	1
       until             	1
       if                	1
       case              	1
       brace group       	1
       redirected group  	1
       subshell          	2
       simple command    	1
       function          	1
ok   a pipeline ending in a simple command reaches the trap under bash 5.2.21(1)-release
ok   and one ending in a function does too, under bash 5.2.21(1)-release
```

**bash 5.2.21 on the runner answers exactly as bash 5.3.9 does here, and neither answers as
3.2.57 does.** That is the difference the first CI run of this branch went red on, now printed
by the suite on the machine it matters on instead of inferred from a Homebrew build.

**Two figures differ from this machine's, and neither is asserted on.** The analyser's exit
status is **1** there against **2** here, which is gawk against the BSD awk macOS ships and is
the difference `bean:0123` had to stop asserting on. And the **line number** in one record is
the interpreter's rather than the gate's: the missing-file plant sits at line 290 of the
mutated copy under bash 5, and `/bin/bash` 3.2.57 reports **271** for the same line because
`$LINENO` inside a `for` body is not the file's line there. Both are printed and neither is
required, for the reason `bean:0123` gives: a number that differs per image is a measurement
and not a requirement.

**And one figure is smaller there, which is the point of it.** `74 passed` against `110` here:
the suite repeats its run assertions per bash MAJOR version, the runner has one, and it says so
in five lines rather than leaving a reader to infer it from a pass count.

## Can the suite tell this change from its absence, and from its deletion?

Twelve mutations of the change, each run against `tools/docs-lint-gate-test.sh` **unaltered**.
Each mutant is a copy of the gate at `tools/.docs-lint-mut-<tag>.sh` and each is run by a copy
of the gate test with its one `GATE=` line repointed at that copy — the repoint is asserted to
have landed exactly once per row — so nothing tracked is written, there is no restore step to
skip, and no `git` operation runs anywhere near `.beans` or `tools` (`bean:0102`). The
**control** is an unmutated copy through the same harness, which is what says the copying is
not what any row below is measuring. Every row is scored over both interpreters, because that
is what the suite now does.

```
head:     688f3ba, working tree; the gate under mutation is tools/docs-lint.sh as this ships
suite:    tools/docs-lint-gate-test.sh, UNALTERED, run under /bin/bash (GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25))
method:   mutant at tools/.docs-lint-mut-<tag>.sh; a copy of the suite at
          tools/.docs-lint-gt-<tag>.sh with its GATE= line repointed at it

mutation                   result
absent                     docs-lint-gate-test: 50 passed, 60 failed, over 2 bash major version(s). (repointed=1, rc=1)
armed-check-removed        docs-lint-gate-test: 104 passed, 6 failed, over 2 bash major version(s). (repointed=1, rc=1)
control                    docs-lint-gate-test: 110 passed, 0 failed, over 2 bash major version(s). (repointed=1, rc=0)
optout-removed             docs-lint-gate-test: 97 passed, 13 failed, over 2 bash major version(s). (repointed=1, rc=1)
optout-widened             docs-lint-gate-test: 102 passed, 8 failed, over 2 bash major version(s). (repointed=1, rc=1)
record-not-flattened       docs-lint-gate-test: 108 passed, 2 failed, over 2 bash major version(s). (repointed=1, rc=1)
scratch-guard-removed      docs-lint-gate-test: 104 passed, 6 failed, over 2 bash major version(s). (repointed=1, rc=1)
trap-deleted               docs-lint-gate-test: 82 passed, 28 failed, over 2 bash major version(s). (repointed=1, rc=1)
trap-disarmed              docs-lint-gate-test: 94 passed, 16 failed, over 2 bash major version(s). (repointed=1, rc=1)
trap-narrowed              docs-lint-gate-test: 89 passed, 21 failed, over 2 bash major version(s). (repointed=1, rc=1)
trap-neutered              docs-lint-gate-test: 88 passed, 22 failed, over 2 bash major version(s). (repointed=1, rc=1)
vanished-guard-removed     docs-lint-gate-test: 104 passed, 6 failed, over 2 bash major version(s). (repointed=1, rc=1)

--- git status --porcelain:
 M .beans/modus-0118--docs-lint-reports-ok-through-almost-every-runtime-failure.md
 M .beans/modus-0124--the-non-analyser-fail-open-boundary-in-docs-lint.md
 M build.gradle.kts
 M tools/docs-lint-gate-test.sh
 M tools/docs-lint.sh
?? tools/docs-lint-boundary-probe.sh
--- (end)
```

What each row edits, beside the score above:

| mutation | edit |
|---|---|
| `control` | a copy of the gate, unmutated |
| `absent` | `tools/docs-lint.sh` as `main` has it |
| `trap-deleted` | the `trap … ERR` line removed; the handler and `absent_ok` stay |
| `trap-neutered` | the handler records nothing |
| `trap-narrowed` | the handler returns early unless the command names one plant |
| `trap-disarmed` | `trap - ERR` planted below every plant this suite carries |
| `armed-check-removed` | the `case "$(trap -p ERR)"` block removed |
| — | *(the mechanism this row mutates was replaced in review; see the stamp below)* |
| `record-not-flattened` | one record spans the lines of the command it names |
| `scratch-guard-removed` | `TMP="$(mktemp -d)" \|\| exit 2` loses its guard |
| `vanished-guard-removed` | the `[ ! -f "$TMP/fails.txt" ]` block removed |
| `optout-widened` | `absent_ok` tolerates every status up to 9 |
| `optout-removed` | `absent_ok` tolerates nothing, so the trap fires on the success path |

The matrix is driven under `/bin/bash` 3.2.57 and each row's suite then exercises both. Every
figure it scores is a pass/fail count and not a figure of the corpus. `repointed=1` is the
harness asserting on itself: a copy whose `GATE=` line did not move would score the unmutated
gate and every row would read like the control.

**THIS MATRIX IS STAMPED AT `688f3ba` AND HAS NOT BEEN RE-RUN AT `0982b71`.** Review replaced
the mechanism three of its rows mutate, and the honest thing is to say which rather than to
leave twelve figures reading as if they described the gate that ships:

- `armed-check-removed` **(104/6)** removed a `case "$(trap -p ERR)"` block that no longer
  exists. Its analogue at this head is "the live fire removed", and it is not re-measured.
- `trap-neutered` **(88/22)** and `trap-deleted` **(82/28)** were separated by that reading.
  They are still separated at this head, and by more: the live fire catches the neutered
  handler by the record it stops producing, which is what the three failure-path copies in the
  suite now plant directly.
- `control` **(110/0)** is **146/0** at this head — the same suite with 36 more assertions.
  Measured: `/bin/bash tools/docs-lint-gate-test.sh`, `docs-lint-gate-test: 146 passed, 0
  failed, over 2 bash major version(s).`, rc=0.

Re-running all twelve rows is 192 full passes over the corpus and was not done here. What was
done instead is three targeted negative controls at this head, each recorded in the sections
above: the reading restored in place of the live fire (**120/23**), `[ -e "$p" ]` restored in
`glob_lines` (**141/2**), and one count moved back into the `printf` (**145/1**). Those bound
the three things this review changed; they do not re-establish the other nine rows, and a
reader should treat the table above as a measurement of `688f3ba` and nothing else
(`doc:50-memory-and-evidence#corpus-figures`).

**Two rows are new, and one of them is the reason this section was rewritten.** `trap - ERR`
planted below every plant scored **49 passed, 0 failed** against the previous suite: a
one-line edit that silences the last hundred lines of the gate, invisible to a clean sheet of
assertions. Removing the armed-at-exit check that now catches it is the twelfth row, so the
catcher is itself mutation-scored rather than trusted.

**Deletion and neutering no longer score identically, and the difference is the armed check.**
The two mutants differ in whether `docs_lint_err` is defined-and-never-called or
called-and-does-nothing; against the previous suite they were the same program at the gate's
interface and both scored 38/11. They are not the same program to a gate that asks the shell
what handler it holds: deletion leaves the ERR slot empty and is reported as a disarm on every
run, neutering leaves the handler installed and is caught only by the records it stops
producing. `bean:0123` separated its equivalent pair with a lexical assertion, and that
assertion is the one `bean:0123` itself records as bounding nothing; this pair is separated by
what the shell holds at runtime. **Absence** is separated from **deletion** as well: the absent
gate has no scratch-directory guard, no vanished-record guard, no `absent_ok` and no armed
check, and it fails many more rows than the deletion does.

**The narrowing is the sharp one.** `bean:0123`'s two narrowing mutants left the guard covering
one call site out of twenty-two and scored a clean sheet against its whole suite. The
equivalent here — the handler returning early unless the failing command names one of the five
plants — fails nine rows: the four class rows it stops covering, the two rows that count what
was recorded, the analyser call-site row, the row that requires the gate to say it failed, and
one that is an artefact of the mutation rather than a signal, since writing a plant's marker
into the handler makes that marker occur in the gate. Each class is asserted by the record it
produces and not by the run going red, which is what makes eight of the nine real.

**One mutation scored a clean sheet in the first matrix, and the repair for it was overstated
twice before it was measured.** Widening `absent_ok` from "status 1" to "every status up to 9"
makes the opt-out swallow a `grep` that could not look, which is fail-open at exactly the sites
the opt-out exists for — and every assertion in the first suite passed. The repair was then
described here as three assertions. It was **one**: of the three rows under `--- the opt-out`,
only "and does not tolerate a grep that could not look" changes its answer under the widening,
and it is a unit test of the function's text cut out with `sed` and run in isolation — not an
observation of the gate over any tree. The other two pass under the widened function as
readily as under the correct one.

The repair now has a gate-level half. The planted table carries an eighth point,
`absent_ok grep -c . /no/such/file/__probe_cannot_look__`, planted into the gate and run over
the whole corpus, and `absent_ok` names the command it refuses rather than leaving the trap to
record `return "$ec"`. Under the widened mutant, four rows change their answer per interpreter
— eight over the two — and two of the four are observations of the gate:

```
head:     this branch's working tree
cmd:      the mutation matrix below, row `optout-widened`, scored against the suite UNALTERED
FAIL and records a grep that could not look, at an opted-out site, once and distinctly [bash 3.2.57(1)-release]
FAIL and records nothing else: eight plants, nine distinct records [bash 3.2.57(1)-release]
FAIL and does not tolerate a grep that could not look, which is a failure [bash 3.2.57(1)-release]
FAIL and names the command it refused to tolerate [bash 3.2.57(1)-release]
[...] the same four rows again, tagged [bash 5.3.9(1)-release]
```

**The mutation that is missing, and why.** "Fires on every input" has no one-line edit here:
the `ERR` trap fires on a non-zero status by construction, and the nearest mutation is
`ERR` → `DEBUG`. That mutant was abandoned after fifteen minutes with the gate test's first
run still going, and this is the reason as a figure — a DEBUG-trap count of the commands one
gate run executes in its top-level shell alone, each of which would otherwise have written a
record through a `printf | tee`:

```
head:     0982b71 plus the working-tree edits; BOTH RUNS TAKEN BACK TO BACK, one command,
          so the corpus is the same for the two of them and only the gate differs
method:   a COPY of each gate with a DEBUG trap that only increments a counter and an EXIT
          trap that prints it; each copy at tools/.docs-lint-dbg-$$.sh, deleted after
interpreter: /bin/bash (GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25))
--- the three lines inserted, against the gate this ships:
32a33,34
> __dbg=0
> trap '__dbg=$((__dbg + 1))' DEBUG
46a49
> trap 'printf "commands executed in the top-level shell: %s\n" "$__dbg" >&2' EXIT
--- the run, against the gate this ships:
exit: 0
commands executed in the top-level shell: 31266
--- the same three lines against `git show 277c4d5:tools/docs-lint.sh`, on the SAME tree:
32a33,34
> __dbg=0
> trap '__dbg=$((__dbg + 1))' DEBUG
38a41
> trap 'printf "commands executed in the top-level shell: %s\n" "$__dbg" >&2' EXIT
exit: 0
commands executed in the top-level shell: 31256
```

**That figure is expired the moment anything in this bean is written, and every earlier
statement of it is already wrong.** This bean recorded `31184`, then `31185`/`31182`; a reader
relaying it recorded `31079`; review re-took it at `160e1ff` as `31228`/`31225`; the pair above
is `31266`/`31256`, and writing this paragraph has moved it again. Nothing was miscounted — the
counter walks `.beans/`, and `.beans/modus-0124--*.md` is one of the files it walks, so a
record measuring a corpus it belongs to changes that corpus with every edit
(`doc:50-memory-and-evidence#corpus-figures`).

**What survives is the two things that are not absolutes.** The order of magnitude — thirty-one
thousand records through a `printf | tee`, which is why the `ERR` → `DEBUG` mutant was
abandoned. And the DELTA between the two gates on one tree, which is a property of the change
rather than of the corpus: `+3` at `160e1ff` and `+10` here, the seven being the four counts
that became statements of their own, the live fire, its count, and its check. Both halves are
taken in one command precisely so the delta means something; taking them minutes apart across
an edit to this file is how the earlier pair stopped being comparable.

The over-firing shape that *can* be scored is the last row of the matrix, and it is the one
`doc:00-constitution#observed-failing`'s negative half is about: with the opt-out removed the
trap fires on the success path, and the three rows that catch it are the negative control —
the unmutated copy exits 0, prints the `OK` line, and writes nothing at all to stderr.

## Do the fixtures share a structural assumption?

They share one, and it is stated rather than hidden. **Every plant is a whole line inserted
after an existing line**, so a defect that needed an existing line *changed* — the analyser
plant's shape — is reached only by the first plant, which is `bean:0123`'s and does change a
line. What they deliberately do not share:

- **plant point** — eight points running from just under the trap to the last statement the
  gate executes before it counts its records, one of them inside a loop body rather than at
  the top level, and the boundary table adds a ninth inside a subshell to measure the
  residual. **The last two were added because the eight were once five, all above line 681 of
  an 848-line file, and `trap - ERR` planted at 742 passed the whole suite.** Position is a
  dimension the fixtures vary in, not a property they happen to have;
- **how the status arises** — a command that exits non-zero, a missing file, a builtin
  failing, a pipeline element failing under `pipefail`, the shell itself under `set -u`, and a
  `grep` that could not look at a site the opt-out tolerates "no match" at, which is the one
  status the opt-out must let through;
- **which mechanism records it** — seven of the eight are recorded by the trap and the eighth
  by `absent_ok` itself, in different words, so a suite matching one phrase would miss it;
- **interpreter** — every run assertion is made once per bash major version on the host;
- **whether a diagnostic exists at all** — `false __probe_silent__` writes nothing to stderr,
  which is the row a reader could not have noticed by any other means, and `cat` and the
  unbound variable write one;
- **what the record has to name** — the pipeline row's `$BASH_COMMAND` is the element that
  SUCCEEDED, so that row is asserted on the statuses beside the command rather than on the
  command.

The trap PR #79 found was a suite whose every fixture put its column last, so a defect
involving a middle column was invisible to all of them at once. The equivalent here would be
every plant at the top of the file, where a mechanism armed only at the top would pass
everything; `a3`, `a4`, the check-13c plant and the two below it are the answer to it, and
`a4` is the one that comes back negative and is reported as the residual rather than dropped.
**The first revision of this section claimed the answer and did not have it**: the five plants
it named stopped at line 681, so a mechanism armed only over the first 80% of the file would
have passed everything, and one did.

## Not verified here

- **The runner itself, for the boundary table.** `bean:0118` named the CI image as the figure
  most likely to be wrong, and it was: the first CI run of this branch found a success-path
  site that no run here could see. The audit, the suite and — now that the rig is committed —
  the boundary table are all taken under 3.2.57 and 5.3.9, which is the row this bullet used
  to hold. What is still unmeasured is the runner's own image: bash 5.2.21 and a gawk, where
  the suite runs but the boundary probe does not, because it is not in `qualityCheck`.
  `tools/docs-lint-boundary-probe.sh` is the rig; taking it there is one command and a job.
- **That the trap reaches every statement in the file.** Eight points are made to fail, not
  every point. Nothing here bounds the set of SHAPES it catches; what is bounded now is the
  RANGE the failure path works over, by the gate FIRING that path at its last trap-visible
  statement and requiring the record — and that bound has a hole of its own, a break that
  undoes itself before that line. The residuals are named above and the fail-closed harness is
  `bean:0126`. Three ways of breaking the path are planted and caught; **three is not a claim
  that three is all there are**, and the reason the live fire is worth more than the reading it
  replaced is precisely that it does not have to enumerate them.
- **The status-2 tolerance at check 13c's `grep -qx "$nid" "$TMP/bean-ids-main.txt" || continue`.**
  The one blanket `|| continue` left. Here the STATUS is the answer, so `absent_ok` — which is
  for sites that read a VALUE — does not fit, and `|| continue` therefore tolerates 2 along
  with 1. It is unreachable today: the file is written unconditionally a few lines above inside
  the same `if`, and the loop body runs only for ids this branch introduced, which is empty on
  a clean tree, so no plant in the suite reaches it. Named at the site rather than fixed,
  because a fix nothing can be observed to protect is not enforcement
  (`doc:00-constitution#observed-failing`). Whoever makes it reachable owes it a narrowing.
- **Every bash there is.** The suite exercises one interpreter per MAJOR version found on the
  host, from a candidate list of paths. Here that is 3.2.57 and 5.3.9; on the runner it is
  5.2.21 alone, and the suite says so in five lines rather than leaving it inferred. A bash 4
  is on neither machine, and the row of the shape table it would fill is unmeasured.
- **A check that runs, exits 0 and examines nothing.** No trap can see it. `bean:0126`.
- **`tools/docs-lint-test.sh` and `tools/bash-compat-lint.sh`**, which have the same shape and
  are `bean:0125`'s.
