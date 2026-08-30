---
# modus-0031
title: Module installation and per-domain module visibility
status: todo
type: feature
priority: high
order: AT
created_at: 2026-08-29T00:00:00Z
parent: modus-0012
blocked_by: [modus-0030, modus-0066]
---

# Module installation and per-domain module visibility

Second child of `bean:0012`, on the `Domain` root `bean:0030` builds.

Why: `doc:10-architecture#domain-root-convention` §5.4 says a module installed in domain A
is invisible to domain B — not listable, not reachable, not discoverable through the
OpenAPI document. `AccessDecision` already makes 404-not-403 a property of the model for
grants; nothing makes it one for modules.

## Success criteria and evidence

Sibling parity with `bean:0030` is deliberate: the second child is not a lighter bean
because it is second.

| # | criterion | evidence kind |
|---|---|---|
| 1 | `ModuleInstallation` under `..domainmgmt.aggregate`: private constructor, named factory, no public mutable surface | citation + `rule:archunit/aggregatesAreSealedOrFinal` |
| 2 | Installing, uninstalling and reinstalling a module raise exactly the events those words claim, and no others | test-run |
| 3 | Visibility returns an absence, and the signature makes the leak unrepresentable rather than discouraged | test-run, both the visible and invisible case |
| 4 | A caller cannot distinguish "not installed" from "installed and refused" | test-run: the two paths return the same value, asserted directly |
| 5 | `GrantRevoked` is consumed from `identity`'s published language, and that is the context's only cross-context import | citation + `rule:archunit/publishedLanguageIsLeaf` |
| 6 | 100% branch coverage over `..domainmgmt.aggregate`, `config/coverage/baseline.tsv` moved by exactly this bean | `./gradlew qualityCheck` |
| 7 | Each added test is load-bearing: broken subject, observed assertion recorded verbatim, reverted | test-run, per `doc:35-testing#load-bearing-evidence` |
| 8 | Fixtures vary collection size across 0, 1 and 2-or-more wherever a collection is accepted | citation, per `doc:35-testing#fixture-variation` |
| 9 | `./gradlew qualityCheck` is green | test-run |

Specifics:

- `ModuleInstallation` under `..domainmgmt.aggregate`, with the boundary
  `doc:20-ddd-practices#aggregates` §2.3 gives it: module id, version, visibility, config;
  `DomainId` by reference. `ModuleInstalled` and `ModuleUninstalled` published.
- Visibility returns an **absence**, not a denial. The signature makes the leak
  unrepresentable rather than merely discouraged, the way `AccessDecision.DomainNotVisible`
  does — a caller must not be able to distinguish "not installed" from "installed and
  refused", because there is no such distinction to draw.
- `GrantRevoked` is consumed from `identity`'s published language. This is `domainmgmt`'s
  half of §3's `Consumes` column and the second half of what `bean:0023` needs.

## The one decision to take at step 2

`ModuleInstallation` is identified by the natural key
`(DomainId, ModuleId)` rather than a synthetic `InstallationId`. A module is installable at
most once into a domain, so a synthetic id admits a second row for a state that cannot
exist. `doc:20-ddd-practices` §3.2's required-value-object list names `ModuleId` and no
installation id, which agrees. Confirm against `doc:40-durability`'s on-disk layout before
committing to it — the store addresses records by path, and a composite key is a two-segment
path, not a file name.
