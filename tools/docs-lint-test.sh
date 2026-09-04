#!/usr/bin/env bash
# Tests for the check 14 analyser (doc:05-authoring-for-agents#checks), in two halves
# that are deliberately not one half.
#
#   PERCEPTION  which lines tools/lib/docs-lint-fence.awk classifies in-fence and which
#               out. Asserted directly, on the classification, never through a verdict.
#   VERDICT     what tools/lib/docs-lint-c14.awk decides about a bean.
#
# There are two perception layers, not one, and bean:0093 is why the second is asserted
# separately:
#
#   CITATION SITE  which lines citation_site() will read a `criterion N` citation from.
#                  Asserted on the function's own answer, for the same reason: a bean whose
#                  criterion comes back unanswered cannot say whether the site was refused
#                  or the citation was never written.
#   CITATION TEXT  what citation_text() actually reads from such a line, which since
#                  bean:0121 is a narrower question than the shape: a site inside the wrong
#                  REGION is read as nothing, and the EVIDENCE CELL of a row is cut out of
#                  what is read. Asserted on that function's own answer too, and for a third
#                  reason on top of the two above — a row still answers its own number
#                  through the evidence-row path, so no verdict anywhere can show whether
#                  its cell was masked.
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
# can detect THE MUTATION THAT WAS MADE. Two mechanisms CHANGED here — the classifier
# decides where a fence is, the citation-site requirement decides where a citation counts
# — and a single mutation leaves the second one unexercised while the suite still reports
# green. Both are mutated here, separately:
#
#   classifier only      fence_classify replaced by the pre-bean:0063 toggle: a single
#                        in/out flag flipped by every line matching /^[ \t]*```/, returning
#                        OPEN on the flip to in, CLOSE on the flip to out, IN between and
#                        OUT elsewhere. READ tools/lib/docs-lint-fence.awk's header comment
#                        for that shape — it is the only place the replaced toggle is
#                        written down. The real measurement helpers are kept.
#                                                    ->  59 passed, 12 failed
#   citation site only   citation_site() returns 1 for every line, which is the
#                        pre-bean:0093 rule with its two exclusions also removed
#                                                    ->  51 passed, 20 failed
#
# Neither mutation reaches the other's assertions, which is the whole point: the second
# mutation was added after the first was found to say nothing about the citation scanner.
#
# A third mutation is the complement of the second, and it is why the negative controls below
# exist. Narrowing the citation scanner and DELETING it are different faults, and every
# rejection in this file passes under both:
#
#   citation scanner deleted   citation_text() returns "" for every line, so nothing is
#                              ever cited                ->  54 passed, 17 failed
#
# The seventeen that fail are the only assertions in this file that require something to BE
# answered by the CITATION SCAN. They are NAMED and not counted, because a count is the one
# claim about a set that goes stale without anyone noticing, and the summary below this block
# was wrong for exactly that reason until this bean's review:
#
#   control: an evidence sub-heading naming the criterion answers it
#   control: an evidence row citing a range of criteria answers all of them
#   accepted: a heading that denies its criterion still answers it
#   ACCEPTED: a heading-shaped line inside <pre>, <details> or an HTML comment answers
#   a citing sub-heading outside the evidence region answers nothing
#   citation text: and the region is what refuses them, not their shape
#   accepted: a top-level section devoted to a criterion answers it
#   of two adjacent citing headings, only the second is answered
#   control: prose under a citing heading is content
#   control: a fenced block under a citing heading is content
#   control: a deeper heading under a citing heading is content
#   accepted: an EMPTY fenced block under a citing heading is still content
#   citation text: the evidence cell is cut out of the row, and the rest of it is not
#   citation text: the escape is counted as cell content, not as a cell boundary
#   control: an honest span in a non-evidence column answers, and the row keeps its own number
#   the same span in the FIRST cell costs the row its own criterion
#   control: the identical line under a delimiter row IS a row, and answers
#
# Two of those seventeen are REJECTIONS and not controls, and they are here because each
# carries an answered criterion as its own non-triviality guard: `a citing sub-heading outside
# the evidence region` answers criterion 1 normally, and `of two adjacent citing headings`
# answers the second one. Delete the scanner and the guard stops holding, so the fixture fails
# on the half that was never the point. That is worth knowing about them and it does not make
# them controls.
#
# Before bean:0093 there was one such control — a line of top-level PROSE — and bean:0093
# turned that line into a rejection, because prose is exactly what the narrowed rule refuses.
# Had it not been replaced, this mutation would have scored the same as the real narrowing and
# the suite could not have told the two apart.
#
# THE SAME TRAP ONE LEVEL UP, and bean:0121 is where it was walked into deliberately rather
# than found. A narrowing, its ABSENCE and its DELETION are three states, and a suite that
# scores the last two alike is the blindness that let the original defect through. So the
# absence is measured too, as its own mutation:
#
#   bean:0121 deleted whole    the region clause, the evidence-cell mask and the pending
#                              buffer all removed — bean:0121's three constraints, back to
#                              the rule as it stood at 3b02871. rowcells() is LEFT IN PLACE:
#                              it is not one of the three, and with the mask gone it decides
#                              only which field `evcol` addresses
#                                                      ->  58 passed, 13 failed
#
# Read the two figures together. Deleting the SCANNER kills seventeen and deleting the
# NARROWING kills thirteen, and the shape of each set is the point rather than its size:
# the first is dominated by CONTROLS — things that must still be answered — and the second by
# REJECTIONS, things that must no longer be. Neither set is pure, and the two impurities are
# named above and below rather than rounded away.
#
# The two sets MEET in five assertions, not in the probes alone:
#
#   citation text: and the region is what refuses them, not their shape
#   citation text: the evidence cell is cut out of the row, and the rest of it is not
#   citation text: the escape is counted as cell content, not as a cell boundary
#   a citing sub-heading outside the evidence region answers nothing
#   of two adjacent citing headings, only the second is answered
#
# The first three are the citation-TEXT probes, which assert the mechanism directly and so are
# killed by anything that touches it. The last two are the rejections named above: each is a
# rejection under the narrowing and each keeps an answered criterion beside it as its
# non-triviality guard, so both mutations reach them, by opposite halves. An earlier version
# of this paragraph said the sets meet in `the two citation-text probes` and that every member
# of the scanner set is a control; there were three probes when it said two, and it was
# already false of the region rejection. Name the members; do not count them.
#
# Of the thirteen in the narrowing set, ten are rejections and three are the citation-text
# probes. Nothing else this file asserts moves under either.
#
# And each of bean:0121's three constraints is mutated ON ITS OWN, because its second
# criterion is that neither of the first two may be landed on the other's evidence:
#
#   region off        the `region != "EV" && region != "BOTH"` clause deleted
#                                                      ->  66 passed,  5 failed
#   emptiness off     a citing heading's hits committed at once instead of pending
#                                                      ->  67 passed,  4 failed
#   cell off          the evidence cell not cut out of a row
#                                                      ->  65 passed,  6 failed
#
# The three failure sets are disjoint apart from the citation-text probes, which every one of
# them reaches: `region off` kills neither emptiness assertion, `emptiness off` kills neither
# region one and no cell one either, and `cell off` kills only cell verdicts. That is the
# measurement, not the argument.
#
# AND ONE MORE INSIDE `emptiness`, because the constraint has two halves and the suite could
# see only one of them until this bean's review. `Content is a non-blank line` is written
# `line !~ /^[ \t\r]*$/`, and both emptiness fixtures above terminated their section with a
# GENUINELY ZERO-LENGTH line — EOF in one, a sibling heading in the other — so the character
# class was decorative to the suite:
#
#   emptiness, whitespace-blind   `line !~ /^[ \t\r]*$/` relaxed to `line != ""`, so a line
#                                 of spaces or tabs becomes content
#                                                      ->  70 passed,  1 failed
#
# At 58210d6 that same mutation scored 62 passed, 0 failed, rc=0 against the suite as it then
# stood, while a five-criterion bean whose whole `## Evidence` is `### Criteria 1-5` followed
# by ONE SPACE closed green. `a whitespace-only line under a citing heading is not content` is
# the assertion added for it, and it is the only one this mutation kills. This is the
# `intable sticky` blindness one level down and it was found the same way: by mutating a
# clause nobody had aimed a fixture at and watching the suite stay green.
#
# `ACCEPTED: a Markdown table pasted inside <pre> is entered like any other` deliberately
# does NOT fail here: its row is numbered, so the evidence-row path answers it and the
# citation scan is not what closes it. Recorded because it is the difference between a
# control and a fixture that merely looks like one.
#
# A fourth mutation narrows the citation site by ONE clause, and it exists because a reviewer
# measured that clause to be decorative over the corpus and it is not decorative here:
#
#   citation site, no intable  `line ~ /^#+ / || line ~ /^\|/`, the coupling to the
#                              analyser's own table state dropped
#                                                    ->  65 passed,  6 failed
#
# At d914eb5 that form and the shipped form give byte-identical verdicts over all 103 beans
# (103 compared, 0 differing), which is a fact about those inputs and not about the rule. The
# figure is stamped because a bean count in a comment dates itself and this one already did:
# it read 102 while its own head carried 103.
#
# The six assertions it kills are the shape the corpus does not contain — a `|`-leading line
# with no delimiter row over it, which is not a table row to any renderer either — plus the
# probes that read the mechanism directly:
#
#   citation site: a heading and a row of an entered table are sites; prose is not, in or out of raw HTML
#   citation site: raw HTML is NOT modelled: a heading-shaped and a row-shaped line inside <pre> are sites
#   citation text: the evidence cell is cut out of the row, and the rest of it is not
#   citation text: the escape is counted as cell content, not as a cell boundary
#   a pipe-led line that is not a row of an entered table is not a site
#   a table the analyser has LEFT is not entered, so a later stray row is not a site
#
# A FIFTH mutation breaks the same coupling the OTHER way, and it is here because the fourth
# alone left half of `intable` uncovered:
#
#   intable sticky             the three `intable = 0` resets deleted — the `## ` branch,
#                              the `#+ ` branch, and the else branch — so the flag stays set
#                              once any table has been seen
#                                                    ->  70 passed,  1 failed
#
# `citation-site-no-intable` proves the flag is READ. Nothing proved it is CLEARED: against the
# 48-assertion suite at d914eb5 this mutation scored 48 passed, 0 failed — a real weakening the
# suite could not see, which is the blindness bean:0093's blocker entered through. Deleting the
# resets makes a stray `|`-leading line, quoted out of a transcript two paragraphs below a
# table that has ended, into a row of that table.
#
# EVERY FIGURE ABOVE IS RE-MEASURED WHENEVER AN ASSERTION IS ADDED. They were recorded at
# a 31-assertion suite, four assertions were added, and all four went stale at once — in a
# comment block whose whole purpose is to say what the suite can detect. They went stale a
# second time at bean:0093, a third time in that bean's review, when five assertions were
# added for the container limit and the `intable` coupling, and a fourth time in the review of
# THAT review, when three more were added for the pasted-stdout-in-a-cell residual and the
# `intable` resets; every figure moved each time, including the NOEVCOL figure below, which
# had been left at a 43-assertion reading. They went stale a FIFTH time at bean:0121, when
# eleven were added for the region, emptiness and cell constraints; every figure moved again,
# and two of them — `citation site, no intable` and `citation scanner deleted` — moved in
# their PROSE as well as in their numbers, because the set of assertions each kills changed
# and the sentence naming that set is the part a reader relies on. They went stale a SIXTH
# time in bean:0121's review, when nine more were added — the whitespace half of emptiness,
# the CRITERIA region, the adjacent-sibling cost, the empty fence, the two row-shapes that got
# past the cut with its text probe, and the pair that says where an honest span goes — and a
# FOURTEENTH mutation joined them. Every figure above moved a sixth time.
#
# THE PROSE GOES STALE BEFORE THE NUMBERS DO, and that is the lesson of this round rather
# than the arithmetic. Three sentences in this block described kill-sets that their own
# adjacent figures already refuted: the sets `meet only in the two citation-text probes` when
# there were three of them and five members in the meet, `every one of them is a CONTROL`
# when one of the twelve was a rejection, and `both isevcol mutations move the citation-text
# probe` beside a figure reading one failure. So the kill-sets are now ENUMERATED, member by
# member, and never counted in a sentence a reader could believe without checking. That is
# why the mutations are stated as edits anyone can reapply rather than as a reference to
# `scratch/mutate.sh`, a script this repository does not contain. Re-measure by making the
# edit named beside each figure and re-running this file; do not edit a number, and do not
# leave a sentence naming a set you did not re-enumerate.
#
# WHAT THIS SUITE DOES NOT COVER, stated because the sentence above would otherwise imply
# it does. docs-lint-c14.awk owns four further mechanisms that no assertion here targets:
# `allkinds()`/HOLLOW, EMPTYCELL, `isevcol()`/NOEVCOL and NOEV. They are moved-verbatim code
# — a normalised diff against the inline awk they came from shows only the fence changes —
# so they are INHERITED UNTESTED rather than newly untested, which is a weaker claim than
# covered and is the honest one. It was five until bean:0121: the `## `-heading REGION
# tracking is now targeted, because the region constraint reads it and `region off` kills
# assertions that name it.
#
# One of the five still fails OPEN with this suite completely GREEN, which is the sharp form
# and is measured, not argued. It used to be two:
#
#   allkinds-off   HOLLOW detection disabled                   rc=0   71 passed,  0 failed
#   isevcol-true   every column counts as an evidence column   rc=1   70 passed,  1 failed
#   isevcol-false  no column ever counts                       rc=1   59 passed, 12 failed
#
# `isevcol-true` was the second green fail-open until bean:0093, and nothing was done to
# cover it: the evidence-row control added for the citation scan carries an `evidence kind`
# column beside the criteria it cites, so a mutation that counts that column as evidence
# turns the cell into a HOLLOW finding and the control notices. That is incidental coverage
# of a mechanism this file still does not target, and it is recorded as incidental rather
# than claimed as a test, because the next fixture edit could remove it silently. bean:0121
# added a second helping of the same incidental kind and it is recorded on the same terms:
# `evcol` now decides which part of a row is read, so `isevcol-FALSE` moves the citation-text
# probes as well — no column is an evidence column, so no cell is cut and the probes read the
# whole row.
#
# `isevcol-TRUE` does NOT, and an earlier version of this paragraph said both did. Its own
# figure beside it says otherwise and always did: one failure, not three. The single
# assertion it kills is `control: an evidence row citing a range of criteria answers all of
# them`, through the HOLLOW route described above and not through the mask at all — the probe
# fixture's `evidence` column is the LAST column, so making every column an evidence column
# leaves `evcol` where it was and the cut unmoved. A prose claim that its own adjacent figure
# refutes is the cheapest kind of drift there is; the figure is the record.
# Neither mutation is a test of `isevcol()`.
#
# `allkinds-off` makes check 14 ACCEPT beans it should reject and nothing here notices. An
# earlier version of this comment claimed instead that NOEVCOL masks the assertions above it
# without failing any; that does NOT reproduce — forcing `noevcol = 1` on every line gives
# rc=1, 41 passed, 30 failed, so the suite does detect it. The corpus differential does catch
# the fail-open, but that is a one-off run by hand and is not in the gate.
#
# ENFORCEMENT GAP, and it names its bean because doc:00-constitution#observed-failing requires
# a demoted gap to name the work item that closes it: `allkinds()`/HOLLOW is what decides
# whether an evidence cell holds evidence or only the NAME of an evidence kind, and that is
# `bean:0087` (todo, high) — evidence-cell strength. Until it lands, the green line above is
# a claim about the citation and fence mechanisms and not about the cell conditions.
#
# Fixtures are heredocs beside their assertions rather than a fixture directory: the
# repository had no fixture location for docs-lint, and a fixture whose expected output
# lives in another file is read twice and updated once.
#
# WHAT LIVES IN THE SIBLING FILE. This file's subject is the two awk libraries under
# tools/lib/, fed fixtures. tools/docs-lint-gate-test.sh's subject is the SHELL script:
# whether tools/docs-lint.sh goes red when one of its analysers dies, which can only be
# observed by running the whole gate over the whole repository. Adding a gate-level
# assertion here would restate every mutation figure above — EVERY FIGURE ABOVE IS
# RE-MEASURED WHENEVER AN ASSERTION IS ADDED says so — and each re-measurement would then
# carry two full gate runs it has no use for. That is why it is not here (bean:0118).
#
# bash 3.2 (macOS): what that forbids is enumerated in tools/lib/bash32-forbidden.tsv and
# enforced by tools/bash-compat-lint.sh in qualityCheck, not restated here (bean:0049).
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

