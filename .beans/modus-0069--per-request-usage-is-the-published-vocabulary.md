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
`doc:10-architecture#bounded-contexts` makes published language expensive to change — a
change to a published contract **needs an ADR** (`doc:20-ddd-practices` §4.1.5). It is
currently specified to publish a **wrong** usage model, and the wrong model is written in
three places that cite **each other** rather than any observation:

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

Measured with `cost_lib` over the 82 transcripts present on 2026-08-29 — a superset of the 65
runs the committed baseline was generated from, because live sessions keep appending. Figures
are evidence of a point in time, not constants; nothing below is copied into merged code
(`bean:0059`), and a re-run tomorrow will differ in the digits and not in the shape.

| claim | observed |
|---|---|
| usage is **per-request**, not cumulative | `outputTokens` **falls** from one message to the next on 3,026 of 6,467 consecutive pairs (46.8%). The denominator is *pairs*, not messages: a fall is a property of two adjacent messages and the first message of each of the 82 runs has no predecessor, so pairs = 6,549 messages − 82 runs. A cumulative counter never decreases even once. |
| frames of one message must be **deduplicated** on `message.id` | frame multiplicity 1.83x — 11,956 frames for 6,549 messages. Summing them multiplies the bill. |
| the frame to keep is the one with the **largest `outputTokens`** | 0 disagreements between frames of one message on the other four kinds; the committed baseline reports 1,129,285 output tokens (47.91% of all output) recovered by taking the largest frame rather than the first. **This premise is observed, not asserted** — see the detection gap below. |
| **cache tokens are the bill** | cache reads are 97.59% of all tokens (98.05% in the committed baseline). Fresh input is 13,044 tokens of 997,772,971 — 0.00%. Input plus output together are 0.30%. A model that prices only those two prices almost nothing. |
| **peak context is observable** | `cost_lib.peak_context_tokens` = `max(inputTokens + cacheReadTokens + cacheWrite5mTokens + cacheWrite1hTokens)`; largest single request in the corpus 865,375 tokens, 2.9x `doc:00-constitution#context-budget`'s 300k ceiling. What makes it derivable is the **cache kinds**: successive differences of a cumulative in/out counter would recover per-request input, but no arithmetic recovers a cache-read figure that was never reported, and cache reads are almost the whole prompt. |
| **a per-message status exists; a run terminal does not** | `message.stop_reason` is present on **all 11,510** assistant lines and non-null on 4,255 (`tool_use` 4,041, `end_turn` 187, `stop_sequence` 27); `apiErrorStatus: 429` with `error: "rate_limit"` appears on 19 lines and `server_error` on 8. So a status field IS on the wire. It is not a run terminal: the **last** assistant line of a run carries `stop_reason` null on **54 of 82** runs, `end_turn` on 19, `stop_sequence` on 6, `tool_use` on 3 — so a consumer settling on `end_turn` hangs on 63 of 82. No field of any of the 76 `*.meta.json` sidecars carries an exit code, status or end reason, and a cancelled run's process is killed before writing anything. `session-end{reason:'cancelled'}` is consumer-synthesised **by definition**. |

Commands, both repeatable: `python3 tools/cost-replay.py --check --transcripts <dir>` for the
committed baseline, and two scripts over `cost_lib.read_messages`, `frame_disagreements` and
`peak_context_tokens` — the same functions the replay calls — plus a direct scan of
`message.stop_reason`, `apiErrorStatus` and the sidecar key sets for the last row.

**Read what `--check` checks before citing it.** It hashes the inputs the baseline recorded
and reports **drift** if any has changed, which any live session causes by appending a line;
it returns before comparing figures at all in that case, and the comparison it would reach is
of file *text*. So a red `--check` says "the committed figures describe a different input
set", not "the figures were wrong". It also needs `--transcripts DIR` in a worktree, because
the project directory is derived from the checkout path. Observed here:
`baseline inputs have moved on: 5 changed, 0 gone.`

**The baseline is deliberately NOT regenerated.** It is `bean:0054`'s merged evidence record;
`adr:0005-evidence-lives-in-the-work-item` puts evidence in the work item, and regenerating it
from a corpus that now contains this very session would make the record describe the act of
measuring it.

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
forbids; the division by 1,000,000 now happens once, at the render boundary. **The field
keeps the name `costUsd`** — the name `doc:60-cost-model#spend-record` gives it and the name
every record in `domains/modus/cost/0001.ndjson` already carries for the same integer-micros
quantity. A `costUsdMicros` here would have been a *third* name for one concept, which is the
defect this bean exists to remove rather than to commit.

An unpriced model id yields **no price**, never a defaulted one. `cost_lib.normalise_model`
raises on one; `costMicros` returns `null` and the console shows no figure. The mechanism
differs because a reducer inside React cannot throw without taking the console down
mid-stream; the outcome does not: neither side ever returns a wrong price. The previous
`?? BASE_RATES_UPM['claude-sonnet-5']` would have priced a `claude-opus-4-8` run 60% low with
nothing on screen to say so, defaulting onto the one entry whose rate has an expiry.

Each of the three artefacts states this and cites the corpus. **None cites another of the
three as its authority for the usage model** — bean references remain, as provenance, which is
a different thing and is said so in the text.

## Success criteria and evidence

