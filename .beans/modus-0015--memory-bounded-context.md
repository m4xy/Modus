---
# modus-0015
title: The memory bounded context
status: todo
type: feature
priority: normal
created_at: 2026-08-29T00:00:00Z
parent: modus-0011
blocked_by: [modus-0013]
---

# The memory bounded context

Why: `doc:50-memory-and-evidence` is a complete specification with no implementation.
Evidence-backed durable memory at domain, epic and story scope is what separates Modus
from a prompt log, and `doc:00-constitution#evidence-rule` is enforced by nobody.

Success criteria:

- `Memory` and `EvidenceRecord`, with the scopes of
  `doc:50-memory-and-evidence#memory-scopes`, the six kinds of `#evidence-kinds` as a
  closed set with no `other`, the fields and caps of `#evidence-record`, and the statuses
  and triggers of `#invalidation`.
- A memory with no evidence record cannot be constructed — the invariant is in the
  aggregate, so a schema validator is defence in depth and not the mechanism.
- `MemoryRecorded`, `MemoryInvalidated` published; `WorkItemClosed` and
  `AgentRunCompleted` consumed.

`bean:0014` is not a blocking edge either way: §3.1 states the cycle is intentional.