# The citation-site probe: one citation_site() decision per line, read off the analyser's
# own state and never inferred from a verdict. Loaded AFTER the analyser so that `intable`
# is the value the real citation scan saw for that line, not the value it had a line
# earlier. A line the analyser skipped before reaching this rule — the inside and the
# delimiters of a fenced block — is recorded `f`: "never asked" and "asked and refused"
# are different answers, and a map that wrote `.` for both would survive a mutation that
# simply stopped asking.
cat > "$TMP/site.awk" <<'SITEAWK'
{ while (++seen < FNR) { map = map "f" }
  map = map (citation_site($0) ? "Y" : ".") }
END { while (++seen <= NR) { map = map "f" }
      printf "sites=%s\n", map }
SITEAWK

# The citation-TEXT probe. citation_site() answers whether a line has the SHAPE of a site;
# citation_text() answers what, if anything, is read from it, and the two are different
# questions now that region and the evidence cell narrow the second. A verdict cannot tell
# `the region refused this line` from `the citation was never written` any more than it
# could tell a refused site from an absent one, and for the row case it cannot see the
# masked cell at all — the row still answers through its own number. So the text is read
# off the analyser's own state, one line at a time, on the same `f` convention: a line the
# analyser never reached is `f`, a line read is `Y`, a line refused is `.`.
cat > "$TMP/read.awk" <<'READAWK'
{ while (++seen < FNR) { map = map "f" }
  map = map (citation_text($0) == "" ? "." : "Y") }
