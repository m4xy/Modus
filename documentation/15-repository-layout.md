---
id: doc:15-repository-layout
title: Repository layout and this repository's own architecture rules
status: active
superseded_by: null
read_when:
  - path: core/**
  - path: adapters/**
  - path: app/**
  - path: architecture-tests/**
  - path: build-logic/**
  - path: settings.gradle.kts
  - task: (add|new|move|rename|delete|split).{0,40}(class|package|adapter|port|bounded context|gradle module|endpoint)
  - task: layering|dependency rule|repository layout|where does it go|package rule|trigger|testing architecture|which tier
provides:
  - doc:15-repository-layout#repository-layout
  - doc:15-repository-layout#placement-table
  - doc:15-repository-layout#tiers
  - doc:15-repository-layout#core-package-rules
  - doc:15-repository-layout#adapter-rules
  - doc:15-repository-layout#cross-cutting-flows
  - doc:15-repository-layout#extending
depends_on: [doc:00-constitution, doc:10-architecture, doc:20-ddd-practices, doc:60-cost-model]
---

# 15 — Repository layout and this repository's own architecture rules

Read this before adding a class, a package, a Gradle module, a bounded context, or an
adapter to this repository. Nothing here binds a third-party Module author; the contract
that does is `doc:10-architecture`.

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
.beans/                                 work items — the modus domain's own store
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

### 2.2 Which tier <a id="tiers"></a>

Placement answers *where*; it does not answer *who receives it*. Three tiers share this tree
and `adr:0006-framework-boundary#classification` is their single normative statement, one
row per file. The rows below are its rendering over this tree; on disagreement it wins.

| tier | receives it | in this tree |
|---|---|---|
| 1 — framework | every tenant | `core/`, `adapters/`, `modules/`, `app/`, `doc:10-architecture` |
| 2 — build discipline | nobody outside this repository | `build-logic/`, `architecture-tests/`, `config/`, this document |
| 3 — the `modus` domain's SDLC | one domain, which happens to be this one | `.beans/`, `tools/`, `AGENTS.md`, the pull-request template |

The test, stated once in `adr:0006-framework-boundary#the-test`: tier 1 if a tenant's running
system still needs it once this repository is deleted; tier 3 if another domain adopting
Modus would plausibly want it different; tier 2 otherwise.

---

## 4. Dependency rules — machine-readable

The Gradle module table is `doc:10-architecture#module-dependencies` §4.1. These are the
package- and adapter-level rules that sit under it.

### 4.2 Package-level rules inside `core-domain` <a id="core-package-rules"></a>

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
| `PublishedLanguageIsLeaf` | No type in `..domain.event..` or `..domain.published..` depends on anything beyond the Kotlin stdlib, `java.time` types, its own context's `..domain.published..`, and the shared kernel. It decides only what reaches bytecode — a `@JvmInline value class` held in a plain field erases to its underlying type and leaves no edge to see — so it is paired with `PublishedLanguageSourceIsLeaf`, which reads source and has no such blind spot. Neither replaces the other. This is the rule §3.1 rests on. The shared kernel is the whole of the exemption and its members are named in the rule, never matched by package: `DomainEvent`, without which every context would declare an identical marker of its own and there would be no type to dispatch a cross-context event as; and `DomainId`, without which every context would declare its own tenant identifier and no two would be equal (`adr:0004-domain-id-shared-kernel`). |
| `PublishedLanguageSourceIsLeaf` | No Kotlin **source** file under `..domain.<ctx>.published..` or `..domain.<ctx>.event..` names another context's package, in an import or a qualified name, outside comments. Source is where an erased reference is still a reference: the identical violation that this rule rejects leaves `PublishedLanguageIsLeaf` green (`bean:0034`). |
| `SharedKernelIsLeaf` | The shared kernel depends on nothing beyond the Kotlin stdlib and `java.time`. Every context imports it, so a dependency added there is one every context inherits unseen. Scoped on the outermost enclosing class, because a `private companion object` and a top-level `private val` both generate classes the source does not name. |
| `PortsAreInterfaces` | Every type in `..domain.port..` is an `interface` with no default implementations that perform IO. |

### 4.3 Adapter rules <a id="adapter-rules"></a>

| Rule | Detail |
|---|---|
| `AdaptersImplementPorts` | Every class in `..adapter..` annotated `@Component`/`@Repository`/`@Service` implements at least one `core` port, or is a Spring plumbing type on a named allowlist. |
| `DomainTypesDoNotEscape` | No `core-domain` type appears in an `adapter-rest` controller signature — DTOs only. |
| `NoDtoInCore` | No type named `*Dto`, `*Request`, `*Response` exists under `core/`. |
| `ControllersAreDomainScoped` | Every `@RequestMapping`/`@GetMapping`/… path starts with `/domains/{domainId}`, unless it matches the **non-domain-scoped route allowlist** in `doc:10-architecture#domain-root-convention` §5.1. The rule reads that list; it does not restate it. |
| `NoFieldInjection` | No `@Autowired` on a field or setter anywhere. Constructor injection only. |

**Enforcement gap (§4.2):** five of the thirteen rules in §4.2 do not exist either —
`EventsAreDataClasses`, `PortsAreInterfaces`, `NoAmbientRandom`, `NoAmbientConcurrency` and
`NoReflection`. `architecture-tests` implements the other eight, and `domainIsFrameworkFree`
covers the first four rows as one rule rather than four. Until they exist, an event that is
not a data class, a port that is not an interface, and a call to `UUID.randomUUID()` in the
domain all merge green. `bean:0027` carries the audit; `bean:0034` carries the consequence
that `PublishedLanguageIsLeaf` cannot lean on `EventsAreDataClasses` to see a value class in
an erased position — which `bean:0034` has since closed with `PublishedLanguageSourceIsLeaf`,
reading source rather than bytecode.

**Enforcement gap (§4.3):** none of the five rules above exist. `DomainTypesDoNotEscape`,
`NoDtoInCore` and `ControllersAreDomainScoped` are `adapter-rest`-specific and every
adapter today, including it, is a placeholder with no controllers to check; `bean:0018`
carries them. `AdaptersImplementPorts` and `NoFieldInjection` apply to any adapter;
`bean:0017` is the first to implement one and is the first point either can fire.

---

## 6. Cross-cutting flows <a id="cross-cutting-flows"></a>

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

## 9. Extending the architecture <a id="extending"></a>

Adding a Gradle module, a bounded context, or a non-domain-scoped route each require an
ADR in `documentation/adr/`. Everything else — a new aggregate, a new port, a new
adapter implementation of an existing port — is ordinary work and needs only a work item.
