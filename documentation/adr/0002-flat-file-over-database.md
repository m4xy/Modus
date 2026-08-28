# ADR 0002 — Flat files over a database

- **Status:** Accepted
- **Date:** 2026-08-28
- **Deciders:** Modus core
- **Supersedes:** —
- **Superseded by:** —

## Context

Modus stores domains, work items, epics, evidence-backed memories, actors, permission
grants, module installations, agent runs, run output, and cost records. It must be durable:
losing a memory loses a decision, and losing a cost record loses money.

The default answer for a Kotlin/Spring Boot service is a relational database — Postgres, or
an embedded H2/SQLite for a single-node deployment. We are not taking the default. The
factors that push against it here are specific and, taken together, decisive.

**The primary consumers are agents, not application code.** An agent's cheapest, most
reliable interface to data is a file it can `cat`, `rg`, and diff. Every layer between the
agent and the bytes — a query language, a schema, a connection, an ORM — costs context
tokens, adds a failure mode, and adds something an agent can get subtly wrong. Context is
the binding constraint (`00-constitution.md` §6) and it is also the dominant cost line
(`60-cost-model.md` §3.1).

**The store is also the human surface.** Work items and memories are Markdown, read by
people. With a database there are two representations — rows and a rendering — and they
drift. With files there is one. That is the "AI-optimal, sanitised for humans" principle in
its most literal form.

**Data changes deserve review.** A change to a work item's success criteria or to a
domain-scoped memory is as consequential as a code change and benefits from the same
branch/PR/review flow. Files in git get that for free. Database rows do not get it at all.

**The workload is small and human-paced.** Project management for a set of repositories:
thousands to low hundreds of thousands of documents, single-digit writes per second,
one writer process. The only high-rate stream is agent stdout, which is append-only and
sequentially read — the shape a log file is best at and a relational table is worst at.

**Operational surface is a real cost.** Modus is meant to be run by one person
orchestrating from afar. A database is a component to provision, size, secure, back up,
upgrade, and debug at 2am. Its absence is a feature.

Against all that: we lose ad-hoc query, store-enforced referential integrity, real
multi-writer concurrency, and multi-record transactions. Those are genuine losses and the
decision stands or falls on whether they can be lived with.

## Decision

**Modus uses a durable flat-file store as its system of record. There is no database.**

1. **Two storage shapes only.** Human-facing entities are **Markdown with YAML
   frontmatter**; high-volume machine data is **newline-delimited JSON append-only logs**.
   Anything that fits neither is mis-modelled. Specified in
   `documentation/40-durability.md` §2.

2. **The file is the record.** Not a cache, not an export, not a projection of something
   held elsewhere. Hand-editing a file outside Modus is a supported workflow.

3. **Every document write is atomic**: serialise fully in memory → temp file in the same
   directory → `fsync` the file → `rename` → `fsync` the parent directory. Implemented
   once, in one class; no other code performs a file write. `40-durability.md` §4.

4. **Explicit durability boundaries.** Documents, domain events, and cost records are
   fsynced per write. Agent output is group-committed at 200 ms, and a `seq` is never
   advertised to a client as a resume cursor before it is synced. `40-durability.md` §5.

5. **Append integrity is detection, not prevention.** Log appends rest on three separable
   mechanisms and claim nothing beyond them: `O_APPEND` so one appender never overwrites
   another; a per-log appender lock held across the whole record, with a retry loop on
   short writes, so Modus's own writers never interleave at any size; and a per-record
   CRC-32C so any record that did tear is **detected on read**, skipped, and reported
   rather than parsed as data. No claim is made that a write of any size is atomic on a
   regular file — POSIX does not offer one, and the JVM cannot promise one syscall per
   record. This is the honest version of a guarantee an earlier draft overstated, and it
   is why recovery-on-read (§9) rather than write-side atomicity carries the weight.
   `40-durability.md` §2.2, §4.2.

6. **Optimistic concurrency replaces row versioning.** Every mutation is conditional on the
   content hash the writer read; a mismatch throws `StaleWriteException` → `409 Conflict`.
   There is no unconditional write API. `40-durability.md` §6.4.

7. **No multi-document transactions.** This is the storage-layer face of the one-aggregate
   per-transaction rule (`20-ddd-practices.md` §2.1.2). Operations spanning documents use
   the intent-log pattern with idempotent replay: at-least-once with idempotence, honestly
   labelled, rather than atomicity we cannot deliver. `40-durability.md` §6.5.

8. **Indexes are derived and disposable.** They live under a git-ignored `.modus/index/`,
   are never fsynced, are rebuilt on demand, and are never the source of truth. Deleting
   the whole index directory at any moment is a safe operation. `40-durability.md` §9.

9. **Recovery is explicit and never repairs data.** Startup sweeps orphan temp files,
   truncates a torn final log line, replays incomplete intents, and **quarantines** any
   document that fails schema validation. Auto-repair of a document is forbidden — a
   silently rewritten decision is worse than a visibly broken one. `40-durability.md` §7.

10. **The ban is mechanical.** No `java.sql`, `javax.sql`, `jakarta.persistence`,
    `org.hibernate`, or `org.jooq` type may be referenced anywhere, and a database driver
    on any Gradle configuration fails the build. Enforced by ArchUnit and a Gradle
    dependency-verification rule (`00-constitution.md` §2, `10-architecture.md` §4.1).

11. **Documented scale limits.** `40-durability.md` §10 states, per dimension, where the
    model is comfortable, where it degrades, and where it requires a superseding ADR.
    Crossing a limit with measurements is the trigger to revisit this decision — nothing
    else is.

