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
reads like a type name and is not one.

**An earlier version of this decision justified the suffix with a gloss on `doc:00`'s
precedence line — "precedence resolves two documents stating the same rule differently; it
does not let one overrule a rule the other never states". That gloss is nowhere in `doc:00`,
whose precedence line is flatly unqualified, and `bean:0068` retracted it.** The ruling is
unchanged; the reason is replaced by the three textual grounds now on `main`:

| # | ground |
|---|---|
| 1 | **`doc:00` already defers to an owning document against its own precedence line.** §1.1 ends by deferring to `doc:10-architecture#module-dependencies` §4.1 and calling its own table the bug on disagreement. Ownership-over-precedence is a pattern the constitution applies to itself. |
| 2 | **Precedence produces an incoherent result.** §1.3 covers time and identifiers and never mentions randomness, so it cannot name a third port. Reading precedence as decisive yields `{Clock, IdGenerator, RandomPort}` — two names from one document and one from another, for three ports of one kind. |
| 3 | **The naming rule pre-existed and `doc:00` states nothing contrary.** `doc:20-ddd-practices#ports-and-adapters`'s outbound-port row already gave `<Noun>Port` with `ClockPort` as its own example, and the prohibitions table already named `ClockPort` as the replacement for `Instant.now()` — both on `main` before any of this. |

`doc:20-ddd-practices#ambient-ports` §5.3 is now the single owner and says so in terms: it
**decides** the names, and `doc:00` §1.3 names none.

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
### Correction — the argument that chose this module was right about the module and wrong about why

`core-domain` was argued on the ground that it is **strictly the more permissive placement**: a
port there is usable by both layers, whereas one in `core-application` could never be used by an
aggregate, because `core-domain` depends on nothing (`core/core-domain/build.gradle.kts` declares
no dependencies at all). The motivating case given was `bean:0014`'s `AgentRun` minting its own
start instant.

**PR #51 merged, and the merged document contradicts that motivating case.**
`doc:20-ddd-practices` §2.2 now states: *"Time arrives as a parameter (`at: Instant`), supplied by
the use case from the `ClockPort` port. The aggregate never asks what time it is."* §4.1.3 says
the same of an event's `occurredAt`. So no aggregate will mint its own instant, `AgentRun`
included, and the one-way-door argument rests on a door the model has now closed by rule.

The conclusion survives and is no longer an inference: `doc:20-ddd-practices` §5.1's package table
**names this package outright**, which is a stronger warrant than the argument it replaces. The
reversibility point is kept only as a secondary observation about cost, not as the reason.

Recorded rather than quietly rewritten, because the failure generalises and is the same one this
bean already carries once, in decision 3: **an argument whose premise a later document contradicts
must be corrected, not silently kept when its conclusion happens to survive.** A conclusion that
outlives its reason is indistinguishable, to the next reader, from one that was well-argued.

### The three placement options as argued at the time

| **A context-free `uk.m4xy.modus.core.domain.port` package** | **Chosen.** A sibling of the kernel, not part of it. It needs no exemption from `rule:archunit/publishedLanguageIsLeaf`, because no event will ever reference a port, and `doc:15-repository-layout#extending` §9 puts "a new aggregate, a new port, a new adapter implementation of an existing port" in the class of ordinary work that "needs only a work item" — so no ADR. |

**The package root is taken from the live tree, not from `doc:20-ddd-practices` §5.1.** Every row of §5.1's package tables was found wrong in both root and segment order — `com.modus.core.<ctx>.domain.aggregate` where the tree and `PUBLISHED_LANGUAGE`, `DOMAIN_EVENTS` and `AGGREGATES` (`ArchitectureRulesTest.kt`) all say `uk.m4xy.modus.core.domain.<ctx>.aggregate`. The package this bean creates is therefore `uk.m4xy.modus.core.domain.port`, derived from `core/core-domain/src/main/kotlin/` as it actually stands. The §5.1 row of criterion 5 must be written in the corrected shape or it will name a package nothing can see — which is exactly the defect criterion 4 guards.

