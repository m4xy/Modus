// The root project owns no production code. It exists to lint the build scripts
// themselves and to give humans/CI a single aggregate entry point.
plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint)
}

ktlint {
    version = libs.versions.ktlint
    filter {
        exclude { it.file.path.contains("${File.separator}build${File.separator}") }
    }
}

// The single aggregate entry point. CI runs exactly this, so "green locally"
// and "green in CI" cannot mean two different things.
tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs every mechanical gate: compilation, tests, ktlint, Detekt and ArchUnit — build-logic included."
    dependsOn(
        subprojects.map { "${it.path}:check" } + listOf("ktlintCheck"),
    )
    // An included build's tasks are not reached by anything in this build, so
    // the convention plugins' own ktlint/Detekt/allWarningsAsErrors gates have
    // to be asked for by name or they would exist and never run.
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