12. **The domain does not know.** `core-domain` sees repository ports and no file paths.
    If this decision is ever reversed, the change is confined to
    `adapters/adapter-persistence-flatfile`. That containment is the main thing that makes
    the bet affordable.

## Consequences

### Positive

- **Agents read and write the store with `cat` and `rg`** — no query language, no schema
  fetch, no connection lifecycle, no ORM mapping. Fewer tokens, fewer failure modes.
- **One representation, not two.** The bytes a human reads are the bytes the system stores.
  No sync, no drift, no export step.
- **Data changes are reviewable.** A memory or a success-criteria change arrives as a diff
  in a pull request, with blame and history, using the same machinery as code.
- **Backup is `cp -r`; restore is `cp -r`; migration between machines is `rsync`.**
- **Deployment is one JVM process and a directory.** No component to provision, patch,
  size, or secure.
- **Durability bugs leave readable evidence.** A torn line or an orphan temp file can be
  inspected with `less`. A corrupted index page cannot.
- **git is the audit log**, for free, with cryptographic integrity and point-in-time
  recovery.
- **Tests are trivially isolated**: a temp directory per test, no fixtures, no migrations,
  no shared server. This keeps `core` tests in milliseconds and integration tests fast.

### Negative

- **No ad-hoc query.** Every access path must be anticipated as a purpose-named repository
  method or served by a derived index. Analytical questions mean folding logs.
  *Mitigation:* the index layer (§8); the workload's queries are few and known.
- **No store-enforced referential integrity.** A dangling `epicId` is possible.
  *Mitigation:* value-object identifiers (`20-ddd-practices.md` §3), schema validation on
  read and write, and a consistency-check sweep that raises operator actions rather than
  silently repairing.
- **No multi-record transactions.** Some use cases must be restructured into
  event-driven idempotent steps. *Mitigation:* this is required by the aggregate rules
  anyway; the constraint reinforces the design rather than fighting it.
- **Single-writer.** Horizontal scale-out of writes is not available without a superseding
  ADR. *Mitigation:* the workload is human-paced; §11's limits make the boundary explicit.
- **We implement durability ourselves.** `fsync` ordering, `rename` atomicity, `O_APPEND`
  semantics and their **limits**, torn-record detection, and lock breaking are subtle, and
  a database vendor has spent decades on them. *Mitigation:* one narrow implementation
  surface (one writer class, one append path), plus a crash-consistency test suite that
  `SIGKILL`s the process at randomised points and asserts every acknowledged cursor
  survives. This is the consequence that bit first: the append path's original atomicity
  argument was wrong (§5), and it was wrong in the direction of claiming more than the
  platform offers — the failure mode to watch for in every future durability change.
- **Filesystem behaviour varies.** `rename` atomicity holds within a filesystem but not
  across one; some network filesystems weaken `fsync` guarantees. *Mitigation:* temp files
  are always created in the target directory; a supported-filesystem list, with NFS and
  similar explicitly unsupported for the store root.
- **Directory scaling.** Large directories degrade on some filesystems. *Mitigation:*
  sharded subdirectories above 5,000 entries, per §10.

### Neutral

- Full-text search will need an index (or `rg`) rather than `LIKE`. At this scale `rg`
  across the store is genuinely fast enough, and it is the tool agents already use.
- Schema evolution becomes a document-migration concern rather than a DDL concern: a
  versioned frontmatter schema with a migration step on read. Different work, not more.

## Alternatives considered

| Alternative | Assessment | Why not |
|---|---|---|
| **Postgres** | The strongest technical option: real transactions, real concurrency, mature durability, excellent query. | Adds an operational component to a system designed to be run by one person from afar. Creates two representations of every human-facing entity, which then drift. Makes data changes unreviewable. Costs agents tokens and adds a whole class of things to get wrong. None of its strengths — multi-writer concurrency, ad-hoc analytics, cross-record transactions — is load-bearing for this workload. |
| **SQLite (embedded)** | Removes the operational component while keeping transactions and query. Genuinely attractive. | Still opaque: an agent cannot `rg` it, a human cannot read it, a diff cannot review it, git cannot merge it. It would force the two-representation problem back in, and the Markdown surface would become an export — exactly what §2 forbids. Its real advantage over files is multi-record transactions, which the aggregate rules forbid us from using anyway. |
| **Hybrid: files for documents, a database for logs and indexes** | Plays to each store's strengths; logs are the one place a database's write path is clearly better. | Two durability models, two recovery stories, two backup procedures, two failure modes, and a permanent ambiguity about which store owns a given fact. Append-only NDJSON is the shape a log file is *best* at; the marginal gain does not pay for the second system. |
| **Event sourcing into an append-only log as the sole store** | Excellent audit properties; fits the domain-event design already in place. | Reading current state requires replay or snapshots, which destroys the property that motivates the whole decision: a human or an agent opening a work-item file and reading it. We keep event logs alongside documents rather than instead of them. |
| **Git as the store, via a git library** | Versioning, merge, and history without extra machinery — a natural fit for a git-backed product. | Object-database write latency and locking are poor for per-record writes; concurrent index operations are fragile; and it couples the store to the VCS such that a `git` operation can corrupt live data. We keep the directory *in* git and let git observe it, rather than writing through git. |
| **A document database (Mongo, CouchDB)** | Schema flexibility matches Markdown frontmatter reasonably well. | All of Postgres's operational costs with weaker durability defaults, and none of the file-based benefits: still opaque, still unreviewable, still two representations. |
| **Files now, database later behind the same ports** | Keeps the option open at low cost. | This *is* the position — see Decision §12. The ports make reversal a contained change. What we reject is adding the database *now*, before any measurement shows a need. §11's limits define what that measurement would have to show. |
