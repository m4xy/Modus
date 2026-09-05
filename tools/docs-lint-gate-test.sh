#!/usr/bin/env bash
# docs-lint-gate-test — does tools/docs-lint.sh go RED when something in it fails at runtime?
#
# THE DEFECT (bean:0118). tools/docs-lint.sh runs awk twenty-two times and inspected the
# exit status of exactly one of them. An analyser destroyed by a syntax error wrote
# nothing, the loop that read it found nothing, no `fail` fired, and the gate printed its
# `OK` line at exit 0 with stdout BYTE-IDENTICAL to the clean run. `set -u` is the only
# fail-closed mechanism in that file and it is fail-closed only in the TOP-LEVEL shell;
# no analyser runs there.
#
# AND THE REST OF THAT BOUNDARY (bean:0124), which is the second plant below. `false`, a
# missing file, a failed `cd`, a failed pipeline element and an unbound variable expanded
# inside `$( )` all reached the same `OK` line at exit 0 — three of them having written
# nothing at all to stderr, so no diagnostic existed for a caller to notice. The mechanism
# is an ERR trap recording through the same `fail`; what it does NOT cover is a failure
# whose status an enclosing construct discards, which in this gate means a non-final
# command inside a `printf … | while read` body, that body running in a subshell of its own.
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
# The same holds of the second plant: five points in the gate are made to fail, not every
# point, and no assertion here bounds the set of shapes the trap catches.
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
RUNTIME="$ROOT/tools/.docs-lint-probe-$$-runtime.sh"
TMPFAIL="$ROOT/tools/.docs-lint-probe-$$-notmp.sh"
VANISH="$ROOT/tools/.docs-lint-probe-$$-vanish.sh"
trap 'rm -rf "$TMP"; rm -f "$MUTANT" "$CONTROL" "$RUNTIME" "$TMPFAIL" "$VANISH"' EXIT
TAB="$(printf '\t')"

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
echo "--- the second plant: five runtime failures that are not an analyser"

# FIVE CLASSES IN ONE COPY, not five copies. Each is asserted by the RECORD it produces and
# not merely by the run going red, so a mechanism that caught one of the five would fail the
# other four rows — while five separate copies would cost five more full passes over the
# corpus to answer what these five records already answer.
#
# THE FIVE SHARE NO PLANT POINT AND NO SHAPE, which is deliberate: a suite whose every
# fixture makes the same structural assumption is blind wherever that assumption is what
# matters. The points run from the top of the file to the bottom — just under the trap that
# arms the failure path, inside check 2's per-document loop, at the end of check 6, at the
# head of check 12, and inside check 13c — and one of the five is inside a loop body rather
# than at the top level. The shapes differ in what a reader could otherwise have noticed:
# `false` writes NOTHING to stderr, `cat` and the unbound variable write a diagnostic, the
# failed `cd` changes nothing at all, and the pipeline's failing element is not the one
# `$BASH_COMMAND` names — that record names `sed`, and the statuses beside it are what say
# the failure was at the other end.
#
# Each line carries a marker token that occurs nowhere else in the gate, asserted below, so
# the record it produces is matched by something no other line can produce. `false __probe_…`
# exits 1 and prints nothing, exactly as a bare `false` does.
#
# The columns are: name, the gate line to plant after, the line to plant, and the marker.
PLANTS="$TMP/plants.tsv"
cat > "$PLANTS" <<'PLANTS_EOF'
a silent non-zero exit	FM_FILES="$(ls documentation/*.md documentation/adr/*.md)"	false __probe_silent__	__probe_silent__
a missing file	  id="$(field "$f" S id)"	cat /no/such/file/__probe_missing_file__	__probe_missing_file__
a failed pipeline element	sort -u "$TMP/refs.tsv" > "$TMP/refs.uniq"	false | sed -n 's/__probe_pipeline__//p'	__probe_pipeline__
an unbound variable inside $( )	: > "$TMP/beans.tsv"	probe="$(echo "${__probe_unbound_subst}")"	__probe_unbound_subst
a failed cd	awk -F'\t' '{ print $1 }' "$TMP/beans.tsv" | sort -u > "$TMP/bean-ids-tree.txt"	cd /no/such/dir/__probe_failed_cd__	__probe_failed_cd__
PLANTS_EOF

awk -F'\t' '
  NR == FNR { after[$2] = $3; next }
  { print }
  ($0 in after) && !done[$0] { print after[$0]; done[$0] = 1 }
' "$PLANTS" "$GATE" > "$RUNTIME"

n_plants=0
while IFS="$TAB" read -r name anchor line marker; do
  [ -n "$name" ] || continue
  n_plants=$((n_plants + 1))
  check "the plant point for $name occurs exactly once in the gate" \
    "1" "$(grep -cxF "$anchor" "$GATE")"
  check "and $name is planted exactly once in the copy" \
    "1" "$(grep -cxF "$line" "$RUNTIME")"
  check "and $name's marker occurs nowhere in the gate itself" \
    "0" "$(grep -cF "$marker" "$GATE")"
