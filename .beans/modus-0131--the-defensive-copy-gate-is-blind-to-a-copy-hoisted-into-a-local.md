---
# modus-0131
title: The defensive-copy gate is blind to a copy hoisted into a local
status: todo
type: fix
priority: high
created_at: 2026-09-05T00:00:00Z
---

# The defensive-copy gate is blind to a copy hoisted into a local

`DefensiveCopy.kt`'s `leak` check decides in two steps
(`architecture-tests/src/test/kotlin/uk/m4xy/modus/architecture/DefensiveCopy.kt:181-201`):

```kotlin
val field = mentioned(expression, backing) ?: return null
…
function.collection && !isCopyChain(expression) -> "$where — ${leakedBy(expression, field)}"
```

`mentioned` is a whole-word regex match of a private property's **name** against the text of
a returned expression (`:203-206`). `expression` is the text after `return`, or after `=` for
an expression body (`:495`, `:504-515`). Nothing resolves anything.

So a non-private function that assigns the backing field to a local and returns the local is
never examined: `mentioned("drained", {granted, revoked, events})` finds nothing, the guard
short-circuits, and neither arm of the rule is reached.

```kotlin
public fun leak(): List<Capability> {          // rejected today
    return granted
}

public fun leak(): List<Capability> {          // identical defect, gate green
    val out = granted
    return out
}
```

**Observed, both halves, in `bean:0066`'s worktree.** Unfiltered `:architecture-tests:test`
runs — the whole module, 63 tests, not the 34 in `DefensiveCopySourceTest` alone. A function
was planted in `PermissionGrant.kt` in the direct form and the gate rejected it:

```
> Task :architecture-tests:test --rerun-tasks
DefensiveCopySourceTest > noDomainTypePublishesACollectionItOwns() FAILED
      core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain/identity/aggregate/PermissionGrant.kt:58: PermissionGrant.leakDirect() — it returns `granted`, a live view of the backing collection `granted`, not a copy (doc:20-ddd-practices §3.1). `asReversed`, `subList` and a bare field all write through.
63 tests completed, 1 failed
BUILD FAILED in 9s
```

The same function, changed only from `return granted` to `val out = granted; return out` —
same defect, same call site, one extra line:

```
> Task :architecture-tests:test --rerun-tasks
BUILD SUCCESSFUL in 4s
```

63 tests, 0 failed. Both plants reverted. This is not a tolerated shape. It is the same class of defect as the four the gate has
already been walked past — `bean:0036`, `bean:0064` — and the same class as `bean:0034`'s
finding that `PublishedLanguageIsLeaf` could not see an erased reference. `doc:00-constitution#observed-failing`'s
own summary of that history is the rule to apply: *"enumerating the shapes a gate accepts
fails open; requiring the token that settles the question fails closed."* One-line dataflow
inside a function body is exactly the kind of enumeration the gate has no business doing, so
the fix is likely to be a requirement rather than an analysis — for example, that a
collection-returning non-private function in `core-domain` must return a copy chain
syntactically, with no intermediate local.

Found while writing `bean:0066`, whose `drainEvents` uses precisely this shape:

```kotlin
override fun drainEvents(): List<DomainEvent> {
    val drained = events.toList()
    events.clear()
    return drained
}
```

That implementation is correct — it copies, and the copy is what makes the following
`clear()` safe — and it cannot be written as a single copy chain, because `isCopyChain`
(`:284-293`) accepts one receiver and one argument-free call and rejects
`events.toList().also { events.clear() }` on both the lambda and the second call. So the
three roots `bean:0066` touches are three functions the gate does not examine, and its
non-vacuity floors (≥20 files, ≥12 collection-typed properties, `DefensiveCopySourceTest.kt:400`,
`:403`) are unmoved by that, because they count files and properties rather than functions.

The non-vacuity floors also count 34 tests inside one class; the module runs 63. Neither
figure is the one that matters here, and this bean says so rather than letting a filtered
count stand in for a suite.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | A planted `val out = granted; return out` in a non-private `core-domain` function is rejected, observed failing before the fix and green after (`doc:00-constitution#observed-failing`) | |
| 2 | The three `drainEvents` implementations `bean:0066` added are examined by the gate rather than skipped by it, and pass | |
| 3 | A non-vacuity assertion counts the **functions** the gate examined, not only the files and properties it saw — the floors at `DefensiveCopySourceTest.kt:400` and `:403` cannot notice this class of skip | |
| 4 | `./gradlew qualityCheck` green | |