END { while (++seen <= NR) { map = map "f" }
      printf "reads=%s\n", map }
READAWK

# And one probe that prints the text ITSELF, for the one fact no map can carry: which part
# of a row survives the evidence-cell mask.
cat > "$TMP/text.awk" <<'TEXTAWK'
{ t = citation_text($0); if (t != "") { printf "text=%d<%s>\n", FNR, t } }
TEXTAWK

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

sites() { # sites <name> <expected map>
  check "citation site: $1" "$2" \
    "$(awk -v KINDS="$KINDS" -f "$FENCE" -f "$C14" -f "$TMP/site.awk" "$FIX" |
       sed -n 's/^sites=//p')"
}

reads() { # reads <name> <expected map>
  check "citation text: $1" "$2" \
    "$(awk -v KINDS="$KINDS" -f "$FENCE" -f "$C14" -f "$TMP/read.awk" "$FIX" |
       sed -n 's/^reads=//p')"
}

texts() { # texts <name> <expected lines>
  check "citation text: $1" "$2" \
    "$(awk -v KINDS="$KINDS" -f "$FENCE" -f "$C14" -f "$TMP/text.awk" "$FIX" |
       sed -n 's/^text=//p')"
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
echo "--- citation site: where the analyser will read a criterion citation"
#
# The second perception layer, and it is a separate mechanism from the fence classifier:
# the classifier decides where a fenced block is, citation_site() decides where a citation
# counts. bean:0093 narrowed it from an EXCLUSION rule — everything is prose except an
# indent or a `>` — to a POSITIVE one: a heading the analyser tracks, or a row of a table
# it has entered. Asserted here on the function's own answer, because a verdict cannot
# tell a site that was refused from a citation that was never written.

