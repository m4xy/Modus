---
id: doc:40-durability
title: Durability model
status: active
superseded_by: null
read_when:
  - path: adapters/adapter-persistence-flatfile/**
  - path: "**/*.ndjson"
  - task: on-disk format|atomic write|fsync|append-only|file lock
provides:
  - doc:40-durability#atomic-write
  - doc:40-durability#append-only-log
depends_on: [doc:00-constitution]
---

# 40 — Durability

The flat-file durability model. Read this before touching persistence, file IO, or
locking.

**Rule 2 of the constitution:** Modus stores its durable state as files on disk. There is
no database. This document explains how that is made *safe*, which is the only thing that
makes it defensible.

---

## 1. Why files, not a database

Full rationale and alternatives: `adr/0002-flat-file-over-database.md`. The short form:

| Property | What files buy us |
|---|---|
| **The store is the human surface** | A work item is a Markdown file. `git log`, `rg`, `less`, an editor, a diff view, and a code review all work on the live data with no export step. |
| **The store is the agent surface** | An agent reads and writes the same bytes with `cat` and `rg`. No query language to learn, no schema to fetch, no connection to manage, no ORM impedance. Cheaper in tokens and in failure modes. |
| **Versioning is free** | git gives point-in-time history, blame, branching, and merge review of *data*, not just code. A work-item change can be reviewed in a pull request. |
| **Backup and restore are `cp -r`** | Disaster recovery is a directory copy. Migration between machines is `rsync`. |
| **No operational surface** | No server to run, patch, size, or secure. `app/modus-server` plus a directory is the entire deployment. |
| **Correctness is inspectable** | A durability bug leaves evidence on disk you can read. A corrupted B-tree does not. |

What we give up, honestly: ad-hoc query, referential integrity enforced by the store,
multi-writer scale, and transactions across many records. Sections 5–8 describe the
mechanisms that make those losses survivable at Modus's scale. If Modus ever needs
genuine multi-node write concurrency, that is an ADR superseding `0002`, not a patch.

---

## 2. The two storage shapes

Everything durable is one of exactly two shapes. If your data does not fit either, you
are modelling it wrong.

### 2.1 Documents — Markdown with YAML frontmatter

For entities a human will read or edit: work items, memories, domain definitions,
process definitions, skill definitions, decisions.

```
---
id: "0042"
title: Add cost projection to the run detail view
status: in-progress
---

Body in Markdown. Cross-references to other documents are ordinary
Markdown links: [0038](0038-run-detail-view.md).
```

Rules:

| # | Rule |
|---|---|
| 2.1.1 | The file **is** the record. It is not a cache, an export, or a render of something else. |
| 2.1.2 | Frontmatter holds machine-readable fields. The body holds prose. Never duplicate a field between the two. |
| 2.1.3 | Cross-references are relative Markdown links, so they resolve in an editor, on GitHub, and in the backoffice with no rewriting. |
| 2.1.4 | Filenames are `<zero-padded-id>-<slug>.md`. The id in the filename must equal the id in the frontmatter. |
| 2.1.5 | A file that fails schema validation is quarantined, never silently repaired. See §7. |
| 2.1.6 | Documents are read whole and written whole. There is no partial update. |

The exact frontmatter schema for work items is the upstream `hmans/beans` convention —
filename `<prefix><id>--<slug>.md` carries the id and slug, and `.beans.yml` carries the
project config; `bean:0008` records the migration. This document specifies *how* files are
written, not *what is in them*.

### 2.2 Logs — newline-delimited JSON, append-only <a id="append-only-log"></a>

For high-volume machine data: agent-run output, domain events, cost events, audit trail.

```
{"seq":1,"at":"2026-08-28T09:14:02.117Z","kind":"run.started","runId":"01J...","payload":{...},"crc":"3b8a1f04"}
{"seq":2,"at":"2026-08-28T09:14:02.664Z","kind":"run.output","runId":"01J...","payload":{...},"crc":"a1077c9e"}
```

Rules:

| # | Rule |
|---|---|
| 2.2.1 | One JSON object per line. No pretty-printing, no embedded newlines (escape them). |
| 2.2.2 | Append-only. A record is **never** modified or deleted in place. A correction is a new record that supersedes an earlier one by `seq`. |
| 2.2.3 | Every record carries a monotonic `seq` (per log), an ISO-8601 UTC `at`, and a `kind`. |
| 2.2.4 | `seq` is the resume cursor for streaming. An SSE `Last-Event-ID` is a `seq`. |
| 2.2.5 | Every record carries `crc`, the CRC-32C of the record's canonical serialisation with the `crc` field itself omitted (§8 makes serialisation deterministic, which is what makes this reproducible on read). `crc` is the last key. |
| 2.2.6 | A record whose line does not parse, lacks `crc`, or whose recomputed `crc` does not match is **torn**: it is skipped on read, reported, and the log is marked `degraded` (§7). It is never repaired and never silently dropped. |
| 2.2.7 | A truncated final line — no trailing newline, at the end of the file — is discarded on read and truncated away before the next append. This is the **only** permitted repair, it applies only to the last line, and it is logged. |
| 2.2.8 | Logs roll at a size threshold into `NNNN.ndjson` segments. Segments are immutable once rolled. |

### 2.3 Choosing between them

| Question | Document | Log |
|---|---|---|
| Will a human read or edit it directly? | yes | no |
| Does it have a lifecycle with states? | yes | no |
| Is it produced faster than a human can read? | no | yes |
| Does the latest version matter, or the whole history? | latest | whole history |
| Is it derived from something else? | neither — derived data is an index (§9) | |

---

## 3. On-disk layout

```
<root>/
  domains/
    <domainId>/
      domain.md                       the domain document
      work/…                          work-item documents (schema owned separately)
      memories/
        domain/<memoryId>.md
        epic/<epicId>/<memoryId>.md
        story/<storyId>/<memoryId>.md
      runs/
        <runId>/
          run.md                      run summary document
          output/0001.ndjson          append-only output log segments
      events/0001.ndjson              domain event log
      cost/0001.ndjson                spend event log
      modules/<moduleId>.md           installation records
  identity/
    actors/<actorId>.md
    grants/<grantId>.md
  .modus/
    locks/                            lock files (never committed)
    index/                            derived indexes (never committed, rebuildable)
    quarantine/                       files that failed validation
    tmp/                              never used — temp files go beside their target
```

`.modus/` is git-ignored in its entirety. Everything outside `.modus/` is intended to be
committed, which is what makes `git` the audit log.

### 3.1 `.beans/` and `domains/<domainId>/work/` are the same thing

There is **one** work-item concept, stored in **one** shape, and it lives at
`domains/<domainId>/work/`. `.beans/` at this repository's root is that directory for the
`modus` domain — the domain whose product is Modus itself — reached by a shorter path
because a repository root is where humans and agents look first.

- The **schema** is identical in both places (the upstream `hmans/beans` convention; see
  `bean:0008`). A tool that reads one reads the other.
- `00-constitution.md` §7.2 and `80-agent-operating-procedure.md` step 1 say "the work item
  in `beans/`" because an agent working *on this repository* is always in the `modus`
  domain. That is a shorthand for the general path, not a second mechanism.
- When Modus manages this repository (`00-constitution.md` §12), `.beans/` is what it
  manages, with no migration and no import step. That is the point of picking one shape:
  self-hosting must not require a conversion.

If a future store root wants `modus`'s work items under the general path instead, that is
a directory move, not a design change.

---

## 4. Atomic write <a id="atomic-write"></a>

**Every document write uses this sequence, without exception.** It is implemented once,
in `adapters/adapter-persistence-flatfile`, and nothing else in the codebase calls
`Files.write`, `File.writeText`, or an `OutputStream` on a target path.

```
1. Serialise the whole record into a byte array in memory.
2. Create a temp file IN THE SAME DIRECTORY as the target:  <target>.<random>.tmp
   (Same directory is mandatory: rename is only atomic within a filesystem.)
3. Write all bytes to the temp file.
4. fsync the temp file's descriptor.          <-- data is on stable storage
5. Close the temp file.
6. rename(temp, target)                        <-- ATOMIC_MOVE, REPLACE_EXISTING
7. fsync the PARENT DIRECTORY's descriptor.    <-- the rename itself is durable
```

Step 7 is the one people skip. Without it, POSIX permits the directory entry to be lost
in a crash even though the file's data was synced: you get a durable file nobody can
find, and the old content still visible. **Enforcement gap:** neither the ArchUnit rule
restricting `java.nio.file.Files` write methods to a single class, `AtomicFileWriter`, nor
the Detekt `ForbiddenMethodCall` entry for `File.writeText`, `File.writeBytes` and
`Files.newOutputStream` outside that class exists — `AtomicFileWriter` itself does not
exist. `bean:0017` carries it.

