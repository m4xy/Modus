---
# modus-0157
title: A foreign process that declares an item's state bypasses the evidence guard
status: todo
type: fix
priority: high
order: AS
created_at: 2026-09-05T00:00:00Z
---

# A foreign process that declares an item's state bypasses the evidence guard

`doc:00-constitution#evidence-rule` refuses a work-item transition to done without evidence.
`WorkItem.transitionTo` enforces it against **the process it is handed**, and nothing binds a
work item to the process of *its own* domain. So the guard is bypassed by handing it a
different one.

Found in review of `bean:0152`, by an independent reviewer, and **not closed there**. What
shipped in `bean:0152` is a partial check that catches a strictly smaller case.

## The bypass, observed

An `ENGINEERING` item at `doing` with three unevidenced criteria, moved to `shipped` under a
process that permits `doing -> shipped` and calls `shipped` an ordinary intermediate:

```
observed: SUCCEEDED state=shipped closed=false
```

The move is permitted, the target is not terminal under the process supplied, so no evidence
is owed and no `WorkItemClosed` is raised. The item is then in `shipped` — which its **own**
domain's process declares terminal and permits no exit from. Closed, with nothing proved, and
no event saying it closed.

`WorkItemEvidenceGuardTest > a foreign process declaring this item's state still bypasses the
close guard - bean 0157` pins this, deliberately, as a characterisation test. **Closing this
bean means rewriting that test to assert the refusal.**

## Why the obvious fix does not work

The reviewer proposed `require(currentState.asStateName() in process.states)` at the head of
`transitionTo` and `recordEvidence`. That guard shipped, and it is worth having, but it does
**not** close this: any process permitting a move out of `doing` must declare `doing`
(`ProcessDefinition` refuses a transition naming an undeclared state), so membership is
implied by the move being permitted at all. It was implemented, and the probe above still
succeeded against it. The guard catches a process that cannot describe the item at all —
handing an item at `doing` a process that has no `doing`. That is a different, smaller case.

Nothing inside the aggregate can close it. Binding the item to a process means either holding
the process — caching another aggregate's state, stale the moment `Domain.adoptProcess` runs —
or giving `ProcessDefinition` an identity, which is `domainmgmt`'s to decide.

## Where it belongs

The use case. `WorkItem` has a `domainId`; the transition use case must load the process for
**that** domain and never accept one from its caller. `bean:0153` builds those use cases, and
this is a constraint on how, not a separate mechanism.

## Success criteria

| # | criterion | evidence kind |
|---|---|---|
| 1 | No caller of `WorkItem.transitionTo` or `recordEvidence` outside `core-domain`'s tests can choose the process: the use case resolves it from the item's `domainId` | citation + test-run |
| 2 | The probe above is refused end to end through the use case — a caller naming a work item and a target state cannot reach a process that is not its domain's | test-run, observed failing first |
| 3 | The characterisation test in `WorkItemEvidenceGuardTest` is rewritten to assert the refusal, and its `bean 0157` suffix removed | `git diff` |
| 4 | The partial check `requireGoverning` is kept or removed with a stated reason, not left undescribed | citation |
| 5 | `doc:00-constitution` §3's `Enforcement gap:` reflects what the guard actually covers once this lands | citation, net-neutral on line count |

Blocks nothing; `bean:0153` should not close while criterion 1 is unmet.
