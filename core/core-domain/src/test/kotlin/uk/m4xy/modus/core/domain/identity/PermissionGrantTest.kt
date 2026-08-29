package uk.m4xy.modus.core.domain.identity

import io.kotest.assertions.throwables.shouldThrow
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
import uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant
import uk.m4xy.modus.core.domain.identity.event.GrantIssued
import uk.m4xy.modus.core.domain.identity.event.GrantRevoked
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.GrantId
import kotlin.test.Test

class PermissionGrantTest {
    @Test
    fun `issuing raises GrantIssued carrying exactly the capabilities granted`() {
        val issued = grant("g1", capabilities = setOf(AGENTS_READ, AGENTS_RUN))

        issued.id shouldBe GrantId("g1")
        issued.actorId shouldBe ALICE
        issued.domainId shouldBe MODUS
        issued.capabilities shouldBe setOf(AGENTS_READ, AGENTS_RUN)
        issued.isRevoked shouldBe false

        val event = issued.pendingEvents.single() as GrantIssued
        event.grantId shouldBe GrantId("g1")
        event.actorId shouldBe ALICE
        event.domainId shouldBe MODUS
        event.capabilities shouldBe setOf(AGENTS_READ, AGENTS_RUN)
        event.occurredAt shouldBe AT
    }

    @Test
    fun `refuses to issue a grant carrying no capability`() {
        shouldThrow<IllegalArgumentException> { grant("g1", capabilities = emptySet()) }
            .message shouldBe "grant g1 must carry at least one capability"
    }

    @Test
    fun `permits every capability it was granted and denies one it was not`() {
        val issued = grant("g1", capabilities = setOf(AGENTS_READ, COST_READ))

        issued.permits(AGENTS_READ) shouldBe true
        issued.permits(COST_READ) shouldBe true
        issued.permits(AGENTS_RUN) shouldBe false
    }

    @Test
    fun `covers only the one actor and the one domain it names`() {
        val issued = grant("g1", actor = ALICE, domain = MODUS)

        issued.covers(ALICE, MODUS) shouldBe true
        issued.covers(BOB, MODUS) shouldBe false
        issued.covers(ALICE, SECRET) shouldBe false
        issued.heldBy(ALICE) shouldBe true
    }

    @Test
    fun `a revoked grant covers nothing and permits nothing`() {
        val issued = grant("g1", capabilities = setOf(AGENTS_READ, COST_READ))

        issued.revoke(AT)

        issued.isRevoked shouldBe true
        issued.permits(AGENTS_READ) shouldBe false
        issued.permits(COST_READ) shouldBe false
        issued.covers(ALICE, MODUS) shouldBe false
        issued.heldBy(ALICE) shouldBe false
    }

    @Test
    fun `revoking raises GrantRevoked after the issue event`() {
        val issued = grant("g1")

        val event = issued.revoke(AT).pendingEvents.last() as GrantRevoked
        event.grantId shouldBe GrantId("g1")
        event.actorId shouldBe ALICE
        event.domainId shouldBe MODUS
        event.occurredAt shouldBe AT
    }

    @Test
    fun `refuses to revoke a grant that is already revoked`() {
        val issued = grant("g1")
        issued.revoke(AT)

        shouldThrow<IllegalStateException> { issued.revoke(AT) }
            .message shouldBe "grant g1 is already revoked"
    }

    @Test
    fun `does not share the capability set it was issued with`() {
        val caller = mutableSetOf(AGENTS_READ, COST_READ)
        val issued = PermissionGrant.issue(GrantId("g1"), ALICE, MODUS, caller, AT)

        caller += AGENTS_RUN

        issued.permits(AGENTS_RUN) shouldBe false
        issued.capabilities shouldBe setOf(AGENTS_READ, COST_READ)
    }

    /**
     * The outbound half of the same rule, and the one that was open: `capabilities` used
     * to return the backing collection. Kotlin's `Set` is a read-only view, not an
     * immutable type, so for two or more capabilities the caller can down-cast it to
     * `MutableSet` and add one nobody granted. Two capabilities on purpose — with one, the
     * backing is `setOf(x)` and the cast throws, which is what hid this.
     */
    @Suppress("UNCHECKED_CAST")
    @Test
    fun `does not hand out the capability set it decides with`() {
        val issued = grant("g1", capabilities = setOf(AGENTS_READ, COST_READ))

        (issued.capabilities as MutableSet<Capability>).add(AGENTS_RUN)

        issued.permits(AGENTS_RUN) shouldBe false
        issued.capabilities shouldBe setOf(AGENTS_READ, COST_READ)
        PermissionResolver.decide(ALICE, MODUS, AGENTS_RUN, listOf(issued)) shouldBe
            AccessDecision.CapabilityNotGranted
    }

    /**
     * Entity, not value: identity is the [GrantId] and nothing else. Without this a
     * `Set<PermissionGrant>` holds a live alias beside a revoked one and the resolver
     * counts both.
     */
    @Test
    fun `is the same grant as any other instance carrying its id`() {
        val issued = grant("g1", capabilities = setOf(AGENTS_READ))
        val alias = grant("g1", capabilities = setOf(COST_READ)).revoke(AT)
        val another = grant("g2")

        (issued == issued) shouldBe true
        (issued == alias) shouldBe true
        (issued == another) shouldBe false
        issued.equals(issued.id) shouldBe false
        issued.hashCode() shouldBe alias.hashCode()
        setOf(issued, alias).size shouldBe 1
    }

    @Test
    fun `pending events cannot be mutated through the root`() {
        val issued = grant("g1")

        val drained = issued.pendingEvents
        issued.revoke(AT)

        drained.size shouldBe 1
        issued.pendingEvents.size shouldBe 2
    }
}
