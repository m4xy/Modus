---
# modus-0017
title: The flat-file durable store adapter
status: todo
type: epic
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

## Split into four children

Sized at the start of implementation and found over the ceiling
(`doc:00-constitution#context-budget` §6.2): the four bullets above are four independent
mechanisms — an atomic rename, an append with a checksum, a Markdown codec, and a startup
walk — sharing only the directory they write into. One pull request carrying all four is
neither reviewable nor separately revertible, and each bullet is separately shippable
green. So this bean becomes the epic and the work is its children:

| child | the bullet it owns |
|---|---|
| `bean:0130` | atomic document write, locking, and the optimistic-concurrency check — bullet 1 |
| `bean:0131` | the append-only NDJSON log — bullet 2 |
| `bean:0132` | the Markdown/YAML document codec and the two identity repositories — bullet 3 |
| `bean:0133` | the startup recovery pass, quarantine, derived indexes, and the write-path enforcement rules |

Bullet 4 is not a fifth child: "integration tests only, every failure path observed" is a
standard on each of the four, not work of its own, and a child that deferred its own
negative tests would be shipping the defect `doc:00-constitution#observed-failing` exists
to refuse.

### Two consequences of the split, recorded because both are edges someone must not undo

**The `blocked_by` edges naming this bean moved to the children.** `docs-lint` check 12
refuses a `blocked_by` edge onto a `type: epic` bean, and rightly: an epic is never
selectable, so an edge onto one can never be satisfied. `bean:0018`, `bean:0039` and
`bean:0067` each named `modus-0017`; each now names all four children. All four rather than
the subset each consumer strictly needs, because "which child does the REST layer actually
require" is a judgement none of the three beans wrote down, and inventing one here would
narrow an edge on a guess. A child may be dropped from an edge later by the bean that can
show it is not needed.

**The `Enforcement gap:` lines in `documentation/` still name `bean:0017`, deliberately.**
`doc:15-repository-layout` §4.3 and `doc:40-durability` §4, §5, §6.4, §7, §8 and §9 each
close only when the whole store exists, and this bean is what "the whole store" now names.
Re-pointing each line at a child is the job of the child's own pull request, at the moment
it can show the gap closed — a gap re-pointed before it is closed is a citation that has
moved without anything being enforced.
