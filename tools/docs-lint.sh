#!/usr/bin/env bash
# docs-lint — the mechanical checks of doc:05-authoring-for-agents#checks. That table
# is the one place the checks are counted; a count repeated here would drift, and did.
#
# Bash, not Kotlin: the checks are line- and glob-shaped, bash already runs in CI
# and locally, and a JavaExec task would need a source set, a toolchain and a test
# fixture to do the same string matching. No bash 4 feature is used: macOS ships 3.2 and
# /bin/bash there is still 3.2.57, which build.gradle.kts now pins the gate to. That is a
# gate rather than a comment — tools/bash-compat-lint.sh parses this file under the pinned
# interpreter and scans it for the constructs 3.2 lacks, from qualityCheck (bean:0049).
# Every failure is appended to one file so a check that fires inside a pipeline subshell
# still changes the exit status.
#
# Front-matter is parsed against the exact serialisation doc:05 shows: one
# `key: value` per top-level key, list items as `  - item`, `depends_on` as a flow
# list. Anything else is a check 2 failure by design — a parser that accepts every
# YAML spelling of one document lets two spellings drift.
#
# Anchors are read from heading lines only (doc:05 §2: `#anchor` selects the owning
# heading), so an `<a id>` quoted inside a table cell is not an anchor.
#
# Check 14's analyser lives in tools/lib/, split into where a fenced block is and what
# that means, because bean:0063 showed the two concerns fail separately: a fence marker
# that was CONTENT inverted the analyser's sense of inside and outside for the rest of the
# file, and no verdict test could tell that apart from a correct reading. What it
# perceives and what it decides are tested separately by tools/docs-lint-test.sh, which
# qualityCheck runs.
#
# This script's own FAILURE PATH — that an analyser dying makes the gate go red rather than
# print OK — is tested by tools/docs-lint-gate-test.sh, also from qualityCheck. It is a
# separate file because it can only observe this one by running the whole gate (bean:0118).
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 2

# Both statuses are checked because EVERY failure record in this file is a line of
# $TMP/fails.txt and the exit status is that file's line count: a $TMP that was never
# created makes every record vanish into a directory that is not there, and the gate then
# prints `docs-lint: OK` at exit 0 with a `FAIL check` line for every lost record above it on
# its own stdout — 287 of them on the corpus at the head bean:0124 records, which is a figure
# of that corpus and moves with it (doc:50-memory-and-evidence#corpus-figures). Measured, by
# making mktemp fail: bean:0124. These two lines are above the ERR trap because the trap
# cannot record before the file it records into exists.
TMP="$(mktemp -d)" || exit 2
trap 'rm -rf "$TMP"' EXIT
: > "$TMP/fails.txt" || exit 2

fail() { printf 'FAIL check %-2s %s\n' "$1" "$2" | tee -a "$TMP/fails.txt"; }
TAB="$(printf '\t')"

# THE FAILURE PATH for the runtime errors that are not an analyser (bean:0124). The awk
# wrapper below covers the analysers. This covers the rest of bean:0118's boundary table —
# a `false`, a missing file, a failed `cd`, a failed pipeline element, and an unbound
# variable expanded inside `$( )` or inside a pipeline element — every one of which reached
# the `OK` line at exit 0, three of them having written nothing at all to stderr.
#
# AN ERR TRAP, NOT `set -e`. errexit abandons the run at the first non-zero status, so a
# gate whose purpose is to report every defect in one pass would report one and stop —
# before the counts line that is its own vacuity assertion. The trap records through the
# same `fail` every check uses and lets the run continue: the exit status changes and
# nothing else does.
#
# AND NOT `set -E` (errtrace), which is the measured half of that choice. Without errtrace
# the trap is not inherited by functions, subshells or command substitutions, so a failure
# inside one is recorded once, at the enclosing statement whose status it makes non-zero.
# With errtrace the same failure records three and four times, at a depth that is bash's
# business and not this gate's — and bean:0123 already had to stop asserting on one number
# that varied by interpreter.
#
# WHAT IT COSTS is a failure whose status an enclosing construct then discards: a command in
# the body of a `while read` loop that is itself the last element of a pipeline, that body
# running in a subshell of its own. The loops of that shape here are check 7's over
# `read_when` and check 2's over `provides`, both fed by `printf`; check 5's two, fed by
# `comm`; and check 10's, fed by a `grep`. Named rather than counted, since the count moves
# with the next check anyone writes.
#
# UNDER THE PINNED /bin/bash 3.2.57 THAT RESIDUAL IS LARGER, and the two halves are one fact
# rather than two: 3.2 does not reach the trap AT ALL for a pipeline whose last element is a
# compound command, so there it is not only the body that is unseen but the whole pipeline,
# its final command included. Under bash 5 the pipeline is seen and the body is not. Both
# measured in bean:0124, and tools/docs-lint-gate-test.sh prints the ten-shape table it was
# measured with on every run. Closed by neither this change nor bean:0123.
#
# Redirected to stderr for the reason the awk wrapper is: a call site inside `$( )` has its
# stdout captured, and a record on stdout becomes part of the value the caller parses.
#
# Every field is passed in rather than read inside the handler, where `$BASH_COMMAND` names
# the handler and `$LINENO` names its own line. `$BASH_COMMAND` on a failing PIPELINE holds an
# element that is very often NOT the one that failed, and WHICH element differs by
# interpreter: the LAST under both when the pipeline ends in a simple command or a function,
# and the FIRST under bash 5 when it ends in a compound command, where 3.2.57 does not reach
# the trap at all. So the statuses go in beside it and are what the record is read on:
# `false | cat` reports `'cat'` and `1 0`, and the `1` is what says which end broke.
#
# ONE RECORD IS ONE LINE, which is why the command is flattened and clipped here.
# `docs-lint: N failure(s).` is a count of the LINES of $TMP/fails.txt, so a record
# carrying a nineteen-line awk program verbatim reported `20 failure(s).` for two records —
# measured, on check 12's analyser, in bean:0124. The line number is where the command ENDS,
# which is what `$LINENO` holds for a multi-line command AT THE TOP LEVEL — and only there.
# Inside a `for` or `while` body under 3.2.57 it is the LOOP HEADER instead: this file's own
# check 5 record reads `line 301` under 3.2.57 and `line 303` under 5.3.9 for one command, and
# the runner reported `226` where 3.2.57 reported `207`. Both measured in bean:0124; neither
# is asserted on, for the reason bean:0123 gives about numbers the interpreter chooses.
docs_lint_err() { # <status> <command> <line> <PIPESTATUS as one word-separated string>
  local cmd="$2"
  local extra=""
  cmd="${cmd//$'\n'/ }"
  if [ "${#cmd}" -gt 120 ]; then cmd="${cmd:0:117}..."; fi
  case "$4" in *" "*) extra=" (pipeline exited $4, left to right)" ;; esac
  fail - "line $3: a command exited $1 and nothing checked it: '$cmd'$extra" >&2
}
trap 'docs_lint_err $? "$BASH_COMMAND" "$LINENO" "${PIPESTATUS[*]}"' ERR

