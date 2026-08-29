---
# modus-0001
title: Foundation documentation package
status: completed
type: epic
created_at: 2026-08-28T00:00:00Z
---

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
| `documentation/adr/0002-flat-file-over-database.md` | The flat-file decision, its twelve constituent commitments, honest negative consequences, and seven rejected alternatives including Postgres, SQLite and a hybrid |

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
| 12 | `wc -l documentation/*.md documentation/adr/*.md` after review cycle 1 reports 107–419 lines per file (README 107; the nine numbered files **266–419**, all inside the 250–500 range README requires; ADRs 130 and 178); 3,709 lines total across the package. Before the cycle, `30-code-style.md` was 233 — under the range, which the review noted; the finding-4 and finding-8 fixes brought it inside. | `command` |
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

---

## Review cycle 1 — pull request #1

Eight inline threads were opened against the package, plus twelve lower-priority
observations in the review summary body. Every thread was resolved by a change to the
package; none was declined. The pricing figures and all nine model IDs in
`60-cost-model.md` §2 were independently verified against the `claude-api` skill and were
correct — they were not touched except to add the effort column and the lapse note.

The pattern behind six of the eight findings is the same one, and it is worth naming: a
rule was stated in more than one place, and the copies drifted. The fix in each case was
to make one statement normative and have the others cite it, rather than to correct both
copies and leave the drift mechanism in place.

### The eight findings and their fixes

| # | Where | Finding | Fix | Evidence |
|---|---|---|---|---|
| 1 | `60` §4.1 | The benchmark grid crossed `claude-haiku-4-5` with four effort levels, but Haiku 4.5 rejects `output_config.effort` with a `400`. Four cells that cannot run. | The price table in §2 gains a normative **effort column** per model; §4.1's grid is explicitly ragged and `module-cost` must enumerate cells from that column, enforced by the same validation rule that rejects an unknown `ModelId`. §4.4 and `70` §3.7 now cite it. | `60` §2 table, §4.1; `claude-api` skill: effort errors on Sonnet 4.5 and Haiku 4.5 |
| 2 | `10` §3.1 | §3.1 forbade exactly the imports §3's `Consumes` column, §6.1's flow and `60` §3.2's spend record require. An ArchUnit rule derived from the table was unsatisfiable. | Published-language split (below). §3.1 rewritten; every row now matches §3 exactly. | `10` §3.1, §4.2 `PublishedLanguageIsLeaf`; `30` §5 `ContextIsolationRules` |
| 3 | `10` §4.3 | The `ControllersAreDomainScoped` allowlist omitted `/domains`, which §5.1 declares required — so the rule failed the build for a required controller. Same gap in `DomainScopedRoute`, and a third looser phrasing in `00` §8. | §5.1 is now the **single normative copy**, named "the non-domain-scoped route allowlist", with `/domains` as an exact match. Both rules and `00` §8 cite it by name and carry no members. | `10` §5.1, §4.3; `30` §4; `00` §8 |
| 4 | `30` §5 | ArchUnit was claimed to enforce a `//` comment on `@Disabled`. Comments are discarded by the compiler; no ArchUnit rule can ever see one. Three documents told agents the build caught it. | Replaced with `DisabledCarriesWorkItem` (`30` §5.1): the reference is an **annotation value** matching `^beans/\d{4}`, which is retained in the class file. Kotest's `enabled = false` is forbidden because it has no annotation to carry the reference. `30` §7 and `80` step 6 updated. Added an explicit note on what Detekt can see that ArchUnit cannot. | `30` §4 (closing note), §5.1, §7; `80` step 6 |
| 5 | `20` §2.2 | The canonical aggregate example violated §2.1.4, `JustifiedVar` and §7.2 — the most-copied snippet in the package failed the build it teaches. | §2.1.4 clarified to "no mutable **public** API"; the example now carries a justified `private var state`, a `private val successCriteria`, throws `WorkItemTransitionNotPermittedException` instead of `require`, and lives in `..domain.aggregate`. | `20` §2.1.4, §2.2 |
| 6 | `00` §5 | Skill extraction fired on the second occurrence here and the third in `60`/`70`, and this file has precedence — a rule an agent cannot obey. | Threshold is **three**, everywhere. `60` §5.3 is the single normative trigger table and absorbed the two triggers that existed only in `70`; `00` §5 states the principle and `70` §2.1 keeps the rationale, neither restating a number. | `00` §5; `60` §5.3; `70` §2.1, §2.2 |
| 7 | `40` §2.2.5, §4.2 | The `PIPE_BUF` guarantee was wrong: it bounds pipe writes, not regular-file writes; it is 512 on macOS, this project's dev platform; and no JVM API can promise one `write(2)` per record. Load-bearing for §5 and `adr/0002`. | Underlying design replaced (below), not just the sentence. `adr/0002` gains Decision §5 stating append integrity honestly. | `40` §2.2.5–2.2.7, §4.2, §5, §6.1, §7; `adr/0002` §5 |
| 8 | `20` §8 | "Each is enforced by ArchUnit, by `ForbiddenDomainApi`, or by both" was false for three rows, plus an undecidable `*Service` allowlist and an unscopeable coverage claim. | §8 split into §8.1 (per-row `Enforced by`) and §8.2 (`Enforcement gap: review only`). Mutable singleton state gets a real tool — the new Detekt rule `NoMutableSingletonState`. `*Service` banned outright. §7.3's coverage floor rescoped to the `..domain.aggregate` package, which Jacoco can actually target. | `20` §8.1, §8.2, §5.1, §7.3; `30` §4 |

