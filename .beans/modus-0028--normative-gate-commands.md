---
# modus-0028
title: Correct the normative gate to the commands that exist
status: completed
type: task
priority: high
order: A
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T12:55:02Z
---

# Correct the normative gate to the commands that exist

`doc:00-constitution` §7.2.4 is **the gate, stated once**: `doc:30-code-style` §6 and
`doc:80-agent-operating-procedure` step 6 both defer to it and deliberately carry no
command list of their own. All three of its commands are wrong, and the first thing every
agent does on every work item is run them.

Found by running it, not by reading it — which is `doc:00-constitution#observed-failing`
working as intended, one step later than it should have.

```
cmd:      ./gradlew spotlessApply check
observed: FAILURE: Build failed with an exception.
          * What went wrong:
          Selection failed
            Task 'spotlessApply' not found in root project 'modus' and its subprojects.
```

## What is claimed, and what the build does

| # | claimed | claimed in | what the build does |
|---|---|---|---|
| 1 | `./gradlew spotlessApply` / `spotlessCheck` | `doc:00` §7.2.4, `doc:30` §1 (×4), `doc:80` §5.5, §6 | no Spotless plugin exists anywhere; `build-logic/src/main/kotlin/modus.kotlin-base.gradle.kts:16` applies `org.jlleitschuh.gradle.ktlint`, so the formatter is `ktlintFormat` |
| 2 | `./gradlew check` also runs the backoffice checks | `doc:00` §7.2.4, `doc:30` §0, §6, `doc:80` §6 | `backoffice/` and `e2e/` are not Gradle projects — `settings.gradle.kts` declares ten modules and neither is among them. No Gradle task invokes npm |
| 3 | `./gradlew e2eTest` | `doc:00` §7.2.4 | the task does not exist; nothing runs Playwright, locally or in CI |
| 4 | "CI runs `check` and `e2eTest` with no extra arguments" | `doc:00` §7.2.4, `doc:30` §0 | `.github/workflows/ci.yml:73` runs `./gradlew qualityCheck --stacktrace` and nothing else |
| 5 | `./gradlew build ktlintCheck detekt` is "exactly what CI runs" | `AGENTS.md` Commands | the command itself **passes** — unqualified selection matches the per-module `detekt` tasks. Only the claim is false: CI runs `qualityCheck`, and `build ktlintCheck detekt` skips `docsLint`, the root `ktlintCheck`, the aggregate coverage report and the included build's own gates |
| 6 | `./gradlew :core:core-domain:check` | `doc:80` §5.4 **and `doc:20` §10** | project names are flat (`settings.gradle.kts`): the path is `:core-domain:check`, and the stated one fails with `project 'core' is ambiguous`. Two occurrences, identically worded; the first sweep found only one, and review found the second |
| 7 | `knip` is `error` in CI | `doc:30` §6 | knip is not a dependency, a script or a workflow step anywhere in the repository |
| 8 | "backoffice tests" run inside `check` | `doc:00` §7.2.4 | `backoffice/package.json` has no `test` script; only `e2e/` has one, and it is Playwright |
| 9 | Spotless also formats `*.ts`, `*.md`, `*.yaml`, `*.json` | `doc:30` §1 | no such mechanism; `backoffice/package.json` carries its own `format`/`format:check` Prettier scripts, invoked by nobody |
| 10 | — | — | consequence of #2: with nothing running them, `npm --prefix backoffice run format:check` **fails today** on 71 pre-existing files. `typecheck` and `lint` still exit 0. Reformatting them is `bean:0029`, not this bean |

## Scope

Owned: `documentation/00-constitution.md` §7.2.4 and §7.3's surrounding block,
`documentation/20-ddd-practices.md` §10's checklist, `documentation/30-code-style.md` §0,
§1, §3 and §6, `documentation/80-agent-operating-procedure.md` §5.4, §5.5 and step 6,
`AGENTS.md`'s Commands block, and this bean.

Not owned, and deliberately not touched: `build-logic/`, `settings.gradle.kts`,
`.github/workflows/ci.yml`, `backoffice/**`, `e2e/**`. Making the build do what the
documents claimed is `bean:0029`; this bean only stops the documents claiming it.

The direction is set by `doc:00-constitution#observed-failing`: a mechanism that cannot be
made to fire is demoted to an `Enforcement gap:` naming the bean that closes it. It is not
this bean's business to decide whether Modus adopts Spotless — only to stop the
constitution instructing agents to run a task that is not there.

## Success criteria and evidence

1. Every command named in `documentation/**` and `AGENTS.md` exists and exits 0 when run
   against a clean tree. Evidence: each one run, output recorded verbatim.
2. §7.2.4 remains the single normative statement of the gate — `doc:30` §6 and `doc:80`
   step 6 still cite it and still carry no command list.
3. Rows 2, 3, 7, 8 and 9 above become `Enforcement gap:` lines naming `bean:0029`, rather
   than being deleted. An admitted gap keeps someone looking; a deletion does not.
4. `bash tools/docs-lint.sh` passes, and the line budget of `adr:0003` still holds.
5. `./gradlew qualityCheck` is green.
6. The backoffice checks this bean tells agents to run by hand are stated at their real
   strength: `typecheck` and `lint` pass, `format:check` does not, and the document says so
   rather than sending the next agent into a 71-file diff it did not cause. The instruction
   is runnable from a clean checkout — it names the `npm ci` step, without which `typecheck`
   fails with `sh: tsc: command not found`.
7. The sweep is exhaustive rather than sampled: every backticked `./gradlew` and `npm`
   invocation in `documentation/**` and `AGENTS.md` is enumerated and run, not just the ones
   in the gate block. Review found a second `:core:core-domain:check` the first pass missed;
   this criterion exists so that cannot recur silently.

## Summary of Changes

Merged as PR #12 (`5f8b569`). `doc:00-constitution` §7.2.4's gate is `ktlintFormat` then
`qualityCheck` — the commands that exist and the task CI actually runs. Ten divergences are
tabulated above; the ones needing build work are `Enforcement gap:` lines naming `bean:0029`
rather than deletions, per `doc:00-constitution#observed-failing`.

Review found the sweep incomplete: `doc:20-ddd-practices` §10 carried the same broken
`:core:core-domain:check` path in identical wording, in a document the first pass never
opened — so criterion 1 was false when written. Criterion 7 was added to make the sweep
exhaustive rather than sampled. Row 5 was narrowed after review showed
`./gradlew build ktlintCheck detekt` actually passes; only the claim that it is what CI runs
was false.
