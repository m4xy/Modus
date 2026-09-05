---
# modus-0130
title: The §5.1 placement table has no room for two packages that now exist
status: todo
type: fix
priority: medium
created_at: 2026-09-05T00:00:00Z
---

# The §5.1 placement table has no room for two packages that now exist

`bean:0066` added two packages that `doc:20-ddd-practices#ports-and-adapters` §5.1's
placement table does not carry a row for:

| package | holds | belongs to |
|---|---|---|
| `uk.m4xy.modus.core.domain.aggregate` | `RaisesDomainEvents`, the drain contract every aggregate root implements | no bounded context |
| `uk.m4xy.modus.core.application.event` | the dispatch port, the handler contract, `EventSubscription`, `SynchronousDomainEventDispatch`, `WriteThenDispatch` | no bounded context |

Both are argued in their own KDoc against §5.1 and `adr:0004-domain-id-shared-kernel#shared-kernel-membership`,
and both follow the precedent §5.1 already sets with `uk.m4xy.modus.core.domain.port` — a
subpackage of the shared kernel's package that is not a member of the shared kernel, existing
because the thing it holds belongs to no context. That argument is in a source file, which is
the wrong home for a placement rule: §5.1 is normative and a reader consulting it will not
find these two.

**The row was not added because the file has no room.** `documentation/20-ddd-practices.md` is
499 lines against the `max_lines: 500` budget `adr:0003` sets and `docs-lint` check 8 enforces,
so §5.1 cannot gain a line — let alone the two rows and the sentence each needs — without
something else leaving. `documentation/README.md#changing-this-package` states what a file at
the ceiling means: *"A file that outgrows the ceiling is two files, or it contains material
that belongs in an ADR or in a skill."* Deciding which of those `20-ddd-practices` is, is this
bean, and it is not a change `bean:0066` could make on the way past.

The `core-application` half is the sharper of the two. §5.1's table carries exactly one
`core-application` row — `uk.m4xy.modus.core.application.<ctx>.usecase` — so it has no answer
at all for an application-layer type that belongs to no context, and it had none before
`bean:0066`; the packages that bean added are the first to ask the question.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | `documentation/20-ddd-practices.md` is under the 500-line budget with both rows added, by a split, an extraction or a deletion that is argued rather than incidental | |
| 2 | §5.1 carries a row for each of the two packages, and the KDoc in `RaisesDomainEvents.kt` and the `core.application.event` types cites it rather than restating it (`doc:05-authoring-for-agents#one-fact-one-place`) | |
| 3 | `./gradlew qualityCheck` green, `docs-lint` check 8 included | |
