---
# modus-0012
title: The domainmgmt bounded context
status: todo
type: feature
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

Success criteria:

- `Domain` and `ModuleInstallation`, placed per `doc:20-ddd-practices#ports-and-adapters`
  §5.1. `DomainCreated`, `ModuleInstalled`, `ModuleUninstalled`,
  `ProcessDefinitionChanged` published; `GrantRevoked` consumed.
- Module visibility returns an absence, not a denial, so 404-not-403 stays a property of
  the model as `AccessDecision` already makes it. Persistence is `bean:0017`.

Blocks `bean:0013` (published language) and `bean:0023`: a second context is what makes
`ContextInternalsAreSealed` and `PublishedLanguageAllowlist` capable of failing.
