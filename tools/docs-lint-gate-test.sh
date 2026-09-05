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
# whose status an enclosing construct discards, which in this gate means a command inside a
# loop body whose enclosing loop is a pipeline element, that body running in a subshell of
# its own — and, under bash 3.2 only, the whole of such a pipeline.
#
# WHERE IN THE FILE, which is a separate question from what shape. `trap - ERR` inserted at
# one line of the gate scored a clean sheet against an earlier revision of this file: every
# plant it carried sat above the disarm, so the last hundred lines of the gate were covered
# by nothing and the suite could not tell the whole file covered from most of it. That is
# bean:0123's "22 call sites or 1 call site" moved from call sites to line ranges. Two things
# answer it here, and only the second bounds anything: two more plants extend the planted
# points to the last statement before the count, and — the bound — the gate now ASKS THE
# SHELL at its last line whether the handler is still armed, so a disarm anywhere between the
# arming and there is caught without enumerating where it might be. The fifth copy below is
# that mutation, run as a plant.
#
# UNDER EVERY BASH THIS MACHINE HAS, not just the one that invoked this file. An earlier
# revision ran `${BASH}` alone — 3.2.57 locally, bash 5 on the runner — while the change it
# proves makes claims about BOTH, and the runner then went red on a clean tree at a site no
# local run could see (bean:0124). Every gate run below is repeated once per distinct bash
# MAJOR version found on the host, and when only one is found the banner says so and names
# what is therefore unverified here. The ERR-trap visibility table is printed per interpreter
# for the same reason: the divergence is a premise of tools/docs-lint.sh's trap comment, so it
# is measured on every run rather than remembered.
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
# The same holds of the second plant: eight points in the gate are made to fail, not every
# point, and no assertion here bounds the set of SHAPES the trap catches — only the armed
# check bounds the RANGE it is armed over, and only against a disarm that does not re-arm.
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

shell_version() { "$1" -c 'printf %s "$BASH_VERSION"' 2>/dev/null; }
shell_major() { "$1" -c 'printf %s "${BASH_VERSINFO[0]}"' 2>/dev/null; }

# ONE INTERPRETER PER MAJOR VERSION, the pinned one first. The candidate list is a list of
# PATHS and not of behaviours, so a bash this list does not name costs coverage and never
# correctness; what would fail open is asserting a bash-5-only property while running 3.2,
# and the banner below refuses to let that pass silently.
SHELLS="$SHELL_BIN"
MAJORS=" $(shell_major "$SHELL_BIN") "
for cand in /bin/bash /usr/bin/bash /usr/local/bin/bash /opt/homebrew/bin/bash "$(command -v bash 2>/dev/null || true)"; do
  [ -n "$cand" ] || continue
  [ -x "$cand" ] || continue
  cand_major="$(shell_major "$cand")"
  [ -n "$cand_major" ] || continue
  case "$MAJORS" in *" $cand_major "*) continue ;; esac
  SHELLS="$SHELLS $cand"
  MAJORS="$MAJORS$cand_major "
done
n_majors="$(printf '%s\n' $MAJORS | grep -c .)"

TMP="$(mktemp -d)"
MUTANT="$ROOT/tools/.docs-lint-probe-$$-mutant.sh"
CONTROL="$ROOT/tools/.docs-lint-probe-$$-control.sh"
RUNTIME="$ROOT/tools/.docs-lint-probe-$$-runtime.sh"
TMPFAIL="$ROOT/tools/.docs-lint-probe-$$-notmp.sh"
VANISH="$ROOT/tools/.docs-lint-probe-$$-vanish.sh"
DISARM="$ROOT/tools/.docs-lint-probe-$$-disarm.sh"
trap 'rm -rf "$TMP"; rm -f "$MUTANT" "$CONTROL" "$RUNTIME" "$TMPFAIL" "$VANISH" "$DISARM"' EXIT
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
for s in $SHELLS; do
  echo "docs-lint-gate-test: exercising $s (bash $(shell_version "$s"))"
