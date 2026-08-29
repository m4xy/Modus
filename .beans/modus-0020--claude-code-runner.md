---
# modus-0020
title: The Claude Code runner
status: todo
type: feature
priority: normal
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0014]
---

# The Claude Code runner

Why: `adapters/adapter-agent-claude` is a placeholder descriptor, so `doc:10-architecture`
§6.1's trigger → agent run → stream flow has no runner and the self-hosting destination of
`doc:00-constitution` §12 has no first step.

Success criteria:

- The `execution` run port implemented against Claude Code, spawning the process and
  turning its output into the ordered, additive events `bean:0014` publishes.
- Usage emitted **per request**, carrying `message.id` and the five token kinds
  `bean:0014` publishes. The runner does not accumulate: it forwards what one request cost
  and lets the consumer deduplicate on `message.id`, keeping the frame with the largest
  `outputTokens`. Evidence that this is the real shape — output tokens observed falling
  between consecutive messages, ~1.8 frames per message, cache reads dominating the token
  total — is `domains/modus/cost/replay/baseline.md`, re-checkable with
  `python3 tools/cost-replay.py --check`; `bean:0069` records the measurement and why the
  earlier "running cumulative total" criterion here was wrong.
- Model and effort are a recorded decision (`doc:60-cost-model#model-selection`), and the
  run emits what `bean:0016` needs to attribute spend — five token kinds, not two, or 98%
  of the spend is unpriced.
- A cancelled run is **synthesised, not observed**: the process is killed and writes no
  terminal record, and neither the transcript nor a subagent's `*.meta.json` sidecar
  carries an exit code, status or end reason. The runner emits `AgentRunCompleted` itself
  on process exit; it must never block waiting for a final frame.
- Peak context recorded per run, as
  `max(inputTokens + cacheReadTokens + cacheWrite5mTokens + cacheWrite1hTokens)` over the
  run's requests (`cost_lib.peak_context_tokens`). This is what the per-request criterion
  above buys: the figure is not derivable from a cumulative in/out pair. Satisfying it is
  what closes the `Enforcement gap:` on `doc:00-constitution#context-budget`, which stays
  open until the recorder actually runs.
- Integration tests only: `doc:35-testing#purity-rules` forbids starting a process in
  `src/test`.

Blocks `bean:0021`.