# `grep` exits 1 when the pattern is ABSENT — an answer about the input — and 2 or more when
# it could not look, which is a failure. `grep -c` says the same thing twice: it exits 1
# whenever the count it has just printed is 0. So WHEREVER THIS FILE READS THE OUTPUT — the
# count, the matching lines, the file they were written into — the status restates something
# the gate already holds, and a trap armed on it turns a CLEAN TREE red. That is worse than
# the gap, because it gets removed rather than fixed (doc:00-constitution#observed-failing).
#
# WHICH SITES: derived from the sentence above, not from what one corpus happens to exercise.
# The first version of this opt-out held the two sites a red-corpus run was seen firing at,
# and inferred from that run that every other site was "already reported by the check beside
# it". Two were not, and both made the gate red on a tree that breaks no documented rule: a
# `status: draft` document declaring no anchor at all, which doc:05-authoring-for-agents §2's
# front-matter table permits in as many words, and an `AGENTS.md` carrying no `derived` row,
# which nothing requires. A site belongs here when its "nothing found" is a legal state of a
# LEGAL tree, or is a defect the check beside it already names in full. It stays ARMED when
# "nothing found" means the tree is broken and no check says so — `.beans.yml` with no
# `prefix:` is the only such site left in this file, and the comment on it says why.
#
# THE RULE HAS A THIRD OUTCOME, and the sentence above read as if it had two. Check 11's
# `sed -n '/^## Amendments/,$p' | absent_ok grep '^### '` is a site whose "nothing found" means
# the tree IS broken and no check said so — and the answer there is neither to arm it nor to
# leave it, but to READ the answer: the opt-out is correctly derived and the missing thing was
# a `fail`. That `fail 11` is now beside it. A site the derivation sends here can still be a
# site whose result nobody checks, and those are two questions, not one (bean:0124).
#
# `ls` is deliberately NOT routed through this. Its "no such file" is status 1 on the BSD `ls`
# macOS ships and 2 on GNU coreutils, so tolerating it here would mean tolerating 2 — exactly
# the widening this opt-out exists to refuse, and the one tools/docs-lint-gate-test.sh now
# plants a `grep` that could not look to catch. `glob_lines` below answers the same question
# with the shell, which cannot fail, so there is no status left to tolerate.
#
# What the trap SEES is separately the interpreter's business: under bash 3.2 a pipeline whose
# last element is a compound command does not reach the trap and under bash 5 it does, so an
# audit taken under the pinned interpreter alone is incomplete — measured in bean:0124, after
# the runner went red on a clean tree at a site this machine could not see. The command below
# runs inside an `||` list, the one context errexit and the ERR trap both exempt.
absent_ok() { # absent_ok <command…> — status 1 is "no match"; 2 and above still record
  local ec=0
  "$@" || ec=$?
  [ "$ec" -le 1 ] && return 0
  # THE SITE NAMES ITSELF, because the trap cannot name it. The ERR trap fires at the caller
  # for this function's non-zero return, and `$BASH_COMMAND` there holds `return "$ec"` — the
  # last command THIS function ran — under both 3.2.57 and 5.3.9, measured in bean:0124. So a
  # `grep` that could not look would otherwise be recorded as a `return`. One such failure is
  # two records, this one and the trap's, exactly as one dead analyser is two.
  fail - "an opted-out command exited $ec and could not look, which is not 'no match': $*" >&2
  return "$ec"
}

# The members of a glob that NAME something, one per line: `ls <glob> 2>/dev/null` without the
# status. In the `ls … | grep -c .` this replaces BOTH elements report "none" with a non-zero
# status that is not a failure, and `ls`'s number differs by implementation, so there is no
# single status to tolerate. Answering with the shell's own test leaves nothing to opt out of.
#
# `-e` ALONE WOULD BE A DETECTION REGRESSION, and it shipped as one. `-e` follows symlinks, so
# a path the shell globbed and this function cannot stat is dropped — silently, at status 0,
# because the whole point of the function is that it has no status to fail with. A broken
# symlink `.beans/modus-0199--a-broken-symlink-bean.md -> /no/such/target` was reported by
# `ls` on the base of this branch as five records (three analysers that could not open it,
# `front-matter carries 0 '# modus-…' id markers`, and `113 bean file(s) on disk but 112
# parsed`) and by `-e` alone as `docs-lint: OK … 112 beans, … 112 bean ids` at exit 0 —
# measured at 160e1ff under /opt/homebrew/bin/bash 5.3.9, both halves, and the assertion in
# tools/docs-lint-gate-test.sh named "a bean the shell can glob but not stat" now pins it.
#
# THE COUNTS LINE CANNOT SEE THIS, which is why it needs its own assertion rather than the
# vacuity assertion at the end: `n_bean_files` and `n_beans` both derive from this function's
# output, so a dropped member shrinks the count and the thing it is compared against together
# and the pair reads a clean `112 / 112`. A vacuity assertion over two figures with one source
# is not one.
#
# `-L` IS THE WIDENING THAT NARROWS: an unstattable name is emitted and then fails LOUDLY at
# whatever tries to read it, which is the behaviour `ls` had. `documentation/`'s list at the
# `ls` below never lost it, and that asymmetry is why the regression was invisible here.
glob_lines() { # glob_lines <path…> — the ones that name something, one per line
  local p
  for p in "$@"; do
    if [ -e "$p" ] || [ -L "$p" ]; then printf '%s\n' "$p"; fi
  done
  return 0
}

