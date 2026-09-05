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


## The audit, which is what made the trap safe to arm — and which had to be run twice

`bean:0124` names the audit of every command in the file as the work, and
`doc:00-constitution#observed-failing`'s negative half is why: a mechanism that fires when
nothing is wrong is worse than the gap, because it will be removed rather than fixed. The
audit is a measurement, not a reading. A copy of the gate gets `set -E` and an `ERR` trap that
only **records** — errtrace ON here, so a firing inside a function or a subshell cannot be
missed — and the green run says which commands return non-zero when nothing is wrong.

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

All three now go through `absent_ok`, which tolerates `grep`'s status 1 — the pattern is
absent — and nothing above it. The audit against the gate as it ships, under both
interpreters, finds nothing:

```
head:     this branch's working tree; the gate under audit is the one this ships
method:   the same recording trap, at the same insertion point, under /bin/bash 3.2.57
gate under audit: [...]/tools/docs-lint.sh
interpreter:      /bin/bash (GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25))
--- the two lines inserted:
32a33,34
> set -E
> trap 'printf "%s\t%s\n" "$?" "$BASH_COMMAND" >> [...]/errsites-after.txt' ERR
--- the run:
exit: 0
stdout:
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr byte count: 0
--- ERR firings on a green run, by (status, command), most frequent first:
--- total firings: 0
--- distinct commands: 0
--- git status --porcelain:
 M tools/docs-lint.sh
--- (end)
```

```
head:     this branch's working tree; the gate under audit is the one this ships
method:   the same recording trap, at the same insertion point, under bash 5.3.9
gate under audit: [...]/tools/docs-lint.sh
interpreter:      /opt/homebrew/bin/bash (GNU bash, version 5.3.9(1)-release (aarch64-apple-darwin25.1.0))
--- the two lines inserted:
32a33,34
> set -E
> trap 'printf "%s\t%s\n" "$?" "$BASH_COMMAND" >> [...]/errsites-after5.txt' ERR
--- the run:
exit: 0
stdout:
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr byte count: 0
--- ERR firings on a green run, by (status, command), most frequent first:
--- total firings: 0
--- distinct commands: 0
--- git status --porcelain:
 M tools/docs-lint.sh
--- (end)
```

And the suite itself, run under bash 5.3.9 rather than the pinned 3.2.57, which is the check
that would have caught the site before the runner did:

```
head:     this branch's working tree
cmd:      /opt/homebrew/bin/bash tools/docs-lint-gate-test.sh > [...]/gate-test-bash5.txt 2>&1
observed: docs-lint-gate-test: interpreter /opt/homebrew/bin/bash (bash 5.3.9(1)-release)
          [...] the forty-nine assertion rows and the banners between them, identical to the
                3.2.57 run quoted under criterion 4 except for the interpreter line, the awk
                version line, and the analyser's exit status where it is reported
          docs-lint-gate-test: 49 passed, 0 failed.
exit:     0
```

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

Seven distinct sites fire, and every one of them is a line whose next few lines report the
same defect on stdout — `key 'depends_on' appears 0 times`, the check 6 record for a `doc:`
reference to a document that is not there, and `blocked_by 'modus-9998' resolves to 0 bean
files`; all three are in the fence above, where a fenced block keeps the reference itself from
resolving. The run is red either way; what the trap adds there is a second record for a defect
already named, which is noise on a red run and not a red run of its own. **If one of those
sites ever fires on a green corpus the gate goes red on a clean tree**, which is the risk this
audit exists to bound — and the CI run above is what that risk looks like when the bound is
taken on one interpreter only.

## What changed, and why this mechanism and not the other three

**An `ERR` trap that records through `fail`, plus one named opt-out.** `tools/docs-lint.sh`
now arms `trap 'docs_lint_err $? "$BASH_COMMAND" "$LINENO" "${PIPESTATUS[*]}"' ERR` as soon
as the file every record is written into exists, and the handler appends one line through
the same `fail` every check uses. The run continues; the exit status changes, because
`docs-lint: N failure(s).` is a count of that file's lines. Two commands whose non-zero
status is an **answer** rather than a failure — `grep` reporting no match — go through an
`absent_ok` wrapper that tolerates status 1 and nothing above it. There turned out to be
three, and the third was found by the runner rather than by this machine.

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
call site, which `bean:0123`'s wrapper cannot name.

