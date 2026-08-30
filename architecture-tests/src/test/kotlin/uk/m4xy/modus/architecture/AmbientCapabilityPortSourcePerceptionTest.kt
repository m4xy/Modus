package uk.m4xy.modus.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What the leaf scan **perceived** in the real port sources, asserted separately from the
 * verdict `AmbientCapabilityPortSourceTest` reaches from it.
 *
 * ## Every assertion here is positive, and that is the correction
 *
 * The first version of this class asserted that each port's `imports` and `qualifiedNames`
 * were **empty** — which is exactly what a blinded scan returns. The expected value *was* the
 * failure signature, so the assertion passed when the mechanism worked and passed when it was
 * broken. A `@Suppress("/\*")` fixture that deleted half the file from the scan's input left
 * every assertion here green (`bean:0065`).
 *
 * **An assertion whose expected value is also the failure signature asserts nothing.** It is
 * the third form of one lesson this sprint: a rule over an empty scope is vacuous, a verdict
 * over an unread file is vacuous, and an emptiness check over a blinded parse is vacuous.
 *
 * So every assertion below expects something only a working scan produces: a named
 * declaration from each file, and an import that is known to be present. Emptiness now fails.
 */
class AmbientCapabilityPortSourcePerceptionTest {
    @Test
    fun `the scan sees the declaration each shipped port actually makes`() {
        val perceptions = perceptions()

        assertEquals(
            EXPECTED_PORTS,
            perceptions.keys.toSortedSet(),
            "the scan found a different set of port sources than the three that ship",
        )

        // Positive, per file. A scan that read nothing returns no declarations and fails here,
        // where an emptiness assertion would have passed.
        EXPECTED_DECLARATIONS.forEach { (file, expected) ->
            assertTrue(
                expected in perceptions.getValue(file).declarations,
                "the scan did not see `fun $expected` in $file — it declares it, so a scan that " +
                    "cannot see it has not read the file, whatever verdict it then reaches",
            )
        }
    }

    /**
     * `ClockPort` must import `java.time.Instant`, so its perception distinguishes "read the
     * file" from "read nothing at all".
     *
     * **Presence, not set equality.** An earlier version asserted the import list *equalled*
     * `["java.time.Instant"]`, which made every legal edit to the file break the test — adding
     * a second, perfectly leaf-safe `java.time` import failed it with a message reading
     * "seeing none would make the leaf verdict vacuous" when what had happened was seeing two.
     * A test whose message describes a different failure than the one that occurred invites
     * the repair a future author actually reaches for, `assertTrue(imports.isNotEmpty())`,
     * which restores the blindness this test exists to prevent.
     *
     * The positive claim needed here is only that a known-present token is seen. What else the
     * file legally imports is the verdict's business, not perception's.
     */
    @Test
    fun `the scan sees the one import that is known to be present`() {
        val imports = perceptions().getValue("ClockPort.kt").imports

        assertTrue(
            KNOWN_IMPORT in imports,
            "the scan did not see `import $KNOWN_IMPORT` in ClockPort.kt, which declares it. " +
                "A scan that cannot see a known-present import has not read the file, and the " +
                "leaf verdict over it is vacuous. Imports actually seen: $imports",
        )
    }

    /**
     * Prose is not code, and the shipped ports are the hardest real input available: all three
     * discuss `identity.published` at length in KDoc explaining why it may not appear in them.
     *
     * This assertion is negative and so cannot stand alone — a blinded scan would satisfy it.
     * It is meaningful only because the two positive tests above run over the same parse and
     * would fail first. Stated here so a later reader does not lift it out on its own.
     */
    @Test
    fun `the scan strips prose, on files whose prose names the forbidden package`() {
        val sources = portSources().map { relativeName(it) to it.readText() }

        val discussing = sources.filter { (_, text) -> "identity.published" in text }.map { it.first }
        assertTrue(
            discussing.isNotEmpty(),
            "no shipped port mentions identity.published even in prose, so this test asserts " +
                "nothing — re-point it at whatever file does",
        )

        sources.forEach { (name, text) ->
            val perceived = AmbientCapabilityPortSource.perceived(text)
            assertFalse(perceived.unterminated, "$name: the scan could not account for the whole file")
            // Not "no qualified names at all" — ClockPort legitimately names java.time.Instant on
            // its import line, and the arm now reads every qualified name rather than modus ones
            // only. What must not appear is a context package, which here exists only in prose.
            assertEquals(
                emptyList(),
                perceived.qualifiedNames.filter { it.startsWith("uk.m4xy.modus.core.domain.") },
                "$name: the scan read a context package out of what should be prose only",
            )
        }
    }

    @Test
    fun `the scan accounts for the whole of every shipped port`() {
        portSources().forEach { file ->
            assertFalse(
                AmbientCapabilityPortSource.perceived(file.readText()).unterminated,
                "${relativeName(file)}: source ends inside a comment or string, so the scan is " +
                    "not trustworthy",
            )
        }
    }

    /**
     * Keyed on the path relative to the scanned root, never on the basename.
     * `associate` keeps the last value for a duplicate key, so keying on `File.name` would let
     * `port/internal/ClockPort.kt` silently overwrite `port/ClockPort.kt` and hold the key set
     * at three — the same class of hole as everything else this file guards against.
     */
    private fun perceptions() = portSources().associate { relativeName(it) to AmbientCapabilityPortSource.perceived(it.readText()) }

    private fun relativeName(file: File): String =
        file
            .relativeTo(File(repoRoot, AmbientCapabilityPortSource.SOURCE_ROOT))
            .invariantSeparatorsPath

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

        private val EXPECTED_PORTS = sortedSetOf("ClockPort.kt", "IdGeneratorPort.kt", "RandomPort.kt")

        /** One import known to be present, asserted by presence so a legal edit cannot break it. */
        private const val KNOWN_IMPORT = "java.time.Instant"

        /** One known-present declaration per file. Adding a port is a deliberate edit here. */
        private val EXPECTED_DECLARATIONS =
            mapOf(
                "ClockPort.kt" to "now",
                "IdGeneratorPort.kt" to "newId",
                "RandomPort.kt" to "nextInt",
            )
    }
}
