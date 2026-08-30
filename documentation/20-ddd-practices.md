---
id: doc:20-ddd-practices
title: DDD practices
status: active
superseded_by: null
read_when:
  - path: core/**
  - task: aggregate|value object|entity|domain event|invariant|domain service|port interface|use case|ubiquitous language
  - task: (add|new|write|create).{0,30}class
provides:
  - doc:20-ddd-practices#aggregates
  - doc:20-ddd-practices#value-objects
  - doc:20-ddd-practices#domain-events
  - doc:20-ddd-practices#ports-and-adapters
  - doc:20-ddd-practices#ambient-ports
  - doc:20-ddd-practices#invariants
  - doc:20-ddd-practices#domain-prohibitions
depends_on: [doc:00-constitution, doc:10-architecture, doc:15-repository-layout, doc:30-code-style]
---

# 20 — DDD Practices

Read this before writing anything in `core/`. These conventions are strict because
`core-domain` is the only part of Modus that is expensive to get wrong: everything else
can be replaced behind a port.

---

## 1. The layers, restated in DDD terms

| Layer | Gradle module | Contains | Never contains |
|---|---|---|---|
| Domain | `core/core-domain` | Aggregates, entities, value objects, domain events, domain services, ports, domain exceptions, specifications | Frameworks, IO, serialisation, transactions, DTOs |
| Application | `core/core-application` | Use cases, command/query objects, transaction boundaries, event dispatch, authorisation calls | Business invariants, HTTP concepts, file formats |
| Infrastructure | `adapters/*`, `modules/*` | Port implementations, mapping, serialisation, protocol | Business rules |
| Composition | `app/modus-server` | Bean wiring, configuration | Anything else |

**The load-bearing rule:** a business invariant lives in exactly one place — inside the
aggregate that owns it. If you find yourself checking a rule in a controller, a use case,
and a repository, the rule is in the wrong place.

---

## 2. Aggregate design <a id="aggregates"></a>

### 2.1 Rules

| # | Rule |
|---|---|
| 2.1.1 | An aggregate is a **consistency boundary**. Everything inside it is transactionally consistent; anything outside is eventually consistent. |
| 2.1.2 | **One aggregate per transaction.** A use case that mutates two aggregates in one commit is a design error — split it, and connect the halves with a domain event. |
| 2.1.3 | Aggregates reference other aggregates **by identifier only**, never by object reference. `WorkItem` holds an `EpicId`, not an `Epic`. |
| 2.1.4 | All mutation goes through methods on the aggregate root. **No mutable public API**: no public setter, no `var` in the root's public surface, no exposed mutable collection. A `private var` for state the root itself owns and mutates *is* legal — that is what "goes through methods on the root" means — and each one carries the one-line justification the `JustifiedVar` Detekt rule requires (`30-code-style.md` §4). Where a property is never reassigned, it is `private val`; Detekt's `VarCouldBeVal` fails the build otherwise. |
| 2.1.5 | The root validates every invariant it owns **before** the state change is visible. Construct-invalid is impossible: a constructed aggregate is a valid aggregate. |
| 2.1.6 | Keep aggregates small. If a root loads more than a few hundred child objects, the boundary is wrong. |
| 2.1.7 | Aggregates are `final` (Kotlin default). Never `open`. Inheritance between aggregates is forbidden; model variation with value objects or a sealed state hierarchy. |

### 2.2 Shape

This snippet is the most-copied thing in the package, so it obeys every rule the package
states — §2.1.4, `JustifiedVar` (`30-code-style.md` §4) and §7.2 included. If it ever
stops obeying them, fix the snippet, not the rules.

```kotlin
package uk.m4xy.modus.core.domain.work.aggregate

class WorkItem private constructor(
    val id: WorkItemId,
    val domainId: DomainId,
    // JustifiedVar: the state machine is this root's reason to exist; transitionTo is the
    // only writer, and it validates against the domain's ProcessDefinition first.
    private var state: WorkItemState,
    private val successCriteria: List<SuccessCriterion>,
    private val events: MutableList<DomainEvent> = mutableListOf(),
) {
    val pendingEvents: List<DomainEvent> get() = events.toList()

    fun transitionTo(
        target: WorkItemState,
        process: ProcessDefinition,
        actorId: ActorId,
        at: Instant,
    ): WorkItem {
        if (!process.allows(state, target)) {
            throw WorkItemTransitionNotPermittedException(id, state, target)
        }
        val from = state
        state = target
        events += WorkItemTransitioned(id, domainId, from, target, actorId, at)
        return this
    }

    companion object {
        fun create(/* … */): WorkItem { /* validate, then construct */ }
    }
}
```

Notes on the shape:
- It lives in `..domain.aggregate`, which is what gives `AggregatesAreSealedOrFinal`
  (`doc:15-repository-layout#core-package-rules` §4.2) and the aggregate coverage floor (§7.3) a decidable scope.