cat > "$FIX" <<'EOF'
# a bean

## Evidence

| # | criterion | evidence |
|---|---|---|
| 1 | one | criterion 1 is answered here |

criterion 1 in running prose

<pre>
criterion 1 in a raw HTML block
</pre>
EOF
sites "a heading and a row of an entered table are sites; prose is not, in or out of raw HTML" \
  "Y.Y..YY......"

# THE FIXTURE ABOVE NAMES A PROPERTY IT DOES NOT TEST UNLESS THIS ONE STANDS BESIDE IT. Its
# <pre> holds one line of plain prose — no `#`, no pipe — so `.` there is the PROSE rule
# answering, not a container rule, and the fixture would read identically with the <pre> tags
# deleted. citation_site() takes the line and one flag of the analyser's own state; it has no
# raw-HTML-block state and cannot refuse a container. So both shapes are put inside a <pre>
# here and both are SITES. This is the limitation asserted, not a defect newly introduced:
# bean:0121 owns it, and the assertion is what makes the day it changes visible.
cat > "$FIX" <<'EOF'
# a bean

## Evidence

<pre>
# criterion 1 is not answered in the evidence
| # | criterion | evidence |
|---|---|---|
| 2 | two | criterion 2 is not answered in the evidence |
</pre>
EOF
sites "raw HTML is NOT modelled: a heading-shaped and a row-shaped line inside <pre> are sites" \
  "Y.Y..Y.YY."

cat > "$FIX" <<'EOF'
### Criterion 1
> criterion 1 in a block quote
    criterion 1 indented four columns
<!-- criterion 1 in an HTML comment -->
1. criterion 1 in a list item
EOF
sites "no container is a site, and none of them had to be named to be refused" \
  "Y...."

cat > "$FIX" <<'EOF'
## Evidence

```
criterion 1 inside a fence
```
criterion 1 below it
EOF
sites "a fence's inside and its delimiters are never asked, and the answer is not '.'" \
  "Y.fff."

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

# The same citation at top level no longer answers either. Until bean:0093 this fixture
# was the negative control for the three above, and it asserted the opposite: that a line
# of running prose DOES answer. That is the rule bean:0093 removed — the matcher reads the
# presence of a number and never the polarity of the claim around it, so a top-level line
# reading `criterion 1 is not answered` answered criterion 1 — and this fixture is now the
# planted defect rather than the control.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### The run

criterion 1 is not answered in the evidence, says this line of top-level prose
EOF
decides "the planted defect: pasted output at top level cannot answer its criterion" \
  "$(printf 'UNANSWERED\t1\nSTATS\t1\t0')"

# The negative controls that replace it, and there are two because a citation has two
# structural sites. WITHOUT THESE, EVERY REJECTION ABOVE WOULD ALSO PASS WITH THE CITATION
# SCANNER DELETED OUTRIGHT — which is not a hypothetical: narrowing citation_site() to the
# structural sites and deleting the scanner produce the SAME failure set against the suite
# as it stood before bean:0093, so the suite as it stood could not tell them apart.

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### Criterion 1

the run, described in prose the analyser does not read for citations
EOF
decides "control: an evidence sub-heading naming the criterion answers it" \
  "$(printf 'STATS\t1\t0')"

# The other structural site: a row of a table the analyser has entered. The row is NOT
# numbered, so the evidence-row path (`A[first]` on a numbered row) cannot be what answers
# here — only the citation scan can, which is what makes this a control for the scan and
# not for the row rule beside it.
#
# The citation stands in the row's FIRST cell and not in its evidence cell, because
# bean:0121 took the evidence cell out of the citation scan. That is the same control it
# always was — an unnumbered row, answering only through the scan — moved one column left.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | one | test-run |
| 2 | two | test-run |
| 3 | three | test-run |

## Evidence

| what | evidence |
|---|---|
| criteria 1-3 | `docs-lint: OK`, exit 0 |
EOF
decides "control: an evidence row citing a range of criteria answers all of them" \
  "$(printf 'STATS\t3\t0')"

# THE BOUNDARY OF THE NEW RULE, ACCEPTED AND ASSERTED RATHER THAN LEFT TO BE DISCOVERED.
# The matcher still reads the presence of a number and never the polarity of the claim
# around it — bean:0093 did not remove that, it removed prose from the matcher's reach. So
# a heading that DENIES its criterion still answers it. That is intended and it is not the
# defect this file closed: a heading is not a place output gets pasted, it is an author
# filing a section under a criterion, and the section under it is that criterion's evidence
# home (adr:0005-evidence-lives-in-the-work-item#evidence-home). A bean ruling a criterion
# unmeetable and recording why has answered it. It is live, not hypothetical:
# .beans/modus-0049 heads a section `## Criterion 2 cannot be met as written`, which is why
# the corpus differential for bean:0093 reports that bean losing its first and third
# criteria and not its second.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### Criterion 1 cannot be met as written

the ruling, and the reason for it
EOF
decides "accepted: a heading that denies its criterion still answers it" \
  "$(printf 'STATS\t1\t0')"

# A container hides a transcript from the ENTRY count too, which fails closed: a bean
# whose only evidence is quoted or indented has no entry and cannot close.
#
# This pair is a pair for a reason. The first fixture alone was VACUOUS: its expected output
# is byte-identical to the same fixture with the three quoted lines deleted, so it could not
# tell "the container hid the transcript" from "there is no transcript", and none of the
# three mutations below killed it. The second fixture is the same transcript at top level;
# it is what makes the first one mean anything, because it is the only reason to believe the
# EMPTYEV in the first came from the container.
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

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

