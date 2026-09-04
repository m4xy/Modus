---
# modus-0049
title: docs-lint claims bash 3.2 compatibility and nothing tests it
status: in-progress
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# `docs-lint` claims bash 3.2 compatibility and nothing tests it

`tools/docs-lint.sh:6` states, as a constraint on everyone who edits it:

> No bash 4 feature is used (macOS ships 3.2)

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

Planted `declare -A seen` at `tools/docs-lint.sh:30`, immediately after `set -uo pipefail`, and
ran the gate task whose interpreter is the one criterion 2 is about:

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

Five entries, all from the first review round on this bean's own pull request, #69, against
head `13d8c27`; the last is taken on the head that round produced. Every figure below was
redirected to a file and pasted from it. The only edits to a capture are elisions of an
absolute path, each marked `[...]`.

### 2026-09-04 · bean:0049

*A citation to a differential this bean does not contain.*

**Claimed:** `tools/bash-compat-lint.sh:21-22` at `13d8c27` — "The full 23-construct
differential against bash 5.3.9 is in bean:0049."

**Found:** there is no 23-construct differential here and there never was; the figure 23
occurs nowhere in this file. What entry 6 carries is a six-row table of families that diverge
silently or behind a diagnostic, and one sentence recording that `/bin/bash -n` rejects five
of the sixteen. The pattern file carried sixteen rules. 23 is a number no run in this
repository produced, sitting inside the file whose stated thesis is that its list is measured
rather than remembered (`doc:50-memory-and-evidence#unevidenced-assertions`). The sentence now
names entry 6 and the third entry below by what each actually contains, and says in terms that
the file is a denylist of what was measured rather than a differential of anything.

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

Then both halves of the fixture, through the gate. The extended negative control was
committed first, and the three pre-fix regexes planted back into
`tools/lib/bash32-forbidden.tsv` afterwards, so the two runs differ in the regex column and in
nothing else (`bean:0102`):

```
cmd:      ./gradlew bashCompatLint        # extended fixture, pre-fix regexes planted back
observed: > Task :bashCompatLint FAILED
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
exit:     1

cmd:      ./gradlew bashCompatLint        # plant reverted with git checkout -- on that one file
observed: bash-compat: interpreter /bin/bash (bash 3.2.57(1)-release)
          bash-compat: OK — 3 scripts parsed, 21 rules, 21 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
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
observed: > Task :docsLint
          tools/docs-lint.sh: line 29: declare: -A: invalid option
          declare: usage: declare [-afFirtx] [-p] [name[=value] ...]
          docs-lint: OK — 19 documents, 111 anchors, 1471 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 100 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 17s
exit:     0

cmd:      ./gradlew docsLint      # with `mapfile -t docs < /dev/null` at tools/docs-lint.sh:29
observed: > Task :docsLint
          tools/docs-lint.sh: line 29: mapfile: command not found
          docs-lint: OK — 19 documents, 111 anchors, 1471 references, 98 beans, 37 graph edges, 44 selectable, 98 bean ids, 0 introduced, 100 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
          BUILD SUCCESSFUL in 18s
exit:     0
```

Both plants were made against a copy of `tools/docs-lint.sh` taken before the first one and
restored from that copy after each, with no `git` operation involved, so no other uncommitted
work in the tree could be discarded (`bean:0102`, `AGENTS.md`). `diff` against that copy after
the last restore reported the files identical.

Those counts are of a corpus this bean belongs to and are already stale: `100 on origin/main`
is one higher than entry 5's `99`, because `origin/main` moved while these runs were in
flight. Nothing here rests on them — the load-bearing observation is `exit: 0` beside the word
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
observed: bash-compat: interpreter /bin/bash (bash 5.2.21(1)-release)
          bash-compat: OK — 3 scripts parsed, 21 rules, 21 planted violations each caught exactly once, 0 hits on the negative control, 0 findings.
          docs-lint-test: 37 passed, 0 failed.
          docs-lint: OK — 19 documents, 111 anchors, 1520 references, 101 beans, 37 graph edges, 46 selectable, 101 bean ids, 1 introduced, 100 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
```

That `docs-lint` line reads `101 beans` where this tree measures `99`, because CI lints the
merge of this branch with `origin/main` and `origin/main` has gained beans since this branch
was cut. It is the corpus moving under the figure, not two measurements disagreeing
(`doc:50-memory-and-evidence#corpus-figures`). The `bash-compat` line is unaffected: it counts
rules and scripts, neither of which the merge changes.
