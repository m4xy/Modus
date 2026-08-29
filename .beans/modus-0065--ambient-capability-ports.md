---
# modus-0065
title: The ambient-capability ports — ClockPort, IdGeneratorPort, RandomPort
status: in-progress
type: feature
priority: high
order: AP
created_at: 2026-08-29T00:00:00Z
---

# The ambient-capability ports — ClockPort, IdGeneratorPort, RandomPort

`doc:00-constitution` §1.3 bans `Instant.now()`, `LocalDate.now()`,
`System.currentTimeMillis()` and `UUID.randomUUID()` inside `core-domain`.
`doc:20-ddd-practices` §5.3 names the replacements and calls them required: `ClockPort`,
`IdGeneratorPort` and `RandomPort` "exist and are injected".

**None of the three exists.** No Kotlin or Gradle source file in the tree declares a type by
any of those names, or by the unsuffixed `Clock` / `IdGenerator` either. The only source
occurrences of the word are prose — `architecture-tests/.../ArchitectureRulesTest.kt` and
`:266`, in the KDoc and the `because(...)` string of the rule that pushes code towards a port
that is not there, plus a transcript in `bean:0003`. Three ports do exist in `core-domain`:
`ActorRepository`, `PermissionGrantRepository` and `DomainRepository`. All three are
aggregate repositories; none is an ambient capability.

The aggregates written so far hide the gap rather than close it. `Domain.adoptProcess`,
`PermissionGrant.issue`, `PermissionGrant.revoke` and `Actor.register` each take
`at: Instant` as a caller-supplied parameter, and every identifier arrives already
constructed. `doc:20-ddd-practices#domain-prohibitions` §8 sanctions that shape explicitly —
the replacement for `Instant.now()` is "`ClockPort`, **or an `Instant` parameter**" — so
those aggregates are correct as they stand. It works only because nothing above them has yet
had to *produce* an instant or an id: `core-application` holds two files,
`ListBoundedContexts` and a provisional `UseCase`, and no adapter implements any port. The
first caller that must mint a value has nowhere to get one.

## The enforcement picture is weaker than `doc:15-repository-layout#core-package-rules` §4.2 reads

Verified against the source, because this bean's premise is that rules push code towards
ports that are not there.

| documented rule | what is actually implemented |
|---|---|
| `NoAmbientTime` | Does not exist under that name. `rule:archunit/timeIsInjectedNeverReadFromAStaticClock` (`ArchitectureRulesTest.kt`) is the real rule. It is a `noClasses()` assertion — repository-wide, not scoped to `core-domain` — and it bans **only** `Instant.now`, `LocalDate.now` and `LocalDateTime.now`. |
| `NoAmbientRandom` | Does not exist at all. §4.2's own `Enforcement gap:` paragraph says so: "a call to `UUID.randomUUID()` in the domain merges green". |

Observed rather than asserted (`doc:00-constitution#observed-failing`): `UUID.randomUUID()`,
`System.currentTimeMillis()`, `System.nanoTime()`, `Clock.systemUTC()` and `Math.random()`
were each planted in `core-domain` and the build stayed green, with `Instant.now()` as a
control that correctly failed. The ban on ambient time is real but narrower than documented;
the ban on ambient randomness is a claim. Closing those rule gaps is `bean:0027`'s audit.
This bean supplies the alternative they exist to push code towards — and `RandomPort` is the
port for the one capability with no rule at all.

## Decision 1 — the names are suffixed, and the suffix is load-bearing

`ClockPort`, `IdGeneratorPort`, `RandomPort`, per `doc:20-ddd-practices` §5.3 and the
name-pattern rule in §5.1's second table (`<Noun>Port` or `<Aggregate>Repository`).

`doc:00-constitution` §1.3 says "a `Clock` port passed as a constructor argument", which
reads like a type name and is not one: that section's subject is the **ban** on ambient
capability, and it states no rule about port naming. §5.3 and §5.1 are the only places that
do. Precedence resolves two documents stating the same rule differently; it does not let one
document overrule a rule the other never states — a distinction worth writing down, because
the precedence claim was true and irrelevant at the same time, which is harder to catch than
a false one.

The suffix is also what keeps the type legible. `core-domain` may reference `java.time`
types, so an unsuffixed `Clock` would stand beside `java.time.Clock` in the same file, and
`Instant.now(clock)` — the shape `rule:archunit/timeIsInjectedNeverReadFromAStaticClock`
deliberately leaves legal — would read identically whether `clock` were the port or the JDK
type.

