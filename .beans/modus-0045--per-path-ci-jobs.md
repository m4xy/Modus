---
# modus-0045
title: Split CI into per-path jobs so a change pays only for what it touches
status: completed
type: feature
priority: high
order: AI
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T16:01:48Z
---

# Split CI into per-path jobs so a change pays only for what it touches

`bean:0029` was right and made this worse. Wiring `backoffice/` and `e2e/` into
`qualityCheck` closed a real hole — a TypeScript error merged green before it — but it also
means **every Kotlin-only change now runs `npm ci`, `tsc`, ESLint and Prettier**, and every
change of any kind runs Playwright.

Measured on this repository's own history:

| | duration |
|---|---|
| `main`, Kotlin only, before `bean:0029` | ~50–53s |
| after wiring, with npm and Playwright | 2m02s |

That is the cost `bean:0039` is really about, and it is a **CI topology** problem rather
than a repository-boundary problem. Fixing it here is cheap and answers whether the
separation wanted is about gates or about repositories — which is the question `bean:0039`
cannot answer until someone tries the cheap thing.

## Success criteria

- Separate jobs with `paths` filters: a Kotlin change does not run the backoffice checks, a
  backoffice change does not run the Kotlin suites, and a change touching both runs both.
- **Branch protection still passes when a job is skipped.** This is the part that goes wrong:
  a required check that is skipped rather than passed blocks the merge forever. Either the
  required check is an aggregating job that succeeds when its dependencies are skipped, or
  the ruleset names something that always runs. Verify against the `main-protected` ruleset
  on a real pull request, not by reading the workflow.
- `doc:00-constitution` §7.2.4's promise still holds: a green local `qualityCheck` means a
  green CI run. If CI runs a subset per path, the local command must remain the superset, so
  the promise becomes "green locally implies green in CI" and not the reverse. State the
  asymmetry in §7.2.4 rather than letting it become a second silent divergence.
- Measure and record the result. The claim is that this recovers most of the time a split
  would; if it does not, that is `bean:0039`'s answer.

## What the work found

**Branch protection has no `required_status_checks` rule at all.** The second success
criterion assumed a skipped job would block a required check; it cannot, because CI has
never been required.

```
cmd:      gh api repos/m4xy/Modus/rulesets/21765196 -q '.rules[].type'
observed: deletion non_fast_forward pull_request
```

That makes this bean simpler and exposes something larger, raised as `bean:0047`. The `gate`
job here is built to *be* the required check once someone turns it on: it always runs, and
fails only when a half that actually ran failed. Turning the requirement on is held back one
step deliberately — a required check that never reports locks the repository, so the name
must be observed green on a real pull request first.

## Shape

Four jobs. `changes` classifies the diff; `build` and `frontend` are conditional on it;
`gate` aggregates.

The classifier is **fail-safe by construction**: `kotlin` runs unless *every* changed path is
under `backoffice/` or `e2e/`, so anything unclassified runs the superset. A new branch, a
force-push, or a base commit that cannot be resolved runs both halves rather than guessing.
`.editorconfig` triggers both — ktlint and Prettier each read it, which is exactly how a
Kotlin indent setting silently reindented every TypeScript file in `bean:0029`.

No third-party action. `ci.yml` already notes that its one third-party action is the reason
the job holds `pull-requests: write`; a path-filter action would add a second for fifteen
lines of shell.

`qualityCheck` stays the complete local gate and CI subtracts from it with `-x`, rather than
CI gaining a task local runs do not have. `doc:00-constitution` §7.2.4's promise only holds
while the local command is the superset, and that is now stated there as one-directional.

## Evidence

| # | criterion | observed |
|---|---|---|
| 1 | separate jobs with path filters; Kotlin-only skips the frontend half, frontend-only skips the Kotlin half, both runs both | run `33261902606` — `backoffice + e2e` **skipped** on a real pull request; plus the classifier's decision function over five path sets, below |
| 2 | branch protection still passes when a job is skipped | `gate` observed `success` on run `33261902606`, in which `backoffice + e2e` was skipped — the aggregating job reports green rather than never reporting. The ruleset also carries no `required_status_checks` rule at all, so nothing is blocked today either way (`bean:0047`) |
| 3 | `doc:00-constitution` §7.2.4 states the promise one-directionally | citation below |
| 4 | measure and record the result | run `33256522531`, below |

### Criterion 1 — a half observed skipped on a real pull request

The change that closed this bean touches `.beans/` and nothing else, which is the
Kotlin-side classification. Both events on the same commit, in the same push:

```
run:      33261902606 (pull_request, base resolvable)
observed: which halves            success
          build + mechanical gates success
          backoffice + e2e         skipped
          gate                     success

run:      33261878230 (push, new branch — github.event.before is all zeros)
observed: which halves            success
          build + mechanical gates success
          backoffice + e2e         success
          gate                     success
```

The second is the fail-safe branch firing rather than a contradiction: an unresolvable base
runs both halves rather than guessing, and a brand-new branch has no `before` to diff
against. So the same commit is classified selectively where the diff is knowable and
conservatively where it is not, which is the intended behaviour and had not been observed
until now.

`gate` is `success` in both, which is criterion 2's real answer: an aggregating job whose
dependency was skipped still reports green, so requiring it would not block this change.

### Criterion 1, continued — the classifier's decision function

The `case` block from `.github/workflows/ci.yml`'s `filter` step, extracted verbatim and
driven by a path list:

```
Kotlin-only change (core/core-domain/…/A.kt, build.gradle.kts):
-> kotlin=true frontend=false
backoffice/e2e-only change (backoffice/src/App.tsx, e2e/tests/smoke.spec.ts):
-> kotlin=false frontend=true
change touching both:
-> kotlin=true frontend=true
.editorconfig alone:
-> kotlin=true frontend=true
```

`build` carries `if: needs.changes.outputs.kotlin == 'true'` and `frontend` carries
`if: needs.changes.outputs.frontend == 'true'`, so `false` is a skipped job. The fourth row
is the `bean:0029` trap held: a Kotlin indent knob reindented every TypeScript file, so
`.editorconfig` runs both halves rather than either.

`qualityCheck` stays the superset locally; CI subtracts with
`-x backofficeTypecheck -x backofficeLint -x backofficeFormatCheck` (`ci.yml`) rather than
CI gaining a task local runs do not have.

### Criterion 4 — the measurement

```
run:      33256522531 (pull request #22, this bean's own)
observed: build 50s, frontend 1m32s, gate 4s — gate green
```

Recorded in `bean:0047`, which is the bean this observation was held back for: the `gate`
check name exists, always runs, and reports, which is the precondition for requiring it.
That run exercised both halves — the change touched `.github/workflows/ci.yml`, which the
classifier routes to both by design — so the two halves ran in parallel rather than in the
2m02s serial sequence measured above. Requiring the check is `bean:0047` and is blocked on
a human: the harness refuses branch-protection edits to an agent.

### Criterion 3 — the asymmetry is stated, not glossed

`doc:00-constitution` §7.2.4 now reads: "**CI runs a subset of this per change, so the
promise is one-directional** … a green local `qualityCheck` **plus** `e2eTest` implies a
green CI run, and the reverse does not hold. The local command stays the superset
deliberately — the moment CI can run something local cannot, this promise is gone."
