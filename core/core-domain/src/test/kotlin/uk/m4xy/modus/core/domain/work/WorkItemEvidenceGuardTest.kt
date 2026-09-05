package uk.m4xy.modus.core.domain.work

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.work.WorkFixture.ABANDONED
import uk.m4xy.modus.core.domain.work.WorkFixture.AT
import uk.m4xy.modus.core.domain.work.WorkFixture.CITATION
import uk.m4xy.modus.core.domain.work.WorkFixture.DOING
import uk.m4xy.modus.core.domain.work.WorkFixture.ENGINEERING
import uk.m4xy.modus.core.domain.work.WorkFixture.FIRST
import uk.m4xy.modus.core.domain.work.WorkFixture.ITEM
import uk.m4xy.modus.core.domain.work.WorkFixture.LATER
import uk.m4xy.modus.core.domain.work.WorkFixture.MODUS
import uk.m4xy.modus.core.domain.work.WorkFixture.NO_CRITERIA
import uk.m4xy.modus.core.domain.work.WorkFixture.ONE_CRITERION
import uk.m4xy.modus.core.domain.work.WorkFixture.SECOND
import uk.m4xy.modus.core.domain.work.WorkFixture.SHIPPED
import uk.m4xy.modus.core.domain.work.WorkFixture.THIRD
import uk.m4xy.modus.core.domain.work.WorkFixture.THREE_CRITERIA
import uk.m4xy.modus.core.domain.work.WorkFixture.evidence
import uk.m4xy.modus.core.domain.work.WorkFixture.item
import uk.m4xy.modus.core.domain.work.WorkFixture.readyToClose
import uk.m4xy.modus.core.domain.work.event.WorkItemClosed
import uk.m4xy.modus.core.domain.work.published.SuccessCriterionId
import kotlin.test.Test

/**
 * `doc:00-constitution#evidence-rule`: no assertion is recorded as true without evidence
 * attached, and that applies to **every work-item transition to done**. This is that rule as
 * a domain guard.
 *
 * Both halves are here. A guard that refused every close would satisfy the rejecting tests
 * and nothing else (`doc:00-constitution#observed-failing`), so each refusal below has an
 * accepting counterpart that differs by exactly the thing the guard is about.
 */
class WorkItemEvidenceGuardTest {
    @Test
    fun `refuses a close when no success criterion carries evidence`() {
        val subject = item(criteria = THREE_CRITERIA).transitionTo(DOING, ENGINEERING, AT)

        val thrown = shouldThrow<WorkItemNotClosableException> { subject.transitionTo(SHIPPED, ENGINEERING, LATER) }

        thrown.workItemId shouldBe ITEM
        thrown.unmetCriteria shouldBe setOf(FIRST, SECOND, THIRD)
        thrown.message shouldBe "work item 'modus-0152' cannot close: no evidence recorded for c1, c2, c3"
    }

    /** The accepting half: the same item, the same move, with a record against each criterion. */
    @Test
    fun `permits a close when every success criterion carries evidence`() {
        val subject = readyToClose()

        subject.transitionTo(SHIPPED, ENGINEERING, LATER)

        subject.state shouldBe SHIPPED

        val raised = subject.pendingEvents.last() as WorkItemClosed
        raised.workItemId shouldBe ITEM
        raised.domainId shouldBe MODUS
        raised.finalState shouldBe SHIPPED
        raised.evidencedCriteria shouldBe 3
        raised.occurredAt shouldBe LATER
    }

    /**
     * The rule is a record **per success criterion**. A guard written as "some evidence
     * exists", or as "there are at least as many records as criteria", passes this fixture's
     * three records and is wrong: they all evidence the same criterion.
     */
    @Test
    fun `refuses a close when three records all evidence one criterion`() {
        val subject = item(criteria = THREE_CRITERIA).transitionTo(DOING, ENGINEERING, AT)
        subject.recordEvidence(evidence(FIRST), ENGINEERING)
        subject.recordEvidence(evidence(FIRST, CITATION), ENGINEERING)
        subject.recordEvidence(evidence(FIRST, CITATION, LATER), ENGINEERING)

        val thrown = shouldThrow<WorkItemNotClosableException> { subject.transitionTo(SHIPPED, ENGINEERING, LATER) }

        thrown.unmetCriteria shouldBe setOf(SECOND, THIRD)
    }

    /** A partly evidenced item names exactly what is outstanding, not the first gap found. */
    @Test
    fun `a refused close names every unevidenced criterion, not the first`() {
        val subject = item(criteria = THREE_CRITERIA).transitionTo(DOING, ENGINEERING, AT)
        subject.recordEvidence(evidence(SECOND), ENGINEERING)

        val thrown = shouldThrow<WorkItemNotClosableException> { subject.transitionTo(SHIPPED, ENGINEERING, LATER) }

        thrown.unmetCriteria shouldBe setOf(FIRST, THIRD)
        thrown.message shouldBe "work item 'modus-0152' cannot close: no evidence recorded for c1, c3"
    }

    /**
     * Zero criteria is zero records owed, and that is the rule rather than a hole in it:
     * stating a criterion is what makes evidence owed. Every state-machine test in this
     * context relies on it, so it is asserted here rather than assumed there.
     */
    @Test
    fun `an item with no success criteria closes with no evidence`() {
        val subject = item(criteria = NO_CRITERIA)

        subject.transitionTo(ABANDONED, ENGINEERING, LATER)

        subject.state shouldBe ABANDONED
        subject.pendingEvents.last() shouldBe WorkItemClosed(ITEM, MODUS, ABANDONED, 0, LATER)
    }

