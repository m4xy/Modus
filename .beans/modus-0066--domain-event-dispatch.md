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

### The adapter row was right, and the first attempt at this bean departed from it

Recorded because the departure reached review rather than being caught in writing. The
implementation was first placed in `core-application`, on the argument that a `for` loop over
application-layer handlers names no technology and that moving it outward would put routing
of application-layer types outside the application layer.

**Both halves of that argument are wrong**, and PR 83's review ruled so.

- The second half inverts the sanctioned direction. An adapter naming and invoking
  application-layer types is what `doc:20-ddd-practices#ports-and-adapters` §5.1's
  `<Noun>Controller` in `adapter.rest.<ctx>` does for its whole existence; if the argument
  held, a REST controller could not call a use case. `doc:10-architecture#module-system`
  §7.2's "adapter ports is not a thing that exists" cuts the other way — it fixes the *port*
  inside and says nothing about implementations.
- The first half is refuted by a rule neither the bean nor the first review cited:
  `doc:20-ddd-practices#domain-events` §4.1.7 requires every event to be appended to the
  durable event log **before any handler runs**. A conforming dispatcher therefore touches
  durable storage. Today's synchronous fan-out is not a differently-named concern that
  happens to share a seam with the durable one — it is the same concern with the durable half
  missing (`bean:0160`). Renaming it would have forfeited the seam.

So `InProcessDomainEventDispatch` lives in `adapters/adapter-events-inprocess`, the port stays
in `core-application`, and `doc:15-repository-layout#placement-table` §2.1 carries the row and
the rationale — which that section mandates in the same pull request, and which the first
attempt skipped.

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
| 1 | Each of `Domain`, `Actor` and `PermissionGrant` gains a drain operation that returns the accumulated events **and** leaves the aggregate with none; draining twice yields the second call an empty list | **MET.** `RaisesDomainEvents.drainEvents()`, implemented by all three. `DrainEventsTest`, 10 tests. Plant A removes `events.clear()` from all three roots and 14 tests go red across three modules; plant C returns the backing list and 15 more do |
| 2 | The drain returns a copy, so `bean:0036`'s gate stays green and the returned list cannot be mutated back into the aggregate. Asserted with a collection of two or more, per `doc:35-testing#fixture-variation` | **MET.** Plant C — with the `clear()` kept, so it isolates the copy — turns 6 `core-domain` tests red at two-or-more, and plant A turns both mutation-named tests red. `bean:0036`'s gate is green. *Note, not a qualification:* that gate does not examine `drainEvents` at all — a copy hoisted into a local is invisible to it, observed both ways in `bean:0131`, which carries the fix. One assertion inside the two mutation-named tests is also unreachable under either plant; the Verification section states which and why |
| 3 | A dispatch port is declared in `core-application` and nothing in `core-domain` references it; `rule:archunit/domainDependsOnNoOuterLayer` and `rule:archunit/domainIsFrameworkFree` stay green | **MET.** `core.application.event.DomainEventDispatchPort`, which stayed in `core-application` when its implementation moved out (`doc:10-architecture#module-system` §7.2). Both rules green in the 63-test `:architecture-tests:test` run. Plant H: the reference is refused one step earlier than ArchUnit, by the Gradle module boundary |
| 4 | `core-application` remains framework-free: `rule:archunit/applicationDependsOnDomainOnly` and `rule:archunit/applicationIsFreeOfDeliveryConcerns` stay green with the port and handler contract added | **MET.** Both green in the same run. `core-application`'s only dependency is still `api(project(":core-domain"))`; nothing was added to its `build.gradle.kts`, and the new adapter depends on it rather than the other way round |
| 5 | Edge 1 works end to end: revoking a `PermissionGrant` causes `domainmgmt` to observe `GrantRevoked`, through the dispatcher, with no import of `identity`'s aggregates or ports on the consuming side | **MET.** `adapter-events-inprocess`'s `GrantRevokedEdgeTest`: real use case, real `WriteThenDispatch`, real `InProcessDomainEventDispatch`, real `ObserveGrantRevokedUseCase`. Plants D and E. The consuming side's whole import list is `identity.event.GrantRevoked`, the shared kernel's `DomainId` and `domainmgmt`'s own port |
| 6 | Dispatch happens **after** the write, not before or during. Observed by a test in which the repository write fails and no event is delivered | **MET.** Plant B swaps the two lines in `WriteThenDispatch.write` and 6 tests go red across two modules, including the direct ordering assertion and the edge test's "reaches no handler at all" |
| 7 | A handler that throws does not silently swallow the event: the behaviour on handler failure is stated and asserted, rather than left to whatever the implementation happens to do | **MET.** Stated in `doc:20-ddd-practices#domain-events` §4.1.8 rather than only in a KDoc, because the four contexts that inherit it will read §4.1: propagate, delivery stops, and the undelivered suffix is **lost permanently** for want of §4.1.7's log. Four tests pin it; plant F swallows and all four go red |
| 8 | The test doubles are exercised on their own input surface, not only through the verdict: a test asserts which events the dispatcher was **given** separately from what a handler concluded, so a fixture that hands a well-formed event to a handler cannot stand in for testing the code that drains and routes it | **MET, and now structurally.** The two subjects are in different Gradle modules: `RevokeGrantUseCaseTest` can only see what the dispatcher was handed, because `core-application` may not reach an adapter. `RecordingDispatch.calls` records one entry per call, not a flattened stream, so "drained twice, once each" is distinguishable from "drained once, two events"; `EventSubscription.deliver` reports its own routing decision. Plant D makes routing report success while delivering nothing, and 13 tests go red |
| 9 | `./gradlew qualityCheck` green | **MET.** Transcript below |

