package uk.m4xy.modus.core.application.event

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.application.ApplicationFixture.AGENTS_READ
import uk.m4xy.modus.core.application.ApplicationFixture.ALICE
import uk.m4xy.modus.core.application.ApplicationFixture.AT
import uk.m4xy.modus.core.application.ApplicationFixture.COST_READ
import uk.m4xy.modus.core.application.ApplicationFixture.LATER
import uk.m4xy.modus.core.application.ApplicationFixture.MODUS
import uk.m4xy.modus.core.application.ApplicationFixture.PROCESS
import uk.m4xy.modus.core.application.HandlerRefused
import uk.m4xy.modus.core.application.RecordingHandler
import uk.m4xy.modus.core.application.ThrowingHandler
import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.domainmgmt.event.ProcessDefinitionChanged
import uk.m4xy.modus.core.domain.identity.event.GrantIssued
import uk.m4xy.modus.core.domain.identity.event.GrantRevoked
import uk.m4xy.modus.core.domain.identity.published.GrantId
import kotlin.test.Test

/**
 * Routing: which subscription gets which event, and what happens when one refuses
 * (`bean:0066` criteria 5, 7 and 8).
 *
 * This is the half of criterion 8 that `WriteThenDispatchTest` cannot supply. There, the
 * assertion is on the list the dispatcher was handed and no handler runs. Here, the
 * assertion is on which handler ran — and `EventSubscription.deliver` reports its own
 * decision, so a subscription that accepted an event and a handler that recorded one are
 * two separately observable facts rather than one inferred from the other.
 */
class SynchronousDomainEventDispatchTest {
    private val revoked = GrantRevoked(GrantId("g1"), ALICE, MODUS, LATER)
    private val issued = GrantIssued(GrantId("g2"), ALICE, MODUS, setOf(AGENTS_READ, COST_READ), AT)
    private val processChanged = ProcessDefinitionChanged(MODUS, PROCESS, LATER)

    private fun <E : DomainEvent> subscribe(
        accepts: (DomainEvent) -> E?,
        handler: DomainEventHandler<E>,
    ) = EventSubscription(accepts, handler)

    // --- the subscription's own decision, observed directly ------------------------------

    @Test
    fun `a subscription reports that it accepted the event it is bound to`() {
        val handler = RecordingHandler<GrantRevoked>()

        val delivered = subscribe({ it as? GrantRevoked }, handler).deliver(revoked)

        delivered shouldBe true
        handler.handled shouldBe listOf(revoked)
    }

    @Test
    fun `a subscription reports that it declined an event it is not bound to, without calling the handler`() {
        val handler = RecordingHandler<GrantRevoked>()

        val delivered = subscribe({ it as? GrantRevoked }, handler).deliver(issued)

        delivered shouldBe false
        handler.handled shouldBe emptyList()
    }

    // --- what the dispatcher does with a list of them -------------------------------------

    @Test
    fun `delivers each event only to the subscriptions that accept it`() {
        val onRevoked = RecordingHandler<GrantRevoked>()
        val onIssued = RecordingHandler<GrantIssued>()
        val dispatch =
            SynchronousDomainEventDispatch(
                listOf(
                    subscribe({ it as? GrantRevoked }, onRevoked),
                    subscribe({ it as? GrantIssued }, onIssued),
                ),
            )

        dispatch.dispatch(listOf(issued, revoked, processChanged))

        onRevoked.handled shouldBe listOf(revoked)
        onIssued.handled shouldBe listOf(issued)
    }

    @Test
    fun `delivers one event to every subscription that accepts it`() {
        val first = RecordingHandler<GrantRevoked>()
        val second = RecordingHandler<GrantRevoked>()
        val dispatch =
            SynchronousDomainEventDispatch(
                listOf(
                    subscribe({ it as? GrantRevoked }, first),
                    subscribe({ it as? GrantRevoked }, second),
                ),
            )

        dispatch.dispatch(listOf(revoked))

        first.handled shouldBe listOf(revoked)
        second.handled shouldBe listOf(revoked)
    }

