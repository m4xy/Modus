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

## Two disagreements with `doc:20-ddd-practices` §5.1 as merged

Surfaced rather than closed over, per the instruction to compare the merged row against this
criterion as worded. Neither blocks the criterion; both are `documentation/` follow-ups this bean
does not own.

1. **§5.1's note says the package is not instantiated.** *"Instantiated today: … Not yet:
   `uk.m4xy.modus.core.domain.port` and the `core-application` and adapter rows, which state an
   intended shape."* This bean's own pull request instantiates it, so that sentence is false the
   moment the change merges. It was true when written.
2. **§5.1's note says ArchUnit scopes no port row.** *"ArchUnit scopes three rows by package —
   `PUBLISHED_LANGUAGE`, `DOMAIN_EVENTS`, `AGGREGATES` — and the port, kernel and default rows not
   at all."* This change adds `rule:archunit/portsAreInterfaces` over every `..port..` package and
   `rule:archunit/ambientCapabilityPortsAreLeaf` over the context-free one, so two of those rows
   become scoped. Also false on merge.

Both are the ordinary cost of a document describing a tree that changed under it, and both are
one-line edits. They are named here so the next reader of §5.1 is not misled by a sentence this
bean falsified.

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
| 1 | `ClockPort`, `IdGeneratorPort` and `RandomPort` are interfaces in `core-domain`, referencing nothing beyond the Kotlin stdlib and `java.time` | `rule:archunit/portsAreInterfaces` and `rule:archunit/ambientCapabilityPortSourceIsLeaf` green over all three; plants 1 and A–C below |
| 2 | `IdGeneratorPort` returns the raw `String` a context's identifier value class wraps, and no type in `..core.domain.port` names any context's package | **plants A and B** — a value-class return and a value-class parameter, each rejected by file and name. The bytecode rule stayed green on both, which is why the gate reads source |
| 3 | The rule scoping `..core.domain.port` is **source-reading**, because a `@JvmInline value class` erases to its underlying type and leaves no bytecode edge (`doc:00-constitution#observed-failing`, `bean:0034`) | plants A and B rejected by the source rule and **invisible** to the bytecode one; plant C, an enum, fires on both — the control that isolates erasure as the explanation |
| 4 | `rule:archunit/portsAreInterfaces` reaches every port package, and the ambient package's membership is asserted at the granularity that matters | plant 1 rejected in `identity.port`, a package that exists today; `everyPortPackageIsSeenByPortsAreInterfaces` asserts the glob's reach |
| 5 | `doc:20-ddd-practices` §5.1's package table carries a row for `..core.domain.port`, and `docs-lint` is green with it | **MET.** PR #51 merged the row (`doc:20-ddd-practices` §5.1, the `uk.m4xy.modus.core.domain.port` row). Read as merged against this criterion as worded: the row exists and says what the criterion required. Two disagreements with the rest of the merged section are recorded below rather than closed over |
| 6 | A hand-written test double for each port lives where `doc:35-testing` puts it, with no mocking framework, and is deterministic | `AmbientCapabilityDoubles.kt` in `core-domain/src/test`; no mocking dependency exists on the unit-test classpath allowlist (`doc:35-testing#unit-classpath`) |
| 7 | The doubles' own behaviour is asserted, not merely relied on | `AmbientCapabilityDoublesTest`, 14 tests, green in the 109-test `:core-domain:test` run below |
| 8 | The **gate's** input surface is asserted separately from its verdict: a test states what the leaf scan *read* out of each shipped port, distinct from the verdict reached from it | `AmbientCapabilityPortSourcePerceptionTest`, observed failing on a dead `IMPORT` regex **while the verdict stayed green** — plant D. An earlier wording cited `AmbientCapabilityDoublesTest`, which asserts the doubles' input surface, a different artefact entirely |
| 9 | `config/coverage/baseline.tsv` moves by exactly the rows this change earns, and any comment or provenance line `coverageBaselineWrite` drops is restored by hand and reported against `bean:0033` | it earns **none**: three interfaces generate no instructions, so every figure is byte-identical. The writer still erased six provenance lines — recorded below |
| 10 | `./gradlew qualityCheck` green | `BUILD SUCCESSFUL`, `167 actionable tasks` |


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
12 computes the ready *set* (`tools/docs-lint.sh:442`) and uses it only for a duplicate
`(priority, order)` guard and a non-emptiness check; `N selectable` is a set size, not an
ordering. The ranking in `AGENTS.md` step 1 is applied by whoever is reading. `bean:0094`
carries the gap.

So `order` here is a **claim on a backlog-wide lexicographic index**, not three local edits,
and it must be stated as one. Measured rather than asserted, ready high-priority beans before
and after:

| # | on `origin/main` | with this change |
|---|---|---|
| 1 | `modus-0047` (`AK`) | `modus-0047` (`AK`) |
| 2 | `modus-0031` (`AT`) | **`modus-0066` (`AQ`)** |
| 3 | `modus-0027` (`B`) | `modus-0027` (`B`) |
| 4 | `modus-0017` (`C`) | `modus-0017` (`C`) |

**One insertion, and it is a substitution.** `modus-0031` held second place and is now
`blocked_by: [modus-0030, modus-0066]`, so the work that was second is gated on `0066`, and
`0066` takes the slot it vacated. `modus-0027` and `modus-0017` are untouched at third and
fourth. `bean:0065` itself is `in-progress` and so is not in the ready set at all.

**This was not true when first written, and the correction is the point.** `bean:0092` and
`bean:0097` were raised at `AS` and `AU`, which put two fix beans ahead of `B` and `C` and
pushed `modus-0017` — the flat-file store, `bean:0067`'s blocker and the source of the
real-store evidence `bean:0066` admits it cannot produce — down two places. Neither fix bean
unblocks anything. They were re-ordered to `CA` and `CB`, behind the store. A backlog-wide
reorder presented as three local edits is the defect; a reorder that displaces the bean two of
your own beans depend on is the defect doing damage.