## Verification

Eight plants, each observed failing and reverted (`doc:00-constitution#observed-failing`,
`doc:35-testing#load-bearing-evidence`). The healthy run is recorded beside each, because a
guard that fires on every input scores identically to a correct one.

**Every transcript below is an unfiltered `--rerun-tasks` run of whole modules.** An earlier
revision of this bean recorded two filtered runs as if they were full ones — plant C's "10
tests completed" was `DrainEventsTest`'s own count against a module of 119, and
`:architecture-tests` was stated as 34, which is `DefensiveCopySourceTest`'s count against a
module of 63. A filtered green baseline does not establish that the rest of the suite was
green, which is the whole load a baseline bears. Corrected here and in `bean:0131`; the
`core-application` figures were never wrong.

Green baseline for every plant, measured from the JUnit XML after
`--rerun-tasks`: `:core-domain:test` **119**, `:core-application:test` **24**,
`:adapter-events-inprocess:test` **14**, `:architecture-tests:test` **63**, all 0 failed.

All eight were re-run after the PR 83 review moved `InProcessDomainEventDispatch` into its
own module, so every transcript is against the code as shipped rather than against the
arrangement it replaced.

### Plant A — `events.clear()` removed from all three drains

The defect this bean was raised for, reintroduced. `sed -i '' '/^        events.clear()$/d'`
over `Domain.kt`, `Actor.kt`, `PermissionGrant.kt`.

```
> Task :core-domain:test FAILED
DrainEventsTest > the list Domain hands over cannot be mutated back into it() FAILED
DrainEventsTest > a second drain of Domain yields nothing() FAILED
DrainEventsTest > a second drain of Actor yields nothing() FAILED
DrainEventsTest > the list PermissionGrant hands over cannot be mutated back into it() FAILED
DrainEventsTest > PermissionGrant hands over everything it raised and keeps none of it() FAILED
DrainEventsTest > Actor hands over the one event it raises and keeps none of it() FAILED
DrainEventsTest > Domain hands over everything it raised and keeps none of it() FAILED
DrainEventsTest > a second drain of PermissionGrant yields nothing() FAILED
DrainEventsTest > a command raised after a drain hands over only what it raised() FAILED
119 tests completed, 9 failed
> Task :core-application:test FAILED
WriteThenDispatchTest > a second write dispatches only what the second command raised() FAILED
WriteThenDispatchTest > a second write of the same aggregate dispatches nothing() FAILED
WriteThenDispatchTest > a write that raised nothing dispatches an empty list rather than skipping the call() FAILED
RevokeGrantUseCaseTest > re-writing the same grant in a second transaction publishes nothing() FAILED
24 tests completed, 4 failed
> Task :adapter-events-inprocess:test FAILED
GrantRevokedEdgeTest > re-writing the same grant in a second transaction reaches no handler() FAILED
14 tests completed, 1 failed
```

