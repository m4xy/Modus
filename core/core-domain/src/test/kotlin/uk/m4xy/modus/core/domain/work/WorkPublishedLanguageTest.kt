package uk.m4xy.modus.core.domain.work

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.port.SequenceIdGenerator
import uk.m4xy.modus.core.domain.work.published.EpicId
import uk.m4xy.modus.core.domain.work.published.SuccessCriterionId
import uk.m4xy.modus.core.domain.work.published.WorkItemId
import uk.m4xy.modus.core.domain.work.published.WorkItemState
import uk.m4xy.modus.core.domain.work.published.WorkItemTitle
import kotlin.test.Test

/**
 * Every invariant in this context's published language and its internal value objects, with
 * an accepting and a rejecting case each (`doc:20-ddd-practices#invariants` §7.3).
 */
class WorkPublishedLanguageTest {
    @Test
    fun `accepts the identifier shapes an adapter may use unencoded as a file name`() {
        WorkItemId("modus-0152").value shouldBe "modus-0152"
        WorkItemId("a").value shouldBe "a"
        WorkItemId("a".repeat(64)).value shouldBe "a".repeat(64)
        EpicId("epic.2026_01-a").value shouldBe "epic.2026_01-a"
        SuccessCriterionId("c1").value shouldBe "c1"
    }

    @Test
    fun `refuses an identifier that could not survive a URL, a file name or a log field`() {
        listOf("", " ", "Modus", "with space", "-leading", "trailing-", "a/b", "..", "a".repeat(65), "naïve")
            .forEach { shouldThrow<IllegalArgumentException> { WorkItemId(it) } }
    }

    @Test
    fun `every identifier in this context reports its own name when it refuses`() {
        shouldThrow<IllegalArgumentException> { WorkItemId("A") }.message!!.startsWith("workItemId") shouldBe true
        shouldThrow<IllegalArgumentException> { EpicId("A") }.message!!.startsWith("epicId") shouldBe true
        shouldThrow<IllegalArgumentException> {
            SuccessCriterionId("A")
        }.message!!.startsWith("successCriterionId") shouldBe true
    }

    /**
     * A constant that must match an authority (`bean:0090`): `IdGeneratorPort.newId()`
     * promises 1..64 characters of `a-z`, `0-9`, `.`, `_` and `-`, opening and closing
     * alphanumeric, and this context wraps what it returns. If the two ever disagree, a
     * freshly generated id throws at the wrap.
     */
    @Test
    fun `every id this repository's generator promises can be wrapped by this context`() {
        val generator = SequenceIdGenerator(listOf("a", "a".repeat(64), "modus-0152", "epic.2026_01-a", "0"))

        repeat(generator.remaining) {
            val fresh = generator.newId()
            WorkItemId(fresh).value shouldBe fresh
            EpicId(fresh).value shouldBe fresh
            SuccessCriterionId(fresh).value shouldBe fresh
        }
    }

    // ---- WorkItemState -----------------------------------------------------------------

    @Test
    fun `accepts a state name of any shape a domain may choose`() {
        WorkItemState("todo").value shouldBe "todo"
        WorkItemState("needs-more-evidence").value shouldBe "needs-more-evidence"
        WorkItemState("a".repeat(64)).value shouldBe "a".repeat(64)
    }

    /**
     * The length is checked separately from the pattern, because the segment quantifier
     * bounds each run between hyphens and not the whole string — the defect `StateName`
     * shipped with, where the message promised 64 and `a-a-a-…` was accepted at 399.
     */
    @Test
    fun `refuses a state name longer than its message promises, hyphens included`() {
        shouldThrow<IllegalArgumentException> { WorkItemState("a".repeat(65)) }
        shouldThrow<IllegalArgumentException> { WorkItemState(List(40) { "a" }.joinToString("-")) }
    }

    @Test
    fun `refuses a state name that could not survive a URL, a file name or a log field`() {
        listOf("", " ", "Todo", "in progress", "in_progress", "-leading", "trailing-", "double--hyphen", "has.dot")
            .forEach { shouldThrow<IllegalArgumentException> { WorkItemState(it) } }
    }