```
the whole evidence, at top level
```
EOF
decides "control: the same transcript unquoted IS an entry" \
  "$(printf 'UNANSWERED\t1\nSTATS\t1\t0')"

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

# --- CLOSED by bean:0093: pasted output at column zero ------------------------------
#
# These six were `DEFECT (open)` pins: each asserted that something DOES answer, and each
# was owed a bean. They are one class — a `criterion N is not answered` line standing at
# column zero, with no fence marker and no `>`, inside a container the line-oriented
# analyser cannot enter or inside no container at all — and one change closed all six,
# because the rule that closed them names where a citation MAY stand instead of naming the
# containers it may not. Every one is now a rejection, asserted as a VERDICT.
#
# Two quoted markers BALANCE, so the segment between them is top-level prose — to this
# analyser and to every renderer alike. There was never a perception divergence here to
# fix, which is why the repair is in the citation site and not in the fence classifier.
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
decides "an EVEN number of quoted markers no longer answers the criterion" \
  "$(printf 'UNANSWERED\t1\nSTATS\t1\t0')"

# --- CLOSED by bean:0093: containers the old rule did not model ---------------------
#
# The exclusion rule modelled three containers. These five were not modelled, and each
# renders as code, as a container, or as nothing while standing at column zero with no
# fence marker and no `>`. Every one answered its criterion on the rule that preceded
# bean:0093; none is rejected here by having been ADDED to a list of containers, and that
# is the property under test. `<pre>` is the sharpest — GitHub's sanitiser allows it and it
# renders exactly as a code block — and the HTML comment is the strangest, since it renders
# as nothing at all and closed a criterion with text no reader could see.
#
# doc:05-authoring-for-agents#checks states the rule positively for this reason: an
# enumeration of excluded containers is an allowlist, and this is the proof.

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

### The run

<pre>
FAIL check 14: criterion 1 is not answered in the evidence
FAIL check 14: criterion 2 is not answered in the evidence
</pre>
EOF
decides "a raw HTML <pre> block no longer answers its criteria" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nSTATS\t2\t0')"

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### The run

<!-- criterion 1 is not answered in the evidence -->
EOF
decides "an HTML comment renders as nothing and no longer answers" \
  "$(printf 'UNANSWERED\t1\nSTATS\t1\t0')"

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### The run

<details><summary>run</summary>
<pre>
criterion 1 is not answered in the evidence
</pre>
</details>
EOF
decides "<details> wrapping a <pre> does not answer either" \
  "$(printf 'UNANSWERED\t1\nSTATS\t1\t0')"

# The info-string rule is CommonMark-correct and makes these lines prose rather than a
# fence opener. main holds both only by accident: its toggle flips ON and hides the rest of
# the file, which is the defect this bean removes and which would equally hide a real
# evidence table. So this is the correct reading with an uncovered consequence, not a
# regression to undo — and it is asserted as a verdict, not only as a classification.

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### The run

```json```
FAIL check 14: criterion 1 is not answered in the evidence
EOF
decides "a line-initial inline code span leaves the next line unable to answer" \
  "$(printf 'UNANSWERED\t1\nSTATS\t1\t0')"

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### The run

```sh -c `date`
FAIL check 14: criterion 1 is not answered in the evidence
EOF
decides "a backtick in the info string does the same" \
  "$(printf 'UNANSWERED\t1\nSTATS\t1\t0')"

# --- ACCEPTED, and pinned as a verdict: containers are refused by SHAPE, not modelled -----
#
# The six rejections above are all PROSE inside a container, and every one of them would read
# the same with the container deleted. citation_site() receives the line and one flag of the
# analyser's own state; it holds no raw-HTML-block state, so it cannot refuse a container. Put
# a heading-shaped or row-shaped line inside the same containers and it is a site.
#
# This is the LIMIT of what bean:0093 narrowed, asserted here rather than left for a reviewer
# to find. By this file's own rule it is a DEFECT and not a residual — a residual claims the
# divergence does not change the outcome, and this one DOES change it: the verdict below is a
# green close on three criteria nothing answers. So it owes a bean.
# ACCEPTED, not DEFECT (open), records the second half: the decision not to close it in the
# change that found it. Closing it needs a model of which HTML blocks hold literal
# content, which is an enumeration of tag names — the allowlist the positive rule replaced —
# and would be wrong in the other direction too: a `#` heading inside <details> with blank
# lines around it renders as a heading to CommonMark and to GitHub alike.
#
# RESTATED at bean:0121, which owned all four residuals and closed three of them. These two
# pins are the fourth and they STAY ACCEPTED, deliberately and not by omission. The three
# that closed are each an extra CONDITION on a site this analyser already recognises, decided
# from state it already holds — `region`, the heading level, `evcol` — and each fails closed.
# This one is not of that kind: it needs a perception layer the analyser does not have, and
# the argument above is why building it is the wrong trade. That argument was re-checked
# against `pandoc 3.7.0.2` at bean:0121 and holds: a `#` heading inside <details> with blank
# lines around it is a heading. The residual is now `bean:0129` and these two pins move when
# it does. Note that the region and emptiness constraints do NOT reach it — the container
# below stands inside `## Evidence` under a `### The run` heading with content, which is
# where evidence belongs.
#
# What bean:0093's narrowing DOES still close is the shape it was raised for, and the
# qualifier that makes that sentence true is AT COLUMN ZERO. Check 14's own stdout is
# `FAIL check 14 …: criterion N is not answered in the evidence`, which is neither heading-
# nor row-shaped by itself, so pasting it at column zero into any of the three containers
# below answers nothing — and the `#` prefixes those three fixtures carry had to be written
# by hand.
#
# The qualifier had a cost and the third fixture in this block was it, pinned ACCEPTED here
# until bean:0121. It is now a rejection and it kept its place: paste the same stdout where it
# is NOT at column zero — into the evidence CELL of a row — and the line AROUND it is
# row-shaped. Nothing is written by hand there, which is why it was the one residual of the
# four worth closing first.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three
4. four

## Evidence

### Criterion 1

the run, genuinely recorded here

### The run

<pre>
# criterion 2 is not answered in the evidence
</pre>

<details><summary>run</summary>
<pre>
# criterion 3 is not answered in the evidence
</pre>
</details>

<!--
# criterion 4 is not answered in the evidence
-->
EOF
decides "ACCEPTED: a heading-shaped line inside <pre>, <details> or an HTML comment answers" \
  "$(printf 'STATS\t4\t0')"

