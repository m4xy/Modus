# AGENTS.md

Modus is a harness for agentic development: a Kotlin/Spring Boot server, DDD-layered
(`core-domain` → `core-application` → adapters and modules → `app/modus-server`), whose
source of truth is flat files — Markdown documents and append-only NDJSON logs — rooted at
`/domains/{domainId}`. Rules are enforced by ktlint, Detekt, ArchUnit and CI, not by review
comments. A backoffice renders the flat files for humans.

This file is canonical for agent onboarding and routes; it never restates a rule.
`documentation/` is the authority.

## Routing

`documentation/README.md`'s reading order and its "minimum an agent must read" rule are
the one source for `doc:00`–`doc:80`. Follow them; this file does not restate them, so
there is nothing here for those rows to drift against (`doc:05-authoring-for-agents#one-fact-one-place`).

Two routing facts README does not carry:

| task shape | read | do not read |
|---|---|---|
| `documentation/**`, `AGENTS.md`, `beans/**`, the PR template | `doc:05-authoring-for-agents` — its own `read_when` front-matter states the predicates; derived, not restated here (`#checks` check 9) | `doc:10`–`doc:80` |
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

Ceiling: 300k tokens per work package (`doc:00-constitution` §6); see `bean:0004` for the
measured overrun.

- Load front-matter first, select with `read_when`, then read only the selected documents.
- Follow a reference to its anchor and read that section — not the file it lives in.
- `depends_on` is not a reading list (`doc:05-authoring-for-agents#front-matter`).
- Never paste dependency trees, build scans, generated reports or whole files into context.
- If the work does not fit, split the bean. Do not grow the context.