**Does not catch**, named because the assertions would otherwise imply it:

- **A failure whose status an enclosing subshell discards.** In this file that is a non-final
  command inside a `printf … | while read` body: the loop runs in a subshell of its own and
  its status is the last command's. The loops of that shape are check 7's over `read_when`,
  check 2's over `provides`, check 5's two over `comm`, and check 10's over a `grep -noE` —
  named rather than counted, since the count moves with the next check anyone writes.
  Measured as the fourth plant point of the table below, where every row reaches `OK` at exit
  0 exactly as it did before the change. `set -E` closes it, at the price stated above; that price is the one thing in this
  work item a reviewer could reasonably reverse.
- **A pipeline whose last element is a compound command, under bash 3.2 only.** The trap does
  not reach it there and does reach it under bash 5, which is how a success-path site got past
  the audit and turned the runner red. Measured in the audit section above. It is a difference
  in what the trap SEES, not in what the gate does, and it means the audit is only as complete
  as the set of interpreters it was run under — here, 3.2.57 and 5.3.9.
- **A failure in a tested context** — `if cmd`, `while cmd`, `cmd && …`, `cmd || …`. The
  status is consumed there, which is the same rule errexit uses, so a `grep` that could not
  look, at a site where the gate reads non-zero as "no match", is still read as an answer.
  The sites are enumerated by the red-corpus audit below rather than by reading.
- **A command that runs, exits 0, and examines nothing.** That is `bean:0126`, and no trap
  can see it.
- **`ROOT` coming out empty.** `cd ""` exits **0** under `/bin/bash` 3.2.57 — measured —
  so `cd "$ROOT" || exit 2` would not fire if `ROOT="$(cd "$(dirname "$0")/.." && pwd)"`
  produced nothing. It cannot while the directory the running script sits in exists, and it
  is named here so the next reader does not re-derive it as an open gap.
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
head:     this branch's working tree; the gate under probe is a frozen copy of
          tools/docs-lint.sh taken by the same script before the first plant
method:   each row planted into a COPY at tools/.docs-lint-p-<point>-N.sh, the copy run, the
          copy deleted; six copies at a time; no tracked file written (bean:0102). The anchor
          and the planted line reach awk through the ENVIRONMENT and not `-v`, which processes
          escapes: the `printf … | while read` anchor contains a backslash and an n, and with
          `-v` it silently matched nothing — which is what the `lines added` column is for

=== plant point a1, immediately after: set -uo pipefail
planted line                                   exit OK?  records distinct lines added
echo "$__probe_unbound_top"                    1    no   0       0       1
;;                                             2    no   0       0       1
echo x | ;;                                    2    no   0       0       1
false                                          0    yes  0       0       1
/usr/bin/false                                 0    yes  0       0       1
cat /no/such/file/anywhere                     0    yes  0       0       1
cd /no/such/dir/anywhere                       0    yes  0       0       1
false | cat                                    0    yes  0       0       1
probe_x="$(echo "$__probe_unbound_sub")"       0    yes  0       0       1
echo "$__probe_unbound_pipe" | cat             0    yes  0       0       1
probe_y="$( ;; )"                              0    yes  0       0       1
awk "BEGIN { x = = 1 }" /dev/null | cat        0    yes  0       0       1
probe_z="$(awk "BEGIN { x = = 1 }" /dev/null)" 0    yes  0       0       1

=== plant point a2, immediately after: FM_FILES="$(ls documentation/*.md documentation/adr/*.md)"
planted line                                   exit OK?  records distinct lines added
echo "$__probe_unbound_top"                    1    no   0       0       1
;;                                             2    no   0       0       1
echo x | ;;                                    2    no   0       0       1
false                                          1    no   1       1       1
/usr/bin/false                                 1    no   1       1       1
cat /no/such/file/anywhere                     1    no   1       1       1
cd /no/such/dir/anywhere                       1    no   1       1       1
false | cat                                    1    no   1       1       1
probe_x="$(echo "$__probe_unbound_sub")"       1    no   1       1       1
echo "$__probe_unbound_pipe" | cat             2    no   878     43      1
probe_y="$( ;; )"                              1    no   1       1       1
awk "BEGIN { x = = 1 }" /dev/null | cat        1    no   1       1       1
probe_z="$(awk "BEGIN { x = = 1 }" /dev/null)" 1    no   1       1       1

