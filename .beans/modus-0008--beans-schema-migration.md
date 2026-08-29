---
# modus-0008
title: Migrate work items to the hmans/beans on-disk convention
status: in-progress
type: task
priority: high
created_at: 2026-08-29T00:00:00Z
---

# Migrate work items to the hmans/beans on-disk convention

Work items lived at `beans/NNNN-slug.md` with the id in front-matter — a bespoke shape
invented before the upstream `hmans/beans` on-disk schema was known. Migrate every
existing bean to the real convention (`.beans.yml` + `.beans/<prefix><id>--<slug>.md`,
id echoed as a `# <id>` comment, upstream front-matter fields) so the `beans`
CLI/TUI/GraphQL tooling works against this repository unmodified, and every
`bean:NNNN` reference across the documentation package keeps resolving.

## Decision — keep numeric ids

Upstream generates new ids as a short nanoid (`NewID`, `pkg/bean/id.go`) using
`beans.id_length` and its own alphabet, but it does not require that shape on import.
`ParseFilename` (same file) only splits the filename on `--` (or `.`, or a single `-`
as a legacy fallback) — the id is whatever text sits before the separator, no length
or charset check. `Core.Load` (`pkg/beancore/core.go:203`) takes that id verbatim; the
`# <id>` comment line is written by `Render` but never parsed back (`Parse` ignores
it entirely — the id always comes from the filename). Consequence: `0001`..`0006` are
exactly as valid an id as `jwy7`. Kept numeric — every merged PR body already cites
`bean:0004`-style ids, and renaming to random ids would break every one for no
functional gain.

## Decision — prefix `modus-`

`.beans.yml`'s `beans.prefix` is concatenated directly onto the id with no inserted
separator (`DefaultWithPrefix`, config doc: `"myproject-abc1"`), so a prefix must carry
its own trailing separator. `modus-` ties bean ids to the project namespace shown in
`.beans.yml`'s `project.name`, reads naturally (`modus-0004`), and preserves the
existing numeric sequence every citation already uses.

## Decision — no archiving on migration

`status: completed` does not auto-move a bean; `Core.Archive` (`pkg/beancore/core.go`)
is a separate, explicit action. All six migrated beans keep `status: completed` (they
are fully delivered, merged, evidenced work) but stay in `.beans/`, not
`.beans/archive/`, matching upstream's actual behaviour and keeping `bean:NNNN`
resolution a flat, one-directory glob.

## Relationships recorded

- `modus-0006` (test taxonomy) gets `parent: modus-0003` (build foundation) — the
  taxonomy work is mechanically enforced by the Gradle foundation build 0003
  establishes, and the task brief names this relationship explicitly.
- `modus-0005` (front-matter and docs-lint) gets `blocked_by: [modus-0004]` — its own
  body states it "closes the four back-fill and enforcement follow-ups of `bean:0004`".
- No other relationship is invented; the remaining four beans are independent
  top-level work.

## Success criteria and evidence

| # | criterion | evidence | result |
|---|---|---|---|
| 1 | `.beans.yml` exists with a justified prefix and sensible defaults | this file, decisions above | met |
| 2 | Every pre-existing bean migrated to `.beans/<prefix><id>--<slug>.md`, front-matter converted to the upstream schema exactly, body/evidence preserved verbatim | `git mv` for all six files; `git diff --stat` shows front-matter-only content changes | met |
| 3 | Every `bean:NNNN` reference in the repository still resolves to exactly one file | `tools/docs-lint.sh` check 6 updated to glob `.beans/<prefix><id>*.md`, prefix read from `.beans.yml`; `docsLint` passes (below) | met |
| 4 | `documentation/40-durability.md` §3.1 states `.beans/` as the `modus` domain's work store; nothing else in that file touched | `git diff documentation/40-durability.md` — one section, five `beans/` → `.beans/` substitutions | met |
| 5 | Rename history preserved | `git log --follow` on each new path resolves to its original commit | met |
| 6 | `./gradlew clean && ./gradlew --no-build-cache qualityCheck` passes | PR body `verify` block | met |
| 7 | Check 6 provably fires on a broken reference | a planted reference to a non-existent bean id failed with `resolves to 0 files, expected exactly 1`, then reverted | met |
| 8 | No dangling `bean:` reference remains anywhere in the repository | exhaustive `grep -rn 'bean:[0-9]'` cross-checked against `.beans/` contents | met |

Evidence for criteria 3, 6, 7, 8 is recorded verbatim in this work item's pull request
body (`verify` block), not duplicated here.
