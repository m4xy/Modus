---
# modus-0066
title: Domain event dispatch — nothing drains pendingEvents
status: todo
type: feature
priority: high
order: AQ
created_at: 2026-08-29T00:00:00Z
---

# Domain event dispatch — nothing drains pendingEvents

Three aggregates accumulate domain events into a private `MutableList` and expose them as a
defensive copy:

```
core/core-domain/.../domainmgmt/aggregate/Domain.kt:35
core/core-domain/.../identity/aggregate/Actor.kt:23
core/core-domain/.../identity/aggregate/PermissionGrant.kt:40
    public val pendingEvents: List<DomainEvent> get() = events.toList()
```

`Domain.kt` states the intended contract in one line — *"Raised, not dispatched: the
application layer drains these after the write."* **There is no application layer.**
`core/core-application/src/main` holds two files, `ListBoundedContexts.kt` and a
provisional `UseCase.kt` whose own KDoc says it will be "superseded by the real application
layer in a later work item". Nothing reads `pendingEvents` outside `core-domain`'s own unit
tests.

Two consequences, and the second is the one that will bite:

1. **Every `Consumes` edge in `doc:10-architecture#bounded-contexts` §3 is fiction.** The
   table is a design, not a description. The enumeration is below; it is this bean's value.
2. **There is no drain, only a read.** `pendingEvents` copies out; nothing clears `events`.
   An aggregate re-saved in a second transaction would re-publish everything it has ever
   raised. Unbounded accumulation on one instance holds for `Domain` alone:
   `Domain.adoptProcess` mutates and returns `this` and may be called any number of times,
   whereas `PermissionGrant.revoke` opens with `check(!revoked)` (`PermissionGrant.kt`)
   and so raises at most once — an earlier draft named both and was wrong about the second.
   Re-publication on a second save is the general defect and it applies to all three roots;
   growth without bound is `Domain`'s. The copy-out that `bean:0036` gated is correct and
   stays; what is missing is the operation that says "these have been handed over".

## The seven edges, and the state of each today

`doc:10-architecture#bounded-contexts` §3's `Consumes` column, expanded. `identity` consumes
nothing, so it does not appear as a consumer.

| # | consumer | event | publisher | publisher exists? | consumer exists? | dispatch exists? |
|---|---|---|---|---|---|---|
| 1 | `domainmgmt` | `GrantRevoked` | `identity` | yes — `identity/event/IdentityEvents.kt`, raised at `PermissionGrant.kt` | yes — `Domain` aggregate | **no** |
| 2 | `work` | `ProcessDefinitionChanged` | `domainmgmt` | yes — `domainmgmt/event/DomainMgmtEvents.kt`, raised at `Domain.kt` | no — `work` is `WorkContext`, an object holding `NAME` | **no** |
| 3 | `memory` | `WorkItemClosed` | `work` | no | no | **no** |
| 4 | `memory` | `AgentRunCompleted` | `execution` | no | no | **no** |
| 5 | `execution` | `WorkItemTransitioned` | `work` | no | no | **no** |
| 6 | `execution` | `MemoryRecorded` | `memory` | no | no | **no** |
| 7 | `cost` | `AgentRunCompleted` | `execution` | no | no | **no** |

Edge 1 is the only one whose publisher, event type and consumer aggregate all exist on
`main` today, and it is still unimplemented — `core/core-domain/.../domainmgmt/README.md:16`
says so in as many words: *"`bean:0031` uses it to consume `GrantRevoked`; nothing here does
yet."* Edge 1 is therefore the one this bean can prove itself on end to end; the other six
are blocked on their contexts, not on dispatch.

`work`, `memory`, `execution` and `cost` are each a single marker object holding a `NAME`
constant, whose KDoc says "Delete it as soon as the context has a real aggregate root".

## Why this port is in `core-application` and `bean:0065`'s is in `core-domain`

Neither bean argued the module question when it was written, which is what let them look
consistent while being unexamined. The reconciliation is recorded identically in both.

