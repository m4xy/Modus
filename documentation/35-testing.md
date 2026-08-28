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
  - doc:35-testing#mutation-testing
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
| classpath | no `org.springframework..` at all | Spring, plus everything main sees |

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
`rule:archunit/unitTestsDoNotTouchTheFilesystemOrTheNetwork` — and, before any of them,
by the classpath (§3).

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

## 3. The unit-test classpath has no Spring on it <a id="unit-classpath"></a>

This is the mechanism the whole taxonomy rests on. Misclassification is not caught in
review, and not even caught by a rule — it fails to compile.

```
modus.spring-module:
  implementation                    -> spring-boot-starter
  integrationTestImplementation     -> spring-boot-starter-test, the Boot BOM
  testCompileClasspath              -> exclude group: org.springframework
  testRuntimeClasspath              -> exclude group: org.springframework.boot
```

`testImplementation` extends `implementation`, so the exclusions are what actually does
the work: without them a module's own production dependency on `spring-boot-starter`
would put Spring back on the unit-test classpath.

A unit test that imports `org.springframework.boot.test.context.SpringBootTest` therefore
fails at the import, in `:modus-server`, the module that depends on all of Spring:

```
e: .../src/test/kotlin/uk/m4xy/modus/app/PlantedViolationTest.kt:3:12 Unresolved reference 'springframework'.
```

`Enforced by:` the Kotlin compiler, and `assertUnitTestClasspathIsSpringFree` in
`modus.kotlin-base`, which fails the build if any artifact whose group starts
`org.springframework` reaches `testCompileClasspath`. The exclusion list names two groups;
the task is what stops that list from silently rotting when a third one appears.

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
| `rule:archunit/unitTestsDoNotTouchTheFilesystemOrTheNetwork` | unit tests | no dependency on `java.nio.file..` or `java.net..` |
| `rule:archunit/nothingSleepsTheThread` | every class | no call to `Thread.sleep` |
| `rule:archunit/disabledCarriesAWorkItem` | every class | the `@Disabled` contract of `doc:30-code-style` §5.1 |

- `rule:archunit/nothingSleepsTheThread` is not scoped to tests. Sleeping is a race with a
  timer attached: slow when it passes, flaky when it does not. Await the condition, or
  inject the clock.
- Nothing in `architecture-tests` names a module. The analysed classpath and the guards'
  expectations both derive from the project structure, so a new module's unit tests are
  analysed without anyone remembering to add them.
- `rule:archunit/everyUnitTestPackageIsAnalysed` is the guard on the guards. It fails if
  the unit-test bytecode is missing — the only way the rules above could become a silent
  no-op.

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
  manages `org.junit` and `org.assertj`, but not `io.kotest`.
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

---

## 7. Mutation testing is rejected as a CI gate <a id="mutation-testing"></a>

Mutation testing is the obvious mechanical answer to §6 and Modus does **not** adopt it.
Four independent reasons, each sufficient:

| # | reason |
|---|---|
| 1 | `gradle-pitest-plugin#402`: cross-subproject mutation is broken on Gradle 9. Modus is eleven subprojects. |
| 2 | `gradle-pitest-plugin#401`: incremental history is disabled on JDK 25. Modus is a JDK 25 toolchain. Every run is a full run. |
| 3 | PIT declines to support Kotlin. Kotlin support is the commercial Arcmutate add-on. |
| 4 | Kotlin's exhaustive `when` over a sealed class compiles to an unreachable `else` branch. Mutating it produces a mutant no test can kill, so the score has a permanent, meaningless deficit. |

Reasons 1 and 2 are bugs and may be fixed. Reasons 3 and 4 are structural.

**Chosen substitute: targeted agent mutation.** For each success criterion in a bean, the
agent breaks the specific behaviour under test and records the observed failure, per §6.
It answers what a mutation score is a proxy for — did this test ever detect anything — and
it produces the evidence the pull request has to carry anyway.

`Enforcement gap:` targeted mutation is exhaustive over a bean's criteria, not over the
source, so it cannot report an untested branch nobody wrote a criterion for. `bean:0006`
carries it.

---

## 8. Gaps <a id="gaps"></a>

Stated so they can be closed rather than discovered.

| gap | closing condition |
|---|---|
| **Coverage is not measured.** No JaCoCo, no threshold, no report. Out of scope here on purpose: a gate added in the same change as the taxonomy would be tuned to whatever the current tests happen to reach. | its own pull request, against this taxonomy |
| Integration-test bytecode is not analysed (§4). | publish `<module>-integration-tests.jar` beside the unit-test jar |
| Nothing measures test duration or asserts an integration test earns its context (§1). | a duration budget in the `Test` task configuration |
| Nothing mechanically compares `rule:ci/build` with the documented local command (§2). | a check that the workflow invokes exactly the aggregate task |
| Load-bearing evidence is a review obligation (§6). | a PR-body checker, part of the `docs-lint` step `bean:0004` describes |

`bean:0006` carries every row.