# Every analyser in this file runs through this wrapper, which shadows the name rather
# than guarding each call site. `set -u` is fail-closed only in the top-level shell: an
# analyser that dies writes nothing, the loop that reads it finds nothing, no `fail`
# fires, and this script printed its `OK` line at exit 0 through the failure. Destroying
# check 12's cycle analyser produced byte-identical stdout to the clean run (bean:0118).
#
# Shadowing, not a per-site `rc=$?`: most call sites are inside `$( )` or are pipeline
# elements, where there is no statement after the analyser to read `$?` at. Redirected to
# stderr because a call site inside `$( )` has its stdout captured, and `fail`'s line would
# otherwise become part of the value the caller parses. Dropping the `>&2` and forcing the
# branch turns one run's stdout into 908 lines, 907 of them FAIL, as check 2 reads the
# guard's own words back as front-matter keys (bean:0123). The record that changes the exit
# status is the append to fails.txt, which is a real file and so survives the subshell.
#
# THE COST of that redirect: this is the only `fail` in the file that does not reach stdout,
# so `./gradlew docsLint | tee log` leaves `docs-lint: 1 failure(s).` in the log with no
# reason beside it. Gradle and CI capture both streams, so the runner's log is complete.
# Replaying the guard's records to stdout after the count line would fix it and keep `$( )`
# safe; it is not done here because an unasserted change to what the gate prints is the
# shape this whole change exists to close (doc:00-constitution#observed-failing). bean:0123.
#
# `command awk` below is the one deliberate bypass. tools/docs-lint-gate-test.sh asserts it
# is still the only one — but by enumerating spellings, which fails open on the nine that
# comment names, so that assertion bounds nothing (doc:00-constitution#observed-failing).
awk_wrap_arg=""
awk() {
  command awk "$@"
  awk_wrap_rc=$?
  if [ "$awk_wrap_rc" -ne 0 ]; then
    for awk_wrap_arg in "$@"; do :; done
    fail - "an analyser exited $awk_wrap_rc and examined nothing; its last argument was '$awk_wrap_arg'" >&2
  fi
  return "$awk_wrap_rc"
}

FM_FILES="$(ls documentation/*.md documentation/adr/*.md)"
REF_FILES="$FM_FILES $(glob_lines .beans/*.md) AGENTS.md CLAUDE.md .github/pull_request_template.md"
REQUIRED_KEYS="id title status superseded_by read_when provides depends_on"

# Beans live under .beans/<prefix><id>--<slug>.md (the hmans/beans on-disk convention);
# .beans.yml's beans.prefix is the one source of that prefix, read here rather than
# duplicated as a second hard-coded copy (doc:05-authoring-for-agents#one-fact-one-place).
#
# NOT `absent_ok`, and this is the one grep in the file that is deliberately left armed. A
# `.beans.yml` with no `prefix:` is not a legal tree — every bean path checks 6, 12 and 13
# build comes from this value — and unlike `id_length:` below, no check reports its absence.
# The trap's record is the only signal there is, so the site keeps it (bean:0124).
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
    n="$(keys "$f" | absent_ok grep -cx "$k")"
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
  absent_ok grep -oE '^#+ .*<a id="[a-z0-9-]+">' "$f" |
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
    absent_ok grep -oE "$REF_RE" |
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
      ci) n="$(ci_jobs | absent_ok grep -cx "$ident")" ;;
      archunit) n="$(absent_ok grep -rhoE "(val|fun) $ident\b" architecture-tests |
        absent_ok grep -c .)" ;;
      detekt) n="$(absent_ok grep -cE "^[[:space:]]+$ident:" config/detekt/detekt.yml)" ;;
      *) n=0 ;;
    esac
    [ "$n" = "1" ] || fail 6 "$src: '$ref' resolves to $n targets, expected exactly 1"
    continue
  fi
  case "$kind" in
    doc) target="$(glob_lines documentation/"$rest"*.md)" ;;
    adr) target="$(glob_lines documentation/adr/"$rest"*.md)" ;;
    bean) target="$(glob_lines .beans/"${BEAN_PREFIX}${rest}"*.md)" ;;
    *) target="" ;;
  esac
  n="$(printf '%s' "$target" | absent_ok grep -c .)"
  if [ "$n" != "1" ]; then
    fail 6 "$src: '$ref' resolves to $n files, expected exactly 1"
    continue
  fi
  [ -n "$anchor" ] || continue
  tid="$(field "$target" S id)"
  found="$(awk -F'\t' -v a="$tid#$anchor" -v f="$target" '$1 == a && $2 == f' "$TMP/provides.tsv" |
    absent_ok grep -c .)"
  [ "$found" = "1" ] || fail 6 "$src: '$ref' — $target does not provide '#$anchor'"
done < "$TMP/refs.uniq"

# ----------------------------------------------------------------- check 8 ---
# Both bounds live in doc:README#changing-this-package; this reads them there.
max_lines="$(absent_ok grep -oE 'max_lines: [0-9]+' documentation/README.md |
  head -1 | absent_ok grep -oE '[0-9]+')"
min_lines="$(absent_ok grep -oE 'min_lines: (none|[0-9]+)' documentation/README.md |
  head -1 | sed 's/min_lines: //')"
if [ -z "$max_lines" ] || [ -z "$min_lines" ]; then
  fail 8 "documentation/README.md states no 'max_lines:'/'min_lines:' budget for docs-lint to read"
