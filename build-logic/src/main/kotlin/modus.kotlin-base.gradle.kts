import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

// Baseline for every Kotlin module in Modus: JVM toolchain, compiler strictness,
// and the mechanical gates (ktlint, Detekt, tests).
//
// No module may configure Kotlin, ktlint or Detekt itself. If a rule needs to
// change, it changes here, once, for everybody.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    jacoco
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val javaToolchains = extensions.getByType<JavaToolchainService>()

/** The JVM Modus itself is built for and runs on. */
val projectJdk =
    libs
        .findVersion("javaToolchain")
        .get()
        .requiredVersion
        .toInt()

/** The JVM the Detekt CLI is executed on. See the Detekt section below. */
val detektJdk = 21

group = "uk.m4xy.modus"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(projectJdk)

    // The public API of every module must be spelled out: visibility modifiers
    // and explicit return types. Cheap to satisfy, and it stops accidental API
    // leaks out of core-domain. Kotlin exempts test source sets automatically.
    explicitApi()

    compilerOptions {
        // Treat JSR-305 / Spring nullability annotations as real Kotlin types
        // rather than platform types.
        freeCompilerArgs.add("-Xjsr305=strict")

        // A warning nobody has to fix is a warning that will never be fixed.
        allWarningsAsErrors = true
    }
}

// --- ktlint ---------------------------------------------------------------
// Official Kotlin style, configured entirely in .editorconfig. Failing, never
// warning: style is not a review topic.
ktlint {
    version = libs.findVersion("ktlint").get().requiredVersion
    ignoreFailures = false
    filter {
        exclude { it.file.path.contains("${File.separator}build${File.separator}") }
    }
}

// --- Detekt ---------------------------------------------------------------
// Detekt 1.23.8 is the current stable release (2.x is still alpha) and it embeds
// an IntelliJ core whose JavaVersion parser rejects the JDK 25 version string
// "25.0.1", so the Gradle plugin's in-daemon invoker dies on any JDK 25 build:
//
//   java.lang.IllegalArgumentException: 25.0.1
//     at ...intellij.util.lang.JavaVersion.parse(JavaVersion.java:307)
//     at ...cli.jvm.modules.CoreJrtFileSystem.globalJrtFsCache$lambda$2
//
// Neither --jvm-target nor --jdk-home avoids it: the parse is of the *running*
// JVM. Rather than downgrade the project toolchain, the Detekt CLI is run as a
// plain JavaExec on a JDK 21 launcher. Detekt only parses Kotlin — it never
// emits bytecode — so the JVM it runs on cannot change a single finding.
// Revisit and go back to the Gradle plugin once Detekt 2.x is stable.
val detektCli =
    configurations.create("detektCli") {
        isCanBeConsumed = false
        isCanBeResolved = true
        description = "Classpath for the Detekt command line analyser."
    }

dependencies {
    detektCli(libs.findLibrary("detekt-cli").get())
}

val detektConfigFile = isolated.rootProject.projectDirectory.file("config/detekt/detekt.yml")
val detektReportFile = layout.buildDirectory.file("reports/detekt/detekt.html")
val kotlinSources = layout.projectDirectory.dir("src")

// The task is cacheable, so nothing about it may depend on where the checkout
// lives: an absolute path in `args` would be part of the cache key and make
// every entry unusable on another machine (or another worktree). Everything the
// CLI is told is therefore relative to the working directory, which is this
// project's directory.
val detektWorkingDir = layout.projectDirectory.asFile
val detektConfigArg = detektConfigFile.asFile.relativeTo(detektWorkingDir).invariantSeparatorsPath
val detektInputArg = kotlinSources.asFile.relativeTo(detektWorkingDir).invariantSeparatorsPath
val detektReportArg =
    detektReportFile
        .get()
        .asFile
        .relativeTo(detektWorkingDir)
        .invariantSeparatorsPath

