---
# modus-0010
title: Close the merged work items, publish the backlog, encode the session learnings
status: in-progress
type: task
priority: high
created_at: 2026-08-29T00:00:00Z
---

# Close the merged work items, publish the backlog, encode the session learnings

A cold-start agent could read `AGENTS.md` and learn how to work here, and could not learn
what to do next: `.beans/` held nine finished things and no open one, three still
`in-progress` against merged pull requests. The learnings from the session that produced
`bean:0007`, `bean:0008` and `bean:0009` lived outside the repository, which
`documentation/README.md`'s encoding rule forbids.

## Scope

Owned: `.beans/**` and `documentation/*.md`. Not owned: `core/`, `adapters/`,
`backoffice/`, `e2e/`, `build-logic/`, `config/`, `tools/`. No mechanism changes.

## Success criteria and evidence

### 1. The three merged work items are closed against what actually landed

`bean:0007`, `bean:0008` and `bean:0009` move to `completed` with `updated_at` set to
their pull request's merge timestamp and a `## Summary of Changes` section. Every
criterion was re-read against `main`. Three claims had drifted and are corrected in place,
marked as corrections:

| bean | claim | what `main` says |
|---|---|---|
| `bean:0007` | R5: "this file stays at `beans/0007-coverage.md`" | the rebase landed first, so the merge commit added it already migrated and the old path never existed on `main` |
| `bean:0009` | criterion 8: "32 tests" | 42 identity tests, 43 in `:core-domain`; ten arrived in the review cycle |
| `bean:0009` | criterion 9: "no `-Pcoverage.regress` was needed" | `config/coverage/baseline.tsv` carries the accepted-regression comment for `:core-domain` covered branches `44 -> 38` |

### 2. The backlog exists as `todo` beans with a real dependency graph

`bean:0011` through `bean:0027`. `parent` carries the bounded-context epic; `blocked_by`
carries the cases where one bean cannot start without another's output. The
published-language table of `doc:10-architecture#bounded-contexts` §3.1 is the source for
the context ordering. The intentional `memory`/`execution` cycle is deliberately **not**
encoded as mutual `blocked_by`: it would deadlock, and neither context needs the other's
internals.

### 3. The learnings are in the document that owns the topic, once

| learning | home |
|---|---|
| targeted agent mutation, as a requirement | `doc:35-testing#mutation-testing`, raised to MUST |
| uniform fixtures hide reachable defects | `doc:35-testing#fixture-variation`, new |
| citations are to primary sources, re-read | `doc:50-memory-and-evidence#primary-sources`, new |
| Detekt 1.23.8 on JDK 25, and PSI-only analysis | `doc:30-code-style#detekt-configuration` |
| Spring Boot 4.1.1 manages JUnit Jupiter 6 | `doc:35-testing#assertions` |
| JaCoCo 0.8.13 on Java 25; `const val` inlining | already stated; verified, not restated |
| **a gate is unverified until observed failing** | `doc:00-constitution#observed-failing`, new |

No new document was created. `documentation/README.md`'s `Enforced by:` convention cites
the new anchor rather than repeating it.

### 4. Two enforcement claims were found false while writing §3, and are recorded

`doc:30-code-style#custom-detekt-rules` names eleven custom Detekt rules and a
`build-logic` rule-set provider. Neither exists:

```
cmd:      grep -rln "RuleSetProvider\|ForbiddenDomainApi" build-logic/src config
observed: (no output)
```

`doc:30-code-style` §4 — which names §2's `Enforced by:` column with it — and
`doc:00-constitution#layering` §1.3 now carry an `Enforcement gap:` naming `bean:0026`.
The rules are neither implemented nor deleted here:
that is a `build-logic` change this bean does not own. `bean:0027` carries the sweep over
every other `Enforced by:` line, which the new rule makes overdue.

### 5. The gate is green

The `verify` block of the pull request.