else
  for f in documentation/*.md; do
    n="$(absent_ok grep -c '' "$f")"
    [ "$n" -le "$max_lines" ] || fail 8 "$f: $n lines, over the $max_lines ceiling"
    if [ "$min_lines" != none ] && [ "$n" -lt "$min_lines" ]; then
      fail 8 "$f: $n lines, under the $min_lines floor"
    fi
  done
fi
agents_lines="$(absent_ok grep -c '' AGENTS.md)"
[ "$agents_lines" -le 120 ] || fail 8 "AGENTS.md: $agents_lines lines, over the 120 ceiling"

# ----------------------------------------------------------------- check 9 ---
absent_ok grep -nE '^\|.*derived' AGENTS.md > "$TMP/derived.txt"
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
# `absent_ok` because finding no bare path is the whole point of this check, and because
# whether the ERR trap SEES that is the interpreter's business: under bash 3.2 a pipeline
# whose last element is a compound command does not reach the trap, and under bash 5 it does.
# Found by the CI runner, on a run that had been green on macOS (bean:0124).
absent_ok grep -noE '\bbeans/[0-9]' documentation/*.md AGENTS.md CLAUDE.md 2>/dev/null |
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
    old_n="$(git show "$BASE:$f" | absent_ok grep -c '')"
    if ! git show "$BASE:$f" | diff -q - <(head -n "$old_n" "$f") >/dev/null 2>&1; then
      fail 11 "$f: completed bean edited in place; it may only gain '## Amendments' entries (adr:0005#amendments)"
      continue
    fi

    # Anything appended must belong to an Amendments section.
    if ! git show "$BASE:$f" | grep -q '^## Amendments'; then
      first_new="$(tail -n +"$((old_n + 1))" "$f" | absent_ok grep -m1 '[^[:space:]]')"
      if [ -n "$first_new" ] && [ "$first_new" != "## Amendments" ]; then
        fail 11 "$f: appended '$first_new'; a completed bean may only gain a '## Amendments' section (adr:0005#amendments)"
        continue
      fi
    fi

    # Every amendment states when, by whom, what was claimed, what was found and
    # the evidence. An amendment that only asserts a correction is the thing
    # doc:00-constitution#evidence-rule forbids everywhere else.
    if grep -q '^## Amendments' "$f"; then
      sed -n '/^## Amendments/,$p' "$f" | absent_ok grep '^### ' > "$TMP/amend-heads.txt"
      while IFS= read -r h; do
        [ -n "$h" ] || continue
        case "$h" in
          '### '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]' · bean:'[0-9][0-9][0-9][0-9]) ;;
          *) fail 11 "$f: amendment heading '$h' is not '### YYYY-MM-DD · bean:NNNN'" ;;
        esac
      done < "$TMP/amend-heads.txt"
      n_amend="$(absent_ok grep -c . "$TMP/amend-heads.txt")"
      # AN `## Amendments` SECTION WITH NO AMENDMENT IN IT IS THE VACUOUS PASS OF THIS CHECK,
      # and it needs a `fail` of its own rather than the trap. The `absent_ok grep '^### '`
      # above finds nothing, `n_amend` and every `n_k` become 0, `[ 0 != 0 ]` is false, and the
      # loop below reports nothing — the gate printed `docs-lint: OK` at exit 0 for a completed
      # bean carrying `## Amendments` and bare prose. Arming that `grep` would be the WRONG fix:
      # "no `### ` heading" is a legal answer about the input and the site's opt-out is correctly
      # derived; what was missing is a check that reads the answer. Measured at this branch's
      # head under /opt/homebrew/bin/bash 5.3.9, with the positive control that the same bean
      # carrying `### not-a-valid-heading` fires four `fail 11` records (bean:0124).
      [ "$n_amend" -gt 0 ] || fail 11 "$f: has an '## Amendments' section carrying no '### ' amendment heading, so every per-amendment check below it compares 0 against 0 and passes vacuously (adr:0005#amendments)"
      for k in Claimed Found Evidence; do
        n_k="$(sed -n '/^## Amendments/,$p' "$f" | absent_ok grep -c "\*\*$k:\*\*")"
        if [ "$n_amend" != "$n_k" ]; then
          fail 11 "$f: $n_amend amendment(s) but $n_k '**$k:**' line(s) (adr:0005#amendments)"
        fi
      done
    fi
  done < "$TMP/changed-beans.txt"
fi

# ---------------------------------------------------------------- check 12 ---
# The bean dependency graph AGENTS.md step 1 selects work from. Check 6 resolves
# typed `bean:NNNN` references in PROSE only; nothing read `blocked_by`/`parent`
# front-matter at all, so a backlog could deadlock with every file individually
# well-formed and docs-lint green (bean:0035).
#
# Absent scalars are emitted as `-`, never as an empty field: a tab is IFS
# whitespace, so `read` collapses runs of them and one empty middle field would
# shift every field after it.
: > "$TMP/beans.tsv"
for f in $(glob_lines .beans/*.md); do
  bid="${f#.beans/}"
  bid="${bid%%--*}"
  awk -v file="$f" -v id="$bid" '
    NR == 1 { if ($0 != "---") { exit } ; fm = 1; next }
    fm && $0 == "---" { exit }
    fm && /^[a-z_]+:/ {
      k = $0; sub(/:.*$/, "", k)
      v = $0; sub(/^[a-z_]+:[ \t]*/, "", v)
      g[k] = v
    }
    END {
      if (!fm) { exit }
      split("status type priority order parent blocked_by", want, " ")
      printf "%s\t%s", id, file
      for (i = 1; i <= 6; i++) {
        printf "\t%s", (want[i] in g && g[want[i]] != "") ? g[want[i]] : "-"
      }
      printf "\n"
    }
  ' "$f" >> "$TMP/beans.tsv"
done