=== plant point a3, immediately after:   id="$(field "$f" S id)"
planted line                                   exit OK?  records distinct lines added
false                                          1    no   19      1       1
/usr/bin/false                                 1    no   19      1       1
cat /no/such/file/anywhere                     1    no   19      1       1
cd /no/such/dir/anywhere                       1    no   19      1       1
false | cat                                    1    no   19      1       1
probe_x="$(echo "$__probe_unbound_sub")"       1    no   19      1       1
echo "$__probe_unbound_pipe" | cat             2    no   858     39      1
probe_y="$( ;; )"                              1    no   19      1       1

=== plant point a4, immediately after:     printf '%s\n' "$prov" | while IFS= read -r a; do
planted line                                   exit OK?  records distinct lines added
false                                          0    yes  0       0       1
/usr/bin/false                                 0    yes  0       0       1
cat /no/such/file/anywhere                     0    yes  0       0       1
cd /no/such/dir/anywhere                       0    yes  0       0       1
false | cat                                    0    yes  0       0       1
probe_x="$(echo "$__probe_unbound_sub")"       0    yes  0       0       1
echo "$__probe_unbound_pipe" | cat             0    yes  0       0       1
probe_y="$( ;; )"                              0    yes  0       0       1

--- git status --porcelain after every probe was removed:
 M tools/docs-lint.sh
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
site that `bean:0123`'s wrapper cannot name. One dead analyser, two records, both true:

```
head:     this branch's working tree
method:   bean:0123's own plant — check 12's acyclicity analyser, one line replaced by a
          syntax error — in a COPY at tools/.docs-lint-c12.sh, deleted after
--- diff:
573c573
<     removed = 1
---
>     removed = = 1
--- run:
exit: 1
stdout:
docs-lint: 2 failure(s).
stderr:
awk: syntax error at source line 4
 context is
	    removed = >>>  = <<<  1
awk: illegal statement at source line 4
awk: illegal statement at source line 4
FAIL check -  an analyser exited 2 and examined nothing; its last argument was '[...]/tmp.tyriBZxLp3/bean-edges.uniq'
FAIL check -  line 588: a command exited 2 and nothing checked it: 'cycle="$(awk -F'\t' '   { from[NR] = $1; to[NR] = $2; n = NR }   END {     removed = = 1     while (removed) {       ...'
--- git status:
 M tools/docs-lint.sh
```

### Criterion 2 · the harness produces a red run and a green run on the same tree

The four-plant-point table under criterion 1 is both halves, from one harness invocation on
one tree: the `a1` rows reach
`docs-lint: OK` at exit 0 and the `a2` rows do not, and `a4`'s eight rows reach it too. A
harness that had stopped planting would report every row `exit 0 / OK`, and `a2` and `a3` say
it did not stop. The second, independent pair is `tools/docs-lint-gate-test.sh`'s own control,
which runs an unmutated copy of the gate at the same path as every mutant and requires it to
exit 0, print the `OK` line and write nothing at all to stderr — quoted whole under criterion
4 below.

### Criterion 3 · silent on the unmodified tree, byte-identical stdout

The control is `tools/docs-lint.sh` **as `main` has it**, run on **this** tree — not the
capture taken on `main`'s tree, whose corpus differs by this bean's own `status:` line
(`doc:50-memory-and-evidence#corpus-figures`).

```
head:     this branch's working tree
method:   main's gate copied to tools/.docs-lint-baseline.sh and run there, then the gate
          this ships, then `cmp` on the two stdouts; the copy deleted after
--- head, and what the two runs are:
dd64b8f4ef3ace2c3f0db5b97f158e2bc6f74083
before: tools/docs-lint.sh at 277c4d5, copied to [...]/tools/.docs-lint-baseline.sh
after:  tools/docs-lint.sh in the working tree

--- before:
exit: 0
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr bytes: 0

--- after:
exit: 0
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
stderr bytes: 0

--- cmp before.out after.out:
cmp exit: 0
--- git status --porcelain:
 M tools/docs-lint.sh
--- (end)
```

