#!/usr/bin/env bash
#
# plant.sh — plant a mutation, observe the suite fail, revert.
#
# The procedure is doc:35-testing#load-bearing-evidence: a test nobody has watched fail is a
# claim. This mechanises the two ways that procedure goes wrong, both observed in this
# repository during the bean:0147 / bean:0174 sequence:
#
#   1. A plant run against a dirty tree destroys uncommitted work, because the revert is
#      `git checkout --`. An agent lost a refactor that way. This REFUSES to start unless
#      `git status --porcelain` is empty, before every plant and not once at the beginning.
#
#   2. A plant that fails to apply reports a pass. An agent ran a whole loop against
#      unmodified source because a formatter had rewrapped its plant point; every run was
#      green and proved nothing. This is `set -eu`, aborts when the search text is absent,
#      and prints `planted (N+ M-)` from `git diff --numstat` BEFORE the suite runs. That
#      line is diff-shaped proof of application and is what belongs in the evidence — an
#      independent reviewer re-planting the same mutation reproduces the same numstat.
#
# Usage:
#   tools/plant.sh <id> <gradle-task> <file> <search-file> <replace-file>
#
# `search` and `replace` are files rather than arguments so a multi-line plant point needs no
# quoting. Output is appended to $PLANT_OUT (default: build/plants.txt).
set -eu

id="$1"
task="$2"
file="$3"
search_file="$4"
replace_file="$5"
out="${PLANT_OUT:-build/plants.txt}"
mkdir -p "$(dirname "$out")"

if [ -n "$(git status --porcelain)" ]; then
  echo "ABORT $id: tree is not clean; commit first — the revert below is git checkout --" >&2
  exit 1
fi

python3 - "$file" "$search_file" "$replace_file" <<'PY'
import pathlib
import sys

target = pathlib.Path(sys.argv[1])
search = pathlib.Path(sys.argv[2]).read_text().rstrip("\n")
replace = pathlib.Path(sys.argv[3]).read_text().rstrip("\n")
body = target.read_text()
if search not in body:
    sys.stderr.write("PLANT POINT NOT FOUND in %s:\n%s\n" % (target, search))
    sys.exit(2)
target.write_text(body.replace(search, replace, 1))
PY

numstat="$(git diff --numstat -- "$file" | awk '{print $1"+ "$2"-"}')"
if [ -z "$numstat" ]; then
  echo "ABORT $id: the edit produced no diff" >&2
  git checkout -- "$file"
  exit 1
fi

{
  echo "=== $id"
  echo "--- planted ($numstat) in $file"
  git diff --unified=0 -- "$file" | grep -E '^[-+][^-+]' || true
  echo "--- observed"
} >> "$out"

set +e
./gradlew "$task" 2>&1 |
  grep -E "FAILED|Expected|Exception|AssertionFailedError|BUILD" |
  cut -c1-200 | head -25 >> "$out"
set -e

git checkout -- "$file"
echo "--- reverted; tree clean: [$(git status --porcelain | tr '\n' ' ')]" >> "$out"
echo >> "$out"
