package uk.m4xy.modus.core.domain.work.aggregate

import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.work.published.EpicId
import uk.m4xy.modus.core.domain.work.published.WorkItemTitle

/**
 * A body of work several work items belong to.
 *
 * Its own aggregate, not a collection inside [WorkItem]: membership is recorded on the
 * item, as `WorkItem.epicId`, because `doc:20-ddd-practices#aggregates` §2.1.3 makes an
 * aggregate reference another by identifier only, and §2.1.6 rules out a root holding its
 * children. An epic in this repository's own store has dozens of children; loading them to
 * rename it would be the boundary drawn in the wrong place.
 *
 * ## Deliberately thin, and here is the boundary
 *
 * An epic has identity, a home domain and a title, and no behaviour beyond existing. It
 * raises **no domain event**: `doc:10-architecture#bounded-contexts` §3 names the three this
 * context publishes — `WorkItemCreated`, `WorkItemTransitioned`, `WorkItemClosed` — and an
 * `EpicCreated` beside them would add to a published contract that five other contexts read.
 * That is a decision to take with an ADR and a consumer that needs it, not in passing.
 *
 * It carries **no state of its own** for a sharper reason. An epic that moved through the
 * domain's process would need every guard [WorkItem] has, including the evidence guard —
 * and what an epic's success criteria are, given that its children carry their own, is a
 * modelling question `doc:10-architecture` does not answer and this bean does not invent.
 * `bean:0013` records the deferral; `.beans.yml`'s own epics are `type: epic` with a
 * `status`, so the question is real and will come back.
 *
 * Entity, not value: two instances carrying the same [id] are the same epic, whatever they
 * are called, and [equals]/[hashCode] say so. Without it a `Set<Epic>` could hold a stale
 * copy beside a current one and both would answer — the defect `bean:0009` found in
 * `PermissionGrant`.
 */
public class Epic private constructor(
    public val id: EpicId,
    public val domainId: DomainId,
    public val title: WorkItemTitle,
) {
    /** Entity identity: the [id] alone, never the title hanging off it. */
    override fun equals(other: Any?): Boolean = this === other || (other is Epic && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    public companion object {
        /**
         * The only way an [Epic] comes into existence.
         *
         * Every creation invariant it could own already belongs to a value object — `EpicId`
         * and `DomainId` are opaque ids, `WorkItemTitle` is renderable prose — so this
         * factory adds none. That is the intended end state of
         * `doc:20-ddd-practices#value-objects`'s no-primitive-obsession rule, not an
         * omission: there is no argument here that could be wrong and still typecheck.
         *
         * It takes no `at: Instant`, alone among this context's factories, because it raises
         * nothing that could carry one. A timestamp with no event to sit in would be a field
         * kept for a future shape rather than for a fact.
         */
        public fun create(
            id: EpicId,
            domainId: DomainId,
            title: WorkItemTitle,
        ): Epic = Epic(id, domainId, title)
    }
}