done
if [ "$n_majors" -lt 2 ]; then
  echo "docs-lint-gate-test: ONE bash MAJOR VERSION ONLY on this host. The claims in"
  echo "docs-lint-gate-test: tools/docs-lint.sh's trap comment that differ BY interpreter —"
  echo "docs-lint-gate-test: which pipeline shapes reach the ERR trap, which element"
  echo "docs-lint-gate-test: \$BASH_COMMAND names, and what \$LINENO holds inside a loop body —"
  echo "docs-lint-gate-test: are exercised here for bash ${BASH_VERSINFO[0]} and for no other."
fi

echo
echo "--- what an ERR trap can see, per interpreter"

# THE DIVERGENCE IS A PREMISE OF THE GATE, so it is measured here rather than remembered. Ten
# pipeline shapes, distinguished only by their last element. Under bash 3.2.57 a pipeline
# ending in a compound command does not reach the trap; under 5.3.9 it does, and a subshell
# reaches it twice. Only the two rows every record in the gate depends on are ASSERTED — a
# pipeline ending in a simple command, and one ending in a function — because the rest are
# numbers the interpreter chooses and bean:0123 had an assertion fail on the runner for
# requiring one. The whole table is printed, on every run, under every interpreter.
cat > "$TMP/shapes.sh" <<'SHAPES_EOF'
set -uo pipefail
n=0
trap 'n=$((n + 1))' ERR
row() { printf '%-18s\t%s\n' "$1" "$n"; n=0; }
false | while read -r x; do :; done
row "while"
false | for x in 1; do :; done
row "for"
false | until true; do :; done
row "until"
false | if true; then :; fi
row "if"
false | case x in x) : ;; esac
row "case"
false | { :; }
row "brace group"
false | { :; } > /dev/null
row "redirected group"
false | ( : )
row "subshell"
false | cat
row "simple command"
fn() { :; }
false | fn
row "function"
SHAPES_EOF

for s in $SHELLS; do
  "$s" "$TMP/shapes.sh" > "$TMP/shapes.out" 2>/dev/null
  echo "     $s (bash $(shell_version "$s")) — firings per shape"
  sed 's/^/       /' "$TMP/shapes.out"
  check "a pipeline ending in a simple command reaches the trap under bash $(shell_version "$s")" \
    "1" "$(awk -F"$TAB" '$1 ~ /^simple command/ { print $2 }' "$TMP/shapes.out")"
  check "and one ending in a function does too, under bash $(shell_version "$s")" \
    "1" "$(awk -F"$TAB" '$1 ~ /^function/ { print $2 }' "$TMP/shapes.out")"
done

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
echo "--- the second plant: eight runtime failures that are not an analyser"

# EIGHT POINTS IN ONE COPY, not eight copies. Each is asserted by the RECORD it produces and
# not merely by the run going red, so a mechanism that caught one of the eight would fail the
# other seven rows — while eight separate copies would cost eight more full passes over the
# corpus to answer what these eight records already answer.
#
# THEY SHARE NO PLANT POINT AND NO SHAPE, which is deliberate: a suite whose every fixture
# makes the same structural assumption is blind wherever that assumption is what matters. Two
# dimensions vary independently.
#
# SHAPE — `false` writes NOTHING to stderr, `cat` and the unbound variable write a diagnostic,
# the failed `cd` changes nothing at all, the pipeline's failing element is not the one
# `$BASH_COMMAND` names, and the last is a `grep` that COULD NOT LOOK at a site the opt-out
# tolerates "no match" at, which is the one shape the opt-out must not swallow.
#
# POSITION — the points run from just under the trap that arms the failure path to the last
# statement the gate executes before it counts its records: under the trap, in check 2's
# per-document loop, at the end of check 6, at the head of check 12, inside check 13c, in
# check 14's preamble, and after the `done` banner. One is inside a loop body rather than at
# the top level. The last two exist because an earlier revision planted nothing below check
# 13c, and `trap - ERR` planted below the lowest plant passed the whole suite.
#
# Each line carries a marker token that occurs nowhere else in the gate, asserted below, so
# the record it produces is matched by something no other line can produce. `false __probe_…`
# exits 1 and prints nothing, exactly as a bare `false` does.
#
# The columns are: name, the gate line to plant after, the line to plant, the marker, and the
# ERE that must match the record — one column rather than a rule, because the opted-out plant
# is recorded by the opt-out and not by the trap, and says so in different words.
PLANTS="$TMP/plants.tsv"
cat > "$PLANTS" <<'PLANTS_EOF'
a silent non-zero exit	FM_FILES="$(ls documentation/*.md documentation/adr/*.md)"	false __probe_silent__	__probe_silent__	nothing checked it.*__probe_silent__
a grep that could not look, at an opted-out site	REQUIRED_KEYS="id title status superseded_by read_when provides depends_on"	absent_ok grep -c . /no/such/file/__probe_cannot_look__	__probe_cannot_look__	could not look.*__probe_cannot_look__
a missing file	  id="$(field "$f" S id)"	cat /no/such/file/__probe_missing_file__	__probe_missing_file__	nothing checked it.*__probe_missing_file__
a failed pipeline element	sort -u "$TMP/refs.tsv" > "$TMP/refs.uniq"	false | sed -n 's/__probe_pipeline__//p'	__probe_pipeline__	nothing checked it.*__probe_pipeline__
an unbound variable inside $( )	: > "$TMP/beans.tsv"	probe="$(echo "${__probe_unbound_subst}")"	__probe_unbound_subst	nothing checked it.*__probe_unbound_subst
a failed cd	awk -F'\t' '{ print $1 }' "$TMP/beans.tsv" | sort -u > "$TMP/bean-ids-tree.txt"	cd /no/such/dir/__probe_failed_cd__	__probe_failed_cd__	nothing checked it.*__probe_failed_cd__
a silent non-zero exit in check 14's preamble	KINDS=" command test-run diff citation fetch observation "	false __probe_check14__	__probe_check14__	nothing checked it.*__probe_check14__
a missing file at the last statement before the count	# -------------------------------------------------------------------- done ---	cat /no/such/file/__probe_last_statement__	__probe_last_statement__	nothing checked it.*__probe_last_statement__
PLANTS_EOF

