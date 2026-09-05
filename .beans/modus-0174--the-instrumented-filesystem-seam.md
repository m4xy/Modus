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

## Evidence

Procedure per plant: green on unmodified source, break the named thing, run
`./gradlew :adapter-persistence-flatfile:integrationTest`, record the assertion verbatim,
revert, green again (`doc:35-testing#load-bearing-evidence`).

Two hazards were designed out of the driver rather than avoided by care, both from failures
other agents hit this sprint:

- **The tree was committed before the driver ran.** A sibling lost a refactor when
  `git checkout --` inside its own mutation loop reverted uncommitted source. The driver
  refuses to start on a dirty tree and each entry below carries its `tree clean: []`.
- **A plant that fails to apply reports a pass.** Another agent ran a whole loop against
  unmodified source because a formatter had rewrapped its plant point; every run was green
  and proved nothing. The driver is `set -eu`, aborts if the search text is absent, and
  **prints the line count and the diff of each plant before the suite is allowed to run** —
  the `planted (N+ M-)` line below is that proof.

### Criterion 4 — the gap closes

The criterion this bean exists for. Both plants were green in `bean:0147` and are red here,
against production source that this bean did not touch.

```
planted (0+ 1-):  AtomicFileWriter.kt
-            channel.force(true)          [step 4, the temp file]
observed: a write that fails before the rename forces the temp file and never the
          directory() FAILED
          org.opentest4j.AssertionFailedError: expected:<1> but was:<0>
          both forces are issued, on the temp file and then on the parent directory() FAILED
          org.opentest4j.AssertionFailedError: expected:<2> but was:<1>
          the writer opens exactly the two channels it forces() FAILED
          org.opentest4j.AssertionFailedError: expected:<2> but was:<1>
          a second write forces twice again, so the mechanism is per write and not once per
          process() FAILED
          org.opentest4j.AssertionFailedError: expected:<4> but was:<2>
reverted: tree clean: []

planted (0+ 1-):  AtomicFileWriter.kt
-            channel.force(true)          [step 7, the parent directory]
observed: both forces are issued, on the temp file and then on the parent directory() FAILED
          org.opentest4j.AssertionFailedError: expected:<2> but was:<1>
          the writer opens exactly the two channels it forces() FAILED
          org.opentest4j.AssertionFailedError: expected:<2> but was:<1>
          a second write forces twice again, so the mechanism is per write and not once per
          process() FAILED
          org.opentest4j.AssertionFailedError: expected:<4> but was:<2>
reverted: tree clean: []
```

Both plants are one deleted line. In `bean:0147` that same line came out with all 31 tests
green and JaCoCo reporting 0 missed instructions and 0 missed branches on the class.

### Criteria 1, 2 and 3 — the seam does what it claims

`InstrumentedFileSystemIntegrationTest`, six tests, all asserting on behaviour rather than on
the forwarding code:

- **Criterion 1** — a file written through a wrapped path is read back byte-identically
  through an *ordinary* path the test builds for itself. If the seam diverted anything, that
  is where it shows.
- **Criterion 2** — `AtomicFileWriter`, unmodified, opens exactly two channels through the
  provider and forces both. Nothing was recompiled against anything new: the class file is
  the one `main` carries.
- **Criterion 3** — the temp file's force and the directory's force are distinguishable by
  path, and their order is asserted: `[<something>.tmp, <the directory>]`.

### Criterion 5 — the healthy case

Every assertion above is paired with the unmutated run in which the file ends up holding the
right bytes and both forces are present. A mechanism that fires on every input is worthless
(`doc:00-constitution#observed-failing`), so `a channel opened on a wrapped path is
intercepted, and one on a plain path is not` asserts the negative half directly: an ordinary
path must produce **no** recording, and the interception count stays at 1 across a second,
uninstrumented channel open.

### Criterion 6 — the seam cannot silently do nothing

The failure mode this bean was most at risk of shipping: a wrapper that stops intercepting
looks exactly like a mechanism that is working, because both report no missing forces. Two
plants break the **seam** rather than the mechanism, and both are red.