: > "$TMP/bean-edges.tsv"
: > "$TMP/bean-ready.tsv"
while IFS="$TAB" read -r id file status type priority order parent blocked; do
  [ -n "$id" ] || continue

  if [ "$parent" != "-" ]; then
    n="$(glob_lines .beans/"$parent"--*.md | absent_ok grep -c .)"
    [ "$n" = "1" ] || fail 12 "$file: parent '$parent' resolves to $n bean files, expected exactly 1"
  fi

  deps=""
  case "$blocked" in
    "-") ;;
    "["*"]") deps="$(printf '%s' "$blocked" | tr -d '[]' | tr ',' ' ')" ;;
    *) fail 12 "$file: blocked_by '$blocked' is not a flow list" ;;
  esac

  # A bean is READY when every edge it carries points at a completed bean — the
  # condition AGENTS.md step 1 selects on. An unresolvable edge is not satisfied.
  ready=1
  for dep in $deps; do
    dn="$(glob_lines .beans/"$dep"--*.md | absent_ok grep -c .)"
    if [ "$dn" != "1" ]; then
      fail 12 "$file: blocked_by '$dep' resolves to $dn bean files, expected exactly 1"
      ready=0
      continue
    fi
    printf '%s\t%s\n' "$id" "$dep" >> "$TMP/bean-edges.tsv"
    dtype="$(awk -F'\t' -v d="$dep" '$1 == d { print $4 }' "$TMP/beans.tsv")"
    dstatus="$(awk -F'\t' -v d="$dep" '$1 == d { print $3 }' "$TMP/beans.tsv")"
    if [ "$dtype" = "epic" ]; then
      fail 12 "$file: blocked_by '$dep' is a 'type: epic' bean; step 1 never selects an epic, so the edge never clears"
    fi
    [ "$dstatus" = "completed" ] || ready=0
  done

  if [ "$status" = "todo" ] && [ "$type" != "epic" ] && [ "$ready" = "1" ]; then
    printf '%s\t%s\t%s\t%s\n' "$priority" "$order" "$id" "$file" >> "$TMP/bean-ready.tsv"
  fi
done < "$TMP/beans.tsv"

# Acyclicity by edge-removal: an edge whose target has no outgoing edge left is
# an edge onto a bean that can be completed, so it can clear. Repeat to a fixed
# point; whatever survives is reachable only from itself.
sort -u "$TMP/bean-edges.tsv" > "$TMP/bean-edges.uniq"
cycle="$(awk -F'\t' '
  { from[NR] = $1; to[NR] = $2; n = NR }
  END {
    removed = 1
    while (removed) {
      removed = 0
      split("", live)
      for (i = 1; i <= n; i++) { if (!gone[i]) { live[from[i]] = 1 } }
      for (i = 1; i <= n; i++) {
        if (!gone[i] && !(to[i] in live)) { gone[i] = 1; removed = 1 }
      }
    }
    out = ""
    for (i = 1; i <= n; i++) {
      if (!gone[i]) { out = out (out == "" ? "" : ", ") from[i] " -> " to[i] }
    }
    print out
  }
' "$TMP/bean-edges.uniq")"
[ -z "$cycle" ] || fail 12 "blocked_by graph has a cycle: $cycle"

# `order` breaks step 1's tie, so a value shared by two beans that reach the
# tiebreak together makes the choice arbitrary. Beans that carry no `order` are
# not in this check: absence is a defined position (after every bean that has one).
awk -F'\t' '$2 != "-" { print $1 "\t" $2 }' "$TMP/bean-ready.tsv" | sort | uniq -d > "$TMP/order-dupes.tsv"
while IFS="$TAB" read -r pri ord; do
  [ -n "$ord" ] || continue
  ids="$(awk -F'\t' -v p="$pri" -v o="$ord" '$1 == p && $2 == o { printf "%s ", $3 }' "$TMP/bean-ready.tsv")"
  fail 12 "priority '$pri' order '$ord' is shared by selectable beans: ${ids% }"
done < "$TMP/order-dupes.tsv"

n_ready="$(absent_ok grep -c . "$TMP/bean-ready.tsv")"
[ "$n_ready" -gt 0 ] || fail 12 "no bean is selectable: every non-epic 'status: todo' bean has an unsatisfied blocked_by edge, so AGENTS.md step 1 returns nothing"

# ---------------------------------------------------------------- check 13 ---
# Bean id uniqueness. `.beans/` is the allocator: an agent reads it and takes the
# next free number, and nothing serialises two readers. Two agents in parallel
# worktrees both allocated modus-0048; docs-lint was green on both branches,
# because within either tree the id IS unique, and the collision surfaced only as
# a merge conflict (bean:0051).
#
# Three conditions, in widening scope:
#   13a  within the tree  — an id names exactly one file
#   13b  within the file  — the filename's id and the `# <id>` marker agree
#   13c  across branches  — an id this branch introduces is not already on origin/main
#
# 13c is the condition that catches the real defect; 13a and 13b are the local
# invariants it assumes. Neither was covered before: check 6 rejects a duplicated
# id only by accident, when some prose happens to reference it, so an unreferenced
# duplicate passes; and nothing read the marker at all.

# The id width `.beans.yml` declares, read there rather than hard-coded a second
# time (doc:05-authoring-for-agents#one-fact-one-place). Check 6's reference regex
# resolves `bean:` at exactly this width, so a bean of any other width is
# unreferenceable and 13b is what says so.
BEAN_ID_LEN="$(absent_ok grep -E '^ *id_length:' .beans.yml |
  head -1 | sed -E 's/[^0-9]*([0-9]+).*/\1/')"
[ -n "$BEAN_ID_LEN" ] || fail 13 ".beans.yml states no 'id_length:' for docs-lint to read"

# 13a — every bean id appears in exactly one file. Reuses check 12's parse; the
# id column is the filename's, which is the only id upstream ever reads
# (`ParseFilename`/`Core.Load`; `Parse` ignores the marker — bean:0008).
awk -F'\t' '{ print $1 }' "$TMP/beans.tsv" | sort | uniq -d > "$TMP/bean-id-dupes.txt"
while IFS= read -r dup; do
  [ -n "$dup" ] || continue
  files="$(awk -F'\t' -v d="$dup" '$1 == d { printf "%s ", $2 }' "$TMP/beans.tsv")"
  fail 13 "bean id '$dup' names more than one file: ${files% }"
done < "$TMP/bean-id-dupes.txt"

