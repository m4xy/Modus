---
# modus-0056
title: Four completed beans record evidence kinds where adr:0005 requires observed output
status: todo
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# Four completed beans record evidence kinds where `adr:0005` requires observed output

`adr:0005-evidence-lives-in-the-work-item#evidence-home` item 1 requires every success
criterion's evidence to be "command, expectation, verbatim observed output", in the bean,
beside the criterion. Four completed beans instead record the **kind** of evidence that would
exist — the bare tokens `citation`, `diff`, `test-run`. A kind is a plan to gather evidence,
not evidence.

`adr:0005` landed in `b33416a` (#18) and `bean:0038` built its mechanism in `4a8a4db` (#24).
Merge order on `main` is not pull-request-number order, so both are stated:

| bean | evidence table authored | flipped to `completed` | shape |
|---|---|---|---|
| `bean:0030` | `a9d68f0`, before #18 | `6000b46` (#17), **after** #18 | combined table, only extra column headed `evidence kind`; rows 3, 4, 5 and 12 read bare `test-run` |
| `bean:0032` | `b25136f` (#13), before #18 | `6000b46` (#17), **after** #18 | combined table, only extra column headed `evidence kind`; two bare kind cells |
| `bean:0048` | `6347022` (#26), after #24 | same commit | column headed `evidence`; six bare kind cells — `citation` x4, `diff`, `test-run` |
| `bean:0052` | `054d96f` (#34), after #24 | same commit | column headed `evidence`; one bare kind cell — `test-run` |

```
cmd:      git log --format=%h origin/main | grep -n -E '^(6000b46|b33416a|a9d68f0|b25136f)$'
observed: 20:6000b46    <- #17, the closure
          21:b33416a    <- #18, adr:0005
          22:a9d68f0    <- bean:0030's evidence table
          24:b25136f    <- #13, bean:0032's evidence table
exit:     0
```

So for `bean:0030` and `bean:0032` the rule was **not** in force when the tables were written
and **was** in force when they were closed on them. That distinction is the finding, not an
excuse: closing a bean is the act that asserts its evidence satisfies the rule.

## How the four were found, and why the count moved from two to four

`bean:0048` and `bean:0052` were found by hand while closing ten merged beans (#35), by a
reviewer checking whether beans closed without edits satisfied the rule they were being
closed under.

`bean:0030` and `bean:0032` were found mechanically. `bean:0055` built `docs-lint` check 14
and ran its analyser over **every** bean `completed` on `main` — the whole population, not a
sample:

```
cmd:      for each completed bean on origin/main: awk -f <check 14's analyser> <bean>
observed: clean=16 flagged=7 total=23
          modus-0001  NOEV, then UNANSWERED 1..13   — no evidence section at all
          modus-0028  EMPTYEV, then UNANSWERED 2..6 — an evidence section with no entry
          modus-0030  NOEVCOL 'Success criteria and evidence'
          modus-0032  NOEVCOL 'Success criteria and evidence'
          modus-0048  HOLLOW 3 citation; 4 citation; 5 citation; 6 citation; 7 diff;
                      8 test-run
          modus-0051  UNANSWERED 5; UNANSWERED 6
          modus-0052  HOLLOW 8 test-run
```

That is a stronger basis than the hand-reading that found the first two, and it is why the
count is trustworthy at four rather than "at least two": the analyser examined 23 of 23.

`bean:0030` and `bean:0032` escape the `HOLLOW` condition and are caught by `NOEVCOL`
instead, because their kind column is headed `evidence kind` rather than `evidence`, so the
cell is never read as an evidence cell at all. That is the same distinction the ADR draws —
a kind is a plan — expressed as a column name.

`modus-0001` and `modus-0028` are flagged for a different defect: no evidence section, and
an evidence section with no entry. They are not this bean's subject. `modus-0051` is
criteria answered in sections that never name them. Both are recorded here so the run's
denominator is not quoted selectively.

## What this bean is for, restated

**Not "fix two beans". The corpus predates the rule, and this is its true extent.** Four
instances out of twenty-three is a pattern, not two one-offs, and two of the four had their
evidence written before `adr:0005` existed. The work is therefore recording what the corpus
actually contains and amending it where the observation is recoverable — not assigning fault
to individual authors.

`docs-lint` check 14 (`bean:0055`) stops the fifth instance. It cannot reach these four:
check 14 only reads a bean that closes in the change under review, and all four are already
`completed` on `main`. That is deliberate — a rule applies from adoption — and it is exactly
why the backlog has to carry them.

## What is actually wrong, row by row

Not every row is equally bad, and the fix should not pretend otherwise.

**`bean:0030` rows 3, 4, 5 and 12, and `bean:0032`'s two — bare `test-run` and friends under
a column headed `evidence kind`.** `.beans/modus-0030--domainmgmt-domain-aggregate.md:71-73`
and `:80`; `.beans/modus-0032--domain-id-shared-kernel.md`. `bean:0030` row 12 is
`` `./gradlew qualityCheck` is green `` evidenced as `test-run`, which is the same breach as
`bean:0048` row 8 and `bean:0052` row 8. Its other rows carry substantive text in that
column — "test-run, accepting and rejecting case per invariant" — which is a *method*, still
not an observation. Both beans do carry prose evidence elsewhere; the question this bean has
to answer for them is whether a `## Evidence` section that never names a criterion satisfies
"beside the criterion it satisfies".

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

- **All four beans gain a `## Amendments` entry**, appended, dated, naming this bean, stating
  what the original claimed, what was found, and the evidence — the shape
  `adr:0005-evidence-lives-in-the-work-item#amendments` requires. Every one of the four is
  `completed`, so `docs-lint` check 11 makes an append under `## Amendments` **the only route
  that exists**: an in-place correction to any of their tables fails the build. The original
  tables are not edited: a reader must see both the kind that was recorded and the
  observation that was missing.
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
- **A further sweep for the same defect.** The sweep is done: `bean:0055`'s analyser run
  covered 23 of 23 completed beans and four is the whole population, not a sample. What is
  out of scope is the *adjacent* defects the same run surfaced — `modus-0001`'s absent
  evidence section, `modus-0028`'s empty one, `modus-0051`'s unnamed criteria. Those are
  recorded above and belong to `bean:0027`, which owns auditing claims across the package.
- **Adding a `docs-lint` check that rejects a bare evidence kind.** Done, as check 14
  (`bean:0055`): a cell whose every token is a name from
  `doc:50-memory-and-evidence#evidence-kinds` is rejected, and a numbered table in an
  evidence section with no evidence column is rejected outright. It applies to beans closing
  from now on and cannot reach these four.

## Why this is a bean and not a fix in #35

#35 closed ten merged work items. Amending completed beans is a different change with a
different risk profile — it is the first exercise of the append-only path, against a check
that has never run on a real amendment — and bundling it would have put the riskiest edit
inside the most janitorial pull request. That is the same mistake #35's own review found once
already, where 85 lines of authored evidence hid inside nine status flips.
