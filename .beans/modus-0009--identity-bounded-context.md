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

No downward write, so no `-Pcoverage.regress` was needed. Missed instructions and missed
branches are both `0`: the new production code is fully exercised, including both outcomes
of every `require` and `check`. The **branch** figure is what carries that meaning — of the
579 covered instructions roughly 242 are synthetics that no test could fail to cover
(`<clinit>` 62, `<init>` ~105, trivial getters ~75), so the module-wide instruction count
is a regression trip-wire and not a behavioural claim.

Superseded by the review cycle below, which moves the row to `0 0 618 38` and adds a real
behavioural floor: 100% `BRANCH` on `..domain.aggregate`.

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

---

## Review cycle — pull request #9

Eight threads, two of them privilege-escalation defects. Every thread ended in a change.
`documentation/**` and `build-logic/**` were outside this bean's Scope above; threads 7
and 8 moved both, deliberately, because each is a rule this bean is the first to make
binding. The Scope line is amended by this section rather than silently.

### 1. `PermissionGrant.capabilities` handed out the live internal set — privilege escalation

The property returned the backing collection. Kotlin's `Set` is a read-only *view*, not an
immutable type, so for two or more capabilities the caller down-casts it to `MutableSet`
and adds one nobody granted. Reproduced verbatim before the fix:

```
cmd:      PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, listOf(g))  // CapabilityNotGranted
          (g.capabilities as MutableSet<Capability>).add(AGENTS_RUN)
          PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, listOf(g))
observed: AssertionFailedError: expected:<...AccessDecision$CapabilityNotGranted@733f1395>
          but was:<...AccessDecision$Permitted@3e9beef2>
```

Fixed by holding the capabilities as a `private val granted: List<Capability>` and copying
on the way out — `capabilities get() = granted.toSet()` — symmetric with `pendingEvents`.
`GrantIssued` is constructed from a second copy, so the event's set is not an alias of the
grant's either.

The whole suite missed this because every fixture carried exactly one capability, where
`toSet()` degenerates to the immutable `setOf(x)` and the cast throws. `IdentityFixture`'s
default grant now carries **two**, and every test that does not assert on capability
content inherits it; the uniformity was the defect, not just the getter.

### 2. Two aliases of one `GrantId` failed open — privilege escalation

`PermissionGrant` had reference identity, so a `Set<PermissionGrant>` held a live alias
beside a revoked one and the live one outvoted the revocation. Reproduced verbatim before
the fix, with the reviewer's exact input:

```
cmd:      fresh.revoke(AT); PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, setOf(fresh, stale))
observed: AssertionFailedError: expected:<...AccessDecision$DomainNotVisible@26275b46>
          but was:<...AccessDecision$Permitted@3e9beef2>
```

Fixed in two places, because either alone is insufficient:

1. `PermissionGrant` is an entity, so `equals`/`hashCode` are on `id` alone. A
   `Set<PermissionGrant>` can no longer represent two instances of one grant.
2. **The duplicate-id rule, stated:** `PermissionResolver` groups by `GrantId` first, and
   a grant id counts only when *every* instance under it qualifies. **Any revoked
   instance of an id denies the whole id**, in either order, whatever the collection type.
   An ambiguous read is a denial, never a permit.

Equality alone is not enough: it makes a `Set` collapse to an arbitrary winner, which is
*worse* than the original for the ordering where the stale alias wins. Grouping alone is
not enough either, because a `Set` deduplicates before the resolver is called — which is
why `PermissionGrantRepository`'s three reads now return `List` rather than `Set`, with
the MUST beside the existing one: at most one instance per `GrantId`, and that instance is
the current one. The `List` is what carries a violation through to the rule that denies it.

### 3-5. Three surviving mutants, each re-planted and confirmed killed

| # | invariant | new test | mutant re-planted | observed |
|---|---|---|---|---|
| 3 | `Capability` is exactly one `.`, both halves lower-kebab | `refuses a capability carrying more than one dot`, `refuses a capability whose halves are not kebab`, `accepts a capability whose halves are lower kebab` | `SHAPE` relaxed to `^[a-z][a-z0-9.-]*\.[a-z][a-z0-9-]*$` | both rejecting tests: `AssertionFailedError: Expected exception java.lang.IllegalArgumentException but no exception was thrown.` |
| 4 | `DomainId` is lower case | `refuses a domain id that is not lower case` | `SLUG` relaxed to `^[a-zA-Z0-9][a-zA-Z0-9-]{1,62}[a-zA-Z0-9]$` | `AssertionFailedError: Expected exception java.lang.IllegalArgumentException but no exception was thrown.` |
| 5 | `effectiveCapabilities` excludes revoked grants | `a revoked grant contributes no effective capability` | `covers(...)` replaced by `it.actorId == actorId && it.domainId == domainId` | `AssertionFailedError: expected:<[]> but was:<[Capability(value=agents.run), Capability(value=cost.read)]>` |

