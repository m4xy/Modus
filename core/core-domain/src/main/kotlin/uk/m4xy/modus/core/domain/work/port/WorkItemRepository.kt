package uk.m4xy.modus.core.domain.work.port

import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.work.aggregate.WorkItem
import uk.m4xy.modus.core.domain.work.published.EpicId
import uk.m4xy.modus.core.domain.work.published.WorkItemId
import uk.m4xy.modus.core.domain.work.published.WorkItemState

/**
 * Collection-oriented access to [WorkItem]s. Declared here, implemented by an adapter
 * (`doc:20-ddd-practices#ports-and-adapters` §5.2). `bean:0017` implements it over the
 * Markdown-with-frontmatter files in `.beans/`; nothing in `core` knows that.
 *
 * There is deliberately **no `findOpenIn(domainId)`**, and the omission is the same rule
 * the whole context is built on. "Open" means "not in a state this domain's process
 * declares terminal", and a store cannot answer that without the process in hand — so a
 * finder with that name would either hardcode a set of closed states, which
 * `doc:00-constitution#domain-scoping` forbids, or take a `ProcessDefinition` and push
 * `domainmgmt`'s published language into every adapter that implements this port.
 * [findAllInState] names the state instead, and the caller, which has the process, decides
 * which states those are.
 *
 * There is no unqualified `findAll` either, for `DomainRepository`'s reason: every finder
 * here is scoped by a [DomainId] or by a key that already belongs to one domain, so this
 * port cannot be the thing that leaks a work item across the 404-not-403 boundary
 * (`doc:00-constitution#domain-scoping`).
 *
 * An implementation MUST throw when a work item exists but cannot be read. Returning `null`,
 * or omitting it from a list, is indistinguishable from "no such item" — which renders as a
 * `404`, so a broken store would silently look like an authorisation outcome.
 */
public interface WorkItemRepository {
    /** Null means no such work item. It never means "could not read": that is thrown. */
    public fun findById(id: WorkItemId): WorkItem?

    /**
     * Every work item in [domainId] currently in [state], in no guaranteed order.
     *
     * The state is a name, not a status: the caller supplies one its domain's process
     * declares, and an implementation matches it opaquely. An implementation that special-cased
     * a value — treating `done` differently from any other name — would be hardcoding one
     * domain's process into the store.
     */
    public fun findAllInState(
        domainId: DomainId,
        state: WorkItemState,
    ): List<WorkItem>

    /**
     * Every work item whose `epicId` is [epicId], in no guaranteed order. Empty for an epic
     * with no children, and empty for an epic that does not exist — the caller asked which
     * items name it, and no item naming a missing epic is the same answer as no item.
     */
    public fun findAllInEpic(epicId: EpicId): List<WorkItem>

    public fun save(item: WorkItem)
}