The assertion behind the edge test, verbatim:

```
org.opentest4j.AssertionFailedError: Unexpected elements from index 1
expected:<[DomainId(value=modus-core)]> but was:<[DomainId(value=modus-core), DomainId(value=modus-core)]>
```

That is `domainmgmt` being told twice about one revocation. Note the reach: the defect is
caught in `core-domain`'s own unit tests, at the application layer that would have shipped
it, **and** at the consumer.

### Plant B — dispatch moved before the write in `WriteThenDispatch.write`

```
> Task :core-application:test FAILED
WriteThenDispatchTest > writes before it dispatches() FAILED
    expected:<["save", "dispatch(2)"]> but was:<["dispatch(2)", "save"]>
WriteThenDispatchTest > dispatches nothing when the write fails, and leaves the events on the aggregate() FAILED
RevokeGrantUseCaseTest > a failed write leaves the events on the aggregate for the retry to dispatch() FAILED
RevokeGrantUseCaseTest > a failed write dispatches nothing() FAILED
24 tests completed, 4 failed
> Task :adapter-events-inprocess:test FAILED
GrantRevokedEdgeTest > a failed write reaches no handler at all() FAILED
GrantRevokedEdgeTest > a handler that refuses surfaces to the caller, with the write already done() FAILED
14 tests completed, 2 failed
```

### Plant C — the drain returns the backing list instead of a copy

`val drained: List<DomainEvent> = events` in place of `events.toList()`, in `Domain.kt` and
`PermissionGrant.kt`. The `clear()` stays, which is what isolates this plant from plant A.

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
119 tests completed, 6 failed
> Task :core-application:test FAILED
WriteThenDispatchTest > hands over exactly what the aggregate raised, in the order it raised it() FAILED
    org.opentest4j.AssertionFailedError: expected:<2> but was:<0>
