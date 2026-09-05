package uk.m4xy.modus.core.domain.work

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.work.WorkFixture.AT
import uk.m4xy.modus.core.domain.work.WorkFixture.BACKLOG
import uk.m4xy.modus.core.domain.work.WorkFixture.DOING
import uk.m4xy.modus.core.domain.work.WorkFixture.ENGINEERING
import uk.m4xy.modus.core.domain.work.WorkFixture.EPIC
import uk.m4xy.modus.core.domain.work.WorkFixture.FIRST
import uk.m4xy.modus.core.domain.work.WorkFixture.ITEM
import uk.m4xy.modus.core.domain.work.WorkFixture.MODUS
import uk.m4xy.modus.core.domain.work.WorkFixture.NO_CRITERIA
import uk.m4xy.modus.core.domain.work.WorkFixture.ONE_CRITERION
import uk.m4xy.modus.core.domain.work.WorkFixture.OTHER_ITEM
import uk.m4xy.modus.core.domain.work.WorkFixture.SECOND
import uk.m4xy.modus.core.domain.work.WorkFixture.THIRD
import uk.m4xy.modus.core.domain.work.WorkFixture.THREE_CRITERIA
import uk.m4xy.modus.core.domain.work.WorkFixture.TITLE
import uk.m4xy.modus.core.domain.work.WorkFixture.criterion
import uk.m4xy.modus.core.domain.work.WorkFixture.evidence
import uk.m4xy.modus.core.domain.work.WorkFixture.item
import uk.m4xy.modus.core.domain.work.aggregate.WorkItem
import uk.m4xy.modus.core.domain.work.event.WorkItemCreated
import uk.m4xy.modus.core.domain.work.event.WorkItemTransitioned
import uk.m4xy.modus.core.domain.work.published.WorkItemTitle
import kotlin.test.Test

class WorkItemTest {
    @Test
    fun `a created work item carries the id, domain, epic and title it was created with`() {
        val subject = item()

        subject.id shouldBe ITEM
        subject.domainId shouldBe MODUS
        subject.epicId shouldBe EPIC
        subject.title shouldBe TITLE
        subject.successCriteria shouldBe THREE_CRITERIA
        subject.evidenceRecords shouldBe emptyList()
    }

    /**
     * A work item with no epic is a real work item, not a broken one. The null is modelled
     * absence and has no second meaning (`doc:20-ddd-practices#domain-prohibitions` §8.2).
     */
    @Test
    fun `a work item may belong to no epic`() {
        val subject = item(epicId = null)

        subject.epicId shouldBe null
        (subject.pendingEvents.single() as WorkItemCreated).epicId shouldBe null
    }

    /**
     * The event carries the state, so a consumer that has only ever seen `WorkItemCreated`
     * knows where the item began without a port back into `domainmgmt` to read the process.
     */
    @Test
    fun `creating a work item raises exactly one event, carrying the state the process starts in`() {
        val raised = item().pendingEvents.single() as WorkItemCreated

        raised.workItemId shouldBe ITEM
        raised.domainId shouldBe MODUS
        raised.epicId shouldBe EPIC
        raised.title shouldBe TITLE
        raised.state shouldBe BACKLOG
        raised.occurredAt shouldBe AT
    }

    /**
     * Two criteria sharing an id are one criterion the guard counts once and a reader counts
     * twice, so `WorkItemClosed.evidencedCriteria` would disagree with the list that
     * produced it. A malformed argument, so `require` (`doc:20-ddd-practices#invariants` §7.2).
     */
    @Test
    fun `refuses to create a work item whose success criteria share an id`() {
        val thrown =
            shouldThrow<IllegalArgumentException> {
                WorkItemSpecification.of(ITEM, TITLE, listOf(criterion(FIRST), criterion(FIRST)), EPIC)
            }

        thrown.message shouldBe "work item 'modus-0152' declares duplicate success criterion ids: c1"
    }

    /**
     * The refusal names the duplicated id and not the distinct one beside it. A message
     * listing every id a caller supplied tells them nothing about which one is wrong, and a
     * fixture whose ids are *all* duplicated cannot tell the two implementations apart.
     */
    @Test
    fun `a duplicate-id refusal names only the ids that repeat`() {
        val thrown =
            shouldThrow<IllegalArgumentException> {
                WorkItemSpecification.of(ITEM, TITLE, listOf(criterion(FIRST), criterion(SECOND), criterion(FIRST)))
            }

        thrown.message shouldBe "work item 'modus-0152' declares duplicate success criterion ids: c1"
    }

    /** The accepting half: distinct ids at the same size are fine, and both are kept. */
    @Test
    fun `accepts success criteria with distinct ids`() {
        val subject = WorkItemSpecification.of(ITEM, TITLE, listOf(criterion(FIRST), criterion(SECOND)), EPIC)

        subject.criteria.map { it.id } shouldBe listOf(FIRST, SECOND)
    }

