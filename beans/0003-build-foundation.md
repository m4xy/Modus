---
title: Build foundation and mechanical quality gates
status: in-progress
type: epic
priority: high
created_at: 2026-08-28T00:00:00Z
---

# Build foundation and mechanical quality gates

Establish the Gradle build for Modus and make code style, complexity and
architecture enforceable by tooling rather than by review comments. Style and
layering must never be argued about in a PR: the build either accepts the code
or it does not.

## Scope

Owned by this work item:

- `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`,
  `gradle/wrapper/*`, `gradle.properties`
- `build-logic/` convention plugins
- every module `build.gradle.kts` and its provisional source skeleton
- `config/detekt/`, `.editorconfig`, `.github/workflows/`, `CLAUDE.md`

Explicitly **not** owned: `documentation/`, `backoffice/`, `e2e/`.

Also out of scope: the domain model itself. Every source file added here is a
marked placeholder whose only job is to make the packages real so the
architecture rules have something to inspect.

## Success criteria and evidence

### 1. Multi-module graph exists and builds

```
core/core-domain                        pure Kotlin, no framework
core/core-application                   depends on core-domain only
adapters/adapter-persistence-flatfile
adapters/adapter-rest
adapters/adapter-agent-claude
adapters/adapter-vcs-git
modules/module-beans
modules/module-cost
app/modus-server                        wiring only
architecture-tests                      the rules, as tests
build-logic                             convention plugins
```

Bounded contexts inside `core-domain`: `identity`, `domainmgmt`, `work`,
`memory`, `execution`, `cost` — each with a README stating its remit and a
provisional marker type.

Evidence — `./gradlew projects`:

```
project ':adapter-agent-claude' - /adapters/adapter-agent-claude
project ':adapter-persistence-flatfile' - /adapters/adapter-persistence-flatfile
project ':adapter-rest' - /adapters/adapter-rest
project ':adapter-vcs-git' - /adapters/adapter-vcs-git
project ':architecture-tests' - /architecture-tests
project ':core-application' - /core/core-application
project ':core-domain' - /core/core-domain
project ':module-beans' - /modules/module-beans
project ':module-cost' - /modules/module-cost
project ':modus-server' - /app/modus-server

Included builds:

\--- Included build ':build-logic'
```

### 2. No copy-pasted build scripts

Every module applies exactly one convention plugin — `modus.kotlin-base`,
`modus.spring-module` or `modus.spring-app` — and declares only its own
dependencies. No module configures Kotlin, ktlint or Detekt. Every version comes
from `gradle/libs.versions.toml`.

### 3. `./gradlew build` passes

Evidence — cold run, nothing reused from the build cache:

```
$ ./gradlew build ktlintCheck detekt --rerun-tasks --no-build-cache
...
> Task :architecture-tests:test
> Task :architecture-tests:check
> Task :architecture-tests:build
> Task :modus-server:test
> Task :modus-server:check
> Task :modus-server:build

BUILD SUCCESSFUL in 18s
104 actionable tasks: 104 executed
```

Toolchain in use:

```
$ ./gradlew --version
Gradle 9.7.1
Kotlin:        2.4.0
Launcher JVM:  25.0.1 (Amazon.com Inc. 25.0.1+8-LTS)
```

### 4. ktlint fails the build on a style violation

`ktlint_official` style, configured in `.editorconfig`, wired into `check` for
every module and over the build scripts at the root. It was observed failing on
this very branch before the sources were formatted:

```
> Task :ktlintKotlinScriptCheck FAILED
settings.gradle.kts:24:12 Newline expected after opening parenthesis
settings.gradle.kts:24:26 Parameter should start on a newline
settings.gradle.kts:24:38 Newline expected before closing parenthesis
...
> KtLint found code style violations.
```

Fixed with `./gradlew ktlintFormat`, which is the intended workflow.

### 5. Detekt runs from a deliberately tuned config

`config/detekt/detekt.yml` is a set of explicit deviations layered on Detekt's
defaults (`--build-upon-default-config`), not a generated dump; every deviation
carries the reason it exists. `build.maxIssues` is 0 and `config.validation` is
on, so a stale rule reference fails the build.

Detekt is wired into `check` in `modus.kotlin-base`, so `./gradlew build` runs
it; `./gradlew detekt` runs it alone. `build-logic` runs the same CLI against the
same config over its own convention plugins.

Detekt runs **PSI-only** — no type resolution. Every rule that needs it is listed
`active: false` with an `Enforcement gap:` at the head of the config. See R1
below.

### 6. ArchUnit enforces the dependency rules — and genuinely fails

