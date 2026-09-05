package uk.m4xy.modus.core.application.domainmgmt.usecase

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.application.ApplicationFixture.ALICE
import uk.m4xy.modus.core.application.ApplicationFixture.LATER
import uk.m4xy.modus.core.application.ApplicationFixture.MODUS
import uk.m4xy.modus.core.application.ApplicationFixture.SKUNKWORKS
import uk.m4xy.modus.core.application.ApplicationFixture.domain
import uk.m4xy.modus.core.application.InMemoryDomainRepository
import uk.m4xy.modus.core.domain.identity.event.GrantRevoked
import uk.m4xy.modus.core.domain.identity.published.GrantId
import kotlin.test.Test

/** `domainmgmt`'s consumer of edge 1, on its own. */
class ObserveGrantRevokedUseCaseTest {
    private val revoked = GrantRevoked(GrantId("g1"), ALICE, MODUS, LATER)

    @Test
    fun `resolves the domain the revoked grant names`() {
        val domains = InMemoryDomainRepository(listOf(domain(MODUS)))

        ObserveGrantRevokedUseCase(domains).handle(revoked)

        domains.lookups shouldBe listOf(MODUS)
    }

    @Test
    fun `refuses an event naming a domain this context does not hold`() {
        val domains = InMemoryDomainRepository(listOf(domain(SKUNKWORKS)))

        shouldThrow<UnknownDomainOnGrantRevoked> { ObserveGrantRevokedUseCase(domains).handle(revoked) }
            .message shouldBe "GrantRevoked names domain modus-core, which domainmgmt does not hold"

        domains.lookups shouldBe listOf(MODUS)
    }

    @Test
    fun `looks the domain up once per event, not once per handler construction`() {
        val domains = InMemoryDomainRepository(listOf(domain(MODUS), domain(SKUNKWORKS, "Skunkworks")))
        val handler = ObserveGrantRevokedUseCase(domains)

        handler.handle(revoked)
        handler.handle(GrantRevoked(GrantId("g2"), ALICE, SKUNKWORKS, LATER))

        domains.lookups shouldBe listOf(MODUS, SKUNKWORKS)
    }
}
