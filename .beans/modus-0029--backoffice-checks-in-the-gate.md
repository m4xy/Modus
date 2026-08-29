---
# modus-0029
title: Put the backoffice and e2e checks inside the gate
status: todo
type: feature
priority: high
order: AZ
created_at: 2026-08-29T00:00:00Z
---

# Put the backoffice and e2e checks inside the gate

Why: `bean:0028` found that nothing runs the backoffice's own checks. `backoffice/` has
`typecheck`, `lint` and `format:check` scripts and `e2e/` has a Playwright `test` script;
no Gradle task invokes any of them and `.github/workflows/ci.yml` runs only
`./gradlew qualityCheck`. A TypeScript type error, an ESLint error or a broken Playwright
spec merges green today. `doc:00-constitution` §10 says the UI is a deliverable and every
user-facing flow has a Playwright test; nothing enforces either half.

`bean:0028` demoted the claims to `Enforcement gap:` lines pointing here. This bean closes
them by building the mechanism, and by taking the one decision `bean:0028` deliberately
left open.

Success criteria:

- The decision, recorded with its alternative: adopt Spotless as `doc:30-code-style` §1
  describes — one formatter across Kotlin, TypeScript, Markdown, YAML and JSON — or
  standardise on ktlint for Kotlin plus the backoffice's own Prettier, and rewrite §1 to
  match. Either is defensible; shipping neither is what produced `bean:0028`.
- `./gradlew qualityCheck` runs the backoffice's `typecheck`, `lint` and `format:check`,
  and fails when any of them fails. Observed failing per
  `doc:00-constitution#observed-failing`: plant a type error, watch the named task reject
  it, revert.
- An `e2eTest` task exists, runs Playwright against a built and running system, and stays
  outside `check` for the reason recorded in `doc:00-constitution` §7.2.4's
  `Enforcement gap:` — it needs a running system and takes minutes, and inside the fast gate
  it would make agents stop running the gate.
- `knip` is either installed and wired in as `doc:30-code-style` §6 claims, or struck from
  that table. It is currently neither.
- The 71 files currently failing `npm --prefix backoffice run format:check` are reformatted,
  in a commit of their own that touches nothing else, before the check becomes a gate.
  Wiring the check in first would make the next unrelated pull request carry the diff.
- The three `Enforcement gap:` lines `bean:0028` left — `doc:00-constitution` §7.2.4,
  `doc:30-code-style` §1 and §6 — are removed, each replaced by an `Enforced by:` line
  naming the task that was observed rejecting a planted violation. Those three are the
  complete set; `doc:30` §0 and `doc:80` step 6 cite the gap rather than carrying one.