`architecture-tests` puts every module on its test classpath — derived from
`rootProject.subprojects`, never listed — and asserts 14 rules: domain is
framework-free, domain depends on no outer layer, application depends on domain
only, application is free of delivery concerns, adapters do not depend on each
other / on modules / on the app, modules do not depend on each other / on
adapters / on the app, nothing depends on the app, the layering holds, there are
no package cycles, nothing writes to the standard streams, time is injected
rather than read from a static clock, plus a guard asserting that classes for
every module were actually imported (so no rule can pass vacuously).

Evidence, passing:

```
tests="14" skipped="0" failures="0" errors="0"
```

Evidence that the rules bite. A `@Component` was temporarily added to a class in
`core-domain` together with a Spring dependency in its build script:

```
ArchitectureRulesTest > domainIsFrameworkFree FAILED
    java.lang.AssertionError: Architecture Violation [Priority: MEDIUM] -
    Rule 'no classes that reside in a package 'uk.m4xy.modus.core.domain..'
    should depend on classes that reside in any package
    ['org.springframework..', ...]' was violated (1 times):
    Class <uk.m4xy.modus.core.domain.work.Violation> is annotated with
    <org.springframework.stereotype.Component> in (Violation.kt:0)
```

`module-cost` was then temporarily made to depend on `module-beans`, and a
mutual reference introduced between the `work` and `cost` domain packages:

```
ArchitectureRulesTest > modulesDoNotDependOnEachOther FAILED
    Rule 'slices matching 'uk.m4xy.modus.module.(*)..' should not depend on each
    other, because a domain may install any subset of modules, so no module may
    assume another is present' was violated (1 times)
ArchitectureRulesTest > thereAreNoPackageCycles FAILED
    Rule 'slices matching 'uk.m4xy.modus.(**)' should be free of cycles' was
    violated (1 times):
    Cycle detected: Slice core.domain.cost -> ...
```

All three violations were reverted and the build returned to green.

### 7. CI

`.github/workflows/ci.yml` runs on push and pull request: checkout, JDKs 21 and
25 via `actions/setup-java`, `gradle/actions/setup-gradle` for the dependency
and build caches, then `./gradlew qualityCheck` — the same aggregate command
`CLAUDE.md` documents for local use, so "green locally" and "green in CI" cannot
mean two different things. Reports are uploaded only on failure. No deploy or
publish steps.

This is **one build on JDK 25**, not a JDK matrix. `actions/setup-java` points
`JAVA_HOME` at the last version listed, so the daemon and every compile task run
on 25; JDK 21 exists solely as a launcher for the Detekt CLI. A matrix was
considered and rejected: the Kotlin toolchain is pinned to 25, so a second job
with a different launcher JVM would compile with the same toolchain and prove
nothing.

### 8. `CLAUDE.md`

Root `CLAUDE.md` covers build/test/lint commands, the module graph, the
dependency rules, the branch → work item → PR → review → merge rule with no
direct commits to `main`, and the 300k-token agent context budget. It points at
`documentation/` as the authority rather than duplicating it.

## Deviations from the specified versions

One, forced by a real failure.

**Detekt 1.23.8 is kept, but it is no longer applied as a Gradle plugin.** The
`io.gitlab.arturbosch.detekt` plugin runs the analyser inside the Gradle daemon,
and Detekt 1.23.8 embeds an IntelliJ core whose `JavaVersion` parser rejects the
JDK 25 version string:

```
Execution failed for task ':adapter-rest:detekt'
> 25.0.1
Caused by: java.lang.IllegalArgumentException: 25.0.1
    at org.jetbrains.kotlin.com.intellij.util.lang.JavaVersion.parse(JavaVersion.java:307)
    at org.jetbrains.kotlin.com.intellij.util.lang.JavaVersion.current(JavaVersion.java:176)
    at org.jetbrains.kotlin.cli.jvm.modules.JavaVersionUtilsKt.isAtLeastJava9(javaVersionUtils.kt:11)
    at org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem...
```

Neither `--jvm-target` nor `--jdk-home` avoids it: the version being parsed is
that of the *running* JVM. Detekt 2.x is still `2.0.0-alpha.6`, and 1.23.8 is
the last release in the stable line. Rather than downgrade the project
toolchain, `modus.kotlin-base` runs the same Detekt 1.23.8 CLI as a `JavaExec`
on a JDK 21 toolchain launcher. Detekt only parses Kotlin — it never emits
bytecode — so the JVM it runs on cannot change a finding. Revisit once Detekt
2.x is stable.

