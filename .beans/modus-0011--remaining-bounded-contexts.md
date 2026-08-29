---
# modus-0011
title: The remaining five bounded contexts
status: todo
type: epic
priority: high
created_at: 2026-08-29T00:00:00Z
---

# The remaining five bounded contexts

`doc:10-architecture#bounded-contexts` names six. `bean:0009` modelled `identity` and set
the shape every child follows: placement by kind, published language as a leaf, invariants
in `init`, private constructor plus named factory, time as a parameter, ports declared and
unimplemented, evidence per `doc:35-testing#load-bearing-evidence` with fixtures varied per
`doc:35-testing#fixture-variation`. Each child states only what is specific to it. Each
remaining context carries a provisional `*Context` marker under
`core/core-domain/.../domain/<ctx>/`; modelling one deletes its marker and moves its row in
`config/coverage/baseline.tsv`.

Children: `bean:0012`, `bean:0013`, `bean:0014`, `bean:0015`, `bean:0016`. Order is §3.1's
published-language table, not preference; `memory` and `execution` import each other's
published language and that intentional cycle is a blocking edge in neither direction.

Done when `BoundedContexts` goes with the last marker and `bean:0023` closes §3.1's
`Enforcement gap:`.