**The `adr:0004` analogy holds, and an earlier draft conceded it too quickly.** That draft
said the ADR's "two contexts needing one *equal* type" reasoning has no analogue here because
nothing compares clocks. The analogue is not the value — it is the **provider**. A capability
port has exactly one implementation per process by construction, which is a *stronger*
identity requirement than equality of values, not a weaker one. `adr:0004` chose a kernel
over duplication to stop two contexts holding types that must agree but need not; that is
this case exactly, one level up.

**Three propositions, at three different strengths.** An earlier version of this section
listed both conditions below as unverified. That was wrong about the first, and wrong in the
direction that matters: it hedged a claim the ADR states verbatim, while the same bean relied
on that claim as settled thirty lines earlier. Hedging what you have the evidence for reads as
rigour and transmits doubt about something decided.

| # | proposition | status |
|---|---|---|
| 1 | Membership requires **every** test, so failing one is decisive | **Settled.** `adr:0004-domain-id-shared-kernel#shared-kernel-membership` states it verbatim: *"A type joins only if **every** statement below is true of it."* Do not hedge this. |
| 2 | Test 2 fails for a port **today** | **Observed**, and conditional. It holds while `..published..` and `..event..` are leaf packages, since a published type naming a port would itself be the violation. |
| 3 | **No port can ever join the kernel** | **Conjecture.** It depends on proposition 2 being permanent, and `adr:0004-domain-id-shared-kernel#deferred-conflict` explicitly defers the leaf-rule conflict to `bean:0023` and says "this decision should be re-read when it lands". |

So propositions 1 and 2 carry this bean's decision, and proposition 3 is recorded as a
conjecture nothing here establishes. No future change may cite this bean as having settled
it; if it is worth settling, that is `adr:0004`'s owner's call after `bean:0023`.

**What this costs, and who pays it.** `doc:20-ddd-practices` §5.1's package table needs a row
for the new package. That is a `documentation/` change and `documentation/` is not this bean's
to edit; it is reported to the orchestrator, and criterion 5 keeps the bean unclosable until
the row lands. Writing the code first and the row afterwards is how a placement table stops
describing the tree it claims to place things in.

## Decision 3 — `IdGeneratorPort` returns a `String`, on coupling grounds

**The mechanism argument this bean first gave was wrong. It is recorded here rather than
deleted.** It claimed `rule:archunit/sharedKernelIsLeaf` would reject a port returning
`ActorId`. It does not. `SHARED_KERNEL` is scoped by exact type name (`DomainEvent`,
`DomainId`) and `PUBLISHED_LANGUAGE` by the `*.published..` wildcard, so neither of **those
two** rules can see a type in `core.domain.port`. A reviewer planted `fun newActorId(): ActorId`
importing `identity.published.ActorId` and observed `BUILD SUCCESSFUL in 7s`.

**Two further corrections, and the second is the one worth carrying forward.**

First, an earlier wording of this paragraph said *no rule on `main`* could see that package.
That was false and carelessly so: `DOMAIN` is `..`-suffixed, so
`rule:archunit/domainIsFrameworkFree`, `rule:archunit/domainDependsOnNoOuterLayer` and the
repository-wide `rule:archunit/timeIsInjectedNeverReadFromAStaticClock` all scope over it — and
this bean's own enforcement table says the last one is repository-wide, twenty lines above.

Second: **the plant that produced that `BUILD SUCCESSFUL` was consistent with two different
mechanisms, and I recorded only one of them.** Scope-blindness explained it. So did **erasure** —
`ActorId` is a `@JvmInline value class`, which leaves no bytecode edge at all. Both were
sufficient; nothing in the observation distinguished them. `bean:0034` had already documented
the second and I cited neither it nor `PublishedLanguageSourceIsLeaf`. PR #55's own review then
proved erasure was the real one.