- Private constructor plus a named factory in the companion. The factory is where
  creation invariants live and where the `Created` event is raised.
- `state` is a `private var` with its justification comment; `successCriteria` is a
  `private val` because nothing reassigns it. Neither is visible outside the root.
- A refused transition is a **business rule the caller must surface**, so it throws a
  named `DomainException` subtype, not `require` (§7.2). `require` here would produce an
  `IllegalArgumentException` that the REST adapter cannot map to a meaningful status.
- Time arrives as a parameter (`at: Instant`), supplied by the use case from the `ClockPort`
  port. The aggregate never asks what time it is.
- Events accumulate on the root and are drained by the application layer after the write
  succeeds. The domain never dispatches.

### 2.3 The aggregates of Modus

| Context | Root | Boundary contains | Referenced by id |
|---|---|---|---|
| `identity` | `Actor` | credentials, display name | grants |
| `identity` | `PermissionGrant` | actor id, domain id, scope set | — |
| `domainmgmt` | `Domain` | name, process definition, settings | installations |
| `domainmgmt` | `ModuleInstallation` | module id, version, visibility, config | domain id |
| `work` | `WorkItem` | state, success criteria, links, evidence refs | epic id, domain id |
| `memory` | `Memory` | scope, assertion, evidence records, status | subject id |
| `execution` | `Trigger` | condition, target spec, enablement | domain id |
| `execution` | `AgentRun` | status, budget accounting, output-log ref | work item id, trigger id |
| `cost` | `SpendLedger` | append-only spend entries for one scope | run ids, work item ids |

`AgentRun` deliberately does **not** contain its output. The output is an append-only log
addressed by the run id; the aggregate holds a reference and the accounting summary.
Putting a megabyte-scale stream inside an aggregate would violate 2.1.6.

---

## 3. Value objects <a id="value-objects"></a>

### 3.1 Rules

- A value object is an immutable `data class` (or `@JvmInline value class` for a
  single-field wrapper) with **no identity**. Equality is structural.
- **No domain type may hand out a collection it owns.** Copy in at construction, copy out at
  every accessor. Kotlin's `Set`/`List` are read-only *views* rather than immutable types, so
  a caller down-casts what it was given and mutates what the object decides with — at size two
  or more; at size one `toSet()` returns an immutable singleton and the down-cast throws, which
  is why the test proving this MUST use a collection of two or more (`doc:35-testing#fixture-variation`).
  This is the general rule; the `data class` ban below is one consequence of it, not the whole
  of it. It has been broken three times: `PermissionGrant.capabilities` (`bean:0009`),
  all four of `ProcessDefinition`'s collections (`bean:0030`), and `GrantIssued.capabilities`
  (`bean:0036`), which let a handler add a capability to a fact that had already happened.