All other specified versions were used unchanged: Gradle 9.7.1, Kotlin 2.4.10,
Spring Boot 4.1.1, springdoc-openapi 3.1.0, ktlint plugin 14.2.0 with ktlint
1.8.0, ArchUnit 1.5.0, JDK 25 toolchain. JUnit's version is left to Spring
Boot's dependency management.

## Review cycle — PR #2

Five inline threads, all closed by commit on this branch. Each is recorded with
the evidence that the fix works, not just the claim.

### R1 — Detekt was PSI-only, so every type-resolution rule was silently dead

The reviewer proved it: `println("...")` and `Instant.now()`, both banned in
`config/detekt/detekt.yml`, gave `BUILD SUCCESSFUL`. The `JavaExec` passes no
`--classpath`/`--jvm-target`, so Detekt skips every `@RequiresTypeResolution`
rule without a warning, and `config.validation: true` does not catch it — it
checks that a rule *name* exists, not that the rule can run.

Adding `--classpath` is not available: Detekt 1.23.8 embeds Kotlin 2.0.21 against
this project's Kotlin 2.4.10, and detekt#8865 (type resolution against Kotlin
>= 2.3.0 producing "a ton of false positives") was closed *not planned* with no
backport.

The whole config was audited rather than just `ForbiddenMethodCall`. Every class
in the `detekt-rules-*:1.23.8` jars carrying `@RequiresTypeResolution` was
extracted and cross-referenced against Detekt's shipped `default-detekt-config.yml`:

```
$ grep -rl RequiresTypeResolution <detekt-rules-*.jar contents> --include='*.class' | wc -l
      67
```

67 rules require type resolution. Two of them (`MissingWhenCase`,
`RedundantElseInWhen`) are not present in the shipped default config and are
therefore not configurable. The remaining **65** are now listed in
`config/detekt/detekt.yml` with `active: false`, grouped per rule set under a
`type resolution required: cannot fire` marker.

**33 of the 65 were active** in the effective config and had never once been
evaluated:

| rule set | silently-dead rules that were ON |
| --- | --- |
| `complexity` | — |
| `coroutines` | `InjectDispatcher`, `RedundantSuspendModifier`, `SleepInsteadOfDelay`, `SuspendFunWithFlowReturnType` |
| `exceptions` | `ReturnFromFinally` |
| `naming` | `NoNameShadowing` |
| `performance` | `ArrayPrimitive` |
| `potential-bugs` | `AvoidReferentialEquality`, `DoubleMutabilityForCollection`, `HasPlatformType`, `IgnoredReturnValue`, `UnnecessaryNotNullOperator`, `UnnecessarySafeCall`, `UnreachableCatchBlock`, `UnreachableCode`, `UnsafeCallOnNullableType`, `UnsafeCast`, `UnusedUnaryOperator` |
| `style` | `ForbiddenMethodCall`, `ForbiddenVoid`, `ObjectLiteralToLambda`, `RedundantHigherOrderMapUsage`, `UnnecessaryAbstractClass`, `UnnecessaryApply`, `UnnecessaryFilter`, `UseAnyOrNoneInsteadOfFind`, `UseCheckNotNull`, `UseIsNullOrEmpty`, `UseOrEmpty`, `UseRequireNotNull`, `UselessCallOnNotNull`, `VarCouldBeVal` |

Two of those were this repository's own claims, not inherited defaults:
`potential-bugs > UnsafeCallOnNullableType: active: true` and the whole
`style > ForbiddenMethodCall` block. Both are now `active: false`.

The other 32 were already inactive upstream. They are listed anyway, so that
turning one on is a deliberate edit to a line sitting directly under the
`Enforcement gap:` that explains why it cannot work. `config.validation: true`
keeps the list honest: a rename or removal upstream fails the build.

The two bans worth keeping did not just get dropped. They moved to
`architecture-tests`, which reads bytecode and has the whole module graph on its
classpath, so they are enforced for real:

- `nothingWritesToTheStandardStreams` — Kotlin's `println` is `@InlineOnly` and
  compiles to `PrintStream.println`, so the call site is visible.
- `timeIsInjectedNeverReadFromAStaticClock` — bans the **no-argument** overloads
  of `Instant.now`, `LocalDate.now`, `LocalDateTime.now`. `Instant.now(clock)`
  stays legal, which is the behaviour the ban was pushing towards and which
  Detekt's `ForbiddenMethodCall` would have blocked too.

Evidence, both planted in a throwaway module:

