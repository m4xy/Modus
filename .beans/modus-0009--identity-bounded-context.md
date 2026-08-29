---
# modus-0009
title: The identity bounded context
status: in-progress
type: feature
priority: high
created_at: 2026-08-29T00:00:00Z
---

# The identity bounded context

The first domain model in `core-domain`. `identity` goes first because every other
bounded context inherits its authorisation model: Modus is multi-tenant by domain, the
REST root is `/domains/{domainId}`, and a permission grants an actor access to specific
domains (`doc:10-architecture#domain-root-convention`).

## Scope

Owned: `core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/identity/**`,
`uk/m4xy/modus/core/domain/DomainEvent.kt`, `BoundedContexts.kt`,
`core/core-domain/src/test/**`, `config/coverage/baseline.tsv`. Not owned: every other
bounded context, `core-application`, `adapters/**`, `modules/**`, `app/**`,
`backoffice/**`, `e2e/**`, `documentation/**`.

Persistence is a later bean: this bean declares ports and implements none.

## Success criteria and evidence

### 1. The model is placed by kind, per `doc:20-ddd-practices#ports-and-adapters` §5.1

| package | types |
|---|---|
| `identity.published` | `ActorId`, `DomainId`, `GrantId`, `Capability`, `ActorKind` |
| `identity.event` | `ActorRegistered`, `GrantIssued`, `GrantRevoked` |
| `identity.aggregate` | `Actor`, `PermissionGrant` |
| `identity.port` | `ActorRepository`, `PermissionGrantRepository` |
| `identity` | `AccessDecision`, `PermissionResolver` |

Aggregates depend only on `published` and `event`; `AccessDecision` and
`PermissionResolver` depend on the aggregates. Both value-object invariants are enforced
in `init`, both aggregates have a private constructor and a named factory, and time
arrives as an `Instant` parameter — the domain never reads a clock.

```
cmd:      ./gradlew :architecture-tests:test
observed: BUILD SUCCESSFUL
```

`rule:archunit/thereAreNoPackageCycles` slices on `uk.m4xy.modus.(**)`, so the five
sub-packages are five slices. The dependency direction above is what keeps them acyclic,
and it is why `BoundedContexts` now names `identity` as a literal: an edge from
`uk.m4xy.modus.core.domain` into the context would close a cycle back through
`identity.event`, which depends on `DomainEvent`.

### 2. Ports are declared, and nothing implements them

```
cmd:      grep -rln "ActorRepository\|PermissionGrantRepository" --include=*.kt .
observed: core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/identity/port/ActorRepository.kt
          core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/identity/port/PermissionGrantRepository.kt
```

### 3. 404-not-403 is expressible in the domain, not only in the REST layer

`AccessDecision` is a sealed class of three objects, each carrying `domainIsVisible`.
`DomainNotVisible` is the only denial that leaves the domain invisible, so a transport
maps `!domainIsVisible` to `404` and a visible denial to `403` with no `else` branch and
no `if` of its own (`doc:00-constitution#domain-scoping`).

```
planted:  AccessDecision.DomainNotVisible domainIsVisible = false -> true
cmd:      ./gradlew :core-domain:test
observed: FAILED: denies without revealing the domain when the actor holds no grant on it
          org.opentest4j.AssertionFailedError: expected:<false> but was:<true>
          FAILED: denies without revealing the domain when the grant belongs to another actor
          org.opentest4j.AssertionFailedError: expected:<false> but was:<true>
post:     reverted; BUILD SUCCESSFUL

planted:  AccessDecision.CapabilityNotGranted domainIsVisible = true -> false
observed: FAILED: denies while revealing the domain when the actor can see it but lacks the capability
          org.opentest4j.AssertionFailedError: expected:<true> but was:<false>
post:     reverted; BUILD SUCCESSFUL
```

### 4. Fail closed

`PermissionResolver.decide` derives every answer from grants that are present, live and
covering. Absence, revocation, a grant for another actor, a grant on another domain and a
grant set the caller could not read are all the same input — an empty covering list — and
all deny as `DomainNotVisible`. `PermissionGrant.permits` returns `false` once revoked.

```
planted:  PermissionResolver: covering.isEmpty() -> CapabilityNotGranted
cmd:      ./gradlew :core-domain:test
observed: FAILED: denies on an empty grant set, which is also what an unreadable store looks like
          org.opentest4j.AssertionFailedError:
            expected:<...AccessDecision$DomainNotVisible@20b829d5> but was:<...CapabilityNotGranted>
          FAILED: denies without revealing the domain when the only grant was revoked
          FAILED: denies without revealing the domain when the actor holds no grant on it
          FAILED: a registered actor can reach no domain until a grant says otherwise
post:     reverted; BUILD SUCCESSFUL

planted:  PermissionGrant.permits: !revoked && capability in capabilities -> capability in capabilities
observed: FAILED: a revoked grant covers nothing and permits nothing
          org.opentest4j.AssertionFailedError: expected:<false> but was:<true>
post:     reverted; BUILD SUCCESSFUL
```

