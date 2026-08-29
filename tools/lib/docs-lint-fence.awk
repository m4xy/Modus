# Where a fenced block is — CommonMark §4.5, as much of it as a Markdown document in
# this repository can contain. Loaded with `awk -f` beside the analyser that consumes it.
#
# This file owns what an analyser PERCEIVES, which is a separate concern from what it
# DECIDES and carries its own tests (tools/docs-lint-test.sh). A single toggle flipped by
# every line matching /^[ \t]*```/ was the bean:0063 defect: a fence marker that is
# CONTENT — a transcript quoting this repository's own fenced output — contributed an odd
# flip, and every line after it was read with its inside/outside sense reversed. That
# fails open when the reversed segment is pasted lint output and closed when it is a
# filled evidence table, and the parity, not the presence, decides which.
#
# Three properties replace the toggle, and none of them guesses:
#
#   1. A fence closes only on a marker of the SAME character, at least as long as the
#      one that opened it. A transcript quoting three backticks inside a four-backtick
#      fence is content, which is the documented way to quote a marker.
#   2. A backtick fence's info string may not contain a backtick. That is what stops a
#      line-initial inline code span from opening a block.
#   3. A marker indented four or more columns is not a delimiter.
#
# What remains ambiguous is REFUSED rather than guessed at: fence_unterminated() reports
# a block that never closes, and the caller fails the file naming the line. An allowlist
# of prose contexts where a marker is "really" content cannot be written — someone can
# always write a line it does not name — so the requirement is that the file say
# unambiguously where its fences are.
#
# No GNU awk extension is used: this runs under the BSD awk macOS ships.

function fence_reset() {
  FENCE_IN = 0
  FENCE_CHAR = ""
  FENCE_LEN = 0
  FENCE_LINE = 0
}

# Count of leading whitespace CHARACTERS.
function fence_lead(line,   i, c) {
  for (i = 1; i <= length(line); i++) {
    c = substr(line, i, 1)
    if (c != " " && c != "\t") { return i - 1 }
  }
  return length(line)
}

# Leading whitespace measured in COLUMNS, a tab advancing to the next multiple of four
# (CommonMark's preliminary tab expansion). Four or more columns is an indented chunk.
function fence_cols(line, lead,   i, n) {
  n = 0
  for (i = 1; i <= lead; i++) {
    if (substr(line, i, 1) == "\t") { n = n + 4 - (n % 4) } else { n++ }
  }
  return n
}

# Length of the run of `ch` that `body` opens with.
function fence_run(body, ch,   n) {
  n = 0
  while (substr(body, n + 1, 1) == ch) { n++ }
  return n
}

# A closing marker may be followed by whitespace only. The carriage return is included
# so a CRLF file closes its fences rather than reporting every one of them unterminated.
function fence_blank(s,   t) {
  t = s
  gsub(/[ \t\r]/, "", t)
  return (t == "")
}

# Classifies one line and advances the state. Returns exactly one of:
#   OPEN   the line opens a fenced block   — a delimiter, not content
#   CLOSE  the line closes one             — a delimiter, not content
#   IN     the line is content inside a fenced block
#   OUT    the line is outside every fenced block
function fence_classify(line,   lead, cols, body, n, rest) {
  lead = fence_lead(line)
  cols = fence_cols(line, lead)
  body = substr(line, lead + 1)

  if (FENCE_IN) {
    if (cols <= 3) {
      n = fence_run(body, FENCE_CHAR)
      if (n >= FENCE_LEN && fence_blank(substr(body, n + 1))) {
        FENCE_IN = 0
        return "CLOSE"
      }
    }
    return "IN"
  }

  if (cols > 3) { return "OUT" }

  n = fence_run(body, "`")
  if (n >= 3) {
    rest = substr(body, n + 1)
    if (index(rest, "`") > 0) { return "OUT" }
    FENCE_IN = 1
    FENCE_CHAR = "`"
    FENCE_LEN = n
    FENCE_LINE = NR
    return "OPEN"
  }

  n = fence_run(body, "~")
  if (n >= 3) {
    FENCE_IN = 1
    FENCE_CHAR = "~"
    FENCE_LEN = n
    FENCE_LINE = NR
    return "OPEN"
  }

  return "OUT"
}

# The line a still-open block was opened on, or 0. Non-zero means the file did not say
# where its fences are and no verdict derived from the ABSENCE of something after that
# line is admissible.
function fence_unterminated() {
  return FENCE_IN ? FENCE_LINE : 0
}
