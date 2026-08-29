package uk.m4xy.modus.architecture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What [DefensiveCopy]'s parser **can see** — which declarations become a `Property` or a
 * `Function` at all — asserted separately from what the rules then decide about them.
 *
 * This class exists because of where four rounds of review found the defects. Every one of the
 * six escapes that survived a round arrived here, not in the rules: the rule that catches a
 * function returning a live view was correct and could not fire, because a backing field with an
 * inferred type never became a `Property`; the rule that resolves a `typealias` was correct and
 * never saw `typealias Bag<T>`; the rule that finds members was correct and no owner was ever
 * pushed for an anonymous `object :`. In each case the *rule* test passed and the gate failed
 * open, because the rule test supplied its own enabling condition in the fixture.
 *
 * **A lexical gate's real blind-spot list is the list of shapes its regexes cannot see**, which
 * is a different list from the rules it has not implemented — and it needs its own tests, which
 * assert on the parse rather than on the verdict. `DefensiveCopySourceTest` asserts on the
 * verdict.
 */
class DefensiveCopyInputSurfaceTest {
    /**
     * `explicitApi()` forces a type annotation on public API only, so every `private` field and
     * every member of an `internal` class may omit one. Requiring `: Type` made
     * `private val issued = granted.toSet()` invisible (`bean:0036`, round three).
     */
    @Test
    fun `sees a property with no declared type`() {
        val source =
            """
            public class GrantIssued(
                granted: Set<Capability>,
            ) {
                private val issued = granted.toSet()
            }
            """

        val issued = propertyNamed(source, "issued")

        assertEquals("", issued.declaredType, "there is no declared type to read")
        assertTrue(issued.collection, "and it is read from the initialiser instead")
        assertTrue(issued.isPrivate)
    }

    /**
     * Strict mode does not require a visibility modifier on a member of an `internal` or
     * `private` class, so "a member always carries one" was blind inside them (round two).
     */
    @Test
    fun `sees a member that carries no visibility modifier`() {
        val source =
            """
            internal class ProbeRegistry {
                val names = mutableListOf<String>()
            }
            """

        val names = propertyNamed(source, "names")

        assertEquals("ProbeRegistry", names.owner)
        assertTrue(names.collection)
        assertTrue(!names.isPrivate, "public within the module is not private")
    }

    /** The same omission one level along: a return type is optional off the public API (round four). */
    @Test
    fun `sees a function with no declared return type`() {
        val source =
            """
            public class Holder private constructor(
                private val held: List<StateName>,
            ) {
                internal fun leak() = held
            }
            """

        val leak = DefensiveCopy.functions(source.trimIndent()).single { it.name == "leak" }

        assertEquals("", leak.returnType, "there is no declared return type to read")
        assertEquals(listOf("held"), leak.returned, "so the body has to answer for it")
    }

    /**
     * The return type is read from the segment before the body, not by looking for the first
     * colon. Looking for the colon first found the one inside `= granted.also(::noop)` and
     * reported a return type of `:noop)`, so an undeclared return looked declared and the rule
     * that fires on undeclared returns could not fire (review's X4). A parse bug, not a rule gap
     * — which is exactly the distinction this class exists to make.
     */
    @Test
    fun `reads a return type past a method reference in the body`() {
        val source =
            """
            public class Holder private constructor(
                private val held: List<StateName>,
            ) {
                internal fun raised() = held.also(::noop)

                public fun typed(): List<StateName> = held.toList()

                private fun noop(values: List<StateName>): Unit = Unit
            }
            """

        val functions = DefensiveCopy.functions(source.trimIndent()).associateBy { it.name }

        assertEquals("", functions.getValue("raised").returnType, "`::noop` is not a return type")
        assertEquals("List<StateName>", functions.getValue("typed").returnType)
    }

    /** An `object : Steps { … }` declares members and has no name for the type regex to find. */
    @Test
    fun `sees members of an anonymous object expression`() {
        val source =
            """
            public class Recipe private constructor(
                private val steps: List<StateName>,
            ) {
                public fun asSteps(): Steps =
                    object : Steps {
                        override val declared: List<StateName> = steps
                    }
            }
            """

        val declared = propertyNamed(source, "declared")

        assertEquals("<object>", declared.owner, "an anonymous object is still an owner")
        assertTrue(declared.collection)
    }

    /**
     * A character literal is not a string literal. `'('` used to survive stripping, leaving paren
     * depth permanently unbalanced, after which type headers stopped being recognised and every
     * later declaration was attributed to the wrong owner — or exempted outright.
     */
    @Test
    fun `is not derailed by a character literal containing a bracket`() {
        val source =
            """
            public class Delimiters {
                public val open: Char = '('
            }

            public data class Leaky(
                public val values: List<String>,
            )
            """

        assertEquals("Leaky", propertyNamed(source, "values").owner, "the type after it is still parsed")
    }

    /** A function body always opens a brace, so a local sits deeper than a member and needs no modifier. */
    @Test
    fun `does not mistake a local for a member`() {
        val source =
            """
            public class ProcessDefinition private constructor(
                private val declaredStates: List<StateName>,
            ) {
                public fun sorted(): List<StateName> {
                    val seen: MutableList<StateName> = declaredStates.toMutableList()
                    return seen
                }
            }
            """

        assertEquals(
            listOf("declaredStates"),
            DefensiveCopy.properties(source.trimIndent()).map { it.name },
            "`seen` is a local, not a member",
        )
    }

    /**
     * The two input-surface features added with the copy-in rule, enumerated here because they
     * were not: review found `init { this.stored = supplied }` in exactly the gap this class
     * exists to close, one commit after the class shipped. A feature of the parse that nothing
     * enumerates is a feature nothing guards.
     */
    @Test
    fun `sees a reachable secondary constructor and a qualified assignment in an init block`() {
        val source =
            """
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
            """.trimIndent()

        assertEquals(setOf("Delegating"), DefensiveCopy.secondaries(source), "a private one would not count")

        val assignment = DefensiveCopy.assignments(source).single { it.target == "stored" }

        assertEquals("Initialised", assignment.owner)
        assertEquals("supplied", assignment.expression, "`this.` is stripped, as `ROOT` does everywhere else")
        assertTrue("supplied" in assignment.parameters, "and the constructor parameter is in scope for it")
    }

    private fun propertyNamed(
        source: String,
        name: String,
    ): DefensiveCopy.Property = DefensiveCopy.properties(source.trimIndent()).single { it.name == name }
}
