---
# modus-0152
title: The WorkItem aggregate, its per-domain state machine and the evidence guard
status: in-progress
type: feature
priority: high
order: AN
created_at: 2026-09-05T00:00:00Z
parent: modus-0013
blocked_by: [modus-0030]
---

# The WorkItem aggregate, its per-domain state machine and the evidence guard

First child of `bean:0013`. `domainmgmt` now creates a domain and gives it the process it
imposes (`bean:0030`); nothing yet moves through one. This bean is the thing that moves.

The shape is `bean:0009`'s, most recently exhibited by `bean:0030`, and is not restated:
placement by kind, published language as a leaf, invariants in `init`, private constructor
plus named factory, time as a parameter, ports declared and unimplemented, evidence per
`doc:35-testing#load-bearing-evidence` with fixtures varied per
`doc:35-testing#fixture-variation`.

## Scope

Owned: `core/core-domain/src/{main,test}/kotlin/uk/m4xy/modus/core/domain/work/**`,
the `WorkContext` marker with `BoundedContexts`' reference to it, and
`config/coverage/baseline.tsv`. The marker's own KDoc says *"delete it as soon as the
context has a real aggregate root"*, and this bean is where that root arrives;
`bean:0030` set the precedent.

Not owned: `identity/**` and `domainmgmt/**` except as imports; the shared kernel —
`DomainId` is imported, never copied; `core-application/**`, which is `bean:0153`;
`adapters/**` (`bean:0017` owns the store and is in flight); `modules/**`, `app/**`,
`backoffice/**`, `e2e/**`, `documentation/**`.

## Decisions

**The state machine is `domainmgmt`'s `ProcessDefinition`, passed in at the call, and the
aggregate holds no states of its own.** `WorkItem.transitionTo` takes the definition as a
parameter and asks it three questions — `allows(from, to)`, `isTerminal(to)` — and there is
no fourth source of truth. There is no enum anywhere in the context, no `OPEN`/`CLOSED`
constant, and no state literal in `src/main`: `WorkItemState` is a validated *name*, exactly
as `StateName` is, and its membership is per-domain data
(`doc:00-constitution#domain-scoping`). Criterion 3 proves this by construction rather than
by assertion — two processes with disjoint state vocabularies drive the same aggregate, and
a state that is terminal in one is a legal intermediate state in the other.

**A close is a transition to a terminal state, and there is no second method for it.**
`transitionTo` is the only writer of the state. When the target is terminal it applies the
evidence guard before mutating, and raises `WorkItemClosed` after `WorkItemTransitioned`.
The alternative — a separate `close()` beside a `transitionTo()` that permits any legal
move — was rejected because it makes the evidence guard bypassable by calling the other
method, and `doc:00-constitution#evidence-rule` is not a rule a caller may opt out of. What
"done" means is still per-domain: the guard fires on whatever states *this domain's*
process declares terminal.

**`work` declares its own evidence types and imports none of `memory`'s.**
`doc:10-architecture#bounded-contexts` §3.1 forbids `work` from importing `memory` in any
form, so `EvidenceRecord`, `EvidenceKind` and `EvidenceReference` are this context's
internals. `EvidenceKind` is an opaque validated name, never an enum, for the same reason
`StateName` is one: `doc:00-constitution#domain-scoping` lists required evidence kinds
beside states and definition of done as things a domain defines for itself. Per-domain
*required* kinds stay deferred exactly as `bean:0030` deferred them, and for the same
reason — the closed set they would override is `memory`'s and does not exist yet
(`bean:0015`).

