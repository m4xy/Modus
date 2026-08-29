package uk.m4xy.modus.core.domain.identity

import uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.DomainId
import uk.m4xy.modus.core.domain.identity.published.GrantId
import java.time.Instant

/** Immutable, so no test can leak state into another. Time is a constant, never read. */
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
        capabilities: Set<Capability> = setOf(AGENTS_READ),
    ): PermissionGrant = PermissionGrant.issue(GrantId(id), actor, domain, capabilities, AT)
}
