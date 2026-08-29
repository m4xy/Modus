---
# modus-0029
title: Put the backoffice and e2e checks inside the gate
status: in-progress
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

## Decisions taken

**No Spotless.** The choice was between adopting it — one formatter across Kotlin,
TypeScript, Markdown, YAML and JSON, as `doc:30-code-style` §1 used to describe — and
standardising on one tool per language. Standardised: ktlint already owns Kotlin through
`org.jlleitschuh.gradle.ktlint` and works; `backoffice/` already has Prettier with its own
config and works. Spotless would add a second Kotlin formatter beside ktlint and a second
Prettier configuration beside `backoffice/.prettierrc` — one fact in two places, which is
what `doc:05-authoring-for-agents#one-fact-one-place` forbids and what `bean:0028` found
this section had already produced once.

**knip is struck, not installed.** `doc:30-code-style` §6 claimed it was `error` in CI; it
was never a dependency, a script or a workflow step. Installing a tool to make a document
true is backwards — the claim was aspiration, so the row goes. If dead TypeScript becomes a
real problem, that is a bean with a reason behind it.

**`*.md`, `*.yaml` and `*.json` stay unformatted, and that is now stated as accepted rather
than pending.** `docs-lint` already checks what matters about Markdown here, and no rule in
the package depends on YAML or JSON layout.

**The backoffice does not become a Gradle project.** `settings.gradle.kts` stays the one
home for the module set. The checks are `Exec` tasks invoking the npm scripts the backoffice
already declares, so its build stays a normal front-end build that a front-end developer can
run without Gradle.

## Evidence

| # | criterion | observed |
|---|---|---|
| 1 | the formatter decision is taken and recorded | above, with the alternative and why it loses |
| 2 | `qualityCheck` runs `typecheck`, `lint` and `format:check`, and fails when any fails | three plants, below |
| 3 | `e2eTest` exists, runs Playwright, sits outside `check` | `./gradlew e2eTest` → 31 passed; `qualityCheck` does not depend on it |
| 4 | knip installed or struck | struck |
| 5 | the drifted files are reformatted in a commit of their own | first commit on the branch: 77 files, mechanical, `npm run format` output only |
| 6 | the `Enforcement gap:` lines become `Enforced by:` naming a mechanism observed rejecting a planted violation | `doc:00` §7.2.4, `doc:30` §1 and §6, `doc:80` step 6, `AGENTS.md` |

Criterion 2, planted one at a time against `backoffice/src/App.tsx` and reverted:

```
planted:  const planted: number = "not a number";
observed: src/App.tsx(104,7): error TS2322: Type 'string' is not assignable to type 'number'.
          > Task :backofficeTypecheck FAILED

planted:  const unused = 1
observed: 104:7  error  'unused' is assigned a value but never used  @typescript-eslint/no-unused-vars
          ✖ 1 problem (1 error, 0 warnings)
          > Task :backofficeLint FAILED

planted:  const   badly=  formatted   ;
observed: [warn] src/App.tsx
          [warn] Code style issues found in the above file. Run Prettier with --write to fix.
          > Task :backofficeFormatCheck FAILED
```

The drift this closes, measured before the reformat: `bean:0028` recorded 71 files failing
`format:check`; four commits later it was 77. Nothing had checked them since the backoffice
was written.

## What running it in CI found

Wiring the gate immediately surfaced a latent break that nothing could have caught before,
because nothing had ever run Playwright anywhere but a developer's machine.

```
observed: Error: Timed out waiting 120000ms from config.webServer.
          (no webServer stderr at all — it built and served, and the poll never connected)
```

`e2e/playwright.config.ts` polls `http://127.0.0.1:4173`, and `vite preview` binds to
`localhost`, which resolves to `::1` before `127.0.0.1` on the Ubuntu runner. On macOS both
resolve, so it passes locally and always would have. `preview` now binds `127.0.0.1`
explicitly.

Also corrected: the install step was `npx --prefix e2e playwright install`. `--prefix` is an
`npm` option, not an `npx` one — `npx` passes it through to `playwright`, so the version
installed was whatever `npx` resolved from the registry rather than the one `e2e` pins. It is
`npm --prefix e2e exec -- playwright install`, which resolves the pinned binary.

## Review cycle

Three blocking, six should-fix. All three plants reproduced exactly; the reviewer also ran
plant 1 through the **full** `qualityCheck` rather than the sub-task, which is the check I
should have run and did not.

| finding | resolution |
|---|---|
| **blocking** — CI red on the head: `Timed out waiting 120000ms from config.webServer`, with no webServer stderr because the config sets `stdout: 'ignore'` | fixed before the review landed: `vite preview` binds `localhost`, which resolves `::1` first on the runner, while the config polls `127.0.0.1`. A latent break in the e2e setup that only surfaced because something finally ran Playwright outside a laptop |
| **blocking** — `doc:30` §6 still carried the pre-change `Enforcement gap:`, now false, and my earlier edit had mangled its tail | replaced properly, and two **real** gaps stated in its place |
| **blocking** — `doc:00` §7.2.4 said "CI runs this task and no other Gradle invocation" while CI now runs two, and a "the gap below is why" sentence dangled after the gap it referred to was removed | rewritten: two invocations named, and the promise stated one-directionally — green local `qualityCheck` **plus** `e2eTest` implies green CI, and `qualityCheck` alone does not |
| `qualityCheck` never lints `e2e/` — `eslint .` runs with `workingDir = backoffice` and `e2e/` has no ESLint config | demoted to an `Enforcement gap:` naming `bean:0046`. My `Enforced by:` claimed both trees |
| **`jsx-a11y`, `import/no-cycle` and `no-non-null-assertion` are documented `error` and do not exist** — the same class as the `knip` row, and they survived that audit | second `Enforcement gap:`, `bean:0046` |
| coverage publishing was silently suppressed by an e2e failure — no `always()`, so GitHub ANDs an implicit `success()` | `always()` added; a Playwright failure no longer costs an unrelated signal |
| the artifact upload globs `**/build/reports/`, which matches nothing Playwright writes — so the one new failure mode was undiagnosable | `e2e/playwright-report/` and `e2e/test-results/` added |
| `npx --prefix` is not an npx option | `npm --prefix e2e exec --`, already fixed |

### The finding I would not have looked for

**Prettier's indent was coming from `.editorconfig`'s `[*]` block — a ktlint setting.**
`backoffice/.prettierrc.json` sets no `tabWidth`, and Prettier 3 reads `.editorconfig` by
default, so every TypeScript file was formatted at `indent_size = 4`.

```
cmd:      npx prettier --check src/App.tsx
observed: All matched files use Prettier code style!
cmd:      npx prettier --no-editorconfig --check src/App.tsx
observed: [warn] src/App.tsx
```

Three consequences. `doc:30-code-style` §6 documents "2-space indent", which was false and
which this bean was about to make *enforced* and false. The "no Spotless — the backoffice
already has Prettier with its own config" rationale was wrong about where the config lives.
And changing `indent_size` for ktlint would have invalidated every TypeScript file in the
gate.

Fixed at the right layer: `.editorconfig` gains `[*.{ts,tsx,css}] indent_size = 2`, beside
the `[*.{yml,yaml,json}]` block that was already there for the same reason.

**This collapses the reformat from 77 files and 5,883 lines to 9 files and 191.** The
"drift" `bean:0028` measured was real — `format:check` did fail — but the cause was a Kotlin
knob bleeding into TypeScript, not four months of neglect. The corrected figure is what this
bean should have reported, and the original claim is left above with this correction beside
it rather than edited away.
