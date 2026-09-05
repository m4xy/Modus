---
# modus-0132
title: No durable event log exists, so §4.1.7 has never held and dispatch loses events
status: todo
type: feature
priority: high
created_at: 2026-09-05T00:00:00Z
blocked_by: [modus-0017]
---

# No durable event log exists, so §4.1.7 has never held and dispatch loses events

`doc:20-ddd-practices#domain-events` §4.1.7:

> Every event is appended to the durable event log before any handler runs. Handlers are
> idempotent — they may be replayed.

**There is no durable event log.** Nothing in this repository appends a domain event
anywhere: `rg` over `core/`, `adapters/` and `app/` finds no writer, and
`adapters/adapter-persistence-flatfile` is still a placeholder with no tests (`bean:0017`).
So the first clause has never held for a single event, and the second clause asks for a
property — idempotency — whose only purpose is to survive a replay that nothing can perform.

The rule carried **no `Enforced by:` and no `Enforcement gap:` line** until `bean:0066`,
unlike §2, §3 and §5.1 which all carry one. That is what let a handler contract be written
citing §4.1.7 for idempotency while the sentence before it was unmet one class away. The
gap note now names this bean.

## The consequence that is not merely cosmetic

`bean:0066` shipped `InProcessDomainEventDispatch` with the failure semantics now recorded
as §4.1.8: a handler that throws propagates, and delivery of that batch stops there.
Delivery is all-or-a-prefix.

**The undelivered suffix is lost permanently.** Nothing recorded that those events were due,
so nothing can replay them. A `GrantRevoked` that `domainmgmt` refuses takes every event
behind it — in that batch and, once more than one consumer exists, for every consumer after
the failing one — with it, silently.

This is **not** an argument against fail-fast, and the alternative does not fix it: running
every handler and collecting the failures discards exactly the same events on the next
crash, and adds a summary no consumer reads. The fix is the log. Until it exists, every
delivery guarantee this system offers is "best effort, on one thread, once".

## Why it is blocked

`bean:0017` builds the flat-file store. An append-only NDJSON log is what
`doc:00-constitution` §2.3 already prescribes for "machine-facing, high-volume, append-only
data (agent run events, cost events, audit trail)", and
`doc:15-repository-layout#cross-cutting-flows` §6.1 already puts the append before the
fan-out. Neither the format nor the ordering is an open question; the writer is.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | Every dispatched event is appended to a durable log before any handler runs, observed by a test in which the append fails and no handler is reached | |
| 2 | A handler failure no longer loses the suffix: the events behind it are recoverable from the log, observed by replaying a batch whose middle handler threw | |
| 3 | Handler idempotency is asserted for at least one real handler under replay, rather than asked for in prose | |
| 4 | §4.1.7's `Enforcement gap:` line is replaced by an `Enforced by:` naming a mechanism observed rejecting a planted violation (`doc:00-constitution#observed-failing`), and §4.1.8's "lost permanently" clause is corrected in the same change | |
| 5 | `./gradlew qualityCheck` green | |