## Decision 2 — a context-free `..core.domain.port` package, which is not the shared kernel

`doc:20-ddd-practices` §5.1's package table places an outbound port in `<ctx>.domain.port` —
context-scoped — and carries no row for a port belonging to no context. An ambient capability
belongs to none: every context needs the time, and no context owns it. Three options; the
second is the one to kill explicitly.

| option | verdict |
|---|---|
| **Per-context, per §5.1** — `ClockPort` declared inside every context that needs one | **Rejected on correctness, not on verbosity.** Six per-context `ClockPort` declarations are six **distinct types** that are semantically identical and not type-compatible: an adapter implementing `work`'s cannot satisfy `cost`'s. So this does not give one clock injected six times. It gives six ports requiring six separate bindings of what is, by construction, one implementation per process — and nothing stops those bindings diverging. A fixed test double wired into five contexts and forgotten in the sixth is a defect this shape makes **representable**, and a shared port package makes unrepresentable. That is the argument; eighteen interfaces across three ports is the secondary observation. |
| **Shared-kernel membership** | **Rejected twice over — once on the ADR's test, once on the rule that implements it.** (a) `adr:0004-domain-id-shared-kernel#shared-kernel-membership` test 2 requires the type to "appear in the published language of more than one context, or must, for the model to be expressible". A port appears in **no** context's published language and never can: `rule:archunit/publishedLanguageIsLeaf` (`ArchitectureRulesTest.kt`) restricts `*.published..` and `..event..` to the stdlib, `java.time`, the context's own published language and the shared kernel, so a published type or event naming a port is itself the violation. Membership requires **every** test, so test 4's ADR gate is never reached. (b) Better than the argument, a fact about the mechanism: `SHARED_KERNEL` (`ArchitectureRulesTest.kt`) is `setOf(SHARED_KERNEL_EVENT, SHARED_KERNEL_DOMAIN_ID)` — a **name set, not a package prefix**. `rule:archunit/sharedKernelIsLeaf` therefore cannot see a type in `..core.domain.port` at all, whatever it is called. The "on the third member, the kernel gets its own package" trigger counts kernel members, and this bean adds none **by construction rather than by argument**. |
| **A context-free `uk.m4xy.modus.core.domain.port` package** | **Chosen.** A sibling of the kernel, not part of it. It needs no exemption from `rule:archunit/publishedLanguageIsLeaf`, because no event will ever reference a port, and `doc:15-repository-layout#extending` §9 puts "a new aggregate, a new port, a new adapter implementation of an existing port" in the class of ordinary work that "needs only a work item" — so no ADR. |

**The package root is taken from the live tree, not from `doc:20-ddd-practices` §5.1.** Every row of §5.1's package tables was found wrong in both root and segment order — `com.modus.core.<ctx>.domain.aggregate` where the tree and `PUBLISHED_LANGUAGE`, `DOMAIN_EVENTS` and `AGGREGATES` (`ArchitectureRulesTest.kt`) all say `uk.m4xy.modus.core.domain.<ctx>.aggregate`. The package this bean creates is therefore `uk.m4xy.modus.core.domain.port`, derived from `core/core-domain/src/main/kotlin/` as it actually stands. The §5.1 row of criterion 5 must be written in the corrected shape or it will name a package nothing can see — which is exactly the defect criterion 4 guards.

**The `adr:0004` analogy holds, and an earlier draft conceded it too quickly.** That draft
said the ADR's "two contexts needing one *equal* type" reasoning has no analogue here because
nothing compares clocks. The analogue is not the value — it is the **provider**. A capability
port has exactly one implementation per process by construction, which is a *stronger*
identity requirement than equality of values, not a weaker one. `adr:0004` chose a kernel
over duplication to stop two contexts holding types that must agree but need not; that is
this case exactly, one level up.

**An untested corollary, recorded as a conjecture and not as a rule.** The test-2 argument
above settles these three ports. It also suggests a general claim — *no port can ever join
the shared kernel* — which this bean does **not** assert and no future change may cite it as
having settled. That generalisation is valid only if both of the following hold, and neither
has been verified here:

1. test 2 is a **necessary** condition of membership rather than one of four tests weighed
   together, and
2. no future design could place a port type into a context's published language.

The first is a reading of `adr:0004` that its owner has not given; the second is unverifiable
from here. Both are plausible, which is the danger rather than the reassurance: a plausible
unverified generalisation left in a bean is how a conjecture is cited as authority a sprint
later. If the general claim is worth settling it is `adr:0004`'s owner's call and a separate
work item, not a side effect of placing three ports.