- **A value object holding a collection MUST NOT be a `data class`.** It cannot be immutable
  as one: the generated constructor binds the caller's collection, and a `public val` hands the
  backing instance to anyone who asks. Every invariant validated in `init` then holds exactly
  once, at construction. Use a private constructor, a named factory that copies on the way in,
  getters that copy on the way out, and hand-written `equals`/`hashCode` — the shape
  `PermissionGrant.issue` and `ProcessDefinition.of` both use.
  `Enforced by:` `rule:archunit/noDomainTypePublishesACollectionItOwns`, a **source** scan of
  every file under `core/core-domain/src/main`. It reads source, not bytecode: `javap -p` on
  the compiled `GrantIssued` lists no `capabilities` field at all, only `getCapabilities()`, so
  "the backing field is private" has no bytecode witness and only `data`'s `copy` survives for
  a bytecode rule to see — the weaker half, which misses `bean:0009`'s shape (`bean:0034` found
  the mirror image of this from the other side).
  **Copy-out** is enforced in full: a `data class` owning a collection at any visibility; a
  non-private stored collection property, with or without a declared type, in any class
  including an anonymous `object :` expression; an accessor that is not a plain **copy chain**
  — a bare field, a block getter with an early return past the copy, a conditional that copies
  down one branch, or anything that is not **a backing field followed by exactly one copy
  call**: argument-free proved necessary and not sufficient, since `held.register().toList()`
  takes no argument and still runs a side effect on the way past, and a method reference has no
  brace to ban; a non-private function whose returned expression so much as **mentions** a
  backing field and is not that copy, which covers `asReversed`, `subList`,
  `mapOf("all" to held)` and `Pair(held, held.size)` alike, through an expression body, a
  `return` in a block, or a wrapped signature; a function that mentions a backing field and
  **declares no return type**, which is a violation in itself rather than a guess at what the
  expression evaluates to; and a `typealias`, including a generic or twice-aliased one.
  That last rule's cost is larger than it first reads, and is stated here at full size: it is
  **no non-private function without a return type may mention any private field, of any type**.
  `internal fun size() = held.size` and `internal fun isFrozen() = frozen` are both rejected,
  and neither hands anything out. The breadth is deliberate — the check is over every private
  field precisely because a field whose type the scan cannot read is the case it exists for —
  and it fails closed. It costs nothing in `core-domain` today; it is not free in general.
  `bean:0064` carries narrowing it, and correcting its message, which still speaks only of
  collections.
  **Copy-in is enforced structurally, not semantically, and that limit is the rule's:** a
  collection-typed constructor property requires a `private constructor`; a type carrying one
  may not declare a secondary constructor a caller can reach; and a property that stores a
  constructor parameter without copying it — as an initialiser or in an `init` block — is
  rejected. The scan does **not** read a named factory's body, so a factory that forgets to
  copy passes. Do not read the `Enforced by:` line as covering that.
  **What it does not catch, each planted and observed passing, with the cheapest change that
  would close it and why that change is or is not being made.** "A type checker would be
  needed" was the previous phrasing and it overclaimed the limit — twice the cheapest thing
  turned out to be one `filter` clause, which is the same disease as overclaiming enforcement,
  pointed the other way.

  | shape that passes | cheapest change that closes it | taken? |
  |---|---|---|
  | anything outside `core-domain`'s main source | one constant and a `/src/main/` filter, scanning `core/` — measured green today | **No.** This rule binds a *domain* type owning state. `core-application` holds stateless use cases, where a collection crossing the boundary is a result, not a leak. Widening a gate because a module happens not to violate it today buys a grandfather clause the next honest change |
  | a leak laundered through a call, `= passthrough(granted)`, or handed to a callee, `sink.add(granted)` | flag any private collection field appearing as a call argument | **No.** It fires on legitimate internal use: `GrantIssued.hashCode()` passes the backing `issued` to `listOf(…)`, and `PermissionGrant.issue` passes locals named `granted` and `issued` that shadow backing fields, so a lexical check misfires on the real use and on the shadow alike. (An earlier draft cited `ProcessDefinition`'s `walk`, `reachableFrom` and `canFinish` — wrong: those are companion functions taking locals, and a companion cannot see the instance's fields. The conclusion survives on the citations above; the citation did not.) Distinguishing a callee that retains its argument from one that reads it is dataflow, not a regex |
  | a function whose declared return type names no collection at all — `public fun any(): Any = held`, `public fun iterate(): Iterator<StateName> = held.iterator()` | one predicate: apply the leak arm whenever the returned expression mentions a backing field, whatever the return type names | **Not yet — `bean:0064`.** Realistic containers are already caught, because the collection is named inside the generic (`Map<K, List<V>>`, `Pair<List<X>, Int>`); what survives is a signature nothing in `core-domain` writes. Small, but it is a change to a live predicate rather than to a sentence, and this commit is the one that documents the gap |
  | a named factory that forgets to copy, `of(steps) = Uncopied(steps)` | require every argument at a call to the type's own constructor to be a copy chain | **No.** `PermissionGrant.issue` copies into a local one line earlier and passes that local, so the check would reject the shape this very section prescribes. Needs local dataflow |

  Source ktlint has not formatted is outside its assumptions and **cannot reach `main`** —
  `ktlintMainSourceSetCheck` rejects it in the same `qualityCheck` run, and both halves of that
  were observed — but that guards **formatting only**, not shape. In the other direction it
  fails closed: `granted.toSet() + emptySet()` is a real copy and is reported, because a copy
  chain may take no arguments at all.
  **A gate's blind-spot list is part of the gate, and the list that matters is of shapes its
  regexes cannot see, not of rules it did not implement.** It was wrong after each of the first
  three review rounds, every time because a fix's *enabling condition* was never planted: a
  backing field with no declared type never became a property, so the rule that compares against
  it could not fire. The parser's input surface now has its own tests
  (`DefensiveCopyInputSurfaceTest`), separate from the rules (`DefensiveCopySourceTest`), and
  every line above is a planted counter-example rather than a claim (`bean:0036`).