# 13b — filename and marker agree. Upstream never reads the marker back, so a
# rename that updates the filename and not the marker, or the reverse, is silent
# everywhere — and was hit while fixing this very collision. Iterates the
# directory rather than the parse, so a bean file that produced no front-matter
# row is still seen.
n_bean_files=0
for f in $(glob_lines .beans/*.md); do
  n_bean_files=$((n_bean_files + 1))
  base="$(basename "$f" .md)"
  if ! printf '%s' "$base" | grep -qE "^${BEAN_PREFIX}[0-9]{${BEAN_ID_LEN}}--.+$"; then
    fail 13 "$f: filename is not '${BEAN_PREFIX}<${BEAN_ID_LEN} digits>--slug.md' (.beans.yml)"
    continue
  fi
  bid="${base%%--*}"
  # Only comment lines that are shaped like an id marker; a bean's front-matter
  # may carry other `#` comments, and modus-0047 does.
  marker="$(awk -v p="$BEAN_PREFIX" '
    NR == 1 { if ($0 != "---") exit; next }
    $0 == "---" { exit }
    index($0, "# " p) == 1 { print }
  ' "$f")"
  n_marker="$(printf '%s' "$marker" | absent_ok grep -c .)"
  if [ "$n_marker" != "1" ]; then
    fail 13 "$f: front-matter carries $n_marker '# ${BEAN_PREFIX}…' id markers, expected exactly 1"
  elif [ "$marker" != "# $bid" ]; then
    fail 13 "$f: front-matter marker '$marker' does not match the filename id '$bid'"
  fi
done

# 13c — an id this branch INTRODUCES must not already exist on origin/main.
# "Introduced" is a property of a diff, so the classification is check 11's: the
# merge base says which ids this branch is adding, and origin/main says which ids
# a sibling branch has already merged. An id absent from the base and present on
# origin/main was allocated twice — the case no within-tree check can see, because
# on each branch the id genuinely is unique.
#
# No base means no diff to judge, exactly as in check 11; the counts on the OK
# line report `-` so an inert run is visible rather than silently green.
awk -F'\t' '{ print $1 }' "$TMP/beans.tsv" | sort -u > "$TMP/bean-ids-tree.txt"
n_bean_ids="$(absent_ok grep -c . "$TMP/bean-ids-tree.txt")"
n_introduced="-"
n_main_ids="-"
if [ -n "$BASE" ]; then
  bean_ids_of() {
    git ls-tree -r --name-only "$1" -- .beans 2>/dev/null |
      sed -e 's|^\.beans/||' -e 's|\.md$||' -e 's|--.*||' | sort -u
  }
  bean_ids_of "$BASE" > "$TMP/bean-ids-base.txt"
  bean_ids_of origin/main > "$TMP/bean-ids-main.txt"
  comm -23 "$TMP/bean-ids-tree.txt" "$TMP/bean-ids-base.txt" > "$TMP/bean-ids-new.txt"
  n_introduced="$(absent_ok grep -c . "$TMP/bean-ids-new.txt")"
  n_main_ids="$(absent_ok grep -c . "$TMP/bean-ids-main.txt")"
  while IFS= read -r nid; do
    [ -n "$nid" ] || continue
    # THE ONE BLANKET `|| continue` LEFT, and it is named rather than narrowed. Here the STATUS
    # is the answer — this id is on origin/main, or it is not — so `absent_ok`, which is for
    # sites that read a VALUE, does not fit: it returns 0 for both 0 and 1 and would erase the
    # answer. `|| continue` therefore tolerates 2 along with 1, which is the widening this
    # file's opt-out exists to refuse. It is UNREACHABLE today: `bean_ids_of origin/main` writes
    # $TMP/bean-ids-main.txt unconditionally a few lines above, inside the same `if`, so the
    # only way to reach a `grep` here that could not look is for the file to vanish mid-run —
    # and the `[ ! -f "$TMP/fails.txt" ]` guard at the end catches that class by its cause.
    # Left as a note and not a fix because a fix nothing can be observed to protect is not
    # enforcement (doc:00-constitution#observed-failing): the loop body runs only for ids this
    # branch introduced, which is empty on a clean tree, so no plant in
    # tools/docs-lint-gate-test.sh reaches it. Whoever makes it reachable owes it a narrowing.
    grep -qx "$nid" "$TMP/bean-ids-main.txt" || continue
    here="$(awk -F'\t' -v d="$nid" '$1 == d { printf "%s ", $2 }' "$TMP/beans.tsv")"
    there="$(git ls-tree -r --name-only origin/main -- .beans |
      absent_ok grep "^\.beans/$nid--" | tr '\n' ' ')"
    fail 13 "bean id '$nid' is introduced by this branch (${here% }) but already exists on origin/main (${there% }); a sibling branch allocated it first — take the next id free on origin/main, not the next free in this worktree (bean:0051)"
  done < "$TMP/bean-ids-new.txt"
fi

# Non-vacuity for checks 12 and 13. Both read `.beans/`; a run that parsed no
# beans, or that parsed fewer files than are on disk, examined less than it
# claims and must say so rather than exit 0 (doc:00-constitution#observed-failing).
n_beans="$(absent_ok grep -c . "$TMP/beans.tsv")"
[ "$n_bean_files" -gt 0 ] || fail 13 ".beans/ holds no bean files; checks 12 and 13 examined nothing"
if [ "$n_beans" != "$n_bean_files" ]; then
  fail 13 "$n_bean_files bean file(s) on disk but $n_beans parsed; a bean with no front-matter block is invisible to checks 12 and 13"
fi

# ---------------------------------------------------------------- check 14 ---
# A bean may not close without evidence.
#
# adr:0005-evidence-lives-in-the-work-item#evidence-home makes the bean the evidence
# record: every success criterion carries the command, the expectation and the verbatim
# observed output, beside the criterion it satisfies. Nothing read that. Check 11 guards
# a bean once it IS completed and check 13c guards its id; neither looks at whether the
# criteria are answered on the way in, and modus-0045 was found closable with four
# criteria and no evidence at all.
#
# Scope is check 11's diff shape, one status earlier: a bean CLOSES in this change when
# it is `completed` in the working tree and was not `completed` on the merge base. That
# covers in-progress -> completed (doc:00-constitution#bean-lifecycle), todo ->
# completed, and a bean created already completed. It never re-examines a bean that was
# already completed on the base, which is what keeps it off the grandfathered corpus —
# modus-0001 carries no evidence section at all and modus-0028's is empty, and check 11
# has frozen both.
#
# Two shapes are accepted because the corpus uses two:
#   A  `## Success criteria and evidence` — one table, one row per criterion, carrying
#      an `evidence` column
#   B  `## Success criteria` (or `## Restated criteria`) plus a separate `## Evidence`
#      section, table-shaped or fenced-transcript-shaped
# An `evidence kind` column is a PLAN — what will be produced — and is deliberately not
# an evidence column. A table carrying only that column records no observation.
#
# Three conditions:
#   14a  an evidence home exists and is not empty
#   14b  every criterion the bean NUMBERS is answered, and no evidence cell is blank
#   14c  no evidence cell is nothing but evidence-kind names
#
# 14c is the hollow-row case: `test-run` written where `./gradlew qualityCheck` running
# green belongs. The kinds are doc:50-memory-and-evidence#evidence-kinds' closed set,
# named here because that is what the cell is compared against; the anchor owns them.
KINDS=" command test-run diff citation fetch observation "

n_closing="-"
n_c14_crit="-"
n_c14_unnum="-"
if [ -n "$BASE" ]; then
  n_closing=0
  n_c14_crit=0
  n_c14_unnum=0
  { git diff --name-only "$BASE" -- .beans 2>/dev/null || :
    # An added-but-unstaged bean is absent from `git diff <commit>`, so a locally
    # planted closure would be invisible here and caught only in CI.
    git ls-files --others --exclude-standard -- .beans 2>/dev/null || :
  } | sort -u > "$TMP/closing-candidates.txt"

  while IFS= read -r f; do
    [ -n "$f" ] || continue
    [ -f "$f" ] || continue
    now="$(sed -n 's/^status:[[:space:]]*//p' "$f" | head -1)"
    [ "$now" = "completed" ] || continue
    was=""
    if git cat-file -e "$BASE:$f" 2>/dev/null; then
      was="$(git show "$BASE:$f" 2>/dev/null | sed -n 's/^status:[[:space:]]*//p' | head -1)"
    fi
    [ "$was" = "completed" ] && continue
    n_closing=$((n_closing + 1))

    # The analyser is two files on disk now, so it can go MISSING in a way an inline
    # program could not: awk exits 2, writes nothing, the read loop below finds nothing,
    # no `fail` fires, and the run reports `0 criteria checked` beside `1 closing
    # transitions` at exit 0. The counts line calls itself the vacuity assertion; these
    # two conditions are what make it assert rather than describe.
    #
    # This is the one site where the shadow guard and a per-site check both fire, so one
    # dead analyser here is `docs-lint: 2 failure(s).` — observed, in bean:0123. The
    # per-site `fail` stays: the guard's record names the bean it was reading and cannot
    # name the two files that must be present and parse, and this one cannot name the bean.
    # `n_fail` counts RECORDS, not defects, here as everywhere else in this file.
    awk -v KINDS="$KINDS" \
      -f "$ROOT/tools/lib/docs-lint-fence.awk" \
      -f "$ROOT/tools/lib/docs-lint-c14.awk" \
      "$f" > "$TMP/c14.txt"
    awk_rc=$?
    if [ "$awk_rc" -ne 0 ]; then
      fail 14 "$f: the check 14 analyser exited $awk_rc and examined nothing; tools/lib/docs-lint-fence.awk and tools/lib/docs-lint-c14.awk must both be present and parse"
      continue
    fi
    if ! grep -q "^STATS" "$TMP/c14.txt"; then
      fail 14 "$f: the check 14 analyser produced no STATS line; it examined nothing, and a run that examines nothing may not report OK (doc:00-constitution#observed-failing)"
      continue
    fi

    while IFS="$TAB" read -r code a b; do
      case "$code" in
        UNTERMFENCE)
          fail 14 "$f: a fenced block opened at line $a is never closed, so every line after it is read as code and no absence of evidence below it can be observed; close the fence, or — when the marker is part of a transcript's verbatim output — wrap that transcript in a longer fence so the quoted marker is content (doc:05-authoring-for-agents#checks)" ;;
        NOEV)
          fail 14 "$f: closes with no evidence section; a criterion's command, expectation and verbatim observed output live in the bean (adr:0005-evidence-lives-in-the-work-item#evidence-home)" ;;
        EMPTYEV)
          fail 14 "$f: closes with an evidence section carrying no entry — no table row, no sub-heading, no transcript (adr:0005-evidence-lives-in-the-work-item#evidence-home)" ;;
        EMPTYCELL)
          fail 14 "$f: criterion $a closes with an empty evidence cell (adr:0005-evidence-lives-in-the-work-item#evidence-home)" ;;
        HOLLOW)
          fail 14 "$f: criterion $a records '$b' — an evidence KIND, not evidence; the cell must carry the command, the expectation and the verbatim observed output (adr:0005-evidence-lives-in-the-work-item#evidence-home, doc:50-memory-and-evidence#evidence-kinds)" ;;
        NOEVCOL)
          fail 14 "$f: the table under '$a' numbers criteria in an evidence section but carries no evidence column; 'evidence kind' states what will be produced, not what was observed (adr:0005-evidence-lives-in-the-work-item#evidence-home)" ;;
        UNANSWERED)
          fail 14 "$f: criterion $a is not answered in the evidence; no evidence row bears its number and nothing cites it (adr:0005-evidence-lives-in-the-work-item#evidence-home)" ;;
        STATS)
          n_c14_crit=$((n_c14_crit + a)); n_c14_unnum=$((n_c14_unnum + b)) ;;
      esac
    done < "$TMP/c14.txt"
  done < "$TMP/closing-candidates.txt"
