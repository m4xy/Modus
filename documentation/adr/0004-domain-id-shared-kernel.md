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
  runs. Membership is decided on the outermost enclosing class, and `DomainId` keeps its
  regex in a companion rather than at file scope so no facade is generated at all.

### Neutral

- `identity.published` keeps `ActorId`, `GrantId`, `Capability` and `ActorKind`. Those are
  genuinely `identity`'s: no other context can name an actor without asking `identity` what
  one is.

## Alternatives considered

| alternative | rejected because |
|---|---|
| Relax `publishedLanguageIsLeaf` to allow any context's published package, leaving the per-context restriction to `PublishedLanguageAllowlist` | weakens a ratified rule, and `PublishedLanguageAllowlist` does not exist (`bean:0023`). Until it does, every cross-context published import would pass — `cost.event → memory.published` included, which §3.1's table forbids outright |
| Give `domainmgmt` its own `DomainId` | two structurally identical, mutually unequal types for one tenant, at every boundary between the two contexts. `identity`'s case-sensitivity reasoning — `Modus-Core/` and `modus-core/` are one directory on APFS and NTFS — would be duplicated or silently lost in the copy |
| Move `DomainEvent` into the kernel package now, and `DomainId` with it | the right end state, and out of scope for a bean that moves one type. Recorded above as the trigger at three members |