awk -F'\t' '
  NR == FNR { after[$2] = $3; next }
  { print }
  ($0 in after) && !done[$0] { print after[$0]; done[$0] = 1 }
' "$PLANTS" "$GATE" > "$RUNTIME"

n_plants=0
while IFS="$TAB" read -r name anchor line marker record_re; do
  [ -n "$name" ] || continue
  n_plants=$((n_plants + 1))
  check "the plant point for $name occurs exactly once in the gate" \
    "1" "$(grep -cxF "$anchor" "$GATE")"
  check "and $name is planted exactly once in the copy" \
    "1" "$(grep -cxF "$line" "$RUNTIME")"
  check "and $name's marker occurs nowhere in the gate itself" \
    "0" "$(grep -cF "$marker" "$GATE")"
done < "$PLANTS"

check "eight points were planted, one line each" \
  "8 plants, 8 lines" "$n_plants plants, $(diff "$GATE" "$RUNTIME" | grep -c '^[<>]') lines"

echo
echo "--- the third plant: the gate's own scratch directory, which every record is written into"

# THE ONE FAILURE THE TRAP CANNOT RECORD, because it is the failure of the file the trap
# records into. `docs-lint: N failure(s).` counts the lines of $TMP/fails.txt; with $TMP
# never created, every record — the checks' and the trap's alike — is written into a
# directory that is not there, `n_fail` reads 0, and the gate printed `docs-lint: OK` at exit
# 0 with a `FAIL check` line for every one of those records above it on its own stdout. The
# count is a figure of the corpus and is not repeated here: tools/docs-lint.sh's own comment
# on the two `|| exit 2` carries it, stamped, and bean:0124 carries the capture.
# Those two lines are above the trap because the trap cannot exist before the file does.
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
# its twelve counts empty and a `FAIL check` line for every lost record above it (bean:0124).
# The plant is the boundary row itself, not a `rm`, so what is asserted is the thing that
# actually happens.
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
echo "--- the fifth plant: the ERR trap disarmed mid-file, which nothing below it can see"

# ONE LINE, AND IT IS NOT A NO-OP: `trap - ERR` here leaves everything above it recorded and
# everything below it silent, and the gate reports OK at exit 0. Against the revision of this
# file that had no plant below check 13c it scored a clean sheet — 49 passed, 0 failed — which
# is why tools/docs-lint.sh now asks the shell what handler it holds at its last line. The
# plant point is deliberately BELOW every other plant here, so what catches it cannot be one
# of them.
DISARM_ANCHOR='KINDS=" command test-run diff citation fetch observation "'
awk -v anc="$DISARM_ANCHOR" '
  $0 == anc && !d { print "trap - ERR"; d = 1 } { print }
