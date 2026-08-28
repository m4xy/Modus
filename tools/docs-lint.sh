#!/usr/bin/env bash
# docs-lint — the nine mechanical checks of doc:05-authoring-for-agents#checks.
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
REF_FILES="$FM_FILES $(ls beans/*.md) AGENTS.md CLAUDE.md .github/pull_request_template.md"
REQUIRED_KEYS="id title status superseded_by read_when provides depends_on"

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
    bean) target="$(ls beans/"$rest"*.md 2>/dev/null)" ;;
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
