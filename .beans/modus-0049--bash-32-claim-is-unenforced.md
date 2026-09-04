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