done < "$PLANTS"

check "five classes were planted, one line each" \
  "5 plants, 5 lines" "$n_plants plants, $(diff "$GATE" "$RUNTIME" | grep -c '^[<>]') lines"

echo
echo "--- the third plant: the gate's own scratch directory, which every record is written into"

# THE ONE FAILURE THE TRAP CANNOT RECORD, because it is the failure of the file the trap
# records into. `docs-lint: N failure(s).` counts the lines of $TMP/fails.txt; with $TMP
# never created, every record — the checks' and the trap's alike — is written into a
# directory that is not there, `n_fail` reads 0, and the gate printed `docs-lint: OK` at
# exit 0 with 291 `FAIL check` lines above it on its own stdout. Measured in bean:0124.
# The two `|| exit 2` this asserts are the only guard possible for it, and they are above
# the trap because the trap cannot exist before the file does.
NOTMP_LINE='TMP="$(mktemp -d)" || exit 2'
awk -v want="$NOTMP_LINE" '
  $0 == want { print "TMP=\"$(mktemp -d /no/such/dir/__probe_no_tmpdir__/XXXXXX)\" || exit 2"; next }
  { print }
' "$GATE" > "$TMPFAIL"

check "the scratch-directory line occurs exactly once in the gate" \
  "1" "$(grep -cxF "$NOTMP_LINE" "$GATE")"
check "and the copy differs from the gate on exactly one line (one '<', one '>')" \
  "2" "$(diff "$GATE" "$TMPFAIL" | grep -c '^[<>]')"

echo
echo "--- the fourth plant: the scratch directory removed MID-RUN, which one row of the boundary does"

# `set -u` firing inside a TOP-LEVEL PIPELINE ELEMENT exits that element's subshell, and that
# subshell runs the EXIT trap it inherited — `rm -rf "$TMP"`. So this row of bean:0118's
# boundary does not merely go unrecorded: it destroys the file every record is written into,
# the trap's own included. Before the guard this run printed `docs-lint: OK` at exit 0 with
# its twelve counts empty and 287 `FAIL check` lines above it (bean:0124). The plant is the
# boundary row itself, not a `rm`, so what is asserted is the thing that actually happens.
VANISH_ANCHOR=': > "$TMP/bean-edges.tsv"'
VANISH_LINE='echo "${__probe_unbound_in_pipeline}" | cat'
awk -v anc="$VANISH_ANCHOR" -v ins="$VANISH_LINE" '
  { print } $0 == anc && !d { print ins; d = 1 }
' "$GATE" > "$VANISH"

check "the plant point for the vanishing record file occurs exactly once in the gate" \
  "1" "$(grep -cxF "$VANISH_ANCHOR" "$GATE")"
check "and the copy differs from the gate on exactly one line (one '>')" \
  "1" "$(diff "$GATE" "$VANISH" | grep -c '^[<>]')"

echo
echo "--- the runs: both halves, over the whole corpus"

# Backgrounded rather than sequential: each is a full gate run over every document and
# every bean, and three of them one after the other is the whole cost of this file.
"$SHELL_BIN" "$MUTANT" > "$TMP/mutant.out" 2> "$TMP/mutant.err" &
mutant_pid=$!
"$SHELL_BIN" "$CONTROL" > "$TMP/control.out" 2> "$TMP/control.err" &
control_pid=$!
"$SHELL_BIN" "$RUNTIME" > "$TMP/runtime.out" 2> "$TMP/runtime.err" &
runtime_pid=$!
"$SHELL_BIN" "$TMPFAIL" > "$TMP/notmp.out" 2> "$TMP/notmp.err" &
notmp_pid=$!
"$SHELL_BIN" "$VANISH" > "$TMP/vanish.out" 2> "$TMP/vanish.err" &
vanish_pid=$!
wait "$mutant_pid"
mutant_rc=$?
wait "$control_pid"
control_rc=$?
wait "$runtime_pid"
runtime_rc=$?
wait "$notmp_pid"
notmp_rc=$?
wait "$vanish_pid"
vanish_rc=$?

check "a destroyed analyser makes the gate exit non-zero" \
  "rc=1" "rc=$mutant_rc"
# TWO records for one dead analyser, and they are not duplicates: the awk wrapper's names
# the file the analyser was reading and cannot name the call site, and the ERR trap's names
# the call site and cannot name the file. The count was 1 before bean:0124 added the trap.
check "and the gate says it failed rather than printing OK" \
  "docs-lint: 2 failure(s)." "$(head -1 "$TMP/mutant.out")"
