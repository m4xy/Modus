package uk.m4xy.modus.core.domain.identity

import uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.DomainId
import uk.m4xy.modus.core.domain.identity.published.GrantId

/**
 * Resolves an actor's effective access across the several grants it may hold — the one
 * rule belonging to no single aggregate (`doc:20-ddd-practices` §6).
 *
 * Fail closed. Every answer derives only from grants that are present, live and covering;
 * absence, revocation and a grant set the caller could not read are the same input, an
 * empty collection, and all three deny. No path here returns [AccessDecision.Permitted]
 * without a grant that says so.
 *
 * **The duplicate-id rule.** A caller may hand over two instances of one [GrantId] — a
 * warm cache, a retry, or a partial re-read of the flat-file store produces exactly that,
 * and the two can disagree because revocation is mutated in place. Every method here
 * groups by [GrantId] first, and a grant id counts only when **every** instance under it
 * qualifies. One revoked instance therefore denies the whole id, in either order, whatever
 * the collection type. An ambiguous read is a denial, never a permit.
 */
public object PermissionResolver {
    /** The authorisation question every domain-scoped request asks, once. */
    public fun decide(
        actorId: ActorId,
        domainId: DomainId,
        required: Capability,
        grants: Collection<PermissionGrant>,
    ): AccessDecision {
        val covering = covering(actorId, domainId, grants)
        return when {
            covering.isEmpty() -> AccessDecision.DomainNotVisible
            covering.any { it.permits(required) } -> AccessDecision.Permitted
            else -> AccessDecision.CapabilityNotGranted
        }
    }

    /** The union of what the actor may do in one domain; empty when it may do nothing. */
    public fun effectiveCapabilities(
        actorId: ActorId,
        domainId: DomainId,
        grants: Collection<PermissionGrant>,
    ): Set<Capability> = covering(actorId, domainId, grants).flatMapTo(mutableSetOf()) { it.capabilities }

    /** Exactly the domains the actor may know exist. Anything absent is a `404`. */
    public fun visibleDomains(
        actorId: ActorId,
        grants: Collection<PermissionGrant>,
    ): Set<DomainId> = unanimous(grants) { it.heldBy(actorId) }.mapTo(mutableSetOf()) { it.domainId }

    /** The live grants that cover this actor on this domain, one per unambiguous [GrantId]. */
    private fun covering(
        actorId: ActorId,
        domainId: DomainId,
        grants: Collection<PermissionGrant>,
    ): List<PermissionGrant> = unanimous(grants) { it.covers(actorId, domainId) }

    /**
     * One representative per [GrantId], and only for the ids whose every instance
     * satisfies [qualifies]. This is the duplicate-id rule in the class KDoc.
     */
    private fun unanimous(
        grants: Collection<PermissionGrant>,
        qualifies: (PermissionGrant) -> Boolean,
    ): List<PermissionGrant> =
        grants
            .groupBy { it.id }
            .values
            .filter { instances -> instances.all(qualifies) }
            .map { instances -> instances.first() }
}
