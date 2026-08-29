---
# modus-0056
title: Two completed beans record evidence kinds where adr:0005 requires observed output
status: todo
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# Two completed beans record evidence kinds where `adr:0005` requires observed output

`adr:0005-evidence-lives-in-the-work-item#evidence-home` item 1 requires every success
criterion's evidence to be "command, expectation, verbatim observed output", in the bean,
beside the criterion. `bean:0048` and `bean:0052` instead record the **kind** of evidence
that would exist — the bare tokens `citation`, `diff`, `test-run` — in a column headed
`evidence`. A kind is a plan to gather evidence, not evidence.

Both merged **after** `adr:0005` landed, so the rule applied to them:

| bean | merged | ADR in force |
|---|---|---|
| `bean:0038`, which implemented `adr:0005` | `4a8a4db` (#24) | — |
| `bean:0048` | `6347022` (#26) | yes |
| `bean:0052` | `054d96f` (#34) | yes |

Found while closing ten merged beans (pull request #35), by a reviewer checking whether the
beans closed without edits actually satisfied the rule they were being closed under. Stated
with its denominator, because two are in circulation: **seven of the nine** closed on their
existing evidence satisfy item 1, and **eight of the ten** closed in that pull request do,
the eighth being `bean:0045`, which had its evidence authored in the same change. These two
do not, on either count, and the closure asserted they did.

## What is actually wrong, row by row

Not every row is equally bad, and the fix should not pretend otherwise.

**`bean:0048` rows 2–6 — five bare `citation` tokens.** None names a file, an anchor, a
section or a line. A reader cannot follow them to anything; they assert that a citation
could be produced. `.beans/modus-0048--extract-the-first-skills.md:62-66`.
`doc:50-memory-and-evidence#evidence-kinds` §3.2 makes this measurable rather than a matter
of taste: a `citation` carries `path`, `startLine`, `endLine`, `sha` and `quote`. These
carry none of the five. The word is the name of a payload, and the payload is what is
missing.

**`bean:0048` row 7 — bare `diff`.** Names no commit and no hunk.

**`bean:0048` row 1 — `` `ls .claude/skills/*/SKILL.md` ``.** A command with no output. It is
the closest to compliant of the eight and still carries no observation.

**`bean:0048` row 8 and `bean:0052` row 8 — `` `./gradlew qualityCheck` green `` evidenced
as `test-run`.** This is the clearest breach in both beans, and it is the one criterion where
verbatim output is trivially available and universally expected: every other bean in this
repository quotes `BUILD SUCCESSFUL` and the `docs-lint: OK — …` counts line.
`.beans/modus-0048…:68`, `.beans/modus-0052…:55`.

**`bean:0052` rows 1–7 are a weaker finding and may be judged compliant.** They cite named
anchors and sections — `doc:00-constitution#orchestrator`, `§12`, `doc:80…#orchestrating` —
and row 7 carries real figures (`doc:00` 500/500, `doc:80` 432/500). For a criterion whose
subject is "this document now says X", the document is the observation. Deciding whether a
followable citation satisfies item 1 for documentation criteria is part of this bean's work,
not a foregone conclusion.

## Success criteria

- **`bean:0048` and `bean:0052` each gain a `## Amendments` entry**, appended, dated, naming
  this bean, stating what the original claimed, what was found, and the evidence — the shape
  `adr:0005-evidence-lives-in-the-work-item#amendments` requires. The original tables are not
  edited: a reader must see both the kind that was recorded and the observation that was
  missing.
- **The amendment supplies the missing observations where they still exist**, rather than
  restating that they are missing. `qualityCheck` can be re-run on the merge commit; `ls
  .claude/skills/*/SKILL.md` can be re-run; the citations can be resolved to anchors. Where
  an observation cannot be recovered — a plant that was reverted, a run that was not recorded
  — the amendment says so plainly rather than reconstructing it. A reconstructed observation
  is not an observation.
- **A ruling on the `bean:0052` rows 1–7 question**, recorded either way: does a followable
  citation to a named anchor satisfy `adr:0005#evidence-home` item 1 for a criterion whose
  subject is a document's text? If yes, `adr:0005` should say so, because this is the third
  bean to hit the question. If no, those rows are amended too.
- **`docs-lint` check 11 accepts both amendments** — they are the first amendments this
  repository has written, and check 11's amendment schema was itself only ever exercised
  against plants (`bean:0038`). Observed, not assumed.
- `./gradlew qualityCheck` green.

## Deliberately not in scope

- **Editing either bean's original table.** Both are `completed` and frozen; append-only is
  the whole mechanism (`doc:00-constitution#bean-lifecycle`).
- **Auditing every completed bean for the same defect.** `bean:0027` already owns auditing
  `Enforced by:` claims across the package and is the right home for widening this. Two beans
  are the observed instances; a sweep is a different work item with a different cost.
- **Adding a `docs-lint` check that rejects a bare evidence kind.** It is the obvious
  follow-up and it is not free: `citation`, `diff` and `test-run` are the vocabulary
  `doc:50-memory-and-evidence#evidence-kinds` defines, so a check cannot ban the words — it
  would have to decide that a table cell is *only* the token and nothing else. Raise it from what this bean learns,
  with the false-positive rate in hand, rather than guessing at it now.

## Why this is a bean and not a fix in #35

#35 closes ten merged work items. Amending two completed beans is a different change with a
different risk profile — it is the first exercise of the append-only path, against a check
that has never run on a real amendment — and bundling it would put the riskiest edit inside
the most janitorial pull request. That is the same mistake #35's own review found once
already, where 85 lines of authored evidence hid inside nine status flips.