```
ArchitectureRulesTest > nothingWritesToTheStandardStreams FAILED
    Rule 'no classes should call method where a java.io.PrintStream print or
    println method, because use a structured logger or the execution output
    stream, not stdout' was violated (1 times):
    Method <uk.m4xy.modus.module.probe.ProbeModule.leak()> calls method
    <java.io.PrintStream.println(java.lang.Object)> in (ProbeModule.kt:7)

ArchitectureRulesTest > timeIsInjectedNeverReadFromAStaticClock FAILED
    Rule 'no classes should call method Instant.now() or should call method
    LocalDate.now() or should call method LocalDateTime.now(), because inject a
    Clock so time is testable' was violated (1 times):
    Method <uk.m4xy.modus.module.probe.ProbeModule.leak()> calls method
    <java.time.Instant.now()> in (ProbeModule.kt:6)
```

Detekt itself still gates on everything PSI can see:

```
$ ./gradlew :module-probe:detekt --rerun-tasks
ProbeModule.kt:3:1: Track the work in beans/ instead of hiding it in a comment. [ForbiddenComment]
Analysis failed with 1 weighted issues.
BUILD FAILED
```

**Enforcement gap:** no Detekt rule requiring type resolution runs. Recorded in
full at the head of `config/detekt/detekt.yml`. Closing condition: Detekt 2.x
stable — built against Kotlin 2.4.x and tested on JDK 25, currently
`2.0.0-alpha.6` only. When it ships, delete the disabled blocks, pass a
classpath, and re-audit.

### R2 — a new module could escape all architecture analysis

Two hand-maintained lists had to stay in sync: the `testImplementation(project(...))`
literals in `architecture-tests/build.gradle.kts` and the `expected` literal in
`everyModuleIsOnTheAnalysedClasspath`. A module missing from both was never
imported, the `noClasses(...)` rules still matched the existing classes, and the
guard could not know to look.

Both lists are now derived from the project structure and nothing in
`architecture-tests` names a module:

- the classpath comes from `rootProject.subprojects.filter { it.path != project.path }`;
- the guard's expectations come from `writeAnalysedPackages`, a task that reads
  the `package` declaration out of every main-source `.kt` file in those same
  subprojects and writes them to a generated test resource the test reads.

One source, two consumers — they cannot drift.

Proved by adding a throwaway `modules/module-probe` to `settings.gradle.kts` and
nothing else. With **no edit to `architecture-tests`**, its package appeared in
the generated manifest:

```
$ cat architecture-tests/build/generated/analysed-packages/analysed-packages.txt
...
uk.m4xy.modus.module.beans
uk.m4xy.modus.module.cost
uk.m4xy.modus.module.probe

tests="14" skipped="0" failures="0" errors="0"
```

and it was genuinely analysed, not merely listed — a planted cross-module
dependency failed a pre-existing rule:

```
ArchitectureRulesTest > modulesDoNotDependOnEachOther FAILED
    Rule 'slices matching 'uk.m4xy.modus.module.(*)..' should not depend on each
    other ...' was violated (1 times):
    Method <uk.m4xy.modus.module.probe.ProbeModule.leak()> calls method
    <uk.m4xy.modus.module.cost.CostModule.knownContexts()> in (ProbeModule.kt:8)
```

The guard still backstops a misconfigured classpath. Dropping only the probe
from the dependency list, leaving the manifest complete:

```
ArchitectureRulesTest > everyModuleIsOnTheAnalysedClasspath FAILED
    java.lang.IllegalStateException: ArchUnit imported nothing for:
    [uk.m4xy.modus.module.probe] (imported 15 packages)
```

`modules/module-probe` and its `settings.gradle.kts` entry were then removed.

### R3 — `archunit-junit5` against a JUnit Platform two majors newer

Spring Boot 4.1.1 manages Jupiter 6.0.3; `archunit-junit5:1.5.0`'s engine is
built against JUnit Platform 1.14.4 and only worked because the BOM force-upgraded
it across a major version. `gradle/libs.versions.toml` now declares
`com.tngtech.archunit:archunit-junit6` (same `com.tngtech.archunit.junit` package,
so a drop-in change).

Re-proved under the new artifact: the run above showing
`tests="14" skipped="0" failures="0" errors="0"` and the planted
`modulesDoNotDependOnEachOther` / `nothingWritesToTheStandardStreams` /
`timeIsInjectedNeverReadFromAStaticClock` failures were all produced by
`archunit-junit6`.

### R4 — `build-logic` was exempt from every gate it defines

The convention plugins were the only Kotlin in the tree that nothing checked.
`build-logic/build.gradle.kts` now applies ktlint (same version, same
`.editorconfig`), runs the same detekt-cli against the same
`config/detekt/detekt.yml` on the same JDK 21 launcher, and sets
`allWarningsAsErrors = true`. Detekt is wired into `build-logic`'s `check`.

