package uk.m4xy.modus.core.domain.domainmgmt.port

import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.aggregate.Domain

/**
 * Collection-oriented access to [Domain]s. Declared here, implemented by an adapter
 * (`doc:20-ddd-practices#ports-and-adapters`).
 *
 * There is deliberately **no `findAll`**. `/domains` lists the domains an actor may know
 * exist, and that set comes from `identity`'s `PermissionResolver.visibleDomains` — so the
 * caller names the domains it may load, and this port cannot be the thing that leaks one.
 * A `findAll` would put the 404-not-403 rule (`doc:00-constitution#domain-scoping`) back in
 * the hands of whoever remembers to filter its result.
 *
 * An implementation MUST throw when a domain exists but cannot be read. Returning `null`,
 * or omitting it from [findAllById], is indistinguishable from "no such domain" — which
 * renders as a `404`, so a broken store would silently look like an authorisation outcome.
 */
public interface DomainRepository {
    /** Null means no such domain. It never means "could not read": that is thrown. */
    public fun findById(id: DomainId): Domain?

    /**
     * Exactly the domains among [ids] that exist, in no guaranteed order. An id with no
     * domain is absent from the result rather than an error: the caller asked which of these
     * exist.
     */
    public fun findAllById(ids: Set<DomainId>): List<Domain>

    public fun save(domain: Domain)
}
