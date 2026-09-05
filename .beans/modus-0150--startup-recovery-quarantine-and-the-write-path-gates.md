---
# modus-0150
title: Startup recovery, derived indexes, and the write-path gates
status: todo
type: feature
priority: high
order: CH
created_at: 2026-09-05T00:00:00Z
parent: modus-0017
blocked_by: [modus-0147, modus-0176, modus-0149]
---

# Startup recovery, derived indexes, and the write-path gates

The fourth child of `bean:0017`, and the one that turns the other three from mechanisms into
a store that survives being restarted. It is last because §7's recovery table has a row for
each of them: an orphan `.tmp` is `bean:0147`'s, a torn line is `bean:0148`'s, a document
failing schema validation is `bean:0149`'s.

## The `fsync` gap this bean inherited is closed. `bean:0174` closed it.

**Criterion 7 below no longer carries the whole burden, and this section is kept rather than
deleted because the reasoning is what a reader needs.** `bean:0174` built the delegating
`java.nio.file.spi.FileSystemProvider` described below and used it to observe both `fsync`
calls being **issued**; `doc:40-durability` §4 now carries an `Enforced by:` naming that
mechanism and its two planted violations, and `AtomicFileWriter` was not modified to make it
possible.

What criterion 7 still owns is narrower and worth stating precisely, because "the gap is
closed" would otherwise read as more than it is:

| question | who answers it now |
|---|---|
| is `force(true)` called, on which descriptor, in what order? | `bean:0174`, closed |
| does a crash at a randomised point leave the store recoverable? | criterion 7, open |
| does the platform honour a force across a power cut? | nobody, and no test in this repository reaches it |

The `SIGKILL` test is therefore still required and still this bean's, but it is no longer the
*only* thing that could detect a missing `fsync` — which is what the paragraph below claimed
when it was written, and it was true then.

## Why `bean:0147` did not close it, kept because the argument generalises

`bean:0147` implements `doc:40-durability#atomic-write` and **nothing in it establishes that
either `fsync` is issued**. Deleting `channel.force(true)` on the temp file, or on the parent
directory, while leaving the observer notification in place, leaves all of its tests green —
observed twice. JaCoCo reports 100% instruction and branch coverage on that class, because
line coverage counts execution and not consequence.

Criterion 7 below is what closes it: the `SIGKILL`-at-randomised-points test
`doc:40-durability` §5 asks for is the only thing in the four-bean plan that can tell a
forced write from an unforced one, because the difference is only visible after the process
dies without flushing.

**Inherit the argument for why it was not closed earlier, so it is not rediscovered.**
`bean:0147` could have added a seam that lets a test witness `force` — a delegating
`java.nio.file.spi.FileSystemProvider` whose `newFileChannel` returns a recording
`FileChannel` is the obvious one. It did not, on this reasoning:

> The seam that would witness a force is a seam that could suppress one, and a mechanism
> whose test can turn it off is a test of the seam.

**That sentence is too strong as a general proposition, and the corrected form is the useful
one.** The disqualifying property under `doc:00-constitution#observed-failing` is not "could
a double be written that skips the mechanism" — that is true of every double in this
repository and is a property of writing the double badly, visible in its own source. It is
**whether production can be configured into a state where the mechanism does not run.**

Applied to the two seams, that gives opposite answers:

- Promoting `SyncObserver` from observer to **strategy** fails it. The writer would ask a
  collaborator to force, the mechanism moves into the seam, and a wiring supplying a no-op
  collaborator is a production configuration in which no force happens.
- A delegating `java.nio.file.spi.FileSystemProvider` **passes** it. `AtomicFileWriter` is
  unchanged, gains no parameter, and still calls `force(true)` unconditionally; production
  always resolves the default filesystem. `FileChannel.open(Path, Set, FileAttribute…)` is
  specified to dispatch through `path.getFileSystem().provider().newFileChannel(…)`, so the
  seam is a JDK guarantee rather than a trick.

So the provider seam is legitimate and would close this gap earlier than criterion 7. It was
not built in `bean:0147` because it is ~200-250 lines serving two later beans rather than the
one it would have sat in. `bean:0148` needs the same provider to provoke a short write for
`doc:40-durability` §4.2 step 3, so whichever of the two builds it first should carry both
uses — and if it is this bean, say so and re-point the gap here rather than at criterion 7.

## Success criteria

| # | criterion | evidence |
|---|---|---|
| 1 | The §7 recovery pass exists, runs at startup, is a directory walk, and is not optional. Each of its nine rows is constructed on disk and the exact stated action asserted — the suite `doc:40-durability` §7's enforcement gap says does not exist | |
| 2 | Orphan `.tmp` older than one hour is deleted and logged at WARN with the path; younger than one hour is left and logged at DEBUG. The one-hour boundary is decided against an injected clock, never `Instant.now()` — `rule:archunit/timeIsInjectedNeverReadFromAStaticClock` binds the whole repository, not `core-domain` alone (`doc:15-repository-layout` §4.3) | |
| 3 | A document failing schema validation moves to `.modus/quarantine/<timestamp>/` and is never auto-repaired; the operator sees it | |
| 4 | An `intent` record with no `completed` is replayed idempotently and a `completed` appended — §6.5's intent-log pattern, which is what stands in for the multi-document transaction the store does not have | |
| 5 | A stale lock file whose holder PID is dead is broken with a logged warning; a lock whose holder is alive is not. `bean:0147` refuses a held lock and deliberately never breaks one | |
| 6 | §9: `.modus/index/` is derived, git-ignored, never fsynced, rebuilt when missing or stale, and updated after the durable write rather than in the same critical section. The test §9's enforcement gap names — delete the whole index directory between every integration test case and assert identical answers | |
| 7 | §5: the `SIGKILL`-at-randomised-points test, and its companion that corrupts a segment's bytes and asserts the reader skips exactly that record, marks the log `degraded`, and still serves every other record. Both named in §5's enforcement gap. **Narrowed by `bean:0174`**, which closed the "is a force issued at all" half — see the section above; what remains is crash *recovery*, not force *issuance* | |
| 8 | §4's **remaining** enforcement gap closed — `bean:0174` closed the `fsync` half and left this one explicitly to this bean: an ArchUnit rule restricting `java.nio.file.Files` write methods to `AtomicFileWriter`, and the Detekt `ForbiddenMethodCall` entries for `File.writeText`, `File.writeBytes` and `Files.newOutputStream` outside it. Both observed rejecting a planted violation, or demoted to a stated gap — an unfalsifiable gate is worse than an admitted one (`doc:00-constitution#observed-failing`) | |
| 9 | `doc:40-durability`'s `Enforcement gap:` lines, and `doc:00-constitution` §3's and `doc:50-memory-and-evidence`'s, are re-pointed or removed as each is actually closed — the citations `bean:0017` holds for the epic's life | |
| 10 | `./gradlew qualityCheck` green, baseline row written by `coverageBaselineWrite` | |

## The Detekt half of criterion 8 may not be satisfiable, and that is a finding

`ForbiddenMethodCall` needs type resolution, which this repository deliberately does not run
— `build-logic/src/main/kotlin/modus.kotlin-base.gradle.kts` records why, and
`config/detekt/detekt.yml` lists 65 rules `active: false` for it. `doc:15-repository-layout`
§4.3 already notes that three such bans moved into `architecture-tests` for exactly this
reason. If the Detekt entry cannot be observed rejecting a plant, it is not written down as
enforced; the ArchUnit rule carries the whole ban and §4's gap line says so.
