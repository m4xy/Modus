package uk.m4xy.modus.core.domain.identity.port

import uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.DomainId
import uk.m4xy.modus.core.domain.identity.published.GrantId

/**
 * Collection-oriented access to [PermissionGrant]s. Declared here, implemented by an
 * adapter (`doc:20-ddd-practices#ports-and-adapters`).
 *
 * An implementation MUST throw when a grant exists but cannot be read. Returning an empty
 * set for an unreadable store is indistinguishable from "no grant" — which the resolver
 * correctly denies, but the denial would be the only trace of a broken store.
 *
 * An implementation MUST return at most one instance per [GrantId], and that instance MUST
 * be the current one. A cache, a retry or a partial re-read that returns a live alias
 * beside a revoked one is a contract violation.
 *
 * The reads return `List`, not `Set`, deliberately: a `Set` would silently drop the second
 * instance of a duplicated [GrantId] — and which of the two it drops is arbitrary, so the
 * revoked one can be the one discarded. A `List` carries the violation through to
 * `PermissionResolver`, whose duplicate-id rule denies it.
 */
public interface PermissionGrantRepository {
    /** Every grant this actor holds on this one domain, revoked ones included. */
    public fun grantsFor(
        actorId: ActorId,
        domainId: DomainId,
    ): List<PermissionGrant>

    /** Every grant this actor holds, across every domain it can reach. */
    public fun grantsFor(actorId: ActorId): List<PermissionGrant>

    /** Every grant on one domain — the read behind `/domains/{domainId}/grants`. */
    public fun grantsOn(domainId: DomainId): List<PermissionGrant>

    public fun save(grant: PermissionGrant)
}