### Design decision — finding 2: the dependency direction

**Domain events as a published language, not a direct dependency.** Each context is split
into a **published language** — `..domain.event` (its events) and `..domain.published`
(its identifiers, plus any value object appearing in an event signature) — and its
**internals**, which nobody may import, with no allowlist and no exception. A context may
import another context's published packages only where §3.1's table says so, and those
rows now match §3's `Consumes` column exactly.

Two properties make this expressible as ArchUnit rules rather than as prose.
`PublishedLanguageIsLeaf`: a published package may reference only the Kotlin stdlib,
`java.time` types, and its own context's published package. Because published packages are
leaves, `memory` and `execution` importing each other's events is a cycle at *context*
granularity but not in the *package* graph — so the no-cycles rule is stated over the
internals slices and both facts stay true at once. The split has a useful side effect:
putting a type into an event's signature publishes it, which is a visible, reviewable act
and, per §4.1.5, a breaking contract change requiring an ADR.

The alternative considered and rejected was an anti-corruption port per consumer. It
avoids the context-level cycle entirely, but adds a translation layer per edge for no
benefit at this scale and contradicts §4.1.6's "events are the cross-context coupling
mechanism".

### Design decision — finding 7: the durability mechanism

**Detection, not prevention — three separable mechanisms, each claiming only what it
actually provides.** `O_APPEND` guarantees that offset selection and the write are one
atomic step against other appenders, so an append never *overwrites* another; it does not
promise the write is one syscall. A per-log **appender lock**, held across the whole record
with a retry loop on short writes, means Modus's own writers never interleave at any size —
this replaces the old size threshold entirely, so a record's length no longer changes which
code path runs. A per-record **CRC-32C** (`crc`, last key, computed over the canonical
serialisation with `crc` omitted) makes any record that did tear **detectable on read**: it
is skipped, counted, and the log marked `degraded`, never repaired and never silently
dropped.

The reader checks every line, not only the last, because a fragment of one record can be
followed by a complete later record and then the fragment's tail. Truncation repair is
narrowed to the final line only. Durability itself comes from `fsync` (§5) and never from
write size; the resume-cursor contract now rests on that alone, so a record skipped for a
bad CRC is always one the client was never promised. Linux's inode-lock serialisation of a
single `write()` is noted as a non-portable implementation detail we deliberately do not
rely on, because macOS is a supported development platform.

### Also actioned, from the review summary body

