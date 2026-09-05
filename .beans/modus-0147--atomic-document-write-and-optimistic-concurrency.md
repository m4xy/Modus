---
# modus-0147
title: Atomic document write, locking and the optimistic-concurrency check
status: in-progress
type: feature
priority: high
order: CE
created_at: 2026-09-05T00:00:00Z
parent: modus-0017
---

# Atomic document write, locking and the optimistic-concurrency check

The first child of `bean:0017`, and the one the other three stand on: every durable write in
Modus is a rename, and nothing else in the codebase is permitted to reach a target path any
other way (`doc:40-durability#atomic-write`). `adapters/adapter-persistence-flatfile` holds a
placeholder class today, so this bean is where `AtomicFileWriter` first exists.

Scope is the write **primitive** and the concurrency control around it — bytes in, bytes on
disk, rejected if someone else moved first. It does not know what a document contains: the
Markdown/YAML codec and the two identity repositories are `bean:0149`, the NDJSON append path
is `bean:0148`, and the startup recovery walk is `bean:0150`.

## Where the store's public entry point lives, and why it is not a port

`doc:40-durability` §6.4 requires "the store's public port" to carry only an
expected-version-guarded mutation entry point, with no unconditional overwrite API. That
entry point is `DocumentStore`, declared **inside** `adapters/adapter-persistence-flatfile`.

It is not a hexagonal port and must not be made one. `doc:00-constitution` §1.2 puts a port
where a use case or the domain calls it; nothing outside this adapter ever calls
`write(path, expectedVersion, bytes)` — `ActorRepository` and `PermissionGrantRepository`
are the ports the outside sees, and they are implemented *on top of* `DocumentStore` by
`bean:0149`. Declaring a byte-and-path interface in `core-domain` would put `java.nio.file`
in the domain's vocabulary, which §1.3 forbids outright. "Public port" in §6.4 is naming the
store's public surface, not a layer.

## This bean was allocated `modus-0130` and renumbered

Three agents working from `main` at `7731d13` each took the next free bean ids and all three
took `0130` onwards; this branch's `0130`-`0133` collided with two pull requests that were
further along, so this branch renumbered to `0147`-`0150`. `docs-lint` check 13 passed on
every branch throughout, because its cross-branch condition compares against `origin/main`
and every one of those ids is free there.

That is `bean:0051`'s stated residual, at a scale it did not predict, and it is recorded as
an amendment on that bean rather than restated here. Commits on this branch that predate the
rename carry no bean id in their subject, and the four source-comment citations of the
recovery bean now name `bean:0150` under its new id.

## Success criteria

Each is an integration test (`doc:35-testing#definitions`) — this bean is filesystem
behaviour and has no unit-testable surface. Each negative test is run once with the
mechanism it names deliberately disabled and observed going red, and each is paired with the
healthy case that must still pass (`doc:00-constitution#observed-failing`).

| # | criterion |
|---|---|
| 1 | The seven steps of `doc:40-durability#atomic-write` happen, in that order: the temp file is created in the target's own directory, forced before the rename, and the **parent directory** forced after it |
| 2 | A reader never observes a partial document: with a write in flight the target holds either the whole previous version or the whole new one, and never an empty file or a mixture |
| 3 | A write that fails partway leaves the previous version intact and an orphan `.tmp` **beside the target, in the same directory** — the evidence `doc:40-durability` §4.1 says a crash leaves |
| 4 | §6.4: a write conditional on a version that is no longer current is refused with `StaleWriteException` carrying the current version, and a write conditional on the current version succeeds. There is no unconditional overwrite entry point to omit the check on |
| 5 | §6.2: the in-process lock is per canonical path, so two writers to one path serialise and two writers to different paths do not; acquisition times out rather than blocking, and the timeout is `StoreContentionException` |
| 6 | §6.2: two locks are only ever taken through the ordered multi-lock helper, and the ordering is total, so the deadlocking interleaving of two writers taking the same pair in opposite orders cannot be constructed |
| 7 | §6.3: the cross-process lock is held from a **second process** and this one is refused — `StoreContentionException`, not a corrupted file and not an indefinite block |
| 8 | The in-process lock is taken before the cross-process lock, so one JVM locking a path twice waits rather than throwing `OverlappingFileLockException` — the failure `FileLock`'s JVM-wide scope produces if the two are ordered the other way |
| 9 | A target outside the store root is refused rather than written. `ActorId`'s own KDoc authorises an adapter to use an id unencoded as a path segment (`bean:0009` review thread 6), so the store is the last thing standing between a crafted id and a write anywhere on the volume |
| 10 | `./gradlew qualityCheck` green, and `config/coverage/baseline.tsv`'s `:adapter-persistence-flatfile` row moved by `coverageBaselineWrite` rather than by hand |

