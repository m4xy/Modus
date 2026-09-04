# The check 14 analyser — one bean in, one verdict line per finding out. Loaded with
# `awk -f docs-lint-fence.awk -f docs-lint-c14.awk`; the fence file owns where a fenced
# block is and this one owns what that means, because bean:0063 showed the two concerns
# fail separately and so must be tested separately.
#
# Reads one variable, KINDS. Emits, one per line, tab separated:
#   UNTERMFENCE <line>          a fenced block opened there and never closed
#   NOEV                        no evidence home
#   EMPTYEV                     an evidence home with no entry
#   EMPTYCELL <criterion>       an evidence cell that is blank
#   HOLLOW <criterion> <cell>   an evidence cell that is only evidence-kind names
#   NOEVCOL <heading>           a numbered table in an evidence section with no evidence column
#   UNANSWERED <criterion>      a numbered criterion nothing answers
#   STATS <criteria> <unnumbered>

BEGIN { fence_reset() }

function norm(s) {
  gsub(/`/, "", s); gsub(/\*/, "", s)
  sub(/^[ \t]+/, "", s); sub(/[ \t]+$/, "", s)
  return tolower(s)
}
function isevcol(h) {
  return (h == "evidence" || h == "observed" || h == "observed output" ||
          h == "output" || h == "result")
}
# Where a `criterion N` citation counts: a STRUCTURAL SITE, and nowhere else. A structural
# site is a heading this analyser tracks, or a row of a table it has entered — the two
# places a bean names a criterion in order to file evidence under it, rather than in order
# to talk about it.
#
# This replaces an exclusion rule, and the replacement is the point. The exclusion rule
# accepted every line and then subtracted containers it could name — four or more columns
# of indent (CommonMark §4.4), a `>` on the citing line (§5.1). That is an allowlist by
# another name: it was walked past by a raw HTML block, an HTML comment, a `<details>`
# wrapper, a lazy block-quote continuation, and by ordinary top-level prose holding a
# pasted transcript, in which this check's own `criterion N is not answered` message
# answered the criterion it reports unanswered (bean:0093). Each escape cost one more
# subtraction, and the next container nobody had named was free again.
#
# A positive property does not have that failure mode. Running prose is not a citation site
# whatever it renders as, so pasted output and a sentence merely ABOUT a criterion number
# are rejected BY CONSTRUCTION rather than by extension — the matcher never has to read
# polarity, because it never reads running prose at all. A criterion cited only from prose
# is unanswered, which is the direction that fails closed.
#
# WHAT THIS FUNCTION DOES NOT DO, stated because the paragraph above would otherwise be read
# as saying it. It receives the line text and reads one flag of the analyser's own state. It
# has NO model of raw HTML blocks and cannot refuse a container. A container is refused only
# insofar as its CONTENTS are neither heading-shaped nor row-shaped; a `#`-leading line
# inside <pre>, inside <details><pre>, or inside an HTML comment is a site here, and a
# Markdown table pasted inside <pre> is entered, because its delimiter row sets `intable`
# like any other. Every one of those shapes is pinned in tools/docs-lint-test.sh, as a
# verdict and not only as a classification, under the heading `ACCEPTED`.
#
# That residual is ACCEPTED here rather than closed, and bean:0121 owns it. Refusing those
# lines needs a model of which HTML blocks hold literal content — CommonMark §4.6's type 1,
# whose four tag names are the whole rule, and type 2's comment — which is an enumeration of
# containers, the allowlist this replaced. It would also be wrong in the other direction: a
# `# heading` inside <details> with blank lines around it IS a heading to CommonMark and to
# GitHub, so "inside a container" and "not rendered as a heading" are not the same set. The
# narrowing still closes the shape bean:0093 was raised for — check 14's own stdout at column
# zero is neither heading- nor row-shaped wherever it is pasted.
#
# `intable` is the analyser's own table state, set on the delimiter row below. It is NOT a
# container model and the reason to keep it is not one: a `|`-leading line with no delimiter
# row above it is not a table row to any renderer either, and `line ~ /^\|/` alone would make
# one a citation site — a bean quoting a single table row out of a transcript would answer the
# criterion that row names. Asserted below as a verdict, not left to the corpus: over the 102
# beans at this head the two forms are byte-identical, which is a fact about today's inputs
# and not a property of the rule.
function citation_site(line) {
  return (line ~ /^#+ / || (intable && line ~ /^\|/))
}
function allkinds(c,   t, i, n, a) {
  t = norm(c)
  if (t == "") { return 0 }
  gsub(/[,;\/+]/, " ", t); gsub(/ and /, " ", t); sub(/\.$/, "", t)
  n = split(t, a, /[ \t]+/)
  if (n == 0) { return 0 }
  for (i = 1; i <= n; i++) {
    if (a[i] != "" && index(KINDS, " " a[i] " ") == 0) { return 0 }
  }
  return 1
}
{
  line = $0
  k = fence_classify(line)
  # A fence is an ENTRY but is not a citation site, and its delimiters are neither
  # (doc:05-authoring-for-agents#checks). Content inside it is skipped whole: it holds
  # verbatim output, and in this repository that output quotes this check's own
  # `criterion N is not answered` message (bean:0061).
  if (k == "OPEN") {
    if (region == "EV" || region == "BOTH") { entries++ }
    prev = ""; next
  }
  if (k == "CLOSE") { prev = ""; next }
  if (k == "IN") { next }

  if (line ~ /^## /) {
    h = tolower(line)
    if (h ~ /evidence/ && h ~ /criteri/) { region = "BOTH"; has_ev = 1 }
    else if (h ~ /evidence/)             { region = "EV";   has_ev = 1 }
    else if (h ~ /criteri/)              { region = "CRIT" }
    else                                 { region = "NONE" }
    head = line; sub(/^#+ /, "", head)
    intable = 0; evcol = 0
  } else if (line ~ /^#+ /) {
    if (region == "EV" || region == "BOTH") { entries++ }
    intable = 0; evcol = 0
  } else if (region == "NONE" && line ~ /^\*{0,2}Success criteria/) {
    region = "CRIT"
  } else if (line ~ /^\|/) {
    if (line ~ /^\|[ :|-]+\|[ \t]*$/) {
      nh = split(prev, hc, "|"); evcol = 0
      for (i = 2; i < nh; i++) { if (isevcol(norm(hc[i]))) { evcol = i } }
      intable = 1; flagged = 0
    } else if (intable) {
      nc = split(line, cc, "|")
      first = norm(cc[2])
      numbered = (first ~ /^[0-9]+$/)
      if (region == "CRIT" || region == "BOTH") {
        if (numbered) { C[first + 0] = 1; if (first + 0 > maxN) { maxN = first + 0 } }
        else { unnum++ }
      }
      if (region == "EV" || region == "BOTH") {
        entries++
        # A numbered row answers its criterion only when the table carries an
        # evidence column. Without one the row is a restated criterion, and in
        # region BOTH it would otherwise satisfy C[n] and A[n] at once — the
        # bean:0045 defect of this very check, reachable by renaming a column
        # header from `evidence` to `evidence kind`.
        if (numbered && evcol > 1) { A[first + 0] = 1 }
        if (numbered && evcol <= 1 && !flagged) {
          flagged = 1; noevcol = 1
          printf "NOEVCOL\t%s\n", head
        }
      }
      if (evcol > 1 && evcol < nc) {
        cell = cc[evcol]
        if (norm(cell) == "") {
          printf "EMPTYCELL\t%s\n", (numbered ? first : "?")
        } else if (allkinds(cell)) {
          printf "HOLLOW\t%s\t%s\n", (numbered ? first : "?"), norm(cell)
        }
      }
    }
  } else {
    intable = 0; evcol = 0
    if (region == "CRIT" || region == "BOTH") {
      if (line ~ /^[0-9]+\. /) {
        n = line; sub(/\..*/, "", n)
        C[n + 0] = 1; if (n + 0 > maxN) { maxN = n + 0 }
      } else if (line ~ /^[-*] /) { unnum++ }
    }
  }

  # A criterion is answered by an evidence row bearing its number, or by being cited by
  # number from a structural site: `### Criterion 3` as an evidence sub-heading, or a
  # table row reading `| 3 | … | criteria 1–5 | …`. Not from running prose.
  s = citation_site(line) ? tolower(line) : ""
  while (match(s, /criteri(on|a)[^0-9a-z]*[0-9]+([^0-9a-z]{1,3}[0-9]+)?/)) {
    t = substr(s, RSTART, RLENGTH); s = substr(s, RSTART + RLENGTH)
    gsub(/[^0-9]+/, " ", t); nn = split(t, ar, /[ \t]+/)
    lo = 0; hi = 0
    for (i = 1; i <= nn; i++) { if (ar[i] != "") { if (lo == 0) lo = ar[i] + 0; else hi = ar[i] + 0 } }
    if (hi > lo && hi - lo < 20) { for (k2 = lo; k2 <= hi; k2++) { A[k2] = 1 } }
    else if (lo > 0) { A[lo] = 1 }
  }
  prev = line
}
END {
  # An unterminated fence means the file never said where its blocks end, so every line
  # after it was read as code. Findings printed above came from lines read before it and
  # stand; every verdict below is derived from an ABSENCE, and an absence measured through
  # a block of unknown extent is not an observation. Report the cause, not its cascade.
  unterm = fence_unterminated()
  if (unterm) {
    printf "UNTERMFENCE\t%d\n", unterm
  } else if (!has_ev) {
    print "NOEV"
  } else if (entries == 0) {
    print "EMPTYEV"
  }
  # A table with no evidence column is the root cause; the criteria it fails to
  # answer are its cascade, and reporting both buries the one line that says
  # what to fix.
  nc = 0
  for (i = 1; i <= maxN; i++) {
    if (C[i]) { nc++; if (!A[i] && !noevcol && !unterm) { printf "UNANSWERED\t%d\n", i } }
  }
  printf "STATS\t%d\t%d\n", nc, unnum + 0
}