**No domain event in this context names an actor, and `doc:20-ddd-practices` §4.1's own
snippet cannot compile.** That snippet puts `val actorId: ActorId` into
`work.event.WorkItemTransitioned`, importing `identity.published`. `rule:archunit/publishedLanguageIsLeaf`
refuses it: a published package may reference only the Kotlin stdlib, `java.time`, its own
context's published language and the shared kernel. The allowlist in
`doc:10-architecture#bounded-contexts` §3.1 permits `work` to import `identity`'s published
language from its *internals*, not from its own published packages. Attribution's whole
value is in the event, so it is left out entirely rather than modelled where it cannot be
published. `bean:0154` carries correcting the document and deciding whether `ActorId`
becomes a third shared-kernel member (which needs an ADR,
`adr:0004-domain-id-shared-kernel#shared-kernel-membership`).

**A per-context sealed exception root, not a repository-wide `DomainException`.**
`doc:20-ddd-practices#invariants` §7.2 names a sealed `DomainException` hierarchy; a root
spanning every context would be a third shared-kernel member and needs an ADR. `WorkException`
is sealed within this context, which is what makes the REST adapter's `when` exhaustive per
context. `bean:0154` carries the wider question.

## Success criteria and evidence

| # | criterion | evidence kind |
|---|---|---|
| 1 | `WorkItem` and `Epic` are aggregate roots under `..work.aggregate`: private constructor, named factory, no public mutable surface, `final` | citation + `rule:archunit/aggregatesAreSealedOrFinal` |
| 2 | `WorkItemCreated`, `WorkItemTransitioned` and `WorkItemClosed` are raised by the root, never dispatched, and accumulate on `pendingEvents`; `drainEvents` hands them over exactly once | test-run |
| 3 | The state machine is data, not code: two processes with disjoint state vocabularies drive the same aggregate, a state terminal in one is a legal intermediate in the other, and `src/main` under `..domain.work` contains no enum and no state literal | test-run + `grep` over `src/main` |
| 4 | A transition the domain's process does not permit is refused with `WorkItemTransitionNotPermittedException`, and every transition it does permit is allowed | test-run, rejecting **and** accepting case |
| 5 | A transition into a terminal state is refused with `WorkItemNotClosableException` naming every criterion with no evidence record; with an evidence record per criterion it succeeds | test-run, rejecting **and** accepting case |
| 6 | An item with no success criteria closes, and an item whose criteria are partly evidenced does not — the guard is per criterion, not "any evidence at all" | test-run |
| 7 | `WorkItemState` accepts exactly what `domainmgmt`'s `StateName` accepts, so the mapping `work` performs is total | test-run over one corpus, both types |
| 8 | Every published type in `..work.published` and `..work.event` is leaf-safe | `rule:archunit/publishedLanguageIsLeaf`, observed failing on a planted violation |
| 9 | `WorkItemRepository` and `EpicRepository` are interfaces in `..work.port` with no implementation | citation + `rule:archunit/portsAreInterfaces` |
| 10 | No domain type in this context hands out a collection it owns | `rule:archunit/noDomainTypePublishesACollectionItOwns` |
| 11 | 100% branch coverage over `..work.aggregate`, and `config/coverage/baseline.tsv` moved by exactly this bean's figures with its regression-provenance block intact | `./gradlew qualityCheck` + `git diff` |
| 12 | Each test is load-bearing: broken subject, observed assertion recorded verbatim, reverted | test-run, per `doc:35-testing#load-bearing-evidence` |
| 13 | Fixtures vary collection size across 0, 1 and 2-or-more wherever a collection is accepted | citation, per `doc:35-testing#fixture-variation` |
| 14 | The `WorkContext` marker is gone and `BoundedContexts` names `work` as a literal | `git diff`; `BoundedContextsTest` still asserts six |
| 15 | `./gradlew qualityCheck` is green | test-run |

Out of scope, explicitly: consuming `ProcessDefinitionChanged` and every use case
(`bean:0153`); implementing `RaisesDomainEvents` and adding a `DrainEventsTest` case, which
need `bean:0066` merged (`bean:0153`); persistence (`bean:0017`); any REST surface
(`bean:0018`); the two context-isolation ArchUnit rules (`bean:0023`).

## Evidence

To be recorded as each criterion is satisfied.
