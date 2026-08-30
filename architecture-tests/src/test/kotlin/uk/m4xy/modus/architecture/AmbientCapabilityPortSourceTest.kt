package uk.m4xy.modus.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * `rule:archunit/ambientCapabilityPortSourceIsLeaf` — the real guarantee that a
 * context-free ambient-capability port drags nothing behind it.
 *
 * [ArchitectureRulesTest.ambientCapabilityPortsAreLeaf] enforces the same rule against
 * bytecode and **cannot see most of it**. [AmbientCapabilityPortSource] carries the
 * diagnosis; the short form is that every identifier in this model is a `@JvmInline value
 * class`, and a value class leaves no bytecode edge.
 *
 * The verdict over the real tree is below; the input surface is
 * [AmbientCapabilityPortSourceInputSurfaceTest], on synthetic sources, so that the shapes
 * this analyser must catch are asserted directly rather than only through whatever the
 * repository happens to contain today.
 */
class AmbientCapabilityPortSourceTest {
    /**
     * camelCase deliberately, following `DefensiveCopySourceTest`: `bean:0065` cites this gate
     * as `rule:archunit/ambientCapabilityPortSourceIsLeaf`, and `docs-lint` check 6 resolves a
     * `rule:archunit/` reference by finding a `val` or `fun` of exactly that name under
     * `architecture-tests/`. A backticked sentence would leave every citation of this gate
     * pointing at nothing — check 6 rejected exactly that while this bean was being written.
     */
    @Test
    fun ambientCapabilityPortSourceIsLeaf() {
        val files = portSources()

        // Non-vacuity, the same guard everyModuleIsOnTheAnalysedClasspath applies to the
        // bytecode rules: a scan that found the wrong directory would pass forever.
        assertTrue(
            files.size >= MINIMUM_EXPECTED_FILES,
            "scanned ${files.size} ambient-capability port sources under " +
                "${AmbientCapabilityPortSource.SOURCE_ROOT}, expected at least " +
                "$MINIMUM_EXPECTED_FILES — the scan found the wrong directory",
        )

        val violations =
            files.flatMap { file ->
                AmbientCapabilityPortSource
                    .violations(file.readText())
                    .map { "${file.relativeTo(repoRoot).path}: $it" }
            }

        if (violations.isNotEmpty()) {
            fail(
                "an ambient-capability port belongs to no context and is injected into all of " +
                    "them, so it may name only the Kotlin stdlib and java.time — anything else " +
                    "is a dependency every context inherits unseen (bean:0065):\n" +
                    violations.joinToString("\n") { "  $it" },
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

        /** `ClockPort`, `IdGeneratorPort`, `RandomPort`. */
        private const val MINIMUM_EXPECTED_FILES = 3
    }
}

/**
 * The analyser's **input surface**, asserted on synthetic sources.
 *
 * This class exists because of a specific failure, and the failure is worth more than the
 * fix. The first version of this gate was a bytecode rule, and it was proven by planting
 *
 * ```
 * public class PlantedProbe { public fun newActorId(): ActorId = ActorId("planted") }
 * ```
 *
 * which ArchUnit rejected — reporting `calls method ActorId.constructor-impl`. That is a
 * **constructor call in a method body**. A port is an interface and has no bodies, so the
 * plant exercised a shape that cannot occur in the package the rule guards, and the rule's
 * other arm (`beInterfaces`) had already rejected the plant independently. **The fixture
 * supplied an enabling condition the real code omits**, the plant passed, and the rule
 * shipped blind to every value class in the model.
 *
 * So the erasing shapes are pinned here, permanently, on sources that contain no method body
 * at all. If a later author replaces this analyser with a bytecode rule, these tests fail.
 */
class AmbientCapabilityPortSourceInputSurfaceTest {
    @Test
    fun `a value-class return type is caught, though bytecode erases it`() {
        val source =
            """
            package uk.m4xy.modus.core.domain.port

            import uk.m4xy.modus.core.domain.identity.published.ActorId

            public interface IdGeneratorPort {
                public fun newActorId(): ActorId
            }
            """.trimIndent()

        assertEquals(
            listOf(
                "imports 'uk.m4xy.modus.core.domain.identity.published.ActorId'",
                "names 'uk.m4xy.modus.core.domain.identity.published.ActorId'",
            ),
            AmbientCapabilityPortSource.violations(source),
        )
    }

    @Test
    fun `a value-class parameter is caught, which erases identically`() {
        val source =
            """
            package uk.m4xy.modus.core.domain.port

            import uk.m4xy.modus.core.domain.identity.published.ActorId

            public interface ClockPort {
                public fun lastSeen(actor: ActorId): Long
            }
            """.trimIndent()

        assertTrue(AmbientCapabilityPortSource.violations(source).isNotEmpty())
    }

    /**
     * The shape with no import at all. A bytecode rule would miss this for the same reason;
     * a source rule that only read imports would miss it too.
     */
    @Test
    fun `a fully-qualified reference with no import is caught`() {
        val source =
            """
            package uk.m4xy.modus.core.domain.port

            public interface IdGeneratorPort {
                public fun newId(): uk.m4xy.modus.core.domain.identity.published.GrantId
            }
            """.trimIndent()

        assertEquals(
            listOf("names 'uk.m4xy.modus.core.domain.identity.published.GrantId'"),
            AmbientCapabilityPortSource.violations(source),
        )
    }

    @Test
    fun `the shared kernel is not exempt here, unlike in the published-language rule`() {
        val source =
            """
            package uk.m4xy.modus.core.domain.port

            import uk.m4xy.modus.core.domain.DomainId

            public interface ClockPort {
                public fun nowFor(domain: DomainId): Long
            }
            """.trimIndent()

        assertTrue(AmbientCapabilityPortSource.violations(source).isNotEmpty())
    }

    @Test
    fun `java time and the Kotlin stdlib are permitted`() {
        val source =
            """
            package uk.m4xy.modus.core.domain.port

            import java.time.Instant
            import kotlin.jvm.JvmName

            public interface ClockPort {
                public fun now(): Instant
            }
            """.trimIndent()

        assertEquals(emptyList(), AmbientCapabilityPortSource.violations(source))
    }

    /**
     * The analyser's own blind spot, asserted so it is a known limit rather than a surprise:
     * prose is not code. A KDoc naming another context is legal and must stay legal — the
     * shipped ports explain at length why `identity.published` may not appear in them, and a
     * rule that fired on the explanation would be unusable.
     */
    @Test
    fun `a reference inside a comment is not a reference`() {
        val source =
            """
            package uk.m4xy.modus.core.domain.port

            /** Never returns uk.m4xy.modus.core.domain.identity.published.ActorId. */
            public interface IdGeneratorPort {
                // not uk.m4xy.modus.core.domain.identity.published.GrantId either
                public fun newId(): String
            }
            """.trimIndent()

        assertEquals(emptyList(), AmbientCapabilityPortSource.violations(source))
    }

    @Test
    fun `the port's own package is not a foreign reference`() {
        val source =
            """
            package uk.m4xy.modus.core.domain.port

            public interface ClockPort {
                public fun now(): String
            }
            """.trimIndent()

        assertEquals(emptyList(), AmbientCapabilityPortSource.violations(source))
    }
}
