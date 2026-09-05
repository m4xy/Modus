---
# modus-0153
title: The work use cases and the ProcessDefinitionChanged handler
status: todo
type: feature
priority: high
order: AO
created_at: 2026-09-05T00:00:00Z
parent: modus-0013
blocked_by: [modus-0152, modus-0066]
---

# The work use cases and the ProcessDefinitionChanged handler

Second child of `bean:0013`. `bean:0152` builds the model; this bean is everything that
drives it, and the half of `bean:0013`'s first criterion that a branch cut from `main`
cannot compile: **`ProcessDefinitionChanged` consumed.**

**`bean:0066` merged as PR #83 at `99212fc` while `bean:0152` was in review**, so its types
now exist: `RaisesDomainEvents` in `core-domain`, and `DomainEventDispatchPort`,
`DomainEventHandler` and `EventSubscription` in `core-application`. The dispatcher itself
landed in `adapters/adapter-events-inprocess`, not `core-application`, on the review ruling —
so this bean's handler registration must not assume otherwise. `DomainEventHandler.handle`
now states that a handler is called **not necessarily** on the writing thread and not
necessarily before the use case returns; nothing here may assume synchronous delivery.

The `blocked_by` edge stays until `modus-0066` is `completed`, which is a separate change
after its merge (`doc:00-constitution#bean-lifecycle` §7.2.1). `completed` means `completed`.

Blocked by `bean:0066` and not merely sequenced after it. A consumer is a use case
(`doc:20-ddd-practices#domain-events` §4.1.4), and every type it is registered against —
`DomainEventHandler`, `EventSubscription`, the dispatcher — arrives with that bean, as does
`RaisesDomainEvents` and the `DrainEventsTest` case each new root has to add. `completed`
means `completed` (`doc:00-constitution#bean-lifecycle`): the shapes are still under review
and building against an unmerged branch is building against something that can still change.

## Scope

Owned: `core/core-application/src/{main,test}/kotlin/uk/m4xy/modus/core/application/work/**`,
the `work` half of `core-domain`'s `DrainEventsTest`, whatever port the handler needs in
`..domain.work.port`, and `config/coverage/baseline.tsv`.

Not owned: `core-domain`'s `work` model, which `bean:0152` ships; `adapters/**`;
`app/**` wiring, which arrives with the adapter that implements the ports.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `WorkItem` implements `uk.m4xy.modus.core.domain.aggregate.RaisesDomainEvents` and `DrainEventsTest` gains a case for it. **`Epic` does not** — it raises no event, so there is nothing for it to drain; that absence is asserted by `EpicTest` and recorded in `bean:0013` | test-run |
| 2 | A handler in `core.application.work.usecase` consumes `ProcessDefinitionChanged`, registered as an `EventSubscription`, and imports nothing of `domainmgmt` beyond its published language | test-run + citation |
| 3 | The process a domain imposes is available to a transition without a port back into `domainmgmt`: the handler records what it saw, and the transition use case reads it | test-run |
| 4 | An older redelivered `ProcessDefinitionChanged` does not move a domain's work back onto a process it has left (`Domain.drainEvents`' KDoc names this hazard) | test-run |
| 5 | Every use case writes through `write.write(root) { repo.save(it) }`, never `repo.save(root)` directly | citation |
| 6 | The transition use case resolves the process from the work item's `domainId` and never accepts one from its caller, closing `bean:0157`'s bypass end to end | test-run, observed failing first |
| 7 | `doc:00-constitution` §3's `Enforcement gap:` no longer says the `work` transition guard does not exist because "`work` is not built" — `bean:0152` built it. The correction is net-neutral on line count: `doc:00` is at 500/500, its own ceiling | citation + `docs-lint` check 8 |
| 8 | Each test is load-bearing; `./gradlew qualityCheck` green; baseline moved with its regression-provenance block intact | test-run |

## Decisions deferred to this bean

Whether the process projection is a port in `..domain.work.port` or a `core-application`
port turns on where the dispatcher settles, which is `bean:0066`'s open review question.
`bean:0152` deliberately declares neither.
