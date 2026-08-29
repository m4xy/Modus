package uk.m4xy.modus.core.domain.identity.aggregate

import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.identity.event.GrantIssued
import uk.m4xy.modus.core.domain.identity.event.GrantRevoked
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.DomainId
import uk.m4xy.modus.core.domain.identity.published.GrantId
import java.time.Instant

/**
 * One actor's access **within one domain**, carrying the capabilities it permits.
 *
 * Invariant: a grant names exactly one [DomainId]. No grant spans domains and none
 * confers administration of another, which is why there is no global administrator to
 * model (`doc:10-architecture#domain-root-convention` §5.5).
 */
public class PermissionGrant private constructor(
    public val id: GrantId,
    public val actorId: ActorId,
    public val domainId: DomainId,
    public val capabilities: Set<Capability>,
    // JustifiedVar: revocation is the only state this root owns and revoke is its only
    // writer. It is a var rather than a new instance so a held reference cannot keep
    // answering `permits` after the grant is gone.
    private var revoked: Boolean,
    private val events: MutableList<DomainEvent>,
) {
    /** Raised, not dispatched: the application layer drains these after the write. */
    public val pendingEvents: List<DomainEvent> get() = events.toList()

    public val isRevoked: Boolean get() = revoked

    /** True while this grant is live and belongs to [actor]. */
    public fun heldBy(actor: ActorId): Boolean = !revoked && actor == actorId

    /** True only for the one actor and the one domain this grant was issued for. */
    public fun covers(
        actor: ActorId,
        domain: DomainId,
    ): Boolean = heldBy(actor) && domain == domainId

    /**
     * Fail closed: a revoked grant permits nothing, and a capability never granted is
     * absent rather than implied by any other capability.
     */
    public fun permits(capability: Capability): Boolean = !revoked && capability in capabilities

    /**
     * Pre: the grant is live. Post: it permits nothing and [GrantRevoked] is pending.
     *
     * Revoking twice is a state error, not a business rule, so it is a `check`
     * (`doc:20-ddd-practices#invariants` §7.2).
     */
    public fun revoke(at: Instant): PermissionGrant {
        check(!revoked) { "grant ${id.value} is already revoked" }
        revoked = true
        events += GrantRevoked(id, actorId, domainId, at)
        return this
    }

    public companion object {
        /**
         * The only way a [PermissionGrant] comes into existence.
         *
         * Invariant: at least one capability. A grant carrying none would make the domain
         * visible while permitting nothing — visibility is exactly what separates a `404`
         * from a `403`, so an empty grant is a leak with no purpose.
         */
        public fun issue(
            id: GrantId,
            actorId: ActorId,
            domainId: DomainId,
            capabilities: Set<Capability>,
            at: Instant,
        ): PermissionGrant {
            require(capabilities.isNotEmpty()) { "grant ${id.value} must carry at least one capability" }
            val granted = capabilities.toSet()
            val issued = GrantIssued(id, actorId, domainId, granted, at)
            return PermissionGrant(id, actorId, domainId, granted, false, mutableListOf(issued))
        }
    }
}
