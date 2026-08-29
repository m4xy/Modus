---
id: doc:60-cost-model
title: Cost model
status: active
superseded_by: null
read_when:
  - path: modules/module-cost/**
  - path: adapters/adapter-agent-claude/**
  - task: cost|spend|price|budget|token|effort|benchmark|(pick|choose|select).{0,20}model|opus|sonnet|haiku|skill
provides:
  - doc:60-cost-model#price-book
  - doc:60-cost-model#stage-attribution
  - doc:60-cost-model#spend-record
  - doc:60-cost-model#model-selection
  - doc:60-cost-model#extraction-threshold
  - doc:60-cost-model#cost-in-the-ui
  - doc:60-cost-model#budgets
depends_on: [doc:00-constitution, doc:10-architecture, doc:20-ddd-practices, doc:30-code-style, doc:40-durability, doc:50-memory-and-evidence, doc:70-skills, doc:80-agent-operating-procedure]
---

# 60 — Cost Model

Modus knows what every stage of every workflow costs, in dollars, and it acts on that
knowledge. Cost is not a report produced after the fact; it is an input to every routing
decision the system makes.

Owning context: `cost` (`10-architecture.md` §3). Owning module: `modules/module-cost`.

---

## 1. Principles

| # | Principle |
|---|---|
| 1.1 | **Every stage carries a cost.** No agent action happens without a spend record attributed to a work item, a stage, a model, and an effort level. |
| 1.2 | **Model and effort are decisions, not defaults.** Every run records why its model and effort were chosen. "It was the default" is not a reason. |
| 1.3 | **Measure before optimising.** A cost claim is an assertion and needs evidence (`50-memory-and-evidence.md`). Benchmark the task; do not assume a cheaper model is cheaper end-to-end — retries and failed validations are the dominant term. |
| 1.4 | **Repetition is the signal.** A task done three times is a skill (`70-skills.md`). Skills convert variable agent spend into near-fixed, often near-zero, deterministic spend. |
| 1.5 | **Context is cost.** Input tokens are billed on every turn of an agentic loop. Holding the 300k budget (`00-constitution.md` §6) is the highest-leverage cost control that exists, because a bloated context is re-billed on every single turn. |
| 1.6 | **Cost is visible in the UI.** A human orchestrating from afar must see spend at every level without asking. |
| 1.7 | **Never silently downgrade.** Choosing a cheaper model to save money on someone's behalf, without recording it, is forbidden. Record the choice and the rationale. |

---

## 2. The model line-up and price book <a id="price-book"></a>

Sourced from the `claude-api` skill (cached 2026-06-24). **This table is a snapshot, not
the source of truth.** Prices per **million tokens**, Anthropic first-party API rates.

| Model | Model ID | Context | Input $/1M | Output $/1M | `output_config.effort` |
|---|---|---|---|---|---|
| Claude Fable 5 | `claude-fable-5` | 1M | $10.00 | $50.00 | `low` `medium` `high` `xhigh` `max` |
| Claude Mythos 5 *(Project Glasswing only)* | `claude-mythos-5` | 1M | $10.00 | $50.00 | `low` `medium` `high` `xhigh` `max` |
| Claude Opus 5 | `claude-opus-5` | 1M | $5.00 | $25.00 | `low` `medium` `high` `xhigh` `max` |
| Claude Opus 4.8 | `claude-opus-4-8` | 1M | $5.00 | $25.00 | `low` `medium` `high` `xhigh` `max` |
| Claude Opus 4.7 | `claude-opus-4-7` | 1M | $5.00 | $25.00 | `low` `medium` `high` `xhigh` `max` |
| Claude Opus 4.6 | `claude-opus-4-6` | 1M | $5.00 | $25.00 | `low` `medium` `high` `max` — **no `xhigh`** |
| Claude Sonnet 5 | `claude-sonnet-5` | 1M | $3.00 (intro $2.00 **through 2026-08-31**) | $15.00 (intro $10.00) | `low` `medium` `high` `xhigh` `max` |
| Claude Sonnet 4.6 | `claude-sonnet-4-6` | 1M | $3.00 | $15.00 | `low` `medium` `high` `max` — **no `xhigh`** |
| Claude Haiku 4.5 | `claude-haiku-4-5` | 200K | $1.00 | $5.00 | **none — the parameter is rejected (`400`)** |

> **Sonnet 5 introductory pricing lapses after 2026-08-31.** On and after 2026-09-01 the
> rate is the standard **$3.00 / $15.00** shown above — a 50% increase on both input and
> output. If you are reading this on or after that date, the intro figures in the
> parenthetical are **historical**: they are retained because a spend record computed
> before the lapse must stay computable, not because they are current. §2.1 models this as
> a price-book entry with an `effectiveTo`, so no code changes when it lapses; what does
> need to happen is a `fetch`-evidenced entry for the standard rate, which is why
> "populate the initial price book" is a live follow-up in `bean:0001`. Any spend
> projection quoting $2.00/$10.00 for a run after the lapse is wrong.

The **effort column is normative** and is the only statement of effort support in this
package; §4.1, §4.4 and `70-skills.md` §3.7 all derive from it. Effort support is
**per-model**, so the model × effort space is not rectangular — see §4.1.

Notes that materially affect Modus's spend:

- **Batch API: 50% of standard rates**, asynchronous. Anything not latency-sensitive
  (nightly re-validation, bulk classification, cost-profile benchmarking, corpus
  re-indexing) belongs on Batch. This is the single largest structural saving available.
- **Prompt caching** changes the input-token economics substantially for repeated
  prefixes — which describes every agentic loop. Cache write and cache read are billed at
  multipliers of the base input rate; those multipliers are **not** reproduced here
  because they must not be written from memory. Fetch them from the `claude-api` skill or
  the Anthropic pricing page when populating the price book.
- **Fast mode** (research preview; Opus 5 and Opus 4.8 only, Claude API only) is priced
  at **$10.00 / $50.00 per 1M** for Opus 5 — 2× the standard Opus rate for up to 2.5×
  output throughput. Use it only where a human is actively waiting.
- **Priority Tier does not cover Claude Opus 5**, Sonnet 5, or Mythos 5.
- **Partner platforms** (Amazon Bedrock, Google Vertex AI) are separately priced. Claude
  on Microsoft Foundry bills at the standard API rates above.
- **Effort levels** (`output_config.effort`) do not change the per-token price. They change
  *how many tokens are spent*, and the effect is large. Effort is the primary cost dial on
  a fixed model. Which levels a model accepts is in the table above; sending an
  unsupported one is a `400`, not a silent downgrade.

### 2.1 Keeping the price book current

- The authoritative price book lives at `domains/<domainId>/cost/price-book.md`, a
  document in the ordinary flat-file store, versioned in git.
- Each entry carries `effectiveFrom`, `effectiveTo?`, and an **evidence record of kind
  `fetch`** citing the Anthropic pricing page with a retrieval timestamp and content
  hash. A price with no evidence is not loaded.
- Historical spend is **always** computed against the price book entry in force at the
  time of the run. A price change never retroactively rewrites past spend; it is a new
  entry, and past records keep their `priceBookEntryId`.
- Intro pricing (Sonnet 5 through 2026-08-31) is modelled as an entry with an
  `effectiveTo`, not as a discount flag. When it lapses, spend projections change on
  their own with no code change.
- A `fetch` evidence record older than 90 days makes its price entry `stale`
  (`50-memory-and-evidence.md` §6.2), which raises an operator action in the backoffice.
  Modus does not silently bill against a year-old price.
- Model IDs are exact strings and are **never** constructed. No date suffixes, ever.
  **Enforcement gap:** the validation rule in `module-cost` that would reject an unknown
  `ModelId` does not exist; `module-cost` is an empty placeholder module with no tests
  today (`bean:0016`).

---

## 3. Stage-level cost attribution <a id="stage-attribution"></a>

### 3.1 The stages

Every unit of agent work is attributed to exactly one stage. The stage set is fixed so
that costs are comparable across work items and across domains.

| Stage | Covers | Typical driver |
|---|---|---|
| `triage` | Classifying an incoming request, sizing it, routing it | Small input, small output |
| `plan` | Restating success criteria, decomposing, checking the budget | Moderate input (memories + docs) |
| `investigate` | Searching, reading, running things to find out | **Input-dominated** — the top cost line in most work items |
| `implement` | Writing code or documents | Output-dominated |
| `validate` | Running tests, gathering evidence, self-review | Cheap per call, many calls |
| `review` | Reviewing a diff | Input-dominated (the diff plus context) |
| `revise` | Responding to review | Mixed |
| `document` | Encoding learnings back (README encoding rule) | Small |
| `overhead` | Retries, failed runs, abandoned branches, context-budget blowouts | **Must be visible.** Hiding this hides the real cost of a bad approach. |

`overhead` is a first-class stage on purpose. A cheap model that needs three attempts is
not cheap, and the only way to see that is to bill the failures to the same work item.

### 3.2 The spend record <a id="spend-record"></a>

Appended to `domains/<domainId>/cost/NNNN.ndjson` — an append-only log
(`40-durability.md` §2.2), fsynced per record because it is money.

| Field | Notes |
|---|---|
| `at` | ISO-8601 UTC |
| `domainId`, `workItemId`, `epicId?` | Attribution chain |
| `runId` | The agent run |
| `stage` | From §3.1 |
| `modelId` | Exact model ID string |
| `effort` | `low` \| `medium` \| `high` \| `xhigh` \| `max` |
| `speed` | `standard` \| `fast` |
| `channel` | `interactive` \| `batch` |
| `inputTokens`, `outputTokens` | From `response.usage` |
| `cacheWriteTokens`, `cacheReadTokens` | From `response.usage`; billed at their own rates |
| `costUsd` | `Usd` value object — **integer micros, never a float** (`20-ddd-practices.md` §3) |
| `priceBookEntryId` | Which price computed this |
| `skillId?` | Set when the work was done under a skill (§5) |
| `outcome` | `succeeded` \| `failed` \| `abandoned` \| `retried` |
| `peakContextTokens` | For budget accounting (`00-constitution.md` §6) |
| `rationale` | Why this model/effort was chosen. Required, non-empty. |

Token counts come from the API response's `usage` block, captured by
`adapters/adapter-agent-claude` — never estimated. Where a pre-flight estimate is needed
(a projection before running), use the token-counting endpoint, and label the figure
`estimated`. **An estimate is never written into a spend record.**

**Enforcement gap:** none of the three exist — the `NoFloatingPointMoney` custom Detekt
rule (`30-code-style.md` §4, `bean:0026`), schema validation on a spend record, and an
ArchUnit rule restricting spend-record construction to `module-cost` (`bean:0016`, an
empty placeholder module with no tests today).

### 3.3 Rollups

Sums are computed by folding the spend log, never stored as authoritative totals — a
stored total drifts from its inputs (`40-durability.md` §9). Rollups available at:
run → stage → work item → epic → domain → model → skill → time bucket.

---

## 4. Recording cost under different models and effort settings

Modus does not guess which model to use. It **measures**, and stores the measurement as a
`CostProfile`.

### 4.1 The benchmark procedure

For a task category (§5.1), run a representative sample — at least 5 instances, ideally
20 — across a grid, and record every cell.

**The grid is ragged, not a cartesian product.** Effort support is per-model (§2), so
`module-cost` MUST enumerate cells from the price book's effort column rather than
crossing a model list with an effort list. A blind cross product produces cells that
return `400` before they produce a measurement:

```
claude-haiku-4-5:  (one cell, no effort parameter — the model rejects it with a 400)
claude-sonnet-5:   low, medium, high, xhigh
claude-opus-5:     low, medium, high, xhigh
```

`max` is available on both Sonnet 5 and Opus 5 but is deliberately left out of the
standard sweep: it is the "correctness matters more than cost" setting, so it is measured
only for a category where `xhigh` failed to clear the success threshold. Add it as an
extra row there rather than paying for it on every profile.

**Enforcement gap:** the same `module-cost` validation rule §2.1 names, rejecting a
`(modelId, effort)` pair the price book's effort column does not list, does not exist
(`bean:0016`). A benchmark that cannot be constructed is better than one that fails on its
first cell.

For every cell record:

| Metric | Why it matters |
|---|---|
| `costUsd` (mean, p50, p90) | The obvious one, and the least informative alone |
| `successRate` | Fraction meeting the task's success criteria **on the first attempt** |
| `attemptsToSuccess` (mean) | Retries are the hidden cost |
| `effectiveCostUsd` | `meanCost × meanAttemptsToSuccess` — **the number that decides** |
| `wallClockSeconds` (p50, p90) | Matters only when a human is waiting |
| `peakContextTokens` | A model that blows the budget is disqualified regardless of price |
| `escapedDefectRate` | Defects that passed validation and were caught later. Expensive; weight heavily. |

Benchmarks run on the **Batch API** (50% cost) because they are not latency-sensitive.
Benchmarking cost is itself recorded, against the skill or category being profiled.

### 4.2 Selection rule <a id="model-selection"></a>

> Choose the **cheapest cell whose `successRate` clears the category's threshold and whose
> `escapedDefectRate` is within tolerance**, ranked by `effectiveCostUsd`.

Never rank by headline price. A 5× cheaper model that fails half the time and lets
defects escape is more expensive than the alternative, and it also burns human review
attention — the scarcest resource in the system.

### 4.3 Storing the profile

`domains/<domainId>/cost/profiles/<categoryOrSkillId>.md` — a document with the grid in
frontmatter, `measuredAt`, `sampleSize`, the repo sha, and evidence records
(`test-run` and `command` kinds) for the benchmark runs. A profile older than 90 days, or
whose model set no longer matches the price book, goes `stale` and is re-benchmarked.

### 4.4 Standing defaults, pending measurement

Until a category has a measured profile, these are the defaults. They are starting points
to be replaced by evidence, not conclusions.

| Work | Model | Effort | Why |
|---|---|---|---|
| Long-horizon agentic implementation | `claude-opus-5` | `xhigh` | `xhigh` is the recommended setting for coding and agentic work on the Opus 5 generation |
| Ordinary implementation and revision | `claude-opus-5` | `high` | The quality/token sweet spot |
| Investigation subagents, fan-out search | `claude-opus-5` | `low` | Low effort means fewer, more consolidated tool calls; the parent keeps the judgement |
| Mechanical transforms, classification, extraction | `claude-haiku-4-5` | **n/a — the model rejects `effort` (§2)** | 200K context is ample; 1/5 the input price of Opus |
| Bulk re-validation, nightly sweeps, benchmarking | any, via **Batch** | `medium` | 50% rate, latency irrelevant |
| Code review — mechanical classes of defect | `claude-sonnet-5` | `medium` | See §6. Re-check this row once Sonnet 5's introductory rate lapses on 2026-08-31 (§2): it was chosen against $2.00/$10.00, and the standard rate is 50% higher. |
| Code review — design and correctness | `claude-opus-5` | `high` | See §6 |
| A human is actively waiting on output | `claude-opus-5` + fast mode | `high` | 2× price for up to 2.5× throughput; only when the wait is real |

**Never downgrade a model to save money without recording the decision** (§1.7). The
`rationale` field exists for exactly this.

---

## 5. Task categorisation → skill extraction

The pipeline that turns expensive, repeated, variable agent work into cheap, defined,
deterministic work. This is where the real money is.

### 5.1 Categorise

Every work item is assigned a **task category** at `triage` — a stable label describing
the shape of the work, not its subject: `add-rest-endpoint`, `fix-flaky-test`,
`upgrade-dependency`, `add-archunit-rule`, `write-adr`, `add-playwright-flow`,
`investigate-production-error`.

Categories come from a per-domain, extensible list (every domain has its own way of
working — `00-constitution.md` §8). Assignment is recorded on the work item and on every
spend record derived from it.

### 5.2 Detect repetition

`module-cost` continuously reports per category:

- Instance count, and rate over the trailing 90 days
- Mean and total `effectiveCostUsd`
- Variance in cost across instances — **high variance means the task is not yet
  understood**, and understanding it is worth more than optimising it
- Fraction of spend in the `investigate` stage — high fraction means agents keep
  rediscovering the same thing, which is a **memory** gap, not a skill gap

### 5.3 The extraction trigger <a id="extraction-threshold"></a>

**This table is the single normative statement of the extraction thresholds.**
`00-constitution.md` §5 states the principle and `70-skills.md` §2.1 gives the rationale
per trigger; neither restates a number. Two copies of a threshold table diverge — these
two already had.

A category becomes a skill-extraction candidate when **any** holds:

| Trigger | Threshold | Raised by |
|---|---|---|
| Repetition | ≥ **3** instances in 90 days. Not two: a task done twice may never recur, and a skill for a task that does not recur is maintenance cost with no payback. | `module-cost` |
| Aggregate spend | Total category spend ≥ 20× the estimated cost of writing the skill | `module-cost` |
| Variance | p90 cost ≥ 3× p50 cost (unpredictable work is expensive work) | `module-cost` |
| Rediscovery | ≥ 40% of category spend in the `investigate` stage | `module-cost` |
| Escaped defects | ≥ 1 escaped defect traceable to inconsistent execution of the category | `module-cost` |
| Inconsistency | Two agents did the same task materially differently | A human or an agent — **not measurable**; divergence has no numeric threshold |
| Human repetition | A human explained the same thing twice | A human — **not measurable** |

The last two rows have no threshold `module-cost` can compute and are raised by
observation. They are listed here anyway so that the trigger set has one home; a trigger
kept somewhere else because it is not automatable is a trigger that gets forgotten.

**Enforcement gap:** `module-cost` would raise a `skill-extraction-candidate` action into
the domain's action list (§7) for the five measurable rows; the module is an empty
placeholder with no tests today (`bean:0016`). It would not create the skill either way —
a human or an agent decides. The last two rows are review-and-judgement only, by
construction, and stay ungated regardless.

### 5.4 Extract

Follow `70-skills.md`. The cost-relevant parts:

1. A skill turns judgement into procedure. The cheapest skill is one whose steps are a
   deterministic script with **zero** model calls — always ask whether the model is needed
   at each step.
2. Every skill carries a **cost profile** (§4.3) and states the cheapest model and effort
   at which it reliably meets its success criteria.
3. Every skill carries a **validation command** whose exit code is the success signal.
   Deterministic validation is what makes a cheaper model safe: a weaker model plus a
   strong check beats a stronger model plus a weak check, and costs less.
4. The skill's cost profile is re-measured whenever the skill changes materially or the
   price book changes.

### 5.5 Verify the saving

After extraction, compare the category's `effectiveCostUsd` before and after, over an
equal number of instances. Record the result as an **evidence-backed memory** at domain
scope. If the skill did not reduce effective cost, say so and either fix it or retire it.
An unmeasured "optimisation" is an unevidenced assertion (`00-constitution.md` §3).

---

## 6. Cost-conscious code review

Review is a recurring, high-volume, input-dominated cost. It gets the same treatment as
everything else.

| # | Rule |
|---|---|
| 6.1 | **Style is never reviewed by a model.** ktlint, Detekt and ArchUnit catch it for free (`30-code-style.md`). Paying an LLM to notice formatting is pure waste, and it also trains contributors to argue about it. |
| 6.2 | Review is **tiered by defect class**. Mechanical classes (missing test, missing evidence, forbidden import, unhandled error path, missing null case) run at `claude-sonnet-5` / `medium`. Design and correctness review — invariant placement, aggregate boundaries, concurrency, security — runs at `claude-opus-5` / `high`. |
| 6.3 | Review input is **scoped to the diff plus its immediate context**, never the whole repository. `git diff --stat` first, then the hunks, then only the files a hunk actually depends on. |
| 6.4 | Reviewing an unchanged file is billed as `overhead` and is a bug in the review skill. |
| 6.5 | The review prompt prefix is **cached**. It is stable by construction: put the checklist and rules in the prefix, the diff last (`shared prefix → volatile suffix`). Verify cache hits via `cache_read_input_tokens`; a persistently zero value means something is invalidating the prefix. |
| 6.6 | Review cost is attributed to the `review` stage of the work item under review, so the true cost of a change includes the cost of checking it. The author's cost of *responding* to review is a different stage, `revise` (§3.1, `80-agent-operating-procedure.md` §9.6) — one round trip, two stages, because "what did checking this cost?" and "what did fixing it cost?" are different questions. |
| 6.7 | A defect class caught twice by review becomes a **tool rule** (Detekt/ArchUnit) rather than a review instruction. Review that repeatedly catches the same thing is a permanent tax; a lint rule is a one-off payment. |
| 6.8 | Review runs on the **Batch API** when nobody is waiting — for example on a draft PR overnight. |

---

## 7. Cost in the UI <a id="cost-in-the-ui"></a>

The backoffice makes spend impossible to miss and impossible to misread. Every surface
below is asserted by a Playwright test in `e2e/`.

### 7.1 Where cost appears

| Surface | Shows |
|---|---|
| Domain dashboard | Spend this period, trend against the previous period, budget consumed, top 5 categories by spend, open cost actions |
| Epic view | Rolled-up spend, cost per closed work item, forecast to completion |
| Work-item view | Cost by stage as a bar; model and effort used per stage; `overhead` broken out separately and coloured as waste |
| Run view (live) | Tokens and dollars accumulating in real time on the same stream as the output; peak context against the 300k budget |
| Category view | Instance count, mean/p50/p90 effective cost, variance, and the skill-extraction candidacy state |
| Skill view | The cost profile grid, the recommended model/effort cell, measured saving since extraction, staleness of the profile |
| Price book | Current entries, `effectiveFrom`/`effectiveTo`, the `fetch` evidence behind each, staleness warnings |
| Action list | Cost-driven actions, ranked by expected saving (§7.3) |

### 7.2 Display rules

- **Always show the currency and the precision.** Sub-cent figures show as `$0.0043`, not
  `$0.00`. Rounding to zero teaches people that agent work is free.
- **Always pair cost with outcome.** A cost figure without a success rate beside it is
  misleading and is forbidden by the design review.
- **`overhead` is always visible**, never folded into a total. It is the most actionable
  number on the page.
- **Estimates are visually distinct from actuals** — a distinct style plus an explicit
  "estimated" label. Never render an estimate as if it were measured.
- **Every figure is drillable** to the spend records that compose it, in one click.
  Cost is evidence-backed like everything else (`50-memory-and-evidence.md`).
- Colour alone never encodes meaning (accessibility; `30-code-style.md` §6).

### 7.3 The action list

The point of all this, per the product brief: the human orchestrating from afar sees a
ranked list of actions. Cost contributes these action kinds:

| Action | Raised when |
|---|---|
| `extract-skill` | A category crosses a §5.3 trigger. Shows estimated annual saving. |
| `retune-model` | A profile shows a cheaper cell now clears the success threshold |
| `refresh-price-book` | A price entry's `fetch` evidence is stale |
| `rebenchmark-profile` | A profile is > 90 days old, or the price book changed underneath it |
| `investigate-overhead` | A work item's `overhead` share exceeds 25% |
| `add-lint-rule` | A defect class was caught twice in review (§6.7) |
| `split-work-item` | A run's peak context exceeded 240k (80% of budget) |
| `move-to-batch` | A recurring non-latency-sensitive workload is running interactively |
| `budget-threshold` | A domain crosses a spend threshold it defined |

Each action carries an estimated saving and the evidence behind the estimate. Actions are
ranked by expected saving per unit of effort, so the top of the list is always the most
valuable thing the operator could do next.

---

## 8. Budgets <a id="budgets"></a>

- A domain may declare a spend budget per period. Budgets are **advisory by default and
  enforcing by configuration** — a domain may choose to have runs refused past its cap.
- Crossing a threshold (50%, 80%, 100%) emits `BudgetThresholdCrossed`, which raises an
  action and, if configured, a webhook.
- A **per-run** ceiling is always set, whatever the domain budget. A runaway agent loop is
  the most expensive single failure mode in the system, and an unbounded run is never
  acceptable.
- Budget enforcement is checked **before** a run starts and again at each budget
  checkpoint during the run.