val detektTask =
    tasks.register<JavaExec>("detekt") {
        group = "verification"
        description = "Runs Detekt static analysis over this module's Kotlin sources."

        classpath = detektCli
        mainClass = "io.gitlab.arturbosch.detekt.cli.Main"
        javaLauncher =
            javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(detektJdk)
            }
        // Required by detekt-cli's own manifest when it is launched from a
        // classpath rather than as an executable jar.
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        workingDir = detektWorkingDir

        inputs
            .files(fileTree(kotlinSources) { include("**/*.kt") })
            .withPropertyName("kotlinSources")
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .skipWhenEmpty()
        // NONE, not the default ABSOLUTE: the config's contents decide the
        // findings, its location does not.
        inputs
            .file(detektConfigFile)
            .withPropertyName("detektConfig")
            .withPathSensitivity(PathSensitivity.NONE)
        outputs.file(detektReportFile).withPropertyName("detektReport")
        outputs.cacheIf { true }

        // DELIBERATELY PSI-ONLY. There is no `--classpath`/`--jvm-target` here,
        // so Detekt performs no type resolution and skips every rule annotated
        // @RequiresTypeResolution — silently, and without a warning.
        //
        // This is not an oversight and adding `--classpath` is not the fix:
        // Detekt 1.23.8 embeds Kotlin 2.0.21 while Modus compiles with 2.4.10,
        // and detekt#8865 (type resolution against Kotlin >= 2.3.0 producing "a
        // ton of false positives") was closed NOT PLANNED with no backport.
        //
        // The cost is real and is written down rather than hidden: all 65
        // affected rules are listed with `active: false` in
        // config/detekt/detekt.yml, under an `Enforcement gap:` that names what
        // is lost and the condition for closing it (Detekt 2.x stable). Note
        // this is a SEPARATE problem from the JDK 21 launcher above, which is a
        // crash workaround.
        args(
            "--config",
            detektConfigArg,
            "--build-upon-default-config",
            "--input",
            detektInputArg,
            "--report",
            "html:$detektReportArg",
        )
    }

tasks.named("check") {
    dependsOn(detektTask)
}

// --- Test suites ----------------------------------------------------------
// src/test is unit and acceptance: no Spring context, no I/O, fast.
// src/integrationTest gets the context, the filesystem, and permission to be slow.
// Declared with Gradle's JVM Test Suite plugin (incubating in 9.7.1, and the
// supported way to divide tests by purpose). The split is not a naming
// convention review has to police: modus.spring-module strips Spring off the
// unit-test classpath, so misclassification fails to COMPILE. See doc:35-testing.
//
// kotlin-test-junit5 is named outright because a suite's dependency block takes
// a coordinate, and the Kotlin plugin's implicit kotlin-test substitution fires
// only for the built-in `test` source set — on a custom suite it leaves
// `kotlin.test.Test` unresolved.
val kotlinTest = "org.jetbrains.kotlin:kotlin-test-junit5:${libs.findVersion("kotlin").get().requiredVersion}"
val kotestAssertions = libs.findLibrary("kotest-assertions-core").get()

testing {
    suites {
        withType<JvmTestSuite>().configureEach {
            dependencies {
                implementation(kotlinTest)
                // Assertions, not a runner — see gradle/libs.versions.toml.
                implementation(kotestAssertions)
            }
        }

        register<JvmTestSuite>("integrationTest") {
            // `implementation(project())` is the only way a custom suite sees
            // the main output.
            dependencies { implementation(project()) }
        }
    }
}

// A custom suite starts from nothing, where `test` inherits `implementation` and
// `runtimeOnly` for free. Integration tests need at least what main needs.
configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.getByName("implementation"))
}
configurations.named("integrationTestRuntimeOnly") {
    extendsFrom(configurations.getByName("runtimeOnly"))
}

// --- Unit-test classes, published for the architecture rules --------------
// :architecture-tests asserts the test-purity rules against compiled unit-test
// bytecode, which is on no classpath it would otherwise see. Modules expose
// theirs as a jar under a Modus-private Usage attribute: nothing else can
// resolve it, and no module has to opt in.
val unitTestOutput =
    extensions
        .getByType<JavaPluginExtension>()
        .sourceSets
        .named("test")
        .map { it.output }

val unitTestClassesJar =
    tasks.register<Jar>("unitTestClassesJar") {
        group = "verification"
        description = "Packages this module's unit-test classes for :architecture-tests to analyse."
        archiveClassifier = "unit-tests"
        from(unitTestOutput)
    }

