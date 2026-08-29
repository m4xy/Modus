---
# modus-0030
title: The Domain aggregate and per-domain process definitions
status: in-progress
type: feature
priority: high
order: AM
created_at: 2026-08-29T00:00:00Z
parent: modus-0012
---

# The Domain aggregate and per-domain process definitions

First child of `bean:0012`. `identity` grants access to a `DomainId` that nothing creates
and no rule describes; this bean creates it and gives it the process it imposes.

The shape is `bean:0009`'s and is not restated: placement by kind, published language as a
leaf, invariants in `init`, private constructor plus named factory, time as a parameter,
ports declared and unimplemented, evidence per `doc:35-testing#load-bearing-evidence` with
fixtures varied per `doc:35-testing#fixture-variation`.

## Scope

Owned: `core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/domainmgmt/**` except
`ModuleInstallation` and its events, `core/core-domain/src/test/**` for the same,
`config/coverage/baseline.tsv`, and the `DomainMgmtContext` marker with `BoundedContexts`'
reference to it — deleting a 14-line file and editing `BoundedContexts` by +5/-6. It moved
here from `bean:0031` because the marker's own KDoc says *"delete it as soon as the context
has a real aggregate root"*, and this bean is where that root arrives; leaving it beside a
real aggregate is a lie the next agent has to check.

Not owned: `identity/**`, and the shared kernel — `DomainId` is imported, never copied.
`ModuleInstallation`, `ModuleInstalled`, `ModuleUninstalled`, module visibility and
`GrantRevoked` consumption are `bean:0031`. `core-application`, `adapters/**`,
`modules/**`, `app/**`, `backoffice/**`, `e2e/**`, `documentation/**`.

## Decisions

**`DomainId` comes from the shared kernel.** Settled ahead of this bean by
`adr:0004-domain-id-shared-kernel`, which was raised precisely because writing this
context's first event hit it: `domainmgmt`'s events name a domain, `DomainId` lived in
`identity.published`, and a published package may not reach into another context's. Import
`uk.m4xy.modus.core.domain.DomainId`; do not declare one here. `rule:archunit/publishedLanguageIsLeaf`
refuses any other cross-context published import, including `identity`'s `Capability` —
observed, in that ADR's pull request.

**`ProcessDefinition` is published language, not internal.** It appears in
`ProcessDefinitionChanged`'s signature, which publishes it by `doc:10-architecture#bounded-contexts`
§3.1's rule, and `doc:20-ddd-practices#aggregates` §2.2's aggregate shape passes it directly
into `WorkItem.transitionTo` — so `work` needs the type, not a copy of the data.

**States are opaque names, not `work`'s `WorkItemState` and not an enum.** `domainmgmt`
MUST NOT import `work` in any form (§3.1), so it cannot reference `WorkItemState`. It must
not be an enum either: `doc:00-constitution#domain-scoping` says every domain defines its
own work-item states and code MUST NOT hardcode a single process. `StateName` is therefore
a validated value object whose membership is per-domain data. `work` maps its own states
onto these names; that direction is legal, the reverse is not.

Alternative rejected: `ProcessDefinitionChanged` carrying only a `DomainId` and a version,
with consumers re-reading through an anti-corruption port. Smaller published surface, but
`work` cannot guard a transition without the definition in hand, so every consumer would
need a port back into this context — which is the coupling §3.1 exists to prevent.

## Success criteria and evidence

| # | criterion | evidence kind |
|---|---|---|
| 1 | `Domain` is an aggregate root under `..domainmgmt.aggregate`: private constructor, named factory, no public mutable surface, `final` | citation + `rule:archunit/aggregatesAreSealedOrFinal` |
| 2 | A `Domain` cannot be constructed invalid — a blank name, a process whose initial state is not among its states, or a transition naming an unknown state, each refused | test-run, accepting and rejecting case per invariant |
| 3 | `DomainCreated` and `ProcessDefinitionChanged` are raised by the root, never dispatched, and accumulate on `pendingEvents` | test-run |
| 4 | `ProcessDefinition` refuses a definition with no terminal state, and refuses a transition leaving a terminal state | test-run |
| 5 | Changing a domain's process raises `ProcessDefinitionChanged` carrying the new definition; changing it to the definition it already has raises nothing | test-run |
| 6 | `DomainId` resolves to the shared-kernel type — no second declaration exists in this context | `grep`, and the import in the compiled source |
| 7 | Every published type in `..domainmgmt.published` and `..domainmgmt.event` is leaf-safe | `rule:archunit/publishedLanguageIsLeaf`, observed failing on a planted violation |
| 8 | `DomainRepository` is an interface in `..domainmgmt.port` with no implementation | citation. `PortsAreInterfaces` is listed in `doc:10-architecture` §4.2 and **is not implemented** — no `Enforcement gap:` marks it, unlike the adapter rules below it in that table. Raise it against `bean:0027`; do not cite it as enforcing |
| 9 | 100% branch coverage over `..domainmgmt.aggregate`, and `config/coverage/baseline.tsv` moved by exactly this bean's figures | `./gradlew qualityCheck` |
| 10 | Each test is load-bearing: broken subject, observed assertion recorded verbatim, reverted | test-run, per `doc:35-testing#load-bearing-evidence` |
| 11 | Fixtures vary collection size across 0, 1 and 2-or-more wherever a collection is accepted | citation, per `doc:35-testing#fixture-variation` |
| 12 | `./gradlew qualityCheck` is green | test-run |
| 13 | The `DomainMgmtContext` marker is gone and `BoundedContexts` names `domainmgmt` as a literal, exactly as `bean:0009` did for `identity` | `git diff`; `BoundedContextsTest` still asserts six |

