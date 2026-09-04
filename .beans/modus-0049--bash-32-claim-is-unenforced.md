---
# modus-0049
title: docs-lint claims bash 3.2 compatibility and nothing tests it
status: in-progress
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# `docs-lint` claims bash 3.2 compatibility and nothing tests it

`tools/docs-lint.sh:7-8` on `origin/main` states, as a constraint on everyone who edits it —
the locator carries its command, because it was written here as `:6` and is off by one
(`git show origin/main:tools/docs-lint.sh | awk 'NR == 7 || NR == 8'`):

> No bash 4 feature is used (macOS ships
> 3.2)

Nothing checks it. `build.gradle.kts`'s `docsLint` task runs `commandLine("bash", …)`, which
resolves through `PATH` — on the development machine that is Homebrew's bash 5.3.9, and in CI
it is Linux bash 5. Found by `bean:0035`'s implementation, which had to verify by hand:

```
cmd:      bash --version
observed: GNU bash, version 5.3.9(1)-release        # Homebrew, first on PATH
cmd:      /bin/bash --version
observed: GNU bash, version 3.2.57(1)-release       # what macOS actually ships
```

So the constraint is real for anyone running the script directly on a stock macOS shell, and
is enforced against nobody. A bash 4 feature could be added today and every gate would stay
green until it reached a developer whose `PATH` has no newer bash — the worst possible place
to discover it.

## Success criteria

1. Either the script is run under a 3.2-compatible interpreter by the gate, or the constraint
   is struck and the header stops claiming it. Both are defensible; claiming it and not
   checking it is not (`doc:00-constitution#observed-failing`).
2. If kept: `docsLint` invokes an interpreter that actually enforces it, and the choice is
   observed rejecting a planted bash 4 construct — an associative array or `mapfile` — before
   the claim is restored.
3. If struck: the header says which interpreter the script targets, and `bean:0035`'s
   awk-portability notes are re-read, since they were written for the same reason.

The three bullets above are the criteria as written when this bean was raised, renumbered and
not reworded, so that what follows can be read against them rather than against a target moved
to fit it (`bean:0113`).

## Until it is enforced, verifying costs one second

`/bin/bash` on macOS is genuinely 3.2.57, so `/bin/bash tools/docs-lint.sh` is a real
compatibility check that anyone editing the script can run before pushing. `bean:0051`'s
implementation did exactly that. It does not close this bean — a check nobody is required to
run is not a gate — but it removes any excuse for shipping a bash 4 construct in the
meantime.

## Criterion 2 cannot be met as written, and this bean stays open

The claim was kept, not struck, so criterion 3 does not apply and criterion 2 governs. It asks
for **the interpreter** to be observed rejecting a planted associative array or `mapfile`. No
interpreter does. Under a real bash 3.2 both constructs leave the exit status at 0 — this
script has `set -uo pipefail` and no `set -e` — so `docsLint` prints its `OK` line and passes
with the violation in the file. That is measured below, twice, and it is why criterion 2 is
recorded here as NOT MET rather than answered by a mechanism it does not name.

What was built instead is `tools/bash-compat-lint.sh`, which does reject both, on both
platforms, and which is in `qualityCheck`. Whether that satisfies the intent of criterion 2 is
a decision for whoever owns this bean; it is not a decision the implementing agent may take by
amending the criterion. Closing this bean needs either that amendment, made deliberately, or a
design in which the interpreter itself fails — which would mean giving `tools/docs-lint.sh` an
error-trapping mode it does not have, and would still be inert on the Linux CI image, where
`/bin/bash` is bash 5.

## Evidence

Every figure below was redirected to a file and pasted from it. Interpreter versions:
`/bin/bash` is `GNU bash, version 3.2.57(1)-release`, `/opt/homebrew/bin/bash` is
`GNU bash, version 5.3.9(1)-release`, both on `arm64-apple-darwin25`.

### 1 — the gate runs the script under a 3.2-compatible interpreter (MET on macOS, not on CI)

`build.gradle.kts` now names `/bin/bash` by absolute path for `docsLint`, `docsLintTest` and
the new `bashCompatLint`, instead of resolving `bash` through `PATH`. The task reports which
interpreter it got, so the answer is in every run rather than in a comment:

```
cmd:      ./gradlew bashCompatLint
observed: bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 3 scripts parsed, 16 rules, 16 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
```

The same task on the CI runner, which is where the pattern file's POSIX character classes
stop being an assumption about the image's awk and start being an observation:

```
cmd:      gh run view --job 100875457512 --log   # qualityCheck on ubuntu-latest
observed: bash-compat: interpreter /bin/bash (bash 5.2.21(1)-release)
          bash-compat: OK — 3 scripts parsed, 16 rules, 16 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
```

All sixteen samples are caught exactly once under CI's awk as well as under BSD awk locally,
so the scan is not silently inert on the platform where it is the only half that applies.

**The residual, stated rather than glossed:** on the CI image `/bin/bash` is bash 5, so the
half of the gate that is a genuine 3.2 parse runs on macOS only. What CI gets is a *known*
interpreter at a fixed path rather than a *3.2* one. That is the property the three changes
queued behind this bean actually need, and it is less than criterion 1's literal words.

### 2 — the interpreter rejecting a planted bash 4 construct (NOT MET)

Planted `declare -A seen` at `tools/docs-lint.sh:30`, two lines after `set -uo pipefail` at
line 28 — line 29 is blank — and ran the gate task whose interpreter is the one criterion 2 is
about. The later amendment plants at line 29 instead and its diagnostics say `line 29`; both
are what the run printed, and neither is "immediately after" the other:

```
cmd:      ./gradlew docsLint
observed: > Task :docsLint
          tools/docs-lint.sh: line 30: declare: -A: invalid option
          declare: usage: declare [-afFirtx] [-p] [name[=value] ...]
          docs-lint: OK — 19 documents, 111 anchors, 1467 references, 98 beans, 37 graph edges, 45 selectable, 98 bean ids, 0 introduced, 98 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0
```

Replaced it with `mapfile -t docs < /dev/null` at the same line:

```
cmd:      ./gradlew docsLint
observed: > Task :docsLint
          tools/docs-lint.sh: line 30: mapfile: command not found
          docs-lint: OK — 19 documents, 111 anchors, 1467 references, 98 beans, 37 graph edges, 45 selectable, 98 bean ids, 0 introduced, 98 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0
```

Bash 3.2 wrote a diagnostic and carried on in both cases. `declare -A` silently left an
INDEXED array behind, which is the more dangerous of the two: a script that then subscripts it
by string reads element 0. **A real 3.2 interpreter does not reject either construct this bean
names.** Criterion 2 is unmet, and would remain unmet under any interpreter choice.

The counts on those two `docs-lint: OK` lines are a figure about a corpus this bean belongs to
(`doc:50-memory-and-evidence#corpus-figures`). They were captured before this bean's own edit
and are stale by exactly the edit that records them: moving `status: todo` to `in-progress`
takes `selectable` from 45 to 44, which is re-measured in entry 5. Nothing here rests on the
counts — the load-bearing observation is `exit: 0` beside the word `OK`.

### 3 — the mechanism that was built, observed rejecting both (a substitute, not criterion 2)

Same two plants, same lines, against `bashCompatLint`:

```
cmd:      ./gradlew bashCompatLint      # with declare -A seen at tools/docs-lint.sh:30
observed: > Task :bashCompatLint FAILED
          bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          tools/docs-lint.sh:30: associative-array: declare -A seen
          FAIL bash-compat  1 bash 4 construct(s) in scripts that claim bash 3.2 compatibility
          bash-compat: FAILED.
exit:     1

cmd:      ./gradlew bashCompatLint      # with mapfile -t docs < /dev/null at tools/docs-lint.sh:30
observed: > Task :bashCompatLint FAILED
          bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          tools/docs-lint.sh:30: mapfile: mapfile -t docs < /dev/null
          FAIL bash-compat  1 bash 4 construct(s) in scripts that claim bash 3.2 compatibility
          bash-compat: FAILED.
exit:     1
```

A third plant, `true |& cat`, shows the other half of the gate — the `-n` parse under the
pinned interpreter — firing on the same line, ahead of the scan:

```
cmd:      ./gradlew bashCompatLint      # with true |& cat at tools/docs-lint.sh:30
observed: FAIL bash-compat  tools/docs-lint.sh does not parse under /bin/bash (bash 3.2.57(1)-release): tools/docs-lint.sh: line 30: syntax error near unexpected token `&'
          tools/docs-lint.sh: line 30: `true |& cat'
          tools/docs-lint.sh:30: pipe-both-streams: true |& cat
exit:     1
```

Each plant was reverted with `git checkout -- tools/docs-lint.sh` before the next; the working
tree after the last revert differed from `HEAD` in nothing but `domains/modus/cost/0001.ndjson`,
the harness cost log, which was already dirty when the branch was cut. The plants were made
after the implementation was committed (`bean:0102`).

### 4 — the gate is silent on clean input, and says so when it stops being

Firing on every input is also firing (`doc:50-memory-and-evidence#evidence-kinds`), so the
scan re-proves its own discrimination on every invocation and three further plants were made
against that machinery rather than against the scripts:

```
plant:    one rule's regex widened to `.`, which matches every line
observed: FAIL bash-compat  rule 'nameref': its own sample violation was detected 2 times, not once — the sample is <local -n out=$1>
          ... 15 such lines, one per rule ...
          FAIL bash-compat  the negative control is not clean: 8 hit(s) on bash 3.2-legal source
exit:     1

plant:    tools/lib/bash32-scan.awk neutered, `if ($0 ~ re[i])` replaced by `if (0)`
observed: FAIL bash-compat  rule 'associative-array': its own sample violation was detected 0 times, not once — the sample is <declare -A seen>
          ... 16 such lines, one per rule ...
exit:     1

plant:    every rule row stripped from tools/lib/bash32-forbidden.tsv
observed: FAIL bash-compat  no rules loaded from tools/lib/bash32-forbidden.tsv; the scan would report every script clean
          bash32-scan: no patterns loaded from tools/lib/bash32-forbidden.tsv
exit:     1
```

The three cover the three ways this kind of gate dies: too wide, too narrow, and empty. All
three were reverted with `git checkout --` on the single file each touched.

### 5 — the whole gate, green on the restored tree

```
cmd:      ./gradlew bashCompatLint docsLint docsLintTest
observed: bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 3 scripts parsed, 16 rules, 16 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          docs-lint-test: 37 passed, 0 failed.
          docs-lint: OK — 19 documents, 111 anchors, 1471 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 98 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
exit:     0
```

That `docs-lint` line was captured **after** this bean was edited, so it is self-consistent with
the tree that holds it: the two figures that moved are exactly the ones this bean moved —
`references` 1467 to 1471, from the four typed references added below, and `selectable` 45 to 44,
from `status: todo` becoming `in-progress`. Nothing else changed, which is the measurement-neutral
step. It is stamped, not final: a sibling change to `documentation/` and to another bean is in
flight on this sprint, and whichever pull request merges second falsifies the other's counts.
Re-running this line belongs to that merge (`doc:50-memory-and-evidence#corpus-figures`).

### 6 — the pattern file is measured, not remembered

Every row of `tools/lib/bash32-forbidden.tsv` was run under both interpreters and kept only
where they disagreed on stdout, stderr or exit status. Six of the sixteen families diverge with
no error at all or with an error a script without `set -e` swallows, which is the reason the
scan exists beside the parse:

```
construct        bash 3.2.57                                    bash 5.3.9
declare -A m     declare: -A: invalid option, exit 0            m is associative
mapfile          mapfile: command not found, exit 0             reads the input
echo {1..9..2}   {1..9..2}                                      1 3 5 7 9
$SRANDOM         unset                                          set
$EPOCHSECONDS    unset                                          set
$EPOCHREALTIME   unset                                          set
```

And the complementary measurement, which is why the parse half is not the whole gate: of the
sixteen families, `/bin/bash -n` rejects five — `&>>`, `|&`, `;;&`, `coproc` and `[[ -v ]]`.
The other eleven, including both constructs criterion 2 names, parse cleanly under 3.2.

## What is deliberately not in this change

Nothing in `tools/lib/docs-lint-c14.awk` or `tools/lib/docs-lint-fence.awk` was touched, and
check 14's behaviour is unchanged. Three changes to that analyser are queued behind this one;
pinning the interpreter is what gives them something to be validated against, and arriving
early would defeat the ordering.

## Amendments

Entries from the review rounds on this bean's own pull request, #69. **Each entry names the
head its own evidence was taken at, because the blanket stamp this preamble used to carry —
"all from the first review round … against head `13d8c27`" — was not true of all of them.**
The two `./gradlew bashCompatLint` runs in the second entry cite
`tools/bash-compat-lint.sh:180-185`, and at `13d8c27` that file is 150 lines long
(`git show 13d8c27:tools/bash-compat-lint.sh | awk 'END { print NR }'`); the lines they name
are the extended fixture, which exists from `07ace1c`. That is the head those two runs were
taken at, and the entry now says so.

Every figure below was redirected to a file and pasted from it. **Two kinds of edit were made
to a capture, and from the third review round on both are marked `[...]`:** an elision of an
absolute path, and an elision of the scaffolding a tool prints around the lines under
discussion — Gradle's task list, its `BUILD SUCCESSFUL`/`BUILD FAILED` verdict, its failure
tail and its deprecation notice, and the body of a CI log around the four lines quoted from
it. The `exit:` line is stated separately in every block, so a trimmed build verdict removes
nothing a reader needs; the markers were added afterwards, in the third round, and no line
inside a capture was changed to add them.

### 2026-09-04 · bean:0049

*A citation to a differential this bean does not contain.*

**Claimed:** `tools/bash-compat-lint.sh:21-22` at `13d8c27` — "The full 23-construct
differential against bash 5.3.9 is in bean:0049."

**Found:** there is no 23-construct differential here and there never was; the figure 23
occurs nowhere in this file. What entry 6 carries is a six-row table of families that diverge
silently or behind a diagnostic, and one sentence recording that `/bin/bash -n` rejects five
of the sixteen. The pattern file carried sixteen rules at that head. **At `13d8c27`, 23 was a
number no run in this repository had produced**, sitting inside the file whose stated thesis is
that its list is measured rather than remembered
(`doc:50-memory-and-evidence#unevidenced-assertions`). The sentence now names entry 6 and the
third and fourth entries of this section by what each actually contains, and says in terms that
the file is a denylist of what was measured rather than a differential of anything.

The head bound on that claim is not decoration. The third review round took the pattern file to
23 rows, so `bashCompatLint` now prints `23 rules` on every run and 23 *is* a figure this
repository produces — by coincidence, and not as a differential of anything. Left unbound, this
finding would have read as satisfied by the very number it was raised about.

