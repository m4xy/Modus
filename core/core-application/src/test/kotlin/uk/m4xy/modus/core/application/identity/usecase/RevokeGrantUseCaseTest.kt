package uk.m4xy.modus.core.application.identity.usecase

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import uk.m4xy.modus.core.application.ApplicationFixture.ALICE
import uk.m4xy.modus.core.application.ApplicationFixture.BOB
import uk.m4xy.modus.core.application.ApplicationFixture.LATER
import uk.m4xy.modus.core.application.ApplicationFixture.MODUS
import uk.m4xy.modus.core.application.ApplicationFixture.SKUNKWORKS
import uk.m4xy.modus.core.application.ApplicationFixture.grant
import uk.m4xy.modus.core.application.FixedClock
import uk.m4xy.modus.core.application.InMemoryPermissionGrantRepository
import uk.m4xy.modus.core.application.RecordingDispatch
import uk.m4xy.modus.core.application.WriteFailed
import uk.m4xy.modus.core.application.event.DomainEventDispatchPort
import uk.m4xy.modus.core.application.event.WriteThenDispatch
import uk.m4xy.modus.core.domain.identity.event.GrantIssued
import uk.m4xy.modus.core.domain.identity.event.GrantRevoked
import uk.m4xy.modus.core.domain.identity.published.GrantId
import kotlin.test.Test

/**
 * `identity`'s side of edge 1 of `doc:10-architecture#bounded-contexts` §3: what the use
 * case **hands the dispatcher** (`bean:0066` criteria 6 and 8).
 *
 * Every assertion here reads [RecordingDispatch], which delivers to nobody, and that is the
 * point rather than a limitation. `core-application` declares `DomainEventDispatchPort` and
 * may not reach an adapter (`rule:archunit/applicationDependsOnDomainOnly`), so the only
 * thing observable from here is the input surface — which is exactly the half criterion 8
 * asks to be asserted separately. A fixture that constructed a `GrantRevoked` and passed it
 * to a handler would satisfy an end-to-end assertion and say nothing about the drain that
 * produced it. The other half, with a real dispatcher and a real consumer, is
 * `adapter-events-inprocess`'s `GrantRevokedEdgeTest`.
 */
class RevokeGrantUseCaseTest {
    private val command = RevokeGrantCommand(GrantId("g1"), ALICE, MODUS)

    private fun useCase(
        grants: InMemoryPermissionGrantRepository,
        dispatch: DomainEventDispatchPort,
    ) = RevokeGrantUseCase(grants, FixedClock(LATER), WriteThenDispatch(dispatch))

    @Test
    fun `the dispatcher is handed exactly the events the grant raised`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1")))
        val dispatch = RecordingDispatch()

        useCase(grants, dispatch).handle(command)

        grants.contents.single().isRevoked shouldBe true
        dispatch.calls.size shouldBe 1
        dispatch.dispatched.size shouldBe 2
        dispatch.dispatched[0].shouldBeInstanceOf<GrantIssued>()
        val revoked = dispatch.dispatched[1].shouldBeInstanceOf<GrantRevoked>()
        revoked.grantId shouldBe GrantId("g1")
        revoked.actorId shouldBe ALICE
        revoked.domainId shouldBe MODUS
        revoked.occurredAt shouldBe LATER
    }

    @Test
    fun `stamps the revocation from the clock, read once`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1")))
        val clock = FixedClock(LATER)

        RevokeGrantUseCase(grants, clock, WriteThenDispatch(RecordingDispatch())).handle(command)

        clock.readCount shouldBe 1
    }

    /**
     * The whole point of the drain, at the layer that would have shipped the defect: the
     * grant is written a second time and `identity` publishes nothing, because it has
     * nothing left to publish. `domainmgmt` closes an actor's access on `GrantRevoked`; a
     * redelivery closes access that is already closed, and `execution` — edge 1's other
     * consumer once `bean:0014` lands — closes runs a second time.
     */
    @Test
    fun `re-writing the same grant in a second transaction publishes nothing`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1")))
        val dispatch = RecordingDispatch()
        val write = WriteThenDispatch(dispatch)
        RevokeGrantUseCase(grants, FixedClock(LATER), write).handle(command)

        val stored = grants.contents.single()
        write.write(stored) { grants.save(it) }

        dispatch.calls.size shouldBe 2
        dispatch.calls[1] shouldBe emptyList()
        dispatch.dispatched.size shouldBe 2
    }

    // --- criterion 6: dispatch happens after the write ------------------------------------

    @Test
    fun `a failed write dispatches nothing`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1")), failWriteWith = "disk full")
        val dispatch = RecordingDispatch()

        shouldThrow<WriteFailed> { useCase(grants, dispatch).handle(command) }
            .message shouldBe "disk full"

        grants.saved shouldBe listOf(GrantId("g1"))
        dispatch.calls shouldBe emptyList()
    }

    @Test
    fun `a failed write leaves the events on the aggregate for the retry to dispatch`() {
        val held = grant("g1")
        val failing = InMemoryPermissionGrantRepository(listOf(held), failWriteWith = "disk full")
        val dispatch = RecordingDispatch()

        shouldThrow<WriteFailed> { useCase(failing, dispatch).handle(command) }

        dispatch.calls shouldBe emptyList()
        held.pendingEvents.size shouldBe 2
    }

    // --- the use case's own preconditions -------------------------------------------------

    @Test
    fun `refuses a grant the actor does not hold on that domain`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1", actor = BOB)))
        val dispatch = RecordingDispatch()

        shouldThrow<NoSuchGrant> { useCase(grants, dispatch).handle(command) }
            .message shouldBe "no grant g1 for actor alice on domain modus-core"

        grants.saved shouldBe emptyList()
        dispatch.calls shouldBe emptyList()
    }

    @Test
    fun `refuses a grant held on a different domain, and does not write or dispatch`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1", domain = SKUNKWORKS)))
        val dispatch = RecordingDispatch()

        shouldThrow<NoSuchGrant> { useCase(grants, dispatch).handle(command) }

        grants.saved shouldBe emptyList()
        dispatch.calls shouldBe emptyList()
    }

    @Test
    fun `picks the named grant out of the several an actor holds on one domain`() {
        val grants =
            InMemoryPermissionGrantRepository(listOf(grant("g0"), grant("g1"), grant("g2")))
        val dispatch = RecordingDispatch()

        useCase(grants, dispatch).handle(command)

        grants.saved shouldBe listOf(GrantId("g1"))
        grants.contents.filter { it.isRevoked }.map { it.id } shouldBe listOf(GrantId("g1"))
        dispatch.dispatched
            .filterIsInstance<GrantRevoked>()
            .single()
            .grantId shouldBe GrantId("g1")
    }

    @Test
    fun `refuses to revoke a grant that is already revoked, and dispatches nothing for it`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1")))
        val dispatch = RecordingDispatch()
        val subject = useCase(grants, dispatch)
        subject.handle(command)

        shouldThrow<IllegalStateException> { subject.handle(command) }
            .message shouldBe "grant g1 is already revoked"

        dispatch.calls.size shouldBe 1
        dispatch.dispatched.filterIsInstance<GrantRevoked>().size shouldBe 1
    }
}
