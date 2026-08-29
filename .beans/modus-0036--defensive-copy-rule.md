---
# modus-0036
title: Enforce defensive copying of collection-typed properties in the domain
status: completed
type: feature
priority: high
order: AF
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T22:00:00Z
---

# Enforce defensive copying of collection-typed properties in the domain

The same defect has now shipped twice, in two bounded contexts, past two evidence passes.

| where | found by | what escaped |
|---|---|---|
| `PermissionGrant.capabilities` (`bean:0009`) | review | the live `Set` was returned; a caller could down-cast to `MutableSet` and grant itself a capability nobody issued |
| `ProcessDefinition` (`bean:0030`) | review | all four collections were `public val` constructor parameters, copied neither in nor out; a caller could add a transition, widen the terminal set, and mutate the payload of an **already-raised** `DomainCreated` |

`bean:0030`'s second occurrence got past ten planted mutations with a 10/10 kill rate, the
`doc:35-testing#fixture-variation` rule quoted in its own fixture KDoc, and an author who
had read the first occurrence's fix. That is the argument for a tool: the rule is known,
written down, and still not followed, which `doc:00-constitution#mechanical-enforcement`
says is a rule that will keep being broken.

`doc:20-ddd-practices#value-objects` §3.1 now states the rule and carries this bean as its
`Enforcement gap:`.

## Success criteria

Stated as bullets when this bean was cut; numbered here unchanged so the closing evidence
can be read beside them (`adr:0005-evidence-lives-in-the-work-item#evidence-home`).

| # | criterion |
|---|---|
| 1 | A type in `..domain.published..` or `..domain.aggregate..` with a collection-typed property fails the build unless the property is private and every accessor returns a copy. Observed rejecting the pre-fix `ProcessDefinition` and the pre-fix `PermissionGrant` before it is claimed (`doc:00-constitution#observed-failing`). |
| 2 | The rule distinguishes a `private val` backing field from a `public val` — the former is the fix, not the violation. |
| 3 | Choose the mechanism deliberately and record why: a Detekt rule sees the AST and the property's visibility directly, which is what this needs; ArchUnit sees bytecode, where a Kotlin getter returning a copy and one returning the field are hard to tell apart. The Detekt path is blocked on `bean:0026`, which has to establish that custom Detekt rules exist at all — none of the eleven `doc:30-code-style` §4 documents are implemented. |
| 4 | A test asserting the copy property MUST use a collection of size two or more. `Collections.singleton` and `singletonList` throw on mutation, so the same test written against a one-element fixture passes while proving nothing — observed twice in `bean:0030`, once in the aggregate's `pendingEvents` and once in this bean's own subject. |

---

## Mechanism, and why

**Chosen: a source-scanning test**, `rule:archunit/noDomainTypePublishesACollectionItOwns`
in `architecture-tests/` (`DefensiveCopySourceTest`, over the analyser `DefensiveCopy`). It
reads Kotlin source from `modus.repoRoot`, the way `PublishedLanguageSourceTest` already
does.

| candidate | verdict |
|---|---|
| Detekt custom rule | **Unavailable, not merely inconvenient.** It is the right home — it sees the AST — but Modus has never built one, and `bean:0026` exists precisely to establish that custom Detekt rules can run at all. Building the first one is that bean's whole subject; this rule is not waiting on it. |
| ArchUnit, over bytecode | **Cannot see the property this rule is about.** A Kotlin property with a custom getter compiles to a method and *no field*, so `private val granted` behind `val capabilities get() = granted.toSet()` leaves nothing for a visibility rule to read; and the copying getter differs from the leaking one only inside a method body, which is not what ArchUnit's model describes. `data` survives as a `copy` method, so ArchUnit could have caught the **proxy** — and the proxy is the weaker half, because it misses `bean:0009`'s exact shape: a private field published by a non-copying getter. `bean:0034` found the mirror image of this blind spot from the other side. |
| Source scan | **Chosen.** `private`, `data`, and the getter's expression are all written down in source. It rests on two things the build already guarantees — ktlint `ktlint_official` formatting, and `explicitApi()` strict mode, which is what makes an explicit visibility modifier a reliable way to tell a member from a local. |

**Scope widened, deliberately.** The criterion above says `..published..` and
`..aggregate..`. The gate scans **all of `core/core-domain/src/main`** instead, because
`bean:0030`'s stated harm was mutating the payload of an already-raised domain event and
events live in `..event..` — the narrower scope would have excluded the occurrence that
motivated this bean. Widening it immediately found a **third, live** occurrence on `main`.

**What it catches.** Copy-out in full; copy-in structurally. Every line has a planted
counter-example, and each was observed failing when the code that catches it was reverted.

1. A `data class` owning a collection, at any visibility.
2. A non-private stored collection property — `bean:0030`'s shape — **with or without a
   declared type**, carrying no visibility modifier, inside a `companion object`, or inside an
   anonymous `object :` expression.
3. An accessor that is not **a backing field followed by exactly one copy call**: a bare field,
   a block getter with an early return past the copy, a conditional that copies down one branch,
   a chain taking an argument anywhere (a lambda, or a method reference, which has no brace to
   ban), or an argument-free call with a side effect, `held.register().toList()`. Argument-free
   turned out to be necessary and not sufficient.
4. A non-private function whose returned expression **mentions** a backing field and is not that
   copy — `asReversed`, `subList`, `mapOf("all" to held)` and `Pair(held, held.size)` alike,
   through an expression body, a `return` in a block, or a wrapped signature. Keyed on mention
   rather than on the expression's root, because the root of `mapOf(…)` is `mapOf`, so the
   accessor path and the function path now enforce one standard instead of two.
5. A function that mentions a backing field and **declares no return type**, which is a violation
   in its own right rather than a guess at what the expression evaluates to. Its cost is larger
   than it first reads and is stated at full size: **no non-private function without a return
   type may mention any private field, of any type**. `internal fun size() = held.size` and
   `internal fun isFrozen() = frozen` are both rejected and neither hands anything out. The
   breadth is deliberate — the check is over every private field precisely because a field whose
   type the scan cannot read is the case it exists for — and it fails closed. An earlier draft
   said "the cost is zero"; that was true of `core-domain` and false in general. `bean:0064`
   carries narrowing it.
