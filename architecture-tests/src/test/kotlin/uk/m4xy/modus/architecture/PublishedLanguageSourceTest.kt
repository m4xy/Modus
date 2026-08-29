package uk.m4xy.modus.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * `doc:10-architecture#bounded-contexts` §3.1's leaf rule, enforced against **source**.
 *
 * [ArchitectureRulesTest.publishedLanguageIsLeaf] enforces the same rule against bytecode
 * and cannot see all of it. A `@JvmInline value class` erases to its underlying type, so a
 * published type holding another context's identifier in a plain field produces **no
 * bytecode edge at all** — and every identifier in `identity.published` is a value class.
 *
 * Observed before this test existed (`bean:0034`): a non-data
 * `class Probe(val owner: identity.published.ActorId)` in `domainmgmt.published` left
 * `./gradlew :architecture-tests:test` green. The three plants that *did* fire in
 * `bean:0032` were all `data class`es, whose synthesised `equals`/`hashCode`/`toString`
 * call the `-impl` methods — they fired for an incidental reason, not the rule's reason.
 *
 * Source has no such blind spot: a cross-context reference is an import or a qualified
 * name, and both survive erasure. The two rules are complementary and neither replaces the
 * other — bytecode catches what a compiler generates, source catches what a compiler
 * erases.
 */
class PublishedLanguageSourceTest {
    @Test
    fun `no published or event source references another context`() {
        val files = publishedAndEventSources()

        // Non-vacuity, in the same spirit as everyModuleIsOnTheAnalysedClasspath: a rule
        // that silently scanned nothing would pass forever.
        assertTrue(
            files.size >= MINIMUM_EXPECTED_FILES,
            "scanned ${files.size} published/event sources, expected at least " +
                "$MINIMUM_EXPECTED_FILES — the scan found the wrong directory",
        )

        val violations =
            files.flatMap { (context, file) ->
                FOREIGN_CONTEXT
                    .findAll(stripComments(file.readText()))
                    .map { it.groupValues[1] }
                    .filter { it != context }
                    .distinct()
                    .map { "${file.relativeTo(repoRoot).path}: references '$it'" }
            }

        if (violations.isNotEmpty()) {
            fail(
                "a published or event source may reference only its own context, the Kotlin " +
                    "stdlib, java.time and the shared kernel (doc:10-architecture §3.1):\n" +
                    violations.joinToString("\n") { "  $it" },
            )
        }
    }

    private fun publishedAndEventSources(): List<Pair<String, File>> =
        File(repoRoot, DOMAIN_SOURCE_ROOT)
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .mapNotNull { file ->
                val parts = file.relativeTo(File(repoRoot, DOMAIN_SOURCE_ROOT)).invariantSeparatorsPath.split('/')
                // <context>/<published|event>/<File>.kt, possibly nested deeper.
                if (parts.size >= 3 && parts[1] in LEAF_PACKAGES) parts[0] to file else null
            }.toList()

    /**
     * KDoc and comments legitimately name other contexts in prose — `doc:10`'s own rule is
     * discussed in several of these files. Only code counts.
     */
    private fun stripComments(source: String): String =
        source
            .replace(BLOCK_COMMENT, "")
            .lineSequence()
            .joinToString("\n") { it.substringBefore("//") }

    private companion object {
        private val repoRoot =
            File(
                requireNotNull(System.getProperty("modus.repoRoot")) {
                    "modus.repoRoot is not set; see architecture-tests/build.gradle.kts"
                },
            )

        private const val DOMAIN_SOURCE_ROOT =
            "core/core-domain/src/main/kotlin/uk/m4xy/modus/core/domain"

        private val LEAF_PACKAGES = setOf("published", "event")

        /**
         * A context package, never the shared kernel: `DomainEvent` and `DomainId` sit
         * directly in `..core.domain` and start with an upper-case letter, so the
         * lower-case segment this requires cannot match them
         * (`adr:0004-domain-id-shared-kernel`).
         */
        private val FOREIGN_CONTEXT = Regex("""uk\.m4xy\.modus\.core\.domain\.([a-z][a-z0-9]*)\.""")

        private val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)

        /** Two contexts are modelled; `identity` alone has eight published/event files. */
        private const val MINIMUM_EXPECTED_FILES = 8
    }
}
