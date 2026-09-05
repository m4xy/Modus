---
# modus-0013
title: The work bounded context
status: in-progress
type: epic
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

## Split into children, and why

This bean was `type: feature` and is now `type: epic`. Nothing above is edited: the two
criteria are distributed between the children verbatim, and this section records which
child carries which so a reader can see that neither was dropped.

| criterion | child |
|---|---|
| `WorkItem` (root) and `Epic`; `WorkItemCreated`, `WorkItemTransitioned`, `WorkItemClosed` published; the state machine and the definition of done per-domain data | `bean:0152` |
| a close is refused without an evidence record per success criterion | `bean:0152` |
| `ProcessDefinitionChanged` consumed | `bean:0153` |

Two reasons, and the first is not about size.

**`ProcessDefinitionChanged` cannot be consumed on `main`.** A consumer is a use case
(`doc:20-ddd-practices#domain-events` §4.1.4 puts dispatch in the application layer), and the
types a handler is registered against — `DomainEventHandler`, `EventSubscription`,
`SynchronousDomainEventDispatch` — arrive with `bean:0066`, which is open and unmerged. So is
`RaisesDomainEvents`, which every aggregate root now implements, and the `DrainEventsTest`
case a new root has to add. A branch cut from `main` cannot compile against any of them.
Consuming the event, and adopting the drain contract, therefore belong to a bean that is
`blocked_by: [modus-0066]` rather than to one that would have to guess at its shape.

**And the whole is mis-sized for one window** (`doc:00-constitution` §6.2). The domain half
alone is two aggregate roots, five published types, five internal value objects, three
events, a sealed exception hierarchy, two repository ports and the 100% aggregate-branch
floor over all of it.

Children: `bean:0152` (the domain model), `bean:0153` (the application layer and the
`ProcessDefinitionChanged` handler). `bean:0153` is `blocked_by` both `bean:0152` and
`bean:0066`.

Done when both children are `completed`.