| # | criterion | evidence |
|---|---|---|
| 1 | `transport.ts` states per-request usage, `messageId` deduplication keeping max `outputTokens`, five token kinds, and a consumer-synthesised terminal state — each citing the corpus, not the beans | `citation`: `backoffice/src/agent/transport.ts` rules 1–4 and the "Evidence" block, which names `domains/modus/cost/replay/baseline.md` and `tools/cost_lib.py` and no bean |
| 2 | `bean:0020`'s cumulative criterion is replaced, and its "peak context recorded per run" criterion is made satisfiable rather than claimed closed | `diff`: `.beans/modus-0020--claude-code-runner.md`; the criterion now names the `cost_lib.peak_context_tokens` formula and the per-request usage it needs |
| 3 | `bean:0014` is no longer silent: it states the published usage shape it must not get wrong | `diff`: `.beans/modus-0014--execution-bounded-context.md` |
| 4 | No floating-point money survives in the TypeScript half, and no third name for it is introduced | `citation`: `costMicros` returns integer micros and the state field is `costUsd`, the name `doc:60#spend-record` and `0001.ndjson` already use. The only division by 1,000,000 is `AgentConsole.tsx`'s render, guarded so `null` shows `—` rather than `$0.0000` |
| 5 | The console actually implements the rule it publishes — the reducer folds per-request frames and dedupes rather than assigning | `citation`: `useAgentSession.ts` `case 'usage'`, using `keepLargerFrame`/`foldUsage`; the mock emits a partial frame, a finished frame and a repeated finished frame per request so the dedupe path is exercised |
| 6 | The premise `keepLargerFrame` rests on is **detected**, and the detector has been observed firing, firing exactly once, and **not** firing on a clean run | `test-run`: `?fault=usage-disagreement` plants a frame disagreeing on `cacheReadTokens`; `./gradlew e2eTest` → `35 passed`, covering "a usage frame that disagrees on cache tokens is reported, not discarded" (asserts `toHaveCount(1)`) and "an ordinary session reports no frame disagreement" (asserts `toHaveCount(0)`). Both were observed failing first — see the findings below |
| 7 | Sonnet 5 is priced at the rate actually in force, matching `cost_lib` for the same model id | `diff`: `BASE_RATES_UPM` now carries the introductory $2/$10 in force through 2026-08-31, as `cost_lib.BASE_RATES_UPM` does. The previous $3/$15 priced it 50% high every day until the lapse. The e2e ratio asserts 2 |
| 8 | The gate is green | `command`: `./gradlew backofficeTypecheck backofficeLint backofficeFormatCheck` → `BUILD SUCCESSFUL`; `./gradlew docsLint` → `docs-lint: OK`; `./gradlew e2eTest` → `35 passed` |

## Two defects this bean committed, found by running the gate

Recorded rather than quietly fixed, because both are instances of the class this bean is
about and one of them is a class nobody here had named.

**A value meaning "not priced" was also made to mean "nothing yet" — F3 in miniature, on
myself.** `costUsd: null` was intended for "the model is not in `BASE_RATES_UPM`", and was
also returned before any usage had arrived, so an idle console rendered `—` where the honest
answer is `$0.0000`: nothing has been charged, and that is a real zero. Two existing e2e tests
asserted `$0.0000` at idle and failed. **The tests were right and the change was wrong**; they
were left untouched and the code fixed, which is the only order that keeps a suite meaning
anything. Now three explicit states: `0` is a real zero, a number is a priced total, `null` is
usage that cannot be priced. Making one name carry two concepts is exactly the defect this
bean removes from the seam, and it took about an hour to reintroduce.

**A detector whose own retention policy corrupts the state it detects against.** The general
form, because it is not specific to usage frames: *anything that keeps a running "best" value
and compares later values to that retained value, rather than to the source, will report a
single fault repeatedly.* Here, once one frame of a message disagrees, `keepLargerFrame`
retains whichever frame won — possibly the corrupted one — and every subsequent frame of that
message then disagrees **with the retention**, not with the data. One planted fault produced
N−1 notices, and the count was an artefact of the mechanism rather than a property of the
stream. Fixed by reporting once per `messageId`. Any deduplicating detector that keeps a
representative has this shape.

Two things about how it surfaced are worth keeping:

- It failed as a **strict-mode violation** — Playwright resolving two elements where one was
  expected — not as an absence. The detector worked; there was *too much* of it. A weaker
  assertion (`toBeVisible` alone) would have passed and shipped the defect, which is why the
  test now asserts `toHaveCount(1)` and pins the count rather than the presence.
- The evidence deliberately carries **three** observations, not one: the detector fires on a
  planted violation, fires **exactly once**, and does **not** fire on a clean run.
  `doc:00-constitution#observed-failing` currently requires only the first. But a mechanism
  that fires on every run has also been "observed firing" and is worthless, and one that fires
  N times for one fault is worse than useless because the count lies. The negative observation
  is not a courtesy; it is half the evidence.

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

## What is detected, and what still is not

Stated plainly so nobody reads criterion 6 as more than it is.

`framesDisagree` covers the **TypeScript consumer only**. It is a **detector, not a gate**: it
reports into the transcript and does not fail a build. The producing side — `tools/cost_lib.py`
and `tools/cost-replay.py`, which generate the committed baseline every figure above comes
from — remains **unasserted**. All six uses of `frame_disagreements` there render a table row;
there is no `raise` and no non-zero exit, `--check` returns on input drift before reaching any
comparison and compares file text when it does, and `./gradlew qualityCheck` runs **no Python
at all** (`cost_lib.py`'s own module docstring records that gap). So the premise is observed to
hold on this corpus and enforced nowhere. An implementation of `bean:0014` must not treat it as
guaranteed by tooling.

## Findings for others, not acted on here

- **Nothing compares a constant in code to the authority it must match.** Raised as
  `bean:0090`; the rate table is one instance of it.
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