' "$GATE" > "$DISARM"

check "the plant point for the disarmed trap occurs exactly once in the gate" \
  "1" "$(grep -cxF "$DISARM_ANCHOR" "$GATE")"
check "and the copy differs from the gate on exactly one line (one '>')" \
  "1" "$(diff "$GATE" "$DISARM" | grep -c '^[<>]')"
check "and the disarm is planted exactly once in the copy" \
  "1" "$(grep -cx 'trap - ERR' "$DISARM")"
check "and the gate itself disarms nothing" \
  "0" "$(grep -c '^[[:space:]]*trap[[:space:]]*-[[:space:]]*ERR' "$GATE")"

# --------------------------------------------------------------- the runs ---
# Every gate run below happens once per interpreter. Backgrounded within an interpreter
# rather than sequential: each is a full pass over every document and every bean, and six
# of them one after the other is the whole cost of this file.
run_number=0
run_under() { # run_under <shell path>
  local sh="$1"
  local ver
  local d
  local at
  ver="$(shell_version "$sh")"
  at=" [bash $ver]"
  run_number=$((run_number + 1))
  d="$TMP/run$run_number"
  mkdir -p "$d"

  echo
  echo "--- the runs under $sh (bash $ver): both halves, over the whole corpus"

  "$sh" "$MUTANT" > "$d/mutant.out" 2> "$d/mutant.err" &
  local mutant_pid=$!
  "$sh" "$CONTROL" > "$d/control.out" 2> "$d/control.err" &
  local control_pid=$!
  "$sh" "$RUNTIME" > "$d/runtime.out" 2> "$d/runtime.err" &
  local runtime_pid=$!
  "$sh" "$TMPFAIL" > "$d/notmp.out" 2> "$d/notmp.err" &
  local notmp_pid=$!
  "$sh" "$VANISH" > "$d/vanish.out" 2> "$d/vanish.err" &
  local vanish_pid=$!
  "$sh" "$DISARM" > "$d/disarm.out" 2> "$d/disarm.err" &
  local disarm_pid=$!
  wait "$mutant_pid"
  local mutant_rc=$?
  wait "$control_pid"
  local control_rc=$?
  wait "$runtime_pid"
  local runtime_rc=$?
  wait "$notmp_pid"
  local notmp_rc=$?
  wait "$vanish_pid"
  local vanish_rc=$?
  wait "$disarm_pid"
  local disarm_rc=$?

  check "a destroyed analyser makes the gate exit non-zero$at" \
    "rc=1" "rc=$mutant_rc"
  # TWO records for one dead analyser, and they are not duplicates: the awk wrapper's names
  # the file the analyser was reading and cannot name the call site, and the ERR trap's names
  # the call site and cannot name the file. The count was 1 before bean:0124 added the trap.
  check "and the gate says it failed rather than printing OK$at" \
    "docs-lint: 2 failure(s)." "$(head -1 "$d/mutant.out")"
  check "and the trap names the call site the analyser died at$at" \
    "1" "$(grep -c "nothing checked it: 'cycle=" "$d/mutant.err")"

  # The STATUS the analyser exits with is the interpreter's, not this gate's: it was 2 under
  # the BSD awk macOS ships and this assertion was written against that number, which made it
  # the only assertion in this file that failed on the CI runner while the gate itself went
  # red correctly (bean:0123). What is asserted is the attribution — one failure, named as an
  # analyser that examined nothing — and the status is reported beside it rather than fixed,
  # because a number that differs per image is a measurement and not a requirement.
  check "and attributes it to an analyser that examined nothing$at" \
    "1" "$(grep -c 'an analyser exited [0-9][0-9]* and examined nothing' "$d/mutant.err")"
  echo "     (this awk exited $(sed -n 's/.*an analyser exited \([0-9][0-9]*\) .*/\1/p' "$d/mutant.err" | head -1) on the planted syntax error)"

  check "the negative control: the same copy unmutated exits 0$at" \
    "rc=0" "rc=$control_rc"
  check "and prints the OK line$at" \
    "docs-lint: OK" "$(head -1 "$d/control.out" | cut -c1-13)"
  check "and writes nothing at all to stderr$at" \
    "0" "$(grep -c . "$d/control.err")"

  # Printed, not only asserted on. An assertion says whether a line matched; this says what
  # the plant actually produced, which is what a reader needs when the same plant behaves
  # differently under a different awk — and it did (bean:0049, bean:0123).
  echo
  echo "--- the mutated run's stderr under bash $ver: $(grep -c . "$d/mutant.err") line(s), at most 20 shown"
  head -20 "$d/mutant.err" | sed 's/^/     /'

  echo
  echo "--- the runtime failure path under bash $ver: one record per plant, all eight in one run"

  check "an unchecked non-zero exit makes the gate exit non-zero$at" \
    "rc=1" "rc=$runtime_rc"
  # The COUNT in that line is a count of records, and one of the plants sits inside a
  # per-document loop, so it is a figure of the corpus and moves with it
  # (doc:50-memory-and-evidence#corpus-figures). The digits are normalised away and the raw
  # line is printed below; what is asserted is that the gate reported failure rather than OK.
  check "and the gate says it failed rather than printing OK$at" \
    "docs-lint: N failure(s)." "$(head -1 "$d/runtime.out" | sed 's/[0-9][0-9]*/N/')"

  # DISTINCT records, not records: the loop plant fires once per document, and that figure is
  # the corpus's. One distinct record per planted point is this file's "how many times", which
  # doc:50-memory-and-evidence#evidence-kinds requires beside the firing and the silence — a
  # mechanism that fired twenty-three times on eight faults would pass a bare "it fired".
  while IFS="$TAB" read -r name anchor line marker record_re; do
    [ -n "$name" ] || continue
    check "and records $name, once and distinctly$at" \
      "1" "$(grep -E "$record_re" "$d/runtime.err" | sort -u | grep -c .)"
  done < "$PLANTS"

  # The pipeline row twice over: `$BASH_COMMAND` holds a pipeline's LAST element here, which
  # is the `sed` that succeeded, so without the statuses the record would name the wrong end.
  check "and the pipeline record carries the statuses that say which end failed$at" \
    "1" "$(grep -cF '(pipeline exited 1 0, left to right)' "$d/runtime.err")"

  # NINE, for eight plants, and the ninth is not noise the suite failed to name. The opted-out
  # plant is recorded twice: once by `absent_ok`, naming the argv, and once by the trap, whose
  # `$BASH_COMMAND` at that caller holds `return "$ec"` — the last command absent_ok ran. One
  # dead analyser is two records for the same reason, and both are true.
  check "and records nothing else: eight plants, nine distinct records$at" \
    "9" "$(grep -E 'nothing checked it|could not look' "$d/runtime.err" | sort -u | grep -c .)"

  echo
  echo "--- the records under bash $ver, and the $(grep -cE 'nothing checked it|could not look' "$d/runtime.err") firings they came from"
  grep -E 'nothing checked it|could not look' "$d/runtime.err" | sort -u | sed 's/^/     /'

  echo
  echo "--- and with no scratch directory, the gate stops instead of reporting$at"

  check "a gate that cannot create its record file exits 2$at" \
    "rc=2" "rc=$notmp_rc"
  check "and prints nothing at all on stdout, so there is no OK line to misread$at" \
    "0" "$(grep -c . "$d/notmp.out")"
  echo "     (it wrote $(grep -c . "$d/notmp.err") line(s) to stderr; the first is: $(head -1 "$d/notmp.err"))"

  echo
  echo "--- and with the record file removed under it mid-run, likewise$at"

  check "a gate whose record file vanished exits 2$at" \
    "rc=2" "rc=$vanish_rc"
  check "and says so on the line it stops at$at" \
    "1" "$(grep -c '^docs-lint: the failure record .* vanished mid-run' "$d/vanish.out")"
  # The half that matters: it must not reach the OK line. Before the guard this run printed one
  # — with every count between the commas empty, which is the counts line's vacuity assertion
  # reporting a run that examined nothing and being read by nobody (bean:0069, bean:0127).
  check "and never reaches the OK line$at" \
    "0" "$(grep -c '^docs-lint: OK' "$d/vanish.out")"

  echo
  echo "--- and with the ERR trap disarmed below every plant, the gate says so$at"

  check "a gate whose ERR trap was disarmed mid-file exits non-zero$at" \
    "rc=1" "rc=$disarm_rc"
  check "and names the disarm rather than reporting OK$at" \
    "1" "$(grep -c 'the ERR trap was not armed at the end of the run' "$d/disarm.out")"
  check "and never reaches the OK line$at" \
    "0" "$(grep -c '^docs-lint: OK' "$d/disarm.out")"

  echo
  echo "--- the opt-out under bash $ver: what the trap is told not to look at"

  # The trap would fire on a clean tree without this, because commands in the gate return
  # non-zero when nothing is wrong — `grep` finding no match, and `grep -c` restating a count
  # of 0. The opt-out has to silence THAT and not a `grep` that could not look at all, and the
  # difference is one digit. Widening it from 1 to 9 left an earlier revision of this suite at
  # a clean sheet through every assertion above, which is why these four exist, and why the
  # planted table above now carries a `grep` that could not look — so the widening is caught
  # by an observation of the GATE over the corpus and not only by a unit test of the function
  # in isolation (bean:0124, and bean:0123 for the shape).
  #
  # THE FUNCTION IS RUN, not read: the definition is cut out of the gate by a `sed` range whose
  # two ends are asserted, and executed under this interpreter with a stub `fail`. What that
  # does NOT prove is that the gate calls it at the right sites — the negative control above is
  # the half that proves it, since an opt-out that tolerated nothing would turn the clean run
  # red, and the planted `grep` that could not look is the half that proves it still refuses.
  local optout
  local stub='fail() { printf "FAIL check %s %s\n" "$1" "$2"; }; '
  optout="$(sed -n '/^absent_ok() {/,/^}/p' "$GATE")"
  check "the opt-out is one function, cut whole out of the gate$at" \
    "1 definition, 1 close" \
    "$(grep -c '^absent_ok() {' "$GATE") definition, $(printf '%s\n' "$optout" | grep -c '^}') close"
  check "and it tolerates grep's 'no match', which is an answer$at" \
    "0" "$("$sh" -c "$stub$optout"'; absent_ok grep -c . /dev/null > /dev/null; echo $?')"
  check "and does not tolerate a grep that could not look, which is a failure$at" \
    "2" "$("$sh" -c "$stub$optout"'; absent_ok grep -c . /no/such/file/__probe_optout__ 2> /dev/null > /dev/null; echo $?')"
  # Both streams, because the record goes to stderr for the reason every `fail` in the guard
  # does — and `grep`'s own diagnostic names the same file, so the match is on the words only
  # this opt-out writes.
  check "and names the command it refused to tolerate$at" \
    "1" "$("$sh" -c "$stub$optout"'; absent_ok grep -c . /no/such/file/__probe_optout__' 2>&1 | grep -c 'could not look.*__probe_optout__')"

  # The glob helper that replaced `ls <glob> 2>/dev/null` at the sites where "no such file" is
  # an answer. `ls` says it with status 1 on BSD and 2 on GNU, so there is no single status to
  # tolerate; this cannot fail, and both halves of that are checked — it finds what is there
  # and it is silent and green on what is not.
  local globfn
  globfn="$(sed -n '/^glob_lines() {/,/^}/p' "$GATE")"
  check "the glob helper is one function, cut whole out of the gate$at" \
    "1 definition, 1 close" \
    "$(grep -c '^glob_lines() {' "$GATE") definition, $(printf '%s\n' "$globfn" | grep -c '^}') close"
  check "and it prints nothing, at exit 0, for a glob that matches nothing$at" \
    "0 lines, rc=0" \
    "$("$sh" -c "$globfn"'; out="$(glob_lines /no/such/dir/__probe_glob__/*.md)"; rc=$?; printf "%s lines, rc=%s" "$(printf %s "$out" | grep -c .)" "$rc"')"
  check "and prints the one file a glob that matches one does$at" \
    "1 lines, rc=0" \
    "$("$sh" -c "$globfn"'; out="$(glob_lines "'"$GATE"'")"; rc=$?; printf "%s lines, rc=%s" "$(printf %s "$out" | grep -c .)" "$rc"')"
}

for s in $SHELLS; do
  run_under "$s"
done

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
echo "docs-lint-gate-test: $pass passed, $fail failed, over $n_majors bash major version(s)."
[ "$fail" -eq 0 ] || exit 1