    @Test
    fun `accepts a work item with no success criteria at all`() {
        item(criteria = NO_CRITERIA).successCriteria shouldBe emptyList()
    }

    /**
     * `epicId` is the one defaulted argument in this context, and every other call in the
     * suite supplies it — so without this the defaulted path is never executed. `criteria`
     * has **no** default: an item that owes no evidence must be written down as owing none,
     * because it can close having proved nothing.
     */
    @Test
    fun `a work item created without an epic has none, and its criteria are still stated`() {
        val subject = WorkItem.create(WorkItemSpecification.of(ITEM, TITLE, NO_CRITERIA), MODUS, ENGINEERING, AT)

        subject.epicId shouldBe null
        subject.successCriteria shouldBe emptyList()
        subject.state shouldBe BACKLOG
    }

    /**
     * A specification is a value: two built from equal parts are equal, and one built from
     * different parts is not. `equals` is hand-written because the type owns a collection
     * and so cannot be a `data class` (`doc:20-ddd-practices#value-objects` §3.1).
     */
    @Test
    fun `two specifications built from equal parts are equal`() {
        val subject = WorkFixture.spec()

        subject shouldBe WorkFixture.spec()
        subject.hashCode() shouldBe WorkFixture.spec().hashCode()
        subject shouldBe subject
        subject shouldNotBe WorkFixture.spec(criteria = ONE_CRITERION)
        subject shouldNotBe WorkFixture.spec(id = OTHER_ITEM)
        subject shouldNotBe WorkFixture.spec(epicId = null)
        subject shouldNotBe WorkFixture.spec(title = WorkItemTitle("Something else entirely"))
        subject shouldNotBe ITEM
        subject.toString().startsWith("WorkItemSpecification(") shouldBe true

        // The nullable epic is three comparisons, not one, and a fixture that always
        // carries an epic reaches only the first: null-to-null, null-to-present and
        // present-to-null are separate paths through `equals`, and the hash has its own.
        WorkFixture.spec(epicId = null) shouldBe WorkFixture.spec(epicId = null)
        WorkFixture.spec(epicId = null) shouldNotBe subject
        WorkFixture.spec(epicId = null).hashCode() shouldBe WorkFixture.spec(epicId = null).hashCode()
    }

    /**
     * Copy-**in**, which the getter test above cannot reach: a specification built from a
     * list the caller still holds must not change when that list does. The gate in
     * `DefensiveCopySourceTest` does not read a named factory's body
     * (`doc:20-ddd-practices#value-objects` §3.1 says so explicitly), so this invariant has
     * no mechanical guard and is a test or it is nothing.
     */
    @Test
    fun `a caller cannot add a criterion by mutating the list it built the specification from`() {
        val supplied = mutableListOf(criterion(FIRST), criterion(SECOND))
        val subject = WorkItemSpecification.of(ITEM, TITLE, supplied)

        supplied.add(criterion(THIRD))

        subject.criteria.map { it.id } shouldBe listOf(FIRST, SECOND)
    }

    /**
     * Two criteria on purpose: at size one `toList()` returns an immutable singleton and the
     * down-cast throws before it can prove anything (`doc:35-testing#fixture-variation`).
     */
    @Test
    fun `a caller cannot add a criterion through the specification's getter`() {
        val subject = WorkFixture.spec(criteria = THREE_CRITERIA)

        (subject.criteria as MutableList<SuccessCriterion>).clear()

        subject.criteria.size shouldBe 3
    }

    /**
     * The fields of a success criterion and of an evidence record, read one at a time. The
     * suite otherwise compares whole values with `shouldBe`, which exercises `equals` and
     * leaves every accessor unreached.
     */
    @Test
    fun `a success criterion and an evidence record carry what they were given`() {
        val subject = criterion(FIRST)
        subject.id shouldBe FIRST
        subject.statement shouldBe CriterionStatement("criterion c1 holds")

        val record = evidence(FIRST)
        record.criterionId shouldBe FIRST
        record.kind shouldBe WorkFixture.TEST_RUN
        record.reference shouldBe EvidenceReference("build/reports/c1.txt:1")
        record.recordedAt shouldBe AT
    }

    // ---- the drain contract ------------------------------------------------------------

    /**
     * Half one of `bean:0066`'s contract, on its own. The plant that kills it is removing
     * `events.clear()`; nothing else in this file fails when it is removed, which is what
     * makes this test the one that carries the emptying.
     */
    @Test
    fun `drainEvents leaves the root carrying none`() {
        val subject = item().transitionTo(DOING, ENGINEERING, AT)
        subject.pendingEvents.size shouldBe 2

        subject.drainEvents()

        subject.pendingEvents shouldBe emptyList()
        subject.drainEvents() shouldBe emptyList()
    }

