---
# modus-0174
title: The instrumented filesystem seam, and the fsync gap it closes
status: in-progress
type: feature
priority: high
order: CFA
created_at: 2026-09-05T00:00:00Z
parent: modus-0148
---

# The instrumented filesystem seam, and the `fsync` gap it closes

The first child of `bean:0148`, and the only one whose consumer is already on `main`.

`bean:0147` merged carrying an admitted gap: **nothing establishes that either `fsync` in
`AtomicFileWriter` is issued.** Deleting `channel.force(true)` on the temp file, or on the
parent directory, leaves all 31 of its integration tests green. JaCoCo reports 0 missed
instructions and 0 missed branches on that class, because line coverage counts execution and
not consequence. `bean:0150` was given the gap on the reasoning that the
`SIGKILL`-at-randomised-points test was the only thing that could detect a missing force.

That reasoning was one clause short and `bean:0147`'s review supplied the missing clause.
The disqualifying property under `doc:00-constitution#observed-failing` is not "could a test
double be written that skips the mechanism" — true of every double in this repository, and a
property of writing the double badly. It is **whether production can be configured into a
state where the mechanism does not run.**

| seam | production configurable to skip the force? |
|---|---|
| `SyncObserver` promoted from observer to *strategy* | **yes.** The mechanism moves into the seam; a wiring supplying a no-op collaborator is a production configuration in which no force happens. Disqualified |
| a delegating `java.nio.file.spi.FileSystemProvider` whose `newFileChannel` returns a recording `FileChannel` | **no.** `AtomicFileWriter` is unchanged, gains no parameter, and still calls `force(true)` unconditionally; production always resolves the default filesystem |

The dispatch this rests on is a JDK guarantee, not a trick:
`FileChannel.open(Path, Set, FileAttribute...)` delegates to
`path.getFileSystem().provider().newFileChannel(...)`
(`java.base/java/nio/channels/FileChannel.java:300-301`, JDK 25). A test that hands
`AtomicFileWriter` a path belonging to an instrumented filesystem therefore observes the real
calls the real production code makes, with no production change of any kind.

## Scope

Owned: a test-only instrumented `FileSystemProvider`, `FileSystem` and `Path` in
`adapters/adapter-persistence-flatfile/src/integrationTest`, its own tests, and the two
`AtomicFileWriter` assertions it makes possible. `documentation/40-durability.md` §4's
`Enforcement gap:` where this closes it, and `bean:0150`'s criterion 7, which currently
assumes the gap closes no earlier than the `SIGKILL` test.

Not owned: any production source. If this bean finds it must change one, that is the signal
that the seam is the wrong shape, and it stops and reports rather than editing.

The short write is **not** in scope. The same seam provokes one, and
`doc:40-durability` §4.2 step 3 mandates the loop on the append path, which does not exist
yet — `bean:0175` uses this seam for that the moment it does.

## Success criteria

| # | criterion |
|---|---|
| 1 | A path from the instrumented filesystem reaches the real default provider for every operation, so a test using it exercises the same bytes on the same disk as a test that does not. Asserted by round-tripping a file through it, not by reading the delegation code |
| 2 | `FileChannel.open` on such a path returns the instrumented channel, and `AtomicFileWriter` — **unmodified, and not recompiled against anything new** — drives it. Asserted by observing calls it already makes |
| 3 | Every `force` is recorded with the descriptor it was applied to and the order it happened in. The temp file's force and the parent directory's force are distinguishable |
| 4 | **The gap closes.** Deleting `channel.force(true)` on the temp file fails a test; deleting the one on the parent directory fails a different test; both are observed red and reverted (`doc:00-constitution#observed-failing`). This is the criterion the bean exists for — the other three are how it is reached |
| 5 | The healthy case passes with both forces present, so the mechanism does not fire on every input |
| 6 | The seam cannot silently do nothing. A test asserts the instrumented provider was actually on the path — a recorder that observed zero calls and a mechanism that was never invoked both report "no forces missing", and `bean:0051` records that being the entire difference between an inert check and a passing one |
| 7 | `doc:40-durability` §4's `Enforcement gap:` is replaced by an `Enforced by:` naming the mechanism and the plant, or the gap is restated more narrowly with the residual named. `bean:0150`'s criterion 7 is re-pointed either way |
| 8 | `./gradlew qualityCheck` green, and `config/coverage/baseline.tsv` unchanged or moved only by `coverageBaselineWrite` — a test-only change should move no production coverage figure, and if it does, that is a finding |

## The failure mode this bean must not ship

A delegating wrapper is ~350 lines of mechanical forwarding, and the defect it invites is a
method that forwards **incorrectly** rather than one that fails to compile: an override that
unwraps the wrong argument, or returns a delegate path where it should return a wrapped one,
turns a test that appears to exercise the seam into one that quietly bypasses it. Criterion 6
exists for that, and criterion 1 asserts the round trip rather than the code.

`bean:0147`'s own lesson applies with force here: its thirteen plants were chosen by the
author of the code they tested and were blind in the direction that author was. The plants
for this bean must include at least one that breaks the **seam** rather than the mechanism —
a provider that fails to intercept must fail a test, or nothing distinguishes it from a
mechanism that is working.
