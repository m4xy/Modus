package uk.m4xy.modus.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * `doc:20-ddd-practices#value-objects` §3.1's defensive-copy rule, enforced against **source**.
 *
 * The same defect shipped twice — `PermissionGrant.capabilities` (`bean:0009`) and all four of
 * `ProcessDefinition`'s collections (`bean:0030`) — and both times a human reading a diff was
 * the only thing that caught it. `doc:00-constitution#mechanical-enforcement` says a rule that
 * is written down, known, and still broken is a rule that needs a machine. This is the machine.
 * [DefensiveCopy] carries the argument for reading source rather than bytecode, and what four
 * rounds of review taught it.
 *
 * The scan covers the whole of `core-domain`, not only `..published..` and `..aggregate..` as
 * `bean:0036` first proposed. `bean:0030`'s harm was mutating the payload of an
 * **already-raised** domain event, and events live in `..event..`. Scoping it that way found a
 * third, live occurrence on `main`: `GrantIssued` published its `Set<Capability>`.
 *
 * This class asserts on the **verdict**. [DefensiveCopyInputSurfaceTest] asserts on the parse —
 * which declarations the analyser can see at all — because that is where every escape that
 * survived a round of review actually arrived.
 *
 * **Every test below the first is a counter-example, in both directions.** A gate's stated
 * blind-spot list is part of the gate: `doc:00-constitution#observed-failing` warns that an
 * unfalsifiable gate stops anyone looking, and a gate with a **wrong** blind-spot list does the
 * same damage while reading as rigorous.
 */
class DefensiveCopySourceTest {
    /**
     * camelCase, alone among the tests here: `doc:20-ddd-practices` §3.1 cites this gate as
     * `rule:archunit/noDomainTypePublishesACollectionItOwns`, and `docs-lint` check 6 resolves a
     * `rule:archunit/` reference by finding a `val` or `fun` of exactly that name under
     * `architecture-tests/` (`doc:05-authoring-for-agents` §2). A backticked sentence would
     * leave the document's `Enforced by:` line pointing at nothing.
     */
    @Test
    fun noDomainTypePublishesACollectionItOwns() {
        val sources = domainSources()
        val texts = sources.map { it.readText() }
        val aliases = DefensiveCopy.collectionAliases(texts)

        // Non-vacuity, in the spirit of PublishedLanguageSourceTest's file count: a parser that
        // silently stopped recognising declarations would otherwise pass forever.
        assertTrue(
            sources.size >= MINIMUM_EXPECTED_FILES,
            "scanned ${sources.size} domain sources, expected at least $MINIMUM_EXPECTED_FILES — " +
                "the scan found the wrong directory",
        )
        val examined = texts.sumOf { text -> DefensiveCopy.properties(text, aliases).count { it.collection } }
        assertTrue(
            examined >= MINIMUM_EXPECTED_PROPERTIES,
            "examined $examined collection-typed properties, expected at least $MINIMUM_EXPECTED_PROPERTIES — " +
                "the parser is no longer recognising declarations it used to",
        )

        val violations =
            sources.zip(texts).flatMap { (file, text) ->
                DefensiveCopy.violations(file.relativeTo(repoRoot).path, text, aliases)
            }

        if (violations.isNotEmpty()) {
            fail(
                "no domain type may hand out a collection it owns; copy in at construction and copy out at " +
                    "every accessor (doc:20-ddd-practices §3.1):\n" + violations.joinToString("\n") { "  $it" },
            )
        }
    }

    /**
     * `PermissionGrant` before `bean:0009`'s fix. The set was stored and published as-is; at two
     * capabilities `toSet()` returns a `LinkedHashSet`, so a caller down-casts it and grants
     * itself a capability nobody issued (`doc:35-testing#fixture-variation`).
     */
    @Test
    fun `rejects the pre-fix PermissionGrant`() {
        val violations =
            violationsOf(
                """
                public class PermissionGrant private constructor(
                    public val id: GrantId,
                    public val capabilities: Set<Capability>,
                )
                """,
            )

        assertEquals(1, violations.size, "expected exactly one violation")
        assertTrue(violations.single().contains("PermissionGrant.capabilities"), "the violation must name the property")
    }

    /**
     * `ProcessDefinition` before `bean:0030`'s fix: a `data class` with four published
     * collections, copied neither in nor out.
     */
    @Test
    fun `rejects the pre-fix ProcessDefinition`() {
        val violations =
            violationsOf(
                """
                public data class ProcessDefinition(
                    public val states: Set<StateName>,
                    public val initial: StateName,
                    public val terminal: Set<StateName>,
                    public val transitions: Set<StateTransition>,
                )
                """,
            )

        assertEquals(3, violations.size, "one per collection property, and `initial` is not one")
        assertTrue(violations.all { it.contains("data class") }, "the data-class reason must be the one reported")
    }

