---
# modus-0012
title: The domainmgmt bounded context
status: todo
type: epic
priority: high
order: A
created_at: 2026-08-29T00:00:00Z
parent: modus-0011
---

# The domainmgmt bounded context

Why: `bean:0009` shipped grants against a `DomainId` nothing creates and Modules nothing
installs. `domainmgmt` owns domains, module installation, per-domain module visibility and
per-domain process definitions — the half of `doc:00-constitution#domain-scoping` that
`identity` does not cover.

Blocks `bean:0013` (published language) and `bean:0023`: a second context is what makes
`ContextInternalsAreSealed` and `PublishedLanguageAllowlist` capable of failing.

## Split into two, at step 2

Written as one `type: feature` bean. Split under `doc:80-agent-operating-procedure#restate-success-criteria`
point 4 and `doc:00-constitution#context-budget` §6.2, before any code: `identity` is the
only precedent for the size of a bounded context and it landed as one 1,150-line pull
request over eight review threads. Two aggregates, two published sets, four events, two
ports and a visibility rule is more than that, and the ceiling is a work-package ceiling,
not a per-agent one.

The seam is the aggregate, not the layer. Each child ships an aggregate that raises its own
events, so no published type sits on `main` unraised by anything.

| child | ships | order |
|---|---|---|
| `bean:0030` | published language, `ProcessDefinition`, the `Domain` root, `DomainCreated`, `ProcessDefinitionChanged`, `DomainRepository` | AM |
| `bean:0031` | `ModuleInstallation`, `ModuleInstalled`, `ModuleUninstalled`, module visibility as an absence, `GrantRevoked` consumption, `ModuleInstallationRepository`, deletion of the `DomainMgmtContext` marker | AT |

Rejected: a first child shipping the published package and all four events alone. It is the
smallest reviewable unit and it lands the cross-context contract in one piece, but it puts
four events on `main` that nothing raises for the length of a review cycle — and an event
with no raiser cannot be shown to fire, which is the whole of
`doc:00-constitution#observed-failing`.

## Done when

Both children are `completed`, the `domainmgmt` row of
`doc:10-architecture#bounded-contexts` §3 is real in `core-domain`, and `bean:0023` can
show `ContextInternalsAreSealed` and `PublishedLanguageAllowlist` failing against a second
modelled context.
