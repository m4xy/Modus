#!/usr/bin/env bash
# Tests for the check 14 analyser (doc:05-authoring-for-agents#checks), in two halves
# that are deliberately not one half.
#
#   PERCEPTION  which lines tools/lib/docs-lint-fence.awk classifies in-fence and which
#               out. Asserted directly, on the classification, never through a verdict.
#   VERDICT     what tools/lib/docs-lint-c14.awk decides about a bean.
#
# The split is the lesson of bean:0063: every escape from this gate so far entered
# through the PARSE and not through the DECISION, while the decision tests passed
# throughout. A verdict test cannot distinguish "read the file correctly and judged it
# right" from "read it wrongly and judged it wrongly twice".
#
# The third section is adversarial: each case is an attempt to defeat the analyser.
#
# EVERY RESIDUAL NEEDS A VERDICT ASSERTION, NOT ONLY A PERCEPTION ONE. A residual's whole
# claim is "this divergence is acceptable", and acceptability is a claim about the
# OUTCOME. Asserting only the classification documents the divergence without ever asking
# what it costs. When the verdict assertion shows the outcome DOES change, the thing is
# not a residual — it is a defect, and it is labelled DEFECT here and owes a bean.
#
# That rule is written from being caught by it. bean:0063 shipped its two
# highest-blast-radius residuals — a fence inside a block quote, a fence indented into a
# list item — asserted at the perception layer alone, as `OUT OUT OUT OUT OUT`. Both
# turned out to change the verdict: the transcript inside the container was read as prose
# and its pasted `criterion N is not answered` lines answered the criteria they report
# unanswered. One of the two was a REGRESSION against the toggle being replaced. A verdict
# assertion on either would have shown it in this file before review did.
#
# ONE MUTATION PER MECHANISM, NOT PER FILE. A mutation suite proves only that the tests
# can detect THE MUTATION THAT WAS MADE. This analyser is two mechanisms — the classifier
# decides where a fence is, the citation-site requirement decides where a citation counts
# — and a single mutation leaves the second one unexercised while the suite still reports
# green. Both are mutated here, separately:
#
#   classifier only      fence_classify replaced by the pre-bean:0063 toggle, the real
#                        measurement helpers kept    ->  17 passed, 14 failed
#   citation site only   citation_site() bypassed, `s = tolower(line)` restored
#                                                    ->  28 passed,  3 failed
#
# Neither mutation reaches the other's assertions, which is the whole point: the second
# mutation was added after the first was found to say nothing about the citation scanner.
#
# A third mutation is the complement of the second, and it is why the negative control
# below exists. Narrowing the citation scanner and DELETING it are different faults, and
# the three container assertions pass under both:
#
#   citation scanner deleted   `s = ""`, so nothing is ever cited
#                                                    ->  29 passed,  2 failed
#
# The two that fail are "control: the same citation at top level answers its criterion"
# and the DEFECT pin. Without that control the container assertions would be satisfied by
# a scanner that answers nothing at all.
#
# Fixtures are heredocs beside their assertions rather than a fixture directory: the
# repository had no fixture location for docs-lint, and a fixture whose expected output
# lives in another file is read twice and updated once.
#
# bash 3.2 (macOS): no associative array, no ${var^^}, no mapfile.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 2
FENCE="$ROOT/tools/lib/docs-lint-fence.awk"
C14="$ROOT/tools/lib/docs-lint-c14.awk"
KINDS=" command test-run diff citation fetch observation "

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
FIX="$TMP/fixture.md"

pass=0
fail=0

# The perception probe: one classification per line, then the unterminated report.
cat > "$TMP/map.awk" <<'MAPAWK'
BEGIN { fence_reset() }
{ printf "%s ", fence_classify($0) }
END { printf "| unterminated=%d\n", fence_unterminated() }
MAPAWK

check() { # check <name> <expected> <actual>
  if [ "$2" = "$3" ]; then
    pass=$((pass + 1))
    printf 'ok   %s\n' "$1"
  else
    fail=$((fail + 1))
    printf 'FAIL %s\n       expected: %s\n       actual:   %s\n' "$1" "$2" "$3"
  fi
}

perceives() { # perceives <name> <expected map>
  check "perception: $1" "$2" "$(awk -f "$FENCE" -f "$TMP/map.awk" "$FIX")"
}

decides() { # decides <name> <expected codes, newline separated>
  check "verdict: $1" "$2" "$(awk -v KINDS="$KINDS" -f "$FENCE" -f "$C14" "$FIX")"
}

echo "--- perception: where the analyser believes the fences are"

cat > "$FIX" <<'EOF'
prose
```
code
```
prose
EOF
perceives "a balanced fence" "OUT OPEN IN CLOSE OUT | unterminated=0"

cat > "$FIX" <<'EOF'
prose
````
```
quoted
```
````
prose
EOF
perceives "a three-backtick marker inside a four-backtick fence is content" \
  "OUT OPEN IN IN IN CLOSE OUT | unterminated=0"

