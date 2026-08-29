---
# modus-0064
title: Three residuals in the defensive-copy gate, two in a predicate and one in the input surface
status: todo
type: fix
priority: normal
created_at: 2026-08-29T00:00:00Z
---

# Three residuals in the defensive-copy gate, two in a predicate and one in the input surface

`bean:0036` shipped `rule:archunit/noDomainTypePublishesACollectionItOwns` after six review
rounds. Three things were found in the last round, documented rather than fixed, because the
first two are changes to a live predicate and the third is a gap in a test class — none belongs
in the commit whose job was to make the limitation list honest.

`doc:20-ddd-practices#value-objects` §3.1's cost table carries the first as a row and the
second in the paragraph above it. Both name this bean.

## 1. The leak arm is re-gated on the return type naming a collection

`DefensiveCopy.leak` keys on **mention** of a backing field, which was the round-six fix, and
then re-gates the leak arm on `function.collection`. A declared return type naming no collection
therefore disables the check. All three compile under `-Werror`, pass ktlint and Detekt, and
pass the gate:

```
public fun any(): Any = held
public fun paired(): Pair<Any, Int> = Pair(held, held.size)
public fun iterate(): Iterator<StateName> = held.iterator()
```

The first is a bare backing field returned from a public function — the simplest shape the rule
exists to catch.

**Mitigating, and the reason this is a residual rather than a defect:** realistic containers
*are* caught, because `COLLECTION` matches inside the generic — `Map<K, List<V>>` and
`Pair<List<X>, Int>` both fire. What survives is a return type naming no collection anywhere,
which nothing in `core-domain` writes.

## 2. The undeclared-return rule misfires, and its message misdescribes it

`backing` is every private field, of any type, so the undeclared-return rule fires on
`internal fun isFrozen() = frozen` — a `Boolean` — with the message "this mentions a private
field and declares no return type". The requirement is sound and fails closed and should be
kept; the breadth is deliberate, because a field whose type the scan cannot read is the case the
widened `backing` exists for. But the rule is stated in §3.1 as if only collections were
affected, and `bean:0036`'s KDoc originally claimed the cost was zero.

The message was corrected in `bean:0036`; the predicate was not.

## Success criteria

1. `leak`'s second arm no longer depends on the declared return type naming a collection: a
   returned expression that mentions a backing field and is not a copy is a violation whatever
   the signature says. Observed rejecting `public fun any(): Any = held` planted at a real call
   site in `core-domain`, and observed passing `internal fun size(): Int = held.size`
   (`doc:00-constitution#observed-failing`).
2. The undeclared-return rule is either narrowed to fields the scan can read as collections, or
   kept and its cost restated in §3.1 as a deliberate style constraint on `core-domain` rather
   than as a collection rule. Whichever is chosen, the failure message and §3.1 agree with the
   predicate. Observed on `internal fun isFrozen() = frozen`.
3. `DefensiveCopyInputSurfaceTest` enumerates `collectionAliases` and `PRIVATE_CTOR`/
   `inConstructor`. Both are input-surface features and neither is asserted on there today: the
   only assertion on a `typealias` is on the **verdict**, in `DefensiveCopySourceTest`, with the
   fixture supplying its own enabling condition — which is exactly the pattern that class exists
   to break, and which `bean:0036`'s own KDoc names `typealias Bag<T>` as a motivating escape
   for. Observed failing when the alias collection or the private-constructor detection is
   reverted.
4. No criterion above is claimed until its mechanism has been watched rejecting a planted
   violation, per `bean:0036`'s own record: **a feature of the parse that nothing enumerates is
   a feature nothing guards**, and a fix nothing can be observed to protect is not yet enforced.