### Criterion 4 · the proof runs in `qualityCheck`, and `qualityCheck` is green

`tools/docs-lint-gate-test.sh` gains three plants and keeps the one it had. It is registered
as `docsLintGateTest` and is already a `qualityCheck` dependency, so nothing in
`build.gradle.kts` changes.

```
head:     this branch's working tree
cmd:      /bin/bash tools/docs-lint-gate-test.sh > [...]/gate-test-final.txt 2>&1
docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)
docs-lint-gate-test: analyser awk — awk version 20200816

--- the plant: check 12's acyclicity analyser, destroyed
ok   the mutation site occurs exactly once in the gate
ok   the copy differs from the gate on exactly one line (one '<', one '>')
ok   and the line it differs on is the planted syntax error
ok   the control copy is identical to the gate

--- the second plant: five runtime failures that are not an analyser
ok   the plant point for a silent non-zero exit occurs exactly once in the gate
ok   and a silent non-zero exit is planted exactly once in the copy
ok   and a silent non-zero exit's marker occurs nowhere in the gate itself
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
ok   five classes were planted, one line each

--- the third plant: the gate's own scratch directory, which every record is written into
ok   the scratch-directory line occurs exactly once in the gate
ok   and the copy differs from the gate on exactly one line (one '<', one '>')

--- the fourth plant: the scratch directory removed MID-RUN, which one row of the boundary does
ok   the plant point for the vanishing record file occurs exactly once in the gate
ok   and the copy differs from the gate on exactly one line (one '>')

--- the runs: both halves, over the whole corpus
ok   a destroyed analyser makes the gate exit non-zero
ok   and the gate says it failed rather than printing OK
ok   and the trap names the call site the analyser died at
ok   and attributes it to an analyser that examined nothing
     (this awk exited 2 on the planted syntax error)
ok   the negative control: the same copy unmutated exits 0
ok   and prints the OK line
ok   and writes nothing at all to stderr

--- the mutated run's stderr: 7 line(s), at most 20 shown
     awk: syntax error at source line 4
      context is
     	    removed = >>>  = <<<  1
     awk: illegal statement at source line 4
     awk: illegal statement at source line 4
     FAIL check -  an analyser exited 2 and examined nothing; its last argument was '[...]/tmp.ypIlujLE7u/bean-edges.uniq'
     FAIL check -  line 588: a command exited 2 and nothing checked it: 'cycle="$(awk -F'\t' '   { from[NR] = $1; to[NR] = $2; n = NR }   END {     removed = = 1     while (removed) {       ...'

--- the runtime failure path: one class per plant, all five in one run
ok   an unchecked non-zero exit makes the gate exit non-zero
ok   and the gate says it failed rather than printing OK
ok   and records a silent non-zero exit, once and distinctly
ok   and records a missing file, once and distinctly
ok   and records a failed pipeline element, once and distinctly
ok   and records an unbound variable inside $( ), once and distinctly
ok   and records a failed cd, once and distinctly
ok   and the pipeline record carries the statuses that say which end failed
ok   and records nothing else: five plants, five distinct records

--- the five records, and the 23 firings they came from
     FAIL check -  line 147: a command exited 1 and nothing checked it: 'false __probe_silent__'
     FAIL check -  line 207: a command exited 1 and nothing checked it: 'cat /no/such/file/__probe_missing_file__'
     FAIL check -  line 337: a command exited 1 and nothing checked it: 'sed -n 's/__probe_pipeline__//p'' (pipeline exited 1 0, left to right)
     FAIL check -  line 505: a command exited 1 and nothing checked it: 'probe="$(echo "${__probe_unbound_subst}")"'
     FAIL check -  line 681: a command exited 1 and nothing checked it: 'cd /no/such/dir/__probe_failed_cd__'

--- the opt-out: what the trap is told not to look at
ok   the opt-out is one function, cut whole out of the gate
ok   and it tolerates grep's 'no match', which is an answer
ok   and does not tolerate a grep that could not look, which is a failure

--- and with no scratch directory, the gate stops instead of reporting
ok   a gate that cannot create its record file exits 2
ok   and prints nothing at all on stdout, so there is no OK line to misread
     (it wrote 1 line(s) to stderr; the first is: mktemp: mkdtemp failed on /no/such/dir/__probe_no_tmpdir__/M8YRso: No such file or directory)

--- and with the record file removed under it mid-run, likewise
ok   a gate whose record file vanished exits 2
ok   and says so on the line it stops at
ok   and never reaches the OK line

--- the guard covers every call site, because no call site opts in
ok   the guard's own call is the only site that bypasses it

docs-lint-gate-test: 49 passed, 0 failed.
gate test exit: 0
```

