package uk.m4xy.modus.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the leaf scan **perceived** in the real port sources, asserted separately from the
 * verdict `AmbientCapabilityPortSourceTest` reaches from it.
 *
 * `ArchitectureRulesTest.everyAmbientCapabilityPortIsSeenByItsOwnRule` asserts perception at
 * the granularity of **names in a package**, which is the wrong granularity for this gate and
 * says so in its own KDoc: the escape that got past round one was in what a name *declares*,
 * not in the set of names. This class asserts the granularity that matters — what the scan
 * read out of each file — and it is the assertion `bean:0065` criterion 8 should have cited
 * from the start. Criterion 8 originally cited `AmbientCapabilityDoublesTest`, which asserts
 * the input surface of the **test doubles**; useful, and not a statement about this gate at
 * all.
 *
 * The failure mode this exists for: a regex that stops matching returns nothing, and nothing
 * is indistinguishable from a clean file. `AmbientCapabilityPortSource.violations` would then
 * return an empty list over every port in the repository and the gate would be green,
 * permanently, having read none of them.
 */
class AmbientCapabilityPortSourcePerceptionTest {
    @Test
    fun `the scan reads the imports each shipped port actually declares`() {
        val perceptions =
            portSources().associate { file ->
                file.name to AmbientCapabilityPortSource.perceived(file.readText())
            }

        assertEquals(
            setOf("ClockPort.kt", "IdGeneratorPort.kt", "RandomPort.kt"),
            perceptions.keys,
            "the scan found a different set of port sources than the three that ship",
        )

        // ClockPort is the one port that must import something, so it is the one file whose
        // perception can distinguish "read it and found one import" from "read nothing".
        assertEquals(
            listOf("java.time.Instant"),
            perceptions.getValue("ClockPort.kt").imports,
            "ClockPort declares exactly one import and the scan must see it; seeing none " +
                "would make the leaf verdict vacuous for every port",
        )

        // The other two import nothing, which is a real observation and not a parse failure —
        // distinguishable from it only because the assertion above fires on the same regex.
        assertEquals(emptyList(), perceptions.getValue("IdGeneratorPort.kt").imports)
        assertEquals(emptyList(), perceptions.getValue("RandomPort.kt").imports)
    }

    /**
     * Prose is not code, and the shipped ports are the proof: all three discuss
     * `identity.published` at length in KDoc explaining why it may not appear in them. If
     * comment-stripping regressed, those files would be the first to fail the verdict — so
     * asserting that the scan sees **no** qualified name in them is asserting that stripping
     * still works, on the hardest real input available.
     */
    @Test
    fun `the scan strips prose, on files whose prose names the forbidden package`() {
        val sources = portSources().map { it.name to it.readText() }

        val discussing =
            sources.filter { (_, text) -> "identity.published" in text }.map { it.first }
        assertTrue(
            discussing.isNotEmpty(),
            "no shipped port mentions identity.published even in prose, so this test is " +
                "asserting nothing — it must be re-pointed at whatever file does",
        )

        sources.forEach { (name, text) ->
            assertEquals(
                emptyList(),
                AmbientCapabilityPortSource.perceived(text).qualifiedNames,
                "$name: the scan read a qualified name out of what should be prose only",
            )
        }
    }

    private fun portSources(): List<File> =
        File(repoRoot, AmbientCapabilityPortSource.SOURCE_ROOT)
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()

    private companion object {
        private val repoRoot =
            File(
                requireNotNull(System.getProperty("modus.repoRoot")) {
                    "modus.repoRoot is not set; see architecture-tests/build.gradle.kts"
                },
            )
    }
}
