---
# modus-0148
title: The append-only NDJSON log
status: todo
type: feature
priority: high
order: CF
created_at: 2026-09-05T00:00:00Z
parent: modus-0017
blocked_by: [modus-0147]
---

# The append-only NDJSON log

The second child of `bean:0017`: `doc:40-durability#append-only-log`, §4.2, §5 and the log
rows of §7. Bullet 2 of the parent, whole.

`blocked_by: [modus-0147]` for the store root, the lock striping and the exception types
`bean:0147` introduces, not for the rename — an append deliberately does not use one (§4.2).

## Success criteria

Integration tests only (`doc:35-testing#definitions`), every failure path observed and each
paired with the healthy case it must not break
(`doc:00-constitution#observed-failing`).

| # | criterion | evidence |
|---|---|---|
| 1 | §4.2: the descriptor is opened `O_APPEND`, the per-log appender lock is held across the whole record whatever its length, and the write loops until every byte is written. No size threshold and no size-dependent branch — §6.1 says a record's length never changes which code path runs, and §4.2 records why the previous `PIPE_BUF` rule was wrong | |
| 2 | §2.2.3–2.2.5: every record carries a monotonic per-log `seq`, an ISO-8601 UTC `at`, a `kind`, and `crc` last — the CRC-32C of the record's canonical serialisation with `crc` omitted | |
| 3 | §2.2.6: a record whose line does not parse, lacks `crc`, or whose recomputed `crc` does not match is skipped on read, counted, and the log marked `degraded`. Observed by corrupting a CRC and by truncating a record mid-write | |
| 4 | §7: a torn record is **detected anywhere in the file**, not only at the end. A fragment followed by a complete later record followed by the fragment's tail is the fixture, because it is the shape a reader that only inspects the tail passes | |
| 5 | §2.2.7: a truncated final line with no trailing newline is discarded on read and truncated away before the next append — the only permitted repair, only on the last line, logged. A truncated line that is **not** last is not repaired | |
| 6 | §7: a `seq` gap marks the log degraded and is never silently renumbered | |
| 7 | A degraded log still serves every record that is intact. Recovery-on-read accounts for the damage rather than failing the read | |
| 8 | §5: fsync policy per data class, and the one hard requirement — a `seq` is never advertised as a durable cursor before the record at that `seq` is fsynced | |
| 9 | §2.2.8: segments roll at a size threshold and are immutable once rolled | |
| 10 | `./gradlew qualityCheck` green, baseline row written by `coverageBaselineWrite` | |

## Two seams handed to this bean by `bean:0147`

**Criterion 1's short-write loop is this bean's to evidence, not `bean:0147`'s.**
`doc:40-durability` §4.2 step 3 — "LOOPING until every byte is written" — governs the append
path, which is here. `bean:0147` contains a loop of its own and correctly does not claim it:
§4 step 3 asks that class only to write all the bytes, which its 512 KiB round-trip
evidences, and removing its loop leaves its whole suite green.

Provoking a short write needs a seam, and `bean:0147` recorded two rather than asserting
none existed:

| seam | cost | also buys |
|---|---|---|
| the loop extracted into an internal helper taking a `java.nio.channels.WritableByteChannel`, driven by a short-writing fake | one production extraction; the test is a **unit** test — no filesystem | nothing else |
| a delegating `java.nio.file.spi.FileSystemProvider` whose `newFileChannel` returns a short-writing `FileChannel` | no production change at all | `bean:0147`'s `fsync` gap, below |

**And the `fsync` gap `bean:0147` could not close.** Deleting either `channel.force(true)`
from `AtomicFileWriter` leaves that bean's thirty tests green, so nothing establishes that
either force is issued; `bean:0150` owns closing it through the `SIGKILL` test. The second
seam above would let a `FileChannel` record its own `force` calls, which closes it earlier
and more cheaply. If this bean builds that seam, say so and re-point the gap.

## The trap this bean is most likely to fall into

The parent's brief names it: a corruption detector whose fixtures share one structural
assumption is blind in exactly the direction that matters. Criterion 4 exists because the
obvious fixture — append records, chop the file — only ever produces damage at the tail, and
a reader that checks only the tail passes every such fixture while failing the condition §7
actually states. At least one fixture must place a torn record with intact records on
**both** sides of it.
