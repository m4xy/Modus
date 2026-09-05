package uk.m4xy.modus.core.domain.work

import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.domainmgmt.published.StateName
import uk.m4xy.modus.core.domain.work.published.WorkItemState
import kotlin.test.Test

/**
 * `WorkItem` maps its own [WorkItemState] onto `domainmgmt`'s [StateName] to ask the
 * process anything, and that mapping is **total only while the two invariants agree**.
 * Widen `WorkItemState` by one character class and `transitionTo` starts throwing
 * `IllegalArgumentException` from inside a guard — a failure at the wrong layer, in a
 * method whose contract promises a domain exception.
 *
 * A constant that must match an authority takes a test, not a comment (`bean:0090`). This
 * one drives **one corpus through both types** and asserts they reach the same verdict,
 * rather than comparing the two regexes: two patterns that look alike are not evidence
 * that they decide alike, and the length bound is not in either regex at all — `StateName`
 * shipped with a message promising 64 characters and a pattern accepting 399.
 */
class WorkItemStateMatchesStateNameTest {
    private val corpus =
        listOf(
            // accepted by both
            "todo",
            "a",
            "9",
            "in-progress",
            "needs-more-evidence",
            "v2-draft",
            "a".repeat(64),
            "a-a-a-a",
            // rejected by both
            "",
            " ",
            "Todo",
            "TODO",
            "in progress",
            "in_progress",
            "-leading",
            "trailing-",
            "double--hyphen",
            "has.dot",
            "has/slash",
            "café",
            "a".repeat(65),
            "a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a-a",
        )

    @Test
    fun `WorkItemState and StateName reach the same verdict on every value in the corpus`() {
        val disagreements =
            corpus.filter { candidate ->
                accepts { WorkItemState(candidate) } != accepts { StateName(candidate) }
            }

        disagreements shouldBe emptyList()
    }

    /**
     * The corpus is only evidence while it contains both verdicts. A corpus of accepted
     * values alone would agree with a type that accepted everything
     * (`doc:35-testing#load-bearing-evidence`: what a fixture reaches is not what a rule
     * decides), so its own composition is asserted.
     */
    @Test
    fun `the corpus carries both verdicts, so agreement is not vacuous`() {
        corpus.count { accepts { WorkItemState(it) } } shouldBe 8
        corpus.count { !accepts { WorkItemState(it) } } shouldBe corpus.size - 8
    }

    /** The mapping `WorkItem` performs, exercised end to end on the widest accepted value. */
    @Test
    fun `every value WorkItemState accepts can be mapped onto a StateName`() {
        corpus.filter { accepts { WorkItemState(it) } }.forEach { StateName(WorkItemState(it).value).value shouldBe it }
    }

    private fun accepts(construct: () -> Any): Boolean =
        try {
            construct()
            true
        } catch (expected: IllegalArgumentException) {
            check(expected.message != null) { "a rejection must say why" }
            false
        }
}
