// The root project owns no production code. It exists to lint the build scripts
// themselves and to give humans/CI a single aggregate entry point.
plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint)
    // The aggregate coverage report and the ratchet baseline guard. The
    // per-module half is in modus.kotlin-base; no module configures JaCoCo.
    id("modus.coverage")
}

ktlint {
    version = libs.versions.ktlint
    filter {
        exclude { it.file.path.contains("${File.separator}build${File.separator}") }
    }
}

// The nine checks of doc:05-authoring-for-agents#checks. A shell script rather than a
// JavaExec: the checks match lines and globs, so a source set and a toolchain would buy
// nothing. It runs from qualityCheck, which is the only command CI invokes.
tasks.register<Exec>("docsLint") {
    group = "verification"
    description = "Runs the documentation front-matter, anchor and reference checks."
    commandLine("bash", "tools/docs-lint.sh")
}

// The single aggregate entry point. CI runs exactly this, so "green locally"
// and "green in CI" cannot mean two different things.
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs every mechanical gate: compilation, tests, ktlint, Detekt and ArchUnit — build-logic included."
    // The root project's own `check` carries the aggregate coverage report and
    // the baseline guard (modus.coverage), so they cannot be a second command.
    dependsOn(
        subprojects.map { "${it.path}:check" } + listOf("check", "ktlintCheck", "docsLint"),
    )
    // An included build's tasks are not reached by anything in this build, so
    // the convention plugins' own ktlint/Detekt/allWarningsAsErrors gates have
    // to be asked for by name or they would exist and never run.
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
