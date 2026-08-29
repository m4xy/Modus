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

**Both figures above are unsourced and both are wrong.** They name no run, no command and
no unit, and they were written into this bean and copied into `bean:0039` before anyone
asked where they came from. Measured against the real run history, the baseline is 134s and
no reading of the pre-`bean:0029` history supports ~50–53s. The originals are left standing
because they are what the bean was opened on; the sourced replacements are under
"Criterion 4 — the measurement" below, and every figure there names its run id.

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
| 1 | separate jobs with path filters — three clauses, **two of them not observed on a real run** | Kotlin-only skips the frontend half: run `33261902606`, `backoffice + e2e` **skipped**. Frontend-only skips the Kotlin half, and a change touching both runs both: **classifier only** — the decision function below, never a CI run, because no such change has been pushed since the split landed |
| 2 | branch protection still passes when a job is skipped | `gate` observed `success` on run `33261902606`, in which `backoffice + e2e` was skipped — the aggregating job reports green rather than never reporting. The ruleset also carries no `required_status_checks` rule at all, so nothing is blocked today either way (`bean:0047`) |
| 3 | `doc:00-constitution` §7.2.4 states the promise one-directionally | citation below |
| 4 | measure and record the result | runs `33261902606` (66s) against `33256259515` (134s), both `.beans/`-only changes, both run wall clock — a 51% saving, n=1 against n=1. Below, with the two unsourced figures this bean inherited and what they should have been |

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

The two unobserved clauses are evidenced by **running** the classifier, not by reading it.
The command extracts the decision function from `.github/workflows/ci.yml` at the commit
that introduced it and drives it with a path list. The extraction is by **line range**, so
it is pinned to the sha and not to the working tree: any edit above line 57 of a later
`ci.yml` shifts the window and yields a different block that still runs and still prints a
result. `git show 65ba4f5:` is what makes it re-runnable by a reader who is not standing on
this branch:

```
cmd:      bash -s <<'EOF'
          sets=(
            "core/core-domain/src/main/kotlin/A.kt build.gradle.kts"
            "backoffice/src/App.tsx e2e/tests/smoke.spec.ts"
            "core/core-domain/src/main/kotlin/A.kt backoffice/src/App.tsx"
            ".editorconfig"
            ".beans/modus-0045--per-path-ci-jobs.md"
          )
          for s in "${sets[@]}"; do
            export changed="$(printf '%s\n' $s)"
            printf '%-62s' "$s"
            bash -c "$(git show 65ba4f5:.github/workflows/ci.yml | sed -n '57,65p;68p' | sed 's/^ *//')"
          done
          EOF

expect:   kotlin-only -> kotlin=true frontend=false; frontend-only -> the reverse;
          both -> both true; .editorconfig -> both true

observed: core/core-domain/src/main/kotlin/A.kt build.gradle.kts        -> kotlin=true frontend=false
          backoffice/src/App.tsx e2e/tests/smoke.spec.ts                -> kotlin=false frontend=true
          core/core-domain/src/main/kotlin/A.kt backoffice/src/App.tsx  -> kotlin=true frontend=true
          .editorconfig                                                 -> kotlin=true frontend=true
          .beans/modus-0045--per-path-ci-jobs.md                        -> kotlin=true frontend=false
```

`bash -s` is load-bearing and is the reason this is written as a command rather than as a
result. Run under `zsh`, `printf '%s\n' $s` does not word-split, every multi-path set
arrives as one path, and row 3 silently reports `frontend=false` — a wrong classification
from a green command. A pasted snippet that is right in one shell and quietly wrong in
another is the failure `doc:00-constitution#observed-failing` is about, one level down.

This is still weaker than a CI run: it proves the decision function, not that GitHub Actions
skips the job the function excludes. `build` carries
`if: needs.changes.outputs.kotlin == 'true'` and `frontend` carries
`if: needs.changes.outputs.frontend == 'true'`, and only the `frontend` half has been
observed skipping. The frontend-only direction stays unobserved until a backoffice change
is pushed.

The `.editorconfig` row is the `bean:0029` trap held: a Kotlin indent knob reindented every
TypeScript file, so `.editorconfig` runs both halves rather than either.

