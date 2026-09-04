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
# it. One analyser is mutated, check 12's, which is the instance bean:0118 measured. The
# other twenty-one are covered by the guard's SHAPE — a shell function shadowing the name
# `awk`, so no call site opts in — and by the bypass assertion below, not by a run. Turning
# that into a per-check observation is bean:0126, and until it lands this file proves the
# failure path exists rather than that every check reaches it.
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
check "and attributes it to an analyser that examined nothing" \
  "1" "$(grep -c 'an analyser exited 2 and examined nothing' "$TMP/mutant.err")"

check "the negative control: the same copy unmutated exits 0" \
  "rc=0" "rc=$control_rc"
check "and prints the OK line" \
  "docs-lint: OK" "$(head -1 "$TMP/control.out" | cut -c1-13)"
check "and writes nothing at all to stderr" \
  "0" "$(grep -c . "$TMP/control.err")"

echo
echo "--- the guard covers every call site, because no call site opts in"

# The guard is a shell function that SHADOWS the name `awk`. That is what reaches the call
# sites inside `$( )` and the ones that are pipeline elements, where there is no statement
# after the analyser for a per-site `rc=$?` to be written at. The one way past it is to name
# the binary some other way, and a second bypass would be a call site the guard silently
# stops covering — which is the defect this file exists to close, one level up.
#
# This is an ENUMERATION of bypass spellings and therefore fails open on one nobody has
# named, exactly as tools/lib/bash32-forbidden.tsv does and for the same reason
# (doc:00-constitution#mechanical-enforcement). Full-line comments are excluded, so prose
# about `command awk` is not a finding. The instance is NAMED rather than counted, so a
# second bypass fails this with both lines in the message.
check "the guard's own call is the only site that bypasses it" \
  '  command awk "$@"' \
  "$(grep -v '^[[:space:]]*#' "$GATE" |
     grep -E '(^|[^A-Za-z_.-])(command[[:space:]]+awk|env[[:space:]]+awk|\\awk|[^[:space:]]*/awk)[[:space:]]')"

echo
# A run that asserted nothing may not report success — the failure this whole file is about
# (doc:00-constitution#observed-failing).
if [ "$((pass + fail))" -eq 0 ]; then
  echo "docs-lint-gate-test: no assertion ran."
  exit 2
fi
echo "docs-lint-gate-test: $pass passed, $fail failed."
[ "$fail" -eq 0 ] || exit 1
