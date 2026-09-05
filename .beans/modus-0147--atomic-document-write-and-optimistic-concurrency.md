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
| 1 | The steps of `doc:40-durability#atomic-write` happen in the order §4 gives, observed at the two points where a descriptor is forced: the temp file is created in the target's own directory, the first observation falls **before** the rename and the second **after** it, and the second is against the target's parent directory. Whether either `fsync` is issued is **not** established — see the gap under this criterion's evidence |
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

Thirteen mutations at first submission, thirteen killed, every one by tests about the thing
it broke. Two of them were killed only after a defect in the **tests** was found by this pass
and fixed — both recorded under "What this evidence pass found in its own tests" below,
because a mutation that a test lets through is the finding, not a step on the way to one.

Review added three more, and **all three of those found something these thirteen had not**,
which is the honest summary of this bean's evidence and is recorded here rather than buried
in the criterion it belongs to. Two deleted an `fsync` and the suite stayed green; one
restored per-instance lock stripes and the suite stayed green.

The tally, by outcome rather than by round, because the previous version of this paragraph
counted the stripes plant in both columns — calling it a survivor in one sentence and
counting it among the killed in the next:

| | plants | outcome |
|---|---|---|
| first submission | 13 | all killed |
| review, `fsync` on the temp file and on the parent directory | 2 | **both survive** — criterion 1, demoted to an admitted gap owned by `bean:0150` |
| review, per-instance lock stripes | 1 | killed, by a test written in response — criterion 8 |
| **total** | **16** | **14 killed, 2 surviving under one stated gap** |

Sixteen is also the number of `planted:` blocks in this file, which is the only reason those
figures can be checked at all; `bean:0173` raises the check that would compare them
mechanically instead of by eye.

The thirteen were chosen by the author of the code they were testing, and they were blind in
exactly the direction that author was.

The suite:

```
cmd:      ./gradlew :adapter-persistence-flatfile:integrationTest
observed: BUILD SUCCESSFUL
          AtomicWriteIntegrationTest            tests="8"  failures="0" errors="0"
          CrossProcessLockIntegrationTest       tests="5"  failures="0" errors="0"
          OptimisticConcurrencyIntegrationTest  tests="10" failures="0" errors="0"
          PathLockingIntegrationTest            tests="8"  failures="0" errors="0"
```

31 tests. The fifth in `CrossProcessLockIntegrationTest` arrived with the review fix under
criterion 8; the counts elsewhere in this bean that say "thirty" describe the suite as first
submitted and are left as they stood.

### Criterion 1 — the steps, in order, at the two observed points

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

**Enforcement gap: no test establishes that either `fsync` is issued.** Found in review, by
two plants these thirteen did not contain, and reproduced here:

```
planted:  AtomicFileWriter: `channel.force(true)` on the temp file deleted, the
          observer.forced(TEMP_FILE, ...) notification kept
observed: BUILD SUCCESSFUL in 10s     — 30 of 30 green
reverted: tree clean: []

planted:  AtomicFileWriter: `channel.force(true)` on the parent directory deleted, the
          observer.forced(PARENT_DIRECTORY, ...) notification kept
observed: BUILD SUCCESSFUL in 9s      — 30 of 30 green
reverted: tree clean: []
```

The original wording of this passage conceded only that a test cannot show the *platform
honours* a force. The gap is one level up: nothing here shows a force is **called**. The
notification sits beside the force rather than downstream of it, so the three plants at the
top of this criterion establish the ordering of the two **observed steps** relative to the
rename — which the directory-content assertions make real, and which is worth having — and
say nothing about the syscall inside a step.

Two things follow, and both are recorded rather than argued away:

- **JaCoCo reports 100% on this class.** 0 missed instructions, 0 missed branches, on a file
  containing two lines whose deletion nothing notices. Line coverage counts execution, not
  consequence, and this is the cleanest example of that distinction in the repository so far.
- **The `Enforced by:` claim is demoted to an admitted gap**, which is what
  `doc:00-constitution#observed-failing` requires of a mechanism that cannot be made to fail:
  "an unfalsifiable gate is worse than an admitted gap, because it also stops anyone
  looking." `bean:0150` owns it. The `SIGKILL`-at-randomised-points test `doc:40-durability`
  §5 asks for is the only thing in the four-bean plan that could detect a missing `fsync`,
  and `bean:0150` already carries it as criterion 7.

**Not fixed here, deliberately — and the reason needs one clause more than it first
carried.** The disqualifying property under `doc:00-constitution#observed-failing` is not
"could a test double be written that skips the mechanism", which is true of every double in
the repository and is a property of writing the double badly. It is **whether production can
be configured into a state where the mechanism does not run.**

That distinction decides the two seams differently, which is why it is worth stating:

| seam | production configurable to skip the force? |
|---|---|
| `SyncObserver` promoted from observer to *strategy* — the writer asks a collaborator to force | **yes.** The mechanism moves into the seam, and a wiring that supplies a no-op collaborator is a production configuration in which no force happens. Disqualified |
| a delegating `java.nio.file.spi.FileSystemProvider` whose `newFileChannel` returns a recording `FileChannel` | **no.** `AtomicFileWriter` is unchanged, gains no parameter, and still calls `force(true)` unconditionally; production always resolves the default filesystem. `FileChannel.open(Path, Set, FileAttribute…)` is specified to dispatch through `path.getFileSystem().provider().newFileChannel(…)`, so the seam is a JDK guarantee rather than a trick |

So the second seam is legitimate and would close criterion 1's gap. It is not built here
because it is ~200-250 lines serving two *future* beans — `bean:0148` needs the same
provider to provoke a short write — and forcing it into this pull request would put the
larger half of the work in the bean that needs it least. `bean:0150` carries the gap and
inherits this argument; `bean:0148` carries the seam.

The platform-honours question stands as it always did: durability across a power cut is the
OS's guarantee and no test on this side of the syscall reaches it.

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

**This closes `doc:40-durability` §6.4's `Enforcement gap:`, in this pull request.** That
line read "the store's public port does not exist yet, so there is no `write(path, bytes)`
method to have omitted"; the method now exists, is the store's whole mutation surface, and
has been observed refusing. It is replaced by an `Enforced by:` naming `DocumentStore.write`
and the plant above. Under the epic's own policy a child re-points a gap at the moment it can
show it closed, and leaving it standing would guarantee the next agent re-litigates whether
the entry point should have been a port — which it already did twice.

§6.4's remaining sentence — "the caller re-reads, re-applies, retries. Retries are bounded
(3)" — is **caller-side and unimplemented**, so it becomes a gap of its own rather than
disappearing with the one above. `DocumentStore` refuses and hands back the current version;
nothing yet counts attempts. `bean:0149` owns it as the first caller that needs to, and the
bound of 3 is a constant that must match the document (`bean:0090`).

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

**The criterion was false as shipped, and the test above could not see it.** Found in
review. `PathLocks` held its stripes per **instance**, so two `DocumentStore`s over one root
were two disjoint sets of locks: neither writer excluded the other, both reached `tryLock` on
one sidecar, and the JVM refused its own process — the exact exception this criterion records
as impossible. The test above shares one store between its eight threads and so never
constructs the case.

It was reachable by accident rather than by contrivance. `OptimisticConcurrencyIntegrationTest`
builds a fresh `DocumentStore` on every property access, and `bean:0149` puts two repositories
on this class.

Fixed by making the stripe map JVM-wide, which is a correctness requirement and not a cache:
the in-process lock exists to reduce the process to one contender before the operating system
is asked for a `FileLock`, a `FileLock` is held by the **JVM**, and two locks can only be
ordered if they have the same scope. The rejected alternative was a mandatory constructor
argument, which moves the hazard into the wiring rather than removing it — every call site
would have to remember to share an instance, and `doc:00-constitution` §9 says a rule anyone
has to remember is eventually broken.

A second test now constructs the case, and kills the shipped defect:

```
planted:  PathLocks: the JVM-wide STRIPES map replaced by a per-instance one — the shipped
          behaviour, restored
observed: two DocumentStore instances over one root serialise each other() FAILED
          org.opentest4j.AssertionFailedError: Expected null but actual was
            java.nio.channels.OverlappingFileLockException
reverted: tree clean: []
```

Note which test did **not** fail on that plant: `DocumentStore serialises writers within one
process`, the one that had been standing for this criterion. One shared store and eight
threads passes with either scoping, and the difference between the two tests is three lines.

**That test is not vacuous, and the precise fault is worth naming rather than dismissing.**
It kills the lock-order swap, which is a real defect and the one this criterion was written
for. What it has is an **unvaried fixture dimension**: it fixes the store-instance count at
one, and the defect lived in the dimension it never varied
(`doc:35-testing#fixture-variation`, which `bean:0009` review thread 1 established for
capability-set size and which generalises past collections). The other criteria here were
swept on that reading and none rests on an unvaried dimension of comparable consequence.

A consequence of the fix, checked rather than assumed: JVM-wide stripes could couple tests
that share a path. No JUnit parallel execution is configured in this repository, and every
test in this suite uses its own `@TempDir`, so there is no coupling to have.

`CrossProcessLock`'s KDoc said that exception "means the ordering was inverted". It has three
causes and named one; all three are now named there.

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
observed: BUILD SUCCESSFUL in 1s
cmd:      ./gradlew qualityCheck
observed: BUILD SUCCESSFUL in 2m 34s
          165 actionable tasks: 15 executed, 150 up-to-date
cmd:      ./gradlew coverageBaselineWrite
observed: :adapter-persistence-flatfile  33 0 0 0 -> 33 0 780 18

          then, after the review fix moved PathLocks' stripe map to the companion:

cmd:      ./gradlew coverageBaselineWrite
observed: :adapter-persistence-flatfile  33 0 780 18 -> 33 0 778 18  <-- REGRESSION
          > coverageBaselineWrite refuses to record worse coverage:
            :adapter-persistence-flatfile (covered instructions 780 -> 778). Restore the
            coverage, or re-run with -Pcoverage.regress=<reason>
          BUILD FAILED

