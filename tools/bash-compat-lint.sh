#!/usr/bin/env bash
# bash-compat-lint — makes tools/docs-lint.sh's "no bash 4 feature is used" a gate rather
# than a claim (bean:0049).
#
# THE PROBLEM IT SOLVES IS NOT PORTABILITY, IT IS DETERMINISM. tools/docs-lint.sh and the
# two analysers under tools/lib/ have three consecutive changes queued behind them. Until
# this ran, `docsLint` invoked `commandLine("bash", …)` — resolved through PATH, which is
# Homebrew's bash 5.3.9 on the development machine and bash 5 in CI — so every one of those
# changes would have been validated against whichever shell the author happened to have.
# This repository has just been bitten by that exact shape at one remove: `grep` on an
# agent's interactive PATH was a ugrep shim while /usr/bin/grep, `bash -c` and CI were BSD
# grep, and a fence captured under one was irreproducible under the other (bean:0115).
# build.gradle.kts now names /bin/bash, and this script reports which interpreter it got.
#
# THE INTERPRETER IS NOT ENOUGH, AND THAT IS MEASURED. bean:0049 asks for the compatibility
# claim to be observed rejecting "an associative array or `mapfile`". Neither is rejected by
# running under a real 3.2. Under /bin/bash (3.2.57) a planted `declare -A seen` writes
# `declare: -A: invalid option` to stderr, exits 0, and leaves an INDEXED array behind;
# `mapfile` writes `command not found` and exits 0 from the script, which has no `set -e`.
# Five further constructs diverge in total silence — `{1..9..2}` expands to itself,
# $SRANDOM / $EPOCHSECONDS / $EPOCHREALTIME / $BASHPID are simply unset. The differential
# this file was distilled from is in bean:0049, in three places, none of them per row:
# evidence entry 6, whose table is the six families that diverge silently or behind a
# diagnostic `set -e` would have caught, beside the count of families `/bin/bash -n` rejects on
# its own; the amendment "Five constructs absent from rows that already enumerate their
# siblings", which runs eleven further constructs under both interpreters and prints each one's
# stdout, stderr and exit status; and the two second-round amendments, which do the same for
# `[ -v name ]`, `test -v name` and the nine legal lines that round found this gate rejecting.
# Amendments are cited by title, not by ordinal, because the ordinals move. None is a "full"
# differential of bash 5 against bash 3.2 and this file does not claim one: it is a denylist of
# what was measured, and it says so below.
#
# So the gate is two halves, neither of which subsumes the other:
#
#   PARSE   `$BASH -n` over every script, under the interpreter the gate pinned. Fails
#           CLOSED, and on macOS catches every SYNTAX-level bash 4 feature whether or not
#           anyone thought to name it — including ones absent from the pattern file. On
#           Linux it is a bash 5 parse and proves only that the scripts parse.
#   SCAN    tools/lib/bash32-forbidden.tsv, applied by tools/lib/bash32-scan.awk. Fails
#           OPEN, being a denylist (doc:00-constitution#observed-failing on enumerating
#           shapes), but runs IDENTICALLY on macOS and on Linux CI and catches the two
#           constructs bean:0049 names, which the parse half cannot.
#
# THE SCAN PROVES IT DISCRIMINATES ON EVERY RUN, not once by hand. Firing on every input is
# also firing (doc:50-memory-and-evidence#evidence-kinds), so each rule's sample violation
# is planted into a fixture here and the scan must find it, find it exactly once, and find
# nothing at all in a clean fixture of 3.2-legal near-misses. All three, every invocation. A
# rule that stops matching its own sample, or starts matching a second rule's, fails the
# gate rather than quietly widening or narrowing what is enforced.
#
# bash 3.2 (macOS): this script is scanned and parsed by itself, so it observes its own rule.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 2

PATTERNS="tools/lib/bash32-forbidden.tsv"
SCANNER="tools/lib/bash32-scan.awk"
TAB="$(printf '\t')"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

rc=0
fail() { printf 'FAIL bash-compat  %s\n' "$1"; rc=1; }
lines() { awk 'END { print NR }' "$1"; }
scan() { awk -v PAT="$PATTERNS" -f "$SCANNER" "$@"; }

for required in "$PATTERNS" "$SCANNER"; do
  if [ ! -f "$required" ]; then
    printf 'bash-compat: %s is missing; the scan would examine nothing.\n' "$required"
    exit 2
  fi
done

