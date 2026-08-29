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

// --- the backoffice half of the gate -----------------------------------------
//
// backoffice/ and e2e/ are not Gradle projects (settings.gradle.kts), so nothing
// here compiles TypeScript or owns its config. These tasks invoke the scripts the
// backoffice already declares, which keeps one tool per language configured where
// its ecosystem expects: ktlint owns Kotlin, Prettier and ESLint own TypeScript,
// each with one config file. Spotless was considered and rejected in bean:0029 —
// it would add a second Kotlin formatter beside ktlint and a second Prettier
// configuration beside backoffice/.prettierrc, which is one fact in two places.

val npmExecutable = if (System.getProperty("os.name").startsWith("Windows")) "npm.cmd" else "npm"

/**
 * `npm ci` is idempotent but not free, so it is skipped when the tree is already
 * populated. Both lockfiles are committed, so this is reproducible rather than a
 * best-effort resolve.
 */
fun Project.npmProject(
    name: String,
    dir: String,
) = tasks.register<Exec>("${name}Install") {
    group = "nodejs"
    description = "Installs $dir dependencies from its committed lockfile."
    workingDir = file(dir)
    commandLine(npmExecutable, "ci")
    inputs.files(file("$dir/package-lock.json"), file("$dir/package.json"))
    outputs.dir(file("$dir/node_modules"))
}

val backofficeInstall = npmProject("backoffice", "backoffice")
val e2eInstall = npmProject("e2e", "e2e")

fun registerNpmCheck(
    taskName: String,
    script: String,
    what: String,
) = tasks.register<Exec>(taskName) {
    group = "verification"
    description = what
    dependsOn(backofficeInstall, e2eInstall)
    workingDir = file("backoffice")
    commandLine(npmExecutable, "run", script)
}

val backofficeTypecheck =
    registerNpmCheck(
        "backofficeTypecheck",
        "typecheck",
        "tsc --noEmit over backoffice/ and e2e/.",
    )
// Each of the three scripts runs its own tree and then chains e2e/'s, so one task per
// concern covers both trees and `npm run <script>` by hand covers what the gate covers.
// A separate e2eLint task would have left the hand-run command short of the gate, which
// is the shape of the hole bean:0046 closes: e2e/ keeps its own flat config, because
// ESLint 9 resolves one from the working directory, but not its own Gradle task.
val backofficeLint = registerNpmCheck("backofficeLint", "lint", "ESLint over backoffice/ and e2e/.")
val backofficeFormatCheck =
    registerNpmCheck(
        "backofficeFormatCheck",
        "format:check",
        "Prettier --check over backoffice/ and e2e/.",
    )

// Playwright is deliberately OUTSIDE `check` and `qualityCheck`
// (doc:00-constitution §7.2.4). It needs a built and running system and takes
// minutes; inside the fast gate it would make the gate slow enough that agents
// stop running it. Required only when user-visible behaviour changed.
tasks.register<Exec>("e2eTest") {
    group = "verification"
    description = "Playwright end-to-end and accessibility suite against a production build."
    dependsOn(backofficeInstall, e2eInstall)
    workingDir = file("e2e")
    commandLine(npmExecutable, "test")
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
    // The backoffice half. Before bean:0029 nothing reached these, so a TypeScript
    // error, an ESLint error or 77 drifted files all merged green.
    dependsOn(backofficeTypecheck, backofficeLint, backofficeFormatCheck)
    // An included build's tasks are not reached by anything in this build, so
    // the convention plugins' own ktlint/Detekt/allWarningsAsErrors gates have
    // to be asked for by name or they would exist and never run.
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
