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
  between consecutive messages, roughly 1.8 frames per message, cache reads dominating the
  token total — is `domains/modus/cost/replay/baseline.md`; `bean:0069` records the
  measurement, the commands, and why the earlier "running cumulative total" criterion here
  was wrong. (`python3 tools/cost-replay.py --check` re-checks that baseline against the
  inputs it recorded, and reports drift whenever a live session appends a line; it needs
  `--transcripts DIR` in a worktree. A red `--check` is not a wrong baseline.)
- Model and effort are a recorded decision (`doc:60-cost-model#model-selection`), and the
  run emits what `bean:0016` needs to attribute spend — five token kinds, not two, or 98%
  of the spend is unpriced.
- The run's end is **synthesised, not observed**, and the runner must not be built to wait
  for a terminal frame. Note what is and is not there, because the naive reading is wrong:
  every stored assistant message carries a `stop_reason`, so a status field IS visible on
  the wire — but it is a *message* status, not a *run* terminal. On most runs the last
  assistant line's `stop_reason` is null and `end_turn` appears on a small minority, and no
  `*.meta.json` sidecar field carries an exit code, status or end reason at all. A
  cancelled run's process is killed and writes nothing. A runner that settles on `end_turn`
  therefore hangs on most runs. It emits `AgentRunCompleted` itself, on process exit.
  `bean:0069` carries the counts.
- Peak context recorded per run, as
  `max(inputTokens + cacheReadTokens + cacheWrite5mTokens + cacheWrite1hTokens)` over the
  run's requests (`cost_lib.peak_context_tokens`). What makes this derivable is the **cache
  kinds**, not per-request reporting on its own — successive differences of a cumulative
  in/out counter would recover per-request input, but no arithmetic recovers a cache-read
  figure that was never reported, and cache reads are almost the whole prompt. Satisfying
  this criterion is what closes the `Enforcement gap:` on
  `doc:00-constitution#context-budget`, which stays open until the recorder actually runs.
- Integration tests only: `doc:35-testing#purity-rules` forbids starting a process in
  `src/test`.

Blocks `bean:0021`.