`doc:20-ddd-practices#ports-and-adapters` §5.2 declares a port **where it is used**. One rule,
two ports, different possible users:

| port | who can use it | module |
|---|---|---|
| this bean's dispatch port | a use case **only** — an aggregate that publishes its own events is exactly the defect this bean exists to prevent | `core-application` |
| `bean:0065`'s `ClockPort`, `IdGeneratorPort`, `RandomPort` | a use case, and in principle an aggregate; and `doc:15-repository-layout#placement-table` §2.1 names "clock, id generator" for `core/core-domain` | `core-domain` |

So the two beans do not disagree about §5.2. They apply it honestly to ports that differ in
who is permitted to call them. Note this is a **narrower** claim than the earlier draft's:
`core-domain` is not barred from holding a publisher interface — `fun publish(events:
List<DomainEvent>)` names only the shared kernel — and §1.1 permits it. §5.2 decides it, and
nothing else needs to.

## Where dispatch belongs

Stated as a conclusion from the layering rules rather than as a preference, because getting
this wrong is how a framework gets into the core.

| layer | role | why not elsewhere |
|---|---|---|
| `core-domain` | **Raises only.** An aggregate appends to its own list and gains a drain operation that hands the accumulated events over and empties the list in one step. It learns nothing about who receives them. | `doc:00-constitution` §1.3 is absolute: no Spring, no static singletons, no service locators. An `ApplicationEventPublisher`, a static bus, or a `DomainEvents.raise(...)` helper are each a straight violation, and `rule:archunit/domainIsFrameworkFree` rejects the first shape today. |
| `core-application` | **Owns the contract and the ordering.** The use case writes through the repository port, then drains and hands over. The dispatch port is declared here — `doc:00-constitution` §1.2 permits a port in `core-application` when it is use-case-shaped, and `doc:20-ddd-practices#ports-and-adapters` §5.2's first rule settles it: "ports are declared where they are **used**, not where they are implemented". The publisher is used by a use case and by nothing in the domain. | Not because `core-domain` is barred from holding it. An earlier draft of this row argued that, and the argument was a non-sequitur: its premise was about the *handler* — which is a use case, so `doc:00-constitution` §1.1 does forbid `core-domain` naming one — while its conclusion was about the *port*. A port `fun publish(events: List<DomainEvent>)` references only the shared kernel and depends on nothing outer, so §1.1 permits it in `core-domain` and the choice rests on §5.2 alone. It may not sit in an adapter: `doc:00-constitution` §1.2 puts ports inside and implementations outside. |
| an adapter or module | **Implements the port.** Synchronous in-process fan-out is enough for the walking skeleton; Spring's own event machinery is one legal implementation and is invisible to both core modules. | `core-application` may not depend on Spring either (`doc:00-constitution` §1.1, row 2). |
| `app/modus-server` | **Wires handlers to events, exactly once.** | Wiring happens in one place by `doc:00-constitution` §1.2. |

Two constraints that fall out and must be criteria, not afterthoughts:

- **Ordering.** `doc:00-constitution` §2.4 makes every write atomic, and edge 1's whole
  point is that a revoked grant is durable before anything reacts to it. Dispatch happens
  **after** the write commits, mirroring `doc:15-repository-layout#cross-cutting-flows`
  §6.1, where the append to the durable log precedes the fan-out.
- **Published language only.** A handler for edge 1 imports `identity`'s
  `..domain.event..` package and nothing else; `doc:10-architecture#bounded-contexts` §3.1
  makes that package a leaf and forbids reaching an aggregate, a port or a use case across
  the boundary. The dispatcher must not become the hole through which a context reaches
  another context's internals.

## What "end to end" means here, and why this bean is selectable today

Criteria 5 and 6 name a repository write, and no repository is implemented anywhere —
`bean:0009` declared two ports and implemented neither, and `bean:0017` is the bean that
builds the flat-file store. So the question has to be answered before the criteria can be
read: **hand-written in-memory fakes, not a real adapter.**