cmd:      ./gradlew coverageBaselineWrite -Pcoverage.regress="PathLocks moved its stripe map
            from an instance field to the companion object ..."
observed: :adapter-persistence-flatfile  33 0 780 18 -> 33 0 778 18  <-- REGRESSION
          BUILD SUCCESSFUL
```

Written by the task, not by hand, both times. The two instructions are the per-instance field
initialiser leaving the source when the stripe map became JVM-wide; **no instruction became
uncovered** — missed instructions and missed branches stay at 0 — which is why the reason is
recorded rather than the change reverted. The 33 missed instructions that remain are the
pre-existing `FlatFilePersistenceAdapter` placeholder, untouched by this bean. Every
instruction and every branch of the new code is covered, asserted by reading the JaCoCo XML
per method rather than inferred from the module total.

**Re-derived at the rebased head.** Everything above this paragraph is a capture, stamped at
the head it was taken at, and it stays as stamped. The figures below are the live ones, taken
after rebasing onto `99212fc` (PR #83), and they are what the merge commit carries:

```
cmd:      ./gradlew ktlintFormat
observed: BUILD SUCCESSFUL in 2s
cmd:      ./gradlew qualityCheck
observed: BUILD SUCCESSFUL in 2m 5s
          195 actionable tasks: 23 executed, 20 from cache, 152 up-to-date
cmd:      ./gradlew coverageBaselineWrite
observed: :adapter-persistence-flatfile  33 0 0 0 -> 33 0 778 18
          :core-application              6 0 160 6  (unchanged)
          :core-domain                   0 0 1573 130  (unchanged)
          BUILD SUCCESSFUL
cmd:      integration test counts, from build/test-results/integrationTest/*.xml
observed: AtomicWriteIntegrationTest            tests="8"  failures="0" errors="0"
          CrossProcessLockIntegrationTest       tests="5"  failures="0" errors="0"
          OptimisticConcurrencyIntegrationTest  tests="10" failures="0" errors="0"
          PathLockingIntegrationTest            tests="8"  failures="0" errors="0"
```

**No `-Pcoverage.regress` is needed on this base, and that changes what the branch ships.**
PR #83 reset `:adapter-persistence-flatfile` to `33 0 0 0`, so the row moves `0 -> 778`
directly and the 780-to-778 step recorded above does not exist in the diff against `main`.
The `REGRESSION` block that step produced is therefore **not** in the shipped baseline: a
provenance line for a regression that no longer appears in the history would be false
provenance. The observation of the writer's behaviour is unaffected and stays — it happened,
it is captured verbatim, and it is written up in `bean:0033` as observation eight.

**A ninth instance, at the rebase itself.** The re-derivation above moved exactly one row,
upward, and destroyed all three `REGRESSION` blocks then on `main` plus the note recording
that this keeps happening. Restored by hand; that is `bean:0066`'s upward-only shape
confirmed independently, and it is why the tally in `bean:0033` now reads ten.

**`bean:0033`'s defect fired twice on this one branch, and the second form is worse than the
first.** The first write did **not** regress — it only raised covered counts — and the
provenance header went anyway, which is `bean:0065`'s instance exactly. The second write
*did* regress, and while composing its own `REGRESSION` block it erased the **two older
ones**: so recording a regression costs you every regression recorded before it, and the
worse the change the more history the writer destroys. Restored by hand both times, in the
same diff; the header now records both instances beside the previous five.

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

- **That either `fsync` is issued** — criterion 1's `Enforcement gap:`, which is the
  substantive one. Owned by `bean:0150`.
- **`fsync` durability across a power cut** — the same criterion's closing paragraph, and a
  weaker claim than the line above it.
- **The short-write retry loop.** Removing the loop leaves every test in this bean green,
  confirmed. Two corrections to how this entry read before review:

  It cited `doc:40-durability` §4.2 step 3, which is the **append** path's rule and belongs
  to `bean:0148`. What §4 step 3 requires of *this* class is "write all bytes to the temp
  file", and the 512 KiB round-trip under criterion 2 evidences exactly that. So the loop is
  correctly not load-bearing here, and the mandate it satisfies is elsewhere.

  It then said "there is no seam that would make it not", which is wrong. The honest form is
  **no seam this bean was willing to build**, and there are two:

  | seam | cost |
  |---|---|
  | extract the loop into an internal helper taking a `java.nio.channels.WritableByteChannel`, and drive it with a short-writing fake | one production extraction, and the test is a **unit** test — no filesystem, no `@TempDir`, so it also stops being the slowest way to assert this |
  | a delegating `java.nio.file.spi.FileSystemProvider` whose `newFileChannel` returns a `FileChannel` that short-writes | no production change at all, and it is the same seam that would let a test witness an `fsync` |

  Neither is built here because the loop this bean contains is not the loop the rule is
  about. `bean:0148` mandates it, `bean:0148` should build the seam, and the second row buys
  criterion 1's gap at the same time.
- **Windows.** Step 7 opens the parent directory with `READ`, which Linux and macOS accept
  and Windows rejects. The failure would be loud rather than swallowed; no platform other
  than those two is claimed or tested.