### 5. No global admin, and the bootstrap path is the one the docs already state

`PermissionGrant` names exactly one `DomainId`; there is no wildcard grant, no
cross-domain grant and no role type. `Actor` carries no authority at all, so registering
one can never widen access — which is what makes the bootstrap path of
`doc:10-architecture#domain-root-convention` §5.5 safe. Grant administration is a read on
one domain (`PermissionGrantRepository.grantsOn`), subject to the same rule as everything
else.

```
planted:  PermissionGrant.covers: heldBy(actor) && domain == domainId -> heldBy(actor)
cmd:      ./gradlew :core-domain:test
observed: FAILED: covers only the one actor and the one domain it names
          org.opentest4j.AssertionFailedError: expected:<false> but was:<true>
          FAILED: denies without revealing the domain when the actor holds no grant on it
          FAILED: unions the capabilities of every covering grant and of no other
post:     reverted; BUILD SUCCESSFUL
```

### 6. The capability vocabulary is the backoffice's, not an invented one

`Capability` validates the `<resource>.<action>` shape and nothing else; the membership is
open because a Modus Module declares the capabilities it defines
(`doc:10-architecture#module-system`). The test asserts every string
`backoffice/src/api/types.ts` declares is accepted.

```
planted:  Capability SHAPE tightened to Regex("^[a-z][a-z0-9-]*\.read$")
cmd:      ./gradlew :core-domain:test
observed: FAILED: accepts every capability the backoffice renders today
          java.lang.IllegalArgumentException: capability must be '<resource>.<action>': 'work.write'
post:     reverted; BUILD SUCCESSFUL
```

### 7. The provisional marker is gone

`IdentityContext` is deleted. `BoundedContexts.names` keeps six entries; its existing test
still passes.

### 8. Every test is load-bearing

32 tests, 30 planted mutations, every mutation observed to fail the test whose name
describes the broken behaviour, every mutation reverted. The per-test table is in the pull
request body's `verify` block (`doc:35-testing#load-bearing-evidence`).

### 9. Coverage moved, and the ratchet recorded it

```
cmd:      ./gradlew coverageBaselineWrite
observed: :core-domain                   0 0 33 0 -> 0 0 579 44
```

No downward write, so no `-Pcoverage.regress` was needed. 579 covered instructions and 44
covered branches with none missed: the new production code is fully exercised, including
both outcomes of every `require` and `check`.

### 10. The purity rules now guard two packages

The second row of `doc:35-testing#gaps` narrows: `unit-test-packages.txt` gains
`uk.m4xy.modus.core.domain.identity`, and `rule:archunit/everyUnitTestPackageIsAnalysed`
passes with both packages imported.

```
cmd:      cat .../unit-test-packages.txt
observed: uk.m4xy.modus.core.domain
          uk.m4xy.modus.core.domain.identity
```

### 11. The gate is green

See the `verify` block of the pull request.

## Decisions the documents did not already settle

| decision | reason |
|---|---|
| `DomainEvent` lives in `uk.m4xy.modus.core.domain` | `doc:20-ddd-practices#domain-events` names the type but not its package. It is shared kernel, referenced by every context's events, and a per-context copy would make a cross-context handler impossible to type |
| all five identity value objects are published | `ActorKind` and `Capability` appear in event signatures, which publishes them by `doc:10-architecture#bounded-contexts` §3.1's own rule; the ids are published by definition. No unpublished value object remains, which is also what keeps `aggregate` from depending on the context root |
| `Actor` carries no display name | a display name is a profile concern with no invariant. `doc:20-ddd-practices` §2.3 lists it in the boundary; it is deferred rather than modelled as an anaemic field, and credentials with it |
| `PermissionGrant` has no `narrowTo` | `doc:10-architecture#domain-root-convention` §5.5 names issue, narrow and revoke. Narrowing has no consumer until grant administration lands and would ship untested behaviour |
| a revoked grant mutates in place rather than returning a new instance | a held reference must stop answering `permits`; a copy-on-revoke leaves the old one answering `true` |
| `AccessDecision` is a sealed class with `isPermitted`/`domainIsVisible`, not an exception | the authorisation decision is a value the transport maps, not control flow. `doc:20-ddd-practices#invariants` §7.2 reserves named exceptions for business rules the caller surfaces; a denial is the expected outcome of a check, not an exceptional one |
