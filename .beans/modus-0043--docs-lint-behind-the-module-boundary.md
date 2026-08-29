---
# modus-0043
title: Move docs-lint behind the Module boundary
status: todo
type: feature
priority: normal
order: AGZ
created_at: 2026-08-29T00:00:00Z
parent: modus-0040
blocked_by: [modus-0042]
---

# Move `docs-lint` behind the Module boundary

`tools/docs-lint.sh` is 306 lines of policy about how the `modus` domain writes documents —
its front-matter schema, its reference syntax, its line budget, its anchor rules. It sits in
the framework's tree and is invoked by the framework's root build, so a tenant adopting Modus
inherits one domain's authoring conventions as though they were the product.

`doc:10-architecture#module-system` §7.3 already specifies the property that fixes it: a
Module "does nothing at all in a domain where it is not installed" and "MUST NOT register
global state, global routes, or global beans that are observable from an uninstalled domain."
Tier-3 tooling is a Module installed into the domain that wants it.

## The gap this bean has to resolve

`docs-lint` runs at **build time**, in CI, over files. A Module is a **runtime** concern
inside a running Modus. The Module boundary does not reach a shell script in a Gradle build,
so this is not a lift-and-shift.

`doc:00-constitution` §12 is where it resolves: a self-hosting Modus runs the `modus` domain's
checks as part of that domain's process definition, at which point `docs-lint` is a check the
domain requires rather than a task the framework runs. That is the destination; this bean has
to decide how much of it to build now.

Success criteria:

- Decide and record the interim shape: a Module that owns the policy with a thin build-time
  invoker, or the script staying in `tools/` explicitly marked tier 3 until self-hosting makes
  the Module real. Both are defensible; shipping neither is what `adr:0006` calls being
  misfiled by default.
- Whichever is chosen, a tenant's build must not run this domain's document policy. That is
  the property, and it is what the criterion is checked against.
- The policy itself — schema, reference syntax, budgets — becomes data the `modus` domain
  owns, not constants in a framework script. `bean:0030`'s `ProcessDefinition` is the
  precedent: a domain's rules are per-domain data, and `doc:00-constitution#domain-scoping`
  forbids code hardcoding one process.
