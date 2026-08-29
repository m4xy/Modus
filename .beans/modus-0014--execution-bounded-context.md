---
# modus-0014
title: The execution bounded context
status: todo
type: feature
priority: normal
created_at: 2026-08-29T00:00:00Z
parent: modus-0011
blocked_by: [modus-0013]
---

# The execution bounded context

Why: triggers and agent runs are the product's verb. Three `Enforcement gap:` lines in
`doc:00-constitution` — questions asked per work item, peak context per run, and the 240k
at-risk flag of `doc:00-constitution#context-budget` — name this context as the recorder
and cannot close until it exists.

Success criteria:

- `Trigger` and `AgentRun`. `AgentRunStarted`, `AgentRunOutput`, `AgentRunCompleted`,
  `ContextBudgetExceeded` published; `WorkItemTransitioned` and `MemoryRecorded` consumed.
- Context-budget accounting is domain state, so the ceiling is evaluated in the model, not
  in a transport or a script. Token counts are a value object, never a raw `Long`.
- The run output stream is a port here and an adapter in `bean:0021`.

`bean:0015` is not a blocking edge either way: §3.1 states the mutual published-language
cycle is intentional. Blocks `bean:0016`, `bean:0020`.
