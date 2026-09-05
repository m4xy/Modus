---
# modus-0022
title: Replace the backoffice's mocked API with the live one
status: todo
type: feature
priority: normal
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0019, modus-0021, modus-0140]
---

# Replace the backoffice's mocked API with the live one

Why: the backoffice renders fixtures. `bean:0002` built the surface against a mocked
`backoffice/src/api`, correct then and a second source of truth now — a mocked client
cannot disagree with the server, so nothing catches the day they diverge.

Success criteria:

- API types generated from the OpenAPI document `bean:0018` produces, replacing the
  hand-written ones rather than sitting beside them (`doc:30-code-style` §6).
- Every read and write goes to the live server; the mock is deleted, not disabled, except
  the agent-stream fixture `bean:0021` keeps for offline work. `knip` stays clean, so no
  dead export survives the removal.
- A domain the actor cannot see is absent from navigation, search and error text — the UI
  half of `doc:00-constitution#domain-scoping` — and that absence is asserted, not
  eyeballed, with Playwright over every user-visible flow (`doc:00-constitution` §10).