# The same, one layer out: a whole Markdown table pasted inside a <pre>. The delimiter row
# sets `intable` wherever it stands, so the analyser enters the table and the row answers.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### The run

<pre>
| # | criterion | evidence |
|---|---|---|
| 1 | one | criterion 1 is not answered in the evidence |
</pre>
EOF
decides "ACCEPTED: a Markdown table pasted inside <pre> is entered like any other" \
  "$(printf 'STATS\t1\t0')"

# CLOSED by bean:0121, and this assertion was an `ACCEPTED` pin until it was. The pin moved
# with the rule rather than being deleted beside it, so the fixture below is byte-identical
# to the one that stood here and only the expectation changed: `STATS 3 0` became a
# rejection. An ordinary evidence table in an ordinary evidence section; row 2's cell quotes
# this check's own stdout about criterion 3, which no row numbers. The citation scan no
# longer reads a row's evidence cell, so nothing answers criterion 3 and the bean cannot
# close. The three residuals bean:0121 closed are asserted in their own block further down;
# this one keeps its place here because the pin it replaces stood here.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three

## Evidence

| # | criterion | evidence |
|---|---|---|
| 1 | one | `docs-lint: OK`, exit 0 |
| 2 | two | `docs-lint: OK`, exit 0; the earlier run had printed `FAIL check 14 …: criterion 3 is not answered in the evidence` |
EOF
decides "pasted stdout in an evidence CELL no longer answers a criterion no row numbers" \
  "$(printf 'UNANSWERED\t3\nSTATS\t3\t0')"

# This control now says something weaker than it did, and it is kept for what it still says.
# It was the control that stopped the ACCEPTED verdict above being produced by a scanner
# that reads nothing; that verdict is now a rejection, so the two agree and neither
# distinguishes the other. What it still distinguishes is the CELL rule from the whole
# citation scan: strip the pasted string and the verdict is identical, which is exactly the
# claim — the cell contributed nothing either way. The control that keeps the rejection
# above honest is now the first-cell control further up, and the sacrifice fixture below.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three

## Evidence

| # | criterion | evidence |
|---|---|---|
| 1 | one | `docs-lint: OK`, exit 0 |
| 2 | two | `docs-lint: OK`, exit 0 |
EOF
decides "control: the same table without the pasted cell leaves criterion 3 unanswered" \
  "$(printf 'UNANSWERED\t3\nSTATS\t3\t0')"

# --- CLOSED by bean:0121: REGION, EMPTINESS and the cost of the CELL rule ------------------
#
# Three constraints beyond the SHAPE test, all three decided from state the analyser already
# holds, all three failing closed. They are asserted together and then separately, because
# bean:0121's second criterion is that neither of the first two may be landed on the other's
# evidence: each fixture below is rejected by exactly one of them, so a mutation removing
# either is visible here. The mutation figures are in this file's header.
#
# REGION. A citing sub-heading under `## Not in scope`, each with a paragraph under it, so
# the emptiness constraint is satisfied and only the region can be what refuses them.
# Criterion 1 is answered normally, which is what stops this fixture being green for the
# trivial reason that nothing is ever answered.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three

## Evidence

### Criterion 1

the run, genuinely recorded here

## Not in scope

### Criterion 2 is deferred to another bean

the reason it is deferred

### Criterion 3 was not attempted

the reason it was not attempted
EOF
decides "a citing sub-heading outside the evidence region answers nothing" \
  "$(printf 'UNANSWERED\t2\nUNANSWERED\t3\nSTATS\t3\t0')"

# The same fixture read at the layer that decides it. `f` is a line the analyser never
# reached, `Y` a line a citation is read from, `.` a line refused. Both refused headings are
# still SITES by shape — the map above them would read `Y` — and this is the only assertion
# that can show the difference between "refused by region" and "no citation there".
sites "the two refused headings are still sites by shape" \
  "Y.Y.....Y.Y...Y.Y...Y.."
reads "and the region is what refuses them, not their shape" \
  "..Y.....Y.Y...Y........"

# THE BOUNDARY OF THE REGION RULE, and it is a bean on `main` and not a hypothetical. The
# constraint binds every citation site EXCEPT a `## ` heading, because `region` is a property
# a `## ` heading CREATES and asking whether one stands in the right region is asking about
# the region it just set. A top-level section devoted to a single criterion IS that
# criterion's evidence home (adr:0005-evidence-lives-in-the-work-item#evidence-home), and
# .beans/modus-0038 — completed, frozen — writes exactly this: `## Criterion 7 is dropped,
# deliberately`, with the ruling and the reason under it and no row 7 in its evidence table.
# The stricter form that binds `## ` too was measured over the whole corpus at 3b02871 and
# takes criterion 7 off that bean. It is therefore the narrowing that is wrong.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

### Criterion 1

the run, genuinely recorded here

## Criterion 2 is dropped, deliberately

the ruling, and the reason for it
EOF
decides "accepted: a top-level section devoted to a criterion answers it" \
  "$(printf 'STATS\t2\t0')"

# THE OTHER SIDE OF THE REGION BOUNDARY, and it is the half the DOCUMENT got wrong rather
# than the code. `region` has four values and the clause admits two: `EV` and `BOTH`. `CRIT`
# — the region a bare `## Success criteria` heading sets — is REFUSED exactly like `NONE`,
# and doc:05-authoring-for-agents#checks said `inside a criteria or evidence section` in two
# places until this bean's review, promising a shape the gate does not accept. That is the
# fail-open direction of a documentation error: the author follows the doc, writes the
# sub-heading under `## Success criteria`, and the gate refuses their close.
#
# The doc now says `an evidence section`, and this is the assertion that will notice if it
# drifts back. The shape is not a hypothetical one — it is the ruling-in-prose shape the
# emptiness controls below accept, standing one section too high. Criterion 1 is answered
# normally by a numbered row, which stops the fixture being green for the trivial reason.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

### Criterion 2 cannot be met as written

the ruling, and the reason for it

## Evidence

| # | criterion | evidence |
|---|---|---|
| 1 | one | `docs-lint: OK`, exit 0 |
EOF
decides "a citing sub-heading in the CRITERIA region answers nothing" \
  "$(printf 'UNANSWERED\t2\nSTATS\t2\t0')"

# EMPTINESS. The whole of a five-criterion bean's `## Evidence` is one citing sub-heading.
# The region constraint is satisfied — it stands under `## Evidence` — so only the emptiness
# constraint can be what refuses it. There is no EMPTYEV here either: the sub-heading is
# itself an entry, which is why the heading path needed its own analogue of EMPTYCELL.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three
4. four
5. five

