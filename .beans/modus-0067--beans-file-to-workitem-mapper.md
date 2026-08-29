---
# modus-0067
title: The .beans file to WorkItem mapper
status: todo
type: feature
priority: high
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0013]
---

# The .beans file to WorkItem mapper

Modus cannot read its own work items.

`doc:00-constitution` §13 makes self-hosting the destination, and the thinnest form of it is
`POST /domains/modus/runs {beanId}` — an agent run against a bean in this repository.
**Nothing in the tree can turn a `beanId` into anything.** `.beans/` holds one Markdown file
per work item, and the only thing that has ever parsed them is `tools/docs-lint.sh`, in
`awk`, for lint purposes. No Kotlin source mentions `.beans` at all. `{beanId}` is
unresolvable, so the skeleton has no first step.

This is the mapper that makes it resolvable, and it is the reason this bean is on the
critical path rather than beside it.

## What the on-disk convention actually is

Upstream `hmans/beans`, configured by `.beans.yml`. `bean:0008` established these against
the upstream source and they are restated here only as the mapper's input contract — the
migration bean is the record.

| fact | value | consequence for the mapper |
|---|---|---|
| store root | `.beans.yml` `beans.path`, `.beans` | Read from config, not hard-coded. `docs-lint` already reads `beans.prefix` and `beans.id_length` from that file rather than duplicating them; the mapper does the same (`doc:05-authoring-for-agents#one-fact-one-place`). |
| file name | `<prefix><id>--<slug>.md`, prefix `modus-`, no separator inserted between prefix and id | The prefix carries its own trailing separator. |
| **id source** | the **filename**, split on `--` | The `# modus-0067` line inside the front matter is a YAML *comment* written by upstream's `Render` and **never parsed back** — upstream's `Parse` ignores it entirely. A mapper that reads the id from the marker instead of the filename disagrees with every other reader of this store, including `beans` itself. `docs-lint` check 13b exists precisely because the two can drift. |
| id shape | unconstrained by upstream; numeric here by decision | The mapper MUST NOT require 4 digits. `id_length` governs *generation*, not *parsing*. |
| body | Markdown after the front-matter block, verbatim | The file **is** the record (`doc:00-constitution` §2.2), so a round trip may not reformat prose. |
| archiving | `status: completed` does not move a file; `Core.Archive` is a separate explicit action | Resolution is a flat one-directory glob. |

Front-matter keys present across the corpus today: `title`, `status`, `type`, `priority`,
`order`, `parent`, `blocked_by`, `created_at`, `updated_at`. `blocked_by` is a YAML flow
list. `.beans.yml` supplies `default_status: todo` and `default_type: task` for keys a file
omits, and `order` is upstream's fractional index — absence is a defined position, sorting
after every bean that carries one (`AGENTS.md`, workflow step 1).

## Where it goes

| piece | layer | rule |
|---|---|---|
| `WorkItem`, its states, its identifiers | `core-domain`, `work` context | `bean:0013`. This bean does not model it. |
| the repository port the mapper satisfies | `core-domain`, `work` context | `doc:00-constitution` §1.2 — ports inside, adapters outside. |
| front-matter parse, filename parse, body split, render | `adapters/adapter-persistence-flatfile` | `doc:15-repository-layout#placement-table` §2.1, row "A file layout, serialisation format, or locking strategy". |

No YAML or Markdown type crosses into `core-domain`: `doc:00-constitution` §1.1 bars
Jackson and any serialisation library from the core, and §1.3 bars `java.nio.file` from it
as well. The mapper's signature speaks domain types on one side and `String`/`Path` on the
other, and nothing in between escapes inward.

## Ordering, and why this can ship before the store is durable

**`bean:0013` is a hard dependency** and is recorded as `blocked_by` above: a mapper needs
the type it maps to, and `work` is a marker object holding a `NAME` constant today. The
order is `bean:0013` → this bean → anything that resolves a `beanId`.

**`bean:0017` is not a dependency for the reading half.** Resolving `{beanId}` needs a read;
`bean:0017` supplies atomic write, append-only logs, CRC recovery and locking, none of which
a read touches. Writing a bean back — a status transition landing on disk — does need it,
and that half waits. Splitting the bean on that line is what keeps the skeleton's first step
small; a single bean covering both would be blocked on the whole durability stack for a
capability that only reads.

Consequence to state plainly: until `bean:0017` lands, this adapter reads a store that a
human and an agent both write by hand, and a read that races a concurrent write is a real
possibility, not a theoretical one — this repository has already had two agents allocate the
same bean id in parallel worktrees (`bean:0051`). The mapper's job is to fail loudly on a
file it cannot parse, not to guess.

## Scope

Owned: the mapper and its tests, the `work` repository port's flat-file implementation, and
this bean.

Not owned: `WorkItem` itself (`bean:0013`); atomic write, locking and the append-only log
(`bean:0017`); the REST route that would call this (`bean:0018`); `tools/docs-lint.sh`,
which parses the same files for a different purpose and is not replaced by this — a lint
script and a domain mapper answering to the same schema is a duplication worth recording
now and resolving when both exist, not a reason to couple them.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | A `beanId` resolves to a `WorkItem` read from `.beans/<prefix><id>--<slug>.md`, with the id taken from the **filename** and not from the `# <id>` comment marker | |
| 2 | The store root and the prefix are read from `.beans.yml` at runtime, not hard-coded; changing `beans.prefix` there changes what the mapper resolves, with no source edit | |
| 3 | Every front-matter key the corpus uses maps to something on `WorkItem` or is explicitly and visibly discarded — `title`, `status`, `type`, `priority`, `order`, `parent`, `blocked_by`, `created_at`, `updated_at`. A key silently dropped is a data-loss defect, so the mapper reports unknown keys rather than ignoring them | |
| 4 | `.beans.yml`'s `default_status` and `default_type` are applied to a file that omits those keys, and a file that states them is not overridden | |
| 5 | `blocked_by` parses as a list, including the empty and single-element cases, and a `parent` or `blocked_by` id that resolves to no file is reported rather than dropped | |
| 6 | Reading every bean file on disk succeeds, and the count of files parsed equals the count of files present — a mapper that parses a subset and reports success has examined less than it claims (`doc:00-constitution#observed-failing`) | |
| 7 | A malformed bean — no front-matter block, an unterminated block, a filename with no `--` — fails loudly and names the file. Observed on each of the three, planted and reverted | |
| 8 | The Markdown body survives a read unchanged, byte for byte, including fenced blocks and tables | |
| 9 | The **input surface** is asserted separately from the **verdict**: a test states which bytes the parser was given and which fields it perceived, distinct from any test of what a caller concludes from a `WorkItem`. A fixture that hands the mapper a well-formed front-matter block has not tested the code that reads a front-matter block off disk | |
| 10 | Fixtures vary rather than repeat: at least one bean with no `order`, one with none of the optional keys, one with a multi-element `blocked_by`, one `completed` and one `todo` (`doc:35-testing#fixture-variation`) | |
| 11 | No serialisation library, no `java.nio.file` type and no Markdown type appears in `core-domain`; `rule:archunit/domainIsFrameworkFree` and `rule:archunit/domainDependsOnNoOuterLayer` stay green | |
| 12 | `./gradlew qualityCheck` green | |
