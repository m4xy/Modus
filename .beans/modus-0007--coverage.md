---
# modus-0007
title: Coverage measurement and an exact per-module ratchet
status: completed
type: feature
priority: normal
created_at: 2026-08-29T00:00:00Z
updated_at: 2026-08-29T00:19:06Z
parent: modus-0006
---

# Coverage measurement and an exact per-module ratchet

Close the first row of `doc:35-testing#gaps`: measure coverage with JaCoCo over both test
suites, report it, and gate it with something that is non-vacuous on a repository where
eight of nine modules have no `src/test` and the domain model does not exist. The rule is
`doc:35-testing#coverage`; this bean is the work and the evidence.

## Scope

Owned: `build-logic/`, `gradle/libs.versions.toml`, `build.gradle.kts`,
`config/coverage/`, `.github/workflows/ci.yml`, `documentation/35-testing.md`. Not owned:
`backoffice/`, `e2e/`, every other document and bean, module `build.gradle.kts` files.

## The scheme, and why

A fixed threshold was rejected before it was written; `doc:35-testing#coverage` §8.2 is
the normative statement. The gate is an **exact per-module ratchet**:
`config/coverage/baseline.tsv` records missed instructions and missed branches per module,
and `coverageRatchet` sets JaCoCo's `minimum` *and* `maximum` to that one number. Missing more fails; missing less fails too, so the
record cannot go stale while the build is green and every movement in coverage is a
reviewable line in the diff. No number in the file was invented — `coverageBaselineWrite`
reads them out of the reports the ratchet itself reads.

Coverage is a complement to `doc:35-testing#load-bearing-evidence`, never a substitute:
an assertion-free test raises coverage and proves nothing. Stated in §8.

## Success criteria and evidence

### 1. JaCoCo is wired through the convention plugins, pinned, over both suites

`modus.kotlin-base` applies `jacoco`, pins `toolVersion` from the catalog, and registers
`coverageReport` over `test.exec` + `integrationTest.exec`. `modus.coverage` (root)
registers the aggregate. No module `build.gradle.kts` changed.

```
cmd:      grep -rn "jacoco" --include=build.gradle.kts core adapters modules app
observed: (no output)

cmd:      ./gradlew --no-build-cache qualityCheck
observed: > Task :core-domain:coverageReport
          > Task :core-domain:coverageRatchet
          > Task :modus-server:coverageReport
          > Task :modus-server:coverageRatchet
          > Task :coverageAggregateReport
          > Task :qualityCheck
          BUILD SUCCESSFUL
```

### 2. The reports carry real counters, not 0/0

Read off the bundle totals of the generated XML — the last `<counter>` elements in each
report. `:core-domain` has a unit test, `:modus-server` an integration test,
`:adapter-rest` neither.

```
cmd:      # bundle-level counters, module reports
observed: core/core-domain     INSTRUCTION missed=0 covered=33   LINE 0/8   METHOD 0/2   CLASS 0/1
          app/modus-server     INSTRUCTION missed=9 covered=3    LINE 2/1   METHOD 1/1   CLASS 1/1
          adapters/adapter-rest INSTRUCTION missed=3 covered=0   LINE 1/0   METHOD 1/0   CLASS 1/0

cmd:      # bundle-level counters, build/reports/jacoco/aggregate/coverage.xml
observed: <counter type="INSTRUCTION" missed="36" covered="129"/>
          <counter type="LINE" missed="8" covered="28"/>
          <counter type="COMPLEXITY" missed="7" covered="19"/>
          <counter type="METHOD" missed="7" covered="19"/>
          <counter type="CLASS" missed="3" covered="12"/>
```

`covered=33` is the proof the agent attached and the exec data was read. The aggregate
reads higher than the sum of the module figures because `:modus-server`'s integration test
loads other modules' classes through the Spring context; that data is deliberately not
credited to those modules' ratchets (`doc:35-testing#coverage` §8.4).

`:adapter-rest` reporting 3 missed / 0 covered rather than being skipped is the empty
`.exec` seed working: without it a module with no tests produces no report at all and the
ratchet would be vacuous on exactly the eight modules that need it most.

### 3. The gate fails when coverage drops — weakened test

`:core-domain`'s single assertion replaced by `assertEquals(6, 6)`, so `BoundedContexts`
is never loaded. Reverted after observation.

```
cmd:      ./gradlew --no-build-cache :core-domain:coverageRatchet
observed: FAILURE: Build failed with an exception.

          * What went wrong:
          Execution failed for task ':core-domain:coverageRatchet' (registered by plugin 'modus.kotlin-base').
          > A failure occurred while executing org.gradle.internal.jacoco.AntJacocoCheck
             > Rule violated for bundle core-domain: instructions missed count is 33, but expected maximum is 0
post:     test restored; BUILD SUCCESSFUL
```

