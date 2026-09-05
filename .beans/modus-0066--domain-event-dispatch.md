---
# modus-0066
title: Domain event dispatch — nothing drains pendingEvents
status: in-progress
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
| 1 | Each of `Domain`, `Actor` and `PermissionGrant` gains a drain operation that returns the accumulated events **and** leaves the aggregate with none; draining twice yields the second call an empty list | **MET.** `RaisesDomainEvents.drainEvents()`, implemented by all three. `DrainEventsTest`, 10 tests. Plant A below removes `events.clear()` from all three roots and 9 tests go red across both modules; plant C returns the backing list and 6 more do |
| 2 | The drain returns a copy, so `bean:0036`'s gate stays green and the returned list cannot be mutated back into the aggregate. Asserted with a collection of two or more, per `doc:35-testing#fixture-variation` | **MET as a test; NOT met as a gate, and stated as such.** Plant C. `bean:0036`'s gate is green and **does not examine `drainEvents` at all** — a copy hoisted into a local is invisible to it, observed in `bean:0131`, which carries the fix. The two properties are also not independently plantable, for the reason below |
| 3 | A dispatch port is declared in `core-application` and nothing in `core-domain` references it; `rule:archunit/domainDependsOnNoOuterLayer` and `rule:archunit/domainIsFrameworkFree` stay green | **MET.** `core.application.event.DomainEventDispatchPort`. Both rules green in the `:architecture-tests:test` run below. Plant H: the reference is refused one step earlier than ArchUnit, by the Gradle module boundary |
| 4 | `core-application` remains framework-free: `rule:archunit/applicationDependsOnDomainOnly` and `rule:archunit/applicationIsFreeOfDeliveryConcerns` stay green with the port and handler contract added | **MET.** Both green in the same run. `core-application`'s only dependency is still `api(project(":core-domain"))`; nothing was added to its `build.gradle.kts` |
| 5 | Edge 1 works end to end: revoking a `PermissionGrant` causes `domainmgmt` to observe `GrantRevoked`, through the dispatcher, with no import of `identity`'s aggregates or ports on the consuming side | **MET.** `RevokeGrantUseCaseTest`, real use case, real `WriteThenDispatch`, real `SynchronousDomainEventDispatch`, real `ObserveGrantRevokedUseCase`. Plants D and E below. The consuming side's whole import list is `identity.event.GrantRevoked`, the shared kernel's `DomainId` and `domainmgmt`'s own port |
| 6 | Dispatch happens **after** the write, not before or during. Observed by a test in which the repository write fails and no event is delivered | **MET.** Plant B swaps the two lines in `WriteThenDispatch.write` and 5 tests go red, including the direct ordering assertion |
| 7 | A handler that throws does not silently swallow the event: the behaviour on handler failure is stated and asserted, rather than left to whatever the implementation happens to do | **MET.** Stated in `SynchronousDomainEventDispatch`'s KDoc: propagate, and delivery stops at the failure. Three tests pin both halves; plant F swallows and all three go red |
| 8 | The test doubles are exercised on their own input surface, not only through the verdict: a test asserts which events the dispatcher was **given** separately from what a handler concluded, so a fixture that hands a well-formed event to a handler cannot stand in for testing the code that drains and routes it | **MET.** `RecordingDispatch.calls` records one entry per call, not a flattened stream, so "drained twice, once each" is distinguishable from "drained once, two events". `EventSubscription.deliver` reports its own routing decision. Plant D makes routing report success while delivering nothing, and 10 tests go red |
| 9 | `./gradlew qualityCheck` green | **MET.** Transcript below |

## Verification

Eight plants, each observed failing on the branch and reverted
(`doc:00-constitution#observed-failing`, `doc:35-testing#load-bearing-evidence`). The healthy
run is recorded beside each, because a guard that fires on every input scores identically to
a correct one. Green baseline for every plant: `:core-domain:test` 119 tests, 0 failed;
`:core-application:test` 34 tests, 0 failed; `:architecture-tests:test` 34 tests, 0 failed.

