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

// Every tools/*.sh task below invokes /bin/bash by absolute path, never `bash` through
// PATH. `bash` resolved to Homebrew's 5.3.9 on the development machine and to bash 5 in CI,
// so nothing these scripts do was ever validated against a known interpreter — and
// tools/docs-lint.sh's header claims bash 3.2 compatibility that no run had exercised
// (bean:0049). /bin/bash is 3.2.57 on macOS, which Apple cannot move, and bash 5 on the CI
// image; either way it is the SAME interpreter for every agent and every run, which is the
// property the three changes queued behind this one need. The bashCompatLint task reports
// which one it got and gates the claim itself. Windows was already out of scope here: these
// scripts use mktemp -d, glob expansion and awk.
val gateShell = "/bin/bash"

// The mechanical checks of doc:05-authoring-for-agents#checks — counted there and
// nowhere else, because a count restated here drifts, and had. A shell script rather than a
// JavaExec: the checks match lines and globs, so a source set and a toolchain would buy
// nothing. It runs from qualityCheck, which is the only command CI invokes.
tasks.register<Exec>("docsLint") {
    group = "verification"
    description = "Runs the documentation front-matter, anchor and reference checks."
    commandLine(gateShell, "tools/docs-lint.sh")
}

// docs-lint's own tests. What check 14's analyser PERCEIVES — which lines are inside a
// fenced block — is a separate concern from what it DECIDES, and bean:0063 is the fourth
// mechanism here to fail open through the parse while its decision tests passed. A test
// that is not in qualityCheck is not run (doc:00-constitution#observed-failing), so this
// is a dependency of the aggregate rather than a command someone remembers.
tasks.register<Exec>("docsLintTest") {
    group = "verification"
    description = "Runs the perception and verdict tests for the docs-lint check 14 analyser."
    commandLine(gateShell, "tools/docs-lint-test.sh")
}

// The bash 3.2 claim in tools/docs-lint.sh's header, made falsifiable. Two halves: a parse
// of every tools/*.sh under the pinned interpreter, and a scan for the constructs bash 3.2
// lacks. The scan re-proves on every run that it discriminates — each rule's own sample
// violation is planted and must be caught exactly once, and a fixture of legal bash 3.2
// must be clean (doc:00-constitution#observed-failing and its negative half in
// doc:50-memory-and-evidence#evidence-kinds).
tasks.register<Exec>("bashCompatLint") {
    group = "verification"
    description = "Checks tools/*.sh against the constructs bash 3.2 does not have."
    commandLine(gateShell, "tools/bash-compat-lint.sh")
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
        subprojects.map { "${it.path}:check" } + listOf("check", "ktlintCheck", "docsLint", "docsLintTest", "bashCompatLint"),
    )
    // The backoffice half. Before bean:0029 nothing reached these, so a TypeScript
    // error, an ESLint error or 77 drifted files all merged green.
    dependsOn(backofficeTypecheck, backofficeLint, backofficeFormatCheck)
    // An included build's tasks are not reached by anything in this build, so
    // the convention plugins' own ktlint/Detekt/allWarningsAsErrors gates have
    // to be asked for by name or they would exist and never run.
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