fi

# -------------------------------------------------------------------- done ---
# THE RECORD FILE HAS TO STILL BE HERE. `set -u` firing inside a TOP-LEVEL PIPELINE ELEMENT
# exits that subshell, and that subshell runs the EXIT trap it inherited — which deletes
# $TMP out from under the rest of the run. Every record after that is lost, the trap's own
# included, and the gate then printed `docs-lint: OK` at exit 0 with its twelve counts EMPTY
# and 287 `FAIL check` lines above it on the same stdout. Measured in bean:0124, at the plant
# point bean:0118's table cannot reach. `$BASH_SUBSHELL` cannot be used to stop the deletion
# instead: it reads 0 inside that subshell's EXIT trap under /bin/bash 3.2.57, also measured.
if [ ! -f "$TMP/fails.txt" ]; then
  printf 'docs-lint: the failure record %s vanished mid-run; nothing above this line can be trusted.\n' "$TMP/fails.txt"
  exit 2
fi

# EVERY COUNT IS TAKEN BEFORE THE EXIT DECISION READS `n_fail`. Four of the twelve used to be
# `$( )` arguments of the `printf` below, which runs only on the OK branch — AFTER the `if`
# has already decided the exit status. A "could not look" at one of those four was recorded by
# `absent_ok` into $TMP/fails.txt, and NOTHING READS THAT FILE AGAIN: the run printed
# `docs-lint: OK — 19 documents,  anchors, 1738 references, …` at exit 0 with the anchor count
# blank and the record on stderr, uncounted. Their statuses cannot reach the trap either,
# being command substitutions inside a `printf` argument list. Measured at this branch's head
# under /opt/homebrew/bin/bash 5.3.9 by planting `rm -f "$TMP/provides.tsv"` immediately above
# the `n_fail` read; the OK line above is that run's stdout, verbatim.
#
# `- introduced` versus `0 introduced` is the same failure one character over: check 11's
# inert CI runs differed from real ones by exactly that (doc:00-constitution#observed-failing).
# A blank between two commas is what a count that could not be taken looks like, and the
# vacuity assertion cannot assert over a figure the branch that prints it computes.
n_documents="$(printf '%s\n' $FM_FILES | absent_ok grep -c .)"
n_anchors="$(absent_ok grep -c . "$TMP/provides.tsv")"
n_references="$(absent_ok grep -c . "$TMP/refs.uniq")"
n_edges="$(absent_ok grep -c . "$TMP/bean-edges.uniq")"

