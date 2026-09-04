#!/usr/bin/env bash
# docs-lint-gate-test — does tools/docs-lint.sh go RED when one of its analysers dies?
#
# THE DEFECT (bean:0118). tools/docs-lint.sh runs awk twenty-two times and inspected the
# exit status of exactly one of them. An analyser destroyed by a syntax error wrote
# nothing, the loop that read it found nothing, no `fail` fired, and the gate printed its
# `OK` line at exit 0 with stdout BYTE-IDENTICAL to the clean run. `set -u` is the only
# fail-closed mechanism in that file and it is fail-closed only in the TOP-LEVEL shell;
# no analyser runs there.
#
# WHY THIS IS NOT IN tools/docs-lint-test.sh, which is the other half of docs-lint's tests.
# The subject differs: that file feeds fixtures to the two awk libraries under tools/lib/,
# and this one can only observe the SHELL script by running the whole gate over the whole
# repository. Keeping them apart also keeps that file's mutation figures — "39 passed, 12
# failed" and its siblings — measurements of the suite they were measured against. Its own
# header requires every one of them to be re-measured whenever an assertion is added there,
# and each re-measurement would then carry two full gate runs it has no use for.
#
# THE PROBES ARE COPIES, never edits to the tracked file. docs-lint.sh derives its root
# from `dirname "$0"/..`, so a copy has to sit in tools/ to see the repository at all; the
# leading dot keeps it out of `ls tools/*.sh`, which is how tools/bash-compat-lint.sh
# chooses its targets. Nothing here writes to a tracked file, so there is no restore step
# to skip and no `git checkout` runs anywhere near .beans or tools (bean:0102, AGENTS.md).
#
# BOTH HALVES RUN, ALWAYS. A mechanism observed firing and never observed silent is not
# discrimination (doc:50-memory-and-evidence#evidence-kinds). The mutated copy must go red
# AND the unmutated copy at the same path must go green — the control is what says the
# copying, and not the mutation, is not what turns the gate red. The plant itself is
# asserted to have landed before either runs, because a probe harness that quietly stopped
# planting would report every row passing.
#
# WHAT THIS FILE DOES NOT COVER, stated because the assertions below would otherwise imply
# it. One analyser is mutated, check 12's, which is the instance bean:0118 measured. Out of
# band, 17 of the 22 call sites were observed taking the guard's fail branch with the gate's
# stdout uncorrupted, and 5 are not reached at all on a clean corpus — bean:0123 carries the
# transcript and names the five. NONE of that is asserted here: this file proves the failure
# path exists, not that every check reaches it, and its bypass assertion is an enumeration
# that fails open (see the comment on that assertion). bean:0126 is the fail-closed harness.
#
# bash 3.2 (macOS): what that forbids is enumerated in tools/lib/bash32-forbidden.tsv and
# enforced by tools/bash-compat-lint.sh in qualityCheck, not restated here (bean:0049).
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 2

GATE="$ROOT/tools/docs-lint.sh"
[ -f "$GATE" ] || { printf 'docs-lint-gate-test: %s is missing.\n' "$GATE"; exit 2; }

# $BASH is the path of the running shell, so the probes run under exactly the interpreter
# build.gradle.kts invoked this file with rather than whatever `bash` resolves to on PATH
# (bean:0049). That is 3.2.57 on macOS and bash 5 on the CI runner, and bean:0118 recorded
# the runner as the head it could not measure the boundary on.
SHELL_BIN="${BASH:-/bin/bash}"

TMP="$(mktemp -d)"
MUTANT="$ROOT/tools/.docs-lint-probe-$$-mutant.sh"
CONTROL="$ROOT/tools/.docs-lint-probe-$$-control.sh"
trap 'rm -rf "$TMP"; rm -f "$MUTANT" "$CONTROL"' EXIT

pass=0
fail=0

check() { # check <name> <expected> <actual>
  if [ "$2" = "$3" ]; then
    pass=$((pass + 1))
    printf 'ok   %s\n' "$1"
  else
    fail=$((fail + 1))
    printf 'FAIL %s\n       expected: %s\n       actual:   %s\n' "$1" "$2" "$3"
  fi
}

echo "docs-lint-gate-test: interpreter $SHELL_BIN (bash ${BASH_VERSION:-unknown})"
# The awk build, printed for the same reason the interpreter is: bean:0123 first inferred
# which awk the runner has from the SHAPE of a syntax-error diagnostic, and got it wrong.
# An implementation that has no --version says so in the line it prints instead, which
# still names it. Reported, never asserted on — it differs per image (bean:0049).
echo "docs-lint-gate-test: analyser awk — $(awk --version 2>&1 | head -1)"
echo
echo "--- the plant: check 12's acyclicity analyser, destroyed"

# The line that drives the fixed-point loop of the cycle analyser bean:0035 added. Matched
# rather than numbered: a line number in a probe is a locator that stops being true at the
# next edit and takes the probe silently with it (doc:50-memory-and-evidence#capturing).
check "the mutation site occurs exactly once in the gate" \
  "1" "$(grep -c '^    removed = 1$' "$GATE")"

sed 's/^    removed = 1$/    removed = = 1/' "$GATE" > "$MUTANT"
cp "$GATE" "$CONTROL"

# The plant landed, and landed once. Without this the harness could stop mutating and every
# assertion below would still be a statement about an unmodified script.
check "the copy differs from the gate on exactly one line (one '<', one '>')" \
  "2" "$(diff "$GATE" "$MUTANT" | grep -c '^[<>]')"