check "and the trap names the call site the analyser died at" \
  "1" "$(grep -c "nothing checked it: 'cycle=" "$TMP/mutant.err")"

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
echo "--- the runtime failure path: one class per plant, all five in one run"

check "an unchecked non-zero exit makes the gate exit non-zero" \
  "rc=1" "rc=$runtime_rc"
# The COUNT in that line is a count of records, and one of the five plants sits inside a
# per-document loop, so it is a figure of the corpus and moves with it
# (doc:50-memory-and-evidence#corpus-figures). The digits are normalised away and the raw
# line is printed below; what is asserted is that the gate reported failure rather than OK.
check "and the gate says it failed rather than printing OK" \
  "docs-lint: N failure(s)." "$(head -1 "$TMP/runtime.out" | sed 's/[0-9][0-9]*/N/')"

# DISTINCT records, not records: the loop plant fires once per document, and that figure is
# the corpus's. One distinct record per planted class is this file's "how many times", which
# doc:50-memory-and-evidence#evidence-kinds requires beside the firing and the silence — a
# mechanism that fired twenty-three times on five faults would pass a bare "it fired".
while IFS="$TAB" read -r name anchor line marker; do
  [ -n "$name" ] || continue
  check "and records $name, once and distinctly" \
    "1" "$(grep -F "$marker" "$TMP/runtime.err" | grep 'nothing checked it' | sort -u | grep -c .)"
done < "$PLANTS"

# The pipeline row twice over: `$BASH_COMMAND` holds a pipeline's LAST element, which here
# is the `sed` that succeeded, so without the statuses the record would name the wrong end.
check "and the pipeline record carries the statuses that say which end failed" \
  "1" "$(grep -cF '(pipeline exited 1 0, left to right)' "$TMP/runtime.err")"

check "and records nothing else: five plants, five distinct records" \
  "5" "$(grep 'nothing checked it' "$TMP/runtime.err" | sort -u | grep -c .)"

echo
echo "--- the five records, and the $(grep -c 'nothing checked it' "$TMP/runtime.err") firings they came from"
grep 'nothing checked it' "$TMP/runtime.err" | sort -u | sed 's/^/     /'

echo
echo "--- the opt-out: what the trap is told not to look at"

# The trap would fire on a clean tree without this, because two commands in the gate return
# non-zero when nothing is wrong — `grep` finding no match. The opt-out that silences them
# has to silence THAT and not a `grep` that could not look at all, and the difference is one
# digit. Widening it from 1 to 9 leaves this suite at a clean sheet through every assertion
# above, which is why these three exist (bean:0124, and bean:0123 for the shape).
#
# THE FUNCTION IS RUN, not read: the definition is cut out of the gate by a `sed` range whose
# two ends are asserted, and executed under the same interpreter. What that does NOT prove is
# that the gate calls it at the right sites — the negative control above is the half that
# proves it, since an opt-out that tolerated nothing would turn the clean run red.
OPTOUT="$(sed -n '/^absent_ok() {/,/^}/p' "$GATE")"
check "the opt-out is one function, cut whole out of the gate" \
  "1 definition, 1 close" \
  "$(grep -c '^absent_ok() {' "$GATE") definition, $(printf '%s\n' "$OPTOUT" | grep -c '^}') close"
check "and it tolerates grep's 'no match', which is an answer" \
  "0" "$("$SHELL_BIN" -c "$OPTOUT"'; absent_ok grep -c . /dev/null > /dev/null; echo $?')"
check "and does not tolerate a grep that could not look, which is a failure" \
  "2" "$("$SHELL_BIN" -c "$OPTOUT"'; absent_ok grep -c . /no/such/file/__probe_optout__ 2> /dev/null > /dev/null; echo $?')"

echo
echo "--- and with no scratch directory, the gate stops instead of reporting"

check "a gate that cannot create its record file exits 2" \
  "rc=2" "rc=$notmp_rc"
check "and prints nothing at all on stdout, so there is no OK line to misread" \
  "0" "$(grep -c . "$TMP/notmp.out")"
echo "     (it wrote $(grep -c . "$TMP/notmp.err") line(s) to stderr; the first is: $(head -1 "$TMP/notmp.err"))"

echo
echo "--- and with the record file removed under it mid-run, likewise"

check "a gate whose record file vanished exits 2" \
  "rc=2" "rc=$vanish_rc"
check "and says so on the line it stops at" \
  "1" "$(grep -c '^docs-lint: the failure record .* vanished mid-run' "$TMP/vanish.out")"
# The half that matters: it must not reach the OK line. Before the guard this run printed one
# — with every count between the commas empty, which is the counts line's vacuity assertion
# reporting a run that examined nothing and being read by nobody (bean:0069, bean:0127).
check "and never reaches the OK line" \
  "0" "$(grep -c '^docs-lint: OK' "$TMP/vanish.out")"

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
