package uk.m4xy.modus.core.domain.work.port

import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.work.aggregate.Epic
import uk.m4xy.modus.core.domain.work.published.EpicId

/**
 * Collection-oriented access to [Epic]s. Declared here, implemented by an adapter
 * (`doc:20-ddd-practices#ports-and-adapters` §5.2); `bean:0017` implements it.
 *
 * [findAllIn] is scoped by [DomainId] rather than unscoped, for `DomainRepository`'s reason:
 * a finder that could return an epic from a domain the caller may not see would put the
 * 404-not-403 rule (`doc:00-constitution#domain-scoping`) in the hands of whoever remembers
 * to filter its result.
 *
 * An implementation MUST throw when an epic exists but cannot be read, for the reason
 * [WorkItemRepository] gives: a `null` that means "could not read" renders as a `404` and so
 * makes a broken store look like an authorisation outcome.
 */
public interface EpicRepository {
    /** Null means no such epic. It never means "could not read": that is thrown. */
    public fun findById(id: EpicId): Epic?

    /** Every epic in [domainId], in no guaranteed order. Empty is a legitimate answer. */
    public fun findAllIn(domainId: DomainId): List<Epic>

    public fun save(epic: Epic)
}
