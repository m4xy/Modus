#!/usr/bin/env bash
# docs-lint-boundary-probe — bean:0118's boundary table, as a rig rather than as a number.
#
# WHY IT IS COMMITTED. bean:0118's table, and bean:0124's re-derivation of it at four plant
# points, are the evidence that tools/docs-lint.sh's ERR trap catches what it claims to. Both
# were produced by a script that lived in a scratch directory and was deleted, so the table
# could be READ but not RE-RUN — a figure whose measurement needs apparatus nobody has built
# (doc:50-memory-and-evidence#evidence-kinds, the apparatus row). This is the apparatus.
#
# IT IS NOT PART OF qualityCheck and is not meant to be: one full pass is forty-two runs of
# the whole gate over the whole corpus. tools/docs-lint-gate-test.sh is the part of this that
# runs on every build, and it plants eight points rather than these four; what this rig adds
# is the two points that come back NEGATIVE — above the trap, and inside a subshell whose
# status is discarded — which is the residual the gate's comments name and which a suite of
# passing assertions cannot show.
#
# RUN IT AS:  tools/docs-lint-boundary-probe.sh [interpreter]
# The interpreter defaults to the one running this file. bean:0124's table was taken under
# /bin/bash 3.2.57 and listed "the boundary table under bash 5" as not verified; passing
# /opt/homebrew/bin/bash is how that gets closed rather than argued.
#
# NOTHING TRACKED IS WRITTEN. Each row is planted into a COPY at tools/.docs-lint-bp-$$-N.sh,
# the copy is run, the copy is deleted, and `git status --porcelain` is printed at the end so
# the reader can see that (bean:0102). The leading dot keeps the copies out of
# `ls tools/*.sh`, which is how tools/bash-compat-lint.sh chooses its targets.
#
# THE ANCHOR AND THE PLANTED LINE REACH awk THROUGH THE ENVIRONMENT, not through `-v`, which
# processes escapes: the a4 anchor contains a backslash and an `n`, and with `-v` it silently
# matched nothing. That is what the `lines added` column is for — a row that added no line
# planted nothing, and its `exit 0 / reached OK` would then be a fact about the harness.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 2

GATE="$ROOT/tools/docs-lint.sh"
[ -f "$GATE" ] || { printf 'docs-lint-boundary-probe: %s is missing.\n' "$GATE"; exit 2; }

SH="${1:-${BASH:-/bin/bash}}"
[ -x "$SH" ] || { printf 'docs-lint-boundary-probe: %s is not executable.\n' "$SH"; exit 2; }

TMP="$(mktemp -d)" || exit 2
trap 'rm -rf "$TMP"; rm -f "$ROOT"/tools/.docs-lint-bp-$$-*.sh' EXIT

# The four plant points, most-significant first. a1 is bean:0118's own anchor, above every
# mechanism in the file; a2 is the first statement below both the awk shadow and the ERR
# trap; a3 is inside check 2's per-document loop, mid-file; a4 is the first command of a
# `printf … | while read` body, which runs in a subshell of its own.
POINTS="$TMP/points.tsv"
cat > "$POINTS" <<'POINTS_EOF'
a1	set -uo pipefail
a2	FM_FILES="$(ls documentation/*.md documentation/adr/*.md)"
a3	  id="$(field "$f" S id)"
a4	    printf '%s\n' "$prov" | while IFS= read -r a; do
POINTS_EOF

# The rows. Three are shell-level failures that abort the parse or the top-level shell, and
# two are bean:0123's analyser rows, which the awk shadow already closes; all five are planted
# at a1 and a2 only, which is where bean:0118's table has them. The remaining eight are the
# boundary bean:0124 is about, and are planted at all four points.
ROWS_ALL="$TMP/rows-all.tsv"
cat > "$ROWS_ALL" <<'ROWS_EOF'
echo "$__probe_unbound_top"
;;
echo x | ;;
awk "BEGIN { x = = 1 }" /dev/null | cat
probe_z="$(awk "BEGIN { x = = 1 }" /dev/null)"
ROWS_EOF
ROWS_EIGHT="$TMP/rows-eight.tsv"
cat > "$ROWS_EIGHT" <<'ROWS8_EOF'
false
/usr/bin/false
cat /no/such/file/anywhere
cd /no/such/dir/anywhere
false | cat
probe_x="$(echo "$__probe_unbound_sub")"
echo "$__probe_unbound_pipe" | cat
probe_y="$( ;; )"
ROWS8_EOF

printf 'head:     %s\n' "$(git rev-parse HEAD)"
printf 'gate under probe: %s\n' "$GATE"
printf 'interpreter:      %s (%s)\n' "$SH" "$("$SH" --version | head -1)"
printf 'method:   each row planted into a COPY at tools/.docs-lint-bp-%s-N.sh, the copy run,\n' "$$"
printf '          the copy deleted; the rows of one plant point run concurrently\n'
printf '\n'

plant_one() { # plant_one <copy> <anchor> <line>
  BP_ANCHOR="$2" BP_LINE="$3" awk '
    { print }
    $0 == ENVIRON["BP_ANCHOR"] && !done { print ENVIRON["BP_LINE"]; done = 1 }
  ' "$GATE" > "$1"
}

while IFS="$(printf '\t')" read -r tag anchor; do
  [ -n "$tag" ] || continue
  printf '=== plant point %s, immediately after: %s\n' "$tag" "$anchor"
  printf 'occurrences of that anchor in the gate: %s\n' "$(grep -cxF "$anchor" "$GATE")"
  printf '%-46s %s\n' "planted line" "exit OK?  records distinct lines added"

  case "$tag" in
    a1 | a2) cat "$ROWS_ALL" "$ROWS_EIGHT" > "$TMP/rows.tsv" ;;
    *) cp "$ROWS_EIGHT" "$TMP/rows.tsv" ;;
  esac

  i=0
  while IFS= read -r line; do
    [ -n "$line" ] || continue
    i=$((i + 1))
    copy="$ROOT/tools/.docs-lint-bp-$$-$i.sh"
    plant_one "$copy" "$anchor" "$line"
    diff "$GATE" "$copy" | grep -c '^>' > "$TMP/added-$i.txt"
    ( "$SH" "$copy" > "$TMP/out-$i.txt" 2> "$TMP/err-$i.txt"; echo $? > "$TMP/rc-$i.txt" ) &
  done < "$TMP/rows.tsv"
  wait

  i=0
  while IFS= read -r line; do
    [ -n "$line" ] || continue
    i=$((i + 1))
    rc="$(cat "$TMP/rc-$i.txt")"
    reached_ok=no
    grep -q '^docs-lint: OK' "$TMP/out-$i.txt" && reached_ok=yes
    records="$(grep -c 'nothing checked it\|could not look\|examined nothing' "$TMP/err-$i.txt")"
    distinct="$(grep 'nothing checked it\|could not look\|examined nothing' "$TMP/err-$i.txt" | sort -u | grep -c .)"
    printf '%-46s %-4s %-4s %-7s %-8s %s\n' \
      "$line" "$rc" "$reached_ok" "$records" "$distinct" "$(cat "$TMP/added-$i.txt")"
    rm -f "$ROOT/tools/.docs-lint-bp-$$-$i.sh"
  done < "$TMP/rows.tsv"
  printf '\n'
done < "$POINTS"

printf -- '--- git status --porcelain after every probe was removed:\n'
git status --porcelain
printf -- '--- (end)\n'