**What this costs, and who pays it.** `doc:20-ddd-practices` §5.1's package table needs a row
for the new package. That is a `documentation/` change and `documentation/` is not this bean's
to edit; it is reported to the orchestrator, and criterion 5 keeps the bean unclosable until
the row lands. Writing the code first and the row afterwards is how a placement table stops
describing the tree it claims to place things in.

## Decision 3 — `IdGeneratorPort` returns a `String`, on coupling grounds

**The mechanism argument this bean first gave was wrong. It is recorded here rather than
deleted.** It claimed `rule:archunit/sharedKernelIsLeaf` would reject a port returning
`ActorId`. It does not. `SHARED_KERNEL` is scoped by exact type name (`DomainEvent`,
`DomainId`) and `PUBLISHED_LANGUAGE` by the `*.published..` wildcard, so **no rule on `main`
can see a type in `core.domain.port` at all**. A reviewer planted `fun newActorId(): ActorId`
importing `identity.published.ActorId` and observed `BUILD SUCCESSFUL in 7s`. The conclusion
survives; the reason given for it does not, and citing a rule that cannot fire is the same
defect as an unobserved `Enforced by:` line.

The real reason is coupling, and it needs no rule to decide:

- A port returning typed identifiers needs **one method per identifier type** —
  `newActorId`, `newGrantId`, `newWorkItemId`, `newAgentRunId`, `newMemoryId`. A context-free
  port would then have to know every context's identifier set, and would gain a method every
  time any context added one. That is precisely the cross-context coupling
  `doc:10-architecture#bounded-contexts` §3.1's allowlist exists to prevent, reintroduced
  through a package the allowlist does not cover.
- Every context injecting the port would transitively name `identity.published`, whether or
  not it may import `identity` at all. The rule that would catch it is `bean:0023`'s
  context-isolation work, which would then need an exemption for the port package on its
  first day.

So the port returns the raw `String` the value classes wrap, and each context wraps it:
`ActorId(ids.newId())`. The type safety lost at the port is recovered one line later — the
value classes validate on construction, so a malformed value fails at the wrap rather than
travelling as a plausible-looking id.

Rejected alternative: `fun <T> newId(wrap: (String) -> T): T`. It is `newId()` plus a call at
the call site, with no guarantee the plain form lacks, and it puts a generic into a port whose
whole job is to be trivially fakeable.

## Decision 4 — `RandomPort` is in scope

An earlier draft dropped it as an abstraction with no caller. It stays.
`doc:20-ddd-practices` §5.3 requires all three, and `RandomPort` covers the one capability
`doc:15-repository-layout#core-package-rules` §4.2 claims is banned and that nothing checks —
`Math.random()` and `kotlin.random.Random.Default` were both planted in `core-domain` and
merged green. Shipping two of the three would leave §5.3 asserting a port that never arrives,
with nothing recording the gap.

## Scope

Owned: `core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/port/`,
`core/core-domain/src/test/kotlin/uk/m4xy/modus/core/domain/port/`, the new ArchUnit rule of
criterion 3, `config/coverage/baseline.tsv`, this bean.

