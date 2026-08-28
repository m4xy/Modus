---
title: Test taxonomy — unit/integration split, enforced mechanically
status: in-progress
type: feature
priority: high
created_at: 2026-08-29T00:00:00Z
---

# Test taxonomy — unit/integration split, enforced mechanically

Divide Modus's tests into unit/acceptance and integration by a mechanism that makes
misclassification impossible rather than discouraged. The rule is `doc:35-testing`; this
bean is the work and the evidence.

## Scope

Owned: `build-logic/`, `gradle/libs.versions.toml`, module `build.gradle.kts` files,
`architecture-tests/`, `.github/workflows/ci.yml`, `documentation/35-testing.md`.

Not owned: `backoffice/`, `e2e/`, every other `documentation/*.md`, `AGENTS.md`,
`CLAUDE.md`, `settings.gradle.kts` — a concurrent work stream holds those. Deferred:
JaCoCo coverage — a gate introduced alongside the taxonomy would be calibrated to whatever
the current tests happen to reach. First row of `doc:35-testing#gaps`.

## Success criteria and evidence

### 1. Two suites exist, both wired into the gate

`test` (unit/acceptance) and `integrationTest`, declared once in `modus.kotlin-base` via
the JVM Test Suite plugin, both reached by `check` and therefore by `qualityCheck`.

```
cmd:      ./gradlew :core-domain:check :modus-server:check --rerun-tasks
observed: > Task :core-domain:test
          > Task :core-domain:check
          > Task :modus-server:integrationTest
          > Task :modus-server:check
          BUILD SUCCESSFUL in 8s
```

`:modus-server:test` and `:core-domain:integrationTest` are both `NO-SOURCE` — the split
working. The only Spring-context test moved from `src/test` to `src/integrationTest`.

### 2. Spring is absent from the unit-test classpath

```
cmd:      ./gradlew :modus-server:dependencies --configuration testCompileClasspath
observed: kotlin-stdlib, project ':core-domain', project ':core-application',
          project ':adapter-rest', kotlin-test-junit5:2.4.10,
          kotest-assertions-core-jvm:6.2.4 — no org.springframework* entry

cmd:      ./gradlew :modus-server:compileTestKotlin   # planted @SpringBootTest unit test
observed: e: .../src/test/kotlin/uk/m4xy/modus/app/PlantedViolationTest.kt:3:12
             Unresolved reference 'springframework'.
          Execution failed for task ':modus-server:compileTestKotlin'.

cmd:      ./gradlew :modus-server:assertUnitTestClasspathIsSpringFree
          # with Spring temporarily re-admitted to testImplementation
observed: Spring is on the unit-test compile classpath of :modus-server:
          [org.springframework.boot:spring-boot, ... org.springframework:spring-webmvc].
          Unit tests may not see Spring; move the test to src/integrationTest,
          or add the group to the exclusions in modus.spring-module.
```

### 3. Every test-purity rule has been observed to fail

One planted violation per rule, at a real call site, reverted after observation.
`const val` references are inlined by Kotlin and leave no trace, so nothing was
planted at a constant.

| rule | planted | observed |
|---|---|---|
| `unitTestsDoNotDependOnSpring` | `@SpringBootTest` unit test, Spring temporarily re-admitted | `Rule 'no classes that unit-test classes should depend on classes that reside in any package ['org.springframework..']' was violated (4 times)` |
| `unitTestsAreNotSpringBootTests` | same | `Rule 'no classes that unit-test classes should be annotated with @SpringBootTest' was violated (1 times)` |
| `nothingSleepsTheThread` | `Thread.sleep(1L)` in a `core-domain` unit test | `Method <...PlantedViolationTest.planted violation()> calls method <java.lang.Thread.sleep(long)>` |
| `unitTestsDoNotTouchTheFilesystemOrTheNetwork` | `Path.of`, `Files.exists`, `URI.create` in a `core-domain` unit test | `Rule '... should depend on classes that reside in any package ['java.nio.file..', 'java.net..']' was violated (4 times)` |
| `disabledCarriesAWorkItem` | `@Disabled("flaky")` | `@Disabled("flaky") must open with a beans/NNNN reference and a reason` |
| `disabledCarriesAWorkItem` (negative) | `@Disabled("beans/0006: planted while proving the rule fires")` | `BUILD SUCCESSFUL` |
| `everyUnitTestPackageIsAnalysed` | unit-test jars removed from the analysis classpath | `no unit-test bytecode was imported for: [uk.m4xy.modus.core.domain] (imported 0 unit-test packages). The test-purity rules would pass vacuously.` |

The last row is the one that matters: without it every `noClasses(...)` rule passes on an
empty set. `ImportOption.DoNotIncludeTests` is not used here for that reason
(`doc:35-testing#purity-rules`).

### 4. Nothing hardcodes a module list

`architecture-tests/build.gradle.kts` derives the analysed projects, the unit-test
classpath and both guard manifests from `rootProject.subprojects`. A module added to
`settings.gradle.kts` is analysed, or a guard fails.

### 5. Assertions pinned, no second runner

`kotest-assertions-core-jvm` 6.2.4 in `gradle/libs.versions.toml`, on both suites. No
`junit-platform` artifact and no engine enters with it — criterion 2's output.

### 6. The regime is written down

`documentation/35-testing.md`: definitions and their enforcing mechanism, load-bearing
evidence, the rejection of mutation testing as a CI gate with its four reasons, and every
gap marked `Enforcement gap:` — coverage first among them.

### 7. The gate is green

```
cmd:      ./gradlew clean && ./gradlew --no-build-cache qualityCheck
observed: > Task :modus-server:check
          > Task :qualityCheck
          BUILD SUCCESSFUL in 7s
          123 actionable tasks: 115 executed, 8 up-to-date
```

## Follow-ups this bean carries

Every `Enforcement gap:` in `doc:35-testing` names this bean. Each becomes its own work
item when picked up: JaCoCo coverage (separate pull request); publishing and analysing
integration-test bytecode; a duration budget for the unit suite; a mechanical check that
`rule:ci/build` runs exactly the documented aggregate task; a PR-body checker for the
`verify` block, part of the `docs-lint` step in `bean:0004`.
