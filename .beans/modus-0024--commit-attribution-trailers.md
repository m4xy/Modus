---
# modus-0024
title: Reconcile the commit attribution trailer rule with the history
status: todo
type: task
priority: low
created_at: 2026-08-29T00:00:00Z
---

# Reconcile the commit attribution trailer rule with the history

Why: `doc:00-constitution` §7.3 states that agent-authored commits MUST end with a
`Co-Authored-By:` trailer naming the model, "enforced by a commit-message check in CI".
The history disagrees and nothing checks it, so the rule is a claim
(`doc:00-constitution#observed-failing`).

```
cmd:      git log --format='%h %(trailers:key=Co-Authored-By,valueonly)'
observed: b1e0809 ... eight commits, all blank ...
          7c0a7e6 Claude Opus 5 (1M context) <noreply@anthropic.com>
```

Already decided, recorded so it is not re-litigated: the root commit `7c0a7e6` keeps its
`Co-Authored-By` and `Claude-Session` trailers — the owner chose to leave them rather than
rewrite the root of the history — and no later commit gains one retrospectively, because a
trailer invented after the fact attributes nothing.

Open: either amend §7.3 to describe what this repository does, or write the check and
apply the rule from the commit that introduces it. Either way the `Enforced by:` line must
name a mechanism observed rejecting a real violation.
