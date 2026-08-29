---
# modus-0016
title: The cost bounded context
status: todo
type: feature
priority: normal
created_at: 2026-08-29T00:00:00Z
parent: modus-0011
blocked_by: [modus-0014]
---

# The cost bounded context

Why: `doc:00-constitution` §11 requires every workflow stage to carry an attributed dollar
figure and `doc:60-cost-model` specifies the scheme; nothing records one. The threshold
table of `doc:60-cost-model#extraction-threshold`, which `doc:00-constitution` §5 defers
to, is measured by this context or by nothing.

Success criteria:

- `SpendLedger` and `CostProfile`, with stage attribution including the first-class
  `overhead` stage (`doc:60-cost-model#stage-attribution`) and the record of
  `#spend-record`.
- Money is integral, never `Float` or `Double` (`doc:20-ddd-practices#value-objects`). The
  price book is data with evidence attached, not a constant table
  (`doc:60-cost-model#price-book`).
- `SpendRecorded`, `BudgetThresholdCrossed` published; `AgentRunCompleted` consumed.

Blocked by `bean:0014` — `cost` consumes `AgentRunCompleted`.