configurations.consumable("unitTestClasses") {
    description = "This module's compiled unit-test classes, for architecture analysis only."
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "modus-unit-test-classes"))
    }
    outgoing.artifact(unitTestClassesJar)
}

// --- What a unit test may see, as an allowlist -----------------------------
// `testImplementation` extends `implementation`, so every production dependency
// of a module arrives on the unit-test classpath unless it is cut. The cut is
// the two `exclude` lines below, and it is a DENYLIST — Gradle's exclude matches
// a group exactly, so it can only ever name what is already known to be there.
//
// A denylist is the wrong shape for the guarantee this taxonomy rests on. Every
// group it forgets is admitted SILENTLY: before this task was an allowlist,
// `:adapter-rest`'s unit-test classpath carried springdoc, swagger, jakarta and
// five Jackson artifacts, and a unit test importing them compiled, ran and
// passed. The classpath was half-stripped, which is worse than either extreme —
// the compile-time guarantee is gone AND the test dies at run time inside a
// third-party class with `NoClassDefFoundError: org/springframework/util/Assert`.
//
// So the exclusions are the mechanism and this task is the contract. The set of
// things a *unit* test legitimately needs is small and stable, so it is stated
// positively: Kotlin, the runner, the assertions, the ArchUnit harness, and the
// annotation-only artifacts those drag in. Anything else fails the build until
// somebody decides it belongs on a unit-test classpath — a new dependency is
// refused rather than admitted, and the exclusion list becomes a performance
// detail rather than the load-bearing list.
val unitTestClasspathAllowlist =
    setOf(
        // Kotlin itself, its test bindings, and coroutine test support.
        "org.jetbrains.kotlin",
        "org.jetbrains.kotlinx",
        // Annotation-only artifacts (@Nullable and friends) pulled in by the above.
        "org.jetbrains",
        "org.jspecify",
        // The runner.
        "org.junit",
        "org.junit.jupiter",
        "org.junit.platform",
        "org.opentest4j",
        "org.apiguardian",
        // The assertions, and the diff engine kotest reports failures with.
        "io.kotest",
        "io.github.java-diff-utils",
        // :architecture-tests' harness, and the logging facade ArchUnit binds to.
        "com.tngtech.archunit",
        "org.slf4j",
    )

// Both classpaths, not just the compile one. Compiling is where misclassification
// is supposed to die, but a type that is absent at compile time and present at
// run time is still reachable reflectively, or through a helper that sits on the
// classpath — and the guard whose job is to stop the exclusion list rotting would
// have been looking the other way. `:modus-server`'s testRuntimeClasspath carried
// springdoc-openapi-starter-webmvc-ui and its whole fan-out.
val unitTestClasspathNames = listOf("testCompileClasspath", "testRuntimeClasspath")

// Spring is cut here, in the base plugin, rather than in modus.spring-module:
// :architecture-tests is not a Spring module, yet it puts every other module on
// its test classpath and so inherits the entire Spring runtime graph through
// them. Declared once, it holds for every module without exception.
val excludedFromUnitTestClasspath = listOf("org.springframework", "org.springframework.boot", "org.springdoc")

unitTestClasspathNames.forEach { classpath ->
    configurations.named(classpath) {
        excludedFromUnitTestClasspath.forEach { exclude(group = it) }
    }
}

val unitTestArtifacts =
    unitTestClasspathNames.associateWith { name ->
        configurations.named(name).flatMap { it.incoming.artifacts.resolvedArtifacts }
    }

val unitTestClasspathStamp = layout.buildDirectory.file("reports/test-taxonomy/unit-test-classpath.txt")

