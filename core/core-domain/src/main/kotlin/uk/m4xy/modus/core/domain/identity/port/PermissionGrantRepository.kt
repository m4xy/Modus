package uk.m4xy.modus.core.domain.identity.port

import uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.DomainId

/**
 * Collection-oriented access to [PermissionGrant]s. Declared here, implemented by an
 * adapter (`doc:20-ddd-practices#ports-and-adapters`).
 *
 * An implementation MUST throw when a grant exists but cannot be read. Returning an empty
 * set for an unreadable store is indistinguishable from "no grant" — which the resolver
 * correctly denies, but the denial would be the only trace of a broken store.
 */
public interface PermissionGrantRepository {
    /** Every grant this actor holds on this one domain, revoked ones included. */
    public fun grantsFor(
        actorId: ActorId,
        domainId: DomainId,
    ): Set<PermissionGrant>

    /** Every grant this actor holds, across every domain it can reach. */
    public fun grantsFor(actorId: ActorId): Set<PermissionGrant>

    /** Every grant on one domain — the read behind `/domains/{domainId}/grants`. */
    public fun grantsOn(domainId: DomainId): Set<PermissionGrant>

    public fun save(grant: PermissionGrant)
}
