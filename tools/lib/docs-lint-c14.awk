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
# The SHAPE half of where a `criterion N` citation counts: a STRUCTURAL SITE, and nowhere
# else. A structural site is a heading this analyser tracks, or a row of a table it has
# entered — the two places a bean names a criterion in order to file evidence under it,
# rather than in order to talk about it. Shape is necessary and, since bean:0121, not
# sufficient: read the block under this function for the other three conditions.
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
# as saying it. Two of the three things it does not do are done BELOW IT, by citation_text()
# and by the pending buffer, and this paragraph is about the one that is still not done at
# all. It receives the line text and reads one flag of the analyser's own state. It
# has NO model of raw HTML blocks and cannot refuse a container. A container is refused only
# insofar as its CONTENTS are neither heading-shaped nor row-shaped; a `#`-leading line
# inside <pre>, inside <details><pre>, or inside an HTML comment is a site here, and a
# Markdown table pasted inside <pre> is entered, because its delimiter row sets `intable`
# like any other. Every one of those shapes is pinned in tools/docs-lint-test.sh, as a
# verdict and not only as a classification, under the heading `ACCEPTED`.
#
# That residual is ACCEPTED here rather than closed, and `bean:0129` owns it. Refusing those
# lines needs a model of which HTML blocks hold literal content — CommonMark §4.6's type 1,
# whose four tag names are the whole rule, and type 2's comment — which is an enumeration of
# containers, the allowlist this replaced. It would also be wrong in the other direction: a
# `# heading` inside <details> with blank lines around it IS a heading to CommonMark and to
# GitHub, so "inside a container" and "not rendered as a heading" are not the same set. The
# narrowing still closes the shape bean:0093 was raised for — check 14's own stdout at column
# zero is neither heading- nor row-shaped wherever it is pasted.
#
# `AT COLUMN ZERO` USED TO BE A QUALIFIER THAT COST SOMETHING, and bean:0121 is what stopped
# it costing. The whole line was scanned once it was a site, so the same stdout pasted into
# the evidence CELL of a row was read — the cell is not at column zero, but the row around it
# is row-shaped, and it answered a criterion no row of that table numbered. citation_text()
# below cuts the evidence cell out of the row, so it no longer does.
#
# `intable` is the analyser's own table state, set on the delimiter row below and CLEARED on
# every heading and every line that is not a table row. It is NOT a container model and the
# reason to keep it is not one: a `|`-leading line with no delimiter row above it is not a
# table row to any renderer either, and `line ~ /^\|/` alone would make one a citation site — a
# bean quoting a single table row out of a transcript would answer the criterion that row
# names. The three resets are load-bearing for the same reason and are asserted separately:
# without them the flag is sticky and a stray quoted row two paragraphs below a table that has
# ended is read as a row of it. Asserted as verdicts, not left to the corpus: at d914eb5 the
# two forms are byte-identical over all 103 beans — 103 compared, 0 differing — which is a fact
# about those inputs and not a property of the rule. The figure carries a head because the
# corpus grows under it, and this comment has already had the count go stale once.
function citation_site(line) {
  return (line ~ /^#+ / || (intable && line ~ /^\|/))
}
# THE SHAPE TEST ABOVE IS ONE OF FOUR CONSTRAINTS, AND THE OTHER THREE LIVE BELOW. A site's
# shape says the line is the kind of thing a citation may stand on; it does not say the
# citation counts. bean:0121 measured three ways it did not:
#
#   region     a `### Criterion 3 was not attempted` heading under `## Not in scope`
#              answered criterion 3, because citation_site() never read `region`
#   emptiness  `### Criteria 1-5` as the WHOLE of a five-criterion bean's `## Evidence`
#              closed it, because nothing required the heading to head anything
#   cell       the whole row is scanned, so this check's own stdout pasted into the
#              EVIDENCE CELL of a row answered the criterion that stdout reports unanswered
#
# All three are decided from state this analyser already holds — `region`, the heading
# level, `evcol` — so none of them needs a new perception layer, and all three fail CLOSED:
# a citation that does not satisfy them is not read, and its criterion is reported
# UNANSWERED. None of them reads polarity. `### Criterion 2 cannot be met as written` is
# refused where it stands outside the evidence region or heads nothing, and answers where it
# does not, exactly as before (doc:05-authoring-for-agents#checks).
#
# citation_text() owns region and cell. It returns the TEXT of the line a citation may be
# read from, or the empty string for none, and it is the whole of the answer for a row.
# `region` gates both site kinds, which aligns the citation path with the numbered-row path
# beside it: `A[first]` has always required region EV or BOTH, and only the citation scan
# did not.
#
# The cell rule is `do not read a citation out of the evidence column of a row`, and it
# applies to EVERY row, not only to a row that numbers itself. The narrower form — mask the
# cell only on a numbered row — closes the shape bean:0121 measured and leaves the same
# laundering open one column over, in the evidence cell of an UNNUMBERED row, which is the
# same machine-generated string arriving through the same site. What the broad form
# sacrifices is stated rather than left implicit: a row that legitimately names, in its
# evidence cell, a span of criteria the run recorded in that cell genuinely covers. That
# author writes the span in ANY OTHER COLUMN of the row — the cut is one column wide and the
# rest of the row is read whole. Not, as this comment and doc:05-authoring-for-agents#checks
# both said until this bean's review, `the row's first cell`: in the shape-A table the first
# cell is the criterion NUMBER, and a span written there stops `first ~ /^[0-9]+$/` matching,
# so the row is no longer numbered and stops answering its own criterion. The first cell is
# where the row says what it is about only in the shape-B table, whose rows are unnumbered.
# The evidence cell is where output is PASTED, and that asymmetry is the reason to cut here.
# The corpus does not choose between the two forms: over all 110 beans at 3b02871 they give
# byte-identical verdicts, 110 compared and 0 differing. The reasoning chooses.
#
# WHICH FIELDS OF A ROW ARE ITS CELLS is its own question, and a naive `split(line, c, "|")`
# answers it wrongly in two ways that were each measured to BYPASS the cut. The doc states
# the cell rule unconditionally, so both are closed here rather than described:
#
#   no trailing pipe   `| 3 | three | <pasted stdout>` is a row to GFM, and it is a row to
#                      this analyser, whose table state is set by the delimiter row above and
#                      not by this line's shape. split() then returns the evidence cell as
#                      the LAST field, `evcol < n` was false, and the whole line — cell
#                      included — was read.
#   an escaped pipe    `\|` is the documented way to put a pipe INSIDE a cell (the GFM tables extension).
#                      split() counted it as a delimiter, so every field after it shifted by
#                      one and the mask cut a column that was not the evidence column.
#
# Both are closed in one place, because a second rule for what a row's cells are is how the
# two halves would drift apart. rowcells() returns the index of the LAST REAL cell; the cells
# are c[2] .. c[last]. c[1] is the empty field before the leading pipe and is never a cell.
# The escape becomes SUBSEP rather than being removed, so the matcher and every width the
# caller measures see the same number of characters they always did.
function rowcells(line, c,   s, n) {
  s = line
  gsub(/\\\|/, SUBSEP, s)
  n = split(s, c, "|")
  return (s ~ /\|[ \t]*$/) ? n - 1 : n
}
function citation_text(line,   last, c, i, t) {
  if (!citation_site(line)) { return "" }
  if (line !~ /^## / && region != "EV" && region != "BOTH") { return "" }
  if (line ~ /^#+ /) { return tolower(line) }
  last = rowcells(line, c)
  if (evcol > 1 && evcol <= last) {
    t = ""
    for (i = 2; i <= last; i++) { if (i != evcol) { t = t "|" c[i] } }
    return tolower(t)
  }
  return tolower(line)
}
# The matcher, unchanged in what it reads and moved only so that a heading's hits can be
# held rather than committed. `criteria 1, 2 and 5` is read as 1-2, because ` and ` is not
# a separator here; that is the matcher bean:0093 shipped and this bean does not touch it.
function scan(s, dest,   t, nn, ar, i, lo, hi, k2, hit) {
  hit = 0
  while (match(s, /criteri(on|a)[^0-9a-z]*[0-9]+([^0-9a-z]{1,3}[0-9]+)?/)) {
    t = substr(s, RSTART, RLENGTH); s = substr(s, RSTART + RLENGTH)
    gsub(/[^0-9]+/, " ", t); nn = split(t, ar, /[ \t]+/)
    lo = 0; hi = 0
    for (i = 1; i <= nn; i++) { if (ar[i] != "") { if (lo == 0) lo = ar[i] + 0; else hi = ar[i] + 0 } }
    if (hi > lo && hi - lo < 20) { for (k2 = lo; k2 <= hi; k2++) { dest[k2] = 1; hit = 1 } }
    else if (lo > 0) { dest[lo] = 1; hit = 1 }
  }
  return hit
}
# EMPTINESS, held in P[] because it is not decidable from the citing line. A heading's
# citations are PENDING until something stands under the heading; then they are committed,
# and if the section ends first they are dropped and the criteria come back UNANSWERED.
#
# What counts as standing under it is `a non-blank line`, and the alternative was
# doc:05-authoring-for-agents#checks's ENTRY — a table row, a sub-heading or a fenced block,
# prose explicitly not one. Entry is the stricter reading of the same document and it is
# rejected on measured cost: it refuses a heading naming a criterion followed by the ruling
# and its reason, which that same document accepts in as many words. Two beans on main write
# that shape — .beans/modus-0038 and .beans/modus-0049 — and under the entry rule
# .beans/modus-0038 loses its seventh criterion, which nothing else in it answers. It is
# `completed` and frozen, so it would not fail; a narrowing that would have failed it if it
# closed again is still the wrong narrowing. A ruling in prose IS the evidence for a
# criterion that cannot be met; a run's transcript is not. Both corpus figures are in
# bean:0121. So the constraint here is EMPTYCELL's analogue — nothing at all under the
# heading — and not HOLLOW's.
function pend_commit(   n) { for (n in P) { A[n] = 1; delete P[n] }; pendlvl = 0 }
function pend_drop(   n)   { for (n in P) { delete P[n] };           pendlvl = 0 }
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
    # A fence stands UNDER the heading above it, so it commits that heading's pending
    # citations even though it is not itself a citation site.
    pend_commit()
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
      # rowcells(), not split(), and for the reason above it: `evcol` is an index into a
      # row's cells, so the header and every row it indexes must agree on what a cell is.
      nh = rowcells(prev, hc); evcol = 0
      for (i = 2; i <= nh; i++) { if (isevcol(norm(hc[i]))) { evcol = i } }
      intable = 1; flagged = 0
    } else if (intable) {
      nc = rowcells(line, cc)
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
      if (evcol > 1 && evcol <= nc) {
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
  # number from a structural site INSIDE THE EVIDENCE REGION: `### Criterion 3` as an
  # evidence sub-heading with something under it, or a table row reading
  # `| 3 | criteria 1–5 | … |` outside its evidence cell. Not from running prose, not from
  # another section, not from a heading that heads nothing, not from a pasted cell.
  if (line ~ /^#+ /) {
    # A heading ENDS the section of every heading at its own level or shallower, so a
    # pending citation there was never followed by anything and is dropped. A DEEPER
    # heading stands under it and is an entry, so it commits instead — the citing heading
    # heads a sub-section, which is content by either definition.
    lvl = index(line, " ") - 1
    if (lvl <= pendlvl) { pend_drop() } else { pend_commit() }
    if (scan(citation_text(line), P)) { pendlvl = lvl }
  } else {
    scan(citation_text(line), A)
    if (line !~ /^[ \t\r]*$/) { pend_commit() }
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
