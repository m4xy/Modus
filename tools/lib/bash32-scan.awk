# The bash 3.2 compatibility scan. One pass over a shell script, reporting every line
# that matches a row of tools/lib/bash32-forbidden.tsv (passed as -v PAT=…).
#
# awk rather than grep, deliberately. `grep` on an agent's interactive PATH here has been a
# ugrep shim while /usr/bin/grep, `bash -c` and CI were BSD grep, and a pattern captured
# under one was irreproducible under the other (bean:0115). A gate whose verdict depends on
# which grep answered is not a gate. awk is already the analysis language of tools/lib/ and
# its ERE behaves the same under BSD awk and gawk for the constructs used here — no
# backslash escapes, no interval expressions, no word boundaries, all of which differ. The
# patterns do use POSIX classes, which mawk gained in 1.3.4 and which CI's awk is therefore
# assumed to have rather than observed to have; that assumption is not load-bearing, because
# an awk that compiled them differently would fail the planted-sample assertions in
# tools/bash-compat-lint.sh loudly instead of reporting every script clean.
#
# FULL-LINE COMMENTS ARE SKIPPED, and that is a hole with a reason. A comment never runs, so
# a bash 4 construct inside one cannot break bash 3.2; and the constraint is DOCUMENTED in
# the very scripts this scans, so a scanner that read comments would fire on the prose
# stating its own rule. Trailing comments are NOT stripped: `#` inside a string or a regex is
# ordinary, and a stripper that got that wrong would blind the scan to the code before it.
#
# Output: one `file:line: rule: source` per hit, on stdout. Nothing else, so a caller can
# count lines.

BEGIN {
    n = 0
    while ((getline line < PAT) > 0) {
        if (line ~ /^[ \t]*#/) { continue }
        if (line ~ /^[ \t]*$/) { continue }
        split(line, f, "\t")
        if (f[1] == "" || f[2] == "") { continue }
        n++
        name[n] = f[1]
        re[n] = f[2]
    }
    close(PAT)
    if (n == 0) {
        # A scan with no patterns matches nothing and would report every file clean.
        # doc:00-constitution#observed-failing: a run that examined nothing may not
        # report success.
        print "bash32-scan: no patterns loaded from " PAT > "/dev/stderr"
        exit 2
    }
}

/^[ \t]*#/ { next }

{
    for (i = 1; i <= n; i++) {
        if ($0 ~ re[i]) {
            printf "%s:%d: %s: %s\n", FILENAME, FNR, name[i], $0
        }
    }
}
