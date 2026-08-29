---
id: adr:0004-domain-id-shared-kernel
title: DomainId is shared kernel, not identity's published language
status: active
superseded_by: null
read_when:
  - path: core/core-domain/**
  - task: shared kernel|DomainId|published language|cross-context|bounded context boundary
provides:
  - adr:0004-domain-id-shared-kernel#shared-kernel-decision
  - adr:0004-domain-id-shared-kernel#shared-kernel-membership
  - adr:0004-domain-id-shared-kernel#deferred-conflict
depends_on: [doc:10-architecture, doc:20-ddd-practices]
---

# ADR 0004 — `DomainId` is shared kernel, not `identity`'s published language

- **Date:** 2026-08-29
- **Deciders:** Modus core
- **Supersedes:** —

## Context

`doc:10-architecture#bounded-contexts` §3.1 states two rules that cannot both hold once a
second context exists.

| rule | says |
|---|---|
| the allowlist table, matching §3's `Consumes` column | `domainmgmt` MAY import the published language of `identity` |
| the leaf paragraph, implemented as `rule:archunit/publishedLanguageIsLeaf` | a type in `..domain.event..` or `..domain.published..` may reference only the Kotlin stdlib, `java.time`, and **its own context's** `..domain.published..` |

Every context's events name the domain they concern: `DomainCreated`, `WorkItemCreated`,
`AgentRunStarted` and `SpendRecorded` all carry a `DomainId`. `DomainId` was declared in
`identity.published` because `identity` was the first context modelled (`bean:0009`). The
first event outside `identity` that names a domain therefore violates the leaf rule.

Observed, on the minimal case, before any of this was designed:

```
planted:  domainmgmt/event/Probe.kt — data class Probe(val domainId: identity.published.DomainId, …)
observed: ArchitectureRulesTest > publishedLanguageIsLeaf FAILED
          Method <…domainmgmt.event.Probe.equals(java.lang.Object)> calls method
            <…identity.published.DomainId.equals-impl0(java.lang.String, java.lang.String)>
          23 tests completed, 1 failed
```

The rule had been passing on an empty set: `identity` imports no other context, so nothing
it quantified over existed. This is the vacuity `doc:00-constitution#observed-failing` was
written about, reached one bean earlier than `bean:0023` expected.

## Decision <a id="shared-kernel-decision"></a>

**`DomainId` moves out of every bounded context and becomes shared kernel, beside the
`DomainEvent` marker.** Both §3.1 rules survive unchanged.

1. `DomainId` lives at `uk.m4xy.modus.core.domain.DomainId`. Its invariant, its failure
   message and its case-sensitivity reasoning move unaltered.
2. `rule:archunit/publishedLanguageIsLeaf` exempts it **by name**, exactly as it already
   exempts `DomainEvent`. There is no package wildcard: a wildcard is an exemption that
   widens whenever someone adds a file.
3. A new rule, `rule:archunit/sharedKernelIsLeaf`, holds the kernel to the same standard it
   lets others rely on. Every context imports it, so a dependency added here is one every
   context inherits without seeing it.

It was never `identity`'s concept. `DomainId`'s own KDoc says so — *"the tenant identifier
that scopes every other bounded context"* — and `identity` uses it exactly as `work` and
`cost` will: to say which tenant a fact belongs to.

### Membership, and how it may grow <a id="shared-kernel-membership"></a>

The shared kernel is `DomainEvent` and `DomainId`, and nothing else. A type joins only if
**every** statement below is true of it; a type that merely happens to be used twice does
not qualify.

| # | test |
|---|---|
| 1 | It belongs to no bounded context — no context could claim ownership without the others having to ask permission to name it. |
| 2 | It appears in the published language of more than one context, or must, for the model to be expressible. |
| 3 | It is a leaf: stdlib and `java.time` only, and it stays one. |
| 4 | Adding it is an ADR, because it widens what every context imports unseen. |

**On the third member, the kernel gets its own package.** Two types are scoped by name in
`ArchitectureRulesTest.SHARED_KERNEL` and that is legible; a name list of four is a place
things get added quietly. At three, move them to `..domain.kernel` and scope both rules
structurally, the way `..domain.aggregate` scopes `AggregatesAreSealedOrFinal`
(`doc:20-ddd-practices` §5.1).

### What this ADR does not settle <a id="deferred-conflict"></a>

It resolves one instance and **defers the rule conflict itself**. §3.1's allowlist table and
its leaf paragraph still contradict each other for any type that is genuinely a context's
own; moving `DomainId` out of the argument does not reconcile them. `bean:0023` —
`ContextInternalsAreSealed` and `PublishedLanguageAllowlist` — is where that is settled, and
this decision should be re-read when it lands.

The pressure recurs immediately and cannot be answered the same way twice. `execution`'s
`AgentRunStarted` and `cost`'s `SpendRecorded` both attribute to an actor, so both will want
`ActorId` in a published signature. `ActorId` **fails** membership test 1: `identity`
genuinely owns what an actor is, and no other context can name one without asking. So the
kernel cannot absorb it, and the next occurrence must be resolved by the allowlist rather
than by promotion. Promoting a type to dodge the leaf rule is precisely how a shared kernel
becomes the junk drawer the Negative consequences below warn about — which is also why the
three-member trigger is a brake on the symptom rather than a plan.

## Consequences

### Positive

- Both §3.1 rules stay exactly as ratified; neither is weakened to fit.
- One tenant is one type. There is no boundary at which two `DomainId`s must be reconciled.
- `sharedKernelIsLeaf` closes a hole that predates this ADR: `DomainEvent` was exempt from
  the leaf rule and was itself checked by nothing.

### Negative

- The shared kernel is a real coupling and it now has two members rather than one. The
  membership test above exists because the second member is where a shared kernel starts
  becoming a junk drawer.
- Scoping by name means Kotlin's generated classes must be handled explicitly: a
  `private companion object` is `DomainId$Companion` and a top-level `private val` is a
  `DomainIdKt` file facade. Both were observed failing `sharedKernelIsLeaf` on its first two
  runs. Membership is decided on the outermost enclosing class — walked structurally via
  `JavaClass.getEnclosingClass()`, not by splitting the binary name on `$`. Review showed the
  textual split was forgeable: Kotlin permits `$` in a backticked type name, so a top-level
  `` `DomainId$Evil` `` in this package joined the kernel with the build green and nobody
  editing the member list. `DomainId` also keeps its regex in a companion rather than at file
  scope so no facade is generated at all.
- The rule sees only what reaches bytecode. A `@JvmInline value class` held in a plain field
  erases to its underlying type and leaves no edge, so `publishedLanguageIsLeaf` does not
  catch every cross-context reference it claims to. `bean:0034` carries that; it is older
  than this ADR and is not created by it.

### Neutral

- `identity.published` keeps `ActorId`, `GrantId`, `Capability` and `ActorKind`. Those are
  genuinely `identity`'s: no other context can name an actor without asking `identity` what
  one is.

## Alternatives considered

| alternative | rejected because |
|---|---|
| Relax `publishedLanguageIsLeaf` to allow any context's published package, leaving the per-context restriction to `PublishedLanguageAllowlist` | `PublishedLanguageAllowlist` does not exist (`bean:0023`), so until it does every cross-context published import would pass — `cost.event → memory.published` included, which §3.1's table forbids outright. That is the whole of the rejection, and it is a **timing** argument. "It weakens a ratified rule" is not a second reason and was struck: §3.1's two statements contradict each other, so declining to weaken one does not select between them |
| `DomainId` belongs to `domainmgmt.published` — that context owns domain lifecycle and publishes `DomainCreated` — with the leaf rule consulting §3.1's allowlist | the strongest alternative, and it is the end state `bean:0023` builds toward. Rejected **for now** on the same timing ground: it needs the allowlist rule to exist first. It also fails membership test 1 below in the opposite direction — `identity` would have to ask `domainmgmt` permission to name a tenant, and `identity` predates it |
| Give `domainmgmt` its own `DomainId` | two structurally identical, mutually unequal types for one tenant, at every boundary between the two contexts. `identity`'s case-sensitivity reasoning — `Modus-Core/` and `modus-core/` are one directory on APFS and NTFS — would be duplicated or silently lost in the copy |
| Move `DomainEvent` into the kernel package now, and `DomainId` with it | the right end state, and out of scope for a bean that moves one type. Recorded above as the trigger at three members |
