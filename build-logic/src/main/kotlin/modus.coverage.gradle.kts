import org.gradle.testing.jacoco.tasks.JacocoReport

// Repository-wide coverage, applied to the root project only. The per-module
// half — agent, report, ratchet — is `modus.kotlin-base`; no module configures
// JaCoCo itself. Scheme: doc:35-testing#coverage. This script owns the merged
// cross-module report `rule:ci/build` publishes, the guard that the ratchet
// baseline names every module with production code, and the task that
// regenerates that baseline so moving it is one command and one reviewable diff.
plugins {
    jacoco
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

jacoco {
    toolVersion = libs.findVersion("jacoco").get().requiredVersion
}

/** Every module that carries production code. Derived, never listed. */
val analysedProjects = subprojects.filter { it.file("src/main/kotlin").isDirectory }.sortedBy { it.path }

val analysedPaths = analysedProjects.map { it.path }

val coverageXmlByModule =
    analysedProjects.associate { analysed ->
        analysed.path to
            analysed.layout.buildDirectory
                .file("reports/jacoco/coverageReport/coverageReport.xml")
                .get()
                .asFile
    }

val aggregateExecData =
    files(analysedProjects.map { it.layout.buildDirectory.dir("jacoco") })
        .asFileTree
        .matching { include("*.exec") }

val aggregateClasses =
    files(analysedProjects.map { it.layout.buildDirectory.dir("classes/kotlin/main") })
        .filter { it.exists() }

val aggregateSources = files(analysedProjects.map { it.layout.projectDirectory.dir("src/main/kotlin") })

val aggregateReport =
    tasks.register<JacocoReport>("coverageAggregateReport") {
        group = "verification"
        description = "Merged coverage across every module and both test suites. The XML is what CI publishes."
        dependsOn(analysedPaths.map { "$it:coverageReport" })
        executionData.setFrom(aggregateExecData)
        classDirectories.setFrom(aggregateClasses)
        sourceDirectories.setFrom(aggregateSources)
        reports {
            xml.required = true
            xml.outputLocation = layout.buildDirectory.file("reports/jacoco/aggregate/coverage.xml")
            html.required = true
            html.outputLocation = layout.buildDirectory.dir("reports/jacoco/aggregate/html")
        }
    }

val baselineFile = layout.projectDirectory.file("config/coverage/baseline.tsv")

val coverageBaselineHeader =
    """
    # The coverage ratchet baseline — doc:35-testing#coverage.
    # module, missed instructions, missed branches. Both figures are EXACT bounds:
    # the build fails when a module misses more than its row, and when it misses less.
    # Regenerate with ./gradlew coverageBaselineWrite, and review the diff.
    """.trimIndent() + "\n"

/** The module paths the baseline records, in file order. */
fun baselineModules(text: String): List<String> =
    text
        .lineSequence()
        .map { it.substringBefore("#").trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { it.split(Regex("\\s+")).firstOrNull() }
        .toList()

// The baseline is written from the same module XML the ratchet reads, never from
// a second JaCoCo invocation. Neither task calls a function declared in this
// script from inside its action: a script-object reference is not serialisable
// by the configuration cache, which is on here with problems=fail.
val recordedModules = baselineModules(providers.fileContents(baselineFile).asText.getOrElse(""))

val baselineGuard =
    tasks.register("coverageBaselineIsComplete") {
        group = "verification"
        description = "Fails if the coverage baseline does not name exactly the modules that carry production code."
        val recorded = recordedModules.sorted()
        val expected = analysedPaths.sorted()
        val baseline = baselineFile.asFile
        inputs.property("recorded", recorded)
        inputs.property("expected", expected)
        outputs.upToDateWhen { true }
        doLast {
            check(recorded == expected) {
                "$baseline records ${recorded.size} module(s) $recorded, but the modules with production code " +
                    "are $expected. Every one of them needs a row, and no other row may exist: a module with " +
                    "no recorded figure has no ratchet. Run ./gradlew coverageBaselineWrite."
            }
        }
    }

tasks.register("coverageBaselineWrite") {
    group = "verification"
    description = "Rewrites config/coverage/baseline.tsv from the current module coverage reports."
    dependsOn(analysedPaths.map { "$it:coverageReport" })
    val reports = coverageXmlByModule.toSortedMap()
    val target = baselineFile.asFile
    val width = (analysedPaths.maxOfOrNull { it.length } ?: 0) + 2
    val header = coverageBaselineHeader
    doLast {
        // JaCoCo emits <counter> at method, class, package and report level in
        // that order, so the last occurrence of each type is the bundle total.
        fun missed(
            xml: String,
            counter: String,
        ): String =
            Regex("""<counter type="$counter" missed="(\d+)" covered="\d+"/>""")
                .findAll(xml)
                .lastOrNull()
                ?.groupValues
                ?.get(1)
                ?: "0"

        val rows =
            reports.map { (path, xml) ->
                val text = xml.readText()
                "${path.padEnd(width)}${missed(text, "INSTRUCTION")}\t${missed(text, "BRANCH")}"
            }
        target.writeText(header + rows.joinToString("\n", postfix = "\n"))
    }
}

tasks.named("check") {
    dependsOn(aggregateReport, baselineGuard)
}
