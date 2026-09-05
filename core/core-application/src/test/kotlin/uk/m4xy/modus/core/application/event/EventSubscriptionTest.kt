package uk.m4xy.modus.core.application.event

import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.application.ApplicationFixture.AGENTS_READ
import uk.m4xy.modus.core.application.ApplicationFixture.ALICE
import uk.m4xy.modus.core.application.ApplicationFixture.AT
import uk.m4xy.modus.core.application.ApplicationFixture.COST_READ
import uk.m4xy.modus.core.application.ApplicationFixture.LATER
import uk.m4xy.modus.core.application.ApplicationFixture.MODUS
import uk.m4xy.modus.core.application.ApplicationFixture.PROCESS
import uk.m4xy.modus.core.application.RecordingHandler
import uk.m4xy.modus.core.application.ThrowingHandler
import uk.m4xy.modus.core.domain.domainmgmt.event.ProcessDefinitionChanged
import uk.m4xy.modus.core.domain.identity.event.GrantIssued
import uk.m4xy.modus.core.domain.identity.event.GrantRevoked
import uk.m4xy.modus.core.domain.identity.published.GrantId
import kotlin.test.Test

/**
 * One subscription's own routing decision, observed directly.
 *
 * `EventSubscription` stays in `core-application` while every implementation of
 * `DomainEventDispatchPort` lives in an adapter, so its behaviour is asserted here — where
 * the type is declared and where its coverage is attributed
 * (`doc:35-testing#coverage` §8.4). What a dispatcher does with a **list** of them is
 * `adapter-events-inprocess`'s `InProcessDomainEventDispatchTest`.
 */
class EventSubscriptionTest {
    private val revoked = GrantRevoked(GrantId("g1"), ALICE, MODUS, LATER)
    private val issued = GrantIssued(GrantId("g2"), ALICE, MODUS, setOf(AGENTS_READ, COST_READ), AT)
    private val processChanged = ProcessDefinitionChanged(MODUS, PROCESS, LATER)

    @Test
    fun `reports that it accepted the event it is bound to, and delivers it`() {
        val handler = RecordingHandler<GrantRevoked>()

        val delivered = EventSubscription({ it as? GrantRevoked }, handler).deliver(revoked)

        delivered shouldBe true
        handler.handled shouldBe listOf(revoked)
    }

    @Test
    fun `reports that it declined an event it is not bound to, without calling the handler`() {
        val handler = RecordingHandler<GrantRevoked>()

        val delivered = EventSubscription({ it as? GrantRevoked }, handler).deliver(issued)

        delivered shouldBe false
        handler.handled shouldBe emptyList()
    }

    @Test
    fun `a selector that accepts everything delivers every kind of event`() {
        val handler = RecordingHandler<uk.m4xy.modus.core.domain.DomainEvent>()
        val subscription = EventSubscription({ it }, handler)

        subscription.deliver(issued) shouldBe true
        subscription.deliver(revoked) shouldBe true
        subscription.deliver(processChanged) shouldBe true

        handler.handled shouldBe listOf(issued, revoked, processChanged)
    }

    /**
     * A handler's failure is the subscription's caller's problem, not the subscription's:
     * it neither catches nor reports, so `deliver` never returns false because a handler
     * threw. The delivery policy that follows is `doc:20-ddd-practices#domain-events` §4.1.8,
     * asserted against the implementation that owns it.
     */
    @Test
    fun `a handler that throws propagates out of deliver`() {
        val failing = ThrowingHandler<GrantRevoked>("refused")
        val subscription = EventSubscription({ it as? GrantRevoked }, failing)

        runCatching { subscription.deliver(revoked) }.isFailure shouldBe true
        failing.handled shouldBe listOf(revoked)
    }
}