The rule this leaves behind, which is worth more than either correction:
**an observation consistent with two mechanisms establishes neither.** The remedy is not to
argue for the likelier one but to design a second observation that separates them — here, a
plant whose type is *not* a value class, which fires on the bytecode rule and so isolates
erasure as the explanation for the one that did not.

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

## Why this port is in `core-domain` and `bean:0066`'s is in `core-application`

Neither bean argued the module question at the time, which is what let them look consistent
while being unexamined. **An unexamined agreement and a disagreement are different failures,
and only one of them is visible.** They are reconciled here and in `bean:0066`, identically.

`doc:20-ddd-practices#ports-and-adapters` §5.2 declares a port **where it is used**. One rule,
applied to two ports whose possible users differ:

| port | who can use it | module |
|---|---|---|
| `ClockPort`, `IdGeneratorPort`, `RandomPort` | a use case, and — in principle — an aggregate | `core-domain` |
| `bean:0066`'s dispatch port | a use case **only**: an aggregate that publishes its own events is precisely the defect `bean:0066` exists to prevent | `core-application` |

The supporting facts for `core-domain` are that `doc:15-repository-layout#placement-table`
§2.1 names "repository, clock, id generator, agent launcher" for `core/core-domain`, and that
`doc:20-ddd-practices#ambient-ports` §5.3 sits inside the ports-and-adapters section whose
package table is a `core-domain` table.

**The argument first given for this was different, and it has been invalidated.** It said
`core-domain` is the strictly more permissive placement, since `core-application` would
permanently foreclose an aggregate minting its own instant — a one-way door against a
reversible choice. `doc:20-ddd-practices#aggregates` §2.2 as merged closes that door by rule:
*"Time arrives as a parameter (`at: Instant`), supplied by the use case from the `ClockPort`
port. The aggregate never asks what time it is."* The conclusion survives on the stronger
warrant above; the reason does not.

That is a **fourth direction** the citation defect runs, distinct from the three catalogued
this sprint: not a claim asserted beyond a stationary source, not an accurate quote whose
target moved, not a criterion meeting a document written after it, but **an argument whose
premise was invalidated while its conclusion stayed correct**. It is the least visible of the
four, because nothing looks wrong: the conclusion still holds and the citation still resolves.
**A conclusion that outlives its reason is indistinguishable, to the next reader, from one
that was well argued.**

## The two §5.1 disagreements were resolved by deletion, not by the edit they asked for

This section previously named two sentences in `doc:20-ddd-practices` §5.1 that this change
falsifies on merge — an *"Instantiated today … Not yet"* enumeration, and a claim that ArchUnit
scoped no port row — and asked that they be corrected as one-line edits so *"the next reader of
§5.1 is not misled"*.

**Both sentences no longer exist.** PR #58 deleted the enumeration rather than correcting it,
naming the commands that answer the question instead:

```
cmd:      git show origin/main:documentation/20-ddd-practices.md |
            grep -c 'Instantiated today\|ArchUnit scopes'
observed: 0
```

So the repair this section requested was done, by a better route than the one it proposed, and
the section survived pointing at it. **`adr:0005-evidence-lives-in-the-work-item` makes the bean
the record, so a stale bean outlives an accurate pull-request body** — this bean's own body said
#58 solved it better while the bean went on describing the sentences as live.