### 4. The gate fails when uncovered code is added — through `qualityCheck`

One uncovered public function planted in `BoundedContexts`, nothing else changed. Run
through the documented aggregate command, not the individual task, so this is what CI
would see. Reverted after observation.

```
planted:  public fun isKnown(name: String): Boolean = names.any { it.equals(name, ignoreCase = true) }
cmd:      ./gradlew --no-build-cache qualityCheck
observed: FAILURE: Build failed with an exception.

          * What went wrong:
          Execution failed for task ':core-domain:coverageRatchet' (registered by plugin 'modus.kotlin-base').
          > A failure occurred while executing org.gradle.internal.jacoco.AntJacocoCheck
             > Rule violated for bundle core-domain: instructions missed count is 10, but expected maximum is 0
post:     function deleted; BUILD SUCCESSFUL
```

### 5. The gate fails when the baseline goes stale — the other direction

`:core-domain`'s row raised from `0` to `5` with no source change, standing in for a
commit that improves coverage without lowering its record. Reverted after observation.

```
cmd:      ./gradlew --no-build-cache :core-domain:coverageRatchet
observed: Execution failed for task ':core-domain:coverageRatchet' (registered by plugin 'modus.kotlin-base').
          > A failure occurred while executing org.gradle.internal.jacoco.AntJacocoCheck
             > Rule violated for bundle core-domain: instructions missed count is 0, but expected minimum is 5
post:     row restored to 0; BUILD SUCCESSFUL
```

### 6. A module with no recorded row has no gate, and that is caught

`:module-cost`'s row deleted, standing in for a module added to `settings.gradle.kts` and
forgotten. Reverted after observation.

```
cmd:      ./gradlew --no-build-cache coverageBaselineIsComplete :module-cost:coverageRatchet
observed: Execution failed for task ':coverageBaselineIsComplete' (registered by plugin 'modus.coverage').
          > .../config/coverage/baseline.tsv records 8 module(s) [:adapter-agent-claude,
            :adapter-persistence-flatfile, :adapter-rest, :adapter-vcs-git, :core-application,
            :core-domain, :module-beans, :modus-server], but the modules with production code are
            [:adapter-agent-claude, :adapter-persistence-flatfile, :adapter-rest, :adapter-vcs-git,
            :core-application, :core-domain, :module-beans, :module-cost, :modus-server]. Every one
            of them needs a row, and no other row may exist: a module with no recorded figure has no
            ratchet. Run ./gradlew coverageBaselineWrite.
post:     ./gradlew coverageBaselineWrite; row restored; BUILD SUCCESSFUL
```

### 7. One command, no drift

`coverageRatchet` is reached by each module's `check`; `coverageAggregateReport` and
`coverageBaselineIsComplete` by the root project's `check`, which `qualityCheck` now
depends on. `rule:ci/build` gains no Gradle invocation — only a reporting step that
consumes the XML `qualityCheck` already produced.

### 8. The regime is written down

`doc:35-testing#coverage`: the scheme, its enforcing tasks, the rejected alternatives, the
treatment of provisional code, and the attribution rule. The coverage row of
`doc:35-testing#gaps` is replaced: what remains open is a ratio floor, with its closing
condition stated.

### 9. The gate is green

```
cmd:      ./gradlew clean && ./gradlew --no-build-cache qualityCheck
observed: > Task :modus-server:coverageReport
          > Task :modus-server:coverageRatchet
          > Task :modus-server:check
          > Task :coverageAggregateReport
          > Task :check

          > Task :docsLint
          docs-lint: OK — 15 documents, 81 anchors, 234 references.

          > Task :qualityCheck

          BUILD SUCCESSFUL in 8s
          153 actionable tasks: 144 executed, 9 up-to-date
```

## Review cycle — PR #8

Seven threads. Six are fixed here; thread 5 (the `beans/` → `.beans/` rebase) is
deliberately left open, because PR #7 performs that migration and is still running its own
fix cycle — rebasing now would race it, and an unresolved thread is what correctly blocks
this branch from merging before #7.

### R1. `coverageBaselineWrite` was an unguarded downward ratchet

The reset task was freely runnable, so a regression could be recorded as a one-line diff
indistinguishable from the improvement with its digits swapped. The writer now reads the
existing row, prints the per-module delta, and refuses to raise a missed count without an
explicit opt-out; the reason is written into the baseline so the regression is
self-documenting in the same diff.