- **Validate in `init`.** An invalid value object cannot exist. Throw a domain exception,
  not `IllegalArgumentException`, when the failure is a business rule rather than a
  programming error.
- **No primitive obsession.** Identifiers, money, token counts, durations, paths, and
  states are value objects, not `String`/`Long`/`Int`.
- Value objects are freely shareable and freely copied. They never reference an aggregate.

```kotlin
@JvmInline
value class DomainId(val value: String) {
    init {
        require(value.matches(SLUG)) { "domainId must be a slug: $value" }
    }
    companion object { private val SLUG = Regex("^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$") }
}

data class TokenCount(val value: Long) : Comparable<TokenCount> {
    init { require(value >= 0) { "token count must be non-negative" } }
    operator fun plus(other: TokenCount) = TokenCount(value + other.value)
    override fun compareTo(other: TokenCount) = value.compareTo(other.value)
}

data class Usd(val micros: Long) {          // money is integral; never Double
    init { require(micros >= 0) }
    operator fun plus(other: Usd) = Usd(micros + other.micros)
}
```

### 3.2 Required value objects

`DomainId`, `ActorId`, `WorkItemId`, `EpicId`, `MemoryId`, `RunId`, `TriggerId`,
`ModuleId`, `SkillId`, `EvidenceId`, `TokenCount`, `Usd`, `ModelId`, `EffortLevel`,
`ContextBudget`, `WorkItemState`, `MemoryStatus`, `RunStatus`, `Scope`.

`DomainId` is the exception: it belongs to no context and lives in the **shared kernel**
beside `DomainEvent`, because every context's events name the domain they concern and a
published package may not reach into another context's (`adr:0004-domain-id-shared-kernel`).
Every other `*Id` type, and any value object that appears in a domain event's signature
(`WorkItemState`, `MemoryStatus`, `RunStatus`), is its context's **published language**
and lives in `uk.m4xy.modus.core.domain.<ctx>.published` (§5.1). Everything else on this list
is internal to its context and lives in `uk.m4xy.modus.core.domain.<ctx>`.

**Money is never a `Double`.** `Usd` stores integer micros. **Enforcement gap:** the custom
Detekt rule `NoFloatingPointMoney` this relies on does not exist; see `30-code-style.md`
§4 and `bean:0026`.

---

## 4. Domain events <a id="domain-events"></a>

### 4.1 Rules