### Plant A — `events.clear()` removed from all three drains

The defect this bean was raised for, reintroduced. `sed -i '' '/^        events.clear()$/d'`
over `Domain.kt`, `Actor.kt`, `PermissionGrant.kt`.

```
> Task :core-application:test FAILED
WriteThenDispatchTest > a second write dispatches only what the second command raised() FAILED
WriteThenDispatchTest > a second write of the same aggregate dispatches nothing() FAILED
    expected:<[]> but was:<[DomainCreated(domainId=DomainId(value=modus-core), …)]>
WriteThenDispatchTest > a write that raised nothing dispatches an empty list rather than skipping the call() FAILED
RevokeGrantUseCaseTest > re-writing the same grant in a second transaction publishes nothing() FAILED
    expected:<[]> but was:<[GrantIssued(grantId=GrantId(value=g1), …), GrantRevoked(grantId=GrantId(value=g1), …)]>
34 tests completed, 4 failed
> Task :core-domain:test FAILED
DrainEventsTest > the list Domain hands over cannot be mutated back into it() FAILED
DrainEventsTest > a second drain of Domain yields nothing() FAILED
DrainEventsTest > a second drain of Actor yields nothing() FAILED
    expected:<[]> but was:<[ActorRegistered(actorId=ActorId(value=alice), kind=HUMAN, occurredAt=2026-08-29T00:00:00Z)]>
DrainEventsTest > the list PermissionGrant hands over cannot be mutated back into it() FAILED
DrainEventsTest > PermissionGrant hands over everything it raised and keeps none of it() FAILED
DrainEventsTest > Actor hands over the one event it raises and keeps none of it() FAILED
DrainEventsTest > Domain hands over everything it raised and keeps none of it() FAILED
DrainEventsTest > a second drain of PermissionGrant yields nothing() FAILED
DrainEventsTest > a command raised after a drain hands over only what it raised() FAILED
119 tests completed, 9 failed
```

Note the reach: the defect is caught in `core-domain`'s own unit tests **and** at the
application layer that would have shipped it. Reverted; both suites green.

### Plant B — dispatch moved before the write in `WriteThenDispatch.write`

```
> Task :core-application:test FAILED
WriteThenDispatchTest > writes before it dispatches() FAILED
    expected:<["save", "dispatch(2)"]> but was:<["dispatch(2)", "save"]>
WriteThenDispatchTest > dispatches nothing when the write fails, and leaves the events on the aggregate() FAILED
RevokeGrantUseCaseTest > a failed write leaves the events on the aggregate for the retry to dispatch() FAILED
RevokeGrantUseCaseTest > a failed write dispatches nothing and reaches no handler() FAILED
    expected:<[]> but was:<[GrantRevoked(grantId=GrantId(value=g1), actorId=ActorId(value=alice), domainId=DomainId(value=modus-core), occurredAt=2026-08-30T00:00:00Z)]>
RevokeGrantUseCaseTest > a handler that refuses surfaces to the caller, with the write already done() FAILED
    expected:<[GrantId(value=g1)]> but was:<[]>
34 tests completed, 5 failed
```

### Plant C — the drain returns the backing list instead of a copy

`val drained: List<DomainEvent> = events` in place of `events.toList()`, in `Domain.kt` and
`PermissionGrant.kt`.

```
> Task :core-domain:test FAILED
DrainEventsTest > the list Domain hands over cannot be mutated back into it() FAILED
    org.opentest4j.AssertionFailedError: expected:<2> but was:<0>
DrainEventsTest > the list PermissionGrant hands over cannot be mutated back into it() FAILED
    org.opentest4j.AssertionFailedError: expected:<2> but was:<0>
DrainEventsTest > PermissionGrant hands over everything it raised and keeps none of it() FAILED
DrainEventsTest > pendingEvents still reads without draining, and the drain still finds them() FAILED
DrainEventsTest > Domain hands over everything it raised and keeps none of it() FAILED
DrainEventsTest > a command raised after a drain hands over only what it raised() FAILED
10 tests completed, 6 failed
```

