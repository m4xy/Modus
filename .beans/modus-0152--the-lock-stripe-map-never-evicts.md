---
# modus-0152
title: PathLocks holds a stripe for every path the process has ever touched
status: todo
type: fix
priority: low
order: CK
created_at: 2026-09-05T00:00:00Z
---

# `PathLocks` holds a stripe for every path the process has ever touched

Raised by the review of `bean:0147`. `PathLocks.STRIPES` is a `ConcurrentHashMap<Path,
ReentrantReadWriteLock>` that is never pruned, so a long-lived `modus-server` accumulates one
`ReentrantReadWriteLock` per distinct document path it has ever locked, and never releases
one.

It is a memory question and not a correctness one, which is why it is `low` and not a
blocker. The bound is the store's document count — `doc:40-durability` §10 puts the
comfortable ceiling at 500,000 documents per store — and a `ReentrantReadWriteLock` plus a
map entry is on the order of a hundred bytes, so the worst case is tens of megabytes in a
process that has touched every document. Not free, not urgent.

The map is JVM-wide **deliberately**, and this bean must not undo that. It is what makes the
in-process lock orderable against a `FileLock`, which is JVM-held; `bean:0147`'s criterion 8
records the defect that scoping it per instance produced. Eviction is safe in a way that
narrowing the scope is not: a stripe dropped while nobody holds it and recreated on the next
acquisition is the same lock to every acquirer, because acquirers only ever reach it through
`computeIfAbsent` on the canonical path.

## Success criteria

| # | criterion |
|---|---|
| 1 | A stripe nobody holds becomes eligible for collection, and one somebody holds does not. The second half is the whole difficulty: a naive `WeakHashMap` or a `remove` on release drops a lock **between** one acquirer releasing and another acquiring, and two threads then hold two different locks for one path |
| 2 | The race in criterion 1 is observed, not argued: a test that acquires and releases in a loop on one path from several threads while eviction runs, asserting mutual exclusion held throughout. An eviction scheme nobody has watched fail to lose a lock is a claim (`doc:00-constitution#observed-failing`) |
| 3 | The JVM-wide scope is unchanged, and `bean:0147`'s `two DocumentStore instances over one root serialise each other` still passes |
| 4 | Growth is measured before and after on a fixture that touches many paths, rather than asserted |
| 5 | `./gradlew qualityCheck` green |

Measure before building. `doc:40-durability` §9.6's instinct applies here too: if the figure
at Modus's real document count is a few megabytes, the honest outcome is to write that down
and close this without code.
