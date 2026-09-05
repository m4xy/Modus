---
# modus-0133
title: Startup recovery, derived indexes, and the write-path gates
status: todo
type: feature
priority: high
order: CH
created_at: 2026-09-05T00:00:00Z
parent: modus-0017
blocked_by: [modus-0130, modus-0131, modus-0132]
---

# Startup recovery, derived indexes, and the write-path gates

The fourth child of `bean:0017`, and the one that turns the other three from mechanisms into
a store that survives being restarted. It is last because §7's recovery table has a row for
each of them: an orphan `.tmp` is `bean:0130`'s, a torn line is `bean:0131`'s, a document
failing schema validation is `bean:0132`'s.

## Success criteria

| # | criterion | evidence |
|---|---|---|
| 1 | The §7 recovery pass exists, runs at startup, is a directory walk, and is not optional. Each of its nine rows is constructed on disk and the exact stated action asserted — the suite `doc:40-durability` §7's enforcement gap says does not exist | |
| 2 | Orphan `.tmp` older than one hour is deleted and logged at WARN with the path; younger than one hour is left and logged at DEBUG. The one-hour boundary is decided against an injected clock, never `Instant.now()` — `rule:archunit/timeIsInjectedNeverReadFromAStaticClock` binds the whole repository, not `core-domain` alone (`doc:15-repository-layout` §4.3) | |
| 3 | A document failing schema validation moves to `.modus/quarantine/<timestamp>/` and is never auto-repaired; the operator sees it | |
| 4 | An `intent` record with no `completed` is replayed idempotently and a `completed` appended — §6.5's intent-log pattern, which is what stands in for the multi-document transaction the store does not have | |
| 5 | A stale lock file whose holder PID is dead is broken with a logged warning; a lock whose holder is alive is not. `bean:0130` refuses a held lock and deliberately never breaks one | |
| 6 | §9: `.modus/index/` is derived, git-ignored, never fsynced, rebuilt when missing or stale, and updated after the durable write rather than in the same critical section. The test §9's enforcement gap names — delete the whole index directory between every integration test case and assert identical answers | |
| 7 | §5: the `SIGKILL`-at-randomised-points test, and its companion that corrupts a segment's bytes and asserts the reader skips exactly that record, marks the log `degraded`, and still serves every other record. Both named in §5's enforcement gap | |
| 8 | §4's enforcement gap closed: an ArchUnit rule restricting `java.nio.file.Files` write methods to `AtomicFileWriter`, and the Detekt `ForbiddenMethodCall` entries for `File.writeText`, `File.writeBytes` and `Files.newOutputStream` outside it. Both observed rejecting a planted violation, or demoted to a stated gap — an unfalsifiable gate is worse than an admitted one (`doc:00-constitution#observed-failing`) | |
| 9 | `doc:40-durability`'s `Enforcement gap:` lines, and `doc:00-constitution` §3's and `doc:50-memory-and-evidence`'s, are re-pointed or removed as each is actually closed — the citations `bean:0017` holds for the epic's life | |
| 10 | `./gradlew qualityCheck` green, baseline row written by `coverageBaselineWrite` | |

## The Detekt half of criterion 8 may not be satisfiable, and that is a finding

`ForbiddenMethodCall` needs type resolution, which this repository deliberately does not run
— `build-logic/src/main/kotlin/modus.kotlin-base.gradle.kts` records why, and
`config/detekt/detekt.yml` lists 65 rules `active: false` for it. `doc:15-repository-layout`
§4.3 already notes that three such bans moved into `architecture-tests` for exactly this
reason. If the Detekt entry cannot be observed rejecting a plant, it is not written down as
enforced; the ArchUnit rule carries the whole ban and §4's gap line says so.
