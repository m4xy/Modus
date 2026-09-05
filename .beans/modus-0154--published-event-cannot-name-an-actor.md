---
# modus-0154
title: The documented event snippet names a type its own leaf rule forbids
status: todo
type: fix
priority: medium
order: AP
created_at: 2026-09-05T00:00:00Z
---

# The documented event snippet names a type its own leaf rule forbids

`doc:20-ddd-practices#domain-events` §4.1 carries a worked example of a domain event:

```kotlin
package uk.m4xy.modus.core.domain.work.event

data class WorkItemTransitioned(
    val workItemId: WorkItemId,
    val domainId: DomainId,
    val from: WorkItemState,
    val to: WorkItemState,
    val actorId: ActorId,
    override val occurredAt: Instant,
) : DomainEvent
```

`ActorId` is `identity.published.ActorId`. `rule:archunit/publishedLanguageIsLeaf` — which
exists, runs, and has been observed rejecting exactly this shape (`adr:0004-domain-id-shared-kernel`)
— permits a type in `..domain.<ctx>.event..` to reference only the Kotlin stdlib,
`java.time`, **its own** context's `..published..` and the shared kernel. So the most-copied
event example in the package cannot be built. `doc:20-ddd-practices#aggregates` §2.2's
aggregate snippet has the same problem one layer in: it passes `actorId` to the event.

The two statements do not contradict each other loudly, which is why this survived. The
§3.1 allowlist genuinely does permit `work` to import `identity`'s published language — from
its **internals**. The leaf rule is what stops a *published* package doing it, and §4.1's
snippet is in one.

Found while implementing `bean:0152`, which left attribution out of `work`'s events
entirely rather than model it where it cannot be published.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `doc:20-ddd-practices` §4.1's and §2.2's snippets compile under the rules the package states, or carry the reason they do not | citation |
| 2 | The decision on attribution is recorded: either `work`'s events name no actor, or `ActorId` becomes a shared-kernel member — which needs an ADR superseding nothing but extending `adr:0004-domain-id-shared-kernel#shared-kernel-membership`'s two-member set | ADR or a documented refusal |
| 3 | `doc:20-ddd-practices#invariants` §7.2's repository-wide sealed `DomainException` is either declared (an ADR — it is a third kernel member) or restated as per-context, which is what `work` does today | citation |