| # | Rule |
|---|---|
| 4.1.1 | Named in the **past tense**: `WorkItemTransitioned`, not `TransitionWorkItem`. |
| 4.1.2 | Immutable `data class`, all properties `val`, all properties value objects or primitives. Never an aggregate reference. **An event carrying a collection is the exception**, and follows §3.1 instead: a `data class` cannot own one, so it is a plain class with a private backing field, a copying getter and hand-written `equals`/`hashCode`. `GrantIssued` was a `data class` publishing a `Set<Capability>` until `bean:0036`. §3.1 was scoped to value objects and this taxonomy puts events here, so no rule ever reached it — a **gap**, not a collision, and this sentence is what closes it. |
| 4.1.3 | Carries `occurredAt: Instant`, supplied by the caller from `ClockPort` (§5.3). |
| 4.1.4 | Raised by the aggregate, drained and dispatched by the **application layer** after the write is durable. Never dispatched from inside the domain. |
| 4.1.5 | An event crossing a bounded context is part of that context's published contract. Changing its shape is a breaking change and needs an ADR. |
| 4.1.6 | Events are the only cross-context coupling mechanism, apart from explicit anti-corruption ports. |
| 4.1.7 | Every event is appended to the durable event log before any handler runs. Handlers are idempotent — they may be replayed. |

```kotlin
package uk.m4xy.modus.core.domain.work.event

data class WorkItemTransitioned(
    val workItemId: WorkItemId,
    val domainId: DomainId,
    val from: WorkItemState,
    val to: WorkItemState,
    val actorId: ActorId,
    override val occurredAt: Instant,
) : DomainEvent
```

---

## 5. Ports and adapters — naming and placement <a id="ports-and-adapters"></a>

### 5.1 Naming and package placement

Package placement is not cosmetic here: three mechanical rules — the published-language
allowlist (`doc:10-architecture#bounded-contexts` §3.1), `AggregatesAreSealedOrFinal`
(`doc:15-repository-layout#core-package-rules` §4.2) and the
aggregate coverage floor (§7.3) — can only be scoped because these packages exist. A type
in the wrong package silently removes it from a rule.

| Package | Contains | Notes |
|---|---|---|
| `uk.m4xy.modus.core.domain.<ctx>.aggregate` | Aggregate roots and the entities inside their boundary | Nothing else. This package **is** the definition of "aggregate" for every tool that needs one. |
| `uk.m4xy.modus.core.domain.<ctx>.event` | Domain events | **Published language.** Leaf package — see `doc:10-architecture#bounded-contexts` §3.1. |
| `uk.m4xy.modus.core.domain.<ctx>.published` | Identifier value objects (`WorkItemId`, `ActorId`, …) and any value object that appears in an event's signature (`WorkItemState`, `RunStatus`, …) | **Published language.** Leaf package. Moving a type in here is a deliberate act: it becomes another context's contract. |
| `uk.m4xy.modus.core.domain` | The shared kernel: `DomainEvent` and `DomainId`, and nothing else without an ADR | Belongs to no context. Membership test and the trigger for giving it its own package: `adr:0004-domain-id-shared-kernel#shared-kernel-membership`. |
| `uk.m4xy.modus.core.domain.<ctx>.port` | A context's own outbound ports | |
| `uk.m4xy.modus.core.domain.port` | Outbound ports for an ambient capability no context owns — §5.3's three | **A subpackage of the shared kernel's package, and not a member of the shared kernel.** That is the whole reason this row exists: membership is by name set and not by package, so nesting joins nothing. `rule:archunit/sharedKernelIsLeaf` scopes on the two names in `SHARED_KERNEL`, which a type here is not among. `adr:0004-domain-id-shared-kernel#shared-kernel-membership` does not apply for a second and independent reason: a port fails membership test 2 and cannot pass it while `..published..` and `..event..` are leaf packages, since a published type naming a port would itself be the violation. That is a standing condition rather than a permanent one — `adr:0004-domain-id-shared-kernel#deferred-conflict` defers the leaf rule to `bean:0023` — and the name-set argument above does not depend on it. The "third member" trigger counts kernel members, and this adds none. |
| `uk.m4xy.modus.core.domain.<ctx>` | Unpublished value objects, domain services, domain exceptions, specifications | The default; internal. |
| `uk.m4xy.modus.core.application.<ctx>.usecase` | Inbound ports and use cases | `core-application`. |