check "and the line it differs on is the planted syntax error" \
  "1" "$(grep -c '^    removed = = 1$' "$MUTANT")"
check "the control copy is identical to the gate" \
  "identical" "$(cmp -s "$GATE" "$CONTROL" && echo identical || echo differs)"

echo
echo "--- the runs: both halves, over the whole corpus"

# Backgrounded rather than sequential: each is a full gate run over every document and
# every bean, and two of them one after the other is the whole cost of this file.
"$SHELL_BIN" "$MUTANT" > "$TMP/mutant.out" 2> "$TMP/mutant.err" &
mutant_pid=$!
"$SHELL_BIN" "$CONTROL" > "$TMP/control.out" 2> "$TMP/control.err" &
control_pid=$!
wait "$mutant_pid"
mutant_rc=$?
wait "$control_pid"
control_rc=$?

check "a destroyed analyser makes the gate exit non-zero" \
  "rc=1" "rc=$mutant_rc"
check "and the gate says it failed rather than printing OK" \
  "docs-lint: 1 failure(s)." "$(head -1 "$TMP/mutant.out")"

# The STATUS the analyser exits with is the interpreter's, not this gate's: it was 2 under
# the BSD awk macOS ships and this assertion was written against that number, which made it
# the only assertion in this file that failed on the CI runner while the gate itself went
# red correctly (bean:0123). What is asserted is the attribution — one failure, named as an
# analyser that examined nothing — and the status is reported beside it rather than fixed,
# because a number that differs per image is a measurement and not a requirement.
check "and attributes it to an analyser that examined nothing" \
  "1" "$(grep -c 'an analyser exited [0-9][0-9]* and examined nothing' "$TMP/mutant.err")"
echo "     (this awk exited $(sed -n 's/.*an analyser exited \([0-9][0-9]*\) .*/\1/p' "$TMP/mutant.err" | head -1) on the planted syntax error)"

check "the negative control: the same copy unmutated exits 0" \
  "rc=0" "rc=$control_rc"
check "and prints the OK line" \
  "docs-lint: OK" "$(head -1 "$TMP/control.out" | cut -c1-13)"
check "and writes nothing at all to stderr" \
  "0" "$(grep -c . "$TMP/control.err")"

# Printed, not only asserted on. An assertion says whether a line matched; this says what
# the plant actually produced, which is what a reader needs when the same plant behaves
# differently under a different awk — and it did (bean:0049, bean:0123).
echo
echo "--- the mutated run's stderr: $(grep -c . "$TMP/mutant.err") line(s), at most 20 shown"
head -20 "$TMP/mutant.err" | sed 's/^/     /'

echo
echo "--- the guard covers every call site, because no call site opts in"

# The guard is a shell function that SHADOWS the name `awk`. That is what reaches the call
# sites inside `$( )` and the ones that are pipeline elements, where there is no statement
# after the analyser for a per-site `rc=$?` to be written at. The one way past it is to name
# the binary some other way.
#
# THIS ASSERTION DOES NOT CLOSE THAT, and an earlier revision of this comment said it did.
# It is an ENUMERATION of bypass spellings, and doc:00-constitution#observed-failing says at
# MUST strength that enumerating the shapes a gate accepts fails open — the same property as
# tools/lib/bash32-forbidden.tsv. Run against a fixture of one call per line, this regex
# CATCHES `command awk`, `env awk` and `/usr/bin/awk`, and does not match `exec awk`,
# `command -p awk`, `xargs awk`, `unset -f awk; awk`, `find -exec awk`, `$AWK`, `gawk`,
# `nawk` or `builtin :; awk`. Seven of those nine reach the binary past a function named
# `awk`; `builtin :; awk` does not, and `$AWK` does only when it expands to something other
# than `awk`. Two were then run against this suite whole and both scored 11 passed, 0 failed:
# `unset -f awk` around one call site, and the guard narrowed to the single site the plant
# reaches, which leaves 21 of the 22 unguarded. So this suite cannot tell 22 sites covered
# from 1 site covered. The fail-closed replacement is behavioural rather than lexical — a
# count of guard invocations, asserted — and is bean:0126. Figures in bean:0123.
#
# `\awk` is deliberately NOT in the regex. Backslash-quoting suppresses ALIAS expansion, not
# function lookup, so `\awk` still runs the shadow function; listing it inflated the reach of
# a list that already fails open on nine spellings (bean:0123).
#
# What the assertion does buy: the one deliberate bypass is NAMED rather than counted, so a
# second `command awk` fails this with both lines in the message — measured at 10 passed,
# 1 failed. Full-line comments are excluded, so prose about `command awk` is not a finding.
check "the guard's own call is the only site that bypasses it" \
  '  command awk "$@"' \
  "$(grep -v '^[[:space:]]*#' "$GATE" |
     grep -E '(^|[^A-Za-z_.-])(command[[:space:]]+awk|env[[:space:]]+awk|[^[:space:]]*/awk)[[:space:]]')"

echo
# A run that asserted nothing may not report success — the failure this whole file is about
# (doc:00-constitution#observed-failing).
if [ "$((pass + fail))" -eq 0 ]; then
  echo "docs-lint-gate-test: no assertion ran."
  exit 2
fi
echo "docs-lint-gate-test: $pass passed, $fail failed."
[ "$fail" -eq 0 ] || exit 1
