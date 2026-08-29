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
        val carriesRun = listOf(grant("g1", capabilities = setOf(AGENTS_RUN, COST_READ)))

        val decision = PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, carriesRun)

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
        val revoked = grant("g1", capabilities = setOf(AGENTS_RUN, COST_READ)).revoke(AT)

        val decision = PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, listOf(revoked))

        decision shouldBe AccessDecision.DomainNotVisible
    }

    @Test
    fun `denies while revealing the domain when the actor can see it but lacks the capability`() {
        val readOnly = listOf(grant("g1", capabilities = setOf(AGENTS_READ, COST_READ)))

        val decision = PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, readOnly)

        decision shouldBe AccessDecision.CapabilityNotGranted
        decision.isPermitted shouldBe false
        decision.domainIsVisible shouldBe true
    }

    @Test
    fun `denies on an empty grant set, which is also what an unreadable store looks like`() {
        PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, emptyList()) shouldBe AccessDecision.DomainNotVisible
    }

    /**
     * A warm cache, a retry or a partial re-read hands over a live alias beside a revoked
     * one. The revocation must win in either order and whatever the collection type: this
     * is the one input shape where the resolver used to fail open.
     */
    @Test
    fun `denies when any instance of a grant id was revoked, in either order`() {
        val revoked = grant("g1", capabilities = setOf(AGENTS_RUN, COST_READ)).revoke(AT)
        val staleAlias = grant("g1", capabilities = setOf(AGENTS_RUN, COST_READ))

        listOf(listOf(revoked, staleAlias), listOf(staleAlias, revoked), setOf(revoked, staleAlias)).forEach { grants ->
            PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, grants) shouldBe AccessDecision.DomainNotVisible
            PermissionResolver.effectiveCapabilities(ALICE, MODUS, grants) shouldBe emptySet()
            PermissionResolver.visibleDomains(ALICE, grants) shouldBe emptySet()
        }
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

    /**
     * The leak-freedom argument rests on this returning `emptySet()` for a domain the
     * actor may no longer know exists — a caller that renders "what can I do here" from a
     * non-empty answer admits the domain, which is a `404`-not-`403` leak, not a
     * permissions one.
     */
    @Test
    fun `a revoked grant contributes no effective capability`() {
        val revoked = grant("g1", capabilities = setOf(AGENTS_RUN, COST_READ)).revoke(AT)

        PermissionResolver.effectiveCapabilities(ALICE, MODUS, listOf(revoked)) shouldBe emptySet()
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