```
planted:  :core-domain's single assertion weakened to assertEquals(6, 6)
cmd:      ./gradlew --no-build-cache coverageBaselineWrite
observed: coverageBaselineWrite: missed instructions, missed branches, covered instructions, covered branches
            :core-domain                   0 0 33 0 -> 33 0 0 0  <-- REGRESSION
          > Task :coverageBaselineWrite FAILED
          > coverageBaselineWrite refuses to record worse coverage: :core-domain (missed
            instructions 0 -> 33). Restore the coverage, or re-run with
            -Pcoverage.regress=<reason>; the reason is written into the baseline and
            belongs in the pull request body too.

cmd:      ./gradlew --no-build-cache coverageBaselineWrite \
            "-Pcoverage.regress=proving the opt-out; reverted immediately"
observed: BUILD SUCCESSFUL, and in config/coverage/baseline.tsv:
          # REGRESSION accepted with -Pcoverage.regress: proving the opt-out; reverted immediately
          #   :core-domain: missed instructions 0 -> 33
          :core-domain                   33	0	0	0
post:     test restored; coverageBaselineWrite rewrote :core-domain 33 0 0 0 -> 0 0 33 0,
          the note gone with it; BUILD SUCCESSFUL
```

### R2. The rule bounded `MISSEDCOUNT` only

Deleting fully covered production code left the missed count exactly where the row said
and passed silently while the ratio fell. The baseline gained covered instructions and
covered branches, and `coverageRatchet` pins both with `COVEREDCOUNT`. `doc:35-testing`
§8.2's claim that "every movement is a line in the diff" is now true rather than an
overstatement; §8.1 states the covered half explicitly.

```
planted:  one element deleted from BoundedContexts.names, the test adjusted to expect 5,
          so the missed count is unchanged at 0 and only the covered count falls
cmd:      ./gradlew --no-build-cache :core-domain:coverageRatchet
observed: > Task :core-domain:coverageRatchet FAILED
          > Rule violated for bundle core-domain: instructions covered count is 29, but
            expected minimum is 33
post:     element and test restored; BUILD SUCCESSFUL
```

### R3. The per-module exec set diverged from the aggregate

The gate named `test.exec` and `integrationTest.exec` literally while the aggregate
globbed `*.exec`, so a third suite would be counted in the report and not in the gate. The
per-module set is now the same glob, and the task dependencies are derived from
`tasks.withType<Test>()` rather than named.

```
planted:  a throwaway third suite `smokeTest` in modus.kotlin-base, plus one smoke test
          against :module-cost, whose row records 6 missed instructions and 0 covered
cmd:      ./gradlew --no-build-cache :module-cost:coverageRatchet
observed: > Task :module-cost:smokeTest
          > Task :module-cost:coverageRatchet FAILED
          > Rule violated for bundle module-cost: instructions missed count is 3, but
            expected minimum is 6
            Rule violated for bundle module-cost: instructions covered count is 3, but
            expected maximum is 0

cmd:      # the literal two-name set restored, nothing else changed
          ./gradlew --no-build-cache :module-cost:coverageRatchet --rerun
observed: > Task :module-cost:smokeTest UP-TO-DATE
          > Task :module-cost:coverageRatchet
          BUILD SUCCESSFUL          <-- the suite ran and escaped the gate
post:     glob restored; suite and smoke test removed
```

### R4. A third-party action on a mutable tag with `pull-requests: write`

`madrapps/jacoco-report@v1.8.0` is the only reason the job holds `pull-requests: write`,
and a tag its publisher can move onto arbitrary code. It is pinned to the immutable commit
behind v1.8.0. Audit of the other four: `actions/checkout`, `actions/setup-java` and
`actions/upload-artifact` are GitHub's own; `gradle/actions/setup-gradle` is not — it is
published by the Gradle organisation, so `v4` is equally movable and it is pinned too, at
exactly the commit `v4` resolves to today.

```
cmd:      gh api repos/madrapps/jacoco-report/commits/v1.8.0 --jq .sha
observed: e51ce1f46f7f8b5331593f935e59cbaf44b84920

cmd:      gh api repos/gradle/actions/commits/v4 --jq .sha
observed: ed408507eac070d1f99cc633dbcf757c94c7933a   (tags v4, v4.4.3)

pinned:   uses: madrapps/jacoco-report@e51ce1f46f7f8b5331593f935e59cbaf44b84920 # v1.8.0
          uses: gradle/actions/setup-gradle@ed408507eac070d1f99cc633dbcf757c94c7933a # v4.4.3
```

### R6. The writer parsed XML by regex and declared no inputs or outputs

The pattern demanded an exact attribute order and a self-closing tag, and its failure mode
was `?: "0"` — a parse failure indistinguishable from genuine zero coverage, which would
have silently reset the baseline. It now parses with the JDK's `DocumentBuilderFactory`
(no new dependency, external DTD loading off) and reads the `<counter>` elements that are
direct children of `<report>`, failing loudly and naming the file when a report carries
none. The task declares `inputs.files(reports.values)`, `inputs.property` for the regress
opt-out, and `outputs.file(target)`, so it is no longer always out of date. The
cross-project configuration-time reads in `modus.coverage` are kept — they are what makes
the aggregate work today — but are now headed by a `PROJECT ISOLATION:` comment naming the
constraint and the migration, so the eventual isolation work finds it.