    /**
     * Half two, and it fails for its own name rather than for a precondition. A drain that
     * returns the root's live list and then clears it hands the caller an **empty** list —
     * the events are lost, not merely shared — so the assertion that discriminates is about
     * what came back, not about what stayed behind.
     */
    @Test
    fun `drainEvents hands over everything the root had raised, oldest first`() {
        val subject = item().transitionTo(DOING, ENGINEERING, AT)

        subject.drainEvents() shouldBe
            listOf(
                WorkItemCreated(ITEM, MODUS, EPIC, TITLE, BACKLOG, AT),
                WorkItemTransitioned(ITEM, MODUS, BACKLOG, DOING, AT),
            )
    }

    /**
     * Half three: the handover is a copy. Asserting "mutating the drained list does not
     * change the root" immediately after a drain is vacuous — the root is empty either way.
     * So the item raises a fresh event **after** the mutation, and the assertion is that the
     * root's list holds that one event and not the smuggled one. A drain returning the live
     * list puts the addition back into the root and this fails at size 2 (`bean:0009`:
     * `toList()` at size one returns an immutable singleton and the cast throws instead).
     */
    @Test
    fun `a mutation of the drained list puts nothing back into the root`() {
        val subject = item(criteria = NO_CRITERIA).transitionTo(DOING, ENGINEERING, AT)
        val drained = subject.drainEvents()
        drained.size shouldBe 2

        (drained as MutableList<DomainEvent>).add(WorkItemCreated(OTHER_ITEM, MODUS, null, TITLE, BACKLOG, AT))
        subject.transitionTo(WorkFixture.SHIPPED, ENGINEERING, AT)

        subject.pendingEvents.map { it::class.simpleName } shouldBe listOf("WorkItemTransitioned", "WorkItemClosed")
    }

    /**
     * `pendingEvents` is a read, not a handover. Two events on purpose: at size one
     * `toList()` returns `Collections.singletonList`, whose `clear()` throws before it can
     * prove anything (`doc:35-testing#fixture-variation`).
     */
    @Test
    fun `pendingEvents is a copy, so clearing it cannot empty the root`() {
        val subject = item().transitionTo(DOING, ENGINEERING, AT)

        (subject.pendingEvents as MutableList<DomainEvent>).clear()

        subject.pendingEvents.size shouldBe 2
    }

    // ---- defensive copies --------------------------------------------------------------

    @Test
    fun `a caller cannot add a success criterion through the getter`() {
        val subject = item(criteria = THREE_CRITERIA)

        (subject.successCriteria as MutableList<SuccessCriterion>).clear()

        subject.successCriteria.size shouldBe 3
    }

    /**
     * The escalation shape `bean:0009` shipped, in this aggregate: evidence added through
     * the getter would satisfy the close guard for a criterion nobody evidenced.
     *
     * Two records already held, on purpose. At size zero `toList()` returns `EmptyList` and
     * at size one `Collections.singletonList`, and the down-cast the exploit needs throws on
     * both — the test would pass for the wrong reason (`doc:35-testing#fixture-variation`,
     * and `bean:0009` is the escalation that shipped past exactly this).
     */
    @Test
    fun `a caller cannot evidence a criterion through the evidence getter`() {
        val subject = item(criteria = THREE_CRITERIA).transitionTo(DOING, ENGINEERING, AT)
        subject.recordEvidence(evidence(FIRST), ENGINEERING)
        subject.recordEvidence(evidence(SECOND), ENGINEERING)

        (subject.evidenceRecords as MutableList<EvidenceRecord>).add(evidence(THIRD))

        subject.evidenceRecords.map { it.criterionId } shouldBe listOf(FIRST, SECOND)
        shouldThrow<WorkItemNotClosableException> {
            subject.transitionTo(WorkFixture.SHIPPED, ENGINEERING, AT)
        }.unmetCriteria shouldBe setOf(THIRD)
    }

    // ---- identity ----------------------------------------------------------------------

    /**
     * Entity, not value. Without this a `Set<WorkItem>` could hold a stale copy beside a
     * current one and both would answer — `bean:0009`'s defect in `PermissionGrant`.
     */
    @Test
    fun `two instances of one work item id are the same work item, whatever their state`() {
        val backlog = item()
        val moved = item().transitionTo(DOING, ENGINEERING, AT)

        backlog shouldBe moved
        backlog.hashCode() shouldBe moved.hashCode()
        backlog shouldBe backlog
    }

    @Test
    fun `work items with different ids are different, and a work item is not some other type`() {
        item() shouldNotBe item(id = OTHER_ITEM)
        item() shouldNotBe ITEM
    }
}