6. A `typealias` for a collection: generic, and resolved to a fixed point.
7. **Copy-in, structurally**: a collection-typed constructor property requires a `private
   constructor`; a type carrying one may not declare a reachable secondary constructor; and
   storing a constructor parameter without copying it — as an initialiser, or in an `init` block,
   qualified with `this.` or not — is rejected.

**What it does not catch.** Three entries, each planted at a real call site and observed
passing, each stated as *the cheapest change that would close it and why that change is or is
not being made*. "A type checker would be needed" was the previous phrasing and it overclaimed
the limit — twice the cheapest thing turned out to be one `filter` clause, which is the same
disease as overclaiming enforcement, pointed the other way.

| shape that passes | cheapest change that closes it | taken? |
|---|---|---|
| anything outside `core-domain`'s main source | one constant and a `/src/main/` filter, scanning `core/` — **measured green today** | **No.** The rule binds a *domain* type owning state. `core-application` holds stateless use cases, where a collection crossing the boundary is a result, not a leak. Widening a gate because a module happens not to violate it today buys a grandfather clause on the next honest change |
| a leak laundered through a call, `= passthrough(granted)`, or handed to a callee, `sink.add(granted)` | flag any private collection field appearing as a call argument | **No.** It fires on legitimate internal use: `GrantIssued.hashCode()` passes the backing `issued` to `listOf(…)`, and `PermissionGrant.issue` passes locals named `granted` and `issued` that shadow backing fields, so a lexical check misfires on the real use and the shadow alike. An earlier draft of this row cited `ProcessDefinition`'s `walk`, `reachableFrom` and `canFinish` and was **wrong** — those are companion functions taking locals, and a companion cannot see the instance's fields. Review caught it; the conclusion survives on the citations above, the citation did not |
| a function whose declared return type names no collection at all, `public fun any(): Any = held` | one predicate: apply the leak arm whenever the returned expression mentions a backing field, whatever the return type names | **Not yet — `bean:0064`.** Realistic containers are already caught, because the collection is named inside the generic (`Map<K, List<V>>`, `Pair<List<X>, Int>`); what survives is a signature nothing in `core-domain` writes. Small, but a change to a live predicate, and this commit is the one that documents the gap |
| a named factory that forgets to copy, `of(steps) = Uncopied(steps)` | require every argument at a call to the type's own constructor to be a copy chain | **No.** `PermissionGrant.issue` copies into a local one line earlier and passes that local, so the check would reject the shape §3.1 prescribes. Needs local dataflow |

Two entries left the list this round by being closed rather than argued: `mapOf("all" to
granted).toMap()` is now rejected, because a copy chain may take no arguments; and a collection
nested in a returned value, `= Wrapper(values)`, is now reported **at `Wrapper`'s own
definition**, since any in-domain type that can receive an uncopied collection is itself a
violation.

Beyond the table: source ktlint has not formatted is invisible and **cannot reach `main`**
(`ktlintMainSourceSetCheck` rejects it in the same `qualityCheck` run; both halves observed) —
but that guards **formatting only**, not shape. And it is a lexical scan with no types.

---

## Evidence

`doc:00-constitution#observed-failing`. Every plant was made at a real call site in
`core-domain`, run, and reverted (`doc:35-testing` §6). Elisions in the transcripts below
are marked `[...]` or `[same]` and always repeat a message quoted in full elsewhere.

### The gate rejects live code on `main` — two violations nobody had found

The first run of the finished gate, against unmodified `origin/main`:

```
cmd:      ./gradlew :architecture-tests:test --tests '*DefensiveCopySourceTest*'
observed: DefensiveCopySourceTest > no domain type publishes a collection it owns() FAILED
    org.opentest4j.AssertionFailedError: no domain type may hand out a collection it owns;
    copy in at construction and copy out at every accessor (doc:20-ddd-practices §3.1):
      core/core-domain/.../identity/event/IdentityEvents.kt:23: GrantIssued.capabilities:
        Set<Capability> — a `data class` cannot own a collection. Its generated constructor
        binds the caller's instance and `copy` hands one straight back, so every invariant
        holds exactly once (doc:20-ddd-practices §3.1). Use a private constructor, a factory
        that copies in, getters that copy out, and hand-written equals/hashCode.
      core/core-domain/.../BoundedContexts.kt:28: BoundedContexts.names: List<String> —
        a non-private stored collection property publishes the backing instance. [...]
```

`GrantIssued` is the **third occurrence of this defect**, in the context `bean:0009` was
supposed to have fixed. `PermissionGrant.issue` handed the event a copy, so the grant itself
was safe — but the event's own set was an ordinary `LinkedHashSet` that any handler could
down-cast, adding a capability to a fact that had **already happened**. That is exactly
`bean:0030`'s harm. It was found by a gate, in its first run, not by review.

`BoundedContexts.names` is the same class of leak at lower stakes: `listOf` over more than
one element returns a `java.util.Arrays$ArrayList`, whose `set` works.

Both are fixed in this change.

### `bean:0009`'s original shape, re-planted at its own call site

```
planted:  PermissionGrant — the pre-fix shape restored:
            public val capabilities: Set<Capability>,     (constructor property)
          with the copying getter removed.
cmd:      ./gradlew :architecture-tests:test --tests '*DefensiveCopySourceTest*' --rerun-tasks
observed: DefensiveCopySourceTest > no domain type publishes a collection it owns() FAILED
      core/core-domain/.../identity/aggregate/PermissionGrant.kt:32:
        PermissionGrant.capabilities: Set<Capability> — a non-private stored collection
        property publishes the backing instance. Kotlin's `Set`/`List` are read-only views
        rather than immutable types, so at size two or more a caller down-casts and mutates
        what this type decides with (bean:0009, bean:0030). Make it `private` and expose a
        copying getter.
reverted: yes
```

### `bean:0030`'s original shape, re-planted at its own call site