Which of these packages have members is a fact about the tree, answered by
`grep -rhE "^package uk\." core/ | sort -u`; which rows a rule scopes, and which of those
rules exist, is `doc:15-repository-layout#core-package-rules` §4.2's, rule by rule. Neither is
restated here: a row above is a placement rule, never a claim that its package exists yet.

| Kind | Package | Name pattern | Example |
|---|---|---|---|
| Outbound port (driven) | `uk.m4xy.modus.core.domain.<ctx>.port` | `<Noun>Port` or `<Aggregate>Repository` | `WorkItemRepository`, `ClockPort`, `AgentLauncherPort` |
| Inbound port (driving) | `uk.m4xy.modus.core.application.<ctx>.usecase` | `<Verb><Noun>UseCase` | `TransitionWorkItemUseCase` |
| Adapter | `uk.m4xy.modus.adapter.<tech>.<ctx>` | `<Tech><Port>` | `FlatFileWorkItemRepository`, `ClaudeAgentLauncher`, `GitRepositoryOperations` |
| Controller | `uk.m4xy.modus.adapter.rest.<ctx>` | `<Noun>Controller` | `WorkItemController` |
| DTO | `uk.m4xy.modus.adapter.rest.<ctx>.dto` | `<Noun>Request` / `<Noun>Response` | `TransitionWorkItemRequest` |

Forbidden name suffixes anywhere in `core/`: `*Impl`, `*Manager`, `*Helper`, `*Util`,
`*Utils`, `*Service`, `*Data`, `*Info`, `*Dto`, `*Entity`, `*Bean`. **Enforcement gap:** the
custom Detekt rule `ForbiddenTypeNameSuffix` this relies on does not exist; see
`30-code-style.md` §4 and `bean:0026`.

`*Service` is banned **outright**, with no domain-service exemption. §6 already requires a
domain service to be named for its operation (`PermissionResolver`, not
`PermissionService`), so the exemption could never be exercised, and "except a genuine
domain service" is not a predicate a Detekt rule can evaluate. Deleting it makes the rule
decidable and removes a contradiction between §5.1 and §6.

`*Impl` is banned outright: a port is `WorkItemRepository`; its implementation is
`FlatFileWorkItemRepository`. The implementation's name states the technology, which is
the only interesting thing about it.

### 5.2 Port design rules

- Ports are declared where they are **used**, not where they are implemented. The domain
  declares `WorkItemRepository`; `adapter-persistence-flatfile` implements it.
- A port's signature uses only domain types. No `ResultSet`, no `Path`, no `JsonNode`, no
  `ResponseEntity`, no Spring types, no checked framework exceptions.
- Ports are small and intention-revealing: `WorkItemRepository.findOpenIn(domainId)` beats
  a generic `findAll(spec)`.
- Ports throw **domain exceptions** or return domain result types. An adapter that catches
  an `IOException` translates it; it never lets it escape into the core.
- Repository ports are collection-oriented: `save`, `findById`, purpose-named finders.
  No `flush`, no `merge`, no `detach`, no unit-of-work leakage.

### 5.3 Required ambient-capability ports <a id="ambient-ports"></a>

Because `core-domain` may not touch ambient state, these ports exist and are injected. The
names below are the names, and the suffix is load-bearing: `core-domain` may reference
`java.time` types — `isLeafSafe` permits them and `rule:archunit/timeIsInjectedNeverReadFromAStaticClock`
drives `Instant.now(clock)` into domain code — so an unsuffixed `Clock` would stand beside
`java.time.Clock` in one file. This section **decides** the names; every other mention in the
package uses them and states no naming rule of its own, `doc:00-constitution` §1.3 included.

| Port | Replaces |
|---|---|
| `ClockPort` | `Instant.now()`, `LocalDate.now()` |
| `IdGeneratorPort` | `UUID.randomUUID()` |
| `RandomPort` | `kotlin.random.Random.Default` |

A test supplies a fixed clock and a deterministic id sequence. This is why domain tests
are reproducible; keep it that way.

---

## 6. Domain services