WriteThenDispatchTest > a second write of the same aggregate dispatches nothing() FAILED
WriteThenDispatchTest > a second write dispatches only what the second command raised() FAILED
WriteThenDispatchTest > writes before it dispatches() FAILED
WriteThenDispatchTest > the events retried after a failed write are dispatched by the write that succeeds() FAILED
RevokeGrantUseCaseTest > picks the named grant out of the several an actor holds on one domain() FAILED
24 tests completed, 6 failed
> Task :adapter-events-inprocess:test FAILED
GrantRevokedEdgeTest > revoking a grant reaches domainmgmt's handler through the dispatcher() FAILED
GrantRevokedEdgeTest > re-writing the same grant in a second transaction reaches no handler() FAILED
GrantRevokedEdgeTest > a handler that refuses surfaces to the caller, with the write already done() FAILED
14 tests completed, 3 failed
```

**One recorded limitation, stated narrowly.** The two mutation-named tests fail here on the
size precondition — `expected:<2> but was:<0>` — not on the mutation assertion their names
describe. The reason is specific: clearing a list you have just handed out empties the list
the assertion would have mutated, so under this plant the assertion is never reached. **That
assertion has therefore never been observed failing for its stated reason.**

An earlier revision of this bean generalised that into a claim that the copy and the
emptying "are not independently plantable". That is wrong, and these transcripts disprove it
two sections apart: plant A violates only the emptying — the copy survives — and turns both
mutation-named tests red; plant C keeps the `clear()` and violates only the copy. Each
property is separately plantable. What is not separately reachable is that one assertion.

### Plant D — `EventSubscription.deliver` reports success without calling the handler

The routing half of criterion 8: `accepts(event) ?: return false; return true`.

```
> Task :core-application:test FAILED
EventSubscriptionTest > reports that it accepted the event it is bound to, and delivers it() FAILED
EventSubscriptionTest > a selector that accepts everything delivers every kind of event() FAILED
EventSubscriptionTest > a handler that throws propagates out of deliver() FAILED
24 tests completed, 3 failed
> Task :adapter-events-inprocess:test FAILED
InProcessDomainEventDispatchTest > delivers each event only to the subscriptions that accept it() FAILED
InProcessDomainEventDispatchTest > delivers one event to every subscription that accepts it() FAILED
InProcessDomainEventDispatchTest > delivers events in the order it was given them() FAILED
InProcessDomainEventDispatchTest > registering a subscription after construction changes nothing() FAILED
InProcessDomainEventDispatchTest > a handler that throws propagates, and is not swallowed() FAILED
InProcessDomainEventDispatchTest > delivery stops at the failing handler, and the subscription after it is not reached() FAILED
InProcessDomainEventDispatchTest > events after the failing one are not delivered() FAILED
GrantRevokedEdgeTest > revoking a grant reaches domainmgmt's handler through the dispatcher() FAILED
GrantRevokedEdgeTest > re-writing the same grant in a second transaction reaches no handler() FAILED
GrantRevokedEdgeTest > a handler that refuses surfaces to the caller, with the write already done() FAILED
14 tests completed, 10 failed
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
24 tests completed, 3 failed
> Task :adapter-events-inprocess:test FAILED
GrantRevokedEdgeTest > revoking a grant reaches domainmgmt's handler through the dispatcher() FAILED
GrantRevokedEdgeTest > re-writing the same grant in a second transaction reaches no handler() FAILED
GrantRevokedEdgeTest > a handler that refuses surfaces to the caller, with the write already done() FAILED
14 tests completed, 3 failed
```

### Plant F — the dispatcher swallows a handler failure

`runCatching { subscription.deliver(event) }`. This is the plant for
`doc:20-ddd-practices#domain-events` §4.1.8's first clause.

```
> Task :adapter-events-inprocess:test FAILED
InProcessDomainEventDispatchTest > a handler that throws propagates, and is not swallowed() FAILED
    org.opentest4j.AssertionFailedError: Expected exception uk.m4xy.modus.adapter.events.inprocess.HandlerRefused but no exception was thrown.
InProcessDomainEventDispatchTest > delivery stops at the failing handler, and the subscription after it is not reached() FAILED
InProcessDomainEventDispatchTest > events after the failing one are not delivered() FAILED
GrantRevokedEdgeTest > a handler that refuses surfaces to the caller, with the write already done() FAILED
14 tests completed, 4 failed
```

### Plant G — the subscription list not copied on the way in

`= subscriptions` in place of `= subscriptions.toList()`.

```
> Task :adapter-events-inprocess:test FAILED
InProcessDomainEventDispatchTest > registering a subscription after construction changes nothing() FAILED
    org.opentest4j.AssertionFailedError: expected:<2> but was:<3>
14 tests completed, 1 failed
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
BUILD FAILED in 4s
```

### `config/coverage/baseline.tsv`

The dispatcher's move takes its coverage with it, which is a **recorded regression** on
`:core-application` — the case `doc:35-testing#coverage` §8.1 describes, where fully covered
production code leaves a module and `MISSEDCOUNT` alone would not notice.

```
> Task :coverageBaselineWrite FAILED
  :core-application              6 0 199 10 -> 6 0 160 6  <-- REGRESSION
> coverageBaselineWrite refuses to record worse coverage: :core-application (covered
instructions 199 -> 160, covered branches 10 -> 6). Restore the coverage, or re-run with
-Pcoverage.regress=<reason>
```

Re-run with the reason, which is in the baseline and in the pull-request body:

```
> Task :coverageBaselineWrite
  :core-application              6 0 199 10 -> 6 0 160 6  <-- REGRESSION
BUILD SUCCESSFUL
```

