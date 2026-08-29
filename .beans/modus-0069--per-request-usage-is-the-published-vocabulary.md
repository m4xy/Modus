---
# modus-0069
title: Correct the agent-run usage vocabulary before bean:0014 publishes it
status: in-progress
type: fix
priority: high
created_at: 2026-08-29T00:00:00Z
---

# Correct the agent-run usage vocabulary before `bean:0014` publishes it

Why now: `bean:0014` publishes the execution context's language, and
`doc:10-architecture#bounded-contexts` makes published language expensive to change — an ADR
plus a migration. It is currently specified to publish a **wrong** usage model, and the wrong
model is written in three places that cite **each other** rather than any observation:

| where | what it says | its authority |
|---|---|---|
| `backoffice/src/agent/transport.ts` | rule 2 of the seam: "usage is reported as a running cumulative total"; `Usage` is `tokensIn`/`tokensOut`/`costUsd` | none stated |
| `bean:0020` | "Usage reported as a running cumulative total, so a dropped event costs one frame's accuracy" | "the rule `backoffice/src/agent/transport.ts` already states" |
| `bean:0014` | "Token counts are a value object, never a raw `Long`" — silent on shape | none |

A circle of two citations and one silence. `bean:0014` being silent is how the wrong model
gets published by default: the runner implements what the console already assumed.

`doc:00-constitution#evidence-rule` says an assertion needs an observation. The observation
exists and has since `bean:0054`: `tools/cost_lib.py` over this repository's own transcripts,
replayed into `domains/modus/cost/replay/baseline.md` and re-checkable with
`python3 tools/cost-replay.py --check`. It says something different in every particular.

## What the corpus says

Measured with `cost_lib` over the 78 transcripts present on 2026-08-29 — 2 root sessions and
76 subagent runs, a superset of the 65 runs the committed baseline was generated from. Figures
are evidence of a point in time, not constants; nothing below is copied into merged code
(`bean:0059`).

| claim | observed |
|---|---|
| usage is **per-request**, not cumulative | `outputTokens` **falls** from one message to the next on 2,617 of 5,656 messages. A cumulative counter never decreases. |
| frames of one message must be **deduplicated** on `message.id` | frame multiplicity 1.84x — 10,381 frames for 5,656 messages. Summing them multiplies the bill. |
| the frame to keep is the one with the **largest `outputTokens`** | 0 disagreements between frames of one message on the other four kinds; the committed baseline reports 1,129,285 output tokens (47.91% of all output) recovered by taking the largest frame rather than the first. |
| **cache tokens are the bill** | cache reads are 97.51% of all tokens (98.05% in the committed baseline). Fresh input is 11,258 tokens of 861,927,115 — 0.00%. Input plus output together are 0.32%. A model that prices only those two prices almost nothing. |
| **peak context is observable** | `cost_lib.peak_context_tokens` = `max(inputTokens + cacheReadTokens + cacheWrite5mTokens + cacheWrite1hTokens)`; largest single request in the corpus 865,375 tokens, 2.9x `doc:00-constitution#context-budget`'s 300k ceiling. It is computable **because** usage is per-request and carries the cache kinds, and is not computable from a cumulative two-field counter. |
| **no run records its own end** | no line type in a stored session, and no field of any of the 76 `*.meta.json` sidecars, carries an exit code, a status or an end reason. The 27 `<synthetic>` placeholder messages that do exist are session limits and API errors, and carry zero usage. There is nothing terminal for a consumer to wait for; `session-end{reason:'cancelled'}` is consumer-synthesised **by definition**. |

Command: `python3 - <<'PY'` over `cost_lib.read_messages`, `frame_disagreements` and
`peak_context_tokens` — the same functions `tools/cost-replay.py` calls, so `--check` against
`domains/modus/cost/replay/baseline.md` is the repeatable form of the same measurement.

## The correction

Five token kinds, `cost_lib.USAGE_KINDS` **verbatim** — `inputTokens`, `outputTokens`,
`cacheReadTokens`, `cacheWrite5mTokens`, `cacheWrite1hTokens`. The 5m and 1h halves stay split
because they are billed at different multipliers (1.25x and 2x of base input; cache reads
0.1x), so folding them mis-prices the dominant term. Two names for one concept across the
Python and TypeScript halves would be a new defect, so there are none: `zeroUsage`,
`addUsage`, `costMicros`, `ratesUpm`, `BASE_RATES_UPM`, `peakContextTokens` are `cost_lib`'s
names.

