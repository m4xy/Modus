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
  **prints the numstat and the diff of each plant before the suite is allowed to run** — the
  `planted (N+ M-)` line below is that proof.

  **The driver is `tools/plant.sh`, committed by this bean.** Review observed that its
  claimed properties were unverifiable from the artefact — it was not committed, and nothing
  in the pull request could be checked against — while what actually carried the weight was
  the per-plant numstat, which an independent re-plant reproduced exactly for all four
  original plants. Both halves of that are now true: the driver is readable, and the numstat
  stays the evidence rather than the prose about the driver.

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
looks exactly like a mechanism that is working, because both report no missing forces.

**The first two plants written for this criterion were the wrong plants, and review caught
it.** They broke the seam *loudly* — the suite died on exceptions rather than on the
assertion whose name describes the property — while the failure this criterion exists to
guard against is the **silent** one. That is a fresh instance of the blind-plant defect
inside the pull request that fixes blind plants: the aim was right and the choice of plant
was not. Both are re-run below with their real messages, and two plants that fail on the
named assertion are added.

**The load-bearing pair.** Each disables the interception while leaving everything
compiling and every path looking correct, which is precisely the shape that would otherwise
go unnoticed:

```
planted (1+ 1-)  testfs/InstrumentedFileSystems.kt
-    fun pathTo(path: Path): Path = provider.fileSystem().wrap(InstrumentedPath.unwrap(path))
+    fun pathTo(path: Path): Path = InstrumentedPath.unwrap(path)
observed: 8 FAILED, every one an assertion:
          the writer opens exactly the two channels it forces() FAILED
          org.opentest4j.AssertionFailedError: expected:<2> but was:<0>
          both forces are issued, on the temp file and then on the parent directory() FAILED
          org.opentest4j.AssertionFailedError: expected:<2> but was:<0>
          a second write forces twice again, so the mechanism is per write and not once per
          process() FAILED
          org.opentest4j.AssertionFailedError: expected:<4> but was:<0>
          a write that fails before the rename forces the temp file and never the directory()
          FAILED
          org.opentest4j.AssertionFailedError: expected:<1> but was:<0>
          the seam records opens and forces as separate kinds, in order() FAILED
          org.opentest4j.AssertionFailedError: Collection should contain exactly:
            [OPEN_FILE_CHANNEL, FORCE] but was: []
          a channel opened on a wrapped path is intercepted, and one on a plain path is not()
          FAILED — expected:<1> but was:<0>
          a force that throws is not recorded as having happened() FAILED
            — expected:<1> but was:<0>
          a short-writing channel consumes only what it was allowed, and the caller must
          loop() FAILED — expected:<8L> but was:<4096L>
reverted: tree clean: []

planted (2+ 6-)  testfs/InstrumentedFileSystemProvider.kt
-        return InstrumentedFileChannel(delegate = delegate.newFileChannel(...), …)
+        return delegate.newFileChannel(real, options, *attrs)
observed: 7 FAILED, every one an assertion:
          the writer opens exactly the two channels it forces() FAILED
          org.opentest4j.AssertionFailedError: expected:<2> but was:<0>
          both forces are issued, on the temp file and then on the parent directory() FAILED
          org.opentest4j.AssertionFailedError: expected:<2> but was:<0>
          a second write forces twice again … FAILED — expected:<4> but was:<0>
          a write that fails before the rename … FAILED — expected:<1> but was:<0>
          a channel opened on a wrapped path is intercepted, and one on a plain path is not()
          FAILED
          org.opentest4j.AssertionFailedError: Collection should contain exactly:
            ["watched.md"] but was: []
          the seam records opens and forces as separate kinds, in order() FAILED
          org.opentest4j.AssertionFailedError: Collection should contain exactly:
            [OPEN_FILE_CHANNEL, FORCE] but was: [OPEN_FILE_CHANNEL]
          a short-writing channel … FAILED — expected:<8L> but was:<4096L>
reverted: tree clean: []
```

`expected:<2> but was:<0>` on `interceptedChannelOpens` is the assertion that matters. It is
the vacuity figure, and these two plants are what establish it is live rather than
decorative — a recorder that observed nothing and a mechanism with nothing to observe both
report an empty `forcedPaths`, and `bean:0051` records that being the entire difference
between an inert check and a passing one.

**The two loud plants, kept and correctly described.** They establish that the wrapping is
load-bearing at all; they do **not** establish what the paragraph beside them originally
claimed.

```
planted (1+ 1-)  testfs/InstrumentedPath.kt
-    override fun getFileSystem(): FileSystem = fileSystem
+    override fun getFileSystem(): FileSystem = delegate.fileSystem
observed: 10 FAILED, all with
          java.nio.file.ProviderMismatchException
          (no assertion is reached: `Files` refuses the path before any test asserts)
reverted: tree clean: []

planted (1+ 1-)  testfs/InstrumentedPath.kt
-    override fun resolve(other: Path): Path = wrap(delegate.resolve(unwrap(other)))
+    override fun resolve(other: Path): Path = delegate.resolve(unwrap(other))
observed: 6 FAILED — four of them
          java.nio.file.AtomicMoveNotSupportedException: Atomic move between providers is
            not supported
          and two assertions:
          org.opentest4j.AssertionFailedError:
            expected:<...testfs.InstrumentedFileSystemProvider@7c8df667>
            but was:<sun.nio.fs.MacOSXFileSystemProvider@…>
reverted: tree clean: []
```