cat > "$FIX" <<'EOF'
prose
```
code
```
still prose
```
EOF
perceives "an odd number of markers leaves a block open, and says so" \
  "OUT OPEN IN CLOSE OUT OPEN | unterminated=6"

cat > "$FIX" <<'EOF'
prose
~~~
```
tilde-fenced
~~~
prose
EOF
perceives "a tilde fence is a fence, and a backtick marker inside it is content" \
  "OUT OPEN IN IN CLOSE OUT | unterminated=0"

cat > "$FIX" <<'EOF'
prose
```
tilde inside a backtick fence
~~~
```
prose
EOF
perceives "a tilde marker does not close a backtick fence" \
  "OUT OPEN IN IN CLOSE OUT | unterminated=0"

cat > "$FIX" <<'EOF'
prose
```yaml
code
```
prose
EOF
perceives "an info string opens a fence" "OUT OPEN IN CLOSE OUT | unterminated=0"

cat > "$FIX" <<'EOF'
a line that is one inline code span: ```yaml```
```json```
prose
EOF
perceives "a backtick in the info string is an inline code span, not a fence" \
  "OUT OUT OUT | unterminated=0"

cat > "$FIX" <<'EOF'
prose
    ```
    indented four columns
    ```
prose
EOF
perceives "a marker indented four columns is not a delimiter" \
  "OUT OUT OUT OUT OUT | unterminated=0"

printf 'prose\n\t```\nprose\n' > "$FIX"
perceives "a tab-indented marker is not a delimiter" "OUT OUT OUT | unterminated=0"

cat > "$FIX" <<'EOF'
prose
   ```
three columns is still a fence
   ```
prose
EOF
perceives "three columns of indent still opens and closes" \
  "OUT OPEN IN CLOSE OUT | unterminated=0"

cat > "$FIX" <<'EOF'
| # | criterion | observed |
|---|---|---|
| 1 | ``` in a cell | ok |
| 2 | ```json``` | ok |
EOF
perceives "a marker in a table cell is not line-initial and cannot be a delimiter" \
  "OUT OUT OUT OUT | unterminated=0"

cat > "$FIX" <<'EOF'
```
a closing marker may be longer than the opening one
`````
prose
EOF
perceives "a longer marker closes a shorter fence" "OPEN IN CLOSE OUT | unterminated=0"

cat > "$FIX" <<'EOF'
```
a marker carrying an info string does not close
``` js
```
prose
EOF
perceives "a closing marker may carry nothing but whitespace" \
  "OPEN IN IN CLOSE OUT | unterminated=0"

printf 'prose\r\n```\r\ncode\r\n```\r\nprose\r\n' > "$FIX"
perceives "CRLF line endings still close a fence" \
  "OUT OPEN IN CLOSE OUT | unterminated=0"

echo
echo "--- verdict: what the analyser decides about a bean"

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

| # | criterion | observed |
|---|---|---|
| 1 | one | `docs-lint: OK` exit 0 |
| 2 | two | `git status --porcelain` empty |
EOF
decides "a filled evidence table answers its criteria" "$(printf 'STATS\t2\t0')"

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

```
cmd:      bash tools/docs-lint.sh
observed: docs-lint: OK
```
EOF
decides "a transcript that cites no criterion answers none" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nSTATS\t2\t0')"

# bean:0063's fails-OPEN plant: the transcript quotes one fence marker, so under a
# single toggle the pasted `criterion N is not answered` lines were read as prose and
# answered the criteria they report as unanswered.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

```
cmd:      bash tools/docs-lint.sh
```
FAIL check 14 .beans/modus-0033: criterion 1 is not answered in the evidence
FAIL check 14 .beans/modus-0033: criterion 2 is not answered in the evidence
exit:     1
```
EOF
decides "a quoted fence marker is refused, not laundered into an answer" \
  "$(printf 'UNTERMFENCE\t16\nSTATS\t2\t0')"

# The same evidence, written the way the refusal's message directs.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

````
cmd:      bash tools/docs-lint.sh
```
FAIL check 14 .beans/modus-0033: criterion 1 is not answered in the evidence
FAIL check 14 .beans/modus-0033: criterion 2 is not answered in the evidence
exit:     1
```
````
EOF
decides "quoted correctly, the pasted output stays inside the fence and answers nothing" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nSTATS\t2\t0')"

# bean:0063's fails-CLOSED plant: one stray marker hid a filled table.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

```
| # | criterion | observed |
|---|---|---|
| 1 | one | `docs-lint: OK` exit 0 |
| 2 | two | `git status --porcelain` empty |
EOF
decides "a stray marker above a filled table is named, not reported as missing evidence" \
  "$(printf 'UNTERMFENCE\t10\nSTATS\t2\t0')"

echo
echo "--- adversarial: attempts to defeat the fence tracking"