What that costs. Both `Exec` tasks declare no inputs, so neither is ever up to date and every
figure here is a real run. The gate test's five gate runs are backgrounded against each other,
so five full passes over the corpus cost about one and a half — 28 s against the 17 s a single
gate run takes, where `bean:0123` measured 19 s for the two runs it shipped:

```
head:     this branch's working tree
cmd:      /bin/bash tools/docs-lint.sh and /bin/bash tools/docs-lint-gate-test.sh, timed with
          `date +%s` around each, twice per shape
pass 1  rc=0  tools/docs-lint.sh               17 s
pass 1  rc=0  tools/docs-lint-gate-test.sh     28 s
pass 2  rc=0  tools/docs-lint.sh               18 s
pass 2  rc=0  tools/docs-lint-gate-test.sh     28 s
```

```
head:     this branch's working tree
cmd:      ./gradlew ktlintFormat, then ./gradlew qualityCheck > [...]/quality.txt
[...] lines 1-346: Gradle's own task banners and the output of the tasks between
bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
bash-compat: OK — 4 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
[...] lines 349-350: Gradle's own task banners and the output of the tasks between
docs-lint-gate-test: interpreter /bin/bash (bash 3.2.57(1)-release)
docs-lint-gate-test: analyser awk — awk version 20200816
[...] lines 353-472: Gradle's own task banners and the output of the tasks between
docs-lint-test: 76 passed, 0 failed.
[...] lines 474-511: Gradle's own task banners and the output of the tasks between
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
[...] lines 513-568: Gradle's own task banners and the output of the tasks between
docs-lint-gate-test: 49 passed, 0 failed.
[...] lines 570-572: Gradle's own task banners and the output of the tasks between
BUILD SUCCESSFUL in 1m 17s
161 actionable tasks: 7 executed, 154 up-to-date
Configuration cache entry reused.
qualityCheck exit: 0
(the redirect is 576 lines; the quoted lines are 347, 348, 351, 352, 473, 512, 569, 573, 574, 575, 576)
```

### The runner, where `bean:0118` said the boundary had never been measured

`bean:0118` recorded "could not verify: the boundary on the CI image" and called it the figure
in that bean most likely to be wrong. It was wrong, twice over: the first CI run of this branch
found a success-path site no run here could see, quoted in the audit section above, and this
one is the same tree with that site opted out. Five classes, five records, under bash 5.2.21
and a gawk:

```
head:     44422df, GitHub Actions run 33941899636, job 101240866956, ubuntu-latest
cmd:      GITHUB_TOKEN= gh run view 33941899636 --job 101240866956 --log \
            | /usr/bin/grep -E 'docs-lint-gate-test:|analyser awk|docs-lint: OK|nothing checked it|an analyser exited|BUILD '
observed: [...] each line below is preceded in the capture by the job name, the step name and
                an ISO-8601 timestamp, all three of GitHub's making and stripped here
docs-lint-gate-test: interpreter /bin/bash (bash 5.2.21(1)-release)
docs-lint-gate-test: analyser awk — GNU Awk 5.2.1, API 3.2, PMA Avon 8-g1, (GNU MPFR 4.2.1, GNU MP 6.3.0)
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
     FAIL check -  an analyser exited 1 and examined nothing; its last argument was '/tmp/tmp.ncl5X0Xzpc/bean-edges.uniq'
     FAIL check -  line 588: a command exited 1 and nothing checked it: 'cycle="$(awk -F'\t' '   { from[NR] = $1; to[NR] = $2; n = NR }   END {     removed = = 1     while (removed) {       ...'
     FAIL check -  line 147: a command exited 1 and nothing checked it: 'false __probe_silent__'
     FAIL check -  line 226: a command exited 1 and nothing checked it: 'cat /no/such/file/__probe_missing_file__'
     FAIL check -  line 337: a command exited 1 and nothing checked it: 'sed -n 's/__probe_pipeline__//p'' (pipeline exited 1 0, left to right)
     FAIL check -  line 505: a command exited 1 and nothing checked it: 'probe="$(echo "${__probe_unbound_subst}")"'
     FAIL check -  line 681: a command exited 1 and nothing checked it: 'cd /no/such/dir/__probe_failed_cd__'
docs-lint-gate-test: 49 passed, 0 failed.
BUILD SUCCESSFUL in 51s
exit:     0, and the `gate` job passed on both events
```