## Not in scope, stated so a reviewer does not read the absence as an oversight

- **Breaking a stale lock whose holder PID is dead** (`doc:40-durability` §6.3, last
  sentence) is a startup-recovery action and lands with the rest of §7 in `bean:0150`. This
  bean refuses a held lock; it never breaks one.
- **Sweeping orphan `.tmp` files** (§4.1, §7 rows 1 and 2). Criterion 3 *produces* the orphan
  and asserts it is beside the target; deleting it on a schedule is `bean:0150`.
- **The ArchUnit rule restricting `java.nio.file` write methods to `AtomicFileWriter`, and
  the Detekt `ForbiddenMethodCall` entries beside it** (§4's enforcement gap). Both are
  repository-wide gates over `architecture-tests/` and `build-logic/`, and both are vacuous
  until there is a second writer in the tree to catch. `bean:0150` carries them with the rest
  of the enforcement work.
- **`fsync` making data durable across a power cut.** Criterion 1 observes the *sequence* of
  forces on the real descriptors, which is what a test can decide. That the OS then honours
  a force is the OS's guarantee and no test in this repository establishes it; the
  `SIGKILL`-at-randomised-points test `doc:40-durability` §5 asks for is `bean:0150`.

## Evidence

Procedure, per row: green on unmodified source, break the named mechanism in `src/main`,
run `./gradlew :adapter-persistence-flatfile:integrationTest`, record the assertion
verbatim, revert, green again (`doc:35-testing#load-bearing-evidence`). The tree was clean
before **every** plant and after every revert, asserted by the driver rather than assumed
(`AGENTS.md`, and `bean:0102`); each entry below carries its `tree clean: []`.

Thirteen mutations, thirteen killed, every one by tests about the thing it broke. Two of
them were killed only after a defect in the **tests** was found by this pass and fixed —
both recorded under "What this evidence pass found in its own tests" below, because a
mutation that a test lets through is the finding, not a step on the way to one.

The suite:

```
cmd:      ./gradlew :adapter-persistence-flatfile:integrationTest
observed: BUILD SUCCESSFUL
          AtomicWriteIntegrationTest            tests="8"  failures="0" errors="0"
          CrossProcessLockIntegrationTest       tests="4"  failures="0" errors="0"
          OptimisticConcurrencyIntegrationTest  tests="10" failures="0" errors="0"
          PathLockingIntegrationTest            tests="8"  failures="0" errors="0"
```

### Criterion 1 — the seven steps, in order

Three mutations, one per clause of the criterion. `SyncObserver` records each real `fsync`
and what was on disk at that moment; it is called **after** the force, so nothing passed to
it can skip one.

```
planted:  AtomicFileWriter: step 7 deleted — no force on the parent directory after the rename
observed: forces the temp file before the rename and the parent directory after it() FAILED
          org.opentest4j.AssertionFailedError: Collection should contain exactly:
            [TEMP_FILE, PARENT_DIRECTORY] but was: [TEMP_FILE]
          a failure after the rename leaves the new version published, because the rename
          already happened() FAILED
          org.opentest4j.AssertionFailedError: Expected exception java.io.IOException but
            no exception was thrown.
reverted: tree clean: []

planted:  AtomicFileWriter: the parent directory forced BEFORE the rename instead of after
observed: forces the temp file before the rename and the parent directory after it() FAILED
          org.opentest4j.AssertionFailedError: expected:<second> but was:<first>
          a failure after the rename leaves the new version published, because the rename
          already happened() FAILED
          org.opentest4j.AssertionFailedError: expected:<second> but was:<first>
reverted: tree clean: []

planted:  AtomicFileWriter: Files.createTempFile(directory, ...) -> Files.createTempFile(...),
          putting the temp file in the system temp directory rather than the target's own
observed: forces the temp file before the rename and the parent directory after it() FAILED
          org.opentest4j.AssertionFailedError:
            expected:</var/folders/mg/c8xtgk197f74w3r78q7_9sfc0000gn/T/junit-4462396636169916229>
            but was:</var/folders/mg/c8xtgk197f74w3r78q7_9sfc0000gn/T>
          a write that fails after the temp file is synced leaves the target and an orphan
          beside it() FAILED
          org.opentest4j.AssertionFailedError: expected:<1> but was:<0>
          an in-flight write is not published until the rename, so a reader mid-write sees
          the old bytes() FAILED
          org.opentest4j.AssertionFailedError: expected:<1> but was:<0>
reverted: tree clean: []
```

The second mutation is worth reading twice. Forcing the directory a few lines earlier is a
change nothing about `fsync` can detect — the test catches it because the observer records
*what the target held* at each force, so "after the rename" is an assertion about the
directory's contents rather than about a call order the test was told.

**What this criterion does not establish**, stated because the gap is where an over-claim
would sit: that the platform honours a force. The three plants above establish the sequence
of real `fsync` calls against real descriptors, which is the part Modus controls. Durability
across a power cut is the OS's guarantee; `doc:40-durability` §5's `SIGKILL`-at-randomised-
points test is `bean:0150` and is not claimed here.

### Criterion 2 — a reader never observes a partial document

```
planted:  AtomicFileWriter: the temp file and the rename replaced by Files.write(target, bytes)
          — doc:40-durability §4.2's "a small write is atomic, so nothing can tear", which
          that section records as false
observed: a reader never observes a partial document, and observes both versions() FAILED
          org.opentest4j.AssertionFailedError:
            expected:<["<524288 x 'a'>", "<524288 x 'b'>"]>
            but was:<[<empty string>, "<524288 x 'a'>", "<524288 x 'b'>", "<8192 x 'a'>",
              "<8192 x 'b'>", "<16384 x 'b'>", "<49152 x 'a'>", "<319488 x 'a'>",
              "<327680 x 'a'>", "<65536 x 'a'>", "<344064 x 'a'>", "<90112 x 'a'>",
              "<352256 x 'a'>", "<442368 x 'b'>", "<376832 x 'a'>", "<57344 x 'b'>",
              "<327680 x 'b'>", "<139264 x 'a'>", "<466944 x 'b'>", "<147456 x 'a'>",
              ...and 7 more (set 'kotest.assertions.collection.print.size' to see more /
              less items)]>
          forces the temp file before the rename and the parent directory after it() FAILED
          org.opentest4j.AssertionFailedError: expected:<true> but was:<false>
          a write that fails after the temp file is synced leaves the target and an orphan
          beside it() FAILED
          org.opentest4j.AssertionFailedError: expected:<first> but was:<second>
          an in-flight write is not published until the rename, so a reader mid-write sees
          the old bytes() FAILED
          org.opentest4j.AssertionFailedError: expected:<1> but was:<0>
reverted: tree clean: []
```

**Transcript note:** the message above is 5,636,347 characters as emitted. Every run of 21
or more identical characters is replaced here by `<N x 'c'>`, mechanically; nothing else is
altered and no line is reordered. `<empty string>` is kotest's own rendering. The full text
is reproducible by re-planting the mutation.

The set membership is what carries the meaning: every byte sequence a reader observed is one
of the two whole versions, so a mixture, a truncation or an empty file appears as a further
member. It appears as twenty-five of them — twenty-seven members in all, of which two
are the whole versions — and the first is `<empty string>`: the reader
caught `Files.write` after it had truncated the target and before it had written a byte.
Every other extra member is a prefix: 8,192 characters, 49,152, 319,488. That is the tear
`doc:40-durability` §4.2 says a regular-file write does not protect against, observed rather
than argued.

**Non-vacuity.** The reader loops for as long as there is a writer rather than a fixed
number of times, and the test asserts it saw **both** versions. An `fsync` per write makes
the writer orders of magnitude slower than the reader, so a bounded reader finishes before
the first rename and observes only the version it started with — passing while having
watched nothing. That was the first draft, and the assertion on the set's exact membership
is what would have made it visible.

### Criterion 3 — a failed write leaves the previous version and an orphan beside it

```
planted:  AtomicFileWriter: the write wrapped in try/catch(IOException) { deleteIfExists(temp); throw }
          — "tidy up after yourself", which erases the artefact §4.1 says a crash leaves
observed: a write that fails after the temp file is synced leaves the target and an orphan
          beside it() FAILED
          org.opentest4j.AssertionFailedError: expected:<1> but was:<0>
reverted: tree clean: []
```

The healthy half is a separate test — `the healthy write leaves no orphan at all` — so a
writer that left a temp file behind on **every** write would fail rather than satisfy this
one. A mechanism that fires on every input is worthless
(`doc:00-constitution#observed-failing`), and the orphan is exactly such a mechanism if only
its presence is asserted.

The failure is injected through a `SyncObserver` that throws after the temp file's real
force — the crash window §4.1 describes, reached deterministically. The companion test takes
the other side of the same window: a failure after the rename leaves the **new** version
published, because past the rename there is no rollback and none is wanted.

### Criterion 4 — the optimistic-concurrency check

```
planted:  DocumentStore.write: the `current != expected` guard deleted, leaving the
          unconditional overwrite §6.4 says must not exist
observed: creating a document that already exists is refused, so a create cannot silently
          replace() FAILED
          org.opentest4j.AssertionFailedError: Expected exception
            uk.m4xy.modus.adapter.persistence.flatfile.StaleWriteException but no exception
            was thrown.
          a write conditional on a version someone else replaced is refused, carrying the
          current one() FAILED  — same assertion
          re-reading after a refusal and writing against the new version succeeds() FAILED
            — same assertion
          a hand edit outside Modus is seen, because the version is a hash of the bytes on
          disk() FAILED — same assertion
reverted: tree clean: []
```

Four tests, and the fourth is the one that decides the design: `doc:40-durability` §8 makes
hand-editing a file outside Modus a supported workflow, so the version has to be a content
hash of the bytes on disk and not a counter only a writer maintains. The test edits the file
with `Files.writeString` and asserts the next conditional write is refused.

The healthy half is `a write conditional on the current version succeeds` and
`re-reading after a refusal and writing against the new version succeeds`, so a store that
refused every write would fail rather than pass.

**There is no unconditional entry point to have omitted the check on.** `write` takes a
`DocumentVersion`, and `DocumentVersion.ABSENT` is how a create states what it expected to
find — asserted by `creating a document that already exists is refused`.

### Criterion 5 — the striped lock and its timeout

```
planted:  PathLocks.stripeFor: keyed on the filesystem root rather than the path, so the
          whole store is one lock
observed: two writers to different paths do not contend() FAILED
          org.opentest4j.AssertionFailedError: expected:<true> but was:<false>
          the multi-lock helper acquires in canonical order, whatever order it was given()
          FAILED — same assertion
reverted: tree clean: []

planted:  PathLocks.canonical: toAbsolutePath().normalize() -> toAbsolutePath(), so two
          spellings of one document become two locks
observed: two spellings of one path are one lock() FAILED
          org.opentest4j.AssertionFailedError: Expected exception
            uk.m4xy.modus.adapter.persistence.flatfile.StoreContentionException but no
            exception was thrown.
          the sidecar name is derived from the path, so two documents never share one lock()
          FAILED
          org.opentest4j.AssertionFailedError:
            expected:<b92b3b85353bcc0b5cf87627d1e90ad23dfd5c9163f3d00d07677c70aeca18f6.lock>
            but was:<dd0975a39661c1696bab985b7f31339beec74c9acaae6cb989d704b9e5a42cdc.lock>
          a traversal that only leaves the root once resolved is refused() FAILED
          org.opentest4j.AssertionFailedError: Expected exception
            uk.m4xy.modus.adapter.persistence.flatfile.PathOutsideStoreException but no
            exception was thrown.
reverted: tree clean: []
```

The timeout half is `two writers to one path are serialised, and the second one times out
rather than blocking`, which asserts both sides of the same lock: contention is
`StoreContentionException`, and the identical call succeeds once the holder is gone.
`a shared lock excludes a writer` asserts it is a read-write lock and not an exclusive one
wearing the name — a second reader is admitted while a writer is refused.

The second plant is also the argument for canonicalising rather than trusting the caller's
spelling, and it lands in three places at once: the stripe, the cross-process sidecar name,
and the store-root guard all resolve the path the same way, on purpose.

### Criterion 6 — the ordered multi-lock helper

```
planted:  PathLocks.exclusiveAll: `.sorted()` removed, so the pair is acquired in whatever
          order the caller named
observed: the multi-lock helper acquires in canonical order, whatever order it was given()
          FAILED
          org.opentest4j.AssertionFailedError: expected:<true> but was:<false>
            at PathLockingIntegrationTest.kt:191
            (read from build/test-results/integrationTest/*.xml — the console summary
             prints the message without the frame, and two assertions in this test emit
             the identical message)
          two callers naming the same pair in opposite orders both complete() FAILED
          org.opentest4j.AssertionFailedError: expected:<true> but was:<false>
reverted: tree clean: []
```

The line number is quoted because it is the whole point: two earlier versions of this test
were decided by a race between two lock expiries rather than by the ordering. See "What this
evidence pass found in its own tests" below.

`two callers naming the same pair in opposite orders both complete` is a **positive**
assertion and is not claimed as load-bearing: whether two unordered acquirers actually
interleave into a deadlock on a given run is a race, and it was observed both killing and
surviving the mutation across runs. The load-bearing assertion is the one at line 191, which
does not depend on an interleaving: with the ordering in place the helper is provably
blocked on the canonically first path and has not touched the second, so a third thread
takes the second immediately; with the ordering removed the helper is sitting on it and
cannot let go for a minute.

### Criterion 7 — the cross-process lock, held from a second process

```
planted:  CrossProcessLock.exclusive: the sidecar opened and never locked — the shape that
          looks like locking on disk and excludes nobody
observed: a lock held by another process is refused, and is available again once that
          process exits() FAILED
          org.opentest4j.AssertionFailedError: Expected exception
            uk.m4xy.modus.adapter.persistence.flatfile.StoreContentionException but no
            exception was thrown.
          the cross-process lock alone cannot serialise two threads of one process() FAILED
          org.opentest4j.AssertionFailedError: Expected exception
            java.nio.channels.OverlappingFileLockException but no exception was thrown.
reverted: tree clean: []
```

A **real second JVM**, not a second thread, and the criterion is not satisfiable any other
way: a `FileLock` is held by the JVM, so a second thread of this process gets
`OverlappingFileLockException` rather than contention. `LockHolderProcess` is launched with
`ProcessHandle.current().info().command()`; the handshake is the child's own pipes, so
nothing polls and nothing sleeps (`rule:archunit/nothingSleepsTheThread` binds the whole
repository, not only production code).

Its classpath is built from the `CodeSource` locations of the three classes it needs rather
than from `java.class.path`. A Gradle test worker is launched with the worker jar alone on
that property and loads the test classpath through a classloader of its own, so the obvious
property names a classpath the child cannot run from.

Two healthy halves, so the refusal is neither blanket nor permanent: a **different** document
is lockable while the second process holds this one, and the same call that was refused
succeeds once that process has exited.

### Criterion 8 — the in-process lock is taken first

```
planted:  DocumentStore.write: the two locks swapped, cross-process outside and in-process in
observed: DocumentStore serialises writers within one process, so the file lock never
          overlaps() FAILED
          org.opentest4j.AssertionFailedError: Expected null but actual was
            java.nio.channels.OverlappingFileLockException
reverted: tree clean: []
```

Eight threads, twenty-five rounds each, starting together on a `CyclicBarrier`. Losing the
optimistic race is caught by type as the designed outcome; anything else reaches the
uncaught-exception recorder, and with the order inverted what reaches it is
`OverlappingFileLockException` — the JVM refusing its own process.

That this failure is reachable at all is asserted separately and deterministically, by
`the cross-process lock alone cannot serialise two threads of one process`. Without that
test the assertion above would be "no exception was recorded", which is what a suite that
never exercised the path also reports.

### Criterion 9 — a target outside the store root

```
planted:  DocumentStore.within: the `startsWith(root)` guard deleted
observed: a traversal that only leaves the root once resolved is refused() FAILED
          org.opentest4j.AssertionFailedError: Expected exception
            uk.m4xy.modus.adapter.persistence.flatfile.PathOutsideStoreException but no
            exception was thrown.
          a target outside the store root is refused rather than written() FAILED — same
            assertion
reverted: tree clean: []
```

The traversal test is the one that matters: `root/../<sibling>/escaped.md` starts with the
root element-wise and is outside it once resolved, so a `startsWith` on the path **as given**
accepts it. That is not hypothetical — the `canonical` mutation under criterion 5 produced
exactly that behaviour, and this test is what caught it there too.

The healthy half is `a nested target inside the root is written`, so a guard that refused
everything would fail rather than pass.

### Beyond the numbered criteria — the store's answer is a copy

Not a criterion of this bean, and planted anyway because `bean:0036` and `bean:0064` record
this exact shape twice in `core-domain` and `StoredDocument` is the first place outside it
that hands out a mutable collection.

```
planted:  StoredDocument.bytes: `content.copyOf()` -> `content`
observed: the document the store hands out is a copy, so a caller cannot mutate the store's
          answer() FAILED
          org.opentest4j.AssertionFailedError: expected:<first> but was:<Xirst>
reverted: tree clean: []
```

`DefensiveCopySourceTest`'s analyser reads `core/core-domain/src/main` only — the path is
a literal in that file — so nothing mechanical would have caught this here. The test is the enforcement, and it
is named as such rather than assumed.

### Criterion 10 — the gate, and the coverage baseline

```
cmd:      ./gradlew ktlintFormat
observed: BUILD SUCCESSFUL in 383ms
cmd:      ./gradlew qualityCheck
observed: BUILD SUCCESSFUL in 1m 24s
          165 actionable tasks: 7 executed, 158 up-to-date
cmd:      ./gradlew coverageBaselineWrite
observed: :adapter-persistence-flatfile  33 0 0 0 -> 33 0 780 18
```

Written by the task, not by hand, and no `-Pcoverage.regress` was needed: the missed counts
are unchanged and only the covered counts moved. The 33 missed instructions that remain are
the pre-existing `FlatFilePersistenceAdapter` placeholder, untouched by this bean. Every
instruction and every branch of the new code is covered — asserted by reading the JaCoCo XML
per method, not inferred from the module total.

**The writer erased the regression-provenance header again**, on a run in which no figure
regressed. That is the sixth instance and it is `bean:0033`'s subject; the block is restored
by hand in the same diff and the header now records this instance beside the previous five.

## What this evidence pass found in its own tests

The brief that commissioned this bean warned that a fix ships beside a fresh instance of the
defect it fixes, and that corruption-detection fixtures are prone to sharing an assumption
that makes them blind together. Both happened here, in the tests rather than in the store,
and both were found by the mutation pass rather than by reading.

### The escape tests wrote into the shared system temp directory

`a target outside the store root is refused rather than written` took its outside path from
`root.parent`, which for a JUnit `@TempDir` is the shared system temp directory. The test
asserts that nothing was written there — so a run in which the guard is broken **does** write
there, and the file outlives the test, the JVM and the build.

It did. One planted mutation created `/var/folders/…/T/escaped.md`, and the next five
mutation runs all reported these two tests red for a reason that had nothing to do with what
they had broken:

```
observed: (during m09, m10, m11, m12, m13 — five unrelated plants)
          a target outside the store root is refused rather than written() FAILED
          org.opentest4j.AssertionFailedError: expected:<false> but was:<true>
          a traversal that only leaves the root once resolved is refused() FAILED
          org.opentest4j.AssertionFailedError: expected:<false> but was:<true>
```

Fixed with a second `@TempDir` that the test owns and JUnit tears down. **The general shape:
a test that asserts a file was NOT written is a test that writes that file whenever it
fails, so the path it names has to be one the harness deletes.** Five plants agreeing on a
failure they could not have caused is what made it visible; a single red run would have read
as a real kill.

### The ordered-acquisition test was decided by a race between two expiries

The mutation that removes `.sorted()` from `exclusiveAll` **survived** the test written to
kill it. The test held one path for ten seconds and probed the other with a ten-second lock
timeout: with the ordering removed, the holder's own await expired first, the mutated helper
completed, and the probe acquired the lock 250 ms inside its window.

```
observed: (unordered multi-lock planted, first version of the test)
          BUILD SUCCESSFUL — the whole suite passed with exclusiveAll unordered
```

The second version shortened the probe's timeout, and killed the mutation — through a
**later** assertion, while the assertion its own comment pointed at still passed. That is
arguably worse: the test was green-for-the-right-reason-by-accident, and its comment
described behaviour it was not measuring.

The third version removes the race rather than tuning it: no lock in that test may time out,
a third thread takes the second path, and a **latch** bounds the wait. The only thing that
can end the probe early is acquiring the lock. The line number is recorded under criterion 6
because "which assertion fired" was the question two versions could not answer.

**The general shape: when a concurrency test bounds two waits with one number, the verdict
comes from whichever expires first, and that is not the property under test.** Bound the
observation with a latch and let the lock timeout be a number nothing reaches.

Neither learning can be encoded into `doc:35-testing` in this pull request: that document is
**exactly at** the 500-line ceiling `documentation/README.md` sets, and
`documentation/README.md` says a file that outgrows the ceiling is two files — a split whose
section numbers may never be reallocated (`adr:0005#finalisation`), which is its own change
with its own review surface. `bean:0151` carries the encoding; this section is the record
until it lands.

## What is not evidenced, stated rather than implied

- **The short-write retry loop** (`doc:40-durability` §4.2 step 3). `FileChannel.write` may
  perform a short write, and the loop is there because no API can assert a single syscall.
  No test here provokes one: a 512 KiB write to a local file completes in one call on both
  platforms Modus runs on, and there is no seam that would make it not. Removing the loop
  would leave every test in this bean green. It is written down here rather than claimed.
- **`fsync` durability across a power cut** — criterion 1's closing paragraph.
- **Windows.** Step 7 opens the parent directory with `READ`, which Linux and macOS accept
  and Windows rejects. The failure would be loud rather than swallowed; no platform other
  than those two is claimed or tested.
