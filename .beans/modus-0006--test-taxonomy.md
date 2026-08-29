---
# modus-0006
title: Test taxonomy — unit/integration split, enforced mechanically
status: completed
type: feature
priority: high
created_at: 2026-08-29T00:00:00Z
parent: modus-0003
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
| `unitTestsDoNotTouchTheFilesystemOrTheNetwork` | `Path.of`, `Files.exists`, `URI.create` in a `core-domain` unit test | `Rule '... should depend on classes that reside in any package ['java.nio.file..', 'java.net..']' was violated (4 times)` — extended to `java.io..` in review cycle 1, re-proved below |
| `unitTestsDoNotStartProcesses` | added in review cycle 1 | see below |
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
evidence, the rejection of mutation testing as a CI gate, and every gap marked
`Enforcement gap:` — coverage first among them.

### 7. The gate is green

```
cmd:      ./gradlew clean && ./gradlew --no-build-cache qualityCheck
observed: > Task :modus-server:check
          > Task :qualityCheck
          BUILD SUCCESSFUL in 7s
          123 actionable tasks: 115 executed, 8 up-to-date
```

## Review cycle 1 — PR #6

Six threads. Each is recorded with the fix and the evidence that the fix does what it
claims, per `doc:35-testing#load-bearing-evidence`.

### R1. The Spring guard was a denylist and the classpath was half-stripped

Gradle's `exclude(group =)` matches a group **exactly**, so excluding `org.springframework*`
left `:adapter-rest`'s unit-test classpath carrying `org.springdoc`, `io.swagger`,
`jakarta.validation` and five Jackson artifacts. A unit test importing them compiled and
passed, then died at run time with `NoClassDefFoundError: org/springframework/util/Assert`.

`assertUnitTestClasspathIsSpringFree` is now an **allowlist** over resolved artifact
groups: `org.jetbrains.kotlin`, `org.jetbrains.kotlinx`, `org.jetbrains`, `org.jspecify`,
`org.junit`, `org.junit.jupiter`, `org.junit.platform`, `org.opentest4j`,
`org.apiguardian`, `io.kotest`, `io.github.java-diff-utils`, `com.tngtech.archunit`,
`org.slf4j`. The exclusion list gained `org.springdoc` and moved to `modus.kotlin-base`.

```
cmd:      ./gradlew :adapter-rest:compileTestKotlin
          # PlantedSpringAdjacentTest importing io.swagger.v3.oas.annotations.Operation,
          # jakarta.validation.constraints.NotNull, org.springdoc.core.models.GroupedOpenApi
pre:      BUILD SUCCESSFUL in 9s        (before the fix — the defect)
observed: e: .../PlantedSpringAdjacentTest.kt:4:8 Unresolved reference 'jakarta'.
          e: .../PlantedSpringAdjacentTest.kt:5:12 Unresolved reference 'springdoc'.
          e: .../PlantedSpringAdjacentTest.kt:11:28 Unresolved reference 'Operation'.
          e: .../PlantedSpringAdjacentTest.kt:11:46 Unresolved reference 'NotNull'.
          e: .../PlantedSpringAdjacentTest.kt:11:62 Unresolved reference 'GroupedOpenApi'.
          Execution failed for task ':adapter-rest:compileTestKotlin'.
          BUILD FAILED in 17s
post:     planted test deleted; ./gradlew assertUnitTestClasspathIsSpringFree
          BUILD SUCCESSFUL — all ten modules
```

### R2. The filesystem rule covered a third of what it claimed

`java.io..` added, and `rule:archunit/unitTestsDoNotStartProcesses` added — `ProcessBuilder`
and `Runtime` are in `java.lang`, so no package ban reaches them. `doc:35-testing` §1's
`Enforced by:` for "starts a process" was false until this rule existed.

```
planted:  File.createTempFile("planted", ".txt") + writeText + readText + delete
observed: Rule 'no classes that unit-test classes should depend on classes that reside in
          any package ['java.io..', 'java.nio.file..', 'java.net..'], because real I/O
          belongs in src/integrationTest, where it is allowed to be slow' was violated
          (2 times):
          Method <...PlantedIoTest.planted java io filesystem access()> calls method
          <java.io.File.createTempFile(java.lang.String, java.lang.String)> in (PlantedIoTest.kt:9)

planted:  ProcessBuilder("/bin/echo", "planted").start()
observed: Rule 'no classes that unit-test classes should call method where a
          process-starting method, ...' was violated (1 times):
          Method <...PlantedIoTest.planted process start()> calls method
          <java.lang.ProcessBuilder.start()> in (PlantedIoTest.kt:8)

planted:  Runtime.getRuntime().exec(arrayOf("/bin/echo", "planted"))
observed: Method <...PlantedIoTest.planted runtime exec()> calls method
          <java.lang.Runtime.exec([Ljava.lang.String;)> in (PlantedIoTest.kt:8)

post:     planted test deleted; ./gradlew :architecture-tests:test --rerun-tasks
          BUILD SUCCESSFUL
```

### R3. §3 described an asymmetry the code never had

The document claimed `testCompileClasspath` excluded one group and `testRuntimeClasspath`
another. The code excluded both from both. §3 now shows what the code does, and the guard
inspects **both** configurations rather than only `testCompileClasspath` — a type absent at
compile time but present at run time is still reachable reflectively.

The cut also moved from `modus.spring-module` to `modus.kotlin-base`: `:architecture-tests`
is not a Spring module but puts every other module on its test classpath, and its
`testRuntimeClasspath` carried the entire Spring runtime graph — `spring-core`,
`spring-webmvc`, tomcat, hibernate-validator, logback. It is now stripped like every other
module, and the allowlist guard passes on it unchanged.

### R4/R5. Three of the four mutation-testing reasons did not survive checking

Verified against primary sources rather than restated. §7 now rests on `#402` alone.

| claim | verdict | source |
|---|---|---|
| `#402` cross-subproject mutation broken on Gradle 9 | **stands**, restated | `szpak/gradle-pitest-plugin` issue 402, open, opened by the maintainer from his own PoC branch's `AcceptanceTestsInSeparateSubprojectFunctionalSpec` |
| `#401` incremental history disabled on JDK 25 | **collapses** | `#401` is an unmerged draft PR about Gradle 9.4/JDK 25 build modernisation; `#399` records `PIT 1.22.1 (ASM 9.9.1) works correctly with historyInputLocation on JDK 25` |
| PIT declines to support Kotlin | **overstated** | hcoles authored Apache-2.0 `pitest/pitest-kotlin`; `pitest#1347` merged Kotlin source dirs. True narrower claim: that plugin is archived since 2023 and Arcmutate's Kotlin integration is licensed |
| the sealed-`when` `else` yields an unkillable mutant | **overstated, self-cancelling** | Arcmutate kotlin-plugin 1.1.2/1.2.1: the mutants come from the compiler's redundant final-branch equality and null checks, and both are now suppressed |

"Eleven subprojects" corrected to **ten** (`settings.gradle.kts`). The three failed reasons
are kept in §7 as a correction table rather than deleted, so nobody resurrects them.

### R6. The purity rules currently guard one package

Eight of the nine analysed modules have no `src/test`, so `unit-test-packages.txt` has one
line, `uk.m4xy.modus.core.domain`. Recorded as the first row of `doc:35-testing#gaps` with
its closing condition — it self-closes as modules gain tests, because the analysed
classpath and the manifest are both derived, but that had to be stated rather than assumed.

### The gate after the cycle

```
cmd:      ./gradlew clean && ./gradlew --no-build-cache qualityCheck
observed: > Task :modus-server:integrationTest
          > Task :modus-server:check
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