    @Test
    fun `delivers events in the order it was given them`() {
        val handler = RecordingHandler<DomainEvent>()
        val dispatch = SynchronousDomainEventDispatch(listOf(subscribe({ it }, handler)))

        dispatch.dispatch(listOf(issued, revoked, processChanged))

        handler.handled shouldBe listOf(issued, revoked, processChanged)
    }

    @Test
    fun `an event no subscription accepts is dropped rather than failing`() {
        val handler = RecordingHandler<GrantRevoked>()
        val dispatch = SynchronousDomainEventDispatch(listOf(subscribe({ it as? GrantRevoked }, handler)))

        dispatch.dispatch(listOf(processChanged))

        handler.handled shouldBe emptyList()
    }

    @Test
    fun `dispatching nothing reaches no handler`() {
        val handler = RecordingHandler<DomainEvent>()
        val dispatch = SynchronousDomainEventDispatch(listOf(subscribe({ it }, handler)))

        dispatch.dispatch(emptyList())

        handler.handled shouldBe emptyList()
    }

    @Test
    fun `a dispatcher with no subscription at all delivers nothing and does not fail`() {
        SynchronousDomainEventDispatch(emptyList()).dispatch(listOf(revoked))
    }

    /**
     * The subscription list is copied on the way in, so wiring cannot be changed under a
     * running dispatcher. Asserted at size two — at size one `toList()` returns an immutable
     * singleton and the shape this guards against is not reachable
     * (`doc:35-testing#fixture-variation`).
     */
    @Test
    fun `registering a subscription after construction changes nothing`() {
        val early = RecordingHandler<GrantRevoked>()
        val late = RecordingHandler<GrantRevoked>()
        val wiring =
            mutableListOf(
                subscribe({ it as? GrantRevoked }, early),
                subscribe({ it as? GrantIssued }, RecordingHandler<GrantIssued>()),
            )
        val dispatch = SynchronousDomainEventDispatch(wiring)

        wiring += subscribe({ it as? GrantRevoked }, late)
        dispatch.dispatch(listOf(revoked))

        dispatch.subscriptionCount shouldBe 2
        early.handled shouldBe listOf(revoked)
        late.handled shouldBe emptyList()
    }

    // --- criterion 7: a handler that throws --------------------------------------------

    /**
     * `SynchronousDomainEventDispatch` states its failure behaviour and this is where it is
     * held to it: the exception propagates unchanged, and delivery stops at the failure.
     * Both halves are asserted, because "propagates" alone is satisfied by an implementation
     * that also runs the rest, and "stops" alone by one that swallows.
     */
    @Test
    fun `a handler that throws propagates, and is not swallowed`() {
        val failing = ThrowingHandler<GrantRevoked>("domainmgmt cannot see that domain")
        val dispatch = SynchronousDomainEventDispatch(listOf(subscribe({ it as? GrantRevoked }, failing)))

        shouldThrow<HandlerRefused> { dispatch.dispatch(listOf(revoked)) }
            .message shouldBe "domainmgmt cannot see that domain"

        failing.handled shouldBe listOf(revoked)
    }

    @Test
    fun `delivery stops at the failing handler, and the subscription after it is not reached`() {
        val failing = ThrowingHandler<GrantRevoked>()
        val after = RecordingHandler<GrantRevoked>()
        val dispatch =
            SynchronousDomainEventDispatch(
                listOf(
                    subscribe({ it as? GrantRevoked }, failing),
                    subscribe({ it as? GrantRevoked }, after),
                ),
            )

        shouldThrow<HandlerRefused> { dispatch.dispatch(listOf(revoked)) }

        failing.handled shouldBe listOf(revoked)
        after.handled shouldBe emptyList()
    }

    @Test
    fun `events after the failing one are not delivered`() {
        val failing = ThrowingHandler<GrantRevoked>()
        val everything = RecordingHandler<DomainEvent>()
        val dispatch =
            SynchronousDomainEventDispatch(
                listOf(
                    subscribe({ it }, everything),
                    subscribe({ it as? GrantRevoked }, failing),
                ),
            )

        shouldThrow<HandlerRefused> { dispatch.dispatch(listOf(issued, revoked, processChanged)) }

        everything.handled shouldBe listOf(issued, revoked)
    }
}