`SHAPE` is now `^[a-z][a-z0-9]*(-[a-z0-9]+)*\.[a-z][a-z0-9]*(-[a-z0-9]+)*$`, which also
refuses the trailing hyphen the reviewer found (`agents-.run` constructed before).

Row 5 is treated as security, not hygiene: the leak-freedom argument rests on
`effectiveCapabilities` returning `emptySet()` for a domain the actor may no longer know
exists. A caller rendering "what can I do here" from a non-empty answer admits the domain,
which is a `404`-not-`403` leak.

### 6. `ActorId` and `GrantId` accepted what the KDoc promised they would not

`isNotBlank() && none { isWhitespace() }` accepted `../../etc/passwd`, NUL, U+200B, upper
case and unbounded length — and the KDoc authorises an adapter to use both unencoded as a
file name. Both now require
`^[a-z0-9]([a-z0-9._-]{0,62}[a-z0-9])?$`: 1..64 characters, no traversal, no invisible
alias, no case collision on the case-insensitive volumes the flat-file store runs on. The
KDoc and the `require` now say the same thing.

### 7. Two documented rules that did not exist — implemented, not documented away

`PublishedLanguageIsLeaf` and `AggregatesAreSealedOrFinal` were in `doc:10` §4.2 with no
implementation, and as written the first would have failed this PR's
`ActorRegistered : DomainEvent`. Implemented in `:architecture-tests`, because §3.1 is
written as though the leaf property holds and this is the first PR to create a published
package — documenting the gap would have left the rule §3.1 rests on unenforced at the
moment it first became checkable.

`PublishedLanguageIsLeaf` needs the origin's own context to decide what is legal, which no
`dependOnClassesThat` predicate can see, so it is an `ArchCondition`. The one exemption is
the shared-kernel `DomainEvent` marker, now named in §4.2: without it every context would
declare an identical event interface and there would be no type to dispatch across
contexts as. `org.jetbrains.annotations` is allowed as stdlib — `kotlinc` emits
`@NotNull`/`@Nullable` on every generated member.

Both proven to fire on a planted violation, then reverted:

```
planted:  ActorRegistered/GrantIssued/GrantRevoked gain `get() = BoundedContexts.names`
observed: publishedLanguageIsLeaf FAILED ... was violated (6 times):
          Method <...identity.event.ActorRegistered.getContexts()> calls method
          <uk.m4xy.modus.core.domain.BoundedContexts.getNames()> in (IdentityEvents.kt:17)

planted:  the same getter on `Capability`, in the published package
observed: publishedLanguageIsLeaf FAILED
          Method <...published.Capability.getContexts-impl(java.lang.String)> calls method
          <uk.m4xy.modus.core.domain.BoundedContexts.getNames()> in (Capability.kt:27)

planted:  `public open class PermissionGrant`
observed: aggregatesAreSealedOrFinal FAILED ... was violated (1 times):
          uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant is neither final
          nor sealed, so it is an open aggregate
```

The other two rules §3.1 claims — `ContextInternalsAreSealed` and
`PublishedLanguageAllowlist` — now carry an honest `Enforcement gap:` in `doc:10` §3.1
instead. Both compare one context against another, and `identity` is the only modelled
context, so an implementation today would be a rule that cannot fail. The bean that models
the second context closes them and is the first point at which either can be shown to fire.

### 8. The ratchet floor, and the aggregate branch floor that was missing

**`BoundedContexts` stays, recorded.** Its 31 `<clinit>` instructions do pad the baseline,
but it is not dead: `ListBoundedContexts`, `BeansModule` and `CostModule` read it, three
adapters read it through the use case, and `doc:35-testing` §8 uses it as the worked
example for two coverage evidence passages. Deleting it is a six-module change belonging
with the removal of the other five markers, not with modelling one context. The exact
ratchet makes that deletion a reviewable one-line diff in the baseline, not a blocked one.
The reasoning is recorded in the type's own KDoc so it is found where it matters.

