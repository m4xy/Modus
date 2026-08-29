---
id: doc:10-architecture
title: Architecture and module layout
status: active
superseded_by: null
read_when:
  - path: core/**
  - path: adapters/**
  - path: modules/**
  - path: app/**
  - path: architecture-tests/**
  - path: settings.gradle.kts
  - task: (add|new|move|rename|delete|split).{0,40}(module|class|endpoint|package|adapter|port|bounded context|route)
  - task: layering|dependency rule|repository layout|domain root|trigger
provides:
  - doc:10-architecture#repository-layout
  - doc:10-architecture#placement-table
  - doc:10-architecture#bounded-contexts
  - doc:10-architecture#module-dependencies
  - doc:10-architecture#domain-root-convention
  - doc:10-architecture#module-system
depends_on: [doc:00-constitution, doc:20-ddd-practices, doc:30-code-style, doc:40-durability, doc:60-cost-model]
---

# 10 — Architecture

Read this before adding a module, a class, a package, or an endpoint.

---

## 1. Shape

Modus is a **Gradle multi-module Kotlin/Spring Boot** application built as a
**hexagon** (ports and adapters) around a framework-free domain core, with
**domain-scoped modularity** layered on top.

Three orthogonal ideas, do not confuse them:

| Term | Meaning | Lives in |
|---|---|---|
| **Gradle module** | A build unit. `core-domain`, `adapter-rest`, … | The directory tree below |
| **Bounded context** | A DDD language boundary. `identity`, `work`, `cost`, … | Packages inside `core/*` |
| **Modus Module** | A *runtime, product-level* capability installed into a domain by a user | `modules/module-*`, plus per-domain install records |

A Gradle module named `modules/module-beans` implements a Modus Module. A bounded
context named `work` is a package. These are different things with unfortunately
similar names; always qualify which you mean in code comments and commit scopes.

---

## 2. Repository layout <a id="repository-layout"></a>

```
build-logic/                            convention plugins (style, arch, test)
core/
  core-domain/                          pure Kotlin — aggregates, VOs, domain events, ports
  core-application/                     use cases; depends on core-domain only
adapters/
  adapter-persistence-flatfile/         durable flat-file store
  adapter-rest/                         /domains/{domainId} REST + SSE/WS
  adapter-agent-claude/                 claude-code supervision + output streaming
  adapter-vcs-git/                      git-backed repository operations
modules/
  module-beans/                         work tracking (per-domain installable)
  module-cost/                          LLM spend tracking
app/
  modus-server/                         Spring Boot wiring only
backoffice/                             React + Vite + TypeScript
e2e/                                    Playwright
documentation/                          this package
beans/                                  work items
tools/                                  repository-wide checks that are not Kotlin rules
```

### 2.1 What goes where — the decision table <a id="placement-table"></a>

| You are writing… | It goes in… |
|---|---|
| An aggregate, entity, value object, domain event, or invariant | `core/core-domain` |
| An outbound port interface (repository, clock, id generator, agent launcher) | `core/core-domain` |
| A use case that orchestrates one or more aggregates and commits once | `core/core-application` |
| An inbound port (use-case interface the transports call) | `core/core-application` |
| A file layout, serialisation format, or locking strategy | `adapters/adapter-persistence-flatfile` |
| An HTTP controller, DTO, SSE/WebSocket handler, or an OpenAPI annotation | `adapters/adapter-rest` |
| Process supervision, stdout parsing, token/cost extraction from a claude-code run | `adapters/adapter-agent-claude` |
| Branch, commit, diff, worktree operations | `adapters/adapter-vcs-git` |
| A user-installable capability that some domains have and others do not | `modules/module-*` |
| A Spring `@Configuration`, bean definition, or `application.yaml` | `app/modus-server` |
| A React component, route, or store | `backoffice/` |
| A user-flow assertion | `e2e/` |
| A ktlint/Detekt/ArchUnit rule or a shared Gradle convention | `build-logic/` |
| A repository-wide check over files rather than over Kotlin (`docs-lint`) | `tools/`, invoked by a task in the root `build.gradle.kts` |

If you cannot place your code using this table, you have discovered a gap. Do not guess:
add a row here in the same pull request, with a rationale.

---

## 3. Bounded contexts <a id="bounded-contexts"></a>

All six live as top-level packages under `com.modus.core.<context>` in **both**
`core-domain` and `core-application`. A context never imports another context's internals.
Cross-context communication is by **domain event** or by an explicitly declared
**anti-corruption port**.

The `Consumes` column below is a **published-language** dependency, not an internals
dependency: consuming `AgentRunCompleted` means importing the event type from
`execution`'s published package and nothing else. §3.1 states the rule that makes this
expressible as an ArchUnit allowlist.

| Context | Owns | Key aggregates | Publishes | Consumes |
|---|---|---|---|---|
| `identity` | Actors, credentials, permission grants, sessions | `Actor`, `PermissionGrant` | `ActorRegistered`, `GrantIssued`, `GrantRevoked` | — |
| `domainmgmt` | Domains, module installation, module visibility, per-domain process definitions | `Domain`, `ModuleInstallation` | `DomainCreated`, `ModuleInstalled`, `ModuleUninstalled`, `ProcessDefinitionChanged` | `GrantRevoked` |
| `work` | Epics, stories, work items, state machines, definitions of done | `WorkItem` (root), `Epic` | `WorkItemCreated`, `WorkItemTransitioned`, `WorkItemClosed` | `ProcessDefinitionChanged` |
| `memory` | Evidence-backed durable memories at domain / epic / story scope | `Memory`, `EvidenceRecord` | `MemoryRecorded`, `MemoryInvalidated` | `WorkItemClosed`, `AgentRunCompleted` |
| `execution` | Triggers, agent runs, output streams, context-budget accounting | `Trigger`, `AgentRun` | `AgentRunStarted`, `AgentRunOutput`, `AgentRunCompleted`, `ContextBudgetExceeded` | `WorkItemTransitioned`, `MemoryRecorded` |
| `cost` | Spend attribution per stage, model/effort price book, skill cost profiles | `SpendLedger`, `CostProfile` | `SpendRecorded`, `BudgetThresholdCrossed` | `AgentRunCompleted` |

### 3.1 Context dependency rules

Every context is split into a **published language** and its **internals**. This split is
the whole rule; get it right and the allowlist writes itself.

| Part | Packages | Who may import it |
|---|---|---|
| **Published language** | `com.modus.core.<ctx>.domain.event..` (domain events) and `com.modus.core.<ctx>.domain.published..` (identifier value objects, plus any value object that appears in an event's signature — `WorkItemState`, `RunStatus`, …) | Any context named in the table below |
| **Internals** | Every other package of the context — aggregates, ports, use cases, services, and every value object not published | **Nobody.** No allowlist, no exception. |

The published-language packages are **leaves**: a type in `..domain.event..` or
`..domain.published..` may reference only the Kotlin stdlib, `java.time` types, its
own context's `..domain.published..`, and the **shared kernel** — `DomainEvent` and
`DomainId`, which belong to no context (`adr:0004-domain-id-shared-kernel`). It may not
reference an aggregate, a port, a use case, or another context. That is what makes depending on a published package safe, and
it is what keeps the *package* graph acyclic even where the *context* graph is not.

The rule has a useful consequence: **putting a type into an event's signature publishes
it.** If you want to add a property to a cross-context event, you must first move its
type into `..domain.published..`, which is a visible, reviewable act — and a breaking
change to a published contract, so it needs an ADR (`20-ddd-practices.md` §4.1.5).

| Context | MAY import the published language of | MUST NOT import, in any form |
|---|---|---|
| `identity` | — | every other context |
| `domainmgmt` | `identity` | `work`, `memory`, `execution`, `cost` |
| `work` | `identity`, `domainmgmt` | `memory`, `execution`, `cost` |
| `memory` | `identity`, `domainmgmt`, `work`, `execution` | `cost` |
| `execution` | `identity`, `domainmgmt`, `work`, `memory` | `cost` |
| `cost` | `identity`, `domainmgmt`, `work`, `execution` | `memory` |

Every row matches the `Consumes` column of §3 exactly, and the table above is the only
normative statement of it. `cost` imports `work`'s ids because a spend record carries
`workItemId` and `epicId` (`60-cost-model.md` §3.2); it imports `execution`'s events
because it consumes `AgentRunCompleted` (§6.1). Neither is an internals dependency.

`memory` and `execution` each import the other's published language. That is a cycle at
context granularity and it is **intentional**: both consume each other's events, and
neither can see the other's internals. It is not a cycle in the package graph, because
published packages are leaves.

**Enforced by:** two ArchUnit rules, both derived directly from the tables above.
`ContextInternalsAreSealed` — no type outside `com.modus.core.<ctx>..` may depend on
`com.modus.core.<ctx>..` except on `..domain.event..` or `..domain.published..`.
`PublishedLanguageAllowlist` — a context's dependencies on another context's published
packages are limited to the row above. Plus `PublishedLanguageIsLeaf` (§4.2) and a
"no cycles" rule over the internals slices only, `com.modus.core.(*)..` minus the two
published packages. Assigned to `ContextIsolationRules` (`30-code-style.md` §5).

`PublishedLanguageIsLeaf` and the no-cycles rule are implemented. `PublishedLanguageIsLeaf`
was **vacuous on the cross-context half** until `bean:0032`: `identity` imports no other
context, so the case it exists to catch could not arise. It has since been observed
rejecting one (`adr:0004-domain-id-shared-kernel`).
**Enforcement gap:** `ContextInternalsAreSealed` and `PublishedLanguageAllowlist` are not.
Both compare one context against another and `identity` is the only modelled context, so
both would pass on an empty set of dependencies today — an implementation now would be a
rule that cannot fail. `bean:0009` records this and `bean:0023` closes it, on the second
bounded context (`bean:0012`) — the first point at which either rule can be shown to fire.

---

## 4. Dependency rules — machine-readable

This is the source table an ArchUnit test is derived from. One row per (module, rule).
`ALLOW` rows are exhaustive: anything not listed is denied.

### 4.1 Gradle module dependencies <a id="module-dependencies"></a>

| From | To | Rule |
|---|---|---|
| `core:core-domain` | Kotlin stdlib | ALLOW |
| `core:core-domain` | `java.time.*` (types only, no `now()`) | ALLOW |
| `core:core-domain` | anything else | DENY |
| `core:core-application` | `core:core-domain` | ALLOW |
| `core:core-application` | `kotlinx.coroutines` | ALLOW |
| `core:core-application` | anything else | DENY |
| `adapters:*` | `core:core-domain`, `core:core-application` | ALLOW |
| `adapters:*` | `org.springframework.*`, its own third-party libs | ALLOW |
| `adapters:*` | another `adapters:*` | DENY |
| `adapters:*` | `modules:*`, `app:*` | DENY |
| `modules:*` | `core:core-domain`, `core:core-application` | ALLOW |
| `modules:*` | `org.springframework.*`, its own third-party libs | ALLOW |
| `modules:*` | another `modules:*` | DENY |
| `modules:*` | `adapters:*` | DENY |
| `app:modus-server` | any Gradle module | ALLOW |
| any Kotlin module | `java.sql`, `javax.sql`, `jakarta.persistence`, `org.hibernate`, `org.jooq` | DENY |

### 4.2 Package-level rules inside `core-domain`

| Rule | Detail |
|---|---|
| `NoSpringInDomain` | No type under `org.springframework..` may be referenced. |
| `NoJacksonInDomain` | No type under `com.fasterxml.jackson..` may be referenced. |
| `NoLoggingInDomain` | No `org.slf4j..`, `java.util.logging..`, `println`. |
| `NoIoInDomain` | No `java.io..`, `java.nio.file..`, `java.net..`. |
| `NoAmbientTime` | No call to `Instant.now`, `LocalDate.now`, `LocalDateTime.now`, `Clock.systemUTC`, `System.currentTimeMillis`, `System.nanoTime`. |
| `NoAmbientRandom` | No call to `UUID.randomUUID`, `Math.random`, no `kotlin.random.Random.Default`. |
| `NoAmbientConcurrency` | No `java.util.concurrent..`, no `Thread`, no `Dispatchers`. |
| `NoReflection` | No `java.lang.reflect..`, no `::class.java` beyond `equals`/`hashCode` support. |
| `AggregatesAreSealedOrFinal` | Every type in `..domain.aggregate..` is `final` (Kotlin default) — no `open` aggregate. The package convention (`20-ddd-practices.md` §5.1) is what gives this rule a decidable scope. |
| `EventsAreDataClasses` | Every type in `..domain.event..` is a `data class` and every property is `val`. |
| `PublishedLanguageIsLeaf` | No type in `..domain.event..` or `..domain.published..` depends on anything beyond the Kotlin stdlib, `java.time` types, its own context's `..domain.published..`, and the shared kernel. This is the rule §3.1 rests on. The shared kernel is the whole of the exemption and its members are named in the rule, never matched by package: `DomainEvent`, without which every context would declare an identical marker of its own and there would be no type to dispatch a cross-context event as; and `DomainId`, without which every context would declare its own tenant identifier and no two would be equal (`adr:0004-domain-id-shared-kernel`). |
| `SharedKernelIsLeaf` | The shared kernel depends on nothing beyond the Kotlin stdlib and `java.time`. Every context imports it, so a dependency added there is one every context inherits unseen. Scoped on the outermost enclosing class, because a `private companion object` and a top-level `private val` both generate classes the source does not name. |
| `PortsAreInterfaces` | Every type in `..domain.port..` is an `interface` with no default implementations that perform IO. |

### 4.3 Adapter rules

| Rule | Detail |
|---|---|
| `AdaptersImplementPorts` | Every class in `..adapter..` annotated `@Component`/`@Repository`/`@Service` implements at least one `core` port, or is a Spring plumbing type on a named allowlist. |
| `DomainTypesDoNotEscape` | No `core-domain` type appears in an `adapter-rest` controller signature — DTOs only. |
| `NoDtoInCore` | No type named `*Dto`, `*Request`, `*Response` exists under `core/`. |
| `ControllersAreDomainScoped` | Every `@RequestMapping`/`@GetMapping`/… path starts with `/domains/{domainId}`, unless it matches the **non-domain-scoped route allowlist** in §5.1. The rule reads that list; it does not restate it. |
| `NoFieldInjection` | No `@Autowired` on a field or setter anywhere. Constructor injection only. |

**Enforcement gap:** none of the five rules above exist. `DomainTypesDoNotEscape`,
`NoDtoInCore` and `ControllersAreDomainScoped` are `adapter-rest`-specific and every
adapter today, including it, is a placeholder with no controllers to check; `bean:0018`
carries them. `AdaptersImplementPorts` and `NoFieldInjection` apply to any adapter;
`bean:0017` is the first to implement one and is the first point either can fire.

---

## 5. The `/domains/{domainId}` API root convention <a id="domain-root-convention"></a>

### 5.1 The rule

Every resource in the Modus REST API is nested under a domain:

```
/domains/{domainId}/work-items
/domains/{domainId}/work-items/{workItemId}
/domains/{domainId}/epics/{epicId}/memories
/domains/{domainId}/memories/{memoryId}/evidence
/domains/{domainId}/runs
/domains/{domainId}/runs/{runId}/stream         (SSE)
/domains/{domainId}/runs/{runId}/socket         (WebSocket)
/domains/{domainId}/modules
/domains/{domainId}/cost/summary
/domains/{domainId}/skills
```

#### The non-domain-scoped route allowlist

This is the **single normative copy** of the list. `ControllersAreDomainScoped` (§4.3),
the `DomainScopedRoute` Detekt rule (`30-code-style.md` §4) and `00-constitution.md` §8
all cite it by name; none of them restates its members, because three copies of one
allowlist drift. Adding a member requires an ADR (§9).

| Pattern | Match | Covers |
|---|---|---|
| `/auth/**` | prefix | login, token exchange, session |
| `/domains` | **exact** | list domains this actor can see; create a domain |
| `/health` | exact | liveness |
| `/openapi.json` | exact | API description, filtered to what the actor may see |

`/domains` is an exact match, not `/domains/**`: `/domains/{domainId}/…` is domain-scoped
and is covered by the ordinary rule, so the allowlist must not swallow it.

### 5.2 Why

Permissions in Modus are grants of access to *specific domains*. Putting the domain in
the path means:

- Authorisation is a single uniform check on every request, applied by one filter
  rather than scattered across handlers.
- A URL is self-describing and shareable within its permission boundary.
- Multi-tenancy, multi-process and per-domain module visibility fall out for free.

### 5.3 The authorisation contract

Every domain-scoped request passes through exactly one authorisation step, in this order:

1. Resolve the actor from the session or token. No actor → `401`.
2. Resolve `{domainId}`. Does the actor hold **any** grant on this domain?
   - No → `404 Not Found`. **Never `403`.** A `403` reveals that the domain exists.
3. Does the actor's grant cover the *operation* (read / write / admin) on this
   *resource kind*?
   - No, but the actor can see the domain → `403 Forbidden` is correct here; existence
     is already known.
4. Is the resource served by a **Module** installed in this domain, and visible to this
   actor?
   - No → `404`. A module not installed is indistinguishable from a module that does not
     exist.

### 5.4 Per-domain module visibility

- A Modus Module is *installed into* a domain. Installation is a `ModuleInstallation`
  aggregate in `domainmgmt`.
- Domain A may create and install a Module that domain B cannot see, cannot list, cannot
  reach by URL, and cannot discover through the OpenAPI document.
- The OpenAPI document served at `/openapi.json` is **generated per actor** and contains
  only the routes that actor could successfully call. There is no global spec.
- The backoffice navigation is likewise generated from the installed-and-visible module
  set. There is no hardcoded menu.

**Enforcement gap:** none of the three exist — `ControllersAreDomainScoped`, the
`DomainIsolationIT` integration suite, and the module-visibility Playwright assertion.
`bean:0018` carries the ArchUnit rule and `DomainIsolationIT`; `bean:0022` carries the
Playwright test, once the backoffice runs against a live API.

### 5.5 Grant administration

§5.3 says how a grant is *checked*. This says how one is *created*, which is the other
half and is easy to leave unowned.

- A `PermissionGrant` (`identity`) binds one actor to one domain and one scope set.
  Scopes are `read`, `write`, `admin`, optionally narrowed to a resource kind. There is
  no wildcard grant across domains and no global administrator role — a superuser is a
  cross-domain visibility hole, which `00-constitution.md` §8 forbids.
- Grants are administered **inside the domain they concern**, under
  `/domains/{domainId}/grants`, and are therefore subject to the same 404-not-403 rule as
  everything else. An actor with `admin` on a domain may issue, narrow and revoke grants
  on that domain and no other.
- **Bootstrap:** the first actor is created by `app/modus-server` at first start from
  configuration, and the creator of a domain receives an `admin` grant on it in the same
  transaction as `DomainCreated`. There is no other path to a first grant, because every
  other path is a privilege-escalation path.
- Issuing and revoking emit `GrantIssued` / `GrantRevoked` (§3). `domainmgmt` consumes
  `GrantRevoked`; `execution` closes the revoked actor's open runs
  (`20-ddd-practices.md` §7.1, cross-context row).
- Every grant change is appended to the domain's event log and is therefore auditable
  from the store with no extra machinery (`40-durability.md` §3).

**Enforcement gap:** the backoffice grant-administration screens, and the Playwright
assertions that an actor without `admin` cannot reach them, are not specified here. They
are named as a follow-up in `bean:0001`, which owns raising the work item.

---

## 6. Cross-cutting flows

### 6.1 Trigger → agent run → stream

```
Trigger fires (work item transitioned, schedule, webhook, manual prompt from backoffice)
  → execution.AgentRun created (status=pending)          [core-domain]
  → StartAgentRun use case                               [core-application]
  → AgentLauncherPort.launch(spec)                       [port, core-domain]
  → adapter-agent-claude spawns claude-code, supervises  [adapter]
  → each stdout/stderr chunk → AgentRunOutput event
  → appended to the run's NDJSON log                     [adapter-persistence-flatfile]
  → fanned out to SSE / WebSocket subscribers            [adapter-rest]
  → on exit: AgentRunCompleted (exit code, peak context, token usage)
  → cost.SpendRecorded                                   [cost context]
```

The append to the durable log happens **before** the fan-out to subscribers. A subscriber
that reconnects replays from the log by offset; no output is ever only in memory.

### 6.2 Prompting from the backoffice

`POST /domains/{domainId}/runs` with a prompt creates an `AgentRun` and returns its id
immediately. The client then opens `GET /domains/{domainId}/runs/{runId}/stream` (SSE,
the default) or `/socket` (WebSocket, when the client needs to send input mid-run).
Both carry a `Last-Event-ID` / offset so a dropped connection resumes losslessly.

SSE is the default because it is simpler, proxy-friendly, and sufficient for one-way
output. WebSocket is used only where bidirectional interaction is genuinely required.

### 6.3 Triggers

§6.1 names four things that fire a run. A `Trigger` is the aggregate that decides which,
and it is per-domain like every other process concern (`00-constitution.md` §8).

A `Trigger` carries: a `TriggerId`; the `domainId` it belongs to; a **source** — one of
`work-item-transitioned`, `schedule`, `webhook`, `manual`; a **condition** over that
source (a target state for a transition, a cron expression for a schedule, a signed
path for a webhook); a **target spec** (the model, effort and skill the run starts with,
per `60-cost-model.md` §4.4); and `enabled`. Triggers are ordinary domain documents
under `domains/<domainId>/triggers/<triggerId>.md`, so they are editable, diffable and
reviewable like everything else.

Rules, because the interesting cases are all concurrency cases:

| # | Rule |
|---|---|
| 6.3.1 | A trigger fires by **appending a run request**, never by launching a process inline. Firing is a domain decision; launching is an adapter's job. |
| 6.3.2 | **A trigger has at most one in-flight run per target.** If it fires while a run for the same target is in flight, the firing is recorded as `coalesced` against the in-flight run and no second run starts. Two agents on one work item is the most expensive concurrency bug in the system. |
| 6.3.3 | A firing that is refused — coalesced, disabled, or over the domain's budget (`60-cost-model.md` §8) — is still **recorded**. A trigger that silently does nothing is undebuggable. |
| 6.3.4 | Firings are **idempotent per (triggerId, causeId)**. A redelivered webhook or a replayed event fires once. The `causeId` is the event id for event sources and the delivery id for webhooks. |
| 6.3.5 | A trigger MUST NOT fire on an event its own run published. Self-triggering loops are cut at the trigger, not at the budget. |
| 6.3.6 | Disabling a trigger never cancels an in-flight run; it prevents the next firing. Cancelling a run is a separate, explicit operation. |

**Enforcement gap:** the `Trigger` aggregate does not exist yet (`bean:0014`), so neither
the coalescing/idempotence invariants nor the `(triggerId, causeId)` uniqueness check
against the event log exist either. Per-domain trigger configuration in the backoffice is
additionally unspecified; named as a follow-up in `bean:0001`.

---

## 7. Module system <a id="module-system"></a>

A Modus Module is a Gradle module under `modules/` that:

1. Declares a `ModuleDescriptor` (stable id, version, the resource kinds it serves, the
   permissions it defines, the routes it contributes).
2. Depends only on `core-domain` and `core-application` — never on an adapter. Ports are
   declared in `core` and implemented by adapters (`00-constitution.md` §1.2), so a
   module that needs to persist depends on the repository port in `core-domain`; there is
   no such thing as an "adapter port" to depend on. Spring is permitted in a module
   (§4.1) because a module is wired like an adapter, not like the core.
3. Is installable per domain, and does nothing at all in a domain where it is not
   installed. A module MUST NOT register global state, global routes, or global beans
   that are observable from an uninstalled domain.
4. Contributes routes only under `/domains/{domainId}/…`.

`module-beans` (work tracking) and `module-cost` (spend tracking) are the two first-party
modules. They are structurally ordinary modules — they get no privileges a
third-party-authored module would not get. This is deliberate: it keeps the module
boundary honest.

---

## 8. Testing architecture

| Level | Where | What it may touch |
|---|---|---|
| Domain unit tests | `core/core-domain/src/test` | Pure objects. No IO, no Spring, no temp files. Must run in milliseconds. |
| Use-case tests | `core/core-application/src/test` | Domain plus in-memory fakes of ports. No Spring context. |
| Adapter tests | `adapters/*/src/test` | The real adapter against a temp directory / real `git` binary / a fake claude-code process. |
| Module tests | `modules/*/src/test` | The module against in-memory ports. |
| Integration tests | `app/modus-server/src/test` | Full Spring context, real flat-file store in a temp dir, HTTP over a random port. |
| End-to-end | `e2e/` | The running system through a real browser. |

**Rules:** no mocking framework in `core/` — hand-written fakes only; a mock of your own
domain is a design smell. Adapter and integration tests use a per-test temp directory and
delete it on teardown. No test may depend on wall-clock time; inject the `Clock` port.

---

## 9. Extending the architecture

Adding a Gradle module, a bounded context, or a non-domain-scoped route each require an
ADR in `documentation/adr/`. Everything else — a new aggregate, a new port, a new
adapter implementation of an existing port — is ordinary work and needs only a work item.