```
planted:  ProcessDefinition — the pre-fix shape restored:
            public data class ProcessDefinition(
                public val declaredStates: List<StateName>,
                public val initial: StateName,
                public val declaredTerminal: List<StateName>,
                public val declaredTransitions: List<StateTransition>,
            )
cmd:      ./gradlew :architecture-tests:test --tests '*DefensiveCopySourceTest*' --rerun-tasks
observed: DefensiveCopySourceTest > no domain type publishes a collection it owns() FAILED
      ProcessDefinition.kt:31: ProcessDefinition.declaredStates: List<StateName> — a `data
        class` cannot own a collection. Its generated constructor binds the caller's
        instance and `copy` hands one straight back, so every invariant holds exactly once
        (doc:20-ddd-practices §3.1). Use a private constructor, a factory that copies in,
        getters that copy out, and hand-written equals/hashCode.
      ProcessDefinition.kt:33: ProcessDefinition.declaredTerminal: List<StateName> — [same]
      ProcessDefinition.kt:34: ProcessDefinition.declaredTransitions: List<StateTransition>
      ProcessDefinition.kt:37: ProcessDefinition.states: Set<StateName>
      ProcessDefinition.kt:40: ProcessDefinition.terminal: Set<StateName>
      ProcessDefinition.kt:43: ProcessDefinition.transitions: Set<StateTransition>
    (six violations; `initial` is not a collection and is not reported)
reverted: yes
```

The narrower one-line plant is caught the same way — `private val declaredTransitions` made
`public val` on the current, correct class:

```
observed: ProcessDefinition.kt:34: ProcessDefinition.declaredTransitions:
            List<StateTransition> — a non-private stored collection property publishes the
            backing instance. [...]
reverted: yes
```

**A finding, recorded rather than smoothed over.** The first attempt at this plant was to
add `data` to the *current* `ProcessDefinition`, which has a private constructor. It does
not compile, so that particular shape can no longer exist in this repository at all:

```
e: warnings found and -Werror specified
e: .../ProcessDefinition.kt:30:37 Non-public primary constructor is exposed via the
   generated 'copy()' method of the 'data' class.
```

The plant above therefore restores the **public** constructor `bean:0030` actually shipped,
which is the faithful shape and does compile. The compiler covers one corner of §3.1 that
nobody had noticed it covered; it says nothing about the other three.

### The escape a `data class` ban does not catch

A correctly private backing field, published by a getter that returns it unaltered. This is
the case an ArchUnit rule over `copy` methods would have passed.

```
planted:  PermissionGrant.pendingEvents —
            public val pendingEvents: List<DomainEvent> get() = events
cmd:      ./gradlew :architecture-tests:test --tests '*DefensiveCopySourceTest*' --rerun-tasks
observed: DefensiveCopySourceTest > no domain type publishes a collection it owns() FAILED
      PermissionGrant.kt:40: PermissionGrant.pendingEvents: List<DomainEvent> — the getter
        returns `events`, which is not a copy — it must end in one of toList(), toSet(),
        toMap(), toSortedSet(), toSortedMap(), toTypedArray(), toMutableList(),
        toMutableSet(), toMutableMap(), emptyList(), emptySet(), emptyMap(), or the caller
        keeps a live reference to what this type decides with. Extend that list deliberately
        if a new copying form is genuinely needed.
reverted: yes
```

And the same escape spelled as a method, which is the obvious way around a rule about
properties:

```
planted:  PermissionGrant — public fun raised(): List<DomainEvent> = events
observed: PermissionGrant.kt:45: a function returns the backing collection `events`
            unaltered; return a copy (doc:20-ddd-practices §3.1).
reverted: yes
```

### `private val` is the fix, not the violation

```
cmd:      ./gradlew :architecture-tests:test --tests '*DefensiveCopySourceTest*' --rerun-tasks
observed: BUILD SUCCESSFUL
```

Every plant reverted, `PermissionGrant`, `ProcessDefinition`, `Actor` and `Domain` all keep
private collection backing fields behind copying getters, and none is reported. The gate
tells them apart from the plants above by nothing but the `private` modifier and the
getter's expression, which is the whole reason it reads source.

Non-vacuity is asserted in the test itself, on the pattern `PublishedLanguageSourceTest`
set: it fails if fewer than 20 domain sources are scanned, and separately if fewer than 12
collection-typed properties are examined. A parser that silently stopped recognising
declarations fails rather than passes.

Six of the seven tests in `DefensiveCopySourceTest` are analyser self-tests. They pin both
historical shapes, the getter escape, the method escape, the compliant shape and the
local-variable false positive, permanently and in-repo.

### The subject's own tests use a collection of two or more

`GrantIssuedTest`'s fixtures carry **two** capabilities and `BoundedContextsTest`'s six.
`IdentityFixture`'s KDoc already records why: at size one `toSet()` returns Kotlin's
immutable singleton, the down-cast throws, and the test passes while proving nothing. Both
new copy-out tests perform the down-cast and assert the subject is unchanged, so each fails
if its copy is removed.

---

## Review round two — the blind-spot list was itself a defect

Review broke the shipped gate with five escapes, three of which it claimed to catch. The
finding is worth more than the fixes:

> **A lexical gate's stated blind-spot list is part of the gate.**
> `doc:00-constitution#observed-failing` says an unfalsifiable gate is worse than an admitted
> gap because it stops anyone looking. A gate with a **wrong** blind-spot list does exactly
> the same damage while reading as rigorous — a reader consults the list, sees their case
> named as covered, and stops. Planting violations proves only that the gate fires. **Every
> "it catches" and every "it does not catch" needs its own planted counter-example**, and the
> first version of this rule had eleven such claims and zero counter-examples.

The sharpest instance: `GrantIssued` with
`get() { if (cached.isNotEmpty()) return cached; return cached.toSet() }` passed the gate
while `GrantIssuedTest > hands out a copy…` **failed on the same code**. The gate was weaker
than the hand-written unit test it was written to replace, in the exact type this bean exists
to fix.

### The three defects

| | defect | fix |
|---|---|---|
| 1 | `isCopy` asked only whether the accessor *ended* in a copy call, and a block-bodied getter was flattened onto one line before it was asked | `isCopyChain`: a block body and every control-flow keyword are rejected outright, and the remaining spine must be a chain of calls and nothing else |
| 2 | `LEAKING_FUNCTION`'s body group was `([\w.]+)`, so only a bare identifier ever matched | functions are parsed properly — wrapped signatures, expression bodies and `return` in a block — and any returned expression rooted at a backing field that is not a copy chain is a violation |
| 3 | member-versus-local was decided by "it carries a visibility modifier, because `explicitApi()` is strict". **False:** strict mode does not require one on a member of an `internal` or `private` class | decided structurally, by brace depth relative to the enclosing type's body. A function body always opens a brace; a member never does |

