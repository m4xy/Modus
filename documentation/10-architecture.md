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

## 2. Repository layout

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
```

### 2.1 What goes where — the decision table

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

If you cannot place your code using this table, you have discovered a gap. Do not guess:
add a row here in the same pull request, with a rationale.

---

## 3. Bounded contexts

All six live as top-level packages under `com.modus.core.<context>` in **both**
`core-domain` and `core-application`. A context never imports another context's internals.
Cross-context communication is by **domain event** or by an explicitly declared
**anti-corruption port**.

| Context | Owns | Key aggregates | Publishes | Consumes |
|---|---|---|---|---|
| `identity` | Actors, credentials, permission grants, sessions | `Actor`, `PermissionGrant` | `ActorRegistered`, `GrantIssued`, `GrantRevoked` | — |
| `domainmgmt` | Domains, module installation, module visibility, per-domain process definitions | `Domain`, `ModuleInstallation` | `DomainCreated`, `ModuleInstalled`, `ModuleUninstalled`, `ProcessDefinitionChanged` | `GrantRevoked` |
| `work` | Epics, stories, work items, state machines, definitions of done | `WorkItem` (root), `Epic` | `WorkItemCreated`, `WorkItemTransitioned`, `WorkItemClosed` | `ProcessDefinitionChanged` |
| `memory` | Evidence-backed durable memories at domain / epic / story scope | `Memory`, `EvidenceRecord` | `MemoryRecorded`, `MemoryInvalidated` | `WorkItemClosed`, `AgentRunCompleted` |
| `execution` | Triggers, agent runs, output streams, context-budget accounting | `Trigger`, `AgentRun` | `AgentRunStarted`, `AgentRunOutput`, `AgentRunCompleted`, `ContextBudgetExceeded` | `WorkItemTransitioned`, `MemoryRecorded` |
| `cost` | Spend attribution per stage, model/effort price book, skill cost profiles | `SpendLedger`, `CostProfile` | `SpendRecorded`, `BudgetThresholdCrossed` | `AgentRunCompleted` |

### 3.1 Context dependency rules

| Context | MAY import | MUST NOT import |
|---|---|---|
| `identity` | nothing | every other context |
| `domainmgmt` | `identity` (ids and permission checks only) | `work`, `memory`, `execution`, `cost` |
| `work` | `identity`, `domainmgmt` | `memory`, `execution`, `cost` |
| `memory` | `identity`, `domainmgmt`, `work` (ids only) | `execution`, `cost` |
| `execution` | `identity`, `domainmgmt`, `work` (ids only) | `cost`, `memory` internals |
| `cost` | `identity`, `domainmgmt` | `work`, `memory`, `execution` |

"ids only" means: the identifier value object, nothing else. `work` may hold a
`DomainId`; it may not hold a `Domain`.

**Enforced by:** ArchUnit `SlicesRuleDefinition` over `com.modus.core.(*)..` with the
explicit allowlist above, plus a global "no cycles between slices" rule.

---

## 4. Dependency rules — machine-readable

This is the source table an ArchUnit test is derived from. One row per (module, rule).
`ALLOW` rows are exhaustive: anything not listed is denied.

### 4.1 Gradle module dependencies

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
| `modules:*` | `org.springframework.*` | ALLOW |
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
| `AggregatesAreSealedOrFinal` | Aggregate roots are `final` (Kotlin default) — no `open` aggregate. |
| `EventsAreDataClasses` | Every type in `..domain.event..` is a `data class` and every property is `val`. |
| `PortsAreInterfaces` | Every type in `..domain.port..` is an `interface` with no default implementations that perform IO. |

### 4.3 Adapter rules

| Rule | Detail |
|---|---|
| `AdaptersImplementPorts` | Every class in `..adapter..` annotated `@Component`/`@Repository`/`@Service` implements at least one `core` port, or is a Spring plumbing type on a named allowlist. |
| `DomainTypesDoNotEscape` | No `core-domain` type appears in an `adapter-rest` controller signature — DTOs only. |
| `NoDtoInCore` | No type named `*Dto`, `*Request`, `*Response` exists under `core/`. |
| `ControllersAreDomainScoped` | Every `@RequestMapping`/`@GetMapping`/… path starts with `/domains/{domainId}` (allowlist: `/auth/**`, `/health`, `/openapi.json`). |
| `NoFieldInjection` | No `@Autowired` on a field or setter anywhere. Constructor injection only. |

**Enforced by:** ArchUnit tests in `build-logic`'s `modus.archunit` convention plugin,
applied to every Kotlin module. The tests fail the `check` task.

---

## 5. The `/domains/{domainId}` API root convention

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

The only non-domain-scoped routes, and the complete list of them:

```
/auth/**            login, token exchange, session
/domains            list domains this actor can see; create a domain
/health             liveness
/openapi.json       API description, filtered to what the actor may see
```

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

**Enforced by:** the `ControllersAreDomainScoped` ArchUnit rule; an integration-test
suite (`DomainIsolationIT`) that, for every registered route, asserts a `404` for an
actor without a grant; and a Playwright test asserting the backoffice never renders a
module the session cannot reach.

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

---

## 7. Module system

A Modus Module is a Gradle module under `modules/` that:

1. Declares a `ModuleDescriptor` (stable id, version, the resource kinds it serves, the
   permissions it defines, the routes it contributes).
2. Depends only on `core-domain`, `core-application`, and adapter **ports** — never an
   adapter implementation. A module that needs to persist uses the repository ports.
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