**Recorded honestly:** the failure the mutation-named tests report is `expected:<2> but
was:<0>` — the size precondition — not the mutation assertion their names describe. The two
properties cannot be planted independently here, and the reason is structural rather than a
gap in the tests: `events` is a `val`, so an implementation that hands out the live list
**cannot also clear it** without emptying what it just handed over. "Returns a copy" and
"leaves the aggregate empty" are one property of this shape, and it fails in the two ways
plants A and C record. `doc:35-testing#load-bearing-evidence` asks for the enabling condition
to be planted as well as the claim; here the enabling condition is all there is to plant.

### Plant D — `EventSubscription.deliver` reports success without calling the handler

The routing half of criterion 8: `accepts(event) ?: return false; return true`.

```
> Task :core-application:test FAILED
RevokeGrantUseCaseTest > revoking a grant reaches domainmgmt's handler through the dispatcher() FAILED
    expected:<[DomainId(value=modus-core)]> but was:<[]>
SynchronousDomainEventDispatchTest > a subscription reports that it accepted the event it is bound to() FAILED
SynchronousDomainEventDispatchTest > delivers one event to every subscription that accepts it() FAILED
SynchronousDomainEventDispatchTest > delivers each event only to the subscriptions that accept it() FAILED
SynchronousDomainEventDispatchTest > delivers events in the order it was given them() FAILED
SynchronousDomainEventDispatchTest > registering a subscription after construction changes nothing() FAILED
SynchronousDomainEventDispatchTest > a handler that throws propagates, and is not swallowed() FAILED
SynchronousDomainEventDispatchTest > delivery stops at the failing handler, and the subscription after it is not reached() FAILED
SynchronousDomainEventDispatchTest > events after the failing one are not delivered() FAILED
RevokeGrantUseCaseTest > a handler that refuses surfaces to the caller, with the write already done() FAILED
34 tests completed, 10 failed
```

### Plant E — `ObserveGrantRevokedUseCase`'s guard inverted

`if (domains.findById(event.domainId) != null) throw …`. Both directions fail, which is the
point: the healthy case and the refusal are separately asserted.

```
> Task :core-application:test FAILED
ObserveGrantRevokedUseCaseTest > resolves the domain the revoked grant names() FAILED
    uk.m4xy.modus.core.application.domainmgmt.usecase.UnknownDomainOnGrantRevoked: GrantRevoked names domain modus-core, which domainmgmt does not hold
ObserveGrantRevokedUseCaseTest > refuses an event naming a domain this context does not hold() FAILED
    org.opentest4j.AssertionFailedError: Expected exception uk.m4xy.modus.core.application.domainmgmt.usecase.UnknownDomainOnGrantRevoked but no exception was thrown.
ObserveGrantRevokedUseCaseTest > looks the domain up once per event, not once per handler construction() FAILED
RevokeGrantUseCaseTest > revoking a grant reaches domainmgmt's handler through the dispatcher() FAILED
RevokeGrantUseCaseTest > a handler that refuses surfaces to the caller, with the write already done() FAILED
34 tests completed, 5 failed
```

### Plant F — the dispatcher swallows a handler failure

`runCatching { subscription.deliver(event) }`.

```
> Task :core-application:test FAILED
SynchronousDomainEventDispatchTest > a handler that throws propagates, and is not swallowed() FAILED
    org.opentest4j.AssertionFailedError: Expected exception uk.m4xy.modus.core.application.HandlerRefused but no exception was thrown.
SynchronousDomainEventDispatchTest > delivery stops at the failing handler, and the subscription after it is not reached() FAILED
SynchronousDomainEventDispatchTest > events after the failing one are not delivered() FAILED
RevokeGrantUseCaseTest > a handler that refuses surfaces to the caller, with the write already done() FAILED
34 tests completed, 4 failed
```

