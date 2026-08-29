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

// PROJECT ISOLATION: everything from here to `aggregateSources` reaches into
// other projects' `layout.buildDirectory` and `projectDirectory` at
// CONFIGURATION time. That is legal under the configuration cache, which is
// what this build runs with, and illegal under project isolation, which it does
// not yet. When isolation is turned on this block is the migration: the
// aggregate has to be fed by an `aggregation` configuration that the analysed
// modules publish into, rather than by walking `subprojects`. Nothing else in
// build-logic crosses a project boundary.

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
    # module, missed instructions, missed branches, covered instructions, covered branches.
    # All four figures are EXACT bounds: the build fails when a module's count differs from
    # its row in either direction, so both halves of the ratio move only through this file.
    # Regenerate with ./gradlew coverageBaselineWrite, and review the diff. The writer
    # REFUSES to raise a missed count; a deliberate regression needs
    # -Pcoverage.regress=<reason>, and the reason is recorded below.
    """.trimIndent() + "\n"

/** The data rows of the baseline, comment-stripped and split on whitespace. */
fun baselineColumns(text: String): List<List<String>> =
    text
        .lineSequence()
        .map { it.substringBefore("#").trim() }
        .filter { it.isNotEmpty() }
        .map { it.split(Regex("\\s+")) }
        .toList()

/** The module paths the baseline records, in file order. */
fun baselineModules(text: String): List<String> = baselineColumns(text).mapNotNull { it.firstOrNull() }

/** How many numeric columns a baseline row carries. */
val coverageBaselineFigureCount = 4

/**
 * What is recorded today, per module: missed instructions, missed branches,
 * covered instructions, covered branches. A column the file does not carry
 * reads as 0, so a row written before a column existed still compares.
 */
fun baselineFigures(text: String): Map<String, List<Long>> =
    baselineColumns(text)
        .filter { it.isNotEmpty() }
        .associate { columns ->
            columns[0] to (1..coverageBaselineFigureCount).map { columns.getOrNull(it)?.toLongOrNull() ?: 0L }
        }

// The baseline is written from the same module XML the ratchet reads, never from
// a second JaCoCo invocation. Neither task calls a function declared in this
// script from inside its action: a script-object reference is not serialisable
// by the configuration cache, which is on here with problems=fail.
val recordedBaselineText = providers.fileContents(baselineFile).asText.getOrElse("")
val recordedModules = baselineModules(recordedBaselineText)
val recordedFigures = baselineFigures(recordedBaselineText)

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

// Rewriting the baseline is the only way a figure moves, so this task — not
// review — is where the direction of the movement is judged. A downward write
// (more missed instructions than the row records) produces a diff that is
// indistinguishable at a glance from the improvement with its digits swapped,
// so it is refused unless it is asked for by name.
tasks.register("coverageBaselineWrite") {
    group = "verification"
    description = "Rewrites config/coverage/baseline.tsv from the current module coverage reports."
    dependsOn(analysedPaths.map { "$it:coverageReport" })
    val reports = coverageXmlByModule.toSortedMap()
    val target = baselineFile.asFile
    val width = (analysedPaths.maxOfOrNull { it.length } ?: 0) + 2
    val header = coverageBaselineHeader
    val previous = recordedFigures
    val regressReason =
        providers
            .gradleProperty("coverage.regress")
            .orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    inputs.files(reports.values)
    inputs.property("coverage.regress", regressReason ?: "")
    outputs.file(target)
    doLast {
        // Parsed with the JDK's own parser, not a regex. A regex that stops
        // matching — reordered attributes, a reformatted element — returns
        // nothing, and "nothing" is indistinguishable from genuine zero
        // coverage: the baseline would silently reset to all-zero and the
        // failure would surface later as a coverage message rather than a
        // parser one. JaCoCo's bundle totals are the <counter> elements that
        // are direct children of <report>. External DTD loading is off: the
        // document declares report.dtd, which is not on disk.
        fun bundleCounters(file: java.io.File): Map<String, Pair<Long, Long>> {
            val factory =
                javax.xml.parsers.DocumentBuilderFactory
                    .newInstance()
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.isXIncludeAware = false
            factory.isExpandEntityReferences = false
            val root = factory.newDocumentBuilder().parse(file).documentElement
            val children = root.childNodes
            val counters =
                (0 until children.length)
                    .mapNotNull { children.item(it) as? org.w3c.dom.Element }
                    .filter { it.tagName == "counter" }
                    .associate {
                        it.getAttribute("type") to
                            (
                                (it.getAttribute("missed").toLongOrNull() ?: -1L) to
                                    (it.getAttribute("covered").toLongOrNull() ?: -1L)
                            )
                    }
            check(counters.isNotEmpty() && counters.values.none { it.first < 0 || it.second < 0 }) {
                "$file carries no readable report-level <counter> elements. The JaCoCo report " +
                    "format has changed or the report is truncated; refusing to write a baseline " +
                    "from it rather than recording a silent 0."
            }
            return counters
        }

        // A module with no branches at all gets no BRANCH counter, which is a
        // genuine zero and not a parse failure — bundleCounters has already
        // established that the report parsed and carries totals.
        val current =
            reports.mapValues { (_, xml) ->
                val counters = bundleCounters(xml)
                val instruction = counters["INSTRUCTION"] ?: (0L to 0L)
                val branch = counters["BRANCH"] ?: (0L to 0L)
                listOf(instruction.first, branch.first, instruction.second, branch.second)
            }

        val labels = listOf("missed instructions", "missed branches", "covered instructions", "covered branches")
        val missedColumns = labels.indices.filter { labels[it].startsWith("missed") }
        val coveredColumns = labels.indices.filter { labels[it].startsWith("covered") }
        val regressions =
            current.mapNotNull { (path, now) ->
                val was = previous[path] ?: return@mapNotNull null
                // A missed count is worse when it RISES; a covered count is worse when
                // it FALLS. Guarding only the missed half let lost coverage be
                // rebaselined away without a reason. Columns are derived from `labels`
                // so adding one cannot silently escape the guard.
                val worse =
                    missedColumns.filter { now[it] > was[it] } +
                        coveredColumns.filter { now[it] < was[it] }
                if (worse.isEmpty()) null else path to worse.map { "${labels[it]} ${was[it]} -> ${now[it]}" }
            }

        logger.lifecycle("coverageBaselineWrite: ${labels.joinToString(", ")}")
        current.forEach { (path, now) ->
            val was = previous[path]
            val delta =
                when {
                    was == null -> "${now.joinToString(" ")}  (new row)"
                    was == now -> "${now.joinToString(" ")}  (unchanged)"
                    else -> "${was.joinToString(" ")} -> ${now.joinToString(" ")}"
                }
            val flag = if (regressions.any { it.first == path }) "  <-- REGRESSION" else ""
            logger.lifecycle("  ${path.padEnd(width)}$delta$flag")
        }

        check(regressions.isEmpty() || regressReason != null) {
            "coverageBaselineWrite refuses to record worse coverage: " +
                regressions.joinToString("; ") { "${it.first} (${it.second.joinToString(", ")})" } +
                ". Restore the coverage, or re-run with -Pcoverage.regress=<reason>; the reason is " +
                "written into the baseline and belongs in the pull request body too."
        }

        // The reason lives in the file, so the regression is self-documenting in
        // the same diff that records it and stays in the history afterwards.
        val note =
            if (regressions.isEmpty() || regressReason == null) {
                ""
            } else {
                "# REGRESSION accepted with -Pcoverage.regress: $regressReason\n" +
                    regressions.joinToString("") { (path, worse) -> "#   $path: ${worse.joinToString(", ")}\n" }
            }

        val rows =
            current.map { (path, figures) ->
                "${path.padEnd(width)}${figures.joinToString("\t")}"
            }
        target.writeText(header + note + rows.joinToString("\n", postfix = "\n"))
    }
}

tasks.named("check") {
    dependsOn(aggregateReport, baselineGuard)
}