val assertUnitTestClasspathIsSpringFree =
    tasks.register("assertUnitTestClasspathIsSpringFree") {
        group = "verification"
        description = "Fails if any artifact outside the unit-test allowlist reaches a unit-test classpath."
        val artifacts = unitTestArtifacts
        val allowed = unitTestClasspathAllowlist
        val stamp = unitTestClasspathStamp
        val module = project.path
        artifacts.forEach { (name, resolved) ->
            inputs.property(name, resolved.map { arts -> arts.map { it.id.displayName }.sorted() })
        }
        outputs.file(stamp)
        doLast {
            val report = StringBuilder()
            artifacts.toSortedMap().forEach { (name, resolved) ->
                val groups =
                    resolved
                        .get()
                        .map { it.id.componentIdentifier }
                        .filterIsInstance<ModuleComponentIdentifier>()
                        .map { it.group to "${it.group}:${it.module}" }
                val refused =
                    groups
                        .filterNot { (group, _) -> group in allowed }
                        .map { (_, coordinate) -> coordinate }
                        .distinct()
                        .sorted()
                check(refused.isEmpty()) {
                    "$module's $name is not a unit-test classpath: $refused. " +
                        "A unit test may see only $allowed. Move the test to src/integrationTest, " +
                        "or — if the dependency genuinely belongs to every unit test — widen " +
                        "unitTestClasspathAllowlist in modus.kotlin-base and say why."
                }
                report.append(name).append('\n')
                groups.map { (_, coordinate) -> coordinate }.distinct().sorted().forEach {
                    report.append("  ").append(it).append('\n')
                }
            }
            stamp
                .get()
                .asFile
                .also { it.parentFile.mkdirs() }
                .writeText(report.toString())
        }
    }

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"), assertUnitTestClasspathIsSpringFree)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}

// --- Coverage --------------------------------------------------------------
// The scheme and its rationale are doc:35-testing#coverage; this is its
// mechanism. The pin is load-bearing: Gradle 9.7.1 defaults `toolVersion` to
// 0.8.13, only EXPERIMENTAL on class file version 69. There is deliberately no
// exclude list — 0.8.15 filters the Kotlin synthetics itself.
jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}

// A module with no tests writes no `.exec` file, and a JacocoReport with no
// execution data is skipped — which would make the ratchet vacuous on exactly
// the eight modules that have no `src/test`. An always-present empty exec file
// forces the report out of `classDirectories` alone, counting every instruction
// as missed (doc:35-testing#coverage §8.4).
val emptyCoverageExec = layout.buildDirectory.file("jacoco/empty.exec")

val seedCoverageExecData =
    tasks.register("seedCoverageExecData") {
        description = "Writes an empty JaCoCo exec file so a module with no tests still produces a report."
        val target = emptyCoverageExec
        outputs.file(target)
        doLast {
            target
                .get()
                .asFile
                .also { it.parentFile.mkdirs() }
                .writeBytes(ByteArray(0))
        }
    }

// Every `.exec` under the module's JaCoCo directory: the empty seed plus one
// per test suite. Matched by the SAME glob `modus.coverage` uses to build the
// aggregate, deliberately. Naming `test.exec` and `integrationTest.exec` here
// literally is a divergence waiting to happen: suites are declared through
// `withType<JvmTestSuite>().configureEach`, so a third one is a two-line
// change, and its agent output would then be counted in the report and NOT in
// the gate (doc:35-testing#coverage §8.4).
val coverageExecData =
    layout.buildDirectory
        .dir("jacoco")
        .map { it.asFileTree.matching { include("*.exec") } }

// Kotlin-only repository: `classes/kotlin/main` is the whole of a module's
// production bytecode. `src/main/kotlin` is what the HTML report renders.
val coverageClasses = files(layout.buildDirectory.dir("classes/kotlin/main")).filter { it.exists() }
val coverageSources = layout.projectDirectory.dir("src/main/kotlin")

// The four numeric columns of a baseline row, in file order.
val coverageBaselineFigureCount = 4
val missedInstructionsColumn = 0
val missedBranchesColumn = 1
val coveredInstructionsColumn = 2
val coveredBranchesColumn = 3

/**
 * The four figures recorded for [modulePath] in `config/coverage/baseline.tsv`:
 * missed instructions, missed branches, covered instructions, covered branches,
 * in that order. A module with no row reads as `0 0 0 0` and fails on its first
 * uncovered instruction; `coverageBaselineIsComplete` at the root turns that
 * into a named failure.
 */
fun coverageBaselineRow(
    text: String,
    modulePath: String,
): List<Long> {
    val columns =
        text
            .lineSequence()
            .map { it.substringBefore("#").trim() }
            .filter { it.isNotEmpty() }
            .map { it.split(Regex("\\s+")) }
            .firstOrNull { it.firstOrNull() == modulePath }
            .orEmpty()
    return (1..coverageBaselineFigureCount).map { columns.getOrNull(it)?.toLongOrNull() ?: 0L }
}

