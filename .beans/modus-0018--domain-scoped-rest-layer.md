---
# modus-0018
title: The domain-scoped REST layer at /domains/{domainId}
status: todo
type: feature
priority: high
created_at: 2026-08-29T00:00:00Z
blocked_by: [modus-0147, modus-0148, modus-0149, modus-0150, modus-0031]
---

# The domain-scoped REST layer at /domains/{domainId}

Why: `adapters/adapter-rest` is a placeholder descriptor, so nothing
`doc:10-architecture#domain-root-convention` specifies — the route convention, the §5.1
allowlist, the §5.3 authorisation contract, §5.4 module visibility — is reachable over
HTTP.

Success criteria:

- Every route under `/domains/{domainId}` except the §5.1 allowlist, which is read from
  that section and never copied.
- `AccessDecision` mapped with no conditional of the transport's own: `bean:0009` put the
  whole 404-not-403 decision in `domainIsVisible`, so the mapping is total over the sealed
  class and has no `else`.
- Controllers return DTOs; domain types do not cross the boundary
  (`doc:15-repository-layout#adapter-rules` §4.3). An OpenAPI document is generated —
  `doc:30-code-style` §6 forbids hand-written backoffice API types.
- 404-not-403 asserted in `src/integrationTest` for every cross-domain access path, each
  assertion observed failing on a planted leak (`doc:00-constitution#observed-failing`).

`blocked_by` adds `modus-0012`: the §5.3 authorisation contract's step 4 (module
visibility) needs domainmgmt's `ModuleInstallation`, an internals dependency, not a
published-language one — narrowing this bean's own criteria to exclude step 4 would ship
an authorisation contract with a hole in it and a second bean to patch the hole later.
Adding the edge instead means the contract lands whole, once, the same shape `bean:0009`
put the rest of `AccessDecision`'s mapping in.

Blocks `bean:0019`, `bean:0021`, `bean:0022`.