Money is integer micro-dollars end to end (`doc:20-ddd-practices#value-objects` §3). The
dollar `costUsd: number` that `transport.ts` carried was the floating-point money that rule
forbids; the division by 1,000,000 now happens once, at the render boundary.

Each of the three artefacts states this and cites the corpus. None cites another of the three.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | `transport.ts` states per-request usage, `messageId` deduplication keeping max `outputTokens`, five token kinds, and a consumer-synthesised terminal state — each citing the corpus, not the beans | `citation`: `backoffice/src/agent/transport.ts` rules 1–4 and the "Evidence" block, which names `domains/modus/cost/replay/baseline.md` and `tools/cost_lib.py` and no bean |
| 2 | `bean:0020`'s cumulative criterion is replaced, and its "peak context recorded per run" criterion is made satisfiable rather than claimed closed | `diff`: `.beans/modus-0020--claude-code-runner.md`; the criterion now names the `cost_lib.peak_context_tokens` formula and the per-request usage it needs |
| 3 | `bean:0014` is no longer silent: it states the published usage shape it must not get wrong | `diff`: `.beans/modus-0014--execution-bounded-context.md` |
| 4 | No floating-point money survives in the TypeScript half | `citation`: `costMicros` returns integer micros; `grep -n 'costUsd\b' backoffice/src/agent` returns nothing. The only division by 1,000,000 is in `AgentConsole.tsx`'s render |
| 5 | The console actually implements the rule it publishes — the reducer folds per-request frames and dedupes rather than assigning | `citation`: `useAgentSession.ts` `case 'usage'`, using `keepLargerFrame`/`foldUsage`; the mock emits a partial frame, a finished frame and a repeated finished frame per request so the dedupe path is exercised |
| 6 | The gate is green and the priced-by-model behaviour still holds | `command`: `./gradlew backofficeTypecheck backofficeLint backofficeFormatCheck` → `BUILD SUCCESSFUL`; `./gradlew docsLint` → `docs-lint: OK`; `./gradlew e2eTest` → `33 passed`, including "the session cost is priced from the list price of the model that ran", whose Opus/Haiku = 5x and Sonnet/Haiku = 3x ratios survive the move to five cache-aware kinds because every cache rate is a fixed multiple of the base input rate |

## Two claims checked and **not** encoded

`doc:00-constitution#evidence-rule` cuts both ways, so both were checked before being written
down anywhere, and both turned out weaker than asserted.

**"An earlier spike claimed peak context was unobservable."** Not found. Nothing in
`documentation/`, `.beans/`, `backoffice/`, `tools/` or `domains/` asserts it; the committed
baseline's own "What these transcripts cannot tell you" table already lists
`peakContextTokens` as **obtainable**. There is no such claim to correct, so none was
corrected. What *is* true, and is now stated, is that observability depends on rules 2 and 3 —
the cumulative two-field counter this bean removes is what would have made it unobservable.

**"This closes `bean:0020`'s peak-context criterion and a constitution `Enforcement gap:`."**
It closes neither, and claiming otherwise would be the failure this bean exists to prevent.
`doc:00-constitution` §6.2's gap reads "the recorder is not yet implemented" and closes when
the recorder exists — `bean:0014` and `bean:0020`'s work, not a specification correction's.
`bean:0020`'s criterion is likewise still open. What changed is that the criterion was
**unsatisfiable** against the old specification and is now satisfiable: peak context cannot be
derived from a cumulative `tokensIn`/`tokensOut` pair, so a runner built to that spec could not
have met it. That document is also owned by another agent this sprint, so no edit to it was
made from here.

## Findings for others, not acted on here

- **`doc:60-cost-model#spend-record` §3.2 names four token kinds** — `inputTokens`,
  `outputTokens`, `cacheWriteTokens`, `cacheReadTokens` — and §3.2.1 says "all four token
  kinds". `tools/cost_lib.py` and every record already written to
  `domains/modus/cost/0001.ndjson` carry **five**, splitting `cacheWriteTokens` into
  `cacheWrite5mTokens` and `cacheWrite1hTokens` because the two are billed at different
  multipliers; the ndjson carries the folded `cacheWriteTokens` alongside them. The normative
  document is behind its own implementation. `documentation/` is owned by another agent this
  sprint, so this is reported rather than fixed.
- **`bean:0016`** will consume this vocabulary when it attributes spend, and
  **`bean:0021`** carries it over the wire. Neither is edited here.
