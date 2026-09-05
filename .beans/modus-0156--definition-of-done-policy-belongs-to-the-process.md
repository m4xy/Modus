---
# modus-0156
title: A domain's definition of done is not carried by ProcessDefinition
status: todo
type: feature
priority: medium
order: AR
created_at: 2026-09-05T00:00:00Z
---

# A domain's definition of done is not carried by ProcessDefinition

`doc:00-constitution#domain-scoping` says every domain defines *its own work-item states, its
own definition of done, its own required evidence kinds*. `ProcessDefinition` carries the
first. It carries neither of the others, so there is nowhere to put a policy like **"a work
item in this domain must state at least one success criterion"**.

Raised from `bean:0152`, where the absence became load-bearing. `WorkItem.create` accepts an
empty criteria list, so an item that owes no evidence closes freely. That was reviewed twice
and is **correct as it stands**: requiring at least one criterion is a definition-of-done
policy, and hardcoding it into the aggregate would be the same defect as a hardcoded status
enum, one level up — a single process imposed on every domain by code
(`doc:00-constitution#domain-scoping`). `bean:0152` records it as an explicit decision.

Making the criteria argument mandatory rather than defaulted is as far as the aggregate can
go: it forces a caller to write `emptyList()` deliberately instead of reaching an unevidenced
item by omission. That shipped in `bean:0152`.

## What this bean adds

A place for the policy, and the guard that reads it. Note the ordering constraint: the
required-evidence-kinds half cannot be modelled before `memory` exists, for the reason
`bean:0030` gave and `bean:0152` repeated — the closed set it would override is
`doc:50-memory-and-evidence#evidence-kinds`, owned by `memory` (`bean:0015`). The
minimum-criteria half has no such dependency and can land first.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | `ProcessDefinition` carries a definition-of-done policy, per domain, as data — no constant and no enum | citation + test-run |
| 2 | A domain may require a minimum number of success criteria, and a domain that requires none still works | test-run, both cases |
| 3 | `WorkItem` refuses to be created against a policy it does not satisfy, with a named domain exception rather than `require` — it is a business rule a caller must surface | test-run, accepting and rejecting |
| 4 | Two domains with different policies drive the same aggregate, and disagree | test-run, per `doc:35-testing#fixture-variation` |
| 5 | Changing the shape of `ProcessDefinition` is a published-contract change and carries an ADR (`doc:20-ddd-practices#domain-events` §4.1.5) | ADR |

Out of scope: required evidence **kinds**, which wait on `bean:0015`.