**Evidence:**

```
cmd:      git show 13d8c27:.beans/modus-0049--bash-32-claim-is-unenforced.md | grep -n '23'
observed: (no output)
exit:     1        # ugrep 7.8.4, the harness shim on the interactive PATH. /usr/bin/grep -c
                   # over the same content printed 0 and exited 1, so the two greps agree.
cmd:      git show 13d8c27:tools/lib/bash32-forbidden.tsv | awk -F'\t' '/^[ \t]*#/{next} /^[ \t]*$/{next} NF>=3 && $1!="" && $3!="" {n++} END {print n}'
observed: 16
```

### 2026-09-04 · bean:0049

*The SCAN half rejected legal bash 3.2, and the negative control could not see it.*

**Claimed:** entry 4 above — the scan "is silent on clean input", re-proved on every
invocation by a fixture of "3.2-legal near-misses" that must produce zero hits.

**Found:** the fixture was too small to be the control it claimed to be. Three of the sixteen
rules matched correct bash 3.2, and no line that would have exposed any of them was in the
fixture. `test-v` fired on `[[ -n "$(command -v jq)" ]]`, the standard portable
command-existence idiom, and on `-v` compared as a value; `case-modification` fired on
`${csv%,}` and `${list#,}`, which are suffix and prefix trimming and are not case modification
at all — that is `${v^^}` and `${v,,}`; `printf-time-format` fired on a literal `%%` before a
parenthesis. This is worse than the miss a denylist is admitted to have. A miss leaves the
gate where it already was; a gate that rejects correct code stops the next person's
legitimate work, and `bashCompatLint` runs inside `qualityCheck`. All three rules are now
anchored to the syntax that makes the construct a bash 4 construct, and every line is in the
negative control.

**Evidence:** the scanner run directly, at `13d8c27`, over a file holding just those five
lines. `[...]` is the scratchpad directory the fixture was written to:

```
cmd:      awk -v PAT=tools/lib/bash32-forbidden.tsv -f tools/lib/bash32-scan.awk [...]/fp.sh
observed: [...]/fp.sh:1: test-v: if [[ -n "$(command -v jq)" ]]; then :; fi
          [...]/fp.sh:2: test-v: if [[ "$flag" == -v ]]; then :; fi
          [...]/fp.sh:3: case-modification: trimmed="${csv%,}"
          [...]/fp.sh:4: case-modification: joined="${list#,}"
          [...]/fp.sh:5: printf-time-format: printf 'coverage %d%%(min)\n' 90
exit:     0
```

Then both halves of the fixture, through the gate — **at `07ace1c`, not `13d8c27`**: the
extended negative control was committed first, and the three pre-fix regexes planted back into
`tools/lib/bash32-forbidden.tsv` afterwards, so the two runs differ in the regex column and in
nothing else (`bean:0102`). The line numbers in the second block of hits are `07ace1c`'s, which
is how the head was recovered in the third round:

```
cmd:      ./gradlew bashCompatLint        # extended fixture, pre-fix regexes planted back
observed: [...]
          > Task :bashCompatLint FAILED
          bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          [...]/clean.sh:12: test-v: if [[ -n "$(command -v jq)" ]]; then :; fi
          [...]/clean.sh:13: test-v: if [[ "$flag" == -v ]]; then :; fi
          [...]/clean.sh:15: case-modification: trimmed="${csv%,}"
          [...]/clean.sh:16: case-modification: joined="${list#,}"
          [...]/clean.sh:17: printf-time-format: printf 'coverage %d%%(min)\n' 90
          FAIL bash-compat  the negative control is not clean: 5 hit(s) on bash 3.2-legal source
          tools/bash-compat-lint.sh:180: test-v: if [[ -n "$(command -v jq)" ]]; then :; fi
          tools/bash-compat-lint.sh:181: test-v: if [[ "$flag" == -v ]]; then :; fi
          tools/bash-compat-lint.sh:183: case-modification: trimmed="${csv%,}"
          tools/bash-compat-lint.sh:184: case-modification: joined="${list#,}"
          tools/bash-compat-lint.sh:185: printf-time-format: printf 'coverage %d%%(min)\n' 90
          FAIL bash-compat  5 bash 4 construct(s) in scripts that claim bash 3.2 compatibility
          bash-compat: FAILED.
          [...]
exit:     1

cmd:      ./gradlew bashCompatLint        # plant reverted with git checkout -- on that one file
observed: [...]
          bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 3 scripts parsed, 21 rules, 21 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [...]
exit:     0
```

The second block of five hits in the red run is the fixture found again as ordinary source:
the heredoc lives in `tools/bash-compat-lint.sh`, which the scan covers because it is a
`tools/*.sh`. The gate therefore fails twice over on the same defect, which is why the
negative control is a heredoc in a scanned file rather than a separate fixture.

### 2026-09-04 · bean:0049

*Five constructs absent from rows that already enumerate their siblings.*

**Claimed:** entry 6 — the list is measured, not remembered: every row was run under both
interpreters and kept only where they disagreed.

**Found:** that is true of every row present and says nothing about what is absent. `declare
-g`, `declare -l` and `declare -u` fail in the same builtin, through the same option parser,
as the marquee `declare -A`; `$BASHPID` sits in a row that lists three of its siblings and
omits it. In rows that enumerate, an absence reads as a decision. Eleven constructs were run
under both interpreters here and **all eleven diverge**, so five rows were added
(`declare-global`, `declare-lowercase`, `declare-uppercase`, `printf-v-subscript`,
`varfd-redirect`) and two rows gained alternatives (`shopt-bash4-option` gained `direxpand`,
`autocd`, `dirspell`, `checkjobs` and `globasciiranges`; `bash4-variable` gained `BASHPID`).

One correction to the finding as it reached this bean: it described `declare -g` as *silently*
leaving the variable unset. It is not silent. Bash 3.2 writes `declare: -g: invalid option` to
stderr and exits 0 — exactly the shape of `declare -A`. The word that fits is *swallowed*, and
what swallows it is this script's missing `set -e`. `$BASHPID` is the genuinely silent one of
the five.

Not proved by the gate, stated so it is not mistaken for proved: the machinery plants one
sample per row, so the five `shopt` options and `BASHPID` are covered by the differential
below and by nothing that re-runs. That was already true of `SRANDOM`, `EPOCHREALTIME` and
`BASH_ARGV0` before this change; it is the file's existing shape rather than a new concession,
and it is now written in the file.

**Evidence:** each snippet written to a file and run under both interpreters, comparing
stdout, stderr and exit status. `[...]` is the scratchpad path the snippet was written to.
`declare -A` is included last as a control: it is the construct entry 6 already measured, and
it must come out `DIVERGES` for the harness to be believed.