Not owned: `documentation/20-ddd-practices.md` §5.1's package-table row (criterion 5);
`bean:0027`'s audit of the two missing rule names; retro-fitting the ports into `Domain`,
`PermissionGrant` and `Actor`, which take a caller-supplied `Instant` that
`doc:20-ddd-practices#domain-prohibitions` §8 sanctions and which no caller yet supplies from
a clock.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | `ClockPort`, `IdGeneratorPort` and `RandomPort` are interfaces in `core-domain`, referencing nothing beyond the Kotlin stdlib and `java.time` | `rule:archunit/ambientCapabilityPortsAreLeafInterfaces` green over all three; plant 1 below shows it rejecting a non-interface |
| 2 | `IdGeneratorPort` returns the raw `String` a context's identifier value class wraps, and no type in `..core.domain.port` names any context's package. Observed by reverting to a typed return and watching criterion 3's mechanism reject it | plant 1 below — `newActorId-TPKKjuw() calls method ActorId.constructor-impl` reported as a violation, at the exact shape a reviewer had observed passing on `main` |
| 3 | A **new** ArchUnit rule scopes `..core.domain.port`: every type in it is an `interface`, and it depends on nothing beyond the Kotlin stdlib and `java.time`. Observed rejecting a planted class and a planted import of `identity.published` (`doc:00-constitution#observed-failing`) | plant 1 below, which trips both arms at once — `2 times` |
| 4 | The rule is asserted to **perceive** the package, separately from what it **decides** about it | plants 2 and 3 below. Plant 3 is the decisive one: the leaf rule stays **green** while the guard fails |
| 5 | `doc:20-ddd-practices` §5.1's package table carries a row for `..core.domain.port`, and `docs-lint` is green with it | **UNMET — blocked, not failed.** Owned by the `documentation/` stack; this bean cannot close until it lands |
| 6 | A hand-written test double for each port lives where `doc:35-testing` puts it, with no mocking framework, and is deterministic | `AmbientCapabilityDoubles.kt` in `core-domain/src/test`; no mocking dependency exists on the unit-test classpath allowlist (`doc:35-testing#unit-classpath`) |
| 7 | The doubles' own behaviour is asserted, not merely relied on | `AmbientCapabilityDoublesTest`, 14 tests, green in the 109-test `:core-domain:test` run below |
| 8 | The input surface is tested separately from the verdict | the same class, split under an `input surface` heading — see the note below on what that split caught |
| 9 | `config/coverage/baseline.tsv` moves by exactly the rows this change earns, and any comment or provenance line `coverageBaselineWrite` drops is restored by hand and reported against `bean:0033` | it earns **none**: three interfaces generate no instructions, so every figure is byte-identical. The writer still erased six provenance lines — recorded below |
| 10 | `./gradlew qualityCheck` green | `BUILD SUCCESSFUL`, `167 actionable tasks` |


## Sequencing

**This lands before `bean:0014`.** `AgentRun` needs a start instant, an end instant and an id
of its own; `doc:10-architecture#bounded-contexts` §3 has `execution` publishing
`AgentRunStarted`, `AgentRunOutput`, `AgentRunCompleted` and `ContextBudgetExceeded`, every
one carrying a timestamp the aggregate must mint rather than be handed. There is no way to
write that context without these ports, and no way to write it against `Instant.now()`, which
`rule:archunit/timeIsInjectedNeverReadFromAStaticClock` rejects repository-wide.

Prose is not a sequencing mechanism: `AGENTS.md` step 1 reads `blocked_by`, `priority` and
`order` and nothing else. The `blocked_by` edge belongs on `bean:0014`, whose file this bean
does not own, so the orchestrator carries it. `order: AP` is this bean's own half, placing it
ahead of `bean:0031`'s `AT`.

`bean:0013` does not depend on this — `work`'s `WorkItem` is read from a file that already
carries its `created_at`, so `work` can be built while these ports do not exist. The edge is
to `bean:0014` specifically, and to any later context that mints values.

---

## Evidence

`doc:00-constitution#observed-failing`. Every plant was made at a real call site, run, and
reverted (`doc:35-testing` §6).

### Plant 1 — a class, and a typed identifier, in the port package

Both arms of `rule:archunit/ambientCapabilityPortsAreLeafInterfaces` in one file. The second
arm is the exact shape a reviewer observed passing on `main` before this rule existed.

```
planted:  core-domain/.../port/PlantedProbe.kt
          public class PlantedProbe { public fun newActorId(): ActorId = ActorId("planted") }
cmd:      ./gradlew :architecture-tests:test --tests '*ArchitectureRulesTest*'
observed: ArchitectureRulesTest > ambientCapabilityPortsAreLeafInterfaces FAILED
    Rule 'classes that reside in a package 'uk.m4xy.modus.core.domain.port..' should be
    interfaces and should depend on nothing beyond the Kotlin stdlib and java.time, because
    every context injects an ambient capability, so anything it drags behind it is imported
    unseen' was violated (2 times):
    Class <uk.m4xy.modus.core.domain.port.PlantedProbe> is no interface in (PlantedProbe.kt:0)
    Method <uk.m4xy.modus.core.domain.port.PlantedProbe.newActorId-TPKKjuw()> calls method
      <uk.m4xy.modus.core.domain.identity.published.ActorId.constructor-impl(java.lang.String)>
      in (PlantedProbe.kt:7)
    25 tests completed, 2 failed
```

`everyAmbientCapabilityPortIsSeenByItsOwnRule` failed in the same run, on the set mismatch.

### Plant 2 — the rule scoped to a package that does not exist

`doc:20-ddd-practices` §5.1's own error, reproduced: the wrong root. This is what a rule
written from that table would have done.