### 4.1 Guarantees this gives

- A reader **never** observes a partially written document. It sees the old bytes or the
  new bytes, never a mixture, never an empty file.
- A crash at any point leaves either the previous version intact plus an orphan `.tmp`,
  or the new version complete. Orphan `.tmp` files older than one hour are swept at
  startup and their existence is logged.
- No reader ever needs a lock to read a document. `rename` gives readers atomicity for
  free, which is why reads are lock-free (§6).

### 4.2 Appends are different

An append to an NDJSON log does not use temp-file-and-rename — that would rewrite the
whole file. Instead:

```
1. Serialise the record canonically, compute crc, append '\n'.
2. Take the log's appender lock (§6.2).
3. Write all bytes to a descriptor opened O_APPEND, LOOPING until every byte is written.
4. Release the lock.
5. fsync on the durability boundary (§5).
```

Three separate mechanisms, each doing one job. It matters that they are kept distinct,
because the tempting single-mechanism story — "a small write is atomic, so nothing can
tear" — is **false**, and the design used to rest on it:

| Mechanism | What it actually guarantees | What it does not |
|---|---|---|
| `O_APPEND` | Offset selection and the write are one atomic step against other appenders to the same file, so an append never *overwrites* another. POSIX-specified. | It does not promise the write is issued as one `write(2)`, so it does not prevent *interleaving* between two appenders when one write is split. |
| The appender lock | Every record from this process is written start-to-finish by one holder. Modus's own writers therefore never interleave, at any record size. | Nothing about a second process or a hand-editing human. |
| `crc` + torn-record skip (§2.2.5–2.2.7) | Any record that *did* tear or interleave is **detected** on read and never parsed as data. | It does not prevent tearing; it makes tearing survivable, which is the achievable goal. |

**Why the previous rule was wrong**, recorded so nobody reintroduces it. `PIPE_BUF` is
POSIX's atomicity bound for writes to a **pipe or FIFO**; it says nothing about regular
files, and POSIX promises no non-tearing write to a regular file at any size. It is also
4096 on Linux but **512** on macOS/Darwin — the platform Modus is developed on — so the
constant was misleading as well as inapplicable. Worse, "one `write(2)` per record" is not
something the JVM can offer: `FileChannel.write` and `FileOutputStream.write` may perform
a short write and loop, and there is no API to assert a single syscall. So a size
threshold guarded nothing, while the real risk — a partial write of a large record — was
unhandled. Hence step 3's explicit retry loop and the `crc`, which are things we control.

Linux does happen to serialise a single `write()` to a regular file under the inode lock
regardless of size. That is a stronger property than the one previously claimed, and we
still do not rely on it: it is an implementation detail of one kernel, not a portable
guarantee, and the store must be correct on macOS too.

**Durability itself comes from `fsync` (§5), never from write size.** A write that reaches
the page cache intact is still lost in a power failure; a write that is fsynced is not.
The two questions — "can a record tear?" and "is a record durable?" — are independent, and
conflating them is what produced the original error.

---

## 5. Durability boundaries — when to fsync

`fsync` is the expensive operation. Calling it on every record makes streaming unusable;
never calling it makes durability a lie. The policy:

| Data | fsync policy | Rationale |
|---|---|---|
| Documents (work items, memories, domains, grants) | **Always**, per write (steps 4 and 7 above) | These are the system of record. Losing one loses a decision. |
| Domain event log | **Always**, per record | Events drive projections and other contexts; a lost event corrupts derived state permanently. |
| Cost event log | **Always**, per record | Money. Non-negotiable. |
| Agent-run output log | **Grouped**: fsync at most every 200 ms, or immediately on run completion, or immediately before acknowledging a client's resume cursor | Output is high-volume and reproducible-ish; losing the last 200 ms of a crashed run's stdout is acceptable, and the alternative is a syscall per token. |
| Derived indexes | **Never** | Rebuildable by definition (§9). |

The grouped policy has one hard requirement: **a `seq` is never handed to a client as a
durable cursor before the record at that `seq` is fsynced.** The streaming adapter may
push output to a live subscriber before the fsync (the subscriber has it in memory
anyway), but the resume cursor it advertises lags to the last synced `seq`. On resume
after a crash the client may re-receive up to 200 ms of output; duplicates are cheap,
gaps are not.