## Evidence

### Criteria 1-5
EOF
decides "a citing heading with nothing under it answers nothing" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nUNANSWERED\t3\nUNANSWERED\t4\nUNANSWERED\t5\nSTATS\t5\t0')"

# The section ends at the next heading of its own level or shallower, so a citing heading
# followed immediately by a sibling heading heads nothing even though the FILE continues.
# Without this the constraint would be satisfied by any content anywhere below.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

### Criteria 1-2

### The run

`docs-lint: OK`, exit 0
EOF
decides "a citing heading closed by a sibling heading still answers nothing" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nSTATS\t2\t0')"

# THE OTHER HALF OF THE EMPTINESS CONDITION, unasserted until this bean's review and found
# the way `intable sticky` was found: by mutating the clause and watching the suite stay
# green. `a non-blank line` is `line !~ /^[ \t\r]*$/`, and both fixtures above terminate the
# section with a genuinely ZERO-LENGTH line — EOF in the first, a sibling heading in the
# second — so nothing pinned the `[ \t\r]` half. Relax the test to `line != ""` and the suite
# scored 62 passed, 0 failed, rc=0 at 58210d6 while a line of ONE SPACE became content and
# closed a five-criterion bean on a heading that heads nothing.
#
# That is the same blindness one level down from `intable sticky`, and the same lesson: a
# clause is unasserted until a fixture exercises the character class it names, not merely the
# branch it sits in. This is the ONE fixture in this file written with printf rather than a
# heredoc, and the reason is the fixture itself: its last line is a single SPACE, which a
# heredoc renders invisible and any tool that strips trailing whitespace would silently turn
# back into the zero-length line the two fixtures above already cover. Written as `\n \n` it
# cannot be lost without the diff saying so. `\r` is covered by the CRLF perception assertion
# further up.
printf '# a bean\n\n## Success criteria\n\n1. one\n2. two\n3. three\n4. four\n5. five\n\n## Evidence\n\n### Criteria 1-5\n \n' > "$FIX"
decides "a whitespace-only line under a citing heading is not content" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nUNANSWERED\t3\nUNANSWERED\t4\nUNANSWERED\t5\nSTATS\t5\t0')"

# THE COST OF THE SIBLING RULE, pinned rather than left to be discovered. A citing heading is
# closed by the next heading at its own level or shallower, so two ADJACENT citing headings
# sharing one paragraph give the paragraph to the second and drop the first. It is defensible
# — the paragraph stands under the second heading and under nothing else, to CommonMark as
# much as to this analyser — and it is a real shape an author will write, so the verdict is
# recorded and doc:05-authoring-for-agents#checks now names it.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

### Criterion 1

### Criterion 2

the one paragraph that answers both
EOF
decides "of two adjacent citing headings, only the second is answered" \
  "$(printf 'UNANSWERED\t1\nSTATS\t2\t0')"

# What counts as content is A NON-BLANK LINE, and the three controls below are the three
# shapes that must satisfy it. Prose is the one that decides the rule: the stricter reading
# — doc:05-authoring-for-agents#checks's ENTRY, a table row or a sub-heading or a fenced
# block, prose explicitly not one — refuses `### Criterion N cannot be met as written`
# followed by the ruling, which that same document accepts and .beans/modus-0038 writes. It
# was measured over the corpus at 3b02871 and costs that bean its criterion 7. The
# prose control here is what fails under it.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### Criterion 1 cannot be met as written

the ruling, and the reason for it
EOF
decides "control: prose under a citing heading is content" \
  "$(printf 'STATS\t1\t0')"

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### Criterion 1

```
docs-lint: OK
```
EOF
decides "control: a fenced block under a citing heading is content" \
  "$(printf 'STATS\t1\t0')"

# A DEEPER heading stands under the citing one and is an entry by
# doc:05-authoring-for-agents#checks's own definition, so it commits rather than closing the
# section. This is the half a `drop on every heading` rule would get wrong.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### Criterion 1

#### The run

`docs-lint: OK`, exit 0
EOF
decides "control: a deeper heading under a citing heading is content" \
  "$(printf 'STATS\t1\t0')"

# AN EMPTY FENCED BLOCK IS CONTENT, which is a consequence and not an oversight, and it is
# pinned because a reader can reasonably expect either answer. The emptiness constraint asks
# whether anything STANDS UNDER the heading, and doc:05-authoring-for-agents#checks defines an
# entry as `a fenced block` — the block, not the text inside it. The OPEN delimiter is what
# commits the pending citations, so a block with no lines in it commits them exactly as the
# control above does. Nothing weaker would be coherent: the analyser deliberately never reads
# inside a fence, so `is this fence empty` is a question it has no business answering, and a
# rule that answered it would be reading the transcript it exists to refuse to read.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three
4. four
5. five

## Evidence

### Criteria 1-5

```
```
EOF
decides "accepted: an EMPTY fenced block under a citing heading is still content" \
  "$(printf 'STATS\t5\t0')"

# THE CELL RULE'S COST, pinned rather than described. The rule is `do not read a citation out
# of the evidence column of a row`, and it applies to every row, not only to one that numbers
# itself. The narrower form — mask the cell only on a numbered row — was measured over the
# corpus at 3b02871 and gives byte-identical verdicts over all 110 beans, so the corpus does
# not choose between them; what chooses is that the narrow form leaves the identical
# laundering open in the evidence cell of an UNNUMBERED row.
#
# The case sacrificed is below and it is a legitimate one: row 3 records a run, and that run
# genuinely covers the two criteria its cell names. Those two criteria are now UNANSWERED,
# and the author writes the span in any OTHER column instead — the cut is one column wide,
# and the pair of assertions below this one says so in both directions. Not the first cell in
# a table whose rows are numbered: there the first cell is the criterion NUMBER. The evidence
# cell is where output is PASTED; every other column is written by an author deciding
# something. That asymmetry is the whole of the argument for cutting here.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three

## Evidence

| # | criterion | evidence |
|---|---|---|
| 3 | three | `docs-lint: OK`, exit 0 — the same run covers criteria 1-2 |
EOF
decides "sacrificed: a row's evidence cell no longer answers even when it cites honestly" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nSTATS\t3\t0')"

