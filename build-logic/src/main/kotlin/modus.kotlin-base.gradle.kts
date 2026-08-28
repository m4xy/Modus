import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

// Baseline for every Kotlin module in Modus: JVM toolchain, compiler strictness,
// and the mechanical gates (ktlint, Detekt, tests).
//
// No module may configure Kotlin, ktlint or Detekt itself. If a rule needs to
// change, it changes here, once, for everybody.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
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

// --- The unit-test classpath is Spring-free, and stays that way ------------
// modus.spring-module excludes the two Spring groups Modus resolves today. That
// list is a literal, so it is checked rather than trusted: a third
// org.springframework* group fails here instead of silently re-admitting Spring
// to the source set whose whole point is not having it. Compile classpath,
// because compiling is where misclassification is supposed to die.
val unitTestCompileArtifacts =
    configurations
        .named("testCompileClasspath")
        .flatMap { it.incoming.artifacts.resolvedArtifacts }

val springFreeStamp = layout.buildDirectory.file("reports/test-taxonomy/unit-test-classpath.txt")

val assertUnitTestClasspathIsSpringFree =
    tasks.register("assertUnitTestClasspathIsSpringFree") {
        group = "verification"
        description = "Fails if any org.springframework* artifact reaches the unit-test compile classpath."
        val artifacts = unitTestCompileArtifacts
        val stamp = springFreeStamp
        val module = project.path
        inputs.property("unitTestCompileClasspath", artifacts.map { arts -> arts.map { it.id.displayName }.sorted() })
        outputs.file(stamp)
        doLast {
            val spring =
                artifacts
                    .get()
                    .map { it.id.componentIdentifier }
                    .filterIsInstance<ModuleComponentIdentifier>()
                    .filter { it.group.startsWith("org.springframework") }
                    .map { "${it.group}:${it.module}" }
                    .distinct()
                    .sorted()
            check(spring.isEmpty()) {
                "Spring is on the unit-test compile classpath of $module: $spring. " +
                    "Unit tests may not see Spring; move the test to src/integrationTest, " +
                    "or add the group to the exclusions in modus.spring-module."
            }
            stamp
                .get()
                .asFile
                .also { it.parentFile.mkdirs() }
                .writeText("spring-free\n")
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