Kept, corrected, rather than cut, because the reason generalises and is the same shape as the
table corruption three sections below with a different cause: **a claim can go stale because a
script destroyed it, or because another change landed underneath it, and neither leaves a mark
on the claim.** The document's defect was never the wrong list; it was that the document cached a
fact living in the tree, and my ports would have been the next thing to expire it. Deleting the
cache is the fix; correcting it would have reset the clock.

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
| 1 | `ClockPort`, `IdGeneratorPort` and `RandomPort` are interfaces in `core-domain`, referencing nothing beyond the Kotlin stdlib and `java.time` | plants 1, 2, 3 and 6 below |
| 2 | `IdGeneratorPort` returns the raw `String` a context's identifier value class wraps, and no type in `..core.domain.port` names any context's package | plants 2 and 3 — value-class return and parameter, each rejected by file and name, and each **invisible to the bytecode rule** |
| 3 | The rule scoping `..core.domain.port` is **source-reading**, because a `@JvmInline value class` erases and leaves no bytecode edge (`bean:0034`) | plants 2 and 3 rejected by the source rule only; plant 4, an enum, fires on **both** — the control that isolates erasure rather than inferring it |
| 4 | `portsAreInterfaces` is proven to reach every port package by **evaluating its own glob**, not by re-deriving it | plant 5 — narrowing `ALL_PORTS` is caught; plant 1 fires in `identity.port`, a package that already existed |
| 5 | `doc:20-ddd-practices` §5.1's package table carries a row for `..core.domain.port`, and `docs-lint` is green with it | **MET.** PR #51 merged the row. Read as merged against this criterion as worded: the row exists and says what the criterion required. Two further sentences in that section were falsified by this change and have since been **deleted** by PR #58 rather than corrected — recorded below |
| 6 | A hand-written test double for each port lives where `doc:35-testing` puts it, with no mocking framework, and is deterministic | `AmbientCapabilityDoubles.kt` in `core-domain/src/test`; no mocking dependency exists on the unit-test classpath allowlist (`doc:35-testing#unit-classpath`) |
| 7 | The doubles' own behaviour is asserted, not merely relied on | `AmbientCapabilityDoublesTest`, 14 tests, green in the 109-test `:core-domain:test` run below |
| 8 | The **gate's** input surface is asserted separately from its verdict, positively, so a blinded scan fails | plant 7 — a dead `IMPORT` regex fails perception **while the verdict stays green** |
| 9 | `config/coverage/baseline.tsv` moves by exactly the rows this change earns, and any provenance `coverageBaselineWrite` drops is restored by hand and reported against `bean:0033` | it earns **none** — three interfaces generate no instructions, every numeric row byte-identical. The writer still erased six provenance lines; see below |
| 10 | `./gradlew qualityCheck` green | `BUILD SUCCESSFUL`. The task count depends on configuration-cache state and both figures are real, so the transcript below records **which state produced which** rather than one number a re-runner has a coin-flip chance of matching |


## Sequencing

**This lands before `bean:0014`.** `AgentRun` needs a start instant, an end instant and an id;
`doc:10-architecture#bounded-contexts` §3 has `execution` publishing four events, every one
carrying a timestamp. The use case will mint those from `ClockPort`
(`doc:20-ddd-practices#aggregates` §2.2), so `execution` cannot be written until the port
exists. The `blocked_by` edge is on `bean:0014`, which the orchestrator carries.

### `order` is advisory, and the reorder is backlog-wide

**Correction to an earlier claim in this bean.** It said the sequencing "now lives where the
selector reads it". That is half true, and the half that is false matters: `blocked_by` is
machine-checked by `docs-lint` check 12, but **nothing sorts by priority then order**. Check
12 computes the ready *set* (`tools/docs-lint.sh`) and uses it only for a duplicate
`(priority, order)` guard and a non-emptiness check; `N selectable` is a set size, not an
ordering. The ranking in `AGENTS.md` step 1 is applied by whoever is reading. `bean:0094`
carries the gap.

So `order` here is a **claim on a backlog-wide lexicographic index**, not three local edits,
and it must be stated as one. Re-measured against `origin/main` at `63f367e`, by replaying
`AGENTS.md` step 1 over `git ls-tree` of each ref rather than over the working tree:

| # | on `origin/main` (28 selectable) | with this change (30 selectable) |
|---|---|---|
| 1 | `modus-0047` (`AK`) | `modus-0047` (`AK`) |
| 2 | `modus-0065` (`AP`) | `modus-0066` (`AQ`) |
| 3 | `modus-0066` (`AQ`) | `modus-0027` (`B`) |
| 4 | `modus-0027` (`B`) | `modus-0017` (`C`) |
| 5 | `modus-0017` (`C`) | `modus-0092` (`CA`) |
| 6 | `modus-0013` (none) | `modus-0097` (`CB`) |

**The reorder this section was written about has already landed**, in the pull request that
raised these beans: `modus-0065` and `modus-0066` sit at second and third on `origin/main`
today, and `modus-0031` — which earlier revisions discussed at second place — is blocked and
out of the ready set entirely.

What *this* branch changes is smaller and is the whole of the claim: `modus-0065` leaves the
ready set, because this branch sets it `in-progress`; and `modus-0092`, `modus-0094` and
`modus-0097` are raised, so 28 selectable becomes 30. **`modus-0027` and `modus-0017` each
move up one place and are behind nothing new.** The three new beans sit at `CA`, `CB` and `CC`,
deliberately **behind** `B` and `C`.

**This was not true when first written, and the correction is the point.** `bean:0092` and
`bean:0097` were raised at `AS` and `AU`, which put two fix beans ahead of `B` and `C` and
pushed `modus-0017` — the flat-file store, `bean:0067`'s blocker and the source of the
real-store evidence `bean:0066` admits it cannot produce — down two places. Neither fix bean
unblocks anything. They were re-ordered to `CA` and `CB`, behind the store. A backlog-wide
reorder presented as three local edits is the defect; a reorder that displaces the bean two of
your own beans depend on is the defect doing damage.

---

## Evidence

`doc:00-constitution#observed-failing`. Every plant was made at a real call site, run, and
reverted (`doc:35-testing` §6). **Re-derived against the current head**, not restored: an
earlier revision of this bean lost its whole evidence section to a script of mine that
truncated the file at `## Sequencing`, and the criteria table kept pointing at transcripts
that no longer existed. Restoring the old text would have re-attached evidence for a gate
that has since been rebuilt twice.

### Plant 1 — `portsAreInterfaces`, on a package that already existed

```
planted:  identity/port/PlantedRepository.kt — public class PlantedRepository
cmd:      ./gradlew :architecture-tests:test --rerun-tasks
observed: ArchitectureRulesTest > portsAreInterfaces FAILED
    Class <uk.m4xy.modus.core.domain.identity.port.PlantedRepository> is no interface
    63 tests completed, 1 failed
```

This closes one of the five §4.2 rules `doc:15-repository-layout` records as missing, and it
is observed on `identity.port` rather than on the package this bean adds — so the glob is
proven to reach the context-scoped packages, not assumed to.

### Plants 2 and 3 — the erasing shapes, invisible to bytecode

```
planted:  IdGeneratorPort.kt — public fun newActorId(): ActorId
observed: AmbientCapabilityPortSourceTest > ambientCapabilityPortSourceIsLeaf FAILED
    core/core-domain/.../port/IdGeneratorPort.kt:
      imports 'uk.m4xy.modus.core.domain.identity.published.ActorId'
      names 'uk.m4xy.modus.core.domain.identity.published.ActorId'
    63 tests completed, 2 failed
```

```
planted:  IdGeneratorPort.kt — public fun idFor(actor: ActorId): String
observed: same rule, same file, rejected by name
```

`rule:archunit/ambientCapabilityPortsAreLeaf` — the bytecode rule — **passed both times**.

### Plant 4 — the enum control that isolates erasure

```
planted:  IdGeneratorPort.kt — public fun kind(): ActorKind      (an enum, not a value class)
observed: ArchitectureRulesTest > ambientCapabilityPortsAreLeaf FAILED
    Method <...port.IdGeneratorPort.kind()> has return type
      <uk.m4xy.modus.core.domain.identity.published.ActorKind>
          AmbientCapabilityPortSourceTest > ambientCapabilityPortSourceIsLeaf FAILED
    ... names 'uk.m4xy.modus.core.domain.identity.published.ActorKind'
    63 tests completed, 3 failed
```

