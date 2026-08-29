package uk.m4xy.modus.core.domain.identity

import uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.DomainId

/**
 * Resolves an actor's effective access across the several grants it may hold — the one
 * rule belonging to no single aggregate (`doc:20-ddd-practices` §6).
 *
 * Fail closed. Every answer derives only from grants that are present, live and covering;
 * absence, revocation and a grant set the caller could not read are the same input, an
 * empty collection, and all three deny. No path here returns [AccessDecision.Permitted]
 * without a grant that says so.
 */
public object PermissionResolver {
    /** The authorisation question every domain-scoped request asks, once. */
    public fun decide(
        actorId: ActorId,
        domainId: DomainId,
        required: Capability,
        grants: Collection<PermissionGrant>,
    ): AccessDecision {
        val covering = grants.filter { it.covers(actorId, domainId) }
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
    ): Set<Capability> = grants.filter { it.covers(actorId, domainId) }.flatMapTo(mutableSetOf()) { it.capabilities }

    /** Exactly the domains the actor may know exist. Anything absent is a `404`. */
    public fun visibleDomains(
        actorId: ActorId,
        grants: Collection<PermissionGrant>,
    ): Set<DomainId> = grants.filter { it.heldBy(actorId) }.mapTo(mutableSetOf()) { it.domainId }
}
