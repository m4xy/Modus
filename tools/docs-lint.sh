#!/usr/bin/env bash
# docs-lint — the eleven mechanical checks of doc:05-authoring-for-agents#checks.
#
# Bash, not Kotlin: the checks are line- and glob-shaped, bash already runs in CI
# and locally, and a JavaExec task would need a source set, a toolchain and a test
# fixture to do the same string matching. No bash 4 feature is used (macOS ships
# 3.2), and every failure is appended to one file so a check that fires inside a
# pipeline subshell still changes the exit status.
#
# Front-matter is parsed against the exact serialisation doc:05 shows: one
# `key: value` per top-level key, list items as `  - item`, `depends_on` as a flow
# list. Anything else is a check 2 failure by design — a parser that accepts every
# YAML spelling of one document lets two spellings drift.
#
# Anchors are read from heading lines only (doc:05 §2: `#anchor` selects the owning
# heading), so an `<a id>` quoted inside a table cell is not an anchor.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 2

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
: > "$TMP/fails.txt"

fail() { printf 'FAIL check %-2s %s\n' "$1" "$2" | tee -a "$TMP/fails.txt"; }
TAB="$(printf '\t')"

FM_FILES="$(ls documentation/*.md documentation/adr/*.md)"
REF_FILES="$FM_FILES $(ls .beans/*.md 2>/dev/null) AGENTS.md CLAUDE.md .github/pull_request_template.md"
REQUIRED_KEYS="id title status superseded_by read_when provides depends_on"

