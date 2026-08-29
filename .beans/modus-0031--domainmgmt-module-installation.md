---
# modus-0031
title: Module installation and per-domain module visibility
status: todo
type: feature
priority: high
order: AT
created_at: 2026-08-29T00:00:00Z
parent: modus-0012
blocked_by: [modus-0030]
---

# Module installation and per-domain module visibility

Second child of `bean:0012`, on the `Domain` root `bean:0030` builds.

Why: `doc:10-architecture#domain-root-convention` §5.4 says a module installed in domain A
is invisible to domain B — not listable, not reachable, not discoverable through the
OpenAPI document. `AccessDecision` already makes 404-not-403 a property of the model for
grants; nothing makes it one for modules.

Success criteria:

- `ModuleInstallation` under `..domainmgmt.aggregate`, with the boundary
  `doc:20-ddd-practices#aggregates` §2.3 gives it: module id, version, visibility, config;
  `DomainId` by reference. `ModuleInstalled` and `ModuleUninstalled` published.
- Visibility returns an **absence**, not a denial. The signature makes the leak
  unrepresentable rather than merely discouraged, the way `AccessDecision.DomainNotVisible`
  does — a caller must not be able to distinguish "not installed" from "installed and
  refused", because there is no such distinction to draw.
- `GrantRevoked` is consumed from `identity`'s published language. This is `domainmgmt`'s
  half of §3's `Consumes` column and the second half of what `bean:0023` needs.

Decision to take, not yet taken: `ModuleInstallation` is identified by the natural key
`(DomainId, ModuleId)` rather than a synthetic `InstallationId`. A module is installable at
most once into a domain, so a synthetic id admits a second row for a state that cannot
exist. `doc:20-ddd-practices` §3.2's required-value-object list names `ModuleId` and no
installation id, which agrees. Confirm against `doc:40-durability`'s on-disk layout before
committing to it — the store addresses records by path, and a composite key is a two-segment
path, not a file name.