**Two figures differ from this machine's, and neither is asserted on.** The analyser's exit
status is **1** there against **2** here, which is gawk against the BSD awk macOS ships and is
the difference `bean:0123` had to stop asserting on. And the **line number** in one record is
the interpreter's rather than the gate's: the missing-file plant sits at line 226 of the
mutated copy, which is what bash 5.2.21 reports, and `/bin/bash` 3.2.57 reports **207** for the
same line because `$LINENO` inside a `for` body is not the file's line there. Both are printed
and neither is required, for the reason `bean:0123` gives: a number that differs per image is a
measurement and not a requirement.

## Can the suite tell this change from its absence, and from its deletion?

Ten mutations of the change, each run against `tools/docs-lint-gate-test.sh` **unaltered**.
Each mutant is a copy of the gate at `tools/.docs-lint-mut-<tag>.sh` and each is run by a copy
of the gate test with its one `GATE=` line repointed at that copy, so nothing tracked is
written, there is no restore step to skip, and no `git` operation runs anywhere near `.beans`
or `tools` (`bean:0102`). The **control** is an unmutated copy through the same harness, which
is what says the copying is not what any row below is measuring.

| mutation | edit | result |
|---|---|---|
| the control | a copy of the gate, unmutated | 49 passed, 0 failed |
| the fix is absent | `tools/docs-lint.sh` as `main` has it | 28 passed, 21 failed |
| the trap is deleted | the `trap … ERR` line removed; the handler and `absent_ok` stay | 38 passed, 11 failed |
| the trap is neutered | the handler records nothing | 38 passed, 11 failed |
| the trap is narrowed to one class | the handler returns early unless the command names one plant | 40 passed, 9 failed |
| the record is not flattened | one record spans the lines of the command it names | 48 passed, 1 failed |
| the scratch-directory guard removed | `TMP="$(mktemp -d)" || exit 2` loses its guard | 45 passed, 4 failed |
| the vanished-record guard removed | the `[ ! -f "$TMP/fails.txt" ]` block removed | 46 passed, 3 failed |
| the opt-out is widened | `absent_ok` tolerates every status up to 9 | 48 passed, 1 failed |
| the opt-out is removed | `absent_ok` tolerates nothing, so the trap fires on the success path | 43 passed, 6 failed |

The matrix is taken under `/bin/bash` 3.2.57. Every assertion it scores is a pass/fail count
and not a figure of the corpus, and the same ten rows were scored twice — once before this
section existed and once after — with the same ten results.

**Deletion and neutering score identically, and that is the right answer rather than a blind
spot.** The two mutants differ in whether `docs_lint_err` is defined-and-never-called or
called-and-does-nothing; at the gate's interface they are the same program, and their failing
rows are the same rows — `diff` over the two `FAIL` lists is empty. `bean:0123` separated its
equivalent pair with a lexical assertion, and that assertion is the one `bean:0123` itself
records as bounding nothing. This suite separates **absence** from **deletion** instead, which
is a real difference: the absent gate has no scratch-directory guard, no vanished-record guard
and no `absent_ok` either, and it fails ten more rows than the deletion does.

**The narrowing is the sharp one.** `bean:0123`'s two narrowing mutants left the guard covering
one call site out of twenty-two and scored a clean sheet against its whole suite. The
equivalent here — the handler returning early unless the failing command names one of the five
plants — fails nine rows: the four class rows it stops covering, the two rows that count what
was recorded, the analyser call-site row, the row that requires the gate to say it failed, and
one that is an artefact of the mutation rather than a signal, since writing a plant's marker
into the handler makes that marker occur in the gate. Each class is asserted by the record it
produces and not by the run going red, which is what makes eight of the nine real.