    /**
     * Every accessor shape that is not a copy.
     *
     * Each is asserted on its own inside the loop rather than by a count over one fixture, so a
     * failure still names the shape that broke. The bare field is the escape a `data class` ban
     * alone misses (`bean:0009`). The block with an early return and the conditional both passed
     * round one, which asked only whether the accessor *ended* in a copy call. The two `also`
     * shapes passed rounds two and three: the spine reduced to `granted.also.toList`, so the
     * chain check called it a copy while the lambda — or the method reference, which has no
     * brace to ban — handed the live list to a sink. `register()` is the same escape with no
     * argument at all, so argument-free turned out to be necessary and not sufficient: a copy
     * chain is now a field and exactly one copy call, and nothing else.
     */
    @Test
    fun `rejects every accessor that is not a copy chain`() {
        val shapes =
            listOf(
                "public val capabilities: List<Capability> get() = granted",
                "public val capabilities: List<Capability> get() = if (frozen) granted else granted.toList()",
                "public val capabilities: List<Capability> get() = granted.also { Sink.capture(it) }.toList()",
                "public val capabilities: List<Capability> get() = granted.also(Sink::capture).toList()",
                "public val capabilities: List<Capability> get() = granted.register().toList()",
                "public val capabilities: List<Capability> get() = granted.register().toList()",
                "public val capabilities: List<Capability>\n" +
                    "        get() {\n" +
                    "            if (granted.isNotEmpty()) return granted\n" +
                    "            return granted.toList()\n" +
                    "        }",
            )

        shapes.forEach { accessor ->
            val violations =
                violationsOf(
                    """
                    public class PermissionGrant private constructor(
                        private val granted: List<Capability>,
                    ) {
                        $accessor
                    }
                    """,
                )

            assertTrue(
                violations.singleOrNull()?.contains("not a copy chain") == true,
                "expected exactly one copy-chain violation for `$accessor`, got $violations",
            )
        }
    }

    /**
     * Every function shape that hands back a live view.
     *
     * `asReversed` and `subList` return **writable views** that write through to the backing
     * list; round one's function rule matched a bare identifier only. A wrapped signature and a
     * `return` inside a block were not matched at all.
     */
    @Test
    fun `rejects every function that hands back a live view`() {
        val violations =
            violationsOf(
                """
                public class PermissionGrant private constructor(
                    private val granted: MutableList<Capability>,
                ) {
                    public fun bare(): List<Capability> = granted

                    public fun ordered(): List<Capability> = granted.asReversed()

                    public fun head(): List<Capability> = granted.subList(0, granted.size)

                    public fun wrapped(
                        actor: ActorId,
                        domain: DomainId,
                    ): List<Capability> = granted

                    public fun blocked(): List<Capability> {
                        return granted
                    }

                    public fun grouped(): Map<String, List<Capability>> = mapOf("all" to granted)

                    public fun paired(): Pair<List<Capability>, Int> = Pair(granted, granted.size)

                    public fun copied(): List<Capability> = granted.toList()
                }
                """,
            )

        assertEquals(7, violations.size, "seven leaks and one copy, and the copy is not one of them")
        assertTrue(violations.all { it.contains("the backing collection") })
    }

    /**
     * **The escape that survived three rounds of fixes, each time by removing a precondition.**
     *
     * Round three taught the parser to read a property with no declared type; `Function` kept
     * requiring one, and `explicitApi()` forces a return type on **public API only**. So
     * `internal fun leak() = held` walked out with the live list on a public domain class. A
     * function with no return type is now judged by what its body returns.
     */
    @Test
    fun `rejects a function with no declared return type that hands back a backing field`() {
        val violations =
            violationsOf(
                """
                public class Holder private constructor(
                    private val held: List<StateName>,
                ) {
                    internal fun leak() = held

                    internal fun size() = held.size

                    internal fun counted(): Int = held.size
                }
                """,
            )

        assertEquals(2, violations.size, "both undeclared returns; the declared `Int` one is fine")
        assertTrue(violations.all { it.contains("declares no return type") })
    }

    /**
     * The set of backing fields is every **private** property, not only those whose type says
     * "collection".
     *
     * This is the sharper form of the whole lesson: the honest limit of a lexical gate is not
     * "a type checker would be needed" but "here is the cheapest thing that closes it". Here
     * that was one `filter` clause, and it closes a case previously listed as out of reach.
     */
    @Test
    fun `sees through an initialiser it cannot recognise`() {
        val violations =
            violationsOf(
                """
                public class GrantIssued(
                    granted: Set<Capability>,
                ) {
                    private val issued = copyOf(granted)

                    public fun raised(): Set<Capability> = issued

                    private fun copyOf(values: Set<Capability>): Set<Capability> = values.toSet()
                }
                """,
            )

        assertTrue(violations.single().contains("GrantIssued.raised()"), "the field need not be recognisably a collection")
    }