// A module with no production code (`:architecture-tests`) gets no coverage
// tasks. `coverageBaselineIsComplete` at the root is the guard on this branch:
// the baseline must name exactly the modules that do have production code.
if (coverageSources.asFile.isDirectory) {
    val coverageReport =
        tasks.register<JacocoReport>("coverageReport") {
            group = "verification"
            description = "Merges this module's unit and integration coverage into one HTML + XML report."
            // Derived, not listed: every `Test` task in the module, whichever
            // suite declared it. Pairs with the `*.exec` glob above.
            dependsOn(seedCoverageExecData, tasks.withType<Test>())
            executionData.setFrom(coverageExecData)
            classDirectories.setFrom(coverageClasses)
            sourceDirectories.setFrom(coverageSources)
            reports {
                xml.required = true
                html.required = true
            }
        }

    // The ratchet. Both bounds are the same number, so uncovered code that grows
    // fails and uncovered code that shrinks fails too, until the baseline is
    // lowered in the same commit (doc:35-testing#coverage §8.1). MISSEDCOUNT
    // alone would pin only the uncovered surface: deleting or shrinking fully
    // covered production code leaves it untouched while the ratio falls, so
    // COVEREDCOUNT is pinned the same way and both halves of the fraction move
    // only through a reviewable diff line.
    val baselineFile = isolated.rootProject.projectDirectory.file("config/coverage/baseline.tsv")
    val recorded =
        providers
            .fileContents(baselineFile)
            .asText
            .map { coverageBaselineRow(it, project.path) }
            .getOrElse(List(coverageBaselineFigureCount) { 0L })
    val missedInstructions = recorded[missedInstructionsColumn]
    val missedBranches = recorded[missedBranchesColumn]
    val coveredInstructions = recorded[coveredInstructionsColumn]
    val coveredBranches = recorded[coveredBranchesColumn]

    val coverageRatchet =
        tasks.register<JacocoCoverageVerification>("coverageRatchet") {
            group = "verification"
            description = "Fails unless missed and covered counts equal config/coverage/baseline.tsv."
            dependsOn(coverageReport)
            executionData.setFrom(coverageExecData)
            classDirectories.setFrom(coverageClasses)
            sourceDirectories.setFrom(coverageSources)
            violationRules {
                rule {
                    limit {
                        counter = "INSTRUCTION"
                        value = "MISSEDCOUNT"
                        minimum = missedInstructions.toBigDecimal()
                        maximum = missedInstructions.toBigDecimal()
                    }
                    limit {
                        counter = "BRANCH"
                        value = "MISSEDCOUNT"
                        minimum = missedBranches.toBigDecimal()
                        maximum = missedBranches.toBigDecimal()
                    }
                    limit {
                        counter = "INSTRUCTION"
                        value = "COVEREDCOUNT"
                        minimum = coveredInstructions.toBigDecimal()
                        maximum = coveredInstructions.toBigDecimal()
                    }
                    limit {
                        counter = "BRANCH"
                        value = "COVEREDCOUNT"
                        minimum = coveredBranches.toBigDecimal()
                        maximum = coveredBranches.toBigDecimal()
                    }
                }

                // doc:20-ddd-practices §7.3. The module-wide rule above is a regression
                // trip-wire on a number that includes constructors and getters; this one
                // is the behavioural floor, and it is a ratio rather than a count so it
                // needs no baseline row and never blocks a deletion. "Aggregate method"
                // is not a concept JaCoCo can resolve — the package is, which is why
                // doc:20 §5.1 makes `..aggregate` a convention rather than a suggestion.
                // Vacuous in a module with no aggregate package, and proven non-vacuous
                // in :core-domain by bean:0009's planted uncovered branch.
                rule {
                    element = "PACKAGE"
                    includes = listOf("uk.m4xy.modus.core.domain.*.aggregate")
                    limit {
                        counter = "BRANCH"
                        value = "COVEREDRATIO"
                        minimum = "1.0".toBigDecimal()
                    }
                }
            }
        }

    tasks.named("check") {
        dependsOn(coverageRatchet)
    }
}