**The controlled comparison, which is the whole warrant for shipping a source-reading gate.**
Both shapes on one interface, in one file, in one compile — the only difference is whether the
returned type erases:

| declaration | type | bytecode rule | source rule |
|---|---|---|---|
| `newActorId(): ActorId` | `@JvmInline value class` | **invisible** | fires |
| `kind(): ActorKind` | `enum` | fires | fires |

**The bytecode rule fired once.** The source rule caught both. Nothing else differs, so the
blindness is attributable to erasure rather than to scope, spelling, or the rule being broken
in general — the bytecode rule demonstrably works on the row below it.

That is what makes "the bytecode rule is blind to value classes" an **observation** rather
than an inference, and it is the second observation decision 3 records as missing the first
time round. One green build was consistent with two mechanisms; this pair separates them.

### Plant 5 — the guard evaluates the rule's own glob

`everyPortPackageIsSeenByPortsAreInterfaces` re-derived the glob as `startsWith`/`endsWith`
instead of reading `ALL_PORTS`, so narrowing the rule left `identity.port` unguarded with the
guard still green. It now runs the rule's constant through `PackageMatcher`:

```
planted:  ALL_PORTS narrowed to "$DOMAIN_PORT_PACKAGE.."
observed: everyPortPackageIsSeenByPortsAreInterfaces FAILED
    portsAreInterfaces is scoped at 'uk.m4xy.modus.core.domain.port..', which reaches
    [port], but the port packages in core-domain are [domainmgmt.port, identity.port, port]
```

### Plant 6 — a subpackage port with a colliding basename

Both perception guards keyed on a basename, and `associate` keeps the last value for a
duplicate key, so a subpackage port would have held the key set at three. Keyed on the
relative path and the package-qualified name instead:

```
planted:  port/internal/ClockPort.kt — public fun sneak(): ...identity.published.ActorId
observed: 63 tests completed, 5 failed
    everyAmbientCapabilityPortIsSeenByItsOwnRule, everyPortPackageIsSeenByPortsAreInterfaces,
    both perception tests, and the source verdict — the last naming
    core/core-domain/.../port/internal/ClockPort.kt
```

### Plant 7 — a dead parse, while the verdict stays green

```
planted:  AmbientCapabilityPortSource.IMPORT changed to match `imports`, not `import`
observed: AmbientCapabilityPortSourcePerceptionTest > the scan sees the one import that is
          known to be present() FAILED
    the scan did not see `import java.time.Instant` in ClockPort.kt, which declares it. A scan
    that cannot see a known-present import has not read the file, and the leaf verdict over it
    is vacuous. Imports actually seen: []
    63 tests completed, 2 failed
```

`AmbientCapabilityPortSourceTest` — the **verdict** — passed. That is criterion 8's whole
case. The input-surface test failed too, so the perception test is not the sole guard against
this; the earlier claim that it was, was understated rather than overstated.

### The two escapes that defeated the source gate, and were fixed

**Escape A — a slash-star inside a string literal.** The stripper deleted a lazy slash-star
regex before anything was parsed, so a slash-star inside a **string** opened a phantom comment
running to the close of the next KDoc; everything between vanished from the scan's input.

```
planted:  IdGeneratorPort.kt, inside the interface, above the newId KDoc:
          a @Suppress whose argument is a slash-star, then
          public fun newActorId(): uk.m4xy.modus.core.domain.identity.published.ActorId
before:   BUILD SUCCESSFUL          <- all five guards green, ActorId on a shipped port
after:    AmbientCapabilityPortSourceTest > ambientCapabilityPortSourceIsLeaf FAILED
    core/core-domain/.../port/IdGeneratorPort.kt:
      names 'uk.m4xy.modus.core.domain.identity.published.ActorId'
    63 tests completed, 2 failed
```