**One mutation scored a clean sheet in the first matrix, and the assertion that now catches it
was written because of that.** Widening `absent_ok` from "status 1" to "every status up to 9"
makes the opt-out swallow a `grep` that could not look, which is fail-open at exactly the two
sites the opt-out exists for — and every assertion in the suite passed. The three rows under
`--- the opt-out` are the repair: the function is cut out of the gate by a `sed` range whose
two ends are asserted and **run**, once on an empty file, where `grep` says "no match", and
once on a file that is not there, where it says it could not look.

**The mutation that is missing, and why.** "Fires on every input" has no one-line edit here:
the `ERR` trap fires on a non-zero status by construction, and the nearest mutation is
`ERR` → `DEBUG`. That mutant was abandoned after fifteen minutes with the gate test's first
run still going, and this is the reason as a figure — a DEBUG-trap count of the commands one
gate run executes in its top-level shell alone, each of which would otherwise have written a
record through a `printf | tee`:

```
head:     this branch's working tree
method:   a COPY of the gate with a DEBUG trap that only increments a counter and an EXIT
          trap that prints it; deleted after
--- the three lines inserted:
32a33,34
> __dbg=0
> trap '__dbg=$((__dbg + 1))' DEBUG
44a47
> trap 'printf "commands executed in the top-level shell: %s\n" "$__dbg" >&2' EXIT
--- the run:
exit: 0
docs-lint: OK — 19 documents, 111 anchors, 1736 references, 112 beans, 43 graph edges, 49 selectable, 112 bean ids, 0 introduced, 112 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
commands executed in the top-level shell: 31184
--- git status:
 M tools/docs-lint.sh
```

The over-firing shape that *can* be scored is the last row of the matrix, and it is the one
`doc:00-constitution#observed-failing`'s negative half is about: with the opt-out removed the
trap fires on the success path, and the three rows that catch it are the negative control —
the unmutated copy exits 0, prints the `OK` line, and writes nothing at all to stderr.

## Do the fixtures share a structural assumption?

They share one, and it is stated rather than hidden. **Every plant is a whole line inserted
after an existing line**, so a defect that needed an existing line *changed* — the analyser
plant's shape — is reached only by the first plant, which is `bean:0123`'s and does change a
line. What they deliberately do not share:

- **plant point** — five points running from just under the trap to check 13c, one of them
  inside a loop body rather than at the top level, and the boundary table adds a sixth inside
  a subshell to measure the residual;
- **how the status arises** — a command that exits non-zero, a missing file, a builtin
  failing, a pipeline element failing under `pipefail`, and the shell itself under `set -u`;
- **whether a diagnostic exists at all** — `false __probe_silent__` writes nothing to stderr,
  which is the row a reader could not have noticed by any other means, and `cat` and the
  unbound variable write one;
- **what the record has to name** — the pipeline row's `$BASH_COMMAND` is the element that
  SUCCEEDED, so that row is asserted on the statuses beside the command rather than on the
  command.

The trap PR #79 found was a suite whose every fixture put its column last, so a defect
involving a middle column was invisible to all of them at once. The equivalent here would be
every plant at the top of the file, where a mechanism armed only at the top would pass
everything; `a3`, `a4` and the check-13c plant are the answer to it, and `a4` is the one that
comes back negative and is reported as the residual rather than dropped.

## Not verified here

- **The runner, for the boundary table.** Every row of it is `/bin/bash` 3.2.57 on macOS.
  `bean:0118` named the CI image as the figure most likely to be wrong, and it was: the first
  CI run of this branch found a success-path site that no run here could see, and the audit
  and the suite are now taken under bash 5.3.9 as well. What is still only reasoning is the
  boundary TABLE under bash 5 — the plants themselves have not been run there, only the audit
  and the forty-nine assertions.
- **That the trap reaches every statement in the file.** Six points are made to fail, not
  every point. Nothing here bounds the set of shapes it catches; the residuals are named above
  and the fail-closed harness is `bean:0126`.
- **A check that runs, exits 0 and examines nothing.** No trap can see it. `bean:0126`.
- **`tools/docs-lint-test.sh` and `tools/bash-compat-lint.sh`**, which have the same shape and
  are `bean:0125`'s.