**The `doc:20` §7.3 aggregate floor is now implemented.** `coverageRatchet` gains a second
`violationRules` entry: `element = "PACKAGE"`,
`includes = ["uk.m4xy.modus.core.domain.*.aggregate"]`, `BRANCH` `COVEREDRATIO` minimum
`1.0`. It is a ratio, so it needs no baseline row and never blocks a deletion — unlike the
module-wide count beside it, which is a regression trip-wire and not a behavioural floor.
`doc:20` §7.3's `Enforced by:` line named a `modus.test` convention plugin that does not
exist and a `com.modus.*` package that does not either; both corrected.

Proven non-vacuous:

```
planted:  `permits` gains `&& id.value != "never-covered"`, an untestable branch
observed: Rule violated for package uk.m4xy.modus.core.domain.identity.aggregate:
          branches covered ratio is 0.9, but expected minimum is 1.0
```

### Evidence for the new and changed tests

Procedure per row: green on unmodified source, break the named behaviour, run
`./gradlew :core-domain:test`, record the assertion verbatim, revert, green again
(`doc:35-testing#load-bearing-evidence`). All nine mutations reverted; the `qualityCheck`
below is on the reverted tree.

| test | source broken | observed failure |
|---|---|---|
| `permits every capability it was granted and denies one it was not` | `permits` reduced to `!revoked` | `AssertionFailedError: expected:<false> but was:<true>` |
| `a revoked grant covers nothing and permits nothing` | `permits` reduced to `capability in granted` | `AssertionFailedError: expected:<false> but was:<true>` |
| `does not share the capability set it was issued with` | `issue` keeps the caller's set as the backing collection | `AssertionFailedError: expected:<false> but was:<true>` |
| `does not hand out the capability set it decides with` | `capabilities` returns the backing collection | `AssertionFailedError: expected:<false> but was:<true>` |
| `is the same grant as any other instance carrying its id` | `equals`/`hashCode` removed | `AssertionFailedError: expected:<true> but was:<false>` |
| `denies when any instance of a grant id was revoked, in either order` | `unanimous` uses `any` instead of `all` | `AssertionFailedError: expected:<...DomainNotVisible@76828577> but was:<...CapabilityNotGranted@38732372>` |
| `refuses an actor id that could not survive being a path segment or a file name`, and the `GrantId` mirror | `OPAQUE_ID` relaxed back to `^\S+$` | `AssertionFailedError: Expected exception java.lang.IllegalArgumentException but no exception was thrown.` |
| `accepts an actor id that is an opaque lower-case token` | `OPAQUE_ID` charset narrowed to kebab only | `IllegalArgumentException: actorId must be 1-64 characters of a-z, 0-9, '.', '_' or '-', starting and ending alphanumeric: 'agent.supervisor_2'` |
| `accepts a capability whose halves are lower kebab` | `SHAPE` drops the hyphen groups | `IllegalArgumentException: capability must be '<resource>.<action>': 'work-items.bulk-read'` |

The three mutants of threads 3-5 are in the table in that section and are not repeated.

### Coverage after the review cycle

```
cmd:      ./gradlew coverageBaselineWrite
observed: :core-domain                   0 0 579 44 -> 0 0 618 38
```

Upward on instructions, so no `-Pcoverage.regress`. Branches fall from 44 to 38 because
`ActorId` and `GrantId` each traded a three-branch `isNotBlank() && none { … }` for a
single regex match — a stricter rule expressed in fewer predicates. Missed instructions
and missed branches are both still `0`, and the branch figure is now additionally floored
at 100% for `..identity.aggregate` by the rule above.

### The gate

```
cmd:      ./gradlew clean && ./gradlew --no-build-cache qualityCheck
expect:   BUILD SUCCESSFUL
observed: > Task :docsLint
          docs-lint: OK — 15 documents, 81 anchors, 256 references.

          > Task :modus-server:integrationTest
          > Task :modus-server:coverageReport
          > Task :modus-server:coverageRatchet
          > Task :modus-server:check
          > Task :coverageAggregateReport
          > Task :check
          > Task :qualityCheck

          BUILD SUCCESSFUL in 9s
          153 actionable tasks: 144 executed, 9 up-to-date
```