# AND THE FAILURE PATH HAS TO STILL WORK — asked by FIRING IT, not by reading it. `trap - ERR`
# inserted anywhere between the `trap … ERR` near the top of this file and here silences every
# record below it and the run reports OK at exit 0; against that one-line edit
# tools/docs-lint-gate-test.sh once scored a clean sheet, because every plant it carried sat
# above the disarm (bean:0123's "22 sites or 1 site", moved from call sites to line ranges).
#
# THE READING THIS REPLACES WAS A ONE-TOKEN ALLOWLIST. `case "$(trap -p ERR)" in *docs_lint_err*)`
# asks the shell what handler string it holds and then matches a spelling in it, and its comment
# claimed there was "no list of spellings, which would fail open" — a one-token list is a list,
# and §9.1's MUST binds it. Two ONE-EDIT escapes walked past it, both confirmed under
# /bin/bash 3.2.57 and /opt/homebrew/bin/bash 5.3.9:
#   trap ': docs_lint_err' ERR   — a re-trap whose TEXT merely contains the token; the `case`
#                                  matches and the handler that runs records nothing.
#   docs_lint_err() { :; }       — redefined anywhere below the arming line. `trap -p ERR` is
#                                  byte-identical, the `case` matches, every record is dropped.
#                                  This is the more plausible accident of the two.
# Firing the path answers all three shapes and every shape nobody has thought of, because it
# asks the question the records depend on — does a failing command at the top level of this
# file end up as a LINE OF $TMP/fails.txt — rather than a proxy for it. A `false` that records
# is the whole contract; `{ … } 2>/dev/null` keeps the probe's own record off stderr, and that
# it still reaches the file through the group's redirection is measured under both
# interpreters (bean:0124). The marker is filtered out of `n_fail` below rather than deleted
# from the file, so a failure to remove it cannot lose a real record.
#
# WHAT IT STILL DOES NOT CATCH, stated rather than implied: a disarm that RE-ARMS before this
# line, which needs two edits. Two shapes that look like escapes and are not, measured under
# both interpreters: a disarm inside a `( … )` subshell leaves the parent's trap intact, and
# `trap - ERR` inside a FUNCTION does not disarm the caller — bash restores the function-local
# ERR trap on return. Neither is an escape and neither needs guarding against.
{ false __docs_lint_armed_probe__; } 2>/dev/null
n_armed="$(absent_ok grep -cF '__docs_lint_armed_probe__' "$TMP/fails.txt")"
[ "$n_armed" = 1 ] || fail - "the failure path was not working at the end of the run: a top-level 'false' produced $n_armed record(s) in the failure file instead of exactly 1. The ERR trap is armed once, near the top of this file; nothing below may disarm it, re-trap it, or redefine docs_lint_err. Every runtime failure between the break and here went unrecorded"

# `-v`, so the probe's own record is not a failure of the corpus. It is the only line that can
# carry that marker; if a document ever does, this counts 2 above and the gate goes red rather
# than quiet, which is the direction a token of this shape should fail in.
n_fail="$(absent_ok grep -cvF '__docs_lint_armed_probe__' "$TMP/fails.txt")"
# `[ "$n_fail" -gt 0 ]` on an EMPTY `n_fail` is `[ "" -gt 0 ]`, which is a syntax error the
# trap is exempt from inside an `if` condition — the gate would print OK at exit 0 through it.
# Unreachable: the `[ ! -f ]` guard above returns before here if the file is gone, and every
# other path through `absent_ok grep -c` yields digits. Shape only, named so it stays that way.
if [ "$n_fail" -gt 0 ]; then
  echo "docs-lint: $n_fail failure(s)."
  exit 1
fi
# The counts are the vacuity assertion: a check that silently examined nothing
# reports zero here, where check 11 shipping inert went unnoticed for four plants.
printf 'docs-lint: OK — %s documents, %s anchors, %s references, %s beans, %s graph edges, %s selectable, %s bean ids, %s introduced, %s on origin/main, %s closing transitions, %s criteria checked, %s unnumbered.\n' \
  "$n_documents" \
  "$n_anchors" \
  "$n_references" \
  "$n_beans" \
  "$n_edges" \
  "$n_ready" \
  "$n_bean_ids" \
  "$n_introduced" \
  "$n_main_ids" \
  "$n_closing" \
  "$n_c14_crit" \
  "$n_c14_unnum"
