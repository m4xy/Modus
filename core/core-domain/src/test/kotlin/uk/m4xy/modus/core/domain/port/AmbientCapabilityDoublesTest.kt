package uk.m4xy.modus.core.domain.port

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.identity.published.ActorId
import java.time.Instant
import kotlin.test.Test

/**
 * The doubles are tested, not merely used.
 *
 * A test double that is only ever exercised through the thing it stands in for is asserted
 * by nothing: every assertion passes through a second piece of code that could be
 * compensating for its defect. `bean:0065` criterion 7 requires the doubles' own behaviour
 * to be observed, and criterion 8 requires the **input surface** — what a double was given
 * and what it perceived — to be asserted separately from the **verdict** a caller reaches
 * from it. The two halves are separated below by name, so a reader can see which is which.
 *
 * The concrete failure this guards against: a fixture that hands a well-formed id to code
 * under test has not tested the code that *builds* an id. Those are different assertions
 * and they belong in different tests.
 */
class AmbientCapabilityDoublesTest {
    // ---------------------------------------------------------------- verdict --

    @Test
    fun `the clock returns the instant it was constructed with`() {
        val at = Instant.parse("2026-08-29T00:00:00Z")

        FixedClock(at).now() shouldBe at
    }

    @Test
    fun `the clock is stopped, so two reads are equal`() {
        val clock = FixedClock(Instant.parse("2026-08-29T00:00:00Z"))

        clock.now() shouldBe clock.now()
    }

    @Test
    fun `successive ids differ`() {
        val ids = SequenceIdGenerator(listOf("run-1", "run-2", "run-3"))

        val first = ids.newId()
        val second = ids.newId()
        val third = ids.newId()

        setOf(first, second, third).size shouldBe 3
    }

    /**
     * The port's stated contract is that the value satisfies the shape every identifier
     * value class requires. Asserted by feeding it to a **real** value class rather than by
     * re-stating the regex here: a copy of the pattern in this file could drift from the one
     * that actually validates, and then this test would pass while the port's promise was
     * broken. `ActorId`'s own `init` block is the authority.
     */
    @Test
    fun `generated ids satisfy the identifier invariant the port promises`() {
        val ids = SequenceIdGenerator(listOf("run-1", "actor.two", "a"))

        ActorId(ids.newId()).value shouldBe "run-1"
        ActorId(ids.newId()).value shouldBe "actor.two"
        ActorId(ids.newId()).value shouldBe "a"
    }

    @Test
    fun `an exhausted generator refuses rather than repeating itself`() {
        val ids = SequenceIdGenerator(listOf("only"))
        ids.newId() shouldBe "only"

        val refusal = shouldThrow<IllegalStateException> { ids.newId() }

        refusal.message shouldBe "SequenceIdGenerator is exhausted after 1 id(s): [only]"
    }

    @Test
    fun `the same seed reproduces the same sequence`() {
        val first = SeededRandom(seed = 42)
        val second = SeededRandom(seed = 42)

        val left = List(5) { first.nextInt(100) }
        val right = List(5) { second.nextInt(100) }

        left shouldBe right
    }

    @Test
    fun `different seeds do not produce the same sequence`() {
        val first = List(5) { SeededRandom(seed = 42).nextInt(1_000) }
        val second = List(5) { SeededRandom(seed = 43).nextInt(1_000) }

        (first == second) shouldBe false
    }

    @Test
    fun `values stay inside the bound`() {
        val random = SeededRandom(seed = 7)

        val drawn = List(50) { random.nextInt(10) }

        drawn.all { it in 0..9 } shouldBe true
    }

    @Test
    fun `a non-positive bound is refused rather than absorbed`() {
        val random = SeededRandom(seed = 7)

        shouldThrow<IllegalArgumentException> { random.nextInt(0) }
        shouldThrow<IllegalArgumentException> { random.nextInt(-1) }
    }

    // ---------------------------------------------------------- input surface --

    /**
     * What the generator was **given** and has handed out, asserted directly — not inferred
     * from what a caller built out of the values. This is the half that a test using the
     * double through production code cannot see.
     */
    @Test
    fun `the generator records what it has issued, in order`() {
        val ids = SequenceIdGenerator(listOf("first", "second", "third"))
        ids.issued shouldBe emptyList()
        ids.remaining shouldBe 3

        ids.newId()
        ids.newId()

        ids.issued shouldBe listOf("first", "second")
        ids.remaining shouldBe 1
    }

    /**
     * The recorded input surface is a copy. Without this, a caller that drained `issued`
     * would empty the double's own record, and every later assertion about what it perceived
     * would be an assertion about what the test had already consumed — the defect
     * `bean:0036` gated on the production aggregates, in the test doubles instead.
     *
     * Two elements, not one: `listOf(x)` of size one is backed by a structure that throws on
     * mutation, so the same test written against a single-element fixture passes while
     * proving nothing (`doc:35-testing#fixture-variation`).
     */
    @Test
    fun `the issued record is a copy, so draining it cannot empty the double`() {
        val ids = SequenceIdGenerator(listOf("first", "second"))
        ids.newId()
        ids.newId()

        val drained = ids.issued as MutableList
        drained.clear()

        drained.size shouldBe 0
        ids.issued shouldBe listOf("first", "second")
    }

    /**
     * The random source perceives the bounds it is asked for. A caller asserting only on the
     * drawn values cannot distinguish "asked for `nextInt(10)` fifty times" from "asked for
     * `nextInt(100)` and happened to draw small numbers".
     */
    @Test
    fun `the random source records the bounds it was asked for, in order`() {
        val random = SeededRandom(seed = 7)

        random.nextInt(10)
        random.nextInt(3)
        random.nextInt(10)

        random.bounds shouldBe listOf(10, 3, 10)
    }

    @Test
    fun `a refused bound is not recorded as having been drawn`() {
        val random = SeededRandom(seed = 7)
        random.nextInt(10)

        shouldThrow<IllegalArgumentException> { random.nextInt(0) }

        random.bounds shouldBe listOf(10)
    }

    @Test
    fun `the bounds record is a copy`() {
        val random = SeededRandom(seed = 7)
        random.nextInt(10)
        random.nextInt(20)

        val drained = random.bounds as MutableList
        drained.clear()

        random.bounds shouldBe listOf(10, 20)
    }
}
