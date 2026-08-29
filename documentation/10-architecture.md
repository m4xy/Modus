---
id: doc:10-architecture
title: Architecture and the Module extension contract
status: active
superseded_by: null
read_when:
  - path: modules/**
  - task: (write|author|add|install|publish).{0,40}(modus module|module descriptor)
  - task: extension contract|extension point|third-party module|module system|module visibility|domain-scoped route|non-domain-scoped route|api root convention|authorisation contract
provides:
  - doc:10-architecture#bounded-contexts
  - doc:10-architecture#module-dependencies
  - doc:10-architecture#domain-root-convention
  - doc:10-architecture#module-system
depends_on: [doc:00-constitution, doc:15-repository-layout, doc:20-ddd-practices, doc:30-code-style, doc:40-durability]
---

# 10 — Architecture and the Module extension contract

Read this before writing a Modus Module, or before changing anything a Module can observe.
Everything here binds a third-party Module author. This repository's own tree, package
rules, adapter rules, cross-cutting flows and testing architecture are
`doc:15-repository-layout`, and bind nobody outside this repository.

---

## 1. Shape

Modus is a **Gradle multi-module Kotlin/Spring Boot** application built as a
**hexagon** (ports and adapters) around a framework-free domain core, with
**domain-scoped modularity** layered on top.

Three orthogonal ideas, do not confuse them:

| Term | Meaning | Lives in |
|---|---|---|
| **Gradle module** | A build unit. `core-domain`, `adapter-rest`, … | Its own directory with a `build.gradle.kts`, named in `settings.gradle.kts` |
| **Bounded context** | A DDD language boundary. `identity`, `work`, `cost`, … | Packages inside `core/*` |
| **Modus Module** | A *runtime, product-level* capability installed into a domain by a user | `modules/module-*`, plus per-domain install records |

A Gradle module named `modules/module-beans` implements a Modus Module. A bounded
context named `work` is a package. These are different things with unfortunately
similar names; always qualify which you mean in code comments and commit scopes.

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
because it consumes `AgentRunCompleted` (`doc:15-repository-layout#cross-cutting-flows`
§6.1). Neither is an internals dependency.

`memory` and `execution` each import the other's published language. That is a cycle at
context granularity and it is **intentional**: both consume each other's events, and
neither can see the other's internals. It is not a cycle in the package graph, because
published packages are leaves.

**Enforced by:** two ArchUnit rules, both derived directly from the tables above.
`ContextInternalsAreSealed` — no type outside `com.modus.core.<ctx>..` may depend on
`com.modus.core.<ctx>..` except on `..domain.event..` or `..domain.published..`.
`PublishedLanguageAllowlist` — a context's dependencies on another context's published
packages are limited to the row above. Plus `PublishedLanguageIsLeaf`
(`doc:15-repository-layout#core-package-rules` §4.2) and a
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

This is the **single normative copy** of the list. `ControllersAreDomainScoped`
(`doc:15-repository-layout#adapter-rules` §4.3), the `DomainScopedRoute` Detekt rule
(`30-code-style.md` §4) and `00-constitution.md` §8 all cite it by name; none of them
restates its members, because three copies of one allowlist drift. Adding a member
requires an ADR (`doc:15-repository-layout#extending` §9).

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

## 7. Module system <a id="module-system"></a>

A Modus Module is a Gradle module under `modules/` that:

1. Declares a `ModuleDescriptor` (stable id, version, the resource kinds it serves, the
   permissions it defines, the routes it contributes).
2. Depends only on `core-domain` and `core-application` — never on an adapter. Ports are
   declared in `core` and implemented by adapters (`00-constitution.md` §1.2), so a
   module that needs to persist depends on the repository port in `core-domain`; there is
   no such thing as an "adapter port" to depend on. Spring is permitted in a module
   (§4.1) because a module is wired like an adapter, not like the core. What those ports
   write, and how, is `doc:40-durability`.
3. Is installable per domain, and does nothing at all in a domain where it is not
   installed. A module MUST NOT register global state, global routes, or global beans
   that are observable from an uninstalled domain.
4. Contributes routes only under `/domains/{domainId}/…`.

`module-beans` (work tracking) and `module-cost` (spend tracking) are the two first-party
modules. They are structurally ordinary modules — they get no privileges a
third-party-authored module would not get. This is deliberate: it keeps the module
boundary honest.
