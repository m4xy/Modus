package uk.m4xy.modus.core.domain.work

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.work.WorkFixture.ABANDONED
import uk.m4xy.modus.core.domain.work.WorkFixture.ANSWERED
import uk.m4xy.modus.core.domain.work.WorkFixture.AT
import uk.m4xy.modus.core.domain.work.WorkFixture.BACKLOG
import uk.m4xy.modus.core.domain.work.WorkFixture.DOING
import uk.m4xy.modus.core.domain.work.WorkFixture.DRAFT
import uk.m4xy.modus.core.domain.work.WorkFixture.EDITORIAL
import uk.m4xy.modus.core.domain.work.WorkFixture.ENGINEERING
import uk.m4xy.modus.core.domain.work.WorkFixture.INVESTIGATING
import uk.m4xy.modus.core.domain.work.WorkFixture.ITEM
import uk.m4xy.modus.core.domain.work.WorkFixture.LATER
import uk.m4xy.modus.core.domain.work.WorkFixture.MODUS
import uk.m4xy.modus.core.domain.work.WorkFixture.NO_CRITERIA
import uk.m4xy.modus.core.domain.work.WorkFixture.ONE_CRITERION
import uk.m4xy.modus.core.domain.work.WorkFixture.PRINTED
import uk.m4xy.modus.core.domain.work.WorkFixture.QUESTION
import uk.m4xy.modus.core.domain.work.WorkFixture.RESEARCH
import uk.m4xy.modus.core.domain.work.WorkFixture.SHIPPED
import uk.m4xy.modus.core.domain.work.WorkFixture.SUBEDIT
import uk.m4xy.modus.core.domain.work.WorkFixture.item
import uk.m4xy.modus.core.domain.work.event.WorkItemClosed
import uk.m4xy.modus.core.domain.work.event.WorkItemTransitioned
import kotlin.test.Test

/**
 * `doc:00-constitution#domain-scoping`: the state machine and the definition of done are
 * per-domain data and code MUST NOT hardcode a single process.
 *
 * This file is the proof rather than the assertion. Every test below drives **the same
 * aggregate class** through processes that disagree with one another, so any hardcoded
 * vocabulary — an enum, a set of terminal names, a default initial state — contradicts at
 * least one of them and this file goes red.
 */
class WorkItemStateMachineTest {
    /**
     * Two processes with no state name in common. An aggregate that knew any word of either
     * could not start an item correctly in both.
     */
    @Test
    fun `a work item starts wherever its own domain's process says work begins`() {
        item(process = ENGINEERING).state shouldBe BACKLOG
        item(process = RESEARCH).state shouldBe QUESTION
        item(process = EDITORIAL).state shouldBe DRAFT
    }

    /**
     * The sharpest form of the rule: **the same move**, `-> shipped`, ends the work under
     * `ENGINEERING` and does not under `EDITORIAL`. "Is this item finished" is a question
     * only the process can answer.
     *
     * The editorial half carries an unevidenced criterion on purpose. An implementation
     * holding its own set of terminal names does two visible things here — raises a
     * `WorkItemClosed` that did not happen, and refuses a legal move through the evidence
     * guard — so this fails for its own name rather than incidentally. An earlier version
     * had `shipped` as `EDITORIAL`'s *initial* state, so nothing ever moved into it, and a
     * planted hardcoded terminal set passed this test untouched.
     */
    @Test
    fun `one state name is terminal in one process and a legal intermediate in another`() {
        val engineering = item(criteria = NO_CRITERIA, process = ENGINEERING).transitionTo(DOING, ENGINEERING, AT)
        engineering.transitionTo(SHIPPED, ENGINEERING, LATER)
        engineering.pendingEvents
            .last()
            .shouldBeA<WorkItemClosed>()
            .finalState shouldBe SHIPPED

        val editorial = item(criteria = ONE_CRITERION, process = EDITORIAL)
        editorial.transitionTo(SHIPPED, EDITORIAL, LATER)

        editorial.state shouldBe SHIPPED
        editorial.pendingEvents.none { it is WorkItemClosed } shouldBe true
    }

    /**
     * The accepting half of the transition guard. Without it a guard that refused everything
     * would score identically to a correct one (`doc:00-constitution#observed-failing`).
     */
    @Test
    fun `permits exactly the moves this domain's process declares`() {
        val subject = item(criteria = NO_CRITERIA)

        subject.transitionTo(DOING, ENGINEERING, LATER)

        subject.state shouldBe DOING

        val raised = subject.pendingEvents.last().shouldBeA<WorkItemTransitioned>()
        raised.workItemId shouldBe ITEM
        raised.domainId shouldBe MODUS
        raised.from shouldBe BACKLOG
        raised.to shouldBe DOING
        raised.occurredAt shouldBe LATER
    }

