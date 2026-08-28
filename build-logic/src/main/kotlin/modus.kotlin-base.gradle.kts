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

// --- Tests ----------------------------------------------------------------
dependencies {
    testImplementation(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}
