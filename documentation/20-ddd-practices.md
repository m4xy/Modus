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

## 2. Aggregate design

### 2.1 Rules

| # | Rule |
|---|---|
| 2.1.1 | An aggregate is a **consistency boundary**. Everything inside it is transactionally consistent; anything outside is eventually consistent. |
| 2.1.2 | **One aggregate per transaction.** A use case that mutates two aggregates in one commit is a design error — split it, and connect the halves with a domain event. |
| 2.1.3 | Aggregates reference other aggregates **by identifier only**, never by object reference. `WorkItem` holds an `EpicId`, not an `Epic`. |
| 2.1.4 | All mutation goes through methods on the aggregate root. No public setters, no `var` on a root's properties, no exposed mutable collections. |
| 2.1.5 | The root validates every invariant it owns **before** the state change is visible. Construct-invalid is impossible: a constructed aggregate is a valid aggregate. |
| 2.1.6 | Keep aggregates small. If a root loads more than a few hundred child objects, the boundary is wrong. |
| 2.1.7 | Aggregates are `final` (Kotlin default). Never `open`. Inheritance between aggregates is forbidden; model variation with value objects or a sealed state hierarchy. |

### 2.2 Shape

```kotlin
package com.modus.core.work.domain

class WorkItem private constructor(
    val id: WorkItemId,
    val domainId: DomainId,
    private var state: WorkItemState,
    private var successCriteria: List<SuccessCriterion>,
    private val events: MutableList<DomainEvent> = mutableListOf(),
) {
    val pendingEvents: List<DomainEvent> get() = events.toList()

    fun transitionTo(target: WorkItemState, process: ProcessDefinition, at: Instant): WorkItem {
        require(process.allows(state, target)) { "transition $state -> $target not permitted" }
        state = target
        events += WorkItemTransitioned(id, target, at)
        return this
    }

    companion object {
        fun create(/* … */): WorkItem { /* validate, then construct */ }
    }
}
```

Notes on the shape:
- Private constructor plus a named factory in the companion. The factory is where
  creation invariants live and where the `Created` event is raised.
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

## 3. Value objects

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

**Money is never a `Double`.** `Usd` stores integer micros. **Enforced by:** the custom
Detekt rule `NoFloatingPointMoney` (see `30-code-style.md` §4).

---

## 4. Domain events

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

## 5. Ports and adapters — naming and placement

### 5.1 Naming

| Kind | Package | Name pattern | Example |
|---|---|---|---|
| Outbound port (driven) | `com.modus.core.<ctx>.domain.port` | `<Noun>Port` or `<Aggregate>Repository` | `WorkItemRepository`, `ClockPort`, `AgentLauncherPort` |
| Inbound port (driving) | `com.modus.core.<ctx>.application.usecase` | `<Verb><Noun>UseCase` | `TransitionWorkItemUseCase` |
| Adapter | `com.modus.adapter.<tech>.<ctx>` | `<Tech><Port>` | `FlatFileWorkItemRepository`, `ClaudeAgentLauncher`, `GitRepositoryOperations` |
| Controller | `com.modus.adapter.rest.<ctx>` | `<Noun>Controller` | `WorkItemController` |
| DTO | `com.modus.adapter.rest.<ctx>.dto` | `<Noun>Request` / `<Noun>Response` | `TransitionWorkItemRequest` |

Forbidden name suffixes anywhere in `core/`: `*Impl`, `*Manager`, `*Helper`, `*Util`,
`*Utils`, `*Service` (except a genuine domain service, see §6), `*Data`, `*Info`,
`*Dto`, `*Entity`, `*Bean`. **Enforced by:** the custom Detekt rule
`ForbiddenTypeNameSuffix`.

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

## 7. Invariants

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
both the accepting and the rejecting case. **Enforced by:** review, plus a coverage floor
on `core-domain` (100% branch coverage on aggregate methods) in the `modus.test`
convention plugin.

---

## 8. What is forbidden in `core-domain`

The complete list. Each is enforced by ArchUnit, by the `ForbiddenDomainApi` Detekt rule,
or by both.

| Forbidden | Why | Use instead |
|---|---|---|
| `org.springframework.*` | Framework coupling | Nothing — the domain needs no framework |
| `com.fasterxml.jackson.*` | Serialisation is an adapter concern | Map in the adapter |
| `kotlinx.serialization.*` | Same | Map in the adapter |
| `jakarta.persistence.*`, `java.sql.*`, JDBC, JPA, any ORM | There is no database (`00` §2) | Repository ports |
| `java.io.*`, `java.nio.file.*` | IO is an adapter concern | Repository ports |
| `java.net.*`, any HTTP client | Transport is an adapter concern | Ports |
| `org.slf4j.*`, `java.util.logging.*`, `println` | Logging is infrastructure; the domain communicates through return values and events | Domain events; log in the adapter |
| `Instant.now()`, `LocalDate.now()`, `Clock.systemUTC()`, `System.currentTimeMillis()`, `System.nanoTime()` | Non-determinism | `ClockPort`, or an `Instant` parameter |
| `UUID.randomUUID()`, `Math.random()`, `Random.Default` | Non-determinism | `IdGeneratorPort`, `RandomPort` |
| `java.util.concurrent.*`, `Thread`, `Dispatchers`, `runBlocking` | Concurrency is an adapter concern | Return values; the application layer orchestrates |
| `System.getenv`, `System.getProperty` | Ambient configuration | Constructor parameters |
| `java.lang.reflect.*` | Opacity, and it defeats ArchUnit | Explicit code |
| Mutable `object` singletons, companion-object mutable state | Hidden global state | Constructor injection |
| `lateinit var` | Admits an invalid intermediate state | Constructor parameters |
| `!!` | Admits an unproven assumption | Model the absence in the type |
| Nullable aggregate properties used as flags | Ambiguity | A sealed state hierarchy |
| `*Impl`, `*Manager`, `*Helper`, `*Util` type names | Names that describe nothing | Name it for what it does |
| Checked-exception-style control flow with generic exceptions | Untyped failure | The sealed `DomainException` hierarchy |

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
