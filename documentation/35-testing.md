---
id: doc:35-testing
title: Test taxonomy and evidence
status: active
superseded_by: null
read_when:
  - path: "**/src/test/**"
  - path: "**/src/integrationTest/**"
  - path: architecture-tests/**
  - path: build-logic/**
  - task: write|add|move|fix .*(test|suite|assertion|flake|mutation|coverage)
provides:
  - doc:35-testing#definitions
  - doc:35-testing#source-sets
  - doc:35-testing#unit-classpath
  - doc:35-testing#purity-rules
  - doc:35-testing#assertions
  - doc:35-testing#load-bearing-evidence
  - doc:35-testing#fixture-variation
  - doc:35-testing#mutation-testing
  - doc:35-testing#coverage
  - doc:35-testing#gaps
depends_on: [doc:00-constitution, doc:30-code-style]
---

# 35 — Test taxonomy and evidence

Applies to every `src/test` and `src/integrationTest` tree, to `build-logic/`, and to
`architecture-tests/`. How a test is written is `doc:30-code-style` §7; which drawer it
goes in, and what must be true before it counts, are here. MUST / SHOULD / MAY,
`Enforced by:` and `Enforcement gap:` are defined in `documentation/README.md`.

---

## 1. Two kinds of test, and only two <a id="definitions"></a>

| | unit / acceptance | integration |
|---|---|---|
| source set | `src/test` | `src/integrationTest` |
| Gradle task | `test` | `integrationTest` |
| Spring context | never | expected |
| filesystem, network, subprocess | never | expected |
| clock | injected | injected |
| budget | milliseconds | allowed to be slow |
| classpath | an allowlist: Kotlin, the runner, the assertions, nothing else (§3) | Spring, plus everything main sees |

A test is an integration test **if and only if** it needs something the unit classpath
cannot give it. "Acceptance test" is not a third kind: an acceptance test that drives
domain behaviour through a use case, with hand-built collaborators, is a unit test.

Rules:

- A test that starts a Spring context MUST be in `src/integrationTest`.
- A test that reads or writes a real file, opens a socket, or starts a process MUST be in
  `src/integrationTest`.
- A test in `src/test` MUST NOT be slower than the rest of `src/test` by an order of
  magnitude. Move it or make it smaller.
- A test in `src/integrationTest` MUST justify the context it starts by asserting on
  something only that context provides.

`Enforced by:` `rule:archunit/unitTestsDoNotDependOnSpring`,
`rule:archunit/unitTestsAreNotSpringBootTests`,
`rule:archunit/unitTestsDoNotTouchTheFilesystemOrTheNetwork`,
`rule:archunit/unitTestsDoNotStartProcesses` — and, before any of them, by the
classpath (§3).

`Enforcement gap:` the last two rules above are not machine-checkable — nothing measures a
test's duration, and nothing asserts an integration test earns its context. `bean:0006`
carries both.

---

## 2. Source sets <a id="source-sets"></a>

Both suites are declared once, in `modus.kotlin-base`, using Gradle's JVM Test Suite
plugin. No module declares a test source set of its own.

| fact | value |
|---|---|
| plugin | `jvm-test-suite` (incubating in Gradle 9.7.1; the supported way to divide tests by purpose) |
| unit suite | the built-in `test` suite |
| integration suite | `integrationTest`, registered in `modus.kotlin-base` |
| integration inheritance | `integrationTestImplementation` extends `implementation`; `integrationTestRuntimeOnly` extends `runtimeOnly` |
| wired into | `check`, in every module, for both suites |
| the gate | `doc:00-constitution` §7.2 — one command, run identically by a human and by `rule:ci/build` |

- A module MUST NOT register a further test suite.
- A module MUST NOT put a test dependency in its own `build.gradle.kts`. It goes in
  `modus.kotlin-base` (everywhere) or `modus.spring-module` (Spring modules).
- `rule:ci/build` MUST invoke the same aggregate task the documented local command
  invokes, with no extra arguments. `Enforced by:` review of `.github/workflows/ci.yml`
  against `doc:00-constitution` §7.2. `Enforcement gap:` nothing compares the two
  mechanically; `bean:0006` carries it.

---

## 3. The unit-test classpath is an allowlist <a id="unit-classpath"></a>

This is the mechanism the whole taxonomy rests on. Misclassification is not caught in
review, and not even caught by a rule — it fails to compile.

```
modus.kotlin-base:
  testCompileClasspath  \  exclude group: org.springframework, org.springframework.boot,
  testRuntimeClasspath  /                 org.springdoc
                        -> assertUnitTestClasspathIsSpringFree: every resolved artifact on
                           BOTH configurations must have a group on the allowlist

modus.spring-module:
  implementation                    -> spring-boot-starter
  integrationTestImplementation     -> spring-boot-starter-test, the Boot BOM
```

`testImplementation` extends `implementation`, so the exclusions are what actually does
the work: without them a module's own production dependency on `spring-boot-starter`
would put Spring back on the unit-test classpath. Both configurations are cut, and both
are checked — a type absent at compile time but present at run time is still reachable
reflectively, or through a helper that sits on the classpath.

The cut lives in `modus.kotlin-base`, not in `modus.spring-module`: `:architecture-tests`
is not a Spring module, yet it puts every other module on its test classpath and so
inherited the whole Spring runtime graph through them.

**The exclusion is a denylist and cannot be anything else** — Gradle's `exclude` matches a
group *exactly*, so it can only name what someone already knew was there. It is therefore
the mechanism, not the guarantee. `assertUnitTestClasspathIsSpringFree` states the
guarantee positively:

| a unit-test classpath may carry | for |
|---|---|
| `org.jetbrains.kotlin`, `org.jetbrains.kotlinx` | Kotlin, `kotlin-test`, coroutine test support |
| `org.jetbrains`, `org.jspecify` | annotation-only artifacts the above drag in |
| `org.junit`, `org.junit.jupiter`, `org.junit.platform`, `org.opentest4j`, `org.apiguardian` | the runner |
| `io.kotest`, `io.github.java-diff-utils` | the assertions and their diff engine |
| `com.tngtech.archunit`, `org.slf4j` | the `:architecture-tests` harness and the facade ArchUnit binds to |

Anything else fails the build. That inverts the failure mode: a new dependency is refused
until someone decides it belongs on a unit-test classpath, rather than admitted in silence.
A denylist alone is not enough and the history says so — with only `org.springframework*`
excluded, `:adapter-rest`'s unit-test classpath carried `org.springdoc`, `io.swagger`,
`jakarta.validation` and five Jackson artifacts, and a unit test importing them compiled,
ran and passed `qualityCheck`. A half-stripped classpath is worse than either extreme: it
loses the compile-time guarantee *and* dies at run time inside a third-party class with
`NoClassDefFoundError: org/springframework/util/Assert`.

A unit test that imports `org.springframework.boot.test.context.SpringBootTest` therefore
fails at the import, in `:modus-server`, the module that depends on all of Spring:

```
e: .../src/test/kotlin/uk/m4xy/modus/app/PlantedViolationTest.kt:3:12 Unresolved reference 'springframework'.
```

`Enforced by:` the Kotlin compiler, and `assertUnitTestClasspathIsSpringFree` in
`modus.kotlin-base`. To widen the allowlist is to change this document.

---

## 4. Test-purity rules <a id="purity-rules"></a>

The compiler cannot see a `Thread.sleep`, and it does not read annotation values. ArchUnit
does both, so the second line of enforcement is bytecode.

`TestPurityRulesTest` in `architecture-tests` analyses **compiled unit-test bytecode**,
which reaches it as a `<module>-unit-tests.jar` published by every module under the
`modus-unit-test-classes` usage. `ImportOption.DoNotIncludeTests` MUST NOT be used on
these rules: it matches a `build/classes/.../test/...` directory layout, so on a jar it
excludes nothing and selects nothing, and every `noClasses(...)` rule would pass
vacuously.

| rule | scope | states |
|---|---|---|
| `rule:archunit/unitTestsDoNotDependOnSpring` | unit tests | no dependency on `org.springframework..` |
| `rule:archunit/unitTestsAreNotSpringBootTests` | unit tests | not annotated `@SpringBootTest` |
| `rule:archunit/unitTestsDoNotTouchTheFilesystemOrTheNetwork` | unit tests | no dependency on `java.io..`, `java.nio.file..` or `java.net..` |
| `rule:archunit/unitTestsDoNotStartProcesses` | unit tests | no call to `ProcessBuilder.start`, `ProcessBuilder.startPipeline` or `Runtime.exec` |
| `rule:archunit/nothingSleepsTheThread` | every class | no call to `Thread.sleep` |
| `rule:archunit/disabledCarriesAWorkItem` | every class | the `@Disabled` contract of `doc:30-code-style` §5.1 |

- `rule:archunit/nothingSleepsTheThread` is not scoped to tests. Sleeping is a race with a
  timer attached: slow when it passes, flaky when it does not. Await the condition, or
  inject the clock.
- `java.io..` is banned as a package, not as a hand-picked set of classes. `File`,
  `FileInputStream`, `RandomAccessFile`, … is the same denylist shape §3 rejects, and
  `File.createTempFile`/`readText`/`writeText` is the *likely* way a filesystem-touching
  unit test gets written, not an exotic one. `java.io.IOException` and `java.io.Serializable`
  are caught too: a unit test that needs either is describing I/O it should not be doing.
- Process creation is a call predicate, not a package ban: `ProcessBuilder` and `Runtime`
  live in `java.lang`, where no package ban can reach them without banning the language.
- Nothing in `architecture-tests` names a module. The analysed classpath and the guards'
  expectations both derive from the project structure, so a new module's unit tests are
  analysed without anyone remembering to add them.
- `rule:archunit/everyUnitTestPackageIsAnalysed` is the guard on the guards. It fails if
  the unit-test bytecode is missing — the only way the rules above could become a silent
  no-op. It says nothing about how much bytecode there is, and today there is one
  package's worth: see the second row of §9 before assuming these rules are exercised
  across the module graph.

`Enforcement gap:` integration-test bytecode is not published or analysed, so
`rule:archunit/disabledCarriesAWorkItem` and `rule:archunit/nothingSleepsTheThread` do not
see `src/integrationTest`. `bean:0006` carries it.

---

## 5. Assertions <a id="assertions"></a>

One runner, and never a second one.

| classpath | artifacts |
|---|---|
| `src/test` | `kotlin-test-junit5`, `kotest-assertions-core-jvm` |
| `src/integrationTest` | the above, plus `spring-boot-starter-test` (which brings AssertJ, Mockito and the Boot-managed JUnit Jupiter) |

- Versions live in `gradle/libs.versions.toml`. Kotest MUST be pinned there: the Boot BOM
  manages `org.junit` and `org.assertj`, but not `io.kotest`. Spring Boot 4.1.1 manages
  **JUnit Jupiter 6**, so an artifact built against Jupiter 5 — `archunit-junit5` was the
  one this repository hit — resolves against a platform that is not there.
- `kotest-assertions-core-jvm` MUST remain the assertions artifact and MUST NOT be
  upgraded to `kotest-runner-junit5`. It carries no `junit-platform` artifact and no
  engine, which is the only reason it can sit beside Spring Boot's runner.
- `@MockBean` and `@SpyBean` do not exist: Boot 4.0 removed them. Use `@MockitoBean` and
  `@MockitoSpyBean`, in `src/integrationTest` only.
- Kotest's `.config(enabled = false)`, `xdescribe` and `xit` are forbidden
  (`doc:30-code-style` §5.1). `Enforced by:` the assertions artifact contains no spec DSL,
  so none of the three resolves.

---

## 6. Load-bearing evidence <a id="load-bearing-evidence"></a>

> A test that has never been observed to fail is not evidence. It is a comment that costs
> a build slot.

Every added or changed test MUST ship with proof that it fails when the source it covers
is broken.

Procedure, per test:

1. `Pre:` the test passes on the unmodified source.
2. Break the specific behaviour the test names — invert a condition, drop a guard, return
   the wrong branch. Not the compile, and not a different behaviour.
3. Run the one test. Record the assertion message verbatim.
4. `Post:` revert the source. Re-run. It passes.

- The recorded failure MUST be the assertion the test's name describes. A test that fails
  with `NullPointerException` when its subject is broken has proved nothing.
- The evidence MUST appear in the pull-request body's `verify` block, verbatim, per
  `doc:00-constitution` §3.
- The same procedure applies to every rule added to `architecture-tests`: plant a real
  violation at a real call site, observe the named rule fail, revert. A rule that cannot
  be made to fail is worse than an admitted gap — it also stops anyone looking.
- `const val` references are inlined by the Kotlin compiler and leave no trace in the
  referring class file. Plant violations at call sites, never at a constant.

`Enforcement gap:` this is a review obligation on the `verify` block, not a machine check.
`bean:0006` carries it.

### 6.1 Uniform fixtures hide reachable defects <a id="fixture-variation"></a>

A killed mutant proves the test detects it **on the paths the fixtures reach, and nothing
about the paths they do not**. Coverage of the source is not coverage of the input space.

- A fixture set MUST vary collection size across 0, 1 and 2-or-more, and identity shape
  across distinct, duplicate and aliased, wherever the subject accepts a collection.
- A suite in which every fixture has one shape MUST NOT be cited as evidence that the
  behaviour holds for another.

Observed: `bean:0009` shipped a privilege escalation past 32 tests and 30 verified
mutations. Every fixture carried exactly one capability, and at size one Kotlin's
`toSet()` returns an immutable set, so the down-cast the exploit needs throws. At size
two the same call returns a `LinkedHashSet`, the cast succeeds, and the caller grants
itself a capability nobody issued.

---

## 7. Mutation testing is rejected as a CI gate <a id="mutation-testing"></a>

Mutation testing is the obvious mechanical answer to §6 and Modus does **not** adopt it.
**One reason, and it is the only one that survives checking.**

`gradle-pitest-plugin#402` — an open issue, *Find a way to mutate production code in the
other subproject with Gradle 9+* — is the build Modus actually has. Modus is **ten**
subprojects on Gradle 9.7.1 (`settings.gradle.kts`), and wherever a module's behaviour is
covered by tests that live in a different subproject, mutation analysis needs the plugin
to extend one project's configuration from another's. On Gradle 9 that is refused:

> `Configuration ':itest:mutableCodeBase' in project ':itest' cannot extend configuration
> ':shared:implementation' from project ':shared'. Configurations can only extend from
> configurations in the same context.`

Stated at its real strength, and no higher: the issue is open with no fix, and it was
self-reported by the plugin's maintainer from a functional test on his own Gradle 9
proof-of-concept branch (`AcceptanceTestsInSeparateSubprojectFunctionalSpec`), not by a
user hitting it in the wild. One unresolved incompatibility with this build is the entire
case. If `#402` closes, this section is reopened.

An earlier draft gave four reasons "each sufficient". Three did not hold, and they are
recorded rather than deleted because §6 forbids leaving a bad argument where a future
reader can cite it as settled:

| claimed | what the primary source says |
|---|---|
| `#401`: incremental history is disabled on JDK 25, so every run is a full run | **Collapses.** `#401` is an unmerged *draft PR*, *Migrate build to Gradle 9.4 + JDK25 compatibility tests - PoC*; nothing in it disables incremental analysis. Its companion `#399` records the opposite: `PIT 1.22.1 (ASM 9.9.1) works correctly with historyInputLocation on JDK 25, even with class file version 69.` |
| PIT declines to support Kotlin | **Overstated.** hcoles wrote the Apache-2.0 `pitest/pitest-kotlin` himself and core PIT merged Kotlin source-dir handling (`pitest#1347`). What is true is narrower: that plugin is archived and unmaintained since 2023 and its README points at the commercial Arcmutate, whose Kotlin integration states `Before you can use the integration, you must first acquire a licence`. |
| the `else` of an exhaustive `when` over a sealed class yields an unkillable mutant | **Overstated, and self-cancelling.** The unkillable mutants come from the compiler's redundant equality and null checks on the *final branch*, not from the `else` — and Arcmutate suppressed both (kotlin-plugin 1.1.2, 1.2.1). It is therefore an argument about which tooling you buy, not about mutation testing. |

Licence cost and an unmaintained free plugin are a cost to weigh, not a rejection. `#402`
is the rejection.

**Chosen substitute: targeted agent mutation.** For each success criterion in a bean, the
agent MUST break the specific behaviour under test, record the observed failure verbatim,
and revert, per §6. It answers what a mutation score is a proxy for — did this test ever
detect anything — and it produces the evidence the pull request has to carry anyway. §6.1
bounds what it proves.

`Enforcement gap:` targeted mutation is exhaustive over a bean's criteria, not over the
source, so it cannot report an untested branch nobody wrote a criterion for. `bean:0006`
carries it.

---

## 8. Coverage is a ratchet, not a percentage <a id="coverage"></a>

Coverage is measured by JaCoCo over both suites and gated by an exact per-module baseline.
It is a complement to §6 and never a substitute: an assertion-free test raises coverage
and proves nothing, so a covered line is evidence only after §6's procedure has been run
against the behaviour on it.

| fact | value |
|---|---|
| tool | JaCoCo, `jacoco = "0.8.15"` in `gradle/libs.versions.toml` |
| applied by | `modus.kotlin-base` per module, `modus.coverage` at the root. A module MUST NOT configure JaCoCo |
| suites measured | `test` and `integrationTest`, merged into one report per module |
| per-module report | `coverageReport` — HTML and XML, in every module that has `src/main/kotlin` |
| aggregate report | `coverageAggregateReport` — HTML and XML across every module, at `build/reports/jacoco/aggregate/` |
| published by CI | the aggregate XML, through `madrapps/jacoco-report`, on pull requests only |
| the gate | `coverageRatchet` in every module, reached by `check` and therefore by `qualityCheck` |
| the record | `config/coverage/baseline.tsv` — one row per module: missed instructions, missed branches, covered instructions, covered branches |
| moved by | `coverageBaselineWrite`, which refuses a downward write unless `-Pcoverage.regress=<reason>` is passed |

The version is pinned because Gradle 9.7.1 defaults `jacoco.toolVersion` to 0.8.13, which
lists class file version 69 (Java 25) as experimental; 0.8.14 is the first release with
official Java 25 support.

No exclude list is configured. JaCoCo 0.8.15 filters the Kotlin synthetics no test can
reach — `$default` bridges, inline and reified copies, the `lateinit` null check, the
elvis branch after a safe call, `@JvmStatic` accessors — and a hand-written list would be
a second copy of a filter set the tool already ships.

### 8.1 The rule

- A module's missed instructions, missed branches, covered instructions and covered
  branches MUST each equal the figure in its row. Both bounds are that one number.
- Missing **more** than the row fails: uncovered production code arrived, or a test that
  covered something was weakened or deleted.
- Missing **less** than the row fails: the record is stale. It is lowered in the same
  commit, so an improvement is a reviewable line rather than slack a later regression
  can spend.
- Covering **less** than the row fails too. `MISSEDCOUNT` alone pins only the uncovered
  surface: deleting or shrinking fully covered production code leaves it untouched while
  the ratio falls, so `COVEREDCOUNT` is pinned the same way and both halves of the
  fraction move only through this file.
- Every module with `src/main/kotlin` MUST have exactly one row, and no other row may
  exist. A module with no row has no gate.
- Regenerate with `./gradlew coverageBaselineWrite`, then review the diff. A figure the
  report does not produce MUST NOT be hand-written.
- The writer MUST refuse to raise a missed count. A downward write is a one-line diff
  indistinguishable at a glance from the improvement with its digits swapped, so it
  requires `-Pcoverage.regress=<reason>`; the reason is written into the baseline as a
  comment and MUST also appear in the pull request body. Every write prints the
  per-module delta, so the direction is visible in the CI log and not only in the file.

`Enforced by:` `coverageRatchet` — one `JacocoCoverageVerification` per module, with
`minimum` and `maximum` both set from the row, for all four figures — and
`coverageBaselineIsComplete`, the root guard on the row set. The regression guard is in
`coverageBaselineWrite` (`modus.coverage`), not in review.

Observed with one covered element deleted from `BoundedContexts.names`, the test adjusted
so it still passes, and the missed count therefore unchanged at `0`:

```
> Task :core-domain:coverageRatchet FAILED
Rule violated for bundle core-domain: instructions covered count is 29, but expected minimum is 33
```

Observed attempting to record that same weakened `:core-domain`:

```
> Task :coverageBaselineWrite FAILED
coverageBaselineWrite refuses to record worse coverage: :core-domain (missed instructions
0 -> 33). Restore the coverage, or re-run with -Pcoverage.regress=<reason>; the reason is
written into the baseline and belongs in the pull request body too.
```

Observed with the single assertion in `:core-domain` weakened to `assertEquals(6, 6)`:

```
> Task :core-domain:coverageRatchet FAILED
Rule violated for bundle core-domain: instructions missed count is 33, but expected maximum is 0
```

### 8.2 Why a ratchet and not a threshold

| scheme | what it does on this repository today |
|---|---|
| one high figure, say 90% | eight of nine modules have no `src/test` and the domain model does not exist. It fails all eight for work nobody has started, or — with them excluded — passes on nothing |
| one low figure | a number nobody ever raises. Satisfied on the day it lands and never constraining again |
| a threshold per module | nine invented numbers, each defensible only by the coverage that happens to exist |
| an exact per-module ratchet | non-vacuous on all nine today. The recorded missed *and* covered counts are the measurement, and every movement in either is a line in the diff |

The ratchet states no number anyone invented, and it fails in both directions, so it
cannot go stale while the build is green.

### 8.3 Provisional code is counted, not excluded

Every module holds provisional placeholder code — the bounded-context markers, the module
and adapter descriptors — to be replaced when the domain model lands.

- Provisional code MUST NOT be excluded from coverage. It is bytecode that ships, and an
  exclusion outlives the placeholder while being invisible in the number it changed.
- Counting it is what gives the ratchet teeth before the domain model exists:
  `:adapter-agent-claude` records 33 missed instructions, so a thirty-fourth fails.
- Deleting a placeholder lowers its row, through the same reviewable diff line as any
  other movement.

`Caveat, stated rather than discovered:` the baseline today is largely three copies of one
descriptor. `:adapter-agent-claude`, `:adapter-persistence-flatfile` and `:adapter-vcs-git`
all record `33 0 0 0`; `:core-application`, `:module-beans` and `:module-cost` all record
`6 0 0 0`. The day the placeholders are replaced, every row moves at once, and a reviewer
faced with nine simultaneous changes will rubber-stamp exactly the diff §8.1's regression
guard exists to catch. The guard — not the reviewer — is what makes that day safe.

The branch columns are all zero because there is not yet a branch in any module's
production code, so the `BRANCH` half of the rule is satisfied vacuously today. It is
measured, not merely declared. Observed with one compound condition planted in
`BoundedContexts` and every one of its branches exercised, `coverageBaselineWrite` recorded
`:core-domain  0 0 49 6`; with a single assertion then removed:

```
> Task :core-domain:coverageRatchet FAILED
Rule violated for bundle core-domain: branches missed count is 2, but expected maximum is 0
Rule violated for bundle core-domain: branches covered count is 4, but expected minimum is 6
```

Both reverted; the recorded rows are the ones the reports produce.

### 8.4 Coverage is attributed to the module whose tests produced it

- A module's ratchet reads ONLY the execution data its own two suites wrote. A
  `:modus-server` integration test that loads `:adapter-rest` through the Spring context
  does not credit `:adapter-rest`. A module's gate MUST NOT be satisfiable by another
  module's tests.
- The aggregate report merges every module's execution data against every module's
  classes, so it reads higher than the per-module figures. It is the reporting number and
  MUST NOT be made the gate.
- A module with no test writes no execution data, and a report with none is skipped. An
  empty `.exec` file is therefore always present, so such a module still reports every
  instruction as missed rather than reporting nothing.
- The execution data a module's gate reads MUST be derived by the same `*.exec` glob the
  aggregate uses, never by naming suites literally. Suites are declared through
  `withType<JvmTestSuite>().configureEach`, so a third one is a two-line change; with a
  literal list its agent output would be counted in the report and not in the gate, and
  the two published numbers would disagree with no error anywhere.

`Enforced by:` `coverageExecData` in `modus.kotlin-base`, a glob over
`build/jacoco/*.exec`, with `dependsOn(tasks.withType<Test>())` derived the same way.
Observed with a throwaway third suite `smokeTest` registered and one smoke test written
against `:module-cost`, whose row records six missed instructions and none covered:

```
> Task :module-cost:smokeTest
> Task :module-cost:coverageRatchet FAILED
Rule violated for bundle module-cost: instructions missed count is 3, but expected minimum is 6
Rule violated for bundle module-cost: instructions covered count is 3, but expected maximum is 0
```

With the previous literal `test.exec` + `integrationTest.exec` set restored and nothing
else changed, the same suite ran and the same module passed — the divergence this rule
closes.

---

## 9. Gaps <a id="gaps"></a>

Stated so they can be closed rather than discovered.

| gap | closing condition |
|---|---|
| No coverage **ratio** floor is set. The ratchet (§8) is exact and fires today, but with eight of nine modules at zero tests any percentage would be invented (§8.2). | every analysed module has a `src/test`. The baseline now records covered counts as well as missed, so the floor is read straight off it rather than chosen |
| **The purity rules currently guard one package.** Eight of the nine analysed modules have no `src/test` at all, so their `-unit-tests.jar` is empty and `unit-test-packages.txt` has exactly one line, `uk.m4xy.modus.core.domain`. Every rule in §4 is a `noClasses(...)`, and `rule:archunit/everyUnitTestPackageIsAnalysed` can only assert that whatever unit tests exist were imported — so the mechanism is sound and its current reach is one file. An empty jar is also indistinguishable, from inside the guard, from a jar that failed to be produced. | self-closing: each module's first unit test is analysed automatically, with no list to update. Closed when every analysed module has a `src/test` |
| Integration-test bytecode is not analysed (§4). | publish `<module>-integration-tests.jar` beside the unit-test jar |
| Nothing measures test duration or asserts an integration test earns its context (§1). | a duration budget in the `Test` task configuration |
| Nothing mechanically compares `rule:ci/build` with the documented local command (§2). | a check that the workflow invokes exactly the aggregate task |
| Load-bearing evidence is a review obligation (§6). | a PR-body checker, part of the `docs-lint` step `bean:0004` describes |

`bean:0006` carries every row but the first; `bean:0007` carries the first.