```
planted:  DOMAIN_PORT_PACKAGE = "com.modus.core.domain.port"   (was "$DOMAIN_ROOT.port")
observed: everyAmbientCapabilityPortIsSeenByItsOwnRule FAILED
    java.lang.IllegalStateException: ambientCapabilityPortsAreLeafInterfaces selected no
    class at all: nothing resides in com.modus.core.domain.port, so the rule is vacuously
    satisfied and enforces nothing (doc:00-constitution#observed-failing)
```

**And a finding that cuts against this criterion's original framing, recorded because it is
evidence against my own design.** The leaf rule *also* failed on this plant, and not on the
merits — ArchUnit refuses an empty `should` by default:

```
    Rule '…com.modus.core.domain.port..…' failed to check any classes. This means either
    that no classes have been passed to the rule at all, or that no classes passed to the
    rule matched the `that()` clause. To allow rules being evaluated without checking any
    classes you can either use `ArchRule.allowEmptyShould(true)` on a single rule or set the
    configuration property `archRule.failOnEmptyShould = false`.
```

So for the **empty** case my guard is not the only protection, and claiming otherwise would
have been an overclaim. What it still adds, and plant 3 isolates, is the case
`failOnEmptyShould` cannot see: a scope that matches *something* but not the *right*
something. It is also independent of that default — no `archunit.properties` exists in this
repository today (`find . -name 'archunit*.properties'` returns nothing), so the protection
currently rests on a library default that one config file could switch off globally for all
nine `noClasses()` rules in this file.

### Plant 3 — a port renamed, so the scope matches the wrong set

The decisive one. The leaf rule is **green**: three interfaces, all leaf-safe, all checked.
Only the perception assertion fails.

```
planted:  RandomPort renamed to RandomnessPort (file and type)
observed: ArchitectureRulesTest > everyAmbientCapabilityPortIsSeenByItsOwnRule FAILED
    java.lang.IllegalStateException: uk.m4xy.modus.core.domain.port holds
    [ClockPort, IdGeneratorPort, RandomnessPort], but the rule is declared to cover
    [ClockPort, IdGeneratorPort, RandomPort]. …Update this set deliberately.
    25 tests completed, 1 failed
```

`ambientCapabilityPortsAreLeafInterfaces` passed in that run. That is the whole argument for
criterion 4 in one line: the verdict was green and the perception was wrong.

### Criteria 6–8 — the doubles

```
cmd:      ./gradlew :core-domain:test --rerun-tasks
observed: BUILD SUCCESSFUL — 109 tests, of which AmbientCapabilityDoublesTest contributes 14
```

The input-surface/verdict split earned its keep while being written rather than in review.
`SequenceIdGenerator.issued` and `SeededRandom.bounds` are copies, and the tests that say so
(`the issued record is a copy…`, `the bounds record is a copy`) are input-surface tests with
no verdict counterpart: nothing a caller concludes would ever have revealed a shared mutable
record. Both use a **two**-element fixture, because `listOf(x)` of size one throws on
mutation and the same test at size one passes while proving nothing
(`doc:35-testing#fixture-variation`).

### Criterion 9 — the baseline, and a sixth observation for `bean:0033`

Three interfaces generate no instructions, so the ratchet does not move: every numeric row is
byte-identical before and after, and `coverageBaselineIsComplete` still sees the same module
set. `coverageBaselineWrite` nevertheless erased **six lines of provenance**:

```
cmd:      ./gradlew coverageBaselineWrite
observed: diff baseline.before.tsv config/coverage/baseline.tsv
          8,13d7
          < # REGRESSION accepted with -Pcoverage.regress: identity validators replaced …
          < #   :core-domain: covered branches 44 -> 38
          < # REGRESSION accepted with -Pcoverage.regress: GrantIssued stores its capabilities …
          < #   :core-domain: covered instructions 1549 -> 1543
          < # Restored by hand in bean:0032, bean:0030 and twice in bean:0036 after …
          < # it now also erases a PREVIOUS regression block when recording a new one. …
```

Restored by hand. This is the cleanest instance of `bean:0033` so far and sharpens its
diagnosis: **the erasure is not conditional on a regression occurring.** The writer ends with
`target.writeText(header + note + rows…)` where `header` is a constant and `note` is empty
unless *this* run regressed (`modus.coverage.gradle.kts:245-259`). So a run that changes
nothing at all destroys every hand-written line in the file — which is exactly what happened
here, on a change whose figures were identical. Anyone running the writer to confirm "no
movement" silently loses the history.

