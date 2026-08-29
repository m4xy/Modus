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
- Usage reported as a running cumulative total, so a dropped event costs one frame's
  accuracy rather than corrupting the counter — the rule
  `backoffice/src/agent/transport.ts` already states for the UI side of the same seam.
- Model and effort are a recorded decision (`doc:60-cost-model#model-selection`), and the
  run emits what `bean:0016` needs to attribute spend.
- Peak context recorded per run, closing the `Enforcement gap:` on
  `doc:00-constitution#context-budget`.
- Integration tests only: `doc:35-testing#purity-rules` forbids starting a process in
  `src/test`.

Blocks `bean:0021`.