| Item | Fix |
|---|---|
| The gate specified three ways (`00` §7.2.4, `30` §6, `80` step 6) | `00` §7.2.4 is the single normative gate — `spotlessApply`, `check` (backoffice included), `e2eTest` only when user-visible behaviour changed. Playwright is deliberately outside `check`, with the reason stated. The other two cite it and carry no commands. |
| `modules/*` dependency rules disagreed between `00` §1.1 and `10` §4.1/§7.2 | "adapter ports" deleted — ports live in `core` (§1.2), so there is nothing of that name to depend on. `00` §1.1 now matches `10` §4.1 (Spring allowed, no `adapters/*`), and §4.1 is declared the machine-readable form that wins on disagreement. |
| Two divergent normative copies of the extraction triggers | Merged into `60` §5.3 (see finding 6). The two non-measurable triggers are kept there and marked as raised by observation, so the trigger set has one home. |
| `superseded` with no `supersededBy` target | New `expired` status. The "work item closed → 30 days" trigger now yields `expired`; `superseded` always carries `supersededBy`, enforced by schema validation. |
| Work items had two homes (`beans/` vs `domains/<id>/work/`) | One concept, one schema. `40` §3.1 states that `beans/` **is** the `modus` domain's work store at a shorter path, so self-hosting (`00` §12) needs no migration. |
| `50` §4.1 gate 5 presented as enforced | Hedged like gates 1/2/4, and identified as the weakest of the five — semantic contradiction between free-text assertions is not decidable. `Enforcement gap:` added. |
| `80` §9.6 vs `60` §6.6 attribution | Both amended to say the same thing: performing review bills to `review`; responding to it bills to `revise`. |
| Thin coverage of **triggers** | New `10` §6.3: the `Trigger` shape, per-domain storage, and six rules covering the cases that actually bite — at most one in-flight run per target with coalescing recorded, refusals always recorded, idempotence per `(triggerId, causeId)`, no self-triggering, and disable-does-not-cancel. |
| Thin coverage of **backoffice grant administration** | New `10` §5.5: grants administered under `/domains/{domainId}/grants` and therefore 404-scoped like everything else; no wildcard or global-admin grant; bootstrap defined as the only path to a first grant; revocation's cross-context consequence named. The backoffice screens remain an explicit enforcement gap. |
| `Enforcement gap:` lines not naming a work item | The three gaps in `00` (§3, §4, §6.2) now name `beans/0001` — this file — which owns raising them under "Follow-up work items to raise". |
| Sonnet 5 introductory pricing lapses 2026-08-31 | `60` §2 carries a dated block: from 2026-09-01 the rate is the standard $3.00/$15.00; the intro figures are historical and retained only so pre-lapse spend stays computable; any projection quoting them for a later run is wrong. §4.4's Sonnet 5 review row is flagged for re-check. |

Not actioned, deliberately: the per-domain price-book design question (a genuine design
debate, not a defect — it deserves an ADR rather than a patch); `30` §9.3's
repo-wide-fix rule versus the 300k budget; and the three predicted dead letters in `50`
§3.3 and `30` §2. Each was flagged by the reviewer as take-or-leave, and none is a false
statement in the package.

### Follow-up work items this cycle adds

- Implement the `NoMutableSingletonState` custom Detekt rule (`30` §4) — the eleventh.
- Implement `DisabledCarriesWorkItem` as an ArchUnit rule reading the `@Disabled`
  annotation value (`30` §5.1).
- Implement the three ArchUnit rules the published-language split requires:
  `ContextInternalsAreSealed`, `PublishedLanguageAllowlist`, `PublishedLanguageIsLeaf`.
- Add the `module-cost` validation rejecting a `(modelId, effort)` pair the price book's
  effort column does not list (`60` §4.1).
- Narrower rules for the two `20` §8.2 gaps: forbid a `Boolean?` property on a type in
  `..domain.aggregate`, and raise `TooGenericExceptionThrown` to `error`.
- Specify the backoffice grant-administration screens and their Playwright assertions
  (`10` §5.5).
- Specify per-domain trigger configuration in the backoffice (`10` §6.3).