```
planted (1+ 1-):  InstrumentedPath.kt
-    override fun getFileSystem(): FileSystem = fileSystem
+    override fun getFileSystem(): FileSystem = delegate.fileSystem
observed: 10 tests FAILED — every test in InstrumentedFileSystemIntegrationTest and every
          test in AtomicWriteDurabilityIntegrationTest
reverted: tree clean: []

planted (1+ 1-):  InstrumentedPath.kt
-    override fun resolve(other: Path): Path = wrap(delegate.resolve(unwrap(other)))
+    override fun resolve(other: Path): Path = delegate.resolve(unwrap(other))
observed: every path operation returns a path still bound to the seam() FAILED
          org.opentest4j.AssertionFailedError:
            expected:<...testfs.InstrumentedFileSystemProvider@d13379e>
            but was:<sun.nio.fs.MacOSXFileSystemProvider@7c8df667>
          and all four AtomicWriteDurabilityIntegrationTest tests FAILED
reverted: tree clean: []
```

The second is the more instructive one and it is a **single leaked return value**.
`AtomicFileWriter` reaches its temp file through `Files.createTempFile`, which ends in
`dir.resolve(name)`; with `resolve` handing back a delegate path, the temp file is opened on
the default provider, one of the two forces stops being observed, and the durability tests go
red. That is the defect this class invites, caught by the tests rather than by reading 350
lines of forwarding.

### Criterion 7 — the document

`doc:40-durability` §4's `Enforcement gap:` is replaced by an `Enforced by:` naming the
mechanism and both plants, with the production-configurability argument stated in the
document rather than only in a bean. The section's **other** half — the ArchUnit and Detekt
rules restricting who may call `java.nio.file.Files` write methods — is untouched by this
bean and stays a gap, now naming `bean:0150` rather than the epic. It is about what *other*
code may call, not about what `AtomicFileWriter` does, and conflating the two is how a
half-closed gap reads as closed.

`bean:0150`'s criterion 7 is re-pointed, with a table separating the three questions that
were being carried as one: whether a force is issued (closed here), whether a crash leaves a
recoverable store (still `bean:0150`), and whether the platform honours a force (nobody, and
no test in this repository reaches it).

### Criterion 8 — the gate, and the coverage baseline

```
cmd:      ./gradlew ktlintFormat
observed: BUILD SUCCESSFUL in 12s
cmd:      ./gradlew qualityCheck
observed: BUILD SUCCESSFUL in 2m 55s
          186 actionable tasks: 14 executed, 1 from cache, 171 up-to-date
cmd:      git status --porcelain config/coverage/baseline.tsv
observed: (no output)
```

The baseline is **unchanged**, and the criterion asked for that rather than for a rewrite: a
test-only change should move no production coverage figure, and a moved figure would have
been a finding — either the seam had reached production code, or a test had started covering
a line no test covered before, and both want explaining. Neither happened.

`coverageBaselineWrite` was therefore never run, so `bean:0033`'s provenance blocks are
intact by not being touched. That is the first change in this sequence that did not have to
restore them by hand; the count stays at ten.

## What this bean does not establish

- **That the platform honours an `fsync`.** Unchanged from `bean:0147`, and the one claim
  that stays out of reach. What moved is the level below it: the call is now known to happen.
- **The short write.** `InstrumentedFileSystems.writingAtMost` ships and is tested — a
  single unchecked `write` of 4096 bytes through a channel capped at 8 leaves a 8-byte file,
  and the same channel driven by a loop lands all 4096 — but no *production* code loops
  against it yet. `doc:40-durability` §4.2 step 3 mandates the loop on the append path, which
  does not exist until `bean:0175`. The instrument ships now because a seam with one
  instrument is a seam that gets rebuilt for the second.
- **Anything about `bean:0148`'s log format.** No production source is touched by this bean
  at all; `git diff --stat` on `src/main` is empty.