### R7. The branch column had never fired, and the baseline is boilerplate

Branch coverage is measured, not merely declared:

```
planted:  isKnown(name) = name.isNotEmpty() && names.contains(name) in BoundedContexts,
          with tests exercising all four condition outcomes
cmd:      ./gradlew --no-build-cache coverageBaselineWrite
observed: :core-domain                   0 0 33 0 -> 0 0 49 6

planted:  the assertFalse(isKnown("")) assertion removed
cmd:      ./gradlew --no-build-cache :core-domain:coverageRatchet
observed: > Task :core-domain:coverageRatchet FAILED
          > Rule violated for bundle core-domain: branches missed count is 2, but expected
            maximum is 0
            Rule violated for bundle core-domain: branches covered count is 4, but
            expected minimum is 6
post:     function and tests removed; baseline back to :core-domain 0 0 33 0
```

The reviewer's second point is recorded in `doc:35-testing#coverage` §8.3 rather than
argued away: three rows are `33 0 0 0` and three are `6 0 0 0`, so the day the placeholders
are replaced every row moves at once and review becomes rubber-stamping. R1's guard is what
makes that day safe.

### R5. Sequenced behind PR #7, then resolved

The `beans/` → `.beans/` move waited for PR #7 to merge; the thread stayed unresolved so
the sequencing was enforced by the merge button rather than by memory. Corrected on
closure: the rebase landed before this branch merged, so this work item was added by the
merge commit directly at `.beans/modus-0007--coverage.md` and `beans/0007-coverage.md`
never existed on `main`.

### The gate after the cycle

```
cmd:      ./gradlew clean && ./gradlew --no-build-cache qualityCheck
observed: > Task :docsLint
          docs-lint: OK — 15 documents, 81 anchors, 234 references.
          > Task :coverageAggregateReport
          > Task :qualityCheck
          BUILD SUCCESSFUL
```

## Decisions taken and their reasons

| decision | reason |
|---|---|
| JaCoCo 0.8.15, pinned | Gradle 9.7.1 defaults `toolVersion` to 0.8.13, which is only experimental on class file version 69. 0.8.14 is the first release with official Java 25 support |
| Kover rejected | JDK 25 support unverified; `#825` drops coverage on duplicate `project.name` in a multi-module merge; `#753` misreports inline functions |
| no exclude list | JaCoCo 0.8.15 filters the Kotlin synthetics itself. A hand-written list is a second copy that drifts |
| provisional code counted, not excluded | an exclusion outlives the placeholder and is invisible in the figure it changed. Counting it is what makes the ratchet fire today |
| exact bounds, not "may not increase" | a one-sided ratchet accumulates slack that a later regression spends silently |
| per-module, not aggregate, ratchet | an aggregate figure lets one module's integration test pay for another module's untested code |
| covered counts recorded too, not only missed | `MISSEDCOUNT` alone pins the uncovered surface. Deleting covered code leaves it untouched while the ratio falls |
| the writer refuses a downward write | a reset task anyone can run is not a ratchet. `-Pcoverage.regress=<reason>` makes the regression deliberate, named, and recorded in the file |
| the exec set is a glob, not a list of suite names | the aggregate already globs. A literal list lets a third suite be reported and not gated, with no error anywhere |
| XML parsed with the JDK parser, not a regex | the regex's failure mode was a silent `0`, indistinguishable from genuine zero coverage |
| third-party actions pinned to commits | `madrapps/jacoco-report` is why the job holds `pull-requests: write`; a movable tag would run arbitrary code with it |

## Follow-ups this bean carries

The remaining coverage row of `doc:35-testing#gaps`: a ratio floor, set from the recorded
baselines once every analysed module has a `src/test`. It is not invented before then.

## Summary of Changes

Merged as PR #8 (`d9293fd`). JaCoCo 0.8.15 is applied by `modus.kotlin-base` and
`modus.coverage`, measured over both suites, and gated by `coverageRatchet` against
`config/coverage/baseline.tsv` — one row per module, all four figures exact bounds in
both directions. `coverageBaselineWrite` regenerates the file and refuses a downward
write without `-Pcoverage.regress=<reason>`; `coverageBaselineIsComplete` refuses a
missing or extra row. `doc:35-testing#coverage` is the rule.

Every criterion above was re-checked against `main` at closure and each still holds. The
one drift is R5, corrected in place.

Carried forward: the coverage **ratio** floor, first row of `doc:35-testing#gaps`, which
stays open until every analysed module has a `src/test`.