    /**
     * Copy **in**, which the rule text always demanded and the gate did not check until review
     * made it concrete: a private field and a copying getter still leave the caller holding the
     * live list if it handed that list to something it can call.
     *
     * Four ways in, and the fourth two are why "the only way in is a named factory" was a false
     * claim in `doc:20` until round four: a public **secondary** constructor delegating to the
     * private primary, and an **`init` block** storing the parameter, both bind the caller's
     * collection. The scan still does not read a factory's body; that residual is planted as a
     * blind spot rather than implied away.
     */
    @Test
    fun `rejects every collection that is not copied in`() {
        val violations =
            violationsOf(
                """
                public class Ingredients(
                    private val used: List<StateName>,
                ) {
                    public val all: List<StateName> get() = used.toList()
                }

                public class Wrapped private constructor(
                    supplied: List<StateName>,
                ) {
                    private val kept: List<StateName> = supplied
                }

                public class Delegating private constructor(
                    private val steps: List<StateName>,
                ) {
                    public constructor(supplied: List<StateName>, tag: String) : this(supplied)
                }

                public class Initialised private constructor(
                    supplied: List<StateName>,
                ) {
                    private val stored: List<StateName>

                    init {
                        this.stored = supplied
                    }
                }
                """,
            )

        assertEquals(4, violations.size, "a public constructor, an initialiser, a secondary constructor and an init block")
        assertTrue(violations.any { it.contains("never copied IN") })
        assertTrue(violations.any { it.contains("secondary constructor a caller can reach") })
        assertEquals(2, violations.count { it.contains("without copying it") })
    }

    /** One keyword used to defeat the collection test entirely — aliased twice, or made generic. */
    @Test
    fun `follows a typealias, an alias of an alias, and a generic alias`() {
        val source =
            """
            public typealias Capabilities = Set<Capability>

            public typealias Granted = Capabilities

            public typealias Bag<T> = List<T>

            public data class GrantIssued(
                public val capabilities: Granted,
                public val bag: Bag<StateName>,
            )
            """.trimIndent()
        val aliases = DefensiveCopy.collectionAliases(listOf(source))

        assertEquals(setOf("Capabilities", "Granted", "Bag"), aliases)
        assertEquals(2, DefensiveCopy.violations(LABEL, source, aliases).size, "both aliased collections")
        assertEquals(emptyList(), DefensiveCopy.violations(LABEL, source), "and without the aliases it is invisible")
    }

    /**
     * The fixed shape raises nothing, including a getter wrapped onto its own line.
     *
     * `sorted()` rather than `sortedWith(comparator)`: a copy chain may take no arguments at all,
     * because any call given an argument can be given the receiver. That is the round-four
     * replacement for "no lambda", which was an allowlist wearing a rule's clothes.
     */
    @Test
    fun `accepts a private backing field behind a copying getter`() {
        val violations =
            violationsOf(
                """
                public class ProcessDefinition private constructor(
                    private val declaredStates: List<StateName>,
                    public val initial: StateName,
                ) {
                    public val states: Set<StateName> get() = declaredStates.toSet()

                    public val ordered: List<StateName>
                        get() =
                            declaredStates
                                .toList()

                    public fun allows(from: StateName): Boolean = declaredStates.any { it == from }

                    public fun size(): Int = declaredStates.size
                }
                """,
            )

        assertEquals(emptyList(), violations)
    }

    private fun violationsOf(source: String): List<String> = DefensiveCopy.violations(LABEL, source.trimIndent())

    private fun domainSources(): List<File> =
        File(repoRoot, DOMAIN_SOURCE_ROOT)
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

        /**
         * The module's whole main source set, not the `..core.domain` package inside it.
         * Identical today and divergent the moment a file is placed outside that package, which
         * would have made `doc:20` §3.1's wording and this constant quietly disagree.
         */
        private const val DOMAIN_SOURCE_ROOT = "core/core-domain/src/main"

        private const val LABEL = "Probe.kt"

        /** Two contexts, a shared kernel and four context markers are modelled today. */
        private const val MINIMUM_EXPECTED_FILES = 20

        /** Aggregates alone declare eight; `ProcessDefinition` declares six more. */
        private const val MINIMUM_EXPECTED_PROPERTIES = 12
    }
}
