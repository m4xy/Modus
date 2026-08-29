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
  - doc:20-ddd-practices#invariants
  - doc:20-ddd-practices#domain-prohibitions
depends_on: [doc:00-constitution, doc:10-architecture, doc:30-code-style]
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
package com.modus.core.work.domain.aggregate

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
  (`10-architecture.md` §4.2) and the aggregate coverage floor (§7.3) a decidable scope.
- Private constructor plus a named factory in the companion. The factory is where
  creation invariants live and where the `Created` event is raised.
- `state` is a `private var` with its justification comment; `successCriteria` is a
  `private val` because nothing reassigns it. Neither is visible outside the root.
- A refused transition is a **business rule the caller must surface**, so it throws a
  named `DomainException` subtype, not `require` (§7.2). `require` here would produce an
  `IllegalArgumentException` that the REST adapter cannot map to a meaningful status.
- Time arrives as a parameter (`at: Instant`), supplied by the use case from the `Clock`
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

The `*Id` types, and any value object that appears in a domain event's signature
(`WorkItemState`, `MemoryStatus`, `RunStatus`), are their context's **published language**
and live in `com.modus.core.<ctx>.domain.published` (§5.1). Everything else on this list
is internal to its context and lives in `com.modus.core.<ctx>.domain`.

**Money is never a `Double`.** `Usd` stores integer micros. **Enforced by:** the custom
Detekt rule `NoFloatingPointMoney` (see `30-code-style.md` §4).

---

## 4. Domain events <a id="domain-events"></a>

### 4.1 Rules

| # | Rule |
|---|---|
| 4.1.1 | Named in the **past tense**: `WorkItemTransitioned`, not `TransitionWorkItem`. |
| 4.1.2 | Immutable `data class`, all properties `val`, all properties value objects or primitives. Never an aggregate reference. |
| 4.1.3 | Carries `occurredAt: Instant`, supplied by the caller from the `Clock` port. |
| 4.1.4 | Raised by the aggregate, drained and dispatched by the **application layer** after the write is durable. Never dispatched from inside the domain. |
| 4.1.5 | An event crossing a bounded context is part of that context's published contract. Changing its shape is a breaking change and needs an ADR. |
| 4.1.6 | Events are the only cross-context coupling mechanism, apart from explicit anti-corruption ports. |
| 4.1.7 | Every event is appended to the durable event log before any handler runs. Handlers are idempotent — they may be replayed. |

```kotlin
package com.modus.core.work.domain.event

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
allowlist (`10-architecture.md` §3.1), `AggregatesAreSealedOrFinal` (§4.2) and the
aggregate coverage floor (§7.3) — can only be scoped because these packages exist. A type
in the wrong package silently removes it from a rule.

| Package | Contains | Notes |
|---|---|---|
| `com.modus.core.<ctx>.domain.aggregate` | Aggregate roots and the entities inside their boundary | Nothing else. This package **is** the definition of "aggregate" for every tool that needs one. |
| `com.modus.core.<ctx>.domain.event` | Domain events | **Published language.** Leaf package — see `10-architecture.md` §3.1. |
| `com.modus.core.<ctx>.domain.published` | Identifier value objects (`WorkItemId`, `DomainId`, …) and any value object that appears in an event's signature (`WorkItemState`, `RunStatus`, …) | **Published language.** Leaf package. Moving a type in here is a deliberate act: it becomes another context's contract. |
| `com.modus.core.<ctx>.domain.port` | Outbound ports | |
| `com.modus.core.<ctx>.domain` | Unpublished value objects, domain services, domain exceptions, specifications | The default; internal. |
| `com.modus.core.<ctx>.application.usecase` | Inbound ports and use cases | `core-application`. |

| Kind | Package | Name pattern | Example |
|---|---|---|---|
| Outbound port (driven) | `com.modus.core.<ctx>.domain.port` | `<Noun>Port` or `<Aggregate>Repository` | `WorkItemRepository`, `ClockPort`, `AgentLauncherPort` |
| Inbound port (driving) | `com.modus.core.<ctx>.application.usecase` | `<Verb><Noun>UseCase` | `TransitionWorkItemUseCase` |
| Adapter | `com.modus.adapter.<tech>.<ctx>` | `<Tech><Port>` | `FlatFileWorkItemRepository`, `ClaudeAgentLauncher`, `GitRepositoryOperations` |
| Controller | `com.modus.adapter.rest.<ctx>` | `<Noun>Controller` | `WorkItemController` |
| DTO | `com.modus.adapter.rest.<ctx>.dto` | `<Noun>Request` / `<Noun>Response` | `TransitionWorkItemRequest` |

Forbidden name suffixes anywhere in `core/`: `*Impl`, `*Manager`, `*Helper`, `*Util`,
`*Utils`, `*Service`, `*Data`, `*Info`, `*Dto`, `*Entity`, `*Bean`. **Enforced by:** the
custom Detekt rule `ForbiddenTypeNameSuffix`.

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

### 5.3 Required ambient-capability ports

Because `core-domain` may not touch ambient state, these ports exist and are injected:

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
`com.modus.core.<ctx>.domain`; takes and returns domain types only. If a "domain
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
  cross-reference syntax) is **entirely** the adapter's business and follows
  `documentation/90-work-items.md` (owned separately).
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
- [ ] `./gradlew :core:core-domain:check` passes in under 10 seconds.
