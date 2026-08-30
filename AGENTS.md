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
| `documentation/**`, `AGENTS.md`, `.beans/**`, the PR template | `doc:05-authoring-for-agents` — its own `read_when` front-matter states the predicates; derived, not restated here (`#checks` check 9) | `doc:10`–`doc:80` |
| reviewing a pull request | the PR body, then **the bean it names** whole, then only the documents in its `refs:` | any document the PR body does not reference |

## Commands

```bash
./gradlew qualityCheck               # exactly what CI runs, backoffice included — rule:ci/build
./gradlew e2eTest                    # Playwright; only when user-visible behaviour changed
./gradlew ktlintFormat               # fix style mechanically; never hand-format
./gradlew :core-domain:check         # the fast gate; project names are flat, not :core:core-domain
./gradlew :architecture-tests:test   # rule:archunit/*
./gradlew :modus-server:bootRun      # run the server
GITHUB_TOKEN= gh <args>              # every gh call — the credential trap below
```

A stale `GITHUB_TOKEN` in the environment shadows the keyring credential `gh` would
otherwise use, and surfaces as `HTTP 401: Bad credentials` on an unrelated command.
`gh auth status` names which credential is in use. Clear it inline, as above —
`env -u GITHUB_TOKEN gh …` is equivalent and is refused by default in an agent sandbox,
which cannot verify what `env` does to the command it wraps.

The sandbox refuses further shapes for the same reason — it cannot decide what they touch
before they run. A compound command redirecting `>`/`>>` into a non-literal target, and a
multi-statement `for … do … done` loop that pipes. `cat file >> target` is accepted; one
`awk` over a glob replaces the loop; a `mv`, a run and a `mv` back are three separate calls.
Write new files with the Write tool.

Planting a violation and reverting it (`doc:00-constitution#observed-failing`) usually means a
script ending in `git checkout -- .beans`, which reverts uncommitted edits to **tracked**
files under that path as well as the plant — a new bean is untracked and survives, a bean you
are closing, amending or correcting does not. Commit before you plant (`bean:0102`).

JDK 25 toolchain. Versions live in `gradle/libs.versions.toml` and nowhere else.
Style rules: `doc:30-code-style`. The Module extension contract: `doc:10-architecture`.
This repository's own layout and layering rules: `doc:15-repository-layout`. None is repeated here.

## Workflow

1. Bean — `.beans/<prefix>NNNN--slug.md` (`.beans.yml`). It is the source of truth for what is being done.
   Selecting the next one: skip `type: epic` (not directly actionable — pick one of its
   unblocked children instead); among `status: todo` beans whose every `blocked_by` id is
   `completed`, the highest `priority` wins; ties break on `order`, ascending (the upstream
   `beans` fractional-index field for manual sort — a bean with no `order` sorts after every
   bean that has one).

   `completed` means `completed`. A blocker that is `in-progress` does **not** satisfy the
   edge: its output is unmerged, so work depending on it would be built against something
   that can still change in review. A bean whose only blocker is in flight is correctly
   unselectable, and the answer is to finish the blocker, not to relax the rule
   (`doc:00-constitution#bean-lifecycle`).
2. Branch from `main` (`feat|fix|docs|chore/…`), in a worktree of your own
   (`doc:80-agent-operating-procedure#worktree-per-agent`). No direct commits to `main`.
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
