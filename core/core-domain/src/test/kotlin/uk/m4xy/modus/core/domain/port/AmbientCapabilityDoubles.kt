package uk.m4xy.modus.core.domain.port

import java.time.Instant

// Hand-written doubles for the three ambient-capability ports.
//
// doc:30-code-style#testing-style §7 and doc:15-repository-layout §8 both forbid a mocking
// framework in core/: a mock of your own domain is a design smell, and these are small
// enough that a mock would be more code than the thing it replaced.
//
// Every one is deterministic, and every one records what it was GIVEN separately from what
// it RETURNS, so a test can assert on the input surface without inferring it from a verdict.
// AmbientCapabilityDoublesTest asserts both halves; that separation is the point, not an
// accident of the design.

/**
 * A clock stopped at [at].
 *
 * Records nothing, because it has no input to record beyond its constructor argument —
 * `now()` takes none. The perception assertion for this double is that [at] is what comes
 * back, which is the whole of its input surface.
 */
class FixedClock(
    private val at: Instant,
) : ClockPort {
    override fun now(): Instant = at
}

/**
 * Returns [ids] in order, then refuses.
 *
 * Exhaustion throws rather than wrapping around or returning a placeholder. A generator
 * that silently repeats itself turns "this test needed four ids and the fixture supplied
 * three" into a duplicate-id defect discovered somewhere else entirely — and the whole
 * contract of [IdGeneratorPort] is distinctness.
 *
 * [issued] is the input-surface half: what this double has actually handed out, in order,
 * readable without reconstructing it from whatever the caller built.
 */
class SequenceIdGenerator(
    private val ids: List<String>,
) : IdGeneratorPort {
    private val handedOut = mutableListOf<String>()

    /** What this generator has issued so far, oldest first. A copy: draining it changes nothing. */
    val issued: List<String> get() = handedOut.toList()

    /** How many values remain before [newId] throws. */
    val remaining: Int get() = ids.size - handedOut.size

    override fun newId(): String {
        check(handedOut.size < ids.size) {
            "SequenceIdGenerator is exhausted after ${handedOut.size} id(s): $handedOut"
        }
        val next = ids[handedOut.size]
        handedOut += next
        return next
    }
}

/**
 * A reproducible source, seeded once.
 *
 * Deliberately **not** `kotlin.random.Random(seed)`: that ties the recorded expectations in
 * a test to a stdlib algorithm nobody promised to keep stable across Kotlin versions. This
 * is a small linear congruential generator with its constants written down, so a test that
 * asserts an exact sequence is asserting something this file defines and this file can
 * keep. It is a test double; it is not a source of randomness anything should rely on for
 * anything real.
 *
 * [bounds] is the input-surface half: the bounds this double was asked for, in order.
 */
class SeededRandom(
    seed: Long,
) : RandomPort {
    private var state: Long = seed
    private val asked = mutableListOf<Int>()

    /** Every bound this source has been asked for, oldest first. A copy. */
    val bounds: List<Int> get() = asked.toList()

    override fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive: $bound" }
        asked += bound
        // Numerical Recipes' LCG constants. Stated, not imported, for the reason above.
        state = state * MULTIPLIER + INCREMENT
        // The high bits of an LCG are better distributed than the low ones, so shift
        // before reducing: `state % bound` on the raw value is the classic way to get a
        // generator whose last bit alternates.
        val positive = (state ushr SHIFT).toInt() and Int.MAX_VALUE
        return positive % bound
    }

    private companion object {
        private const val MULTIPLIER = 6_364_136_223_846_793_005L
        private const val INCREMENT = 1_442_695_040_888_963_407L
        private const val SHIFT = 16
    }
}