Out of scope, explicitly: persistence (`bean:0017`), any REST surface (`bean:0018`), the
two context-isolation ArchUnit rules (`bean:0023` — this bean supplies the second context
they need, and does not implement them).

**Per-domain required evidence kinds are deferred, deliberately.** `doc:00-constitution#domain-scoping`
lists them beside states and definition of done as things a domain defines for itself, so
they belong in `ProcessDefinition` eventually. They are not modelled here because the thing
they override does not exist: `doc:50-memory-and-evidence#evidence-kinds` is a closed set of
six owned by `memory`, which is `bean:0015`. Modelling an override of an unbuilt closed set
means inventing its vocabulary twice and reconciling later. `bean:0015` adds the field, with
`domainmgmt` holding opaque names — it may not import `memory` (`doc:10-architecture#bounded-contexts`
§3.1) — and this paragraph is the record that it was a decision, not an omission.

## Evidence

33 new tests (`DomainTest` 11, `ProcessDefinitionTest` 15, `PublishedLanguageTest` 7);
`:core-domain` goes 43 → 76. `./gradlew qualityCheck` green.

Criterion 10, targeted mutation per `doc:35-testing#load-bearing-evidence` — ten planted,
ten killed, each by the test whose name describes the behaviour and each with a real
assertion rather than an incidental exception. Planted, observed, reverted:

| planted | test that caught it | observed |
|---|---|---|
| `adoptProcess` drops the idempotence guard | `adopting the process already in force raises nothing` | `AssertionFailedError: Unexpected elements from index 1` |
| `equals` falls back to reference identity | `two instances of one domain id are the same domain, whatever their process` | `expected:<…aggregate.Domain@d506082c> but was:<…>` |
| `pendingEvents` hands out the backing list | `pendingEvents is a copy, so draining it cannot empty the root` | `expected:<2> but was:<0>` |
| `ProcessDefinition` drops the reachability check | `refuses a process with a state unreachable from its initial state` | `Expected exception java.lang.IllegalArgumentException but no exception was thrown.` |
| `initial` allowed to be terminal | `refuses a process whose initial state is also terminal` | `expected:<processDefinition initial state 'todo' is also terminal, …> but was:<…>` |
| a transition may leave a terminal state | `refuses a transition out of a terminal state` | `Expected exception java.lang.IllegalArgumentException but no exception was thrown.` |
| `allows` ignores the target state | `permits exactly the moves it declares` | `expected:<false> but was:<true>` |
| `StateTransition` permits a self-transition | `refuses a self-transition, in the pair and in the query` | `Expected exception java.lang.IllegalArgumentException but no exception was thrown.` |
| `DomainName` stops rejecting control characters | `refuses a domain name carrying control characters` | `Expected exception java.lang.IllegalArgumentException but no exception was thrown.` |
| `StateName` accepts upper case | `refuses a state name that could not survive a URL, a file name or a log field` | `Expected exception java.lang.IllegalArgumentException but no exception was thrown.` |

Criterion 7 — `rule:archunit/publishedLanguageIsLeaf`, planted on the real event type
rather than a probe:

```
planted:  DomainCreated gains `val probe: Capability get() = Capability("work.read")`
observed: ArchitectureRulesTest > publishedLanguageIsLeaf FAILED … was violated (1 times):
          Method <uk.m4xy.modus.core.domain.domainmgmt.event.DomainCreated.getProbe-OlfN_Ag()>
            calls method <uk.m4xy.modus.core.domain.identity.published.Capability.constructor-impl(
            java.lang.String)> in (DomainMgmtEvents.kt:25)
reverted: yes
```

Criterion 9 — the 100% aggregate branch floor, shown non-vacuous over the **new** package:

```
planted:  Domain gains `fun probe() = if (name.value.isEmpty()) "empty" else "present"`
observed: Rule violated for package uk.m4xy.modus.core.domain.domainmgmt.aggregate:
            branches covered ratio is 0.6, but expected minimum is 1.0
reverted: yes
```

## What the work changed about the plan

**`allows` was rewritten mid-implementation.** It read
`from != to && StateTransition(from, to) in transitions`, which constructs a value object
whose `init` refuses a self-transition — so `allows(x, x)` threw where it must answer
`false`. The mutation pass is what exposed it: killing the guard produced an
`IllegalArgumentException`, and `doc:35-testing#load-bearing-evidence` says a test that
fails with an exception rather than its named assertion has proved nothing. It now matches
on the pair's parts. A query never throws because its argument is uninteresting.

**Criterion 4 was sharpened before any code, not weakened after.** It read "refuses a `done`
transition whose target is not terminal", which names a `done` concept this context does not
have. It is now "refuses a transition leaving a terminal state" — the rule that was actually
meant, and one a test can state.

**`doc:35-testing#fixture-variation` caught a test that passed for the wrong reason.** The
`pendingEvents` copy test was written against a domain with one event. At size one
`toList()` returns `Collections.singletonList`, whose `clear()` throws
`UnsupportedOperationException` before proving anything about copying; at size two it is an
`ArrayList` the cast genuinely reaches. Rewritten against two events. This is the same
size-one degeneracy that hid `bean:0009`'s privilege escalation, in a different `toList()`.

**The coverage ratchet found five unread public accessors** — `ProcessDefinition.initial`
and `.transitions`, `Domain.id` and `.name`, and every property of both events. Nothing
asserted on the contract those getters *are*. Covered by asserting on them, not by
deleting them.
