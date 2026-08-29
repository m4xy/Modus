package uk.m4xy.modus.core.domain.identity

import uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.DomainId
import uk.m4xy.modus.core.domain.identity.published.GrantId
import java.time.Instant

/**
 * Immutable, so no test can leak state into another. Time is a constant, never read.
 *
 * The default grant carries **two** capabilities on purpose. A one-capability grant is
 * backed by Kotlin's immutable `setOf(x)`, which makes an exposed-collection defect throw
 * instead of escalating — every fixture here used to carry exactly one, and that
 * uniformity is what hid the privilege escalation in `PermissionGrant.capabilities`.
 * Anything asserting on capability content names its own set; anything that does not care
 * gets the two-capability default, so the multi-element backing is exercised throughout.
 */
object IdentityFixture {
    val AT: Instant = Instant.parse("2026-08-29T00:00:00Z")
    val ALICE: ActorId = ActorId("alice")
    val BOB: ActorId = ActorId("bob")
    val MODUS: DomainId = DomainId("modus-core")
    val SECRET: DomainId = DomainId("skunkworks")
    val AGENTS_RUN: Capability = Capability("agents.run")
    val AGENTS_READ: Capability = Capability("agents.read")
    val COST_READ: Capability = Capability("cost.read")

    fun grant(
        id: String,
        actor: ActorId = ALICE,
        domain: DomainId = MODUS,
        capabilities: Set<Capability> = setOf(AGENTS_READ, COST_READ),
    ): PermissionGrant = PermissionGrant.issue(GrantId(id), actor, domain, capabilities, AT)
}