This contract rests on `fsync` and on nothing else. In particular it does **not** assume a
record is written by one syscall or that any record size is atomic (§4.2). A record that
was torn by a crash fails its `crc` on read and is skipped; because the advertised cursor
never runs ahead of the last fsynced `seq`, a skipped record is always one the client was
never promised.

**Enforcement gap:** neither test exists — the `SIGKILL`-at-randomised-points test, nor the
companion test that corrupts a segment's bytes and asserts the reader skips exactly that
record, marks the log `degraded`, and still serves every other record. `bean:0017` carries
both.

---

## 6. Concurrency and locking

### 6.1 The model

- **Reads are lock-free.** Atomic rename guarantees a reader sees a complete version.
  A reader that needs a consistent multi-file view takes a shared lock (§6.3).
- **Writes to one document are serialised** by an advisory lock on that document.
- **Appends to one log** are serialised by that log's **appender lock**, held across the
  whole record regardless of size (§4.2). `O_APPEND` prevents a *second process* from
  overwriting; the per-record `crc` makes any residual tear detectable on read. There is
  no size threshold and no size-dependent branch — a record's length never changes which
  code path runs.
- Modus assumes **one writer process** per store root. Multiple processes are supported
  well enough not to corrupt data, but not tuned for.

### 6.2 In-process

A striped `ReentrantReadWriteLock` keyed by canonical path, held for the shortest
possible span: acquire, read-modify-write, release. Rules:

- Never hold a lock across a network call, a subprocess, or an agent invocation. An agent
  run takes minutes; a lock must never take minutes.
- Never acquire two locks without going through the ordered multi-lock helper (canonical
  path order), which is the only place allowed to hold more than one. Deadlock avoidance
  by total ordering.
- Lock acquisition has a timeout (default 10 s). Timing out throws a domain-typed
  `StoreContentionException`, which the REST adapter maps to `409 Conflict`. It never
  blocks indefinitely.

### 6.3 Cross-process

`FileLock` (`FileChannel.tryLock`) on a sidecar file in `.modus/locks/`, named after a
hash of the target path. Exclusive for writes, shared for consistent multi-file reads.
Stale locks (holder PID no longer alive) are broken at startup with a logged warning.
`.modus/locks/` is git-ignored — a lock file must never be committed or restored from a
backup.

### 6.4 Optimistic concurrency for documents

Every document write is conditional on the version the writer read:

- The store records a content hash of the bytes it last read for that path.
- On write, it re-reads and re-hashes under the lock. If the hash differs, someone else
  wrote in between: the write is rejected with `StaleWriteException` → `409 Conflict`
  with the current version in the body.
- The caller re-reads, re-applies, retries. Retries are bounded (3) and the domain layer
  decides whether a retry is safe.

This is what replaces database row versioning. It is **required** for every document
mutation; there is no unconditional overwrite API. **Enforcement gap:** the store's public
port does not exist yet, so there is no `write(path, bytes)` method to have omitted —
`bean:0017` carries defining the port with only the expected-version-guarded mutation
entry point.

### 6.5 Multi-document changes

There are no multi-document transactions, and this is deliberate: it is the same
constraint as `20-ddd-practices.md` §2.1.2 (one aggregate per transaction), surfacing at
the storage layer. A use case that would need to write two documents atomically must be
restructured: write one, emit an event, let the handler write the other, and make the
handler idempotent.

Where an operation genuinely must be all-or-nothing across files (installing a module,
say), use the **intent-log pattern**: append an `intent` record to the event log, perform
the writes idempotently, append a `completed` record. On startup, any `intent` without a
`completed` is replayed. This gives at-least-once with idempotence rather than
atomicity — which is what a distributed-ish system can honestly offer.

---

## 7. Crash consistency and recovery

On startup, `adapter-persistence-flatfile` runs a recovery pass. It is fast (a directory
walk) and it is not optional.