# And the text itself, for the one fact no verdict can show: WHICH PART of that row is read.
# The row still answers criterion 3 through its own number, so a verdict cannot distinguish
# a masked cell from an unmasked one on any row that numbers itself. A heading is read WHOLE
# — there is no cell to cut — and the delimiter row is read too and has nothing in it, both
# recorded here rather than left as surprises.
texts "the evidence cell is cut out of the row, and the rest of it is not" \
  "$(printf '3<## success criteria>\n9<## evidence>\n12<|---|--->\n13<| 3 | three >')"

# --- WHICH FIELDS OF A ROW ARE ITS CELLS, which the cut depends on and nothing pinned -------
#
# The cell rule is stated unconditionally and was conditional in fact: a naive
# `split(line, c, "|")` disagrees with GFM about a row's cells in two ways, and BOTH were
# measured letting the identical laundering string through the cut at 58210d6. rowcells()
# closes them. The two fixtures below are that string — this check's own stdout about criteria
# no row numbers — in the evidence cell of a row shaped each way. Each row is still NUMBERED
# 3, so criterion 3 is answered through the evidence-row path: that is what stops these being
# green for the trivial reason that the row was never read at all.
#
# NO TRAILING PIPE. `| 3 | three | <stdout>` is a row to GFM, and it is a row to this analyser
# too, whose table state comes from the delimiter row above and not from this line's shape.
# split() returned the evidence cell as the LAST field, so `evcol < n` was false and the whole
# line — cell included — was read. At 58210d6 this fixture gave `STATS 3 0`: criteria 1 and 2
# answered by a string nobody wrote about them.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three

## Evidence

| # | criterion | evidence |
|---|---|---|
| 3 | three | criterion 1 is not answered in the evidence; criterion 2 is not answered
EOF
decides "a row with no trailing pipe still has its evidence cell cut" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nSTATS\t3\t0')"

# AN ESCAPED PIPE. `\|` is the documented way to put a pipe inside a cell (the GFM tables extension). split()
# counted it as a delimiter, so every field after it shifted by one and `evcol` addressed the
# column to its LEFT — the mask cut `three` and left the evidence cell whole. At 58210d6 this
# fixture also gave `STATS 3 0`. Note that this is not a cell the author had to contrive: a
# bean pasting a Markdown table into a cell escapes every pipe in it.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three

## Evidence

| # | criterion | evidence |
|---|---|---|
| 3 | three | a \| b criterion 1 is not answered; criterion 2 is not answered |
EOF
decides "an escaped pipe in a cell does not misalign the cut" \
  "$(printf 'UNANSWERED\t1\nUNANSWERED\t2\nSTATS\t3\t0')"

# And the text itself for the escaped-pipe row, because the verdict above is an ABSENCE and
# an absence cannot say WHICH column was cut. `| 3 | three ` is the whole of what survives:
# the escape is inside the masked cell, so no placeholder reaches the matcher either.
texts "the escape is counted as cell content, not as a cell boundary" \
  "$(printf '3<## success criteria>\n9<## evidence>\n12<|---|--->\n13<| 3 | three >')"

# --- WHERE AN HONEST SPAN GOES, which the cell rule's cost sentence names ------------------
#
# `doc:05-authoring-for-agents#checks` and this analyser both said `write that span in the
# row's first cell instead` until this bean's review, and in a table whose rows are NUMBERED
# that instruction destroys the row: the first cell is the criterion number, `first ~
# /^[0-9]+$/` stops matching, and the row no longer answers even its own criterion. The cut
# is one column wide, so the workaround is any OTHER column, and both halves are pinned here
# rather than described — the doc now says `any other column` on the strength of this pair.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three

## Evidence

| # | criterion | evidence |
|---|---|---|
| 3 | three; the same run covers criteria 1-2 | `docs-lint: OK`, exit 0 |
EOF
decides "control: an honest span in a non-evidence column answers, and the row keeps its own number" \
  "$(printf 'STATS\t3\t0')"

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two
3. three

## Evidence

| # | criterion | evidence |
|---|---|---|
| criteria 1-2 | three | `docs-lint: OK`, exit 0 |
EOF
decides "the same span in the FIRST cell costs the row its own criterion" \
  "$(printf 'UNANSWERED\t3\nSTATS\t3\t0')"

# --- the `intable` coupling, which is load-bearing and was measured to be decorative -------
#
# Reduce citation_site() to `line ~ /^#+ / || line ~ /^\|/` and, at d914eb5, every one of the
# 103 beans in the corpus gives a byte-identical verdict. That measurement is true and it is
# evidence about THOSE INPUTS, not about the rule; it carries a head because the corpus grows
# under it. The pair below is the shape the corpus does not happen
# to contain: a `|`-leading line with no delimiter row above it, which is not a table row to
# any renderer either. Without the coupling it is a citation site and closes the criterion it
# reports unanswered; with it, it does not. The rejection is the assertion; the control below
# is what stops the rejection being produced by a citation scanner that reads nothing. The
# third fixture covers the other half of the coupling: the flag must also be CLEARED.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### The run

| criterion 1 | `docs-lint: OK`, exit 0 |
EOF
decides "a pipe-led line that is not a row of an entered table is not a site" \
  "$(printf 'UNANSWERED\t1\nSTATS\t1\t0')"

cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one

## Evidence

### The run

| criterion | evidence |
|---|---|
| criterion 1 | `docs-lint: OK`, exit 0 |
EOF
decides "control: the identical line under a delimiter row IS a row, and answers" \
  "$(printf 'STATS\t1\t0')"

# The coupling has TWO halves and the pair above covers one. `intable` is set on a delimiter
# row and RESET in three places — the `## ` branch, the `#+ ` branch, and the else branch that
# every non-table line falls through. Delete all three resets and the flag is sticky once any
# table has been seen: a filled evidence table, a paragraph, then a row quoted out of a
# transcript, and that stray row is read as a row of the table that ended two paragraphs
# earlier. The suite scored 48 passed / 0 failed against that edit before this fixture existed,
# which is the same blindness that let bean:0093's blocker through — the rejection was there,
# nothing required the reset to produce it.
cat > "$FIX" <<'EOF'
# a bean

## Success criteria

1. one
2. two

## Evidence

| # | criterion | evidence |
|---|---|---|
| 1 | one | `docs-lint: OK`, exit 0 |

The run also printed the line below, quoted here out of its transcript.

| criterion 2 is not answered in the evidence
EOF
decides "a table the analyser has LEFT is not entered, so a later stray row is not a site" \
  "$(printf 'UNANSWERED\t2\nSTATS\t2\t0')"

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
