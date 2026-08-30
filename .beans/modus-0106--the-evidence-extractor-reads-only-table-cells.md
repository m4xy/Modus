---
# modus-0106
title: The evidence extractor reads only table cells, and only table cells reproduce
status: todo
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# The evidence extractor reads only table cells, and only table cells reproduce

`bean:0069` describes a script that "extracts every `` `cmd`: `...` `` from **this table**, runs
each in the repository root, and reports any that produce no output or that cannot be pasted at
all". The scope in that sentence is the finding: a bean file carries `cmd:`/`observed:` blocks in
its prose as well as in its criteria table, and the extractor sees none of them.

The job is to extend it to every `cmd:`/`observed:` block in a bean file. What makes the job
worth doing rather than merely tidy is that the scope boundary was measured, by accident, in
review.

## The comparison, which arrived for free

Round three of `PR #45` re-ran both regions of the same change. The change touches two beans and
both carry evidence in both shapes.

| region | what it is | how it reproduced |
|---|---|---|
| the criteria table in `bean:0069` | 12 `` `cmd`: `` commands across its 8 numbered rows — the region the extractor scans | every one runs and emits the output the cell reports |
| prose in `bean:0069` and `bean:0090` | 7 fenced `cmd:`/`observed:` blocks — the region the extractor does not scan | 3 of the 7 were defective |

Same change, same two files, same author, same sprint, same `bean:0091` transcript conventions
applied throughout. The variable that differs between the two regions is whether a machine ran
the command. **The mechanised region is clean and the unmechanised region is not.** That is a
controlled comparison nobody designed, and it is better evidence for extending the extractor
than any argument about completeness would have been.

The three defects were each of a kind reading does not catch, which is the same property the
extractor's first two catches had:

- **A `sed` whose ranges no longer emit the line quoted beneath them.** `bean:0090` ran
  `sed -n '130,136p;150,153p'` over `bean:0002` and quoted the `| claude-opus-5 | 15 / 75 |`
  row, which falls outside both ranges. The ranges had been narrowed and the output kept. The
  block reads perfectly: the command is well formed, the output is real text from the cited
  file, and only running it shows the two do not correspond.
- **A `grep -n` transcript pasted under a `sed` command.** `bean:0069` carried three
  line-numbered, wrapped lines under `sed -n '168,171p;249,255p'`, which emits neither line
  numbers nor gaps. Again: both halves were true, and only about different commands.
- **One line quoted out of a multi-file result, unmarked.** `bean:0069` ran
  `grep -rn 'once caught\|15/\$75' backoffice/src e2e/tests .beans` and quoted a single hit.
  The command returns many lines across several files, and two of them are the `cmd:` and
  `observed:` lines of the block itself, so the assertion sits inside its own searched corpus
  and its output changes whenever the bean is edited. `[...]` marks elision within a line, not
  omitted lines, so nothing said the rest had been dropped.

None of the three is a wrong conclusion. Every conclusion those blocks support is still true.
What was wrong in each case is the *correspondence between a command and the output printed
beside it* — which is exactly the failure the extractor was built for, and exactly the failure
that is invisible to a reader who reads both halves and finds each plausible.

## The confound, stated rather than buried

The table region is also the region that has had the most review attention, so "mechanised" and
"looked at hardest" are not fully separated here. Two things argue that the mechanism is doing
the work anyway:

- The defects the extractor caught inside the table were specifically the ones **reading could
  not** catch: a `|` that a Markdown cell escapes to `\|`, which is alternation in a basic
  regular expression, and two phrases that wrap across lines in their source files so their
  greps can never match. Both had output pasted beside them and both survived review by eye.
- The three prose defects survived two full review rounds by readers who were, in the same
  sitting, correcting the table.

The honest form: the comparison is observational, the counts are small, and it establishes that
the unmechanised region carried defects of exactly the kind the mechanism exists to catch. It
does not establish a rate.

## What the extension has to handle

Not designed here. These are the constraints the current implementation does not face:

- **Prose contains commands that are quoted, not asserted.** The extractor's region boundary was
  wrong on first use for this exact reason — it anchored on a named heading, prose was added
  above that heading, and it tried to execute an example command from the prose. The extension
  must key on the `cmd:`/`observed:` pair inside a fence, not on anything that looks like a
  command.
- **Not every recorded command is re-runnable where the check runs.** `./gradlew e2eTest` is
  minutes; `git log --all` depends on which refs the checkout fetched, which
  `doc:00-constitution#observed-failing` already records as the way a diff-shaped check silently
  examines nothing. A command that cannot run in the checking environment must be reported as
  *not checked* rather than as passing.
- **What "reproduces" means is stronger in prose than in a cell.** A table cell paraphrases its
  output; a fenced `observed:` block claims to be it, modulo `[...]` elisions. The prose form can
  be compared against the actual output, and that comparison is the one that would have caught
  all three defects above. It is also the harder half to implement.
- **Some blocks are deliberately unrunnable transcripts of a past state** — `bean:0103`'s control
  pinned to `10af4f7` is meant to be re-runnable forever, but a block quoting a pre-fix state is
  not. The convention for marking those does not exist yet and the extension needs one.

Not implemented in this pull request. `PR #45` fixed the three blocks by hand and its contract
does not cover installing a check; where the script belongs and whether it is wired into
`qualityCheck` are the questions `bean:0069` already declined, for the same reason — `tools/` and
`build.gradle.kts` are owned elsewhere this sprint.

## Success criteria and evidence

Evidence is empty by design while this is `todo`: the criteria describe work not yet done, and a
cell filled now would be a plan rather than an observation
(`adr:0005-evidence-lives-in-the-work-item`).

| # | criterion | evidence |
|---|---|---|
| 1 | The extractor reads every `cmd:`/`observed:` block in a bean file, in prose and in table cells alike, and reports which region each came from | |
| 2 | It compares a prose block's `observed:` against the command's real output, not merely that the command produces something, with `[...]` treated as elision | |
| 3 | A command it cannot run where it runs is reported as **not checked**, distinguishable in the output from one that passed (`doc:00-constitution#observed-failing`: a check that examines nothing and a check that passes must not both print OK) | |
| 4 | **Observed failing** against each of the three defects recorded above, replanted from `PR #45`'s pre-fix revision, and observed **not** firing against the fixed revision — see `bean:0105` for why the second half is stated here rather than cited | |
| 5 | Its own region boundary is tested against a bean whose prose quotes an example command it must not execute | |

## Why this is not `bean:0104`

`bean:0104` is about a scripted edit producing no reading of its own result. This is the
opposite direction: a hand-written block that nobody ran. The two meet at the same place — an
artefact whose author never observed its output — and the remedies do not overlap, because
`bean:0104`'s is to read what the script did and this one's is to run what the author wrote.
Recorded separately for that reason.
