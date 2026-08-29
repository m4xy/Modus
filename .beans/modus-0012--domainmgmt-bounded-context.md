---
# modus-0012
title: The domainmgmt bounded context
status: todo
type: epic
priority: high
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
only precedent for the size of a bounded context, and it landed as one pull request over
eight review threads (`bean:0009`, "Eight threads"). Measured rather than recalled —
`14f54ea..b1e0809` is 31 files, +2576/-37, of which `core/` alone is +1023/-26. Two aggregates, two published sets, four events, two
ports and a visibility rule is more than that, and the ceiling is a work-package ceiling,
not a per-agent one.

The seam is the aggregate, not the layer. Each child ships an aggregate that raises its own
events, so no published type sits on `main` unraised by anything.

| child | ships | order |
|---|---|---|
| `bean:0030` | published language, `ProcessDefinition`, the `Domain` root, `DomainCreated`, `ProcessDefinitionChanged`, `DomainRepository`, deletion of the `DomainMgmtContext` marker | AM |
| `bean:0031` | `ModuleInstallation`, `ModuleInstalled`, `ModuleUninstalled`, module visibility as an absence, `GrantRevoked` consumption, `ModuleInstallationRepository` | AT |

Rejected: a first child shipping the published package and all four events alone. It is the
smallest reviewable unit and it lands the cross-context contract in one piece, but it puts
four events on `main` that nothing raises for the length of a review cycle — and an event
with no raiser cannot be shown to fire, which is the whole of
`doc:00-constitution#observed-failing`.

## This epic is never `blocked_by`'d, and that is deliberate

`AGENTS.md` step 1 skips `type: epic`, so an epic is never selected, so it never reaches
`completed` — and any bean whose `blocked_by` names it would wait forever. Review found
exactly that: `bean:0013`, `bean:0018` and `bean:0023` pointed here, and converting this
bean from `feature` to `epic` would have made all three permanently unselectable.

Their edges now name the child that actually unblocks them, which is more precise than the
edge they replaced: `bean:0013` needs this context's published language (`bean:0030`);
`bean:0018` needs `ModuleInstallation` (`bean:0031`); `bean:0023` needs a second context
that imports another's published language, which is `GrantRevoked` consumption
(`bean:0031`).

**The general rule: an epic may have children and `blocked_by` edges of its own, and MUST
NOT be the target of one.** Nothing enforces it — `tools/docs-lint.sh` resolves typed
`bean:` references in prose and never reads `blocked_by` front-matter at all. `bean:0035`
carries the check.

## Done when

Both children are `completed`, the `domainmgmt` row of
`doc:10-architecture#bounded-contexts` §3 is real in `core-domain`, and `bean:0023` can
show `ContextInternalsAreSealed` and `PublishedLanguageAllowlist` failing against a second
modelled context.

## Split merged

The split merged as PR #16 (`2792a8d`); this epic stays `todo` until both children close.

Review of the split found it deadlocked three beans: `bean:0013`, `bean:0018` and
`bean:0023` pointed `blocked_by` at this bean, and step 1 skips epics — so it would never be
worked, never complete, and those edges would never clear. Their edges now name the child
that actually unblocks each. The rule is stated above and `bean:0035` enforces it.

PR #14 was the original; GitHub auto-closed it when `bean:0032`'s merge deleted its base
branch. `bean:0037` records the procedure that would have prevented it.