```
cmd:      /opt/homebrew/bin/bash probe.sh
observed: GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25)
          GNU bash, version 5.3.9(1)-release (aarch64-apple-darwin25.1.0)
          === declare -g [DIVERGES]
              src : f() { declare -g g_var=set; }; f; echo "g_var=[${g_var-UNSET}]"
              3.2 : rc=0 out=<g_var=[UNSET]> err=<[...]/snippet.sh: line 1: declare: -g: invalid option
          declare: usage: declare [-afFirtx] [-p] [name[=value] ...]>
              5.3 : rc=0 out=<g_var=[set]> err=<>
          === declare -l [DIVERGES]
              src : declare -l lower=ABC; echo "lower=[$lower]"
              3.2 : rc=0 out=<lower=[]> err=<[...]/snippet.sh: line 1: declare: -l: invalid option
          declare: usage: declare [-afFirtx] [-p] [name[=value] ...]>
              5.3 : rc=0 out=<lower=[abc]> err=<>
          === declare -u [DIVERGES]
              src : declare -u upper=abc; echo "upper=[$upper]"
              3.2 : rc=0 out=<upper=[]> err=<[...]/snippet.sh: line 1: declare: -u: invalid option
          declare: usage: declare [-afFirtx] [-p] [name[=value] ...]>
              5.3 : rc=0 out=<upper=[ABC]> err=<>
          === BASHPID [DIVERGES]
              src : echo "bashpid=[${BASHPID-UNSET}]"
              3.2 : rc=0 out=<bashpid=[UNSET]> err=<>
              5.3 : rc=0 out=<bashpid=[82846]> err=<>
          === shopt direxpand [DIVERGES]
              src : shopt -s direxpand; echo "rc=$?"
              3.2 : rc=0 out=<rc=1> err=<[...]/snippet.sh: line 1: shopt: direxpand: invalid shell option name>
              5.3 : rc=0 out=<rc=0> err=<>
          === shopt autocd [DIVERGES]
              src : shopt -s autocd; echo "rc=$?"
              3.2 : rc=0 out=<rc=1> err=<[...]/snippet.sh: line 1: shopt: autocd: invalid shell option name>
              5.3 : rc=0 out=<rc=0> err=<>
          === shopt dirspell [DIVERGES]
              src : shopt -s dirspell; echo "rc=$?"
              3.2 : rc=0 out=<rc=1> err=<[...]/snippet.sh: line 1: shopt: dirspell: invalid shell option name>
              5.3 : rc=0 out=<rc=0> err=<>
          === shopt checkjobs [DIVERGES]
              src : shopt -s checkjobs; echo "rc=$?"
              3.2 : rc=0 out=<rc=1> err=<[...]/snippet.sh: line 1: shopt: checkjobs: invalid shell option name>
              5.3 : rc=0 out=<rc=0> err=<>
          === shopt globasciiranges [DIVERGES]
              src : shopt -s globasciiranges; echo "rc=$?"
              3.2 : rc=0 out=<rc=1> err=<[...]/snippet.sh: line 1: shopt: globasciiranges: invalid shell option name>
              5.3 : rc=0 out=<rc=0> err=<>
          === printf -v subscript [DIVERGES]
              src : a=(x y); printf -v "a[0]" %s hi; echo "a0=[${a[0]}]"
              3.2 : rc=0 out=<a0=[x]> err=<[...]/snippet.sh: line 1: printf: `a[0]': not a valid identifier>
              5.3 : rc=0 out=<a0=[hi]> err=<>
          === exec {fd}< [DIVERGES]
              src : exec {fd}</dev/null; echo "fd=[${fd-UNSET}]"
              3.2 : rc=127 out=<> err=<[...]/snippet.sh: line 1: exec: {fd}: not found>
              5.3 : rc=0 out=<fd=[10]> err=<>
          === declare -A (control) [DIVERGES]
              src : declare -A m; m[k]=v; echo "m=[${m[k]}]"
              3.2 : rc=0 out=<m=[v]> err=<[...]/snippet.sh: line 1: declare: -A: invalid option
          declare: usage: declare [-afFirtx] [-p] [name[=value] ...]>
              5.3 : rc=0 out=<m=[v]> err=<>
exit:     0
```

`exec {fd}<` is the only one of the eleven that fails loudly — 3.2 exits 127. It is a row
anyway, because 127 is a status `tools/docs-lint.sh` never inspects; the entry below is why.
The `declare -A` control's stdout agrees under both shells and it diverges on stderr alone:
under 3.2 `m` is an INDEXED array and `m[k]` subscripts element 0, which happens to hold `v`.
That is the silent-wrong-answer shape entry 6 records, caught here by comparing all three
streams rather than stdout.

### 2026-09-04 · bean:0049

*Criterion 2 is ruled unmeetable; its concern goes to `bean:0118`; this bean does not close here.*

**Claimed:** the section above — "Whether that satisfies the intent of criterion 2 is a
decision for whoever owns this bean; it is not a decision the implementing agent may take by
amending the criterion."

**Found:** the owner ruled, and the ruling is that **criterion 2 is NOT MET because the
criterion was wrong, not because the work was.** It names *the interpreter* as the mechanism
that must reject a planted associative array or `mapfile`. No interpreter can: bash 3.2
diagnoses both on stderr and leaves the exit status at 0, and `tools/docs-lint.sh` has
`set -uo pipefail` and no `set -e`, so the script runs on to its `OK` line. Entry 2 measured
that; it was re-measured independently in review, pasted below, and the two agree. Criterion 1
stands as entry 1 records it — met on macOS, and on CI only in the weaker form stated there.
Criterion 3 does not apply: the claim was kept, not struck.

The concern criterion 2 was reaching for is real and is bigger than this bean. It is not that
bash 3.2 tolerates `declare -A`; it is that `tools/docs-lint.sh` reaches its `OK` line at exit
0 through nearly every runtime failure, its own analysers included, and that an entirely inert
check is invisible in its output. That is `bean:0118`, raised in this change with the boundary
measured. It is not a bash 3.2 problem and it does not belong here.

**This bean is NOT set `completed` in this change, and that is a considered departure from the
instruction that produced this amendment.** `doc:00-constitution#bean-lifecycle` §7.2.1 is
unconditional: a bean stays `in-progress` for the whole life of its own pull request,
*including through review*, and moves to `completed` in a separate change after the merge.
Both reasons it gives are live here. This bean's evidence includes the merge, which is the
thing #69 is asking for; and this amendment is itself an author's review fix, which is exactly
what a premature `completed` would have frozen the bean against — every entry above would have
had to be written as an amendment to a closed record instead of as one to an open one. The
precedence rule at the head of `doc:00-constitution` puts that file above anything an agent
says in conversation, so the instruction does not override it. Closing is the next change
after #69 merges, on criteria 1 and 3, with criterion 2 recorded NOT MET and this entry as its
reason.

**Evidence:** criterion 2's own two constructs, re-planted in review at
`tools/docs-lint.sh:29` — immediately after `set -uo pipefail`, which is at line 28 — and run
through the gate task whose interpreter criterion 2 is about:

```
cmd:      ./gradlew docsLint      # with `declare -A seen` at tools/docs-lint.sh:29
observed: [...]
          > Task :docsLint
          tools/docs-lint.sh: line 29: declare: -A: invalid option
          declare: usage: declare [-afFirtx] [-p] [name[=value] ...]
          docs-lint: OK — 19 documents, 111 anchors, 1471 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 100 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 17s
          [...]
exit:     0

cmd:      ./gradlew docsLint      # with `mapfile -t docs < /dev/null` at tools/docs-lint.sh:29
observed: [...]
          > Task :docsLint
          tools/docs-lint.sh: line 29: mapfile: command not found
          docs-lint: OK — 19 documents, 111 anchors, 1471 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 100 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 18s
          [...]
exit:     0
```

Both plants were made against a copy of `tools/docs-lint.sh` taken before the first one and
restored from that copy after each, with no `git` operation involved, so no other uncommitted
work in the tree could be discarded (`bean:0102`, `AGENTS.md`). `diff` against that copy after
the last restore reported the files identical.