The accounting closes exactly: the new `:adapter-events-inprocess` row is `0 0 39 4`, and
39 covered instructions and 4 covered branches are precisely what left `:core-application`.
No missed count rises anywhere; `:core-application`'s six are still `ListBoundedContexts` and
`UseCase`, which have no test.

The writer erased the six-line regression-provenance block twice during this bean, as
`bean:0033` describes, and both times it was restored by hand. **Two new facts for
`bean:0033`:** the first of those runs moved two rows *upward* and the block was destroyed
just the same, so the defect is not conditional on the direction of the write — `bean:0065`
had observed it on a run where no figure changed at all. And on the second run, which *did*
regress, the writer composed its new note and dropped every earlier one, so a file that has
accumulated provenance loses it precisely when a reviewer most needs the history.

### `./gradlew qualityCheck`

```
docs-lint: OK — 19 documents, 111 anchors, 1786 references, 117 beans, 44 graph edges, 52 selectable, 117 bean ids, 5 introduced, 120 on origin/main, 0 closing transitions, 0 criteria checked, 0 unnumbered.
docs-lint-gate-test: 168 passed, 0 failed, over 2 bash major version(s).

> Task :qualityCheck

BUILD SUCCESSFUL
```

## What this bean did not do, and why

| not done | why |
|---|---|
| The wiring in `app/modus-server` | There is nothing to wire. `PermissionGrantRepository` and `DomainRepository` have no implementation — `bean:0009` declared both and implemented neither, `bean:0017` builds the flat-file store — so a Spring configuration for edge 1 would bind a handler to a use case that cannot be constructed. It lands with the first real repository |
| The durable event log `doc:20-ddd-practices#domain-events` §4.1.7 requires | It does not exist and never has; nothing in this repository appends a domain event anywhere. §4.1.7 carried no enforcement note until this change and now names `bean:0160`, which is blocked on `bean:0017`. §4.1.8's permanent loss of an undelivered suffix is its direct consequence |
| Replayable, asynchronous or cross-process delivery, and delivery that survives one handler's failure | Out of scope by the bean's own "Not owned", and the failure policy is the one the toolchain leaves reachable: running every handler needs a broad `catch`, which `TooGenericExceptionCaught` refuses outside two adapters, and the alternative is swallowing, which criterion 7 forbids. Retrying is a property of a durable dispatcher (`bean:0160`) |
| **Any mechanism that makes the drain contract binding** | `RaisesDomainEvents` is a type, not a gate. Three ways past it were planted and verified green: a use case can call `repository.save(root)` then `dispatcher.dispatch(root.pendingEvents)` and never touch `WriteThenDispatch`; `pendingEvents` stays public on all three roots; and **a new aggregate can implement `drainEvents()` as `events.toList()` with no `clear()`** — this bean's own defect — and pass compile, ktlint, Detekt, 119 `core-domain` tests and 63 architecture tests. `DrainEventsTest`'s "visible as an absence" is a human-noticing convention, not a mechanism. `bean:0133` carries all three, and `bean:0013` is being written against this contract now |
| A gate on `drainEvents` at all | `bean:0036`'s defensive-copy gate does not examine it: a copy hoisted into a local is invisible to the gate's `mentioned` check, observed both ways in `bean:0131`. Criterion 2 is met by test, and the note against it says so |

Five beans were raised rather than absorbed: `bean:0130` (`doc:20-ddd-practices` §5.1 has no
row for the two packages this added, and no line budget left to add one), `bean:0131` (the
defensive-copy gate's blind spot), `bean:0160` (no durable event log — it took **two** renumberings to land: 0132 was merged by a
sibling branch first and `docs-lint` check 13 caught it, then 0147 collided with a band already
allocated to `feat/atomic-document-write`, which check 13 could not catch because that branch is
unmerged. `bean:0051` predicted both. The orchestrator now allocates disjoint id bands per agent),
`bean:0133` (the drain
contract is bypassable three ways) and `bean:0134` (test doubles copied between modules,
which this bean's own module split caused).

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
