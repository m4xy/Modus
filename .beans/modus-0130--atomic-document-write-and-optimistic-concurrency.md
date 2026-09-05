---
# modus-0130
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
Markdown/YAML codec and the two identity repositories are `bean:0132`, the NDJSON append path
is `bean:0131`, and the startup recovery walk is `bean:0133`.

## Where the store's public entry point lives, and why it is not a port

`doc:40-durability` §6.4 requires "the store's public port" to carry only an
expected-version-guarded mutation entry point, with no unconditional overwrite API. That
entry point is `DocumentStore`, declared **inside** `adapters/adapter-persistence-flatfile`.

It is not a hexagonal port and must not be made one. `doc:00-constitution` §1.2 puts a port
where a use case or the domain calls it; nothing outside this adapter ever calls
`write(path, expectedVersion, bytes)` — `ActorRepository` and `PermissionGrantRepository`
are the ports the outside sees, and they are implemented *on top of* `DocumentStore` by
`bean:0132`. Declaring a byte-and-path interface in `core-domain` would put `java.nio.file`
in the domain's vocabulary, which §1.3 forbids outright. "Public port" in §6.4 is naming the
store's public surface, not a layer.

## Success criteria

Each is an integration test (`doc:35-testing#definitions`) — this bean is filesystem
behaviour and has no unit-testable surface. Each negative test is run once with the
mechanism it names deliberately disabled and observed going red, and each is paired with the
healthy case that must still pass (`doc:00-constitution#observed-failing`).

| # | criterion | evidence |
|---|---|---|
| 1 | The seven steps of `doc:40-durability#atomic-write` happen, in that order: the temp file is created in the target's own directory, forced before the rename, and the **parent directory** forced after it | |
| 2 | A reader never observes a partial document: with a write in flight the target holds either the whole previous version or the whole new one, and never an empty file or a mixture | |
| 3 | A write that fails partway leaves the previous version intact and an orphan `.tmp` **beside the target, in the same directory** — the evidence `doc:40-durability` §4.1 says a crash leaves | |
| 4 | §6.4: a write conditional on a version that is no longer current is refused with `StaleWriteException` carrying the current version, and a write conditional on the current version succeeds. There is no unconditional overwrite entry point to omit the check on | |
| 5 | §6.2: the in-process lock is per canonical path, so two writers to one path serialise and two writers to different paths do not; acquisition times out rather than blocking, and the timeout is `StoreContentionException` | |
| 6 | §6.2: two locks are only ever taken through the ordered multi-lock helper, and the ordering is total, so the deadlocking interleaving of two writers taking the same pair in opposite orders cannot be constructed | |
| 7 | §6.3: the cross-process lock is held from a **second process** and this one is refused — `StoreContentionException`, not a corrupted file and not an indefinite block | |
| 8 | The in-process lock is taken before the cross-process lock, so one JVM locking a path twice waits rather than throwing `OverlappingFileLockException` — the failure `FileLock`'s JVM-wide scope produces if the two are ordered the other way | |
| 9 | A target outside the store root is refused rather than written. `ActorId`'s own KDoc authorises an adapter to use an id unencoded as a path segment (`bean:0009` review thread 6), so the store is the last thing standing between a crafted id and a write anywhere on the volume | |
| 10 | `./gradlew qualityCheck` green, and `config/coverage/baseline.tsv`'s `:adapter-persistence-flatfile` row moved by `coverageBaselineWrite` rather than by hand | |

## Not in scope, stated so a reviewer does not read the absence as an oversight

- **Breaking a stale lock whose holder PID is dead** (`doc:40-durability` §6.3, last
  sentence) is a startup-recovery action and lands with the rest of §7 in `bean:0133`. This
  bean refuses a held lock; it never breaks one.
- **Sweeping orphan `.tmp` files** (§4.1, §7 rows 1 and 2). Criterion 3 *produces* the orphan
  and asserts it is beside the target; deleting it on a schedule is `bean:0133`.
- **The ArchUnit rule restricting `java.nio.file` write methods to `AtomicFileWriter`, and
  the Detekt `ForbiddenMethodCall` entries beside it** (§4's enforcement gap). Both are
  repository-wide gates over `architecture-tests/` and `build-logic/`, and both are vacuous
  until there is a second writer in the tree to catch. `bean:0133` carries them with the rest
  of the enforcement work.
- **`fsync` making data durable across a power cut.** Criterion 1 observes the *sequence* of
  forces on the real descriptors, which is what a test can decide. That the OS then honours
  a force is the OS's guarantee and no test in this repository establishes it; the
  `SIGKILL`-at-randomised-points test `doc:40-durability` §5 asks for is `bean:0133`.
