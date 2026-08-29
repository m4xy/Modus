---
# modus-0017
title: The flat-file durable store adapter
status: todo
type: feature
priority: high
order: C
created_at: 2026-08-29T00:00:00Z
---

# The flat-file durable store adapter

Why: `doc:00-constitution#flat-file-first` makes the filesystem the source of truth,
`adapters/adapter-persistence-flatfile` holds a placeholder descriptor, and `bean:0009`
declared two ports and implemented neither — so nothing in Modus survives a restart. Every
later context declares more ports against this one adapter, so it is built once.

Success criteria — the mechanisms `doc:40-durability` specifies, each observed working:

- Documents: `doc:40-durability#atomic-write` — temp file in the same directory, `fsync`,
  `rename`, `fsync` the parent — with §6.4's optimistic-concurrency check.
- Logs: `doc:40-durability#append-only-log` — `O_APPEND`, a per-log appender lock with
  short-write retry, per-record CRC-32C, checksummed NDJSON, and recovery-on-read that
  accounts for a degraded log rather than failing the read. A torn tail is detected by its
  checksum, not assumed absent (§7).
- `ActorRepository` and `PermissionGrantRepository` implemented, honouring the
  duplicate-`GrantId` rule of `bean:0009` review thread 2.
- Integration tests only (`doc:35-testing#definitions`). Every failure path observed, not
  argued (`doc:00-constitution#observed-failing`): truncate a record mid-write, corrupt a
  CRC, hold the lock from a second process.

Blocks `bean:0018` and every context's persistence.