# Beans live under .beans/<prefix><id>--<slug>.md (the hmans/beans on-disk convention);
# .beans.yml's beans.prefix is the one source of that prefix, read here rather than
# duplicated as a second hard-coded copy (doc:05-authoring-for-agents#one-fact-one-place).
BEAN_PREFIX="$(grep -E '^ *prefix:' .beans.yml | head -1 | sed -E 's/^ *prefix: *"?([^"[:space:]]*)"?.*/\1/')"

# ------------------------------------------------------------------ parse ---
# Emits per file: K <key> | S <key> <value> | L <key> <value> | E <check> <msg>
: > "$TMP/fm.tsv"
for f in $FM_FILES; do
  awk -v file="$f" '
    function emit(k, a, b) { printf "%s\t%s\t%s\t%s\n", file, k, a, b }
    function unquote(v) {
      if (v ~ /^".*"$/) { v = substr(v, 2, length(v) - 2) }
      return v
    }
    NR == 1 {
      if ($0 != "---") { emit("E", "1", "no front-matter block"); exit }
      state = 1; next
    }
    state == 1 && $0 == "---" { state = 2; next }
    state == 1 {
      if ($0 ~ /^[a-z_]+:/) {
        key = $0; sub(/:.*$/, "", key)
        val = $0; sub(/^[a-z_]+:[ \t]*/, "", val)
        emit("K", key, "")
        if (val != "") { emit("S", key, unquote(val)) }
        next
      }
      if ($0 ~ /^  - /) {
        val = $0; sub(/^  - /, "", val)
        if (key == "") { emit("E", "2", "list item before any key"); next }
        emit("L", key, unquote(val))
        next
      }
      emit("E", "2", "malformed front-matter line: " $0)
      next
    }
    END {
      if (state == 0) { emit("E", "1", "empty file") }
      else if (state == 1) { emit("E", "1", "unterminated front-matter block") }
    }
  ' "$f" >> "$TMP/fm.tsv"
done

field() {
  awk -F'\t' -v f="$1" -v t="$2" -v k="$3" '$1 == f && $2 == t && $3 == k { print $4 }' "$TMP/fm.tsv"
}
keys() { awk -F'\t' -v f="$1" '$1 == f && $2 == "K" { print $3 }' "$TMP/fm.tsv"; }

# ------------------------------------------------------- checks 1, 2, 3, 7 ---
awk -F'\t' '$2 == "E" { print $1 "\t" $3 "\t" $4 }' "$TMP/fm.tsv" > "$TMP/parse.tsv"
while IFS="$TAB" read -r f n msg; do
  [ -n "$f" ] && fail "$n" "$f: $msg"
done < "$TMP/parse.tsv"

: > "$TMP/provides.tsv"
for f in $FM_FILES; do
  base="$(basename "$f" .md)"
  case "$f" in
    documentation/adr/*) want_id="adr:$base" ;;
    *) want_id="doc:$base" ;;
  esac

  for k in $(keys "$f"); do
    case " $REQUIRED_KEYS " in
      *" $k "*) ;;
      *) fail 2 "$f: unknown key '$k'" ;;
    esac
  done
  for k in $REQUIRED_KEYS; do
    n="$(keys "$f" | grep -cx "$k")"
    [ "$n" = "1" ] || fail 2 "$f: key '$k' appears $n times, expected once"
  done

  id="$(field "$f" S id)"
  [ "$id" = "$want_id" ] || fail 2 "$f: id '$id' does not match the filename (expected '$want_id')"
  [ -n "$(field "$f" S title)" ] || fail 2 "$f: title is absent or empty"

  status="$(field "$f" S status)"
  case "$status" in
    active | draft | superseded) ;;
    *) fail 2 "$f: status '$status' is not active|draft|superseded" ;;
  esac

  sb="$(field "$f" S superseded_by)"
  case "$sb" in
    null | doc:* | adr:*) ;;
    *) fail 2 "$f: superseded_by '$sb' is neither null nor a doc:/adr: reference" ;;
  esac
  if [ "$status" = superseded ] && [ "$sb" = null ]; then
    fail 3 "$f: status is superseded but superseded_by is null"
  fi
  if [ "$status" != superseded ] && [ "$sb" != null ]; then
    fail 3 "$f: superseded_by is '$sb' but status is '$status'"
  fi

  deps="$(field "$f" S depends_on)"
  case "$deps" in
    "["*"]") ;;
    *) fail 2 "$f: depends_on '$deps' is not a flow list" ;;
  esac

  rw_scalar="$(field "$f" S read_when)"
  rw_items="$(field "$f" L read_when)"
  if [ -n "$rw_scalar" ]; then
    [ "$rw_scalar" = always ] || fail 7 "$f: read_when scalar is '$rw_scalar', not 'always'"
    [ -z "$rw_items" ] || fail 7 "$f: read_when is both a scalar and a list"
  elif [ -z "$rw_items" ]; then
    fail 7 "$f: read_when is empty; use the scalar 'always' or at least one predicate"
  else
    printf '%s\n' "$rw_items" | while IFS= read -r p; do
      case "$p" in
        "path: "* | "task: "*) ;;
        *)
          fail 7 "$f: read_when entry '$p' is neither a path: nor a task: predicate"
          continue
          ;;
      esac
      v="${p#*: }"
      v="${v#\"}"
      v="${v%\"}"
      [ -n "$v" ] || fail 7 "$f: read_when entry '$p' has an empty value"
      [ "$v" != '**' ] || fail 7 "$f: read_when uses the banned predicate 'path: **'"
    done
  fi

  prov="$(field "$f" L provides)"
  if [ -z "$prov" ]; then
    [ "$status" = draft ] || fail 2 "$f: provides is empty and status is not draft"
  else
    printf '%s\n' "$prov" | while IFS= read -r a; do
      case "$a" in
        "$want_id"#?*) printf '%s\t%s\n' "$a" "$f" >> "$TMP/provides.tsv" ;;
        *) fail 2 "$f: provides entry '$a' is not '<own id>#<anchor>'" ;;
      esac
    done
  fi
done

# ----------------------------------------------------------------- check 4 ---
# On the bare anchor, not the qualified one: check 2 already forces every entry to
# carry its own document's id, so a qualified duplicate cannot occur and a check on
# it could never fire. The name after `#` is the fact's one name repo-wide.
sed -E 's/^[^#]*#//' "$TMP/provides.tsv" | cut -f1 | sort | uniq -d > "$TMP/dupes.txt"
while IFS= read -r dup; do
  [ -n "$dup" ] || continue
  owners="$(awk -F'\t' -v a="#$dup" '$1 ~ a"$" { printf "%s ", $1 }' "$TMP/provides.tsv")"
  fail 4 "anchor '#$dup' is provided by more than one document: $owners"
done < "$TMP/dupes.txt"

# ----------------------------------------------------------------- check 5 ---
for f in $FM_FILES; do
  grep -oE '^#+ .*<a id="[a-z0-9-]+">' "$f" |
    sed -E 's/.*<a id="([a-z0-9-]+)">.*/\1/' | sort -u > "$TMP/declared.txt"
  awk -F'\t' -v f="$f" '$2 == f { print $1 }' "$TMP/provides.tsv" |
    sed -E 's/^[^#]*#//' | sort -u > "$TMP/promised.txt"
  comm -13 "$TMP/declared.txt" "$TMP/promised.txt" | while IFS= read -r a; do
    [ -n "$a" ] && fail 5 "$f: provides '#$a' but no heading declares <a id=\"$a\">"
  done
  comm -23 "$TMP/declared.txt" "$TMP/promised.txt" | while IFS= read -r a; do
    [ -n "$a" ] && fail 5 "$f: declares <a id=\"$a\"> but does not provide it"
  done
done

# ----------------------------------------------------------------- check 6 ---
# Recognition requires the full fixed-width id (doc:05 §2), so a template
# placeholder (`bean:NNNN`) is not a reference. Fenced blocks are skipped: they
# hold the literal templates an author copies, not live references.
REF_RE='(doc:[0-9]{2}[a-z0-9-]*|doc:README|bean:[0-9]{4}[a-z0-9-]*|adr:[0-9]{4}[a-z0-9-]*|rule:[a-z]+/[A-Za-z][A-Za-z0-9]*)(#[a-z0-9-]+)?'
: > "$TMP/refs.tsv"
for f in $REF_FILES; do
  awk '/^```/ { fence = !fence; next } !fence { print }' "$f" |
    grep -oE "$REF_RE" |
    awk -v f="$f" '{ print f "\t" $0 }' >> "$TMP/refs.tsv"
done

ci_jobs() {
  awk '
    /^jobs:/ { inj = 1; next }
    inj && /^[a-z]/ { inj = 0 }
    inj && /^  [a-z][a-z0-9_-]*:/ { k = $1; sub(/:$/, "", k); print k }
  ' .github/workflows/ci.yml
}

sort -u "$TMP/refs.tsv" > "$TMP/refs.uniq"
while IFS="$TAB" read -r src ref; do
  [ -n "$ref" ] || continue
  name="${ref%%#*}"
  anchor=""
  case "$ref" in *#*) anchor="${ref#*#}" ;; esac
  kind="${name%%:*}"
  rest="${name#*:}"
  if [ "$kind" = rule ]; then
    tool="${rest%%/*}"
    ident="${rest#*/}"
    case "$tool" in
      ci) n="$(ci_jobs | grep -cx "$ident")" ;;
      archunit) n="$(grep -rhoE "(val|fun) $ident\b" architecture-tests | grep -c .)" ;;
      detekt) n="$(grep -cE "^[[:space:]]+$ident:" config/detekt/detekt.yml)" ;;
      *) n=0 ;;
    esac
    [ "$n" = "1" ] || fail 6 "$src: '$ref' resolves to $n targets, expected exactly 1"
    continue
  fi
  case "$kind" in
    doc) target="$(ls documentation/"$rest"*.md 2>/dev/null)" ;;
    adr) target="$(ls documentation/adr/"$rest"*.md 2>/dev/null)" ;;
    bean) target="$(ls .beans/"${BEAN_PREFIX}${rest}"*.md 2>/dev/null)" ;;
    *) target="" ;;
  esac
  n="$(printf '%s' "$target" | grep -c .)"
  if [ "$n" != "1" ]; then
    fail 6 "$src: '$ref' resolves to $n files, expected exactly 1"
    continue
  fi
  [ -n "$anchor" ] || continue
  tid="$(field "$target" S id)"
  found="$(awk -F'\t' -v a="$tid#$anchor" -v f="$target" '$1 == a && $2 == f' "$TMP/provides.tsv" | grep -c .)"
  [ "$found" = "1" ] || fail 6 "$src: '$ref' — $target does not provide '#$anchor'"
