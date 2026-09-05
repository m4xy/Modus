---
# modus-0133
title: The drain contract is bypassable in three ways, none of them caught
status: todo
type: fix
priority: high
created_at: 2026-09-05T00:00:00Z
---

# The drain contract is bypassable in three ways, none of them caught

`bean:0066` introduced `RaisesDomainEvents` and `WriteThenDispatch` and framed them as "the
contract every future aggregate follows". The narrow claim in that bean is true and stays
true: `WriteThenDispatch.write` takes a `RaisesDomainEvents` rather than a
`(T) -> List<DomainEvent>`, so `pendingEvents` cannot be substituted **at that call site**.

The framing did more work than the mechanism does. Three bypasses. Two were planted and run
on `bean:0066`'s branch; the third is a fact about the source.

| # | bypass | what stops it today |
|---|---|---|
| 1 | A use case calls `repository.save(root)` and then `dispatcher.dispatch(root.pendingEvents)` directly, never touching `WriteThenDispatch` | nothing |
| 2 | `pendingEvents` remains public on all three roots | nothing; `bean:0066` kept it deliberately, on the bean's own instruction that the copy-out "is correct and stays" |
| 3 | A new aggregate implements `drainEvents()` as `events.toList()` with **no `clear()`** — the exact defect `bean:0066` exists to fix | nothing |

Bypass 3 is the one that matters most, because it is the defect reappearing in the shape of
its own fix, and because `bean:0013`'s `WorkItem` is being written against this contract
right now. `bean:0066`'s `DrainEventsTest` says a root missing from it is "visible as an
absence"; that is a human-noticing convention, not a mechanism, and
`doc:00-constitution#mechanical-enforcement` is explicit that a rule someone has to remember
will eventually be broken.

## Observed

Both plants reverted. Green baseline for both: `:core-domain:test` 119 tests, `:core-application:test` 24,
`:architecture-tests:test` 63, all 0 failed.

**Bypass 3.** A new aggregate root in `identity.aggregate`, shaped like a real one — private
constructor, named factory, a `pendingEvents` copy-out — whose `drainEvents()` copies and
never clears:

```
> Task :core-domain:test
> Task :architecture-tests:test
BUILD SUCCESSFUL in 15s
```

119 `:core-domain` tests and 63 `:architecture-tests`, 0 failed, with `:core-domain:ktlintCheck`
and `:core-domain:detekt` green in the same run.

**The first attempt at this plant was rejected, and reading that as an answer would have been
wrong.** A first draft declared the root with a public constructor taking a `MutableList`, and
`DefensiveCopySourceTest` failed it:

```
PlantedRoot.kt:7: PlantedRoot.events: MutableList<DomainEvent> — a collection reaches this type
through a constructor a caller can call, so it is never copied IN … Make the primary constructor
`private` and add a named factory that copies, the shape `PermissionGrant.issue` and
`ProcessDefinition.of` both use.
```

That rejection is about copying **in**, and says nothing whatever about the missing `clear()`.
A less careful plant stops here and concludes the gap is already closed — the build went red,
after all. Shaped the way a real aggregate is shaped, with a private constructor and a named
factory, nothing sees it. This is `doc:00-constitution#observed-failing`'s point in a form the
document does not yet carry: **a plant that fails for the wrong reason is a false negative for
the gap you were probing**, and the only defence is reading the message rather than the exit
code.

**Bypass 1.** A class in `core.application.identity.usecase` calling `grants.save(grant)` and
then `dispatcher.dispatch(grant.pendingEvents)`:

```
> Task :core-application:test
> Task :architecture-tests:test
BUILD SUCCESSFUL in 4s
```

24 `:core-application` tests and 63 `:architecture-tests`, 0 failed, with
`:core-application:ktlintMainSourceSetCheck` and `:core-application:detekt` green.

**Bypass 2** needs no plant: `public val pendingEvents` is on all three roots, readable in
`Domain.kt`, `Actor.kt` and `PermissionGrant.kt`.

Related but separate: `bean:0131` is about the *defensive-copy* gate failing to examine a
copy hoisted into a local. This bean is about there being no gate on the *drain* at all.

## Shapes that would close it

Recorded as options, not as a decision — `doc:00-constitution#observed-failing` warns that
enumerating accepted shapes fails open, so the requirement should be the one that fails
closed.

- An ArchUnit or source rule: every type in `..domain.<ctx>.aggregate..` implementing
  `RaisesDomainEvents` must have a `drainEvents` body that clears its backing field. This is
  bytecode- or source-shaped and is the direct answer to bypass 3.
- A rule that no type outside `..core.application.event..` reads `pendingEvents` — answering
  bypass 1 and making bypass 2 harmless. Note this must be scoped to production code:
  `DrainEventsTest` reads `pendingEvents` on purpose.
- Deleting `pendingEvents` outright, and having the tests that need it drain instead. The
  cheapest option, and the one that needs an argument against `bean:0066`'s instruction to
  keep it rather than a mechanism.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | Bypass 3 is rejected: a planted `drainEvents()` that copies without clearing fails the build, observed failing and reverted, and the three correct implementations stay green | |
| 2 | Bypass 1 is rejected, or is argued in writing to be acceptable and the claim in `WriteThenDispatch`'s KDoc corrected to match | |
| 3 | Whatever `pendingEvents` becomes — gated, scoped or deleted — the decision is recorded against `bean:0066`'s instruction to keep it, not silently reversed | |
| 4 | The non-vacuity of any new rule is asserted on its own input surface, not only through its verdict (`doc:35-testing#load-bearing-evidence`) | |
| 5 | `./gradlew qualityCheck` green | |
