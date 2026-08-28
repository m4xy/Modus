# AGENTS.md

Modus is a harness for agentic development: a Kotlin/Spring Boot server, DDD-layered
(`core-domain` → `core-application` → adapters and modules → `app/modus-server`), whose
source of truth is flat files — Markdown documents and append-only NDJSON logs — rooted at
`/domains/{domainId}`. Rules are enforced by ktlint, Detekt, ArchUnit and CI, not by review
comments. A backoffice renders the flat files for humans.

This file is canonical for agent onboarding and routes; it never restates a rule.
`documentation/` is the authority. Follow-up (`bean:0004`): reduce `CLAUDE.md` to a pointer
here, and add `doc:05-authoring-for-agents` to `documentation/README.md`.

## Routing

Derived from each document's `read_when` front-matter (`doc:05-authoring-for-agents#read-when`).
Front-matter is normative; on disagreement it wins and this table is the bug.

Read the first row plus every row matching the task. **Read nothing else.**

| task shape | read | do not read |
|---|---|---|
| any task | `doc:00-constitution`, `doc:80-agent-operating-procedure` | every document below that no row selects |
| Kotlin under `core/**` | `doc:20-ddd-practices`, `doc:10-architecture` | `doc:40-durability` unless the change is persistence |
| module graph, ports, adapters, endpoints | `doc:10-architecture` | `doc:20-ddd-practices` unless you add a domain type |
| persistence, file IO, locking, on-disk format | `doc:40-durability` | `doc:10-architecture` |
| a style, ktlint or Detekt failure | `doc:30-code-style` | everything else — run `./gradlew ktlintFormat` first |
| recording or citing a conclusion | `doc:50-memory-and-evidence` | |
| choosing a model, an effort level, reporting spend | `doc:60-cost-model` | |
| a task you have now done three times | `doc:70-skills` | |
| build scripts, `build-logic/`, CI | `doc:30-code-style`, `rule:ci/build` | the rest of `documentation/` |
| `documentation/**`, `AGENTS.md`, `beans/**`, the PR template | `doc:05-authoring-for-agents` | `doc:10`–`doc:70` |
| reviewing a pull request | the PR body, then only the documents in its `refs:` | any document the PR body does not reference |

## Commands

```bash
./gradlew build ktlintCheck detekt   # exactly what CI runs — rule:ci/build
./gradlew ktlintFormat               # fix style mechanically; never hand-format
./gradlew :architecture-tests:test   # rule:archunit/*
./gradlew qualityCheck               # every module's check plus build-script linting
./gradlew :modus-server:bootRun      # run the server
```

JDK 25 toolchain. Versions live in `gradle/libs.versions.toml` and nowhere else.
Style rules: `doc:30-code-style`. Layering rules: `doc:10-architecture`. Neither is repeated here.

## Workflow

1. Bean — `beans/NNNN-slug.md`. It is the source of truth for what is being done.
2. Branch from `main` (`feat|fix|docs|chore/…`). No direct commits to `main`.
3. Conventional commits. PR body: fill `.github/pull_request_template.md`; do not narrate.
4. Review — every thread ends in a change, a new rule, or a stated refusal.
5. `rule:ci/build` green, then merge. Normative: `doc:00-constitution` §7.

## Context budget

Ceiling: 300k tokens per work package (`doc:00-constitution` §6). Measured overrun: 268k on the
PR #1 package, caused by reading documents whole because nothing said what to skip.

- Load front-matter first, select with `read_when`, then read only the selected documents.
- Follow a reference to its anchor and read that section — not the file it lives in.
- `depends_on` is not a reading list (`doc:05-authoring-for-agents#front-matter`).
- Never paste dependency trees, build scans, generated reports or whole files into context.
- If the work does not fit, split the bean. Do not grow the context.
