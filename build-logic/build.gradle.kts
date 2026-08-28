// Convention plugins. Every Modus module applies one of these and configures
// nothing else: the toolchain, compiler flags, ktlint, Detekt and test wiring
// live here so they cannot drift between modules.
//
// These three scripts are the most load-bearing Kotlin in the repository — they
// are the definition of every gate in it — so they are held to the same gates.
// `modus.kotlin-base` cannot be applied to the build that defines it, so ktlint,
// Detekt and `allWarningsAsErrors` are configured here directly. Root
// `qualityCheck` reaches them via an included-build task dependency; an included
// build's tasks are not run by the root `build`, so without that they would be
// a gate that exists and never runs.
plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

val javaToolchains = extensions.getByType<JavaToolchainService>()

dependencies {
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.kotlin.allopen)
    implementation(libs.plugin.springBoot)
    implementation(libs.plugin.ktlint)
}

kotlin {
    compilerOptions {
        // Same rule as every module: a warning nobody has to fix is a warning
        // that will never be fixed.
        allWarningsAsErrors = true
    }
}

// --- ktlint ---------------------------------------------------------------
// Same ktlint version and the same .editorconfig (root = true lives at the
// repository root, which is this build's parent) as the modules.
ktlint {
    version = libs.versions.ktlint
    ignoreFailures = false
    filter {
        exclude { it.file.path.contains("${File.separator}build${File.separator}") }
    }
}

// --- Detekt ---------------------------------------------------------------
// The same detekt-cli, the same config file and the same JDK 21 launcher as
// modus.kotlin-base sets up for the modules. See the long comment there for why
// this is a JavaExec and why it is deliberately PSI-only.
val detektCli =
    configurations.create("detektCli") {
        isCanBeConsumed = false
        isCanBeResolved = true
        description = "Classpath for the Detekt command line analyser."
    }

dependencies {
    detektCli(libs.detekt.cli)
}

val detektConfigFile = layout.projectDirectory.file("../config/detekt/detekt.yml")
val detektReportFile = layout.buildDirectory.file("reports/detekt/detekt.html")
val kotlinSources = layout.projectDirectory.dir("src")

val detektWorkingDir = layout.projectDirectory.asFile
val detektReportArg =
    detektReportFile
        .get()
        .asFile
        .relativeTo(detektWorkingDir)
        .invariantSeparatorsPath

val detektTask =
    tasks.register<JavaExec>("detekt") {
        group = "verification"
        description = "Runs Detekt static analysis over the convention plugins."

        classpath = detektCli
        mainClass = "io.gitlab.arturbosch.detekt.cli.Main"
        javaLauncher =
            javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(21)
            }
        jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        workingDir = detektWorkingDir

        inputs
            .files(fileTree(kotlinSources) { include("**/*.kts") })
            .withPropertyName("kotlinSources")
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .skipWhenEmpty()
        inputs
            .file(detektConfigFile)
            .withPropertyName("detektConfig")
            .withPathSensitivity(PathSensitivity.NONE)
        outputs.file(detektReportFile).withPropertyName("detektReport")
        outputs.cacheIf { true }

        args(
            "--config",
            "../config/detekt/detekt.yml",
            "--build-upon-default-config",
            "--input",
            "src",
            "--report",
            "html:$detektReportArg",
        )
    }

tasks.named("check") {
    dependsOn(detektTask)
}