    // ---- WorkItemTitle -----------------------------------------------------------------

    @Test
    fun `accepts a title a human would write`() {
        WorkItemTitle("The WorkItem aggregate").value shouldBe "The WorkItem aggregate"
        WorkItemTitle("a").value shouldBe "a"
        WorkItemTitle("a".repeat(200)).value shouldBe "a".repeat(200)
    }

    @Test
    fun `refuses a title that is untrimmed, empty, oversized, or carries control characters`() {
        shouldThrow<IllegalArgumentException> { WorkItemTitle(" leading") }
        shouldThrow<IllegalArgumentException> { WorkItemTitle("trailing ") }
        shouldThrow<IllegalArgumentException> { WorkItemTitle("") }
        shouldThrow<IllegalArgumentException> { WorkItemTitle("a".repeat(201)) }
        shouldThrow<IllegalArgumentException> { WorkItemTitle("two\nlines") }
        shouldThrow<IllegalArgumentException> { WorkItemTitle("bell\u0007") }
    }

    // ---- the evidence vocabulary -------------------------------------------------------

    /**
     * Not an enum, and this test is what says so: `doc:00-constitution#domain-scoping` lists
     * required evidence kinds beside states and the definition of done as things a domain
     * defines for itself, so a kind this repository has never heard of is accepted.
     */
    @Test
    fun `accepts an evidence kind no document in this repository names`() {
        EvidenceKind("test-run").value shouldBe "test-run"
        EvidenceKind("notarised-witness-statement").value shouldBe "notarised-witness-statement"
    }

    @Test
    fun `refuses an evidence kind that could not survive a file name or a log field`() {
        listOf("", " ", "Test-Run", "test run", "test_run", "-leading", "a".repeat(65))
            .forEach { shouldThrow<IllegalArgumentException> { EvidenceKind(it) } }
    }

    @Test
    fun `accepts an evidence reference of any shape a citation may take`() {
        EvidenceReference("WorkItem.kt:142").value shouldBe "WorkItem.kt:142"
        EvidenceReference("./gradlew qualityCheck -> exit 0").value shouldBe "./gradlew qualityCheck -> exit 0"
        EvidenceReference("a".repeat(500)).value shouldBe "a".repeat(500)
    }

    /**
     * The bound is what separates a reference from a payload: the command output tail lives
     * in the work item's file and in a memory, not in this aggregate
     * (`doc:20-ddd-practices#aggregates` §2.1.6). A multi-line paste is refused here, which
     * is where that separation is decided.
     */
    @Test
    fun `refuses an evidence reference that is a payload rather than a pointer`() {
        shouldThrow<IllegalArgumentException> { EvidenceReference("a".repeat(501)) }
        shouldThrow<IllegalArgumentException> { EvidenceReference("BUILD FAILED\n> Task :check") }
        shouldThrow<IllegalArgumentException> { EvidenceReference("") }
        shouldThrow<IllegalArgumentException> { EvidenceReference(" untrimmed") }
    }

    @Test
    fun `accepts a criterion statement a reader can check the work against`() {
        CriterionStatement("The gate was observed rejecting a plant").value shouldBe "The gate was observed rejecting a plant"
        CriterionStatement("a".repeat(500)).value shouldBe "a".repeat(500)
    }

    /**
     * Newlines are refused, and that is a modelling decision rather than a formatting one: a
     * criterion needing one is two criteria, and splitting it is what makes each half
     * countable by the evidence guard.
     */
    @Test
    fun `refuses a criterion statement that is two criteria, empty, or oversized`() {
        shouldThrow<IllegalArgumentException> { CriterionStatement("first\nsecond") }
        shouldThrow<IllegalArgumentException> { CriterionStatement("") }
        shouldThrow<IllegalArgumentException> { CriterionStatement("a".repeat(501)) }
        shouldThrow<IllegalArgumentException> { CriterionStatement("untrimmed ") }
    }
}
