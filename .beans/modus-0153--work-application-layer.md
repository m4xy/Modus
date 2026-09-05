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
| 1 | `WorkItem` and `Epic` implement `uk.m4xy.modus.core.domain.aggregate.RaisesDomainEvents`, and `DrainEventsTest` gains a case for each | test-run |
| 2 | A handler in `core.application.work.usecase` consumes `ProcessDefinitionChanged`, registered as an `EventSubscription`, and imports nothing of `domainmgmt` beyond its published language | test-run + citation |
| 3 | The process a domain imposes is available to a transition without a port back into `domainmgmt`: the handler records what it saw, and the transition use case reads it | test-run |
| 4 | An older redelivered `ProcessDefinitionChanged` does not move a domain's work back onto a process it has left (`Domain.drainEvents`' KDoc names this hazard) | test-run |
| 5 | Every use case writes through `write.write(root) { repo.save(it) }`, never `repo.save(root)` directly | citation |
| 6 | Each test is load-bearing; `./gradlew qualityCheck` green; baseline moved with its regression-provenance block intact | test-run |

## Decisions deferred to this bean

Whether the process projection is a port in `..domain.work.port` or a `core-application`
port turns on where the dispatcher settles, which is `bean:0066`'s open review question.
`bean:0152` deliberately declares neither.
