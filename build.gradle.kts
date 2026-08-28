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

tasks.register("qualityCheck") {
    group = "verification"
    description = "Runs every mechanical gate: compilation, tests, ktlint, Detekt and ArchUnit."
    dependsOn(
        subprojects.map { "${it.path}:check" } + listOf("ktlintCheck"),
    )
}