done < "$TMP/refs.uniq"

# ----------------------------------------------------------------- check 8 ---
# Both bounds live in doc:README#changing-this-package; this reads them there.
max_lines="$(grep -oE 'max_lines: [0-9]+' documentation/README.md | head -1 | grep -oE '[0-9]+')"
min_lines="$(grep -oE 'min_lines: (none|[0-9]+)' documentation/README.md | head -1 | sed 's/min_lines: //')"
if [ -z "$max_lines" ] || [ -z "$min_lines" ]; then
  fail 8 "documentation/README.md states no 'max_lines:'/'min_lines:' budget for docs-lint to read"
else
  for f in documentation/*.md; do
    n="$(grep -c '' "$f")"
    [ "$n" -le "$max_lines" ] || fail 8 "$f: $n lines, over the $max_lines ceiling"
    if [ "$min_lines" != none ] && [ "$n" -lt "$min_lines" ]; then
      fail 8 "$f: $n lines, under the $min_lines floor"
    fi
  done
fi
agents_lines="$(grep -c '' AGENTS.md)"
[ "$agents_lines" -le 120 ] || fail 8 "AGENTS.md: $agents_lines lines, over the 120 ceiling"

# ----------------------------------------------------------------- check 9 ---
grep -nE '^\|.*derived' AGENTS.md > "$TMP/derived.txt"
while IFS= read -r row; do
  [ -n "$row" ] || continue
  ln="${row%%:*}"
  case "$row" in
    *doc:[0-9][0-9]*) ;;
    *) fail 9 "AGENTS.md:$ln: derived row cites no doc: id" ;;
  esac
  if printf '%s' "$row" | grep -qE '(path|task):[[:space:]]*[A-Za-z0-9*/._(|-]'; then
    fail 9 "AGENTS.md:$ln: derived row states a path:/task: value instead of citing its doc: id"
  fi