That is the documented level for this test, not a concession. `doc:15-repository-layout` §8
puts use-case tests in `core/core-application/src/test` against "domain plus in-memory fakes
of ports, no Spring context", and rules out a mocking framework in `core/` outright. A fake
repository that throws is exactly how criterion 6 observes a failed write, and it observes it
better than a real store would: it fails on demand, deterministically, where making a real
flat-file write fail requires arranging a filesystem condition.

**Consequence: this bean carries no `blocked_by`.** It is selectable now. What it cannot do
is prove that dispatch survives a *real* durable write; that assertion belongs to the first
bean that owns both halves, and is `bean:0017`'s integration suite rather than this one's.
The bean is honest about which of the two it has evidenced.

## Scope

Owned: the drain operation on the three existing aggregates in `core/core-domain`, the
dispatch port and its handler contract in `core/core-application`, one in-process
implementation, and the wiring for edge 1. This bean.

Not owned: `work`, `memory`, `execution` and `cost` — edges 2 to 7 land with their contexts
(`bean:0013`, `bean:0015`, `bean:0014`, `bean:0016`), each of which then implements its own
handlers against the mechanism this bean supplies. Durable, replayable, or cross-process
delivery: the skeleton needs synchronous in-process dispatch and nothing more, and choosing
an event log or a broker before there is a consumer is a decision without evidence. The
ArchUnit rule that would stop a handler importing another context's internals is
`bean:0023`.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | Each of `Domain`, `Actor` and `PermissionGrant` gains a drain operation that returns the accumulated events **and** leaves the aggregate with none; draining twice yields the second call an empty list | |
| 2 | The drain returns a copy, so `bean:0036`'s gate stays green and the returned list cannot be mutated back into the aggregate. Asserted with a collection of two or more, per `doc:35-testing#fixture-variation` | |
| 3 | A dispatch port is declared in `core-application` and nothing in `core-domain` references it; `rule:archunit/domainDependsOnNoOuterLayer` and `rule:archunit/domainIsFrameworkFree` stay green | |
| 4 | `core-application` remains framework-free: `rule:archunit/applicationDependsOnDomainOnly` and `rule:archunit/applicationIsFreeOfDeliveryConcerns` stay green with the port and handler contract added | |
| 5 | Edge 1 works end to end: revoking a `PermissionGrant` causes `domainmgmt` to observe `GrantRevoked`, through the dispatcher, with no import of `identity`'s aggregates or ports on the consuming side | |
| 6 | Dispatch happens **after** the write, not before or during. Observed by a test in which the repository write fails and no event is delivered | |
| 7 | A handler that throws does not silently swallow the event: the behaviour on handler failure is stated and asserted, rather than left to whatever the implementation happens to do | |
| 8 | The test doubles are exercised on their own input surface, not only through the verdict: a test asserts which events the dispatcher was **given** separately from what a handler concluded, so a fixture that hands a well-formed event to a handler cannot stand in for testing the code that drains and routes it | |
| 9 | `./gradlew qualityCheck` green | |

## Sequencing

**This lands before `bean:0016`.** `cost` consuming `AgentRunCompleted` is edge 7 in the
table above — the one edge whose only reason to exist is cross-context dispatch. `cost`
publishes `SpendRecorded` and `BudgetThresholdCrossed` and consumes nothing else;
`doc:15-repository-layout#cross-cutting-flows` §6.1 ends `on exit: AgentRunCompleted →
cost.SpendRecorded`, which is that edge and no other mechanism. Building `cost` first would
mean building a context whose sole input arrives by a route that does not exist.

It also lands before `bean:0031`, which `domainmgmt/README.md:16` already records as the
consumer of edge 1, and before `bean:0014`, whose `execution` context sits on both sides of
edges 4, 5, 6 and 7.

It does **not** block `bean:0013`. `work` consumes `ProcessDefinitionChanged` (edge 2), but
`WorkItem` can be modelled and read from disk without ever receiving one; the edge is how a
process change reaches existing work items, which is a behaviour `work` can gain afterwards.
