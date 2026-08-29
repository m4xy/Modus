---
# modus-0037
title: Record the stacked-pull-request merge procedure
status: todo
type: task
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# Record the stacked-pull-request merge procedure

`doc:00-constitution#workflow` §7.2.7 says "squash merge" and stops there. It does not
cover a stack, and a stack is what `doc:00-constitution#context-budget` §6.2's
split-the-work-item rule produces — `bean:0012` split into `bean:0030` and `bean:0031`
precisely because one bean was too large, which is the ordinary case, not an exotic one.

Observed: merging `bean:0032`'s pull request with `--delete-branch` removed the branch that
`bean:0012`'s pull request was based on. GitHub closes a pull request when its base branch
is deleted, and a closed pull request can be neither reopened without its base nor
retargeted, so it could not be recovered in place — it had to be rebased onto `main` and
reopened under a new number, losing its review thread continuity.

```
observed: #14 state=CLOSED mergedAt=null mergeCommit=none
          #14 base=refactor/domain-id-shared-kernel mergeable=CONFLICTING
          git show origin/main:.beans/modus-0030--domainmgmt-domain-aggregate.md
            -> fatal: path does not exist in 'origin/main'
```

Nothing was lost but time; the review record survives on the closed pull request and the
commits were re-pushed. It will recur on every stack until it is written down.

## Success criteria

- `doc:00-constitution` §7.2 states the rule: **a pull request that is the base of another
  is merged without `--delete-branch`**, and its branch is deleted only once no open pull
  request targets it.
- The procedure covers what to do after each merge in a stack: the child's base branch has
  moved, so the child is rebased onto the new `main` with the parent's now-squashed commits
  dropped (`git rebase --onto origin/main <parent-tip> <child>`), force-pushed, and its base
  retargeted **before** the parent's branch is deleted.
- Decide and record whether Modus prefers stacked pull requests at all, or whether a split
  bean's children should each branch from `main` and merge independently. Stacking gives
  each child a reviewable diff against its parent; independent branches give each one a
  mergeable diff against `main` and no ordering hazard. `bean:0012`'s two children were
  genuinely sequential — `bean:0031` builds on `bean:0030`'s aggregate — so this is a real
  trade rather than a style question.
- `Enforcement gap:` this is a procedure, not a mechanism. If it can be checked, the check
  belongs in the same place `bean:0035` puts the bean-graph checks.
