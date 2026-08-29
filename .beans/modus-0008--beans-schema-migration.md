---
# modus-0008
title: Migrate work items to the hmans/beans on-disk convention
status: completed
type: task
priority: high
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T00:14:27Z
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

- `modus-0006` (test taxonomy) gets `parent: modus-0003` (build foundation) — `modus-0006`'s
  own scope lists `build-logic/` convention plugins and module `build.gradle.kts` files as
  owned (`.beans/modus-0006--test-taxonomy.md` §Scope); `modus-0003`'s scope is the work
  item that created those same convention plugins and skeleton build files
  (`.beans/modus-0003--build-foundation.md` §Scope). `modus-0006` modifies infrastructure
  `modus-0003` built — a parent/child dependency, not inferred from topic overlap.
- `modus-0005` (front-matter and docs-lint) gets `blocked_by: [modus-0004]` — its own
  body states it "closes the four back-fill and enforcement follow-ups of `bean:0004`".
- No other relationship is invented; the remaining four beans are independent
  top-level work.

## Review cycle 1

Three threads, all fixed in this PR (no follow-up).

| thread | finding | fix | evidence |
|---|---|---|---|
| `tools/docs-lint.sh:36` | nine bare `beans/NNNN` prose references survive the migration, invisible to check 6 (typed references only) | all nine converted — seven to typed `bean:0001`, two (`30-code-style.md`, `80-agent-operating-procedure.md`, the `@Disabled` examples) to the new `bean:NNNN` convention from thread 2; added check 10 (`grep -noE '\bbeans/[0-9]'` over `documentation/*.md`, `AGENTS.md`, `CLAUDE.md`) so a bare path is a lint failure, not a silent gap | planted `beans/0001` in `documentation/00-constitution.md`, ran `tools/docs-lint.sh`, observed `FAIL check 10  documentation/00-constitution.md:138: bare beans/ path in prose; use a typed bean:NNNN reference (doc:05#reference-syntax)`, reverted |
| `.beans/modus-0008--beans-schema-migration.md:50` | `TestPurityRulesTest.kt`'s `disabledCarriesAWorkItem` rule and its failure message still mandate `beans/NNNN`, a path this PR deletes; `30-code-style.md` and `80-agent-operating-procedure.md` document the same dead format | fixed in this PR (the no-`architecture-tests/`-touch restriction was scoped to avoid a concurrent agent that has since finished): `WORK_ITEM` regex, the violation message and both doc citations changed from `beans/NNNN` to `bean:NNNN` | planted `@Disabled("wrong-format")`, ran the test, observed the rule fail with the updated message; planted `@Disabled("bean:0006: proving the rule fires")`, observed `BUILD SUCCESSFUL`; both reverted |
| `.beans/modus-0006--test-taxonomy.md:8` | `parent: modus-0003` cited "the task brief" as evidence, which the reviewer had no access to and could not verify | relationship kept (correct — the brief did name it, and `modus-0006`'s scope genuinely depends on convention plugins `modus-0003` created); evidence reworded above to the concrete scope-overlap dependency instead of an unverifiable citation | `.beans/modus-0006--test-taxonomy.md` §Scope and `.beans/modus-0003--build-foundation.md` §Scope both list `build-logic/` / module `build.gradle.kts` |

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

## Summary of Changes

Merged as PR #7 (`14f54ea`). Work items moved from `beans/NNNN-slug.md` to
`.beans/modus-NNNN--slug.md` with `.beans.yml` carrying `beans.prefix`, ids echoed as a
`# modus-NNNN` comment and upstream front-matter fields, so the `beans` CLI, TUI and
GraphQL server run against this repository unmodified. `tools/docs-lint.sh` reads the
prefix from `.beans.yml` rather than holding a second copy, and gained check 10 —
`doc:05-authoring-for-agents#checks` — which rejects a bare `beans/NNNN` path in prose.

Re-checked against `main` at closure: `.beans.yml`, all six migrated files, check 6 and
check 10, and the `@Disabled` annotation-value contract of `doc:30-code-style#archunit-rules`
§5.1 are all present as evidenced. No claim drifted. Criterion 2's "six migrated beans" is
exact for this commit: `bean:0007` was never on `main` at the old path and arrived
already migrated (see that bean's R5).