# A tilde-fenced transcript was invisible to the old tracking, which knew only
# backticks, so every citation inside one answered its criterion.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

~~~
FAIL check 14: criterion 1 is not answered in the evidence
FAIL check 14: criterion 2 is not answered in the evidence
~~~
EOF
decides "a tilde-fenced transcript cannot answer its own criteria" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nSTATS\t2\t0')"

# A fence nested inside a longer fence must not end the outer block early.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

````
outer
```
inner
```
criterion 1 cited inside the outer fence
````
EOF
decides "a nested fence does not release the outer block" \
  "$(printf 'UNANSWERED\t1\nSTATS\t1\t0')"

# --- the container-block escapes, each asserted as a VERDICT ------------------------
#
# A fence is not the only way to paste output into a bean. CommonMark renders a
# `>`-prefixed line as a block quote (§5.1) and a line indented four or more columns as an
# indented code block (§4.4); a line-oriented reader sees prose in both. That divergence
# was shipped as a perception-only residual and it changed the verdict in both cases, one
# of them a regression against the toggle being replaced. The fix is in the CITATION
# SCANNER, not the classifier: the classifier stays strict, and a citation is required to
# stand in unambiguous top-level prose.

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

### The run

1. pasted inside the list item:

    ```
    FAIL check 14: criterion 1 is not answered in the evidence
    FAIL check 14: criterion 2 is not answered in the evidence
    ```
EOF
decides "a fenced transcript indented into a list item cannot answer its criteria" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nSTATS\t2\t0')"

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

### The run

> ```
> FAIL check 14: criterion 1 is not answered in the evidence
> FAIL check 14: criterion 2 is not answered in the evidence
> ```
EOF
decides "a block-quoted transcript cannot answer its criteria" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nSTATS\t2\t0')"

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### The run

        FAIL check 14: criterion 1 is not answered in the evidence
EOF
decides "an indented chunk with no marker at all cannot answer its criterion" \
  "$(printf 'UNANSWERED\t1\nSTATS\t1\t0')"

# The negative control for the pair above: the identical citation at top level DOES
# answer. Without this, the three assertions above would also pass with the citation
# scanner deleted outright.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### The run

criterion 1 is answered by this line, which stands in top-level prose
EOF
decides "control: the same citation at top level answers its criterion" \
  "$(printf 'STATS\t1\t0')"

# A container hides a transcript from the ENTRY count too, which fails closed: a bean
# whose only evidence is quoted or indented has no entry and cannot close.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

> ```
> the whole evidence, quoted
> ```
EOF
decides "a bean whose only evidence is inside a container has no entry" \
  "$(printf 'EMPTYEV\nUNANSWERED\t1\nSTATS\t1\t0')"

# --- residuals: divergences that do NOT change the verdict --------------------------
#
# Perception assertion and verdict assertion, together. The perception line records that
# the analyser does not see the container; the verdict line is what makes it a residual
# rather than a defect.

cat > "$FIX" <<'EOF'
prose
> ```
> criterion 1 is not answered in the evidence
> ```
prose
EOF
perceives "RESIDUAL: a fence inside a block quote is not seen" \
  "OUT OUT OUT OUT OUT | unterminated=0"

cat > "$FIX" <<'EOF'
1. a list item
    ```
    criterion 1 is not answered in the evidence
    ```
prose
EOF
perceives "RESIDUAL: a fence indented into a list item is not seen" \
  "OUT OUT OUT OUT OUT | unterminated=0"

# --- DEFECT: open, and owed a bean --------------------------------------------------
#
# Two quoted markers BALANCE, so the segment between them is top-level prose — to this
# analyser and to every renderer alike, so there is no perception divergence to fix here.
# The verdict assertion is what classifies it: the outcome DOES change, pasted output DOES
# answer the criterion it reports unanswered, and by this file's own rule that makes it a
# defect and not a residual. It is not reachable from fence tracking or from the citation
# site, because the line is genuinely top-level prose; it belongs to the citation matcher
# (bean:0061). The assertion below pins today's behaviour so the day it changes is visible.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

```
cmd: bash tools/docs-lint.sh
```
criterion 1 is not answered in the evidence
```
tail of the transcript
```
EOF
decides "DEFECT (open): an EVEN number of quoted markers still answers the criterion" \
  "$(printf 'STATS\t1\t0')"

cat > "$FIX" <<'EOF'
~~~
a tilde fence closes only on a marker at least as long
~~~~
prose
EOF
perceives "the length rule applies to tilde fences too" \
  "OPEN IN CLOSE OUT | unterminated=0"

cat > "$FIX" <<'EOF'
~~~~
a shorter tilde marker does not close a longer tilde fence
~~~
prose
EOF
perceives "a shorter tilde marker does not close a longer tilde fence" \
  "OPEN IN IN IN | unterminated=1"

echo
echo "docs-lint-test: $pass passed, $fail failed."
[ "$fail" -eq 0 ] || exit 1