Use a domain service only when a rule genuinely belongs to no single aggregate — for
example, computing an actor's effective permission set across several grants.

Rules: stateless; named for the operation, not for its position in the architecture
(`PermissionResolver`, not `PermissionService`); lives in
`uk.m4xy.modus.core.domain.<ctx>`; takes and returns domain types only. If a "domain
service" is mostly calling ports and coordinating, it is a use case — move it to
`core-application`.

---

## 7. Invariants <a id="invariants"></a>

### 7.1 Where invariants live

| Invariant kind | Home | Example |
|---|---|---|
| Structural (a value's shape) | Value object `init` | `DomainId` is a slug |
| Aggregate-internal | Aggregate method, before mutation | A work item may not close with an unmet success criterion |
| Cross-aggregate, same context | Use case, via a domain service, eventually consistent | A domain may not exceed its module quota |
| Cross-context | Event handler, eventually consistent, compensating | Revoking a grant closes the actor's open runs |

### 7.2 How to express them

- `require(...)` for caller errors (a bad argument) — throws `IllegalArgumentException`.
- `check(...)` for state errors (wrong state for this operation) — throws `IllegalStateException`.
- A named domain exception for a **business rule** the caller is expected to handle and
  surface: `WorkItemNotClosableException(unmetCriteria)`. These extend a sealed
  `DomainException` hierarchy so the REST adapter maps them exhaustively to status codes,
  with no `else ->` branch.
- Never return `null` to signal a rule violation. Never return a boolean where the reason
  matters.

### 7.3 Every invariant has a test

Each invariant has a test named for it, in `core-domain`'s test source set, asserting
both the accepting and the rejecting case.

**Enforced by:** a Jacoco `violationRules` entry on `coverageRatchet` in the
`modus.kotlin-base` convention plugin requiring **100% `BRANCH` coverage** with the rule
element scoped to `PACKAGE`, `includes = ["uk.m4xy.modus.core.domain.*.aggregate"]`. It is
a ratio, so it needs no baseline row and never blocks a deletion; the module-wide exact
ratchet beside it is a regression trip-wire on a count, not a behavioural floor.
"Aggregate method" is not a concept Jacoco can
resolve; the package is, which is why §5.1 makes `..domain.aggregate` a convention rather
than a suggestion. Value objects, events and ids are covered by their own tests but are
outside this floor — their generated `data class` members would make 100% meaningless.
Naming a test after its invariant is checked by review, not by a tool.

---

## 8. What is forbidden in `core-domain` <a id="domain-prohibitions"></a>

The complete list, with the rule that actually catches each row. A row whose enforcement
is a person reading a diff says so; it does not borrow credibility from a tool that
cannot see it (`README.md`, conventions).

### 8.1 Mechanically enforced

| Forbidden | Why | Use instead | Enforced by |
|---|---|---|---|
| `org.springframework.*` | Framework coupling | Nothing — the domain needs no framework | ArchUnit `NoSpringInDomain`; Detekt `ForbiddenDomainApi` |
| `com.fasterxml.jackson.*` | Serialisation is an adapter concern | Map in the adapter | ArchUnit `NoJacksonInDomain`; `ForbiddenDomainApi` |
| `kotlinx.serialization.*` | Same | Map in the adapter | `ForbiddenDomainApi` |
| `jakarta.persistence.*`, `java.sql.*`, JDBC, JPA, any ORM | There is no database (`00` §2) | Repository ports | ArchUnit `NoDatabaseRules` (`10` §4.1) |
| `java.io.*`, `java.nio.file.*` | IO is an adapter concern | Repository ports | ArchUnit `NoIoInDomain`; `ForbiddenDomainApi` |
| `java.net.*`, any HTTP client | Transport is an adapter concern | Ports | ArchUnit `NoIoInDomain` |
| `org.slf4j.*`, `java.util.logging.*`, `println` | Logging is infrastructure; the domain communicates through return values and events | Domain events; log in the adapter | ArchUnit `NoLoggingInDomain`; Detekt `ForbiddenMethodCall` for `println` |
| `Instant.now()`, `LocalDate.now()`, `Clock.systemUTC()`, `System.currentTimeMillis()`, `System.nanoTime()` | Non-determinism | `ClockPort`, or an `Instant` parameter | ArchUnit `NoAmbientTime`; `ForbiddenDomainApi` (call sites) |
| `UUID.randomUUID()`, `Math.random()`, `Random.Default` | Non-determinism | `IdGeneratorPort`, `RandomPort` | ArchUnit `NoAmbientRandom`; `ForbiddenDomainApi` |
| `java.util.concurrent.*`, `Thread`, `Dispatchers`, `runBlocking` | Concurrency is an adapter concern | Return values; the application layer orchestrates | ArchUnit `NoAmbientConcurrency` |
| `System.getenv`, `System.getProperty` | Ambient configuration | Constructor parameters | `ForbiddenDomainApi` |
| `java.lang.reflect.*` | Opacity, and it defeats ArchUnit | Explicit code | ArchUnit `NoReflection` |
| Mutable `object` singletons, companion-object mutable state | Hidden global state | Constructor injection | Detekt `NoMutableSingletonState` (`30-code-style.md` §4) — a `var` or mutable-collection property in an `object` or `companion object` is plainly visible in the AST |
| `lateinit var` | Admits an invalid intermediate state | Constructor parameters | `ForbiddenDomainApi` |
| `!!` | Admits an unproven assumption | Model the absence in the type | Detekt `UnsafeCallOnNullableType` at `error` |
| `*Impl`, `*Manager`, `*Helper`, `*Util`, `*Service` type names | Names that describe nothing | Name it for what it does | Detekt `ForbiddenTypeNameSuffix` (§5.1) |

### 8.2 Not mechanically enforced

Both rows below need the *intent* behind a construct, which no rule in the stated
toolchain can decide. They remain MUST NOTs and they are checked in review.

| Forbidden | Why | Use instead |
|---|---|---|
| Nullable aggregate properties used as flags | Ambiguity — is the absence modelled, or is `null` standing in for `false`? | A sealed state hierarchy |
| Checked-exception-style control flow with generic exceptions | Untyped failure | The sealed `DomainException` hierarchy |

**Enforcement gap: review only.** `Bar?` is legal everywhere in Kotlin and nothing in the
type distinguishes modelled absence from a flag; "control-flow style" is a judgement, not
an API reference. The closest mechanisable substitutes are narrower rules that would catch
some instances — a Detekt rule forbidding a `Boolean?` property on a type in
`..domain.aggregate`, and `TooGenericExceptionThrown` at `error` — and raising those is
named as a follow-up in `bean:0001`. Until one exists, do not claim the build catches
these two.

---

## 9. Persistence and the domain

The domain has no idea it is stored in Markdown. This is not incidental — it is the point.

- The domain declares `WorkItemRepository`. `adapter-persistence-flatfile` implements it
  over Markdown-with-frontmatter files. The mapping (frontmatter keys, body layout,
  cross-reference syntax) is **entirely** the adapter's business and follows the upstream
  `hmans/beans` convention; `bean:0008` records the migration.
- The domain never knows a file path, a filename, or a directory layout.
- If a domain concept exists only to make storage convenient (a `version` field for
  optimistic locking, a `filePath`), it belongs in the adapter's own record type, not in
  the aggregate.
- Conversely, the on-disk shape is allowed to differ from the aggregate shape. The
  adapter owns the translation and owns its tests.

---

## 10. Checklist before you open a pull request touching `core/`

- [ ] Every new type is an aggregate, entity, value object, event, port, domain service,
      or domain exception — and you can say which.
- [ ] No forbidden import from §8 appears.
- [ ] Every invariant has an accepting test and a rejecting test.
- [ ] Time and identity arrive as parameters or ports — never ambiently.
- [ ] Cross-aggregate coordination is by event, not by a two-aggregate transaction.
- [ ] No primitive stands in for a concept that deserves a value object.
- [ ] `./gradlew :core-domain:check` passes in under 10 seconds. Project names are flat
      (`settings.gradle.kts`), so it is `:core-domain`, not `:core:core-domain`.
