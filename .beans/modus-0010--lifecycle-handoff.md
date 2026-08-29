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
internals. Review cycle 2, thread 3, found the one-way half of this still missing — see
below.

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

## Review cycle 2

Five threads, all fixed in this PR (no follow-up).

| thread | finding | fix | evidence |
|---|---|---|---|
| cold-start ambiguity | `bean:0012`, `bean:0017`, `bean:0027` (plus epic `bean:0011`) all `todo`/`high`/unblocked at once; no tiebreak | added the upstream `order` fractional-index field: `bean:0012 order: A`, `bean:0027 order: B`, `bean:0017 order: C` (`pkg/bean/bean.go` confirms the field; `pkg/bean/sort.go`'s `SortByStatusPriorityAndType` sorts status, then `order` ascending — present before absent — then `priority`, then `type`); `AGENTS.md` workflow step 1 gained one rule: skip `type: epic`, else highest `priority` then lowest `order` | re-ran the cold-start test: read only `AGENTS.md` + `.beans/`; unblocked `todo` beans excluding the epic are `{bean:0012, bean:0017, bean:0027}` at `priority: high` and `{bean:0024, bean:0025, bean:0026}` below it; `order` breaks the first set `A < B < C` — **`bean:0012` is next**, precisely because `doc:10-architecture#bounded-contexts` §3.1 already names it as "the first point at which either rule can be shown to fire" for the two vacuous context-isolation rules, and it unblocks `bean:0013` and `bean:0023` |
| `Enforced by:` sweep incomplete | `20-ddd-practices.md` and `60-cost-model.md` still cited `NoFloatingPointMoney`/`ForbiddenTypeNameSuffix` as real, the exact falsehood `doc:00#observed-failing` was written to condemn | swept every `Enforced by:` line in `documentation/**` (44 raw hits; 10 meta/definitional, excluded); of 34 substantive claims: 4 already self-qualified with an adjacent `Enforcement gap:` (left as-is), 9 verified real by grep/build-logic inspection (typed `rule:` references are docs-lint-checked and were not re-litigated), **27 demoted** to `Enforcement gap:` naming the owning bean — `bean:0026` (Detekt rules), `bean:0016` (`module-cost`, empty, zero tests), `bean:0017` (flat-file adapter, empty, zero tests), `bean:0018`/`bean:0022` (REST layer, backoffice-on-live-API), `bean:0013`/`bean:0014`/`bean:0015` (`work`/`execution`/`memory`, unbuilt), `bean:0024` (commit-message check), `bean:0027` (branch protection, DB-driver dependency verification, baseline-file Gradle check — no owning bean existed yet) | per-claim `grep`/`find` against `core/`, `adapters/`, `build-logic/`, `config/`, `.github/`; `gh api repos/m4xy/Modus/branches/main/protection` → `404 Branch not protected` for the branch-protection half of `00-constitution` §7.1; `bash tools/docs-lint.sh` after every edit → `docs-lint: OK` throughout |
| `bean:0027` scope | "Start from" named only `doc:00-constitution`, missing `doc:40-durability`/`doc:60-cost-model`'s ~9 lines for `adapter-persistence-flatfile`/`module-cost` | added a second "Start from" bullet naming both documents and both empty-placeholder modules explicitly | `find adapters/adapter-persistence-flatfile -path '*test*'` and `find modules/module-cost -type f` — no test directory, two/three skeleton files only, in either |
| memory/execution mutual wall | both beans unblock off `bean:0013` simultaneously, each needing the other's not-yet-published event | sequenced, did not split: `bean:0015 blocked_by: [modus-0013, modus-0014]` (one-way only — `bean:0014` keeps `blocked_by: [modus-0013]`); `bean:0014`'s success criteria drop `MemoryRecorded` consumption, `bean:0015`'s success criteria gain "wires `execution`'s deferred `MemoryRecorded` consumption"; `bean:0011`'s epic text restated to describe the one-way edge, not "neither direction" | `.beans/modus-0014--execution-bounded-context.md`, `.beans/modus-0015--memory-bounded-context.md`, `.beans/modus-0011--remaining-bounded-contexts.md` diffs |
| `bean:0018` missing edge | `blocked_by: [modus-0017]` only; §5.3 step 4 needs domainmgmt's `ModuleInstallation` (`bean:0012`) | added the edge (`blocked_by: [modus-0017, modus-0012]`) rather than narrowing the criteria — narrowing would ship the authorisation contract with a known hole and a second bean to patch it later; the edge lands it whole, once | `.beans/modus-0018--domain-scoped-rest-layer.md` diff |
