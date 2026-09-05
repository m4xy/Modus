package uk.m4xy.modus.adapter.events.inprocess

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.adapter.events.inprocess.EventsFixture.ALICE
import uk.m4xy.modus.adapter.events.inprocess.EventsFixture.LATER
import uk.m4xy.modus.adapter.events.inprocess.EventsFixture.MODUS
import uk.m4xy.modus.adapter.events.inprocess.EventsFixture.SKUNKWORKS
import uk.m4xy.modus.adapter.events.inprocess.EventsFixture.domain
import uk.m4xy.modus.adapter.events.inprocess.EventsFixture.grant
import uk.m4xy.modus.core.application.domainmgmt.usecase.ObserveGrantRevokedUseCase
import uk.m4xy.modus.core.application.domainmgmt.usecase.UnknownDomainOnGrantRevoked
import uk.m4xy.modus.core.application.event.EventSubscription
import uk.m4xy.modus.core.application.event.WriteThenDispatch
import uk.m4xy.modus.core.application.identity.usecase.RevokeGrantCommand
import uk.m4xy.modus.core.application.identity.usecase.RevokeGrantUseCase
import uk.m4xy.modus.core.domain.identity.event.GrantRevoked
import uk.m4xy.modus.core.domain.identity.published.GrantId
import kotlin.test.Test

/**
 * Edge 1 of `doc:10-architecture#bounded-contexts` §3, end to end: `identity` revokes a
 * grant, `domainmgmt` observes `GrantRevoked` (`bean:0066` criteria 5, 6 and 7).
 *
 * It lives in this module because it is the only place the whole edge can be assembled:
 * `core-application` declares the port and may not reach an adapter, so a test there can
 * only ever see the events the dispatcher was **handed**
 * (`core-application`'s `RevokeGrantUseCaseTest` does exactly that, and is the other half).
 *
 * "End to end" means every collaborator that decides anything is real — the use case,
 * `WriteThenDispatch`, `InProcessDomainEventDispatch`, `ObserveGrantRevokedUseCase` and the
 * `PermissionGrant` aggregate. The two doubles are repository ports, which decide nothing,
 * and there is no adapter to substitute for them: `bean:0009` declared both ports and
 * implemented neither, and `bean:0017` builds the flat-file store.
 */
class GrantRevokedEdgeTest {
    private val command = RevokeGrantCommand(GrantId("g1"), ALICE, MODUS)

    private fun edge(
        grants: InMemoryPermissionGrantRepository,
        domains: InMemoryDomainRepository,
    ): RevokeGrantUseCase {
        val dispatch =
            InProcessDomainEventDispatch(
                listOf(EventSubscription({ it as? GrantRevoked }, ObserveGrantRevokedUseCase(domains))),
            )
        return RevokeGrantUseCase(grants, FixedClock(LATER), WriteThenDispatch(dispatch))
    }

    @Test
    fun `revoking a grant reaches domainmgmt's handler through the dispatcher`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1")))
        val domains = InMemoryDomainRepository(listOf(domain(MODUS)))

        edge(grants, domains).handle(command)

        grants.contents.single().isRevoked shouldBe true
        domains.lookups shouldBe listOf(MODUS)
    }

    /**
     * Criterion 6. The healthy case above is what makes this one mean something: a
     * dispatcher never called for any input would pass this assertion identically
     * (`doc:00-constitution#observed-failing`).
     */
    @Test
    fun `a failed write reaches no handler at all`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1")), failWriteWith = "disk full")
        val domains = InMemoryDomainRepository(listOf(domain(MODUS)))

        shouldThrow<WriteFailed> { edge(grants, domains).handle(command) }
            .message shouldBe "disk full"

        grants.saved shouldBe listOf(GrantId("g1"))
        domains.lookups shouldBe emptyList()
    }

    /**
     * Criterion 7, through a real use case rather than a throwing double: the handler's own
     * refusal surfaces to the caller, and the write it followed still stands. Whether that
     * is the right coupling to put on a post-write path is `bean:0031`'s question, not this
     * test's — the test pins what happens today.
     */
    @Test
    fun `a handler that refuses surfaces to the caller, with the write already done`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1")))
        val domains = InMemoryDomainRepository(listOf(domain(SKUNKWORKS, "Skunkworks")))

        shouldThrow<UnknownDomainOnGrantRevoked> { edge(grants, domains).handle(command) }

        grants.contents.single().isRevoked shouldBe true
        grants.saved shouldBe listOf(GrantId("g1"))
    }

    /**
     * The whole point of the drain, assembled: the grant is written a second time and
     * `domainmgmt` is not told a second time, because `identity` has nothing left to tell.
     */
    @Test
    fun `re-writing the same grant in a second transaction reaches no handler`() {
        val grants = InMemoryPermissionGrantRepository(listOf(grant("g1")))
        val domains = InMemoryDomainRepository(listOf(domain(MODUS)))
        val dispatch =
            InProcessDomainEventDispatch(
                listOf(EventSubscription({ it as? GrantRevoked }, ObserveGrantRevokedUseCase(domains))),
            )
        val write = WriteThenDispatch(dispatch)
        RevokeGrantUseCase(grants, FixedClock(LATER), write).handle(command)
        domains.lookups shouldBe listOf(MODUS)

        write.write(grants.contents.single()) { grants.save(it) }

        domains.lookups shouldBe listOf(MODUS)
    }
}