### Plant G — the subscription list not copied on the way in

`= subscriptions` in place of `= subscriptions.toList()`.

```
SynchronousDomainEventDispatchTest > registering a subscription after construction changes nothing() FAILED
    org.opentest4j.AssertionFailedError: expected:<2> but was:<3>
34 tests completed, 1 failed
```

Only one test moves, which is the intended reach: nothing else depends on the copy.

### Plant H — `core-domain` referencing the dispatch port

A file in `core.domain.aggregate` importing `DomainEventDispatchPort`. It is refused one
layer earlier than the ArchUnit rule criterion 3 names, by the Gradle module boundary — which
is the stronger guarantee, and the reason `rule:archunit/domainDependsOnNoOuterLayer` cannot
be *made* to fail on this particular violation.

```
e: …/core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/aggregate/PlantedViolation.kt:3:27 Unresolved reference 'application'.
e: …/core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/aggregate/PlantedViolation.kt:6:28 Unresolved reference 'DomainEventDispatchPort'.
BUILD FAILED in 14s
```

### `config/coverage/baseline.tsv`

```
> Task :coverageBaselineWrite
coverageBaselineWrite: missed instructions, missed branches, covered instructions, covered branches
  :core-application              6 0 0 0 -> 6 0 199 10
  :core-domain                   0 0 1543 130 -> 0 0 1573 130
```

No missed count rises, so no `-Pcoverage.regress` was needed. `:core-application`'s six
missed instructions are unchanged and are still `ListBoundedContexts` and `UseCase`, which
have no test; every line this bean added is covered. `:core-application` also gains the
first branches recorded outside `:core-domain`.

The writer erased the six-line regression-provenance block again, as `bean:0033` describes.
It was restored by hand. **New fact for `bean:0033`:** this run moved two rows *upward* and
the block was destroyed just the same, so the defect is not conditional on the direction of
the write — `bean:0065` had observed it on a run where no figure changed at all.

### `./gradlew qualityCheck`

```
docs-lint-gate-test: 168 passed, 0 failed, over 2 bash major version(s).

> Task :qualityCheck

BUILD SUCCESSFUL in 3m 31s
174 actionable tasks: 21 executed, 19 from cache, 134 up-to-date
```

## What this bean did not do, and why

Three things the bean's own "Where dispatch belongs" table names are **not** in this change.
Each is stated here rather than left to be discovered.

| not done | why |
|---|---|
| The in-process implementation in an adapter or module | It is in `core-application` instead. The port is the seam; the implementation is a `for` loop over a list of application-layer handlers and names no technology, and moving it outward would put the routing of application-layer types outside the application layer while everything it routes to stays inside. A reviewer should check this one specifically: it is a departure from `doc:00-constitution` §1.2's "the adapter implements it", it is flagged in `SynchronousDomainEventDispatch`'s KDoc, and the cost of reversing it is one file move |
| The wiring in `app/modus-server` | There is nothing to wire. `PermissionGrantRepository` and `DomainRepository` have no implementation — `bean:0009` declared both and implemented neither, `bean:0017` builds the flat-file store — so a Spring configuration for edge 1 would bind a handler to a use case that cannot be constructed. It lands with the first real repository |
| Durable, replayable or cross-process delivery, and delivery that survives one handler's failure | Out of scope by the bean's own "Not owned". The failure policy is the one the toolchain leaves reachable: running every handler and reporting failures afterwards needs a broad `catch`, which `TooGenericExceptionCaught` refuses outside two adapters, and the alternative is swallowing, which criterion 7 forbids. Retrying delivery is a property of a durable dispatcher, not of this one |

Two beans were raised rather than absorbed: `bean:0130` (§5.1 has no row for the two packages
this added, and no line budget left to add one) and `bean:0131` (`bean:0036`'s gate does not
examine a copy hoisted into a local, so it does not examine `drainEvents`).

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