**Corrected claim.** An earlier version of this section said of the `resolve` plant that
"one of the two forces stops being observed, and the durability tests go red". That is not
what happens. `Files.move` throws `AtomicMoveNotSupportedException` because the temp file and
the target end up on different providers, so the write dies **before any force count is ever
compared**. The four durability failures are that exception, not an assertion. The one thing
the plant does establish is the two assertion failures, in the wrapping test.

### Criterion 6, continued — the seven return values no test constrained

Review found the harder half of the same defect and it was **silent**: leaking
`InstrumentedPath.getName`, `subpath`, `relativize` and `toRealPath`, plus
`Provider.readSymbolicLink`, `Provider.getPath(URI)` and `FileSystem.getRootDirectories`,
**all seven at once**, gave 41 tests and 0 failures. The class KDoc's claim that "every
operation that returns a path returns a wrapped one" was true by inspection only, and
unguarded against the next edit — which is the state this whole bean exists to refuse.

`every path-returning method on the wrapper is wrapped, including the ones no other test
drives` now constrains all seven:

```
planted (7+ 7-)  across InstrumentedPath.kt, InstrumentedFileSystemProvider.kt and
                 InstrumentedFileSystem.kt — three files, so this one plant was applied by
                 hand rather than through tools/plant.sh, whose contract is one file
observed: every path-returning method on the wrapper is wrapped, including the ones no other
          test drives() FAILED
          org.opentest4j.AssertionFailedError:
            expected:<...testfs.InstrumentedFileSystemProvider@acd3460>
            but was:<sun.nio.fs.MacOSXFileSystemProvider@3ea9…>
reverted: tree clean: []
```

The KDoc's claim is now enforced rather than asserted.

### Criterion 8 — the gate, and the coverage baseline

```
cmd:      ./gradlew ktlintFormat
observed: BUILD SUCCESSFUL in 1s
cmd:      ./gradlew qualityCheck
observed: BUILD SUCCESSFUL in 1m 49s
          186 actionable tasks: 17 executed, 7 from cache, 162 up-to-date
cmd:      git status --porcelain config/coverage/baseline.tsv
observed: (no output)
cmd:      integration test counts, from build/test-results/integrationTest/*.xml
observed: 42 tests, 0 failures, 0 errors — AtomicWrite 8, AtomicWriteDurability 4,
          CrossProcessLock 5, InstrumentedFileSystem 7, OptimisticConcurrency 10,
          PathLocking 8
```

The baseline is **unchanged**, and the criterion asked for that rather than for a rewrite: a
test-only change should move no production coverage figure, and a moved figure would have
been a finding — either the seam had reached production code, or a test had started covering
a line no test covered before, and both want explaining. Neither happened.

`coverageBaselineWrite` was therefore never run, so `bean:0033`'s provenance blocks are
intact by not being touched — four `REGRESSION` blocks on this base, and the restoration
tally in `config/coverage/baseline.tsv` reads **sixteen** after `bean:0152` added six. This
is the first change in that sequence that did not have to restore anything by hand, and the
count is unchanged by it.

*(An earlier version of this sentence said "the count stays at ten". That was true at the
head it was written against and false after the rebase onto `60132a8`, which is the
live-figure-versus-stamped-capture distinction applied to a number this bean states about
another file rather than about itself.)*

## What this bean does not establish

- **That the platform honours an `fsync`.** Unchanged from `bean:0147`, and the one claim
  that stays out of reach. What moved is the level below it: the call is now known to happen.
- **The short write.** `InstrumentedFileSystems.writingAtMost` ships and is tested — a
  single unchecked `write` of 4096 bytes through a channel capped at 8 leaves a 8-byte file,
  and the same channel driven by a loop lands all 4096 — but no *production* code loops
  against it yet. `doc:40-durability` §4.2 step 3 mandates the loop on the append path, which
  does not exist until `bean:0175`. The instrument ships now because a seam with one
  instrument is a seam that gets rebuilt for the second.
- **Anything about `bean:0148`'s log format.** No *executable* production change: the only
  edit under `src/main` is KDoc in `AtomicFileWriter.kt`, correcting two passages this bean
  makes false.

  The precise claim, measured rather than asserted, because "byte-identical" would have been
  wrong: `javap -p -c` on `AtomicFileWriter.class` is **identical across the edit** — 219
  lines of disassembly, no diff — while the `.class` file itself is **not**, because a KDoc
  edit that changes line count shifts the `LineNumberTable`. Instructions unchanged, debug
  metadata moved.

```
cmd:      javap -p -c …/AtomicFileWriter.class   # before and after the KDoc edit
observed: IDENTICAL instructions (219 lines of disassembly)
```

  The passages corrected: `AtomicFileWriter`'s class KDoc said "Neither `force` is covered by
  a test … both `channel.force(true)` calls can be deleted with the suite staying green" and
  `SyncObserver`'s said the `SIGKILL` test was "the only thing in the plan that could detect
  a missing `fsync`". This bean makes both false, and `doc:40-durability` §4 and `bean:0150`
  already say so. Leaving them would be the half-closed-reads-as-closed failure this bean was
  careful about elsewhere, inverted — a *closed* gap still telling its reader it is open.
