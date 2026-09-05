---
# modus-0176
title: Recovery-on-read and segment rolling
status: todo
type: feature
priority: high
order: CFC
created_at: 2026-09-05T00:00:00Z
parent: modus-0148
blocked_by: [modus-0175]
---

# Recovery-on-read and segment rolling

The read half of `bean:0148`: criteria 3 to 7 and 9 of that bean —
`doc:40-durability#append-only-log` §2.2.6–2.2.8 and the log rows of §7.

This is where `bean:0148`'s stated trap lives, which is why it is a bean of its own rather
than the back half of the appender's pull request. A corruption detector reviewed in the same
breath as the writer that produced its fixtures is reviewed by someone holding the writer's
assumptions.

## Success criteria

| # | criterion |
|---|---|
| 1 | §2.2.6: a record whose line does not parse, lacks `crc`, or whose recomputed `crc` does not match is skipped on read, counted, and the log marked `degraded`. Observed by corrupting a CRC and by truncating a record mid-write — two separate fixtures, because they fail for different reasons |
| 2 | §7: a torn record is detected **anywhere in the file**, not only at the end. The fixture is a fragment, then a complete later record, then the fragment's tail — see below |
| 3 | §2.2.7: a truncated final line with no trailing newline is discarded on read and truncated away before the next append. The **only** permitted repair, only on the last line, logged with the byte count discarded |
| 4 | A truncated line that is **not** last is not repaired. The negative half of criterion 3, and the one a repair that scans for "the first incomplete line" gets wrong |
| 5 | §7: a `seq` gap marks the log degraded and is never silently renumbered |
| 6 | A degraded log still serves every record that is intact. Recovery accounts for the damage rather than failing the read — a store that refuses to read a damaged log has lost the undamaged records too |
| 7 | §2.2.8: segments roll at a size threshold into `NNNN.ndjson`, and a rolled segment is immutable. Reading spans segments; `seq` stays monotonic across the boundary |
| 8 | `./gradlew qualityCheck` green, baseline row written by `coverageBaselineWrite`; check the provenance blocks survive (`bean:0033`) |

## The trap, restated as a constraint on the fixtures

Inherited from `bean:0148` and sharpened. The obvious fixture — append some records, chop the
file — only ever produces damage **at the tail**. A reader that inspects only the last line
passes every such fixture while failing §7, which says a torn record can sit anywhere.

So at least one fixture must place a torn record with intact records on **both** sides of it,
and the suite must contain a plant that distinguishes the two readers: a reader narrowed to
the tail must fail a test. Without that plant, criterion 2 is a claim.

The second-order version of the same trap, which `bean:0147` met in a different form: if
every fixture is built by the same corrupting helper, they share whatever that helper cannot
express. At least one fixture should be written as literal bytes rather than produced by
corrupting a well-formed log.
