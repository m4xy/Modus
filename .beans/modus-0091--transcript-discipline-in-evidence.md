---
# modus-0091
title: Transcript discipline — mark elisions, and write commands that run
status: todo
type: task
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# Transcript discipline — mark elisions, and write commands that run

`adr:0005-evidence-lives-in-the-work-item#evidence-home` requires the command, the
expectation and the verbatim observed output beside each criterion. It says nothing about
**how faithful the transcript has to be**, and the review of the four closures in PR #47
found three ways a transcript can satisfy the rule as written while misleading a reader.
None of the three changed a conclusion. All three were found by a human-shaped reviewer
re-running commands, and none is visible to `docs-lint`.

## The four shapes, as observed

Each was found in a bean closing in PR #47 and fixed in that same pull request. They are
recorded here as the general shape, not to re-litigate those four beans.

| # | shape | instance |
|---|---|---|
| 1 | an unmarked elision, in a bean that promises elisions are marked `[...]` or `[same]` | `bean:0036` quoted three lines of a four-line `grep`, silently dropping the backing field the accessor copies from; `bean:0058` trimmed two lines from a `gh auth status` transcript; `bean:0036` showed four rows of an eight-row check rollup |
| 2 | a command that cannot be run as written | `grep -n "…" GrantIssuedTest.kt` — a bare filename, exit 2 from the repository root — and paths elided to `core/core-domain/.../BoundedContexts.kt` |
| 3 | a command that does not answer the criterion **as worded** | a criterion reading `./gradlew ktlintFormat && ./gradlew qualityCheck` answered by a cell showing `qualityCheck` alone |
| 4 | a **line-number citation in prose**, which rots | `bean:0065` cited `ArchitectureRulesTest.kt:258` and `:334`; the same branch's own edits moved those symbols to `309` and `492`. It also cited `PermissionGrant.kt:70` for a line at `69` — an off-by-one introduced while correcting an off-by-one |

Shape 4 was found in the sharpest possible way: **not by a rebase, and not by another
agent — by the citing branch's own edit to the file it was citing, inside one session.** The
author rewrote `ArchitectureRulesTest.kt`, and every line number that bean had recorded about
it silently became wrong. There is no interval short enough to make a line number safe, which
is why the remedy is not "re-check them at merge" but "do not write them": the symbol name and
the quoted sentence locate the claim exactly as well and cannot drift.

**The prose/fence distinction is the whole of the rule, and it points opposite ways.** In
prose, `file.kt:258` is a *citation* — it asserts where something is now, and it rots. Inside
a fenced `observed:` block, `(PlantedProbe.kt:0)` is *output* — it is what the tool printed,
and editing it to stay current would be falsifying a transcript. So line numbers must be
stripped from one and preserved in the other, and a single find-and-replace across a bean gets
one of the two wrong. That is the same asymmetry shape 1 turns on: **a tidied transcript is no
longer a transcript.**

Shape 1 is the one worth the bean. Shapes 2 and 3 make a reader do work; shape 1 makes a
reader trust a quotation that is not the output. In every instance above the dropped lines
supported the claim, which is exactly why it is worth a rule: **from the outside, an elision
that helps and an elision that hides are the same edit.**

## No mechanism is proposed, and that is the finding

`docs-lint` cannot tell an elided transcript from a complete one. It has the bean and nothing
else — not the command, not a shell, not the tree the command ran against — so the only thing
it could check is whether a fence *says* it elided something, which an author who elides
silently will not write. Shape 2 is closer to decidable and still not decidable: a fenced
`cmd:` line is prose, the elisions in it are deliberate (`<the project dir>`), and running
arbitrary quoted commands from a linter is not something this repository should build.

So this bean's likely honest outcome is **a stated rule with no `Enforced by:` line and a
named enforcement gap**, which `doc:00-constitution#observed-failing` prefers to a gate that
cannot fail. Deciding that is this bean's work; a mechanism must not be assumed into it.

The one mechanical possibility worth *testing* before it is dismissed: an elision marker is
already a convention (`[...]`, `[same]`), so a check could require that a fence containing
`…` or `...` inside an `observed:` block also carries a marker. That catches typography, not
honesty, and it would fire on every ordinary ellipsis in prose output. Measure the false
positive rate over the corpus before building it, or state the reason for not building it.

## Re-run the plants after every correction, including prose-only ones

The rule above is about writing a transcript. This one is about keeping it true, and it was
learned the expensive way inside PR #47 itself.

Correcting the elisions in `modus-0058` added six words of ordinary prose — "which is what
criterion 6 reads" — to a sentence introducing a fence. `docs-lint` check 14 answers a
criterion by a citation of its number anywhere outside a fence, so that sentence silently took
over the job of an evidence row: with it present, **deleting the evidence row for criterion 6
is no longer detected**. The plant that had failed before the correction passed after it.
Nothing about the check changed. The gate stayed green throughout, and the review that had
already passed could not have caught it, because the words were not there when it ran.

It surfaced only because every plant was re-run against the corrected tree rather than assumed
to be unaffected by a prose edit. That assumption is the defect:

> **Prose is not inert.** A transcript's validity depends on the whole file around it, and in
> this repository a sentence mentioning a criterion number is load-bearing. Re-run every
> plant after every correction, including corrections that touch only prose, and re-take any
> transcript whose tree has changed.

The mechanism — a matcher that cannot tell a citation from a mention — is **not** this bean's.
It is owned by the bean the `docs-lint` owner is raising on the citation matcher, id 0093,
opened as PR #56 and not yet on `main`; `bean:0061` records the same mechanism today from the
numbering side. What is new and belongs on the record here is the **route**: every prior
instance in this repository was planted deliberately, and this one was **observed in the
wild** — introduced accidentally, by an author correcting an unrelated finding, in a live
closure, after review had passed. A defect reachable that way is not one authors can be asked
to avoid by being careful.

(That id is written bare rather than as a typed reference on purpose: `docs-lint` check 6
resolves a typed `bean:` reference against the beans on disk, so citing a sibling's unmerged
bean in the normal form fails the build. Observed here, and again on the junk-cell bean below.)

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | The rule is stated once, in the document that owns evidence records, and names all four shapes | citation |
| 2 | The elision-marker convention is stated as a rule rather than as a promise individual beans make to themselves | citation |
| 3 | Either a mechanical check exists and is observed rejecting a planted violation, or the enforcement gap is stated with the reason it is not buildable | test-run, or a stated exemption |
| 4 | If a check is built, its false-positive rate over the completed beans on `main` is measured before it is turned on | test-run over the corpus |
| 5 | The re-run rule is stated where an implementing agent reads it, not only here: a correction that touches only prose still invalidates every plant in the file | citation |
| 6 | `./gradlew qualityCheck` green | test-run |

Criterion 3 is satisfiable by acting **or** by refusing with a reason on the record. A refusal
closes it; silence does not.

## Not in scope

- The four beans closed in PR #47. All three shapes are fixed there, and those beans are
  `completed` and frozen by `docs-lint` check 11.
- What check 14 can and cannot see in a closing bean — `bean:0061`, and the junk-cell finding
  (a closing table whose every cell reads `TODO` or `n/a` still reports every criterion
  checked), which is raised in a bean not yet on `main` and is owned there. This bean is
  about the faithfulness of a transcript a human reads, not about what a gate reads.
- The stale-figure problem in generated blocks, which is `bean:0059`.