### The five escapes, re-planted at real call sites and now rejected

```
E1  IdentityEvents.kt — private val cached: MutableSet<Capability>, and
      get() { if (cached.isNotEmpty()) return cached; return cached.toSet() }
observed: IdentityEvents.kt:45: GrantIssued.capabilities: Set<Capability> — the accessor is
          `{ if (cached.isNotEmpty()) return cached return cached.toSet()`, which is not a
          copy chain. It must be a plain chain of calls ending in one of toList(), toSet(),
          [...] — no block body, and none of `return`, `if`, `when`, `else`, `try`, `throw`,
          `?:` or `;`. [...]

E2  PermissionGrant.kt — get() = if (revoked) granted else granted.toList()
observed: PermissionGrant.kt:43: PermissionGrant.capabilities: List<Capability> — the
          accessor is `if (revoked) granted else granted.toList()`, which is not a copy chain.

E3  PermissionGrant.kt — two live views of the backing list
observed: PermissionGrant.kt:45: PermissionGrant.ordered() — it returns
          `granted.asReversed()`, a live view of the backing collection `granted`, not a copy
          (doc:20-ddd-practices §3.1). `asReversed`, `subList` and a bare field all write
          through.
          PermissionGrant.kt:47: PermissionGrant.head() — it returns
          `granted.subList(0, granted.size)`, a live view of [...]

E4  domainmgmt/Probe.kt — public val open: Char = '(' ahead of a leaking data class
observed: Probe.kt:10: Leaky.values: List<StateName> — a `data class` cannot own a
          collection. [...]
    (before the fix the unbalanced paren left depth permanently above zero, type headers
     stopped being recognised, and everything after it was misattributed or exempted)

E5  domainmgmt/Probe.kt — public typealias Aliased = Set<StateName>
observed: Probe.kt:8: Leaky.values: Aliased — a `data class` cannot own a collection. [...]

reverted: yes, all five
```

### Every fix is load-bearing

Each fix was reverted in turn and the matching counter-example — and only that one — failed.
This is `doc:35-testing` §6 applied to the gate's own tests.

```
cmd: ./gradlew :architecture-tests:test --tests '*DefensiveCopySourceTest*' --rerun-tasks
  isCopyChain -> the shipped endsWith test
    FAILED: rejects a conditional getter that returns the backing collection on one path
    FAILED: rejects a block-bodied getter whose last statement happens to copy   (2 of 35)
  function leak test -> bare identifiers only
    FAILED: rejects a method returning a live view of the backing collection     (1 of 35)
  member test -> "must carry a visibility modifier"
    FAILED: sees a member of an internal class, which need carry no visibility modifier
  character literals no longer stripped
    FAILED: is not derailed by a character literal containing a bracket
  typealiases no longer collected
    FAILED: follows a typealias, and an alias of an alias, to the collection it names
reverted: yes; all 35 pass
```

### The blind-spot list, re-derived by planting each claim

Every "does not catch" line above was planted at a real call site and the gate observed
**passing** it. Two of those plants failed to prove what they were written to prove, and both
turned into fixes rather than into a sentence:

```
planted:  public class Holder(private val values: List<StateName>) {
              public fun leak(): List<StateName> = values
          }
observed: BUILD SUCCESSFUL     <- intended as "nested value object", actually a parser hole:
                                  a constructor property on the header line was invisible
then:     ./gradlew :core-domain:ktlintFormat
observed: Probe.kt:12: Holder.leak() — it returns `values`, a live view of the backing
          collection `values` [...]
and:      ./gradlew :core-domain:ktlintMainSourceSetCheck on the unformatted file
observed: Probe.kt:7:21 Newline expected after opening parenthesis
          Probe.kt:7:56 Newline expected before closing parenthesis
          BUILD FAILED
```

So the formatting assumption is real but **guarded**: unformatted source is invisible to the
gate and is rejected by ktlint in the same `qualityCheck` run, and both halves are observed
rather than assumed. The second miss — a `typealias` of a `typealias` — was closed outright,
because three lines of fixed-point resolution cost less than the sentence admitting it.

---

## Review round three — plant the enabling condition, not just the claim

Round two's three fixes were all correct, all load-bearing, and **three of six new escapes
walked in underneath them** — not through the logic, through the parser's *input*.

> **It is not enough to plant every claim. You must plant every claim's enabling
> condition.**
> The function rule was correct and could not fire, because a backing field with an inferred
> type never became a `Property` and so never entered the set it compares against. The alias
> rule was correct and never saw `typealias Bag<T>`, because the name was read as `\w+` and a
> `<` cannot follow one. The member rule was correct and no owner was ever pushed for an
> anonymous `object :`. **A lexical gate's real blind-spot list is the list of shapes its
> regexes cannot see, and that is a different list from the rules it did not implement.**

The headline again, for the third round running: `GrantIssued` with
`private val issued = granted.toSet()` and `public fun raised(): Set<Capability> = issued`
passed the gate while the down-cast test on the same code failed —
`expected [agents.read, agents.run] but was [agents.read, agents.run, cost.read]`, a capability
added to a fact already stated. One character, the dropped `: Set<Capability>`.

`explicitApi()` had been deleted as an assumption for member-versus-local and was still
silently load-bearing for **type annotations**: it forces them on public API only, so every
`private` field and every member of an `internal` class was outside the parser's reach. The
assumption had been moved, not removed. It is now assumed nowhere.

### The four fixes

| | defect | fix |
|---|---|---|
| 1 | `PROPERTY` required an explicit `: Type`, so an inferred-type field was invisible | the type is optional; when absent the property's collection-ness is read from its initialiser |
| 2 | `TYPEALIAS` could not match `typealias Bag<T> = List<T>` | the generic parameter list is matched and discarded |
| 3 | `fault` enforced copy-**out** only, while the rule text, the `Enforced by:` line and the failure message all claimed both halves | copy-in enforced structurally: a collection constructor property requires a `private constructor`, and storing a parameter uncopied is a violation. The residual — a factory that forgets to copy — is named in the rule text and planted as a blind spot |
| 4 | `skeleton` deleted lambda bodies, so `steps.also { Sink.capture(it) }.toList()` reduced to a clean chain | a lambda anywhere in a copy chain is a violation. `sorted { }.toList()` must become `sortedWith(cmp).toList()`; that cost falls on the side that fails closed |

### The six escapes, re-planted at real call sites, all now rejected

```
A  IdentityEvents.kt — private val issued = granted.toSet()
                       public fun raised(): Set<Capability> = issued
observed: IdentityEvents.kt:45: GrantIssued.raised() — it returns `issued`, a live view of
          the backing collection `issued`, not a copy (doc:20-ddd-practices §3.1).

B  PermissionGrant.kt — get() = granted.also { escaped += it }.toSet()
observed: PermissionGrant.kt:44: PermissionGrant.capabilities: Set<Capability> — the accessor
          is `granted.also { escaped += it }.toSet()`, which is not a copy chain. [...]

C  PermissionGrant.kt — `private constructor` made public
observed: PermissionGrant.kt:32: PermissionGrant.granted: List<Capability> — a collection
          reaches this type through a constructor a caller can call, so it is never copied
          IN [...]
          PermissionGrant.kt:37: PermissionGrant.events: MutableList<DomainEvent> — [same]

D  domainmgmt/Probe.kt — internal class ProbeRegistry { val names = mutableListOf<String>() }
observed: Probe.kt:6: ProbeRegistry.names: <inferred> — a non-private stored collection
          property publishes the backing instance. [...]

F  domainmgmt/Probe.kt — object : Steps { override val declared: List<StateName> = steps }
observed: Probe.kt:14: <object>.declared: List<StateName> — a non-private stored collection
          property publishes the backing instance. [...]

L  domainmgmt/Probe.kt — public typealias Bag<T> = List<T>
observed: Probe.kt:8: Leaky.values: Bag<StateName> — a `data class` cannot own a
          collection. [...]

reverted: yes, all six
```

### Every fix is load-bearing, again

```
cmd: ./gradlew :architecture-tests:test --tests '*DefensiveCopySourceTest*' --rerun-tasks
  PROPERTY requires ': Type' again    FAILED: sees a property whose type is inferred
  TYPEALIAS drops the generic group   FAILED: follows a typealias, an alias of an alias, and
                                              a generic alias
  copy-in checks removed from fault   FAILED: rejects a collection that is not copied in
  lambda ban removed                  FAILED: rejects every accessor that is not a copy chain
  anonymous objects push no owner     FAILED: sees members of an anonymous object expression
each: 1 of 35 failed, and only the matching one
reverted: yes; all 35 pass
```

### The remaining blind-spot list, every entry planted and observed passing

```
factory forgets to copy   Uncopied.of(steps) = Uncopied(steps)          BUILD SUCCESSFUL
laundered through a call  = passthrough(granted)                        BUILD SUCCESSFUL
passed out as an argument sink.add(granted) in a method body            BUILD SUCCESSFUL
nested in a returned value = Wrapper(values)                            BUILD SUCCESSFUL
live inner collection     mapOf("all" to granted).toMap()               BUILD SUCCESSFUL
unrecognised initialiser  private val issued = copyOf(granted)          BUILD SUCCESSFUL
reverted: yes
```

None is reachable by a lexical scanner without a type checker, and the rule text in
`doc:20-ddd-practices` §3.1 has been narrowed to claim only what the six blocks above show.
An honest smaller claim beats a broad one with six holes.

---

## Review round four — the assumption had been renamed, not removed

Round three's fixes were correct and load-bearing, and six more escapes walked in. Three of
them were the same defect a third time, one level along.

> **A fix that replaces an assumption with an allowlist has renamed it, not removed it.**
> "Requires a type annotation" became "matches one of 32 initialiser verbs" — `.take(`,
> `.drop(`, `.chunked(` are all outside it. "No lambda" became "no `{`" — which
> `granted.also(Escapes::capture).toSet()` walks straight through. Each replacement was
> smaller than what it replaced and each failed open in the same direction. A rule stated as
> what must be **absent** does not have this failure mode: a copy chain now takes no arguments
> at all, which is checkable without knowing which verbs exist.

`explicitApi()` had been removed as an assumption for member-versus-local in round two, and
for property type annotations in round three. It was still load-bearing for **function return
types**: `explicitApi()` forces one on public API only, so `internal fun leak() = held` on a
public domain class handed out the live backing list. Three rounds, three moves, same
assumption. The audit is now written as a grep rather than an assertion — `explicitApi()`
supplies exactly three optional elements (a member's visibility modifier, a property's type,
a function's return type) and the parser needs none of them.

### The four fixes

| | defect | fix |
|---|---|---|
| 1 | `Function.collection` read a declared return type, so a function without one was never scanned | a function with no return type is judged by what its body returns: a bare backing field, or a call known to produce a view. `internal fun size() = held.size` is deliberately not one |
| 2 | copy-in claimed "the only way in is a named factory"; a public secondary constructor and an `init` block both bind the caller's collection | both are rejected, and the remaining residual — a factory that forgets to copy — is in the blind-spot table with its cost |
| 3 | the lambda ban keyed on a literal `{` | a copy chain may take no arguments at all. `sortedWith(cmp).toList()` fails closed and must be `sorted().toList()`; no accessor in `core-domain` pays that cost |
| 4 | `backing` was `filter { isPrivate && collection }`, so a field the scan could not type was not a field | `filter { isPrivate }`. Review found this one and it is the sharper lesson: the honest limit was one `filter` clause away, not a type checker away |

### The six escapes, planted at real call sites, all now rejected

```
R1  PermissionGrant.kt — internal fun raised() = events        (no declared return type)
observed: PermissionGrant.kt:45: PermissionGrant.raised() — it returns `events`, a live view
          of the backing collection `events`, not a copy.

R2a PermissionGrant.kt — public constructor(other: PermissionGrant) : this(...)
observed: PermissionGrant.kt:32: PermissionGrant.granted — the primary constructor is private,
          but a secondary constructor a caller can reach delegates to it [...]
          PermissionGrant.kt:37: PermissionGrant.events — [same]

R2b domainmgmt/Probe.kt — init { stored = supplied }
observed: Probe.kt:11: Initialised.stored — this stores a constructor parameter without
          copying it [...]

R3  PermissionGrant.kt — get() = granted.also(this::capture).toSet()
observed: PermissionGrant.kt:43: PermissionGrant.capabilities — the accessor [...] is not a
          copy chain.

R4  IdentityEvents.kt — private val issued = copyOf(granted)
                        public fun raised(): Set<Capability> = issued
observed: IdentityEvents.kt:45: GrantIssued.raised() — it returns `issued`, a live view of the
          backing collection `issued`, not a copy.

L   PermissionGrant.kt — get() = mapOf("all" to granted).toMap()   (a prior BLIND SPOT)
observed: PermissionGrant.kt:45: PermissionGrant.byName — [not a copy chain]

reverted: yes, all six
```

### Every fix load-bearing, and a caveat on what that now proves

```
cmd: ./gradlew :architecture-tests:test --tests '*DefensiveCopy*' --rerun-tasks
  a function must declare a return type again   FAILED: rejects a function with no declared
                                                        return type that hands back a backing field
  backing narrowed to collection-typed fields   FAILED: sees through an initialiser it cannot recognise
  secondary ctors and init assignments dropped  FAILED: rejects every collection that is not copied in
  argument ban reverted to the brace ban        FAILED: rejects every accessor that is not a copy chain
each: 1 of 38 failed, and only the matching one
reverted: yes
```

**Read that signal at its real strength.** After the 14 → 13 consolidation, one test carries
four accessor shapes and another carries four copy-in shapes, so "exactly one test failed" is
coarser than it was: it localises the break to a *group*, not to a shape. The shapes are
asserted individually inside the loop so a failure still names which one, but a mutation that
broke three of four accessor shapes and a mutation that broke one would both report "1 failed".

### The parser's input surface now has its own tests

`DefensiveCopyInputSurfaceTest` asserts on the **parse** — which declarations become a
`Property` or a `Function` at all — separately from `DefensiveCopySourceTest`, which asserts
on the verdict. This is review's suggestion and it is the change that ends the cycle rather
than surviving one more round of it: **every escape that survived a round arrived in the input
surface, and the rules have been correct every time.** In each case the rule's own test passed
because its fixture supplied the enabling condition the real code omitted — a declared type, a
declared return type, a named owner.

### Scope: deliberately not widened

The cheapest close for the largest blind-spot entry was measured, not estimated: scanning
`core/*/src/main` instead of `core-domain` is one constant plus a `/src/main/` filter, and it
is **green today**. It was not taken. `core-application` holds `UseCase` and one provisional
query; a use case is stateless orchestration, and a collection crossing that boundary is a
result rather than an owned thing escaping. There is no positive reason the rule should bind
it, and a gate widened because a module happens not to violate it today is a gate that fails
the next honest change — at which point the fix is a grandfather clause. `doc:20` §3.1, the
`Enforced by:` line and this bean all say `core-domain`.

---

## Review round six — argument-free was necessary and not sufficient

Four escapes, two of them reachable by ordinary code and two needing a helper planted in
`core-domain` to reach at all. All four are closed, and one of them was a **parse** bug wearing
a rule gap's clothes.

| | escape | why it worked | fix |
|---|---|---|---|
| X1 | `init { this.stored = supplied }` | `ROOT` strips `this.` everywhere in the file; `ASSIGN`, the newest pattern, did not | `ASSIGN` strips it too |
| X2 | `fun grouped(): Map<String, List<Capability>> = mapOf("all" to granted)` | the accessor path demanded a copy chain; the function path only asked whether the expression's **root** was a backing field, and the root of `mapOf(…)` is `mapOf` | the function path keys on **mention**, so both paths enforce one standard |
| X3 | `get() = held.register().toList()` | argument-free, so it passed the round-four rule, while `register()` ran a side effect on the way past | a copy chain is a field and **exactly one** copy call |
| X4 | `internal fun raised() = held.also(::noop)` | `substringAfter(':')` found the colon in `::noop` and reported a return type of `:noop)`, so an undeclared return **looked declared** and the rule that fires on undeclared returns could not fire | the return type is read from the segment before the body, then for a leading `:` |

X4 is the sharpest of the four and belongs with the input-surface lesson rather than with the
rules: the rule was correct, the fixture in its own test declared a return type, and the parse
lied about the real code. It is the fourth consecutive round in which the defect was in what the
parser could see rather than in what the rules decided.

```
X1  Probe.kt:11: Initialised.stored — this stores a constructor parameter without copying it
X2a PermissionGrant.kt:45: PermissionGrant.grouped() — it returns `mapOf( to granted)` [...]
X2b PermissionGrant.kt:45: PermissionGrant.paired() — it returns `Pair(granted, granted.size)` [...]
X3  PermissionGrant.kt:43: PermissionGrant.capabilities — [not a copy chain]
X4  PermissionGrant.kt:45: PermissionGrant.raised() — this returns something derived from a
      backing collection and declares no return type, so nothing can judge whether it hands one
      out. `explicitApi()` requires a return type on public API only; declare one here.
reverted: yes, all four
```

**X4's fix is how the `explicitApi()` assumption was finally removed rather than renamed.** Three
earlier rounds guessed the missing information — from an initialiser verb list, from a view-call
list — and each guess was an allowlist that failed open. This round the gate **requires** the one
token that settles it: a function mentioning a backing field must declare its return type. That
costs nothing (`core-domain` has no such function today) and cannot be walked around by writing
an expression the list does not name.

### Each fix load-bearing

```
ASSIGN no longer strips `this.`        FAILED: rejects every collection that is not copied in
                                       FAILED: sees a reachable secondary constructor and a
                                               qualified assignment in an init block
function rule keys on root again       FAILED: rejects every function that hands back a live view
a copy chain may be any chain          FAILED: rejects every accessor that is not a copy chain
return type read from the first colon  FAILED: reads a return type past a method reference
reverted: yes; 40 pass
```

The third of those had **no** counter-example when first run — `M17` passed — which is the
consolidation caveat below biting immediately: the accessor test carried the shapes it needed
and not the one that distinguished this fix. `held.register().toList()` was added and the
mutation then failed. A fix nothing can be observed to protect is not yet enforced.

### The input-surface class shipped incomplete, and that is recorded too

`Scan.secondaries` and `Scan.assignments` were added in the same commit as the class whose job
is to enumerate the parse, and were not enumerated — X1 was found in exactly that gap, one
commit later. `DefensiveCopy` now exposes both and `DefensiveCopyInputSurfaceTest` asserts on
them, along with the return-type parse. **A feature of the parse that nothing enumerates is a
feature nothing guards.**

### Two corrections to this bean and to `doc:20`

- The cost table's second row cited `ProcessDefinition`'s `walk`, `reachableFrom` and
  `canFinish` as collections passed to a callee. **Wrong**: those are companion functions taking
  locals (`s`, `t`, `moves`), and a companion cannot see the instance's private fields. The real
  citations are `GrantIssued.hashCode()` passing the backing `issued` to `listOf(…)`, and
  `PermissionGrant.issue` passing *locals* whose names shadow backing fields — which makes the
  point twice over. The conclusion stands; the evidence for it did not, and a false
  counter-example in a normative document is precisely what §3.1 warns against.
- `doc:20` said "every file under `core/core-domain/src/main`" while the constant rooted at the
  `..core.domain` package inside it. Identical today, divergent the moment a file is placed
  outside that package. The constant now roots at `core/core-domain/src/main`.

---

## Two residuals handed to `bean:0064`, and why they are not fixed here

Round six's review recommended merge with follow-up and named two shapes absent from the cost
table. Both are now in it, and neither is fixed in this change:

- **The leak arm is re-gated on the return type naming a collection.** Keying on *mention* was
  the round-six fix; the arm behind it still asks `function.collection`, so
  `public fun any(): Any = held` passes — a bare backing field from a public function, the
  simplest shape the rule exists to catch. Realistic containers are caught because `COLLECTION`
  matches inside the generic, so what survives is a signature nothing in `core-domain` writes.
- **The undeclared-return rule misfires and was misdescribed.** `backing` is every private field,
  so it rejects `internal fun isFrozen() = frozen`. The requirement is sound and fails closed and
  is kept; the message and the stated cost were wrong and are corrected here. An earlier draft
  claimed "no such function exists, so the cost is zero" — true of this repository, false as a
  statement about the rule.

They are documented rather than fixed because both are changes to a **live predicate**, and this
commit is the one whose job was to make the limitation list honest. Shipping a predicate change
in the same breath as the sentence describing its absence is how a limitation list stops being
readable evidence — which is the defect these six rounds exist to close, applied to itself.

`bean:0064` also carries a third thing review found, and it is the same lesson once more:
**`collectionAliases` and `PRIVATE_CTOR` are input-surface features that
`DefensiveCopyInputSurfaceTest` does not enumerate.** The only assertion on a `typealias` is on
the *verdict*, with the fixture supplying its own enabling condition — exactly the pattern that
class exists to break, for the feature its own KDoc cites as a motivating escape. A feature of
the parse that nothing enumerates is a feature nothing guards, including in the class written to
enumerate the parse.

## Criteria met — the closing evidence, per criterion

4 of 4, and a fifth thing was found.

Merged as PR #40, squashed onto `main` as `8181726`. The transcripts above were taken while
the work was written; the table below re-observes every criterion **at the merge commit**, in
a worktree at `8181726`, because that is the code `main` carries. Every `observed` cell is
that run's output verbatim, and the fences under it carry the rest of each run.

| # | criterion | command | expectation | observed |
|---|---|---|---|---|
| 1 | the gate rejects a collection-typed property that is neither private nor copied out, on both shapes the criterion names | `./gradlew :architecture-tests:test --tests '*DefensiveCopy*' --rerun-tasks` | the two historical shapes are rejected in-repo, by name, and the run is green because the rejections are what the tests assert | `BUILD SUCCESSFUL in 15s`, `44 actionable tasks: 44 executed`; `tests="10" skipped="0" failures="0" errors="0"` with `testcase name="rejects the pre-fix PermissionGrant()"` and `testcase name="rejects the pre-fix ProcessDefinition()"` — full list below |
| 2 | `private val` behind a copying getter is the fix, not the violation | the same run, plus `rule:archunit/noDomainTypePublishesACollectionItOwns` over live `core/core-domain/src/main` | the compliant shape raises nothing while the plants of it raise one violation each | `testcase name="accepts a private backing field behind a copying getter()"`, and `testcase name="noDomainTypePublishesACollectionItOwns()"` — both in the `failures="0"` run above, the second being the live scan over at least 20 sources and 12 collection properties |
| 3 | the mechanism is chosen deliberately and recorded | `grep -n "Enforced by" documentation/20-ddd-practices.md` and `grep -n "^status" .beans/modus-0026*.md` | `doc:20` §3.1 names the shipped source scan; the Detekt path is still open on `bean:0026` rather than silently dropped | ``154:  `Enforced by:` `rule:archunit/noDomainTypePublishesACollectionItOwns`, a **source** scan of`` / ``155:  every file under `core/core-domain/src/main`.`` and `4:status: todo` for `modus-0026` |
| 4 | the copy tests use a collection of size two or more | `grep -n "AGENTS_READ, AGENTS_RUN" GrantIssuedTest.kt` and `grep -n "assertEquals(6" BoundedContextsTest.kt` | every fixture is size two or more, and each test performs the down-cast the size makes possible | `GrantIssuedTest.kt:107:        capabilities: Set<Capability> = setOf(AGENTS_READ, AGENTS_RUN),` and `GrantIssuedTest.kt:56:        (taken as MutableSet<Capability>) += COST_READ`; `BoundedContextsTest.kt:11:        assertEquals(6, BoundedContexts.names.size)` and `BoundedContextsTest.kt:28:        (taken as MutableList<String>)[0] = "hijacked"` |

### Criterion 1 and criterion 2 — the run at `8181726`

```
cmd:      ./gradlew :architecture-tests:test --tests '*DefensiveCopy*' --rerun-tasks
observed: BUILD SUCCESSFUL in 15s
          44 actionable tasks: 44 executed
exit:     0

cmd:      grep -o 'testcase name="[^"]*"' \
            architecture-tests/build/test-results/test/TEST-*DefensiveCopySourceTest.xml
observed: testcase name="accepts a private backing field behind a copying getter()"
          testcase name="follows a typealias, an alias of an alias, and a generic alias()"
          testcase name="rejects every accessor that is not a copy chain()"
          testcase name="rejects every function that hands back a live view()"
          testcase name="noDomainTypePublishesACollectionItOwns()"
          testcase name="rejects every collection that is not copied in()"
          testcase name="rejects the pre-fix ProcessDefinition()"
          testcase name="rejects the pre-fix PermissionGrant()"
          testcase name="rejects a function with no declared return type that hands back a
            backing field()"
          testcase name="sees through an initialiser it cannot recognise()"

cmd:      grep -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' \
            architecture-tests/build/test-results/test/TEST-*DefensiveCopy*.xml
observed: TEST-…DefensiveCopySourceTest.xml:tests="10" skipped="0" failures="0" errors="0"
          TEST-…DefensiveCopyInputSurfaceTest.xml:tests="8" skipped="0" failures="0" errors="0"
```

`rejects the pre-fix PermissionGrant` asserts `assertEquals(1, violations.size)` and that the
violation names `PermissionGrant.capabilities`; `rejects the pre-fix ProcessDefinition`
asserts `assertEquals(3, violations.size)` and that every one gives the `data class` reason.
The rejection the criterion asks to have observed is therefore asserted on every run of the
gate from here on, not only in the transcripts above.

`noDomainTypePublishesACollectionItOwns` is the live half: it scans
`core/core-domain/src/main` and fails on any violation, with non-vacuity floors of
`MINIMUM_EXPECTED_FILES = 20` and `MINIMUM_EXPECTED_PROPERTIES = 12` so a parser that stopped
recognising declarations fails rather than passes.

### The gate is green where it is claimed to run

```
cmd:      GITHUB_TOKEN= gh run list --branch main --limit 12 --json headSha,conclusion,event,url
observed: {"conclusion":"success","databaseId":33272655061,
           "displayTitle":"feat(architecture-tests): gate defensive copying of domain collection…",
           "event":"push",
           "headSha":"8181726a43742890fe3e9cf98cac142f50fbe84b",
           "url":"https://github.com/m4xy/Modus/actions/runs/33272655061"}

cmd:      GITHUB_TOKEN= gh pr view 40 --json statusCheckRollup
observed: which halves             SUCCESS
          build + mechanical gates SUCCESS
          backoffice + e2e         SUCCESS
          gate                     SUCCESS
          (run 33272507536, https://github.com/m4xy/Modus/actions/runs/33272507536)
```

`doc:00-constitution` §9.1 asks for the mechanism observed where it is claimed to run, and
`architecture-tests` is inside `qualityCheck`, which is what the `build + mechanical gates`
job runs.

### The third occurrence is fixed on `main`

```
cmd:      grep -n "private val\|get() =" \
            core/core-domain/.../identity/event/IdentityEvents.kt core/core-domain/.../BoundedContexts.kt
observed: IdentityEvents.kt:40:    private val issued: Set<Capability> = granted.toSet()
          IdentityEvents.kt:43:    public val capabilities: Set<Capability> get() = issued.toSet()
          BoundedContexts.kt:44:    public val names: List<String> get() = declared.toList()
```

Copy in at construction, copy out at the accessor, in the type the gate found on its first
run — which is the shape §3.1 prescribes and the one criterion 2 says must not be reported.

## What else this found

Four things beyond the four requirements, kept out of the table above so that nothing here
can be read as a fifth requirement nobody wrote down before the work started:

- **A third live occurrence was found and fixed** — `GrantIssued.capabilities`, plus
  `BoundedContexts.names`. Both fixes are on `main`, verified in the block above.
- **Every claim the rule makes about itself is a planted counter-example**, in both
  directions — and the planting is now in-repo rather than in a transcript: 10 tests in
  `DefensiveCopySourceTest` and 8 in `DefensiveCopyInputSurfaceTest`, `failures="0"` in both.
  Three rounds of review found the blind-spot list wrong twice, each time because a fix's
  enabling condition had never been planted.
- **Copy-in is enforced**, structurally — a private primary constructor, no reachable
  secondary constructor, and no parameter stored uncopied — pinned by
  `testcase name="rejects every collection that is not copied in()"` in the run above. The one
  half that is not enforced is named in the rule text rather than implied by it.
- **The blind-spot list is stated as costs, not as impossibility.** Each entry names the
  cheapest change that would close it and why that change is or is not being made; two entries
  were closed rather than argued, one was measured green and still declined on scope grounds,
  and two residuals are carried by `bean:0064`.

## What this leaves for someone else

- `doc:20-ddd-practices` §3.1 was scoped to **value objects**, and §4.1.2's taxonomy puts
  domain events in §4. No rule ever reached `GrantIssued`: it was a **gap**, not a collision
  between two rules — an earlier draft of this bean called it a contradiction and was wrong.
  The finding that survives the correction is the useful one: *a rule broken three times may
  be a documentation gap rather than three careless authors*, and looking for the gap is
  cheaper than blaming the third author. §4.1.2 now closes it.
- `coverageBaselineWrite` erased the `-Pcoverage.regress` provenance comment from
  `config/coverage/baseline.tsv` twice more in this bean (`bean:0032` and `bean:0030` before
  it). The second time was worse and is new information for `bean:0033`: recording a **new**
  regression deletes the **previous** regression's block as well, so the file keeps only the
  most recent reason. Both are restored by hand here.
- The new whole-domain rule lives under the `#value-objects` anchor, which §4.1.2 and the
  `Enforced by:` line now both point at. It works, but the rule has outgrown that section;
  splitting it needs a new anchor and `provides` entry, and section numbers are never
  reallocated (`doc:README#changing-this-package`).
- The gate does not cover `core-application` or the adapters. If an owned collection ever
  crosses out of the domain, the scan root is one constant in `DefensiveCopySourceTest`.
- **A near-miss worth a rule.** Squashing this branch with `git reset --soft origin/main`
  after `origin/main` had advanced staged a diff that *reverted* everything `main` gained
  while the branch was open — three beans deleted, `doc:00` and `doc:80` rolled back — and it
  was force-pushed before the file list was read. Nothing merged and it was rebuilt onto the
  current `main` within minutes, but the failure is silent by construction: `reset --soft`
  keeps the working tree and moves the base, so the staged diff is against a tree the author
  never looked at. The safe form is `git rebase -i origin/main`, or reading
  `git diff --name-only origin/main` **before** pushing and refusing any path the bean does
  not own. `bean:0037` is the nearest owner; it already covers what a moving base does to a
  stack.