done < "$TMP/derived.txt"

# ---------------------------------------------------------------- check 10 ---
# A bare `beans/NNNN` or `.beans/NNNN` path in prose is structurally invisible
# to check 6, which only resolves typed `bean:NNNN` references
# (doc:05#reference-syntax). This migration proved a bare path survives the
# directory it names being deleted with no lint signal at all — a typed
# reference is the only legal way to point at a bean.
grep -noE '\bbeans/[0-9]' documentation/*.md AGENTS.md CLAUDE.md 2>/dev/null |
  while IFS=: read -r f ln _; do
    fail 10 "$f:$ln: bare beans/ path in prose; use a typed bean:NNNN reference (doc:05#reference-syntax)"
  done

# ---------------------------------------------------------------- check 11 ---
# A completed bean is final except for appended amendments
# (adr:0005-evidence-lives-in-the-work-item#finalisation).
#
# Immutability is a property of a DIFF, not of a file, so each changed bean is
# classified by the `status:` it has on the MERGE BASE rather than on the branch.
# A bean moving in-progress -> completed in this change is a legal edit to a
# not-yet-completed bean; the identical edit to one already completed is not.
# Reading the branch instead would either block every closure or permit every
# edit, and both fail silently.
#
# No base means no diff to judge — a detached checkout with no `origin/main` makes
# the check inert by construction rather than guessing. On `main` itself the base
# is HEAD, so the check still sees uncommitted edits.
BASE=""
if git rev-parse --verify -q origin/main >/dev/null 2>&1; then
  BASE="$(git merge-base origin/main HEAD 2>/dev/null || true)"
fi
# The diff is BASE against the WORKING TREE, not against HEAD: every other check
# here reads the working tree, and a check that only sees committed content would
# pass locally and fail in CI after the commit — the slowest possible feedback.
if [ -n "$BASE" ]; then
  git diff --name-only "$BASE" -- .beans 2>/dev/null > "$TMP/changed-beans.txt" || : > "$TMP/changed-beans.txt"
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    # A bean that did not exist on the base is new; there is nothing to protect.
    git cat-file -e "$BASE:$f" 2>/dev/null || continue
    was="$(git show "$BASE:$f" 2>/dev/null | sed -n 's/^status:[[:space:]]*//p' | head -1)"
    [ "$was" = "completed" ] || continue

    if [ ! -f "$f" ]; then
      fail 11 "$f: a completed bean was deleted; it is the durable evidence record (adr:0005#finalisation)"
      continue
    fi

    # The base content must survive verbatim as the head of the new file.
    old_n="$(git show "$BASE:$f" | grep -c '')"
    if ! git show "$BASE:$f" | diff -q - <(head -n "$old_n" "$f") >/dev/null 2>&1; then
      fail 11 "$f: completed bean edited in place; it may only gain '## Amendments' entries (adr:0005#amendments)"
      continue
    fi

    # Anything appended must belong to an Amendments section.
    if ! git show "$BASE:$f" | grep -q '^## Amendments'; then
      first_new="$(tail -n +"$((old_n + 1))" "$f" | grep -m1 '[^[:space:]]' || true)"
      if [ -n "$first_new" ] && [ "$first_new" != "## Amendments" ]; then
        fail 11 "$f: appended '$first_new'; a completed bean may only gain a '## Amendments' section (adr:0005#amendments)"
        continue
      fi
    fi

    # Every amendment states when, by whom, what was claimed, what was found and
    # the evidence. An amendment that only asserts a correction is the thing
    # doc:00-constitution#evidence-rule forbids everywhere else.
    if grep -q '^## Amendments' "$f"; then
      sed -n '/^## Amendments/,$p' "$f" | grep '^### ' > "$TMP/amend-heads.txt" || :
      while IFS= read -r h; do
        [ -n "$h" ] || continue
        case "$h" in
          '### '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]' · bean:'[0-9][0-9][0-9][0-9]) ;;
          *) fail 11 "$f: amendment heading '$h' is not '### YYYY-MM-DD · bean:NNNN'" ;;
        esac
      done < "$TMP/amend-heads.txt"
      n_amend="$(grep -c . "$TMP/amend-heads.txt" || true)"
      for k in Claimed Found Evidence; do
        n_k="$(sed -n '/^## Amendments/,$p' "$f" | grep -c "\*\*$k:\*\*" || true)"
        if [ "$n_amend" != "$n_k" ]; then
          fail 11 "$f: $n_amend amendment(s) but $n_k '**$k:**' line(s) (adr:0005#amendments)"
        fi
      done
    fi
  done < "$TMP/changed-beans.txt"
fi

# -------------------------------------------------------------------- done ---
n_fail="$(grep -c . "$TMP/fails.txt")"
if [ "$n_fail" -gt 0 ]; then
  echo "docs-lint: $n_fail failure(s)."
  exit 1
fi
printf 'docs-lint: OK — %s documents, %s anchors, %s references.\n' \
  "$(printf '%s\n' $FM_FILES | grep -c .)" \
  "$(grep -c . "$TMP/provides.tsv")" \
  "$(grep -c . "$TMP/refs.uniq")"