| Condition found | Action |
|---|---|
| Orphan `*.tmp` beside a target, older than 1 hour | Delete; log at WARN with the path |
| Orphan `*.tmp` younger than 1 hour | Leave (another process may be mid-write); log at DEBUG |
| Log file whose final line is truncated (no trailing newline) | Truncate to the last complete newline; log at WARN with byte count discarded. Only the final line qualifies (§2.2.7). |
| Log line that fails to parse, or whose `crc` does not match | Skip it, count it, mark the log `degraded`, log at ERROR with the byte offset. **Never repair, never renumber.** A torn record can sit anywhere in the file, not only at the end, so the reader checks every line — a fragment of one record can be followed by a complete later record and then the fragment's tail. |
| Log with a `seq` gap | Log at ERROR; mark the log degraded; do not silently renumber |
| Document failing schema validation | Move to `.modus/quarantine/<timestamp>/`, log at ERROR, continue. **Never** auto-repair. |
| `intent` record with no `completed` | Replay the intent idempotently; append `completed` |
| Stale lock file whose PID is dead | Break; log at WARN |
| Derived index missing, stale, or unreadable | Rebuild from source files |

**Never auto-repair a document.** A corrupted work item that the system "fixes" is a
silently rewritten decision. Quarantine, surface it in the backoffice as an operator
action, and let a human or an evidence-gathering agent decide.

**Enforcement gap:** the recovery test suite that would construct each condition above on
disk and assert the exact action taken does not exist. `bean:0017` carries it.

---

## 8. Markdown as both surface and store

This is the design bet, stated plainly: **the same bytes are the durable record, the
agent's working format, the human's reading format, and the diff under review.**

Consequences we accept and must respect:

| Consequence | What we do about it |
|---|---|
| A human may hand-edit a file outside Modus | The store validates on read, quarantines on failure (§7), and never assumes it wrote the last version (§6.4). Hand-editing is a supported workflow, not an accident. |
| Formatting is part of the data | Serialisation is **canonical and deterministic**: stable frontmatter key order, stable list order, LF newlines, no trailing whitespace, one trailing newline. Re-serialising an unchanged record produces byte-identical output. **Enforcement gap:** the round-trip property test — parse → serialise → parse yields an identical record and identical bytes — does not exist. `bean:0017` carries it. |
| Diffs must be reviewable | Never reflow prose on write. Never rewrite the body's whitespace. Touch only the fields that changed. A one-field change must produce a one-line diff. |
| Markdown is not a schema | Frontmatter is validated against an explicit schema on every read and every write. The body is free text and is never parsed for meaning, except for cross-reference links. |
| The backoffice must not become a worse editor than a text editor | The backoffice renders and edits the same file. It never introduces a field that only it understands. |

The rendering direction is one-way: **AI-optimal on disk, sanitised for humans at the
edge.** The backoffice may hide fields, reorder sections, and prettify. It may not
require them to be stored differently.

---

## 9. Derived indexes

Indexes exist for query performance in the REST layer (list work items by status, find
memories by subject, sum cost by epic). Rules:

| # | Rule |
|---|---|
| 9.1 | An index lives under `.modus/index/`, is git-ignored, and is **never** the source of truth. |
| 9.2 | Deleting the entire index directory must be a safe operation at any moment. |
| 9.3 | An index is rebuilt from source files on startup if missing, or if its recorded generation does not match the store's. |
| 9.4 | Indexes are updated **after** the durable write succeeds, never before, never in the same critical section. |
| 9.5 | An index is never fsynced (§5) — it is disposable. |
| 9.6 | A query answerable by reading fewer than ~100 files does not need an index. Measure before adding one. |

**Enforcement gap:** the test that deletes `.modus/index/` between every integration test
case and asserts identical API responses does not exist — there is no `.modus/index/`
code yet. `bean:0017` carries it.

---

## 10. Scale limits — know when this stops working

Stated honestly, so nobody is surprised:

| Dimension | Comfortable | Degrades | Requires an ADR |
|---|---|---|---|
| Documents per directory | < 5,000 | 5k–50k (add sharded subdirectories) | > 50k |
| Total documents per store | < 500,000 | | > 500k |
| Log segment size | < 64 MB | | roll threshold, not a limit |
| Concurrent writer processes | 1 | 2–4 | > 4 |
| Write rate, documents | < 50/s | 50–200/s | > 200/s |
| Append rate, log records | < 5,000/s | | > 5,000/s |

Modus's actual workload — human-scale project management plus agent run output — sits
far inside "comfortable" on every row except log append rate during a busy run, which is
why output logs get the grouped fsync policy. If a real deployment approaches a
"requires an ADR" cell, that is the trigger to revisit `adr/0002`, with measurements as
evidence. Not before.