    @Test
    fun `refuses a move this domain's process does not declare`() {
        val subject = item(criteria = NO_CRITERIA)

        val thrown = shouldThrow<WorkItemTransitionNotPermittedException> { subject.transitionTo(SHIPPED, ENGINEERING, LATER) }

        thrown.workItemId shouldBe ITEM
        thrown.from shouldBe BACKLOG
        thrown.to shouldBe SHIPPED
        thrown.message shouldBe
            "work item 'modus-0152' cannot move from 'backlog' to 'shipped': " +
            "its domain's process does not permit that transition"
    }

    /**
     * The same move, legal under one process and refused under another, on one item. This is
     * what an implementation holding its own transition table cannot do.
     */
    @Test
    fun `the same move is permitted under one process and refused under another`() {
        item(criteria = NO_CRITERIA, process = RESEARCH).transitionTo(INVESTIGATING, RESEARCH, LATER).state shouldBe INVESTIGATING

        shouldThrow<WorkItemTransitionNotPermittedException> {
            item(criteria = NO_CRITERIA, process = RESEARCH).transitionTo(INVESTIGATING, ENGINEERING, LATER)
        }
    }

    /**
     * A refused transition leaves the item exactly as it was and raises nothing
     * (`doc:20-ddd-practices#aggregates` §2.1.5): the guard runs before the state changes.
     */
    @Test
    fun `a refused transition changes nothing and raises nothing`() {
        val subject = item(criteria = NO_CRITERIA)
        val before = subject.pendingEvents

        shouldThrow<WorkItemTransitionNotPermittedException> { subject.transitionTo(SHIPPED, ENGINEERING, LATER) }

        subject.state shouldBe BACKLOG
        subject.pendingEvents shouldBe before
    }

    /**
     * There is no separate "already closed" check, and there does not need to be:
     * `ProcessDefinition` refuses to declare a transition out of a terminal state, so
     * `allows(terminal, anything)` is false for every process that can be built. The closed
     * item is immovable as a consequence of that invariant rather than of a second check
     * which could disagree with it.
     */
    @Test
    fun `a work item in a terminal state cannot move again`() {
        val subject = item(criteria = NO_CRITERIA).transitionTo(DOING, ENGINEERING, AT).transitionTo(SHIPPED, ENGINEERING, AT)

        shouldThrow<WorkItemTransitionNotPermittedException> { subject.transitionTo(ABANDONED, ENGINEERING, LATER) }
        shouldThrow<WorkItemTransitionNotPermittedException> { subject.transitionTo(DOING, ENGINEERING, LATER) }
    }

    /**
     * A close is published as a transition **and** as a close, in that order. A consumer
     * that only tracks movement never has to special-case the last move; a consumer that
     * only cares about completion never has to reconstruct the process to spot one.
     */
    @Test
    fun `reaching a terminal state raises the transition and then the close`() {
        val subject = item(criteria = NO_CRITERIA).transitionTo(DOING, ENGINEERING, AT)
        subject.drainEvents()

        subject.transitionTo(ABANDONED, ENGINEERING, LATER)

        subject.pendingEvents shouldBe
            listOf(
                WorkItemTransitioned(ITEM, MODUS, DOING, ABANDONED, LATER),
                WorkItemClosed(ITEM, MODUS, ABANDONED, 0, LATER),
            )
    }

    /** Both of a process's terminal states close the work. Neither name is special to this context. */
    @Test
    fun `every terminal state a process declares closes the work`() {
        val shipped = item(criteria = NO_CRITERIA).transitionTo(DOING, ENGINEERING, AT).transitionTo(SHIPPED, ENGINEERING, LATER)
        val abandoned = item(criteria = NO_CRITERIA).transitionTo(ABANDONED, ENGINEERING, LATER)
        val printed =
            item(criteria = NO_CRITERIA, process = EDITORIAL)
                .transitionTo(SHIPPED, EDITORIAL, AT)
                .transitionTo(SUBEDIT, EDITORIAL, AT)
                .transitionTo(PRINTED, EDITORIAL, LATER)

        shipped.pendingEvents
            .last()
            .shouldBeA<WorkItemClosed>()
            .finalState shouldBe SHIPPED
        abandoned.pendingEvents
            .last()
            .shouldBeA<WorkItemClosed>()
            .finalState shouldBe ABANDONED
        printed.pendingEvents
            .last()
            .shouldBeA<WorkItemClosed>()
            .finalState shouldBe PRINTED
    }

    /** A move to a non-terminal state raises no close, whichever process is in force. */
    @Test
    fun `a move to a non-terminal state raises no close`() {
        val subject = item(criteria = NO_CRITERIA, process = RESEARCH).transitionTo(INVESTIGATING, RESEARCH, LATER)

        subject.pendingEvents.none { it is WorkItemClosed } shouldBe true
        subject
            .transitionTo(ANSWERED, RESEARCH, LATER)
            .pendingEvents
            .last()
            .shouldBeA<WorkItemClosed>()
    }
}

/**
 * Narrows and returns, so a test can assert on the event's own fields without an unchecked
 * cast expression at every call site. It fails with the type it actually found, which is
 * what a cast would not tell you.
 */
private inline fun <reified T : Any> Any.shouldBeA(): T {
    check(this is T) { "expected ${T::class.simpleName} but was ${this::class.simpleName}" }
    return this
}