Those counts are of a corpus this bean belongs to and are already stale: `100 on origin/main`
is **two** higher than entry 5's `98 on origin/main`, because `origin/main` moved while these
runs were in flight. (This sentence read "one higher than entry 5's `99`" until the third
review round. Entry 5's figure is `98`; `99` is `bean:0118`'s, and it occurs nowhere in this
file.) Nothing here rests on them — the load-bearing observation is `exit: 0` beside the word
`OK`, twice (`doc:50-memory-and-evidence#corpus-figures`). Entry 5's line is not re-taken
here; re-running it belongs to the merge, and two further pull requests that move the same
counts are in flight on this sprint.

### 2026-09-04 · bean:0049

*The anchored regexes under CI's awk, which is not the awk they were written against.*

**Claimed:** `tools/lib/bash32-scan.awk:8-11` — the pattern ERE "behaves the same under BSD awk
and gawk for the constructs used here — no backslash escapes, no interval expressions, no word
boundaries"; and entry 1, that CI's awk was observed to have the POSIX character classes
rather than assumed to.

**Found:** the anchoring added bracket-expression forms that neither observation covers — `[!]`
holding a literal `!` where `^` is the negation character, a positive `[]]` holding a literal
`]`, and `["']`. Those are exactly the shapes that differ between awks, so the claim needed
re-taking rather than inheriting. It could not be checked locally: no gawk, mawk or busybox
awk is installed on the development machine. It was checked on the runner instead, and all
twenty-one samples are still caught exactly once with the extended negative control still
clean. The observation is not load-bearing on its own — an awk that compiled the brackets
differently would fail the planted-sample assertions loudly rather than report every script
clean — but it is the only direct evidence that the widened patterns are portable.

**Evidence:**

```
cmd:      command -v gawk mawk busybox
observed: (no output)
exit:     1
cmd:      awk --version 2>&1 | head -1
observed: awk version 20200816            # BSD awk, what every local figure above was taken under
cmd:      gh run view --job 100960561182 --log      # qualityCheck on ubuntu-latest, head ea4185f
observed: [...]
          bash-compat: interpreter /bin/bash (bash 5.2.21(1)-release)
          bash-compat: OK — 3 scripts parsed, 21 rules, 21 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          docs-lint-test: 37 passed, 0 failed.
          docs-lint: OK — 19 documents, 111 anchors, 1520 references, 101 beans, 37 graph edges, 46 selectable, 101 bean ids, 1 introduced, 100 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
```

That `docs-lint` line reads `101 beans` where this tree measures `99`, because CI lints the
merge of this branch with `origin/main` and `origin/main` has gained beans since this branch
was cut. It is the corpus moving under the figure, not two measurements disagreeing
(`doc:50-memory-and-evidence#corpus-figures`). The `bash-compat` line is unaffected: it counts
rules and scripts, neither of which the merge changes.

### 2026-09-04 · bean:0049

*Three more rows rejecting legal bash 3.2, and the residue that is left rejected on purpose.*

**Claimed:** the entry *The SCAN half rejected legal bash 3.2, and the negative control could
not see it* — "All three rules are now anchored to the syntax that makes the construct a bash 4
construct, and every line is in the negative control."

**Found:** that anchoring pass looked at the three rows review had demonstrated and at no
others. Three further rows reject legal bash 3.2, and one of the three is
`printf-time-format` — a row the pass had **already anchored once**, and beside the fix it
shipped two fresh instances of the same defect:

| row | rejected | what the pre-fix regex could not tell |
|---|---|---|
| `brace-expansion-step` | `cp {../src,../dst} .`, `for d in {../x,../y}; do :; done` | two `..` in one brace LIST from the `{1..9..2}` step form |
| `printf-time-format` | `printf 'coverage %d%%(min)T\n' 90` | a `%` introducing a format from the second half of a `%%` |
| `printf-time-format` | `printf 'see %s\n' "use %(%Y)T for time"` | the format string from a later ARGUMENT — `[^#]*` crossed the closing quote |
| `varfd-redirect` | `echo "{div}<br>"`, `sed 's/{x}<//' f` | a `{name}` redirection from a brace-delimited placeholder inside a word |
| `parameter-transform` | `echo "${email:-me@u}"` | `${v@Q}` from an `@` inside a DEFAULT value — the same shape as `${csv%,}`, which the previous round classed a defect worth blocking on |

All of them are anchored now, and all seven lines are in the negative control, which was
extended **first** and observed failing before any regex moved.

**Two rows are deliberately NOT anchored, and that is now written down rather than left to be
found.** `pipe-both-streams` rejects `printf '%s\n' 'a|&b'` and `case-fallthrough` rejects
`url="http://x/?a=1;&b=2"`. Both lines are legal bash 3.2. Outside a string, `|&` and `;&` are
unambiguously the operator, so there is no syntactic position to anchor to — only a lexical
one, and a scanner that tracked quoting would be a shell lexer. Requiring trailing whitespace
was considered and rejected: it would still reject `'a |& b'` and would miss `run |&tee out`,
buying a real miss for no real coverage. `tools/lib/bash32-scan.awk` and
`tools/lib/bash32-forbidden.tsv` both now state plainly that a legal construct inside a string
literal or a trailing comment will be rejected, and that the way past it is to move the literal
out of the line.

**Evidence.** First the nine lines through the scanner at `91a3c23`, the head review read.
`[...]` is the scratchpad directory the fixture was written to:

```
cmd:      awk -v PAT=tools/lib/bash32-forbidden.tsv -f tools/lib/bash32-scan.awk [...]/fp3.sh
observed: [...]/fp3.sh:1: brace-expansion-step: cp {../src,../dst} .
          [...]/fp3.sh:2: brace-expansion-step: for d in {../x,../y}; do :; done
          [...]/fp3.sh:3: printf-time-format: printf 'coverage %d%%(min)T\n' 90
          [...]/fp3.sh:4: printf-time-format: printf 'see %s\n' "use %(%Y)T for time"
          [...]/fp3.sh:5: varfd-redirect: echo "{div}<br>"
          [...]/fp3.sh:6: varfd-redirect: sed 's/{x}<//' f
          [...]/fp3.sh:7: pipe-both-streams: printf '%s\n' 'a|&b'
          [...]/fp3.sh:8: case-fallthrough: url="http://x/?a=1;&b=2"
          [...]/fp3.sh:9: parameter-transform: echo "${email:-me@u}"
exit:     0
```

Then the same nine under both interpreters, to establish they are legal 3.2 rather than
assumed to be. Each snippet was written to a file and run under `/bin/bash` and
`/opt/homebrew/bin/bash`, comparing stdout, stderr and exit status; the harness prints
`IDENTICAL` only when all three agree, and a construct already known to diverge is included
last so the harness itself is falsifiable. This capture does not depend on the tree:

```
cmd:      /opt/homebrew/bin/bash probe3.sh
observed: GNU bash, version 3.2.57(1)-release (arm64-apple-darwin25)
          GNU bash, version 5.3.9(1)-release (aarch64-apple-darwin25.1.0)
          === brace list with two .. [IDENTICAL]
              src : echo {../src,../dst}
              3.2 : rc=0 out=<../src ../dst> err=<>
              5.3 : rc=0 out=<../src ../dst> err=<>
          === for over brace list [IDENTICAL]
              src : for d in {../x,../y}; do echo "d=$d"; done
              3.2 : rc=0 out=<d=../x
          d=../y> err=<>
              5.3 : rc=0 out=<d=../x
          d=../y> err=<>
          === printf doubled %%(T [IDENTICAL]
              src : printf "coverage %d%%(min)T\n" 90
              3.2 : rc=0 out=<coverage 90%(min)T> err=<>
              5.3 : rc=0 out=<coverage 90%(min)T> err=<>
          === printf %(..)T in arg [IDENTICAL]
              src : printf "see %s\n" "use %(%Y)T for time"
              3.2 : rc=0 out=<see use %(%Y)T for time> err=<>
              5.3 : rc=0 out=<see use %(%Y)T for time> err=<>
          === brace placeholder str [IDENTICAL]
              src : echo "{div}<br>"
              3.2 : rc=0 out=<{div}<br>> err=<>
              5.3 : rc=0 out=<{div}<br>> err=<>
          === brace in sed script [IDENTICAL]
              src : echo "{x}<y" | sed "s/{x}<//"
              3.2 : rc=0 out=<y> err=<>
              5.3 : rc=0 out=<y> err=<>
          === pipe-amp inside string [IDENTICAL]
              src : printf '%s\n' 'a|&b'
              3.2 : rc=0 out=<a|&b> err=<>
              5.3 : rc=0 out=<a|&b> err=<>
          === semi-amp inside string [IDENTICAL]
              src : url="http://x/?a=1;&b=2"; echo "$url"
              3.2 : rc=0 out=<http://x/?a=1;&b=2> err=<>
              5.3 : rc=0 out=<http://x/?a=1;&b=2> err=<>
          === at-u inside default [IDENTICAL]
              src : echo "${email:-me@u}"
              3.2 : rc=0 out=<me@u> err=<>
              5.3 : rc=0 out=<me@u> err=<>
          [... the four `-v` rows, which are the next entry's evidence ...]
          === declare -A (control) [DIVERGES]
              src : declare -A m; m[k]=v; echo "m=[${m[k]}]"
              3.2 : rc=0 out=<m=[v]> err=<[...]/snippet3.sh: line 1: declare: -A: invalid option
          declare: usage: declare [-afFirtx] [-p] [name[=value] ...]>
              5.3 : rc=0 out=<m=[v]> err=<>
exit:     0
```

Then the gate, both halves, at `caf95db` — the head at which this round froze
`tools/lib/bash32-forbidden.tsv` and `tools/bash-compat-lint.sh`, so the line numbers below are
the ones a reader re-running the gate will see. The extended negative
control and the anchored regexes were committed together, and the four pre-anchoring regexes
were planted back afterwards against a pristine `cp` of the pattern file, so the two runs
differ in the regex column and in nothing else, and the line numbers in the second block of
hits are the committed file's (`bean:0102`, `AGENTS.md` — no `git checkout` was used):

```
cmd:      ./gradlew bashCompatLint        # pre-anchoring regexes planted back at caf95db
observed: [...]
          > Task :bashCompatLint FAILED
          bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          [...]/clean.sh:20: brace-expansion-step: cp {../src,../dst} .
          [...]/clean.sh:21: brace-expansion-step: for d in {../x,../y}; do :; done
          [...]/clean.sh:22: printf-time-format: printf 'coverage %d%%(min)T\n' 90
          [...]/clean.sh:23: printf-time-format: printf 'see %s\n' "use %(%Y)T for time"
          [...]/clean.sh:24: varfd-redirect: echo "{div}<br>"
          [...]/clean.sh:25: varfd-redirect: sed 's/{x}<//' f
          [...]/clean.sh:26: parameter-transform: echo "${email:-me@u}"
          FAIL bash-compat  the negative control is not clean: 7 hit(s) on bash 3.2-legal source
          tools/bash-compat-lint.sh:214: brace-expansion-step: cp {../src,../dst} .
          tools/bash-compat-lint.sh:215: brace-expansion-step: for d in {../x,../y}; do :; done
          tools/bash-compat-lint.sh:216: printf-time-format: printf 'coverage %d%%(min)T\n' 90
          tools/bash-compat-lint.sh:217: printf-time-format: printf 'see %s\n' "use %(%Y)T for time"
          tools/bash-compat-lint.sh:218: varfd-redirect: echo "{div}<br>"
          tools/bash-compat-lint.sh:219: varfd-redirect: sed 's/{x}<//' f
          tools/bash-compat-lint.sh:220: parameter-transform: echo "${email:-me@u}"
          FAIL bash-compat  7 bash 4 construct(s) in scripts that claim bash 3.2 compatibility
          bash-compat: FAILED.
          [...]
exit:     1

cmd:      ./gradlew bashCompatLint        # pattern file restored from the pristine cp
observed: [...]
          > Task :bashCompatLint
          bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 3 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [...]
exit:     0

cmd:      diff [...]/tsv.pristine tools/lib/bash32-forbidden.tsv
observed: (no output)
exit:     0
```

The second block of hits in the red run is the fixture found again as ordinary source, for the
reason the previous entry gives: the heredoc lives in a `tools/*.sh`, so the gate fails twice
over on the same defect.

And the residue, so the two rows left rejecting correct code are observed and not merely
described. Same command, same file, at `caf95db`:

```
cmd:      awk -v PAT=tools/lib/bash32-forbidden.tsv -f tools/lib/bash32-scan.awk [...]/fp3.sh
observed: [...]/fp3.sh:7: pipe-both-streams: printf '%s\n' 'a|&b'
          [...]/fp3.sh:8: case-fallthrough: url="http://x/?a=1;&b=2"
exit:     0
```

### 2026-09-04 · bean:0049

*The `-v` positions are three, not one, and two of them were uncaught.*

**Claimed:** `tools/lib/bash32-forbidden.tsv` — the three anchored rows "now require the syntax
that makes the construct a bash 4 construct — `-v` directly after `[[`, `&&` or `||`".

**Found:** that sentence claims a complete set of positions and is not one. `-v` is a bash 4.2
test operator wherever a test operator can appear, and that is `[[ … ]]`, `[ … ]` and `test`.
Neither `[ -v name ]` nor `test -v name` was caught, by the old regex or the anchored one —
both required `[[`. Both diverge, and **neither is caught by the parse half either**:
`/bin/bash -n` accepts both at rc 0, because `[` and `test` are builtins and the divergence is
at run time. The scan is the only half that can reach them.

They are two new rows, `test-v-bracket` and `test-v-command`, rather than two alternatives
added to `test-v`. The machinery plants one sample per row, so two rows are two proofs and two
alternatives would have been none — which is the concession the last entry of this section is
about. The three regexes are mutually exclusive by construction, and the planted-sample
assertion re-checks that on every run: `bash-compat: OK — … 23 planted violations each caught
exactly once` is the observation, and it fails loudly if any two rules overlap.

The reviewer's summary of the divergence was "3.2: `[: -v: unary operator expected`, rc 2; bash
5: rc 0". The rc 2 is the test's status, not the script's — the script exits 0 either way,
which is the whole reason this file exists — and bash 5 returns 0 only when the name is SET.
Unset, it returns 1. Both differ from 3.2, so the row stands, but the figure is corrected here
rather than carried.

**Evidence.** The four `-v` rows of the same probe run as the previous entry, elided there and
pasted here. `[...]` is the scratchpad path each snippet was written to:

```
cmd:      /opt/homebrew/bin/bash probe3.sh
observed: [... the nine Group A rows, in the previous entry ...]
          === single-bracket -v set [DIVERGES]
              src : name=x; [ -v name ]; echo "rc=$?"
              3.2 : rc=0 out=<rc=2> err=<[...]/snippet3.sh: line 1: [: -v: unary operator expected>
              5.3 : rc=0 out=<rc=0> err=<>
          === single-bracket -v unset [DIVERGES]
              src : [ -v nope ]; echo "rc=$?"
              3.2 : rc=0 out=<rc=2> err=<[...]/snippet3.sh: line 1: [: -v: unary operator expected>
              5.3 : rc=0 out=<rc=1> err=<>
          === test -v set [DIVERGES]
              src : name=x; test -v name; echo "rc=$?"
              3.2 : rc=0 out=<rc=2> err=<[...]/snippet3.sh: line 1: test: -v: unary operator expected>
              5.3 : rc=0 out=<rc=0> err=<>
          === test -v unset [DIVERGES]
              src : test -v nope; echo "rc=$?"
              3.2 : rc=0 out=<rc=2> err=<[...]/snippet3.sh: line 1: test: -v: unary operator expected>
              5.3 : rc=0 out=<rc=1> err=<>
          [... the `declare -A` control, in the previous entry ...]
exit:     0
```

And the parse half declining both, which is why a row is the only way to catch them:

```
cmd:      /bin/bash -n vb.sh      # one line: [ -v name ]
observed: (no output)
exit:     0
cmd:      /bin/bash -n vc.sh      # one line: test -v name
observed: (no output)
exit:     0
cmd:      /bin/bash -n v.sh       # one line: if [[ -v opt ]]; then :; fi — the row that IS a parse error
observed: v.sh: line 1: conditional binary operator expected
          v.sh: line 1: syntax error near `opt'
          v.sh: line 1: `if [[ -v opt ]]; then :; fi'
exit:     2
```

`case-modification` and `parameter-transform` both admit a family they cannot reach:
`${arr[${a[0]}]^^}` and `${arr[${a[0]}]@Q}` are not caught, because the `[^]]*` spanning the
subscript cannot cross the inner `]`. That is a miss and not a false positive, so it is named
in the pattern file beside the other named misses rather than fixed here.

### 2026-09-04 · bean:0049

*Three self-instances: this change fixed a defect and shipped the same defect beside it.*

**Claimed:** three sentences of this bean's own record.

1. Above, in *Criterion 2 is ruled unmeetable* — "`100 on origin/main` is one higher than entry
   5's `99`".
2. `bean:0118` — "It reaches beyond `tools/docs-lint.sh`, measured at `13d8c27`", above a
   `grep -n 'set -e'` returning two hits, with prose reading "the two hits are prose about its
   absence".
3. This section's preamble — "The only edits to a capture are elisions of an absolute path,
   each marked `[...]`."

**Found:** each is an instance of the defect class the amendment beside it was raised to fix.

1. Entry 5's figure is `98 on origin/main`, not `99`. `99` occurs nowhere in this file; it is
   `bean:0118`'s figure, read across from the wrong record. And `100 - 98` is **two**. This is
   the first entry of this section — a citation to a figure that occurs nowhere in the file it
   cites — committed inside the fix for it.
2. At `13d8c27` that command returns **one** hit. `tools/bash-compat-lint.sh:23` was introduced
   by `07ace1c`, so the capture was taken at `07ace1c` or later and stamped at a head where it
   cannot be reproduced — which is the entire function of a stamp. Nothing rests on it: the
   conclusion holds at every head on this branch. `bean:0118` is re-stamped to `07ace1c`, with
   the counter-evidence beside it.
3. The captures do not show that discipline. Four Gradle captures in this section were trimmed
   three different ways, and none of the trims was marked: the red `bashCompatLint` run kept
   `> Task :bashCompatLint FAILED` and dropped Gradle's failure tail; the green one dropped
   both the `> Task` line and `BUILD SUCCESSFUL`; the two `docsLint` runs kept both. A fifth,
   a `gh run view --log`, elided an entire CI log around four quoted lines. Nothing
   load-bearing was removed and `exit:` is stated separately in every block — but the preamble
   asserted a discipline the captures did not show. Every trim is now marked `[...]` and the
   preamble states what the marker stands for, rather than being softened to fit the gap.

**And the rest of the sweep**, because fixing only the cross-reference that was handed over
would repeat the defect a fourth time. Every cross-reference in both beans was re-derived from
its source, not re-read from the prose. What moved:

- `tools/docs-lint.sh:6`, the locator this bean opens with, is `:7-8` on `origin/main` — the
  quoted sentence wraps across two lines. It was off by one when the bean was raised and stayed
  off by one through four rounds, being a locator with no command beside it
  (`doc:50-memory-and-evidence#unverified-shapes`). It now carries its command.
- The two `./gradlew bashCompatLint` runs in the second entry of this section were stamped
  `13d8c27` by the preamble. They cite `tools/bash-compat-lint.sh:180-185`, and at `13d8c27`
  that file is 150 lines long. They were taken at `07ace1c`, and the entry now says so.
- Entry 2's "Planted `declare -A seen` at `tools/docs-lint.sh:30`, immediately after
  `set -uo pipefail`" — `set -uo pipefail` is at line 28 and line 29 is blank. Two lines after,
  not immediately after; both that entry's `line 30` and the later entry's `line 29` are what
  the runs printed.
- `bean:0118`'s `tools/bash-compat-lint.sh:116-194` is `119-229` at `caf95db`, after this round extended the
  fixture. It now carries its command, for the reason the first bullet gives.
- The first entry of this section says "23 is a number no run in this repository produced".
  This round took the pattern file to 23 rows, so `bashCompatLint` now prints `23 rules`. The
  sentence is bound to `13d8c27`, where it is true and checkable; left unbound it would have
  read as satisfied by the very figure it was raised about.

What was re-derived and held: `tools/bash-compat-lint.sh:21-22` at `13d8c27`; `16 rules` at
`13d8c27` and `21` at `07ace1c`; the scanner reproducing the round-1 false positives at
`13d8c27`; `/bin/bash -n` rejecting `[[ -v ]]`; `bean:0118`'s `awk_invocations=22` at `13d8c27`
and unchanged at this head, its `6 insertions(+), 3 deletions(-)`, and its citations of
`tools/docs-lint.sh` at `:460-479`, `:463`, `:459`, `:668-670`, `:707-708` and line 660;
`99 beans` on this tree; and `tools/lib/bash32-scan.awk:8-11`, which this round's edit to that
file inserts below and therefore does not move.

**Evidence:**

```
cmd:      git show 13d8c27:tools/bash-compat-lint.sh | /usr/bin/grep -n 'set -e'
observed: 19:# `mapfile` writes `command not found` and exits 0 from the script, which has no `set -e`.
cmd:      git show 07ace1c:tools/bash-compat-lint.sh | /usr/bin/grep -n 'set -e'
observed: 19:# `mapfile` writes `command not found` and exits 0 from the script, which has no `set -e`.
          23:# table is the six families that diverge silently or behind a diagnostic `set -e` would
cmd:      git show 13d8c27:tools/bash-compat-lint.sh | awk 'END { print NR }'
observed: 150
cmd:      git show 07ace1c:tools/bash-compat-lint.sh | awk 'END { print NR }'
observed: 210
cmd:      git show origin/main:tools/docs-lint.sh | awk 'NR == 6 || NR == 7 || NR == 8'
observed: # and locally, and a JavaExec task would need a source set, a toolchain and a test
          # fixture to do the same string matching. No bash 4 feature is used (macOS ships
          # 3.2), and every failure is appended to one file so a check that fires inside a
cmd:      /usr/bin/grep -n 'set -uo pipefail' tools/docs-lint.sh
observed: 28:set -uo pipefail
cmd:      /usr/bin/grep -n 'n_planted=0' tools/bash-compat-lint.sh
observed: 119:n_planted=0
```

