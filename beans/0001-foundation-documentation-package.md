---
id: "0001"
title: Foundation documentation package
status: in-review
kind: epic
domain: modus
created: 2026-08-28
---

> **Provisional frontmatter.** The work-item ("bean") on-disk schema is not yet ratified —
> it is owned by a separate work package and will land as
> `documentation/90-work-items.md`. The frontmatter above is a placeholder agreed for this
> item only. **This file will be migrated when the schema lands**, and that migration is
> the first consumer of the schema — if migrating this file is awkward, the schema is
> wrong.

## What this covers

Authoring `documentation/`, the constitution of the Modus repository: the package every
future agent and human contributor reads before touching the repo. Written for an agent
audience first, humans second, and precise enough to be mechanically checkable wherever a
rule can be.

### Delivered

| File | Covers |
|---|---|
| `documentation/README.md` | Package index, the "read this when" navigation table, the minimum an agent must read, and the **encoding rule** (learnings go back into this package) |
| `documentation/00-constitution.md` | Non-negotiable rules: strict DDD layering and per-layer dependency permissions; flat-file-first; the evidence rule; investigate-don't-ask; the 300k context budget and ten concrete tactics; branch → work item → PR → review → merge with no direct commits to `main`; domain scoping; mechanical enforcement; cost consciousness |
| `documentation/10-architecture.md` | Module layout; the six bounded contexts with their aggregates, published and consumed events, and an import allowlist; the dependency rules as ArchUnit-derivable tables; the `/domains/{domainId}` API root convention and per-domain module visibility (404-not-403); trigger → run → stream flow; testing architecture |
| `documentation/20-ddd-practices.md` | Aggregate design rules and shape; value objects (including integral money); domain events; ports/adapters naming; invariant placement; the complete forbidden list for `core-domain` with a substitute for each entry |
| `documentation/30-code-style.md` | Three-tool division of labour (ktlint/Spotless, Detekt, ArchUnit); Detekt deviations from default with thresholds; **ten custom Detekt rules with rationale**; ArchUnit rule groups; TypeScript regime; the no-baselines/no-freezing rule; how to add a style rule |
| `documentation/40-durability.md` | Why files over a database; the two storage shapes; on-disk layout; the atomic-write sequence including the parent-directory fsync; per-data-class durability boundaries; concurrency, locking and optimistic concurrency; crash recovery table; Markdown as both surface and store; derived indexes; documented scale limits |
| `documentation/50-memory-and-evidence.md` | Memory scopes and selection; the six evidence kinds and their strength ordering; what is explicitly not evidence; the evidence record shape with size caps; the five write gates; invalidation triggers and statuses; hypotheses; the prohibition on unevidenced assertions |
| `documentation/60-cost-model.md` | Stage-level attribution with a first-class `overhead` stage; the spend record; the current Anthropic model line-up and pricing; price-book durability and evidence; benchmarking across model × effort with `effectiveCostUsd` as the decision metric; task-categorisation → skill-extraction pipeline with numeric triggers; cost-conscious code review; how cost surfaces in the UI and the action list |
| `documentation/70-skills.md` | Extraction triggers and the escalation order (make impossible → mechanise → memory → skill → docs → ask); what a skill must contain including success criteria, validation command and cost profile; celebrity skills and long-tail curation; the autonomous test-and-validate contract |
| `documentation/80-agent-operating-procedure.md` | The nine-step SOP: pick up, restate criteria, plan and budget, investigate, implement, self-validate, encode learnings, open the PR, respond to review — with context checkpoints, a tool cost ladder, a PR body template, and a failure-mode table |
| `documentation/adr/0001-record-architecture-decisions.md` | Why ADRs exist here, what is architecturally significant, and six rejected alternatives |
| `documentation/adr/0002-flat-file-over-database.md` | The flat-file decision, its eleven constituent commitments, honest negative consequences, and seven rejected alternatives including Postgres, SQLite and a hybrid |

### Out of scope

- **The work-item ("bean") on-disk schema.** Owned by a separate work package; referenced
  throughout as `documentation/90-work-items.md` and deliberately not specified here.
- **Build files, Gradle convention plugins, Kotlin source, CI configuration.** Owned by
  another agent. This package *names* the tools and rules those files must implement, so
  that work has a specification to build against.
- **The backoffice design system.** Referenced only as a constraint (beautiful; validated
  by Playwright).

## Success criteria

| # | Criterion | Status |
|---|---|---|
| 1 | All twelve files listed above exist under `documentation/`, numbered so reading order is unambiguous | met |
| 2 | Every MUST-level rule either names the tool that enforces it (`Enforced by:`) or states an explicit `Enforcement gap:` | met |
| 3 | The dependency rules are expressed as tables an ArchUnit test can be derived from directly, with no interpretation step | met |
| 4 | `core-domain`'s forbidden list is complete and each entry names a substitute | met |
| 5 | The custom Detekt rules to be written are enumerated, each with a rationale for why an off-the-shelf rule does not cover it | met |
| 6 | Evidence is defined as a closed set of kinds with a required record shape, size caps, and a strength ordering | met |
| 7 | Cost documentation contains the **actual current** Anthropic model IDs and pricing, sourced from the `claude-api` skill rather than written from memory | met |
| 8 | The 300k context budget is stated as a hard ceiling with concrete, orderable tactics — not an aspiration | met |
| 9 | The branch → work item → PR → review → merge workflow is stated with "no direct commits to `main`" and its enforcement | met |
| 10 | Both ADRs follow Status / Context / Decision / Consequences and each records the alternatives rejected | met |
| 11 | No build file, Kotlin source, or CI config is created | met |
| 12 | Each major file is substantial but readable — roughly 250–500 lines, not thousands | met |
| 13 | The work-item schema is referenced as separately owned and not specified | met |

