---
# modus-0007
title: Coverage measurement and an exact per-module ratchet
status: in-progress
type: feature
priority: normal
created_at: 2026-08-29T00:00:00Z
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

## Decisions taken and their reasons

| decision | reason |
|---|---|
| JaCoCo 0.8.15, pinned | Gradle 9.7.1 defaults `toolVersion` to 0.8.13, which is only experimental on class file version 69. 0.8.14 is the first release with official Java 25 support |
| Kover rejected | JDK 25 support unverified; `#825` drops coverage on duplicate `project.name` in a multi-module merge; `#753` misreports inline functions |
| no exclude list | JaCoCo 0.8.15 filters the Kotlin synthetics itself. A hand-written list is a second copy that drifts |
| provisional code counted, not excluded | an exclusion outlives the placeholder and is invisible in the figure it changed. Counting it is what makes the ratchet fire today |
| exact bounds, not "may not increase" | a one-sided ratchet accumulates slack that a later regression spends silently |
| per-module, not aggregate, ratchet | an aggregate figure lets one module's integration test pay for another module's untested code |

## Follow-ups this bean carries

The remaining coverage row of `doc:35-testing#gaps`: a ratio floor, set from the recorded
baselines once every analysed module has a `src/test`. It is not invented before then.