`qualityCheck` stays the superset locally; CI subtracts with
`-x backofficeTypecheck -x backofficeLint -x backofficeFormatCheck` (`ci.yml`) rather than
CI gaining a task local runs do not have.

### Criterion 4 — the measurement

The claim under test is that per-path jobs recover most of the time a repository split
would. It needs a **like-for-like pair**: the same shape of change, the same measure, one
run under each topology. Every figure below is **run wall clock**, `createdAt` to
`updatedAt`, for a push run on `main` or the pull request run of the change itself. Job
durations are quoted separately and always named as such.

```
cmd:      gh run list --branch main --limit 40 \
            --json databaseId,createdAt,updatedAt,headSha,conclusion
          (wall clock = updatedAt - createdAt, successful runs only)

after:    33261902606  pull request #35, `.beans/` only, frontend skipped
          16:03:07Z -> 16:04:13Z                                        =  66s
before:   33256259515  b812a9b `.beans/` only, after bean:0029 wired the
          backoffice in, before this split landed
          13:55:49Z -> 13:58:03Z                                        = 134s

saving:   68s, 51%
```

`b812a9b` is the like-for-like partner and is the reason the pair is worth anything: it
changed three files, all under `.beans/`, and had to run `npm ci`, `tsc`, ESLint, Prettier
and Playwright anyway because the split did not exist yet. It is this pull request's change
under the old topology.

Per-job durations on `33261902606`, for completeness and not as the measurement:
`which halves` 4s, `build` 48s, `backoffice + e2e` skipped, `gate` 4s.

**n=1 against n=1, inside a wide spread.** The twenty-one successful `main` runs in the
history range 47s to 209s, and cache state, runner allocation and change size all move them.
The pair above is two single runs, matched on change shape and nothing else. A saving of 51%
is the honest reading of that pair; it is not a measured mean and this bean does not have one.

#### Two figures this bean inherited, both wrong

**`2m02s` (122s) — the post-`bean:0029` baseline.** Unsourced. The two post-`bean:0029`,
pre-split successful runs on `main` are `33256259515` = 134s and `33256630231` = 133s. The
real baseline is 12s slower than the bean claimed, so the saving is 51% and not the 46% first
recorded here. **The error did not flatter the work**, which is the point: an unsourced
number is not safe just because it happens to understate.

**`~50–53s` — "Kotlin only, before `bean:0029`".** Unsourced, and no reading of the history
supports it. The twelve successful pre-`bean:0029` runs on `main` are 47, 50, 67, 69, 79,
81, 101, 125, 125, 136, 146, 209s. The two closest to this change's shape — documentation
and beans, no code — are `33255099872` (e4dbc48) 47s and `33247011196` (0bc47e0) 50s, which
is probably where the figure came from and is not what the row says.

**The ceiling claim, restated against real runs.** 66s against 47–50s for the same shape
means the split **restores most of what `bean:0029` cost and does not beat what came
before** — it lands 16 to 19 seconds above it. That is the same conclusion this bean drew
from the wrong numbers, now with runs behind it. Whether it settles `bean:0039` is
`bean:0039`'s call; what it rules out is settling it on the unsourced pair.

#### The measurement first written here measured the wrong thing

It cited run `33256522531` (pull request #22) at build 50s / frontend 1m32s / gate 4s. Both
halves ran in that one, because the change touched `.github/workflows/ci.yml` and the
classifier routes that to both by design. End to end it was 113s —
`gh run view 33256522531 --json createdAt,updatedAt` →
`created=2026-08-29T14:02:09Z updated=2026-08-29T14:04:02Z` — which is 8% off a 122s
baseline and 16% off the real 134s one. Either way a both-halves run cannot measure the
saving on a one-half change, and per-job durations from it cannot measure wall clock at all.

Requiring the `gate` check is `bean:0047` and is blocked on a human: the harness refuses
branch-protection edits to an agent.

### Criterion 3 — the asymmetry is stated, not glossed

`doc:00-constitution` §7.2.4 now reads: "**CI runs a subset of this per change, so the
promise is one-directional** … a green local `qualityCheck` **plus** `e2eTest` implies a
green CI run, and the reverse does not hold. The local command stays the superset
deliberately — the moment CI can run something local cannot, this promise is gone."