An included build's tasks are not reached by the root `build`, so root
`qualityCheck` now also depends on
`gradle.includedBuild("build-logic").task(":check")` — otherwise this would be
another gate that exists and never runs.

The gate found real violations the moment it was switched on, which is the point:

```
> Task :ktlintKotlinScriptCheck FAILED
build-logic/build.gradle.kts:65:39 Expected newline before '.'
...
> Task :ktlintMainSourceSetFormat FAILED
modus.kotlin-base.gradle.kts:4:1 A KDoc is not allowed inside 'block' (cannot be auto-corrected)
modus.spring-app.gradle.kts:1:1 A KDoc is not allowed inside 'block' (cannot be auto-corrected)
modus.spring-module.gradle.kts:1:1 A KDoc is not allowed inside 'block' (cannot be auto-corrected)
```

Fixed (`ktlintFormat` for the chains, file-header KDoc converted to line
comments) and green:

```
$ ./gradlew -p build-logic check --no-build-cache
> Task :detekt
> Task :ktlintKotlinScriptCheck
> Task :ktlintMainSourceSetCheck
> Task :validatePlugins
> Task :check
BUILD SUCCESSFUL in 8s
```

`./gradlew qualityCheck --dry-run` confirms the wiring from the root build:

```
:build-logic:detekt SKIPPED
:build-logic:ktlintKotlinScriptCheck SKIPPED
:build-logic:ktlintMainSourceSetCheck SKIPPED
:build-logic:validatePlugins SKIPPED
:build-logic:check SKIPPED
```

### R5 — CI claimed a JDK matrix it did not run

There was no `strategy.matrix`; `actions/setup-java` sets `JAVA_HOME` to the last
version listed, so everything ran on JDK 25. **Decision: correct the claim, do
not add a matrix.** The Kotlin toolchain is pinned to 25, so a second job with a
different launcher JVM would compile with the same toolchain and prove nothing
about JDK 21 compatibility. Modus targets one JVM; the matrix arrives with the
second supported target, not before. The rationale is written into the workflow
next to `setup-java`, into section 7 above and into the PR body.

CI also ran `build ktlintCheck detekt` while `CLAUDE.md` documented
`qualityCheck`. Equivalent-but-different commands drift; CI now runs
`./gradlew qualityCheck`, which is also the only command that reaches
`build-logic`.

**Enforcement gap:** nothing exercises the foojay fallback — the path where no
JDK 21 is pre-installed and the toolchain resolver has to provision one, because
`setup-java` always supplies it. Closing condition: the JDK 21 launcher
disappears entirely when Detekt 2.x is stable, which removes the fallback rather
than testing it.

### Also raised in the review summary

- **Absolute paths made the cacheable Detekt task non-relocatable.** `args(...)`
  embedded `detektConfigFile.asFile.absolutePath` and the absolute report path in
  the cache key, so no entry could be reused on another machine or worktree. The
  task now sets `workingDir` to the project directory and passes only relative
  paths, and `inputs.file(detektConfigFile)` uses `PathSensitivity.NONE` (its
  contents decide the findings; its location does not) instead of the default
  `ABSOLUTE`.
- **`org.gradle.configuration-cache.problems=warn` masked real problems.**
  Removed; the property is back at its default (`fail`). The build is green with
  it, so it was hiding nothing today — and now it cannot start.

### Verification after the review cycle

```
$ ./gradlew clean build ktlintCheck detekt --no-build-cache
...
> Task :architecture-tests:test
> Task :architecture-tests:check
> Task :architecture-tests:build
> Task :modus-server:test
> Task :modus-server:check
> Task :modus-server:build

BUILD SUCCESSFUL in 8s
117 actionable tasks: 108 executed, 9 up-to-date
Configuration cache entry stored.

$ ./gradlew qualityCheck --rerun-tasks
BUILD SUCCESSFUL in 8s
103 actionable tasks: 103 executed

architecture-tests/.../TEST-uk.m4xy.modus.architecture.ArchitectureRulesTest.xml
tests="14" skipped="0" failures="0" errors="0"
```

## Follow-ups

- Replace every provisional placeholder with the real domain model
  (`core-domain`), ports and use cases (`core-application`).
- Reinstate the Detekt Gradle plugin when Detekt 2.x is stable, and at the same
  time close the type-resolution `Enforcement gap:` recorded in
  `config/detekt/detekt.yml`: delete the 65 disabled entries, pass `--classpath`,
  and re-audit which of the restored rules Modus actually wants.
