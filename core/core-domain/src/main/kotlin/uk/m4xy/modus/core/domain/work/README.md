# Bounded context: `work`

Work items — the units of work Modus tracks, their lifecycle, and the relationships between
them. `.beans/` is this repository's own instance of that store
(`doc:00-constitution#workflow` §7.2, `doc:40-durability` §3.1); `WorkItem` is the model
behind it.

Package layout follows `doc:20-ddd-practices#ports-and-adapters` §5.1: `published`
(`WorkItemId`, `EpicId`, `SuccessCriterionId`, `WorkItemState`, `WorkItemTitle`), `event`,
`aggregate`, `port`, and the context root for what is not published — `SuccessCriterion`,
`EvidenceRecord`, `EvidenceKind`, `EvidenceReference` and the sealed `WorkException`.

`DomainId` is not declared here. It is shared kernel, beside `DomainEvent`
(`adr:0004-domain-id-shared-kernel`).

## The state machine is not in this package

`doc:00-constitution#domain-scoping` says every domain defines its own work-item states and
its own definition of done, and that code MUST NOT hardcode a single process. So there is no
enum here, no status constant, and no state literal in `src/main`. Every command that
depends on how work moves takes `domainmgmt`'s `ProcessDefinition` as a parameter:

- `WorkItem.create` takes the initial state from the process, so an item cannot begin
  anywhere but where its domain says work begins.
- `WorkItem.transitionTo` asks `process.allows(from, to)`, and nothing else.
- what counts as *closed* is `process.isTerminal(state)` — per domain, so `done` in one
  domain is a legal intermediate state in another.

`WorkItemState` is a validated name, not a member of a set this context knows. It exists as
a second type beside `domainmgmt`'s `StateName` because it appears in this context's events,
which publishes it, and a published package is a leaf: it may not name another context's.
The mapping runs in `WorkItem`'s internals, where importing `domainmgmt`'s published
language is permitted, and it is total only while the two invariants agree —
`WorkItemStateMatchesStateNameTest` drives one corpus through both types rather than
comparing the two regexes.

## The evidence guard

`doc:00-constitution#evidence-rule` refuses a work-item transition to done without evidence
attached. `WorkItem.transitionTo` is the **only** writer of the state, and it applies the
guard whenever the target is terminal, so there is no second method that reaches a terminal
state without passing it. A refused close names every criterion carrying no evidence, not
the first one found.

The rule is per criterion. An item with three criteria and three records against one of them
is unmet twice over; an item with no criteria closes with no evidence, because zero
criteria is zero records owed.

## What this context imports from outside itself

`domainmgmt`'s published language — `ProcessDefinition`, `StateName` — and the shared
kernel. `doc:10-architecture#bounded-contexts` §3.1 also permits `identity`'s published
language; nothing here uses it, and no **event** here could, because a published package is
a leaf. `bean:0154` carries that, and the fact that §4.1's worked example of
`WorkItemTransitioned` names `ActorId` and therefore cannot be built.

`memory`, `execution` and `cost` are forbidden in any form, which is why `EvidenceRecord`
here is this context's own and not `memory`'s.

## Not built yet

- Consuming `ProcessDefinitionChanged`, and every use case: `bean:0153`.
- `RaisesDomainEvents` — `WorkItem.drainEvents` already has the contract's body and
  signature; adopting the interface needs `bean:0066` merged, and is `bean:0153`.
- Persistence: `bean:0017`. A REST surface: `bean:0018`.
- `Epic` carries no state and raises no event. `bean:0013` records why, and what would have
  to be decided before it does.
