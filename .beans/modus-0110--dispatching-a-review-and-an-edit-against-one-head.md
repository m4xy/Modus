---
# modus-0110
title: Add the racing-dispatch row to doc:80's orchestration anti-patterns
status: todo
type: fix
priority: normal
created_at: 2026-08-30T00:00:00Z
---

# Add the racing-dispatch row to doc:80's orchestration anti-patterns

`doc:80-agent-operating-procedure#orchestrating` carries a table of orchestration
anti-patterns. It is missing one, observed three times in one sprint.

## The finding

Stated by the orchestrator, and recorded here as theirs rather than as this bean's:

> A review and an edit dispatched against the same head race each other, and the review
> loses. The reviewer's evidence is derived from a tree the author is still moving, so the
> disagreement it reports is real and about nothing.

It belongs beside *choosing a merge order so that a gate does not fire* — both are an
orchestrator's scheduling decision producing a defect no agent it dispatched could have
avoided.

## Why it is a family and not an incident

Three instances this sprint, all committed by the orchestrator, all the same error:

| # | instance | what was treated as static |
|---|---|---|
| 1 | a pull request merged while its author was mid-fix | the branch, while its author held it |
| 2 | one bean id allocated to two sibling branches inside one hour | the id space, while another branch held part of it |
| 3 | a review dispatched at a head the author was still moving | the head, while its author held it |

**The common error is treating a branch as static while another party holds it.** Stated that
way it is one mistake with three surfaces, which is why a row in the anti-patterns table is
the right shape rather than three separate cautions.

## The part with teeth

The third instance was caught **before** it cost a round; the first two only after. The
difference was not a mechanism. It was one party holding every branch in view at once — the
same property that caught the id collision in instance 2, and the same property `bean:0109`
prices as the residual on `bean:0051`.

So the safeguard against all three is a person paying attention. **That does not scale, and it
fails silently** — nothing reports that nobody was watching. Any row added to `doc:80` should
say so rather than implying the anti-pattern is avoided by knowing about it.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `doc:80-agent-operating-procedure#orchestrating`'s anti-patterns table carries the racing-dispatch row, stating what the reviewer's verdict is worth when the head moved under it | citation |
| 2 | The row states that the safeguard is attention rather than mechanism, and that it fails silently — not merely that the pattern should be avoided | citation |
| 3 | `doc:80-agent-operating-procedure` stays inside `docs-lint` check 8's budget, measured at fix time with `wc -l` rather than against a figure carried from here — this bean deliberately states none, because a line count in prose is invalidated by any edit to the file, including one that changes no rule (`bean:0089`) | command |
| 4 | The three instances are cited to where they are recorded rather than restated in the row (`doc:05-authoring-for-agents#one-fact-one-place`) | citation |
