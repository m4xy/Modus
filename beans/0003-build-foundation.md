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
it; `./gradlew detekt` runs it alone.

### 6. ArchUnit enforces the dependency rules — and genuinely fails

`architecture-tests` puts every module on its test classpath and asserts 12
rules: domain is framework-free, domain depends on no outer layer, application
depends on domain only, application is free of delivery concerns, adapters do
not depend on each other / on modules / on the app, modules do not depend on
each other / on adapters / on the app, nothing depends on the app, the layering
holds, there are no package cycles, plus a guard asserting that classes for
every module were actually imported (so no rule can pass vacuously).

Evidence, passing:

```
tests="12" skipped="0" failures="0" errors="0"
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
and build caches, then `./gradlew build ktlintCheck detekt`. Reports are
uploaded only on failure. No deploy or publish steps.

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

## Follow-ups

- Replace every provisional placeholder with the real domain model
  (`core-domain`), ports and use cases (`core-application`).
- Reinstate the Detekt Gradle plugin when Detekt 2.x is stable.
- Consider adding type-resolution to Detekt once there is real code, which
  enables the `ForbiddenMethodCall` rules already declared in the config.
