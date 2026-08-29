package uk.m4xy.modus.core.domain.identity

import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.identity.IdentityFixture.AGENTS_READ
import uk.m4xy.modus.core.domain.identity.IdentityFixture.AGENTS_RUN
import uk.m4xy.modus.core.domain.identity.IdentityFixture.ALICE
import uk.m4xy.modus.core.domain.identity.IdentityFixture.AT
import uk.m4xy.modus.core.domain.identity.IdentityFixture.BOB
import uk.m4xy.modus.core.domain.identity.IdentityFixture.COST_READ
import uk.m4xy.modus.core.domain.identity.IdentityFixture.MODUS
import uk.m4xy.modus.core.domain.identity.IdentityFixture.SECRET
import uk.m4xy.modus.core.domain.identity.IdentityFixture.grant
import kotlin.test.Test

class PermissionResolverTest {
    @Test
    fun `permits when a live covering grant carries the capability`() {
        val decision =
            PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, listOf(grant("g1", capabilities = setOf(AGENTS_RUN))))

        decision shouldBe AccessDecision.Permitted
        decision.isPermitted shouldBe true
        decision.domainIsVisible shouldBe true
    }

    @Test
    fun `denies without revealing the domain when the actor holds no grant on it`() {
        val onAnotherDomain = listOf(grant("g1", actor = ALICE, domain = SECRET))

        val decision = PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, onAnotherDomain)

        decision shouldBe AccessDecision.DomainNotVisible
        decision.isPermitted shouldBe false
        decision.domainIsVisible shouldBe false
    }

    @Test
    fun `denies without revealing the domain when the grant belongs to another actor`() {
        val decision = PermissionResolver.decide(BOB, MODUS, AGENTS_RUN, listOf(grant("g1", actor = ALICE)))

        decision.domainIsVisible shouldBe false
    }

    @Test
    fun `denies without revealing the domain when the only grant was revoked`() {
        val revoked = grant("g1", capabilities = setOf(AGENTS_RUN)).revoke(AT)

        val decision = PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, listOf(revoked))

        decision shouldBe AccessDecision.DomainNotVisible
    }

    @Test
    fun `denies while revealing the domain when the actor can see it but lacks the capability`() {
        val readOnly = listOf(grant("g1", capabilities = setOf(AGENTS_READ)))

        val decision = PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, readOnly)

        decision shouldBe AccessDecision.CapabilityNotGranted
        decision.isPermitted shouldBe false
        decision.domainIsVisible shouldBe true
    }

    @Test
    fun `denies on an empty grant set, which is also what an unreadable store looks like`() {
        PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, emptyList()) shouldBe AccessDecision.DomainNotVisible
    }

    @Test
    fun `unions the capabilities of every covering grant and of no other`() {
        val grants =
            listOf(
                grant("g1", capabilities = setOf(AGENTS_READ)),
                grant("g2", capabilities = setOf(AGENTS_RUN)),
                grant("g3", domain = SECRET, capabilities = setOf(COST_READ)),
                grant("g4", actor = BOB, capabilities = setOf(COST_READ)),
            )

        PermissionResolver.effectiveCapabilities(ALICE, MODUS, grants) shouldBe setOf(AGENTS_READ, AGENTS_RUN)
    }

    @Test
    fun `lists only the domains the actor is entitled to know exist`() {
        val grants =
            listOf(
                grant("g1", domain = MODUS),
                grant("g2", domain = SECRET).revoke(AT),
                grant("g3", actor = BOB, domain = SECRET),
            )

        PermissionResolver.visibleDomains(ALICE, grants) shouldBe setOf(MODUS)
    }
}