## How they were validated

| # | Evidence | Kind |
|---|---|---|
| 1 | `ls documentation/ documentation/adr/` lists all twelve files; `wc -l documentation/*.md documentation/adr/*.md` reports line counts | `command` |
| 2 | `rg -c 'Enforced by:\|Enforcement gap:' documentation/` returns non-zero counts in `00`, `10`, `20`, `30`, `40`, `50`, `60`, `70` | `command` |
| 3 | `10-architecture.md` §4.1–§4.3 are three tables of (From, To, Rule) and (Rule, Detail) rows; `30-code-style.md` §5 maps each ArchUnit rule group to the table it derives from | `citation` |
| 4 | `20-ddd-practices.md` §8 is a three-column table — Forbidden / Why / Use instead — with 17 rows; cross-checked against `00-constitution.md` §1.3 and `10-architecture.md` §4.2 for consistency | `citation` |
| 5 | `30-code-style.md` §4 enumerates ten custom rules, each with a "why it exists" column stating the Modus-specific hazard | `citation` |
| 6 | `50-memory-and-evidence.md` §2.1 (six kinds), §2.2 (not-evidence), §2.3 (strength order), §3.1–§3.3 (record shape and caps) | `citation` |
| 7 | Pricing and model IDs were obtained by invoking the `claude-api` skill in this session (cached 2026-06-24) and transcribed verbatim into `60-cost-model.md` §2. The document explicitly marks the table a snapshot and requires a price-book entry to carry a `fetch` evidence record with a retrieval timestamp. Cache-pricing multipliers were **deliberately omitted** rather than written from memory. | `fetch` |
| 8 | `00-constitution.md` §6.1 is a ten-row ordered tactics table; §6.2 states the work-item sizing corollary; `80-agent-operating-procedure.md` places checkpoints at 100k and 200k with a hand-off protocol | `citation` |
| 9 | `00-constitution.md` §7.1–§7.4 | `citation` |
| 10 | Both ADRs carry the header block plus Context / Decision / Consequences (positive, negative, neutral) / Alternatives considered; `0001` rejects six alternatives, `0002` rejects seven | `citation` |
| 11 | `git status --short` on this branch shows additions only under `documentation/` and `beans/` | `command` |
| 12 | `wc -l documentation/*.md documentation/adr/*.md` reports 107–363 lines per file (README 107; the nine numbered files 233–363; ADRs 130 and 164); 3,458 lines total across the package | `command` |
| 13 | `grep -rn '90-work-items' documentation/` shows the reference in `README.md` (×2), `00-constitution.md`, `20-ddd-practices.md` and `40-durability.md`, each stating it is owned separately | `command` |

## Decisions taken while authoring

1. **`documentation/` states the present; ADRs record choices; memories record discovered
   facts.** These three are kept strictly distinct, and `adr/0001` argues why conflating
   memories and decisions would corrupt the invalidation semantics that make memories
   trustworthy.
2. **Every MUST-level rule is annotated with its enforcing tool, or with an explicit
   enforcement gap.** Gaps are named rather than hidden so they can be closed by the build
   work package. This is the mechanism that makes "style is enforced by tools, never by
   review comments" operational rather than aspirational.
3. **Ten custom Detekt rules are specified**, beyond the obvious `ForbiddenDomainApi`.
   Notably `NoFloatingPointMoney` (cost is the product; float money silently fails to
   reconcile), `DomainScopedRoute` (one un-scoped route is a cross-domain data leak), and
   `UnevidencedMemoryWrite` (a static complement to the runtime evidence gate).
4. **Detekt baselines and ArchUnit rule freezing are banned outright**, with a Gradle check
   that fails if a baseline file exists. A baseline converts a rule into permanent hidden
   debt, which is the opposite of mechanical enforcement.
5. **404-not-403 for cross-domain access** is specified as a hard rule with an integration
   suite, because a `403` leaks the existence of a domain an actor may not know about.
6. **`overhead` is a first-class cost stage.** Retries, abandoned branches and blown
   context budgets bill to the work item that caused them. Without this, a cheap model that
   needs three attempts looks cheap.
7. **`effectiveCostUsd` (mean cost × mean attempts to success), not headline price, is the
   model-selection metric**, and the standing defaults in `60-cost-model.md` §4.4 are
   explicitly labelled as starting points to be replaced by measurement.
8. **Cache-pricing multipliers were omitted.** The `claude-api` skill did not supply them,
   and the evidence rule forbids writing pricing from memory. The document says where to
   fetch them.
9. **An escalation order is specified above skills** (make it impossible → mechanise →
   record a fact → write a skill → document → ask a human), so that skills are not reached
   for where a type or a lint rule would be cheaper per use.
10. **The SOP's step 2 (restate the success criteria) is marked unskippable**, and is where
    work-item mis-sizing against the 300k budget is caught — before any spend.

## Follow-up work items to raise

- Implement `build-logic` convention plugins: `modus.kotlin-conventions`,
  `modus.spotless`, `modus.detekt` (including the ten custom rules),
  `modus.archunit`, `modus.test`.
- Implement the ArchUnit rule groups derived from `10-architecture.md` §4.
- Ratify and land `documentation/90-work-items.md`, then migrate this file to it.
- Add the CI commit-message check and `main` branch protection.
- Add the PR-body structural check that closes the evidence enforcement gap
  (`00-constitution.md` §3).
- Implement peak-context recording in the `execution` context to close the budget
  enforcement gap (`00-constitution.md` §6.2).
- Populate the initial price book with `fetch`-evidenced entries.
