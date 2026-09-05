---
# modus-0175
title: The NDJSON record format and the appender
status: todo
type: feature
priority: high
order: CFB
created_at: 2026-09-05T00:00:00Z
parent: modus-0148
blocked_by: [modus-0174]
---

# The NDJSON record format and the appender

The write half of `bean:0148`: criteria 1, 2 and 8 of that bean, which are
`doc:40-durability#append-only-log` §2.2.3–2.2.5, §4.2 and §5.

`blocked_by: [modus-0174]` for the instrumented filesystem seam, which is the only way to
provoke the short write §4.2 step 3 mandates. `bean:0147` disclosed that its own loop was
untested and that the rule it cited governs **this** path; that disclosure becomes a
criterion here.

## Success criteria

| # | criterion |
|---|---|
| 1 | §4.2: the descriptor is opened `O_APPEND`, the per-log appender lock is held across the whole record whatever its length, and the write loops until every byte is written |
| 2 | **The loop is observed, not asserted.** With `bean:0174`'s seam returning a channel that writes fewer bytes than asked, a record still lands whole; with the loop deleted, it does not. This is the mutation `bean:0147` could not plant, and it is the reason that bean is blocked_by this one's dependency rather than the other way round |
| 3 | No size threshold and no size-dependent branch — §6.1 says a record's length never changes which code path runs, and §4.2 records why the previous `PIPE_BUF` rule was wrong. A test drives a record either side of any plausible boundary and asserts the same path runs |
| 4 | §2.2.3–2.2.5: every record carries a monotonic per-log `seq`, an ISO-8601 UTC `at`, a `kind`, and `crc` **last** — the CRC-32C of the record's canonical serialisation with the `crc` field omitted |
| 5 | The serialisation is canonical and deterministic, because §2.2.5's checksum is only reproducible on read if it is (§8). Stable key order, no pretty-printing, embedded newlines escaped |
| 6 | Time and `seq` arrive from outside: `rule:archunit/timeIsInjectedNeverReadFromAStaticClock` binds the whole repository, so the `at` field comes from an injected clock and never `Instant.now()` |
| 7 | §5: the fsync policy is per data class — always per record for documents, domain events and cost events; grouped at most every 200 ms for run output — and the policy is data, not a branch each caller re-decides |
| 8 | §5's one hard requirement: **a `seq` is never advertised as a durable cursor before the record at that `seq` is fsynced.** Observed: the advertised cursor lags the appended `seq` under the grouped policy, and does not under the per-record one |
| 9 | `./gradlew qualityCheck` green, baseline row written by `coverageBaselineWrite` — which destroys the provenance blocks in `config/coverage/baseline.tsv` on any write (`bean:0033`, ten restorations so far). Check they survive |

## Not in scope

Reading. Every criterion here is about what reaches the disk; `bean:0176` owns recovery,
torn-record detection, `degraded`, and rolling. A test here may read back what it wrote to
assert the bytes, and must not build a reader to do it — a private read helper in this bean
and a real reader in the next is two parsers that will disagree.
