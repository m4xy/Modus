---
# modus-0013
title: The work bounded context
status: todo
type: feature
priority: high
created_at: 2026-08-29T00:00:00Z
parent: modus-0011
blocked_by: [modus-0030]
---

# The work bounded context

Why: work items are what Modus manages, and `.beans/` is already the `modus` domain's work
store (`doc:40-durability#append-only-log` §3.1) with no model behind it. Nothing can
attribute a run, a memory or a spend figure until a work item is a domain object.

Success criteria:

- `WorkItem` (root) and `Epic`. `WorkItemCreated`, `WorkItemTransitioned`,
  `WorkItemClosed` published; `ProcessDefinitionChanged` consumed — state machine and
  definition of done are per-domain data, never hardcoded
  (`doc:00-constitution#domain-scoping`).
- The transition guard `doc:00-constitution#evidence-rule` names: a close is refused
  without an evidence record per success criterion.

Blocked by `bean:0012` — `work` consumes `ProcessDefinitionChanged`
(`doc:10-architecture#bounded-contexts` §3.1). Blocks `bean:0014`, `bean:0015`, `bean:0016`.