# ------------------------------------------------------- the governed set ---
# A glob, not a list: a shell script added to tools/ is covered the day it lands, which is
# the property a hand-maintained list loses on its first omission.
ls tools/*.sh > "$TMP/targets.txt"
n_targets="$(lines "$TMP/targets.txt")"
TARGETS="$(cat "$TMP/targets.txt")"

# The same vacuity guard the rule count gets below, for the same reason: a run that
# examined nothing may not report success (doc:00-constitution#observed-failing). It is
# unreachable while this script is itself a tools/*.sh, which is a property of the glob
# rather than a guarantee, and `scan` with no arguments would read stdin and block — so
# this exits rather than accumulating into rc.
if [ "$n_targets" -eq 0 ]; then
  printf 'bash-compat: no script matched tools/*.sh; the parse and the scan would examine nothing.\n'
  exit 2
fi

# ---------------------------------------------------------- interpreter ----
# $BASH is the path of the running shell, so this reports what build.gradle.kts actually
# invoked rather than what it meant to. On macOS /bin/bash is 3.2.57 and the parse below is
# a genuine 3.2 parse; on Linux CI /bin/bash is bash 5 and it is not. Printed either way:
# the next change to the analyser needs to know which one validated it.
printf 'bash-compat: interpreter %s (bash %s)\n' "${BASH:-unknown}" "${BASH_VERSION:-unknown}"

# ---------------------------------------------------------------- parse ----
for f in $TARGETS; do
  if ! "${BASH:-bash}" -n "$f" 2> "$TMP/parse.err"; then
    fail "$f does not parse under ${BASH:-bash} (bash ${BASH_VERSION:-unknown}): $(cat "$TMP/parse.err")"
  fi
done

# ------------------------------------------------- the scan discriminates ---
awk -F"$TAB" '
  /^[ \t]*#/ { next }
  /^[ \t]*$/ { next }
  NF >= 3 && $1 != "" && $3 != "" { printf "%s\t%s\n", $1, $3 }
' "$PATTERNS" > "$TMP/rules.tsv"
n_rules="$(lines "$TMP/rules.tsv")"

if [ "$n_rules" -eq 0 ]; then
  fail "no rules loaded from $PATTERNS; the scan would report every script clean"
fi

n_planted=0
while IFS="$TAB" read -r rule sample; do
  n_planted=$((n_planted + 1))
  printf '%s\n' "$sample" > "$TMP/plant.sh"
  scan "$TMP/plant.sh" > "$TMP/plant.out"
  n_hit="$(lines "$TMP/plant.out")"
  got="$(awk -F': ' 'NR == 1 { print $2 }' "$TMP/plant.out")"
  if [ "$n_hit" -ne 1 ]; then
    fail "rule '$rule': its own sample violation was detected $n_hit times, not once — the sample is <$sample>"
  elif [ "$got" != "$rule" ]; then
    fail "rule '$rule': its own sample violation was attributed to '$got'; two rules overlap"
  fi
done < "$TMP/rules.tsv"

# The negative control. Every line is legal bash 3.2 and every line is a deliberate
# near-miss of some rule in the pattern file — there is no filler here, so a rule that
# matched every line would fail this assertion instead of satisfying the planted-sample
# ones above. Naming what each line guards, in fixture order, because a comment that
# counts ("the last four") stops being true the moment a line is added, and did:
#
#   declare -a list                       indexed array, not `declare -A`
#   declare -i n=0                        an option cluster with no g, l, u, n or A in it
#   declare -x PATH                       likewise
#   local -r frozen=1                     likewise, on `local`
#   run > "$log" 2>&1                     `>&` that is not `&>>`
#   case … a) : ;; *) : ;; esac           `;;` that is not `;;&`
#   for i in 1 2 3; do :; done            a brace-free list, not `{1..9..2}`
#   shopt -s extglob                      a shopt option bash 3.2 has
#   echo "${name}" … "${x:-a,b}" …        a comma inside a default, `[@]` that is not a
#                                         parameter transformation, `${#arr[@]}`
#   printf '%s (%s)\n' one two            a `%s (` that is not a `%(…)T` time format
#   printf 'bash %s\n' "${BASH_VERSION…}" a BASH_ prefix that is not $BASHPID
#
# and the lines that are here because the pattern file REJECTED them while they are correct
# bash 3.2 — the reason `test-v`, `case-modification` and `printf-time-format` are now
# anchored to where their operator can occur. `(review)` marks the ones review ran against
# the scanner and pasted verbatim; the rest are the same shape, pinning the anchoring from
# the sides review did not exercise:
#
#   if [[ -n "$(command -v jq)" ]]        (review) the portable command-existence idiom;
#                                         the `-v` is `command`'s, not `[[`'s
#   if [[ "$flag" == -v ]]                (review) `-v` as a compared VALUE
#   [[ -f "$f" ]] && grep -v x "$f"       `-v` as another command's option after `&&`
#   trimmed="${csv%,}"                    (review) `%` suffix trim; a comma is not `${v,,}`
#   joined="${list#,}"                    (review) `#` prefix trim, likewise
#   printf 'coverage %d%%(min)\n' 90      (review) a literal `%%` before a parenthesis
#   printf -v now %s hi                   `printf -v` onto a plain name, not `a[0]`
#   echo "${x}<div>"                      `}` before `<` that is a parameter expansion,
#                                         not a {varname} file descriptor
#
# and the lines the SECOND review round found rejected, in rules the first round's anchoring
# pass never looked at. Each was run under /bin/bash 3.2.57 and /opt/homebrew/bin/bash 5.3.9
# and came out byte-identical on stdout, stderr and exit status, so each is unambiguously
# legal 3.2; the differential is in bean:0049's fourth 2026-09-04 amendment. `(review)` again
# marks the ones review pasted verbatim:
#
#   cp {../src,../dst} .                  (review) two `..` in one brace list, which is not
#                                         the `{1..9..2}` step form
#   for d in {../x,../y}; do :; done      (review) the same, in the position that reads most
#                                         like a range
#   printf 'coverage %d%%(min)T\n' 90     (review) a doubled `%%` is not a format introducer,
#                                         even when a `T` follows the parenthesis
#   printf 'see %s\n' "use %(%Y)T…"       (review) a `%(…)T` in an ARGUMENT, after the format
#                                         string has closed
#   echo "{div}<br>"                      (review) a brace-delimited placeholder before `<`
#   sed 's/{x}<//' f                      (review) the same, inside a sed script
#   echo "${email:-me@u}"                 (review) an `@u` inside a DEFAULT value, not the
#                                         `${v@u}` transformation
#   [ -f "$f" ] && grep -v x "$f"         `-v` as another command's option after a single
#                                         `[` test — the near-miss for `test-v-bracket`
#   bash tools/docs-lint-test.sh -v       a command name ENDING in `test`, not the `test`
#                                         builtin — the near-miss for `test-v-command`
#
# Adding a line to the fixture means adding it to the list above. The two are checked
# against each other by nothing, which is why they are kept adjacent and short.
cat > "$TMP/clean.sh" <<'CLEAN'
declare -a list
declare -i n=0
declare -x PATH
local -r frozen=1
run > "$log" 2>&1
case "$x" in a) : ;; *) : ;; esac
for i in 1 2 3; do :; done
shopt -s extglob
echo "${name}" "${arr[0]}" "${#arr[@]}" "${x:-a,b}" "${arr[@]}"
printf '%s (%s)\n' one two
printf 'bash %s\n' "${BASH_VERSION:-unknown}"
if [[ -n "$(command -v jq)" ]]; then :; fi
if [[ "$flag" == -v ]]; then :; fi
[[ -f "$f" ]] && grep -v x "$f"
trimmed="${csv%,}"
joined="${list#,}"
printf 'coverage %d%%(min)\n' 90
printf -v now %s hi
echo "${x}<div>"
cp {../src,../dst} .
for d in {../x,../y}; do :; done
printf 'coverage %d%%(min)T\n' 90
printf 'see %s\n' "use %(%Y)T for time"
echo "{div}<br>"
sed 's/{x}<//' f
echo "${email:-me@u}"
[ -f "$f" ] && grep -v x "$f"
bash tools/docs-lint-test.sh -v
CLEAN
scan "$TMP/clean.sh" > "$TMP/clean.out"
n_clean="$(lines "$TMP/clean.out")"
if [ "$n_clean" -ne 0 ]; then
  cat "$TMP/clean.out"
  fail "the negative control is not clean: $n_clean hit(s) on bash 3.2-legal source"
fi

# ----------------------------------------------------------------- scan ----
scan $TARGETS > "$TMP/findings.txt"
n_findings="$(lines "$TMP/findings.txt")"
if [ "$n_findings" -ne 0 ]; then
  cat "$TMP/findings.txt"
  fail "$n_findings bash 4 construct(s) in scripts that claim bash 3.2 compatibility"
fi

if [ "$rc" -ne 0 ]; then
  printf 'bash-compat: FAILED.\n'
  exit 1
fi

printf 'bash-compat: OK — %s scripts parsed, %s rules, %s planted violations each caught exactly once, %s hits on the negative control, %s findings.\n' \
  "$n_targets" "$n_rules" "$n_planted" "$n_clean" "$n_findings"