    /**
     * The guard is on the close, not on movement. An item may travel its whole process
     * unevidenced and is stopped only where `doc:00-constitution#evidence-rule` stops it.
     */
    @Test
    fun `a move to a non-terminal state needs no evidence`() {
        val subject = item(criteria = THREE_CRITERIA)

        subject.transitionTo(DOING, ENGINEERING, LATER)

        subject.state shouldBe DOING
        subject.evidenceRecords shouldBe emptyList()
    }

    /**
     * A refused close leaves the item exactly as it was: the guard runs before the state
     * changes (`doc:20-ddd-practices#aggregates` §2.1.5). Without this an item could be left
     * closed by a transition that threw.
     */
    @Test
    fun `a refused close changes nothing and raises nothing`() {
        val subject = item(criteria = ONE_CRITERION).transitionTo(DOING, ENGINEERING, AT)
        val before = subject.pendingEvents

        shouldThrow<WorkItemNotClosableException> { subject.transitionTo(SHIPPED, ENGINEERING, LATER) }

        subject.state shouldBe DOING
        subject.pendingEvents shouldBe before
    }

    /** The count in the event is the criteria the close was justified against, not the record count. */
    @Test
    fun `the close event reports the number of criteria evidenced, not the number of records`() {
        val subject = readyToClose(ONE_CRITERION)
        subject.recordEvidence(evidence(FIRST, CITATION), ENGINEERING)

        subject.transitionTo(SHIPPED, ENGINEERING, LATER)

        subject.evidenceRecords.size shouldBe 2
        subject.pendingEvents.last() shouldBe WorkItemClosed(ITEM, MODUS, SHIPPED, 1, LATER)
    }

    /**
     * Two records on purpose. At size one `toSet()` returns an immutable singleton and the
     * cast throws before it can prove anything (`doc:35-testing#fixture-variation`).
     */
    @Test
    fun `a caller cannot edit the set of criteria a refusal names`() {
        val subject = item(criteria = THREE_CRITERIA).transitionTo(DOING, ENGINEERING, AT)
        subject.recordEvidence(evidence(THIRD), ENGINEERING)
        val thrown = shouldThrow<WorkItemNotClosableException> { subject.transitionTo(SHIPPED, ENGINEERING, LATER) }

        (thrown.unmetCriteria as MutableSet<SuccessCriterionId>).clear()

        thrown.unmetCriteria shouldBe setOf(FIRST, SECOND)
    }

    // ---- recording evidence ------------------------------------------------------------

    @Test
    fun `refuses evidence for a criterion this work item does not have`() {
        val subject = item(criteria = ONE_CRITERION)

        val thrown = shouldThrow<UnknownSuccessCriterionException> { subject.recordEvidence(evidence(SECOND), ENGINEERING) }

        thrown.workItemId shouldBe ITEM
        thrown.criterionId shouldBe SECOND
        thrown.message shouldBe "work item 'modus-0152' has no success criterion 'c2'"
        subject.evidenceRecords shouldBe emptyList()
    }

    @Test
    fun `accepts evidence for a criterion this work item has`() {
        val subject = item(criteria = ONE_CRITERION)

        subject.recordEvidence(evidence(FIRST), ENGINEERING)

        subject.evidenceRecords shouldBe listOf(evidence(FIRST))
    }

    /**
     * A criterion may be evidenced twice — a test run and a citation. Refusing the second
     * would let the order evidence arrived in decide what is recorded.
     */
    @Test
    fun `accepts a second record against a criterion that already has one`() {
        val subject = item(criteria = ONE_CRITERION)

        subject.recordEvidence(evidence(FIRST), ENGINEERING)
        subject.recordEvidence(evidence(FIRST, CITATION, LATER), ENGINEERING)

        subject.evidenceRecords shouldBe listOf(evidence(FIRST), evidence(FIRST, CITATION, LATER))
    }

    /**
     * The evidence set of a closed item is what its close was justified by, and it does not
     * change afterwards. Which states end the work is per-domain data, so this refusal is
     * decided by the process passed in — the item is unchanged and only the process differs.
     */
    @Test
    fun `refuses evidence for a work item that has already closed`() {
        val subject = readyToClose(ONE_CRITERION).transitionTo(SHIPPED, ENGINEERING, LATER)

        val thrown = shouldThrow<WorkItemAlreadyClosedException> { subject.recordEvidence(evidence(FIRST, CITATION), ENGINEERING) }

        thrown.workItemId shouldBe ITEM
        thrown.state shouldBe SHIPPED
        thrown.message shouldBe
            "work item 'modus-0152' is closed in state 'shipped': " +
            "its evidence is what the close was justified by and cannot be added to"
        subject.evidenceRecords.size shouldBe 1
    }

    /**
     * The accepting half of the same guard, and the proof that it reads the process rather
     * than a name: an item sitting in `shipped` — which ends the work under `ENGINEERING` —
     * accepts evidence under `EDITORIAL`, which passes through it.
     */
    @Test
    fun `accepts evidence in a state another process would call closed`() {
        val subject = item(criteria = ONE_CRITERION, process = WorkFixture.EDITORIAL).transitionTo(SHIPPED, WorkFixture.EDITORIAL, AT)
        subject.state shouldBe SHIPPED

        subject.recordEvidence(evidence(FIRST), WorkFixture.EDITORIAL)

        subject.evidenceRecords shouldBe listOf(evidence(FIRST))
    }
}