**Escape B — the qualified-name arm was modus-only.** An import of a JDK type was rejected
while the same type inline was not, and `java.util` is on the bytecode rule's allowlist, so
the inline form passed **both** rules.

```
planted:  public fun raw(): java.util.UUID
before:   BUILD SUCCESSFUL
after:    core/core-domain/.../port/IdGeneratorPort.kt: names 'java.util.UUID'
    63 tests completed, 1 failed
```

### A failed reproduction is not evidence of absence

**My first attempt at escape A did not reproduce it, and the gate went red.** I placed the
fixture *after* the last KDoc, so the phantom comment found no closing delimiter, the regex
matched nothing, and the scan read the file correctly. Had I stopped there I would have
reported a live escape refuted.

**The fixture's position is load-bearing, not only its content.** The asymmetry is nasty: a
successful reproduction proves the escape, a failed one proves nothing, and only the first
announces itself — a failed reproduction produces exactly the outcome the reproducer was
hoping for, so nothing prompts a second attempt. The same lesson as *a check reporting total
failure is as untrustworthy as one reporting total success*, from the other side: **red is not
evidence the mechanism worked, only that this input did not defeat it.**

### The documentation of a comment-parsing bug is subject to comment parsing

Writing escape A into a KDoc reproduced it inside the compiler: a literal slash-star in a KDoc
opens a **nested** block comment, and the build failed `Syntax error: Unclosed comment`. Those
files now spell the delimiters out in prose. Third artefact this sprint bitten by the exact
mechanism it documents, after a bean quoting check 14's message going unexamined by check 14,
and `doc:20-ddd-practices` §5.1 — the section warning that a misplaced type escapes a rule —
naming packages no rule could see.

### A stale snapshot silently reverted a fix, and the gate caught it

Reverting plant 7 with `cp` from a snapshot taken *before* the lexer rewrite restored the
regex stripper wholesale. Nothing in the diff looked wrong. Re-running escapes A and B is what
found it, which is why both were re-run after every structural change rather than once.

### Criteria 6–7 — the doubles

```
cmd:      ./gradlew :core-domain:test --rerun-tasks
observed: BUILD SUCCESSFUL — 109 tests
```

`SequenceIdGenerator.issued` and `SeededRandom.bounds` are copies, asserted by input-surface
tests with no verdict counterpart: nothing a caller concludes would reveal a shared mutable
record. Both use a **two**-element fixture, because `listOf(x)` of size one throws on mutation
and the same test at size one passes while proving nothing (`doc:35-testing#fixture-variation`).

### Criterion 10 — the gate

```
cmd:      ./gradlew ktlintFormat && ./gradlew qualityCheck
observed: BUILD SUCCESSFUL
          168 actionable tasks: 10 executed, 28 from cache, 130 up-to-date
          Configuration cache entry stored.

cmd:      ./gradlew qualityCheck          (immediately again, cache warm)
observed: BUILD SUCCESSFUL
          159 actionable tasks: 5 executed, 154 up-to-date
          Configuration cache entry reused.
```

Both are green and both are real. The count differs because a stored-cache run configures and
runs the included build's own tasks while a reused-cache run does not, so a reader re-running
this sees 168 or 159 according to a state the command line does not mention. The
`Configuration cache entry` line is the discriminator, which is why it is quoted here and the
bare number is not quoted in the criterion.

### Criterion 9 — the baseline, and a sixth observation for `bean:0033`

Three interfaces generate no instructions, so the ratchet does not move: every numeric row is
byte-identical and `coverageBaselineIsComplete` sees the same module set.
`coverageBaselineWrite` nevertheless erased **six lines of provenance** — both `# REGRESSION`
blocks and the note recording that this keeps happening:

```
cmd:      ./gradlew coverageBaselineWrite
observed: diff baseline.before.tsv config/coverage/baseline.tsv
          8,13d7
          < # REGRESSION accepted with -Pcoverage.regress: identity validators replaced ...
          < #   :core-domain: covered branches 44 -> 38
          < # REGRESSION accepted with -Pcoverage.regress: GrantIssued stores its capabilities ...
          < #   :core-domain: covered instructions 1549 -> 1543
          < # Restored by hand in bean:0032, bean:0030 and twice in bean:0036 after ...
          < # it now also erases a PREVIOUS regression block when recording a new one. ...
```

Restored by hand. This sharpens `bean:0033`: **the erasure is not conditional on a
regression.** The writer rebuilds the file from a constant header plus a note that is empty
unless *this* run regressed, so a run where nothing changed destroys every hand-written line —
which is what happened here, on a change whose figures were identical.

### What this gate still does not catch

Stated because an unstated limit reads as coverage.

| shape | status |
|---|---|
| `import kotlin.io.path.Path` in a port | **passes the source rule.** `kotlin.` is allow-listed as a prefix, and `doc:00-constitution` §1.3 bans a path type from the domain outright. The bytecode rule catches it. Narrowing `kotlin.` means enumerating which parts of the standard library a port may name — a decision, not a patch |
| a port with a default implementation performing IO | **unimplemented.** `doc:15-repository-layout#core-package-rules` §4.2's `PortsAreInterfaces` row requires "an `interface` with no default implementations that perform IO"; this change implements `beInterfaces()` and the second half of the row is not enforced by anything |

---

## One script destroyed three tables in this bean, and two were reported as delivered

Recorded here because this is where the damage was. The general form belongs elsewhere and the
orchestrator carries it; this section is the instance and its cause.

**The cause.** The script that rebuilt this bean's `## Evidence` section replaced criteria rows
by matching any line beginning `| N | `. That pattern matches **every** numbered table in the
file, not the one intended. Three were overwritten with criteria rows:

| table | what it held | how it was recovered |
|---|---|---|
| Decision 1's three textual grounds | the `bean:0068` replacement for the retracted precedence gloss | restored **verbatim from the script that authored it** |
| the settled / observed / conjecture split | the whole of the corollary ruling | restored **verbatim from the script that authored it** |
| the sequencing ready-list | the reorder measurement, under prose reading *"Measured rather than asserted"* | **re-measured**, never restored |

**Two of the three had been reported to the orchestrator as delivered.** The reports were honest
— the work had been done — and the artefact was not, because a later script destroyed it in
sections nobody had reason to re-open. Those come apart, and an orchestrator's explicit request
for a table by name is the strongest available marker that it should not vanish. It vanished
anyway.

**Why it survived a repair pass aimed at the same script's damage.** The earlier pass rebuilt
`## Evidence`, and rebuilding `## Evidence` does not touch `## Sequencing` or the decisions above
it. The repair was scoped to the damage that had been *noticed*, so two further instances of the
same script's damage sat a few sections away, unexamined, through a full review round.
`docs-lint` was green throughout: it validates references, not table semantics, so no mechanical
check would have found any of the three.

**The repair rule this yields, and it is the part worth carrying:** how a destroyed artefact may
be repaired depends on what it claimed.

- A table of **argument** can be restored from the text that authored it, because the argument
  is the text.
- A table of **measurement** may not. Restoring it would assert a measurement nobody took —
  **a restored measurement is a fabricated measurement, however confident the reconstruction.**
  It must be re-run.

Re-running it is what proved the rule rather than merely illustrating it. The measurement had
**changed**: the pull request that raised these beans merged in the interim, so the reorder that
section argued for had already landed, and this branch's actual effect turned out to be smaller
than every earlier revision claimed. A restored table would have been a true-looking claim about
a world that no longer existed — and it would have read as more rigorous than the correct one,
because it was the one whose prose said "measured".

**Sweep before repairing, not after.** The sweep that found the other two was one script over
every bean, looking for identical rows appearing in more than one table. It took less time than
the first repair pass did, and it is the step that pass skipped.