`grep` above is `/usr/bin/grep`, BSD grep 2.6.0-FreeBSD, named because the harness's
interactive `grep` is a `ugrep 7.8.4` shim and a figure that depends on which one answered is
not a figure (`bean:0115`).

### 2026-09-04 · bean:0049

*What the one-sample-per-row concession costs, and why no mechanism was added in a third round.*

**Claimed:** `tools/lib/bash32-forbidden.tsv` — "A row's ALTERNATIVES are not each planted —
one sample per row is what the machinery checks. … That is the file's existing shape, not a new
concession."

**Found:** true, and it stops one sentence short of the consequence. The guarantee the file
rests on is that "a row whose sample stops matching, or starts matching two rules, fails the
gate here rather than quietly widening or narrowing it". That covers the arm the sample
exercises and nothing else. The rows name substantially more constructs than there are samples:
twenty-three samples, one per row, against alternations that between them name `typeset` and
`local` five times over, `readarray`, `,`, eight `@` operators, `;&`, a character brace range,
`{fd}>`, the `&&` and `||` `-v` positions, a bare `a[0]`, seven `shopt` options and four
variables. A typo in any of those would stop enforcing that construct with the gate still
green, which is the exact failure mode `doc:00-constitution#observed-failing` tabulates.

The list is short enough to name, so it is named in the file rather than counted: which arm
each row proves, and what it therefore does not. `typeset` is proved by no row at all.

**The mechanism was declined, deliberately.** A fourth column of extra samples, or one row per
construct, would close this; the two `-v` rows added this round are the second shape working.
Doing it for every alternation means a machinery change to `tools/bash-compat-lint.sh` in a
third review round on a change that is already large, and a mechanism added at that point is
less trustworthy than a limitation stated at it — the reviewer offered exactly this trade and
this is the side taken. The consequence is written in the file, beside the concession, where
the next person to widen an alternation will read it.

**Evidence:** the file itself, at `caf95db`, and the gate's own line, which is the figure the
consequence is about:

```
cmd:      ./gradlew bashCompatLint
observed: [...]
          bash-compat: OK — 3 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [...]
exit:     0
```

Twenty-three planted violations for twenty-three rows is the whole of what re-runs. Everything
else the rows name was measured once and is recorded, and the difference between those two
things is what this entry exists to state.

### 2026-09-04 · bean:0049

*The gate, green on the tree this round leaves behind.*

**Claimed:** entry 5 — the whole gate green, at a head five entries ago, with counts it says
are "stamped, not final" and that two further pull requests would move.

**Found:** the counts have moved, as entry 5 predicted, and the gate is still green. This is
the re-take at the head of the third review round, not a replacement for entry 5: entry 5's
line stays where it is, and re-running *it* still belongs to the merge.

The `bash-compat` line is the one this round is about and is corpus-independent — it counts
rules and scripts. The `docs-lint` line is a figure about a corpus this bean belongs to
(`doc:50-memory-and-evidence#corpus-figures`), and the measurement is neutral at this step: the
line was captured, pasted here, and the gate re-run, and the counts were identical across the
paste, because a `docs-lint` counts line carries no typed reference for check 6 to find.

**Evidence:**

```
cmd:      ./gradlew qualityCheck
observed: [...]
          docs-lint-test: 37 passed, 0 failed.
          [...]
          bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 3 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [...]
          docs-lint: OK — 19 documents, 111 anchors, 1489 references, 99 beans, 37 graph edges, 45 selectable, 99 bean ids, 1 introduced, 101 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          [...]
          BUILD SUCCESSFUL in 20s
          [...]
exit:     0
```

The three lines are in the order this run printed them, which is not the order the tasks are
declared in: Gradle's scheduling put `docsLintTest` ahead of `bashCompatLint` here and behind it
on the previous run. An `[...]` between two lines of a capture marks omitted output, not
adjacency, and never a sequence chosen by the author.

### 2026-09-04 · bean:0049

*The third round's bracket forms under CI's awk, which the previous round's observation does not cover.*

**Claimed:** the entry *The anchored regexes under CI's awk* — all twenty-one samples caught
exactly once on the runner, with the negative control clean, so the widened patterns are
portable.

**Found:** that observation was taken at `ea4185f` and covers the bracket forms that existed
there — `[!]`, a positive `[]]`, and `["']`. This round added shapes it does not cover, and
they are the shapes most likely to differ between awks: `[^-[:alnum:]_./[]`, a NEGATED class
holding a literal `[` with the `-` placed first so it is literal too; `[^#'"]` and `[^"'%]`,
negated classes holding both quote characters. A claim carried forward from an observation
that predates the thing it is claimed about is a hypothesis, not a citation
(`doc:50-memory-and-evidence#primary-sources`), so it was re-taken rather than inherited. It
still could not be checked locally — no gawk, mawk or busybox awk on the development machine,
which the previous entry recorded and which has not changed — so it was checked on the runner.
All twenty-three samples are caught exactly once under CI's awk and the extended negative
control is still clean.

As before this is not load-bearing on its own: an awk that compiled the brackets differently
would fail the planted-sample assertions loudly rather than report every script clean. It is
the only direct evidence that this round's patterns are portable.

**Evidence:**

```
cmd:      gh run view --job 100977630618 --log      # qualityCheck on ubuntu-latest, head 9de5401
observed: [...]
          bash-compat: interpreter /bin/bash (bash 5.2.21(1)-release)
          bash-compat: OK — 3 scripts parsed, 23 rules, 23 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          [...]
          docs-lint-test: 37 passed, 0 failed.
          [...]
          docs-lint: OK — 19 documents, 111 anchors, 1530 references, 101 beans, 37 graph edges, 45 selectable, 101 bean ids, 1 introduced, 100 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          [...]
cmd:      gh pr checks 69      # tab-separated; the trailing URL column is [...]
observed: build + mechanical gates	pass	59s	[...]
          build + mechanical gates	pass	1m4s	[...]
          gate	pass	2s	[...]
          gate	pass	4s	[...]
          backoffice + e2e	skipping	0	[...]
          backoffice + e2e	skipping	0	[...]
          which halves	pass	6s	[...]
          which halves	pass	4s	[...]
cmd:      gh run view 33858610317 --json headSha,conclusion --jq '{headSha,conclusion}'
observed: {"conclusion":"success","headSha":"9de540100d415ae218e6cb5013f3a71100c7c508"}
cmd:      gh run view 33858614475 --json headSha,conclusion --jq '{headSha,conclusion}'
observed: {"conclusion":"success","headSha":"9de540100d415ae218e6cb5013f3a71100c7c508"}
```

Each check appears twice because two workflow runs were triggered for the same head; the two
`gh run view` lines are there so the pair is not read as two different heads agreeing.

That `docs-lint` line reads `101 beans` and `1530 references` where the local run above reads
`99` and fewer, for the reason the previous entry gives: CI lints the merge of this branch with
`origin/main`, and `origin/main` has moved. The `bash-compat` line is unaffected — it counts
rules and scripts, neither of which the merge changes — and it is the line this entry is about.
The `gate` job is the one `bean:0047` is holding back a required-status rule for; it is green
here, on a real pull request, which is the observation that unblocks it.
