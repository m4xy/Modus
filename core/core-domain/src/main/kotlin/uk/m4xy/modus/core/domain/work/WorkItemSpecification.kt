package uk.m4xy.modus.core.domain.work

import uk.m4xy.modus.core.domain.work.published.EpicId
import uk.m4xy.modus.core.domain.work.published.SuccessCriterionId
import uk.m4xy.modus.core.domain.work.published.WorkItemId
import uk.m4xy.modus.core.domain.work.published.WorkItemTitle

/**
 * What a work item **is**, as opposed to where it currently stands: its identity, what it is
 * called, what it must prove, and which epic it belongs to.
 *
 * Every property here is fixed for the life of the item. `WorkItem` holds one of these and
 * owns only the two things that change — its state and its pending events — which is why the
 * root's constructor takes four arguments rather than eight.
 *
 * ## Why this type exists, stated honestly
 *
 * It was introduced because `WorkItem.create` had seven parameters and Detekt's
 * `LongParameterList` refuses six. The first fix was to default `criteria` to `emptyList()`,
 * and that was **wrong**: an item with no success criteria closes with no evidence — which
 * is the correct rule — so a default made the shortest path to a `WorkItem` the one that
 * produces an item able to close having proved nothing. A hole in the exact rule this
 * context exists to enforce, reachable by writing less code.
 *
 * The lint was not the problem it was reporting. A constructor with too many parameters is a
 * missing concept, and this is the concept: [criteria] is mandatory and always was.
 *
 * ## The duplicate-id invariant lives here
 *
 * Two criteria sharing an id are one criterion the evidence guard counts once and a reader
 * counts twice, so `WorkItemClosed.evidencedCriteria` would disagree with the list that
 * produced it. It is a structural property of this value, not a rule about a transition, so
 * it belongs in this factory rather than in the aggregate
 * (`doc:20-ddd-practices#invariants` §7.1).
 *
 * **Not a `data class`, and not by oversight** (`doc:20-ddd-practices#value-objects` §3.1):
 * it owns a collection, so the criteria are copied on the way in — before they are validated
 * — and on the way out, and `equals`/`hashCode` are hand-written.
 */
public class WorkItemSpecification private constructor(
    public val id: WorkItemId,
    public val title: WorkItemTitle,
    private val declared: List<SuccessCriterion>,
    public val epicId: EpicId?,
) {
    /** A fresh copy every read: mutating it adds no criterion an item must satisfy. */
    public val criteria: List<SuccessCriterion> get() = declared.toList()

    /** Structural equality over the content, since this is a value object with no identity. */
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is WorkItemSpecification &&
                    id == other.id &&
                    title == other.title &&
                    epicId == other.epicId &&
                    declared == other.declared
            )

    override fun hashCode(): Int = listOf(id, title, epicId, declared).hashCode()

    override fun toString(): String = "WorkItemSpecification(id=$id, title=$title, epicId=$epicId, criteria=$criteria)"

    public companion object {
        /**
         * The only way a [WorkItemSpecification] comes into existence. The criteria are
         * copied before they are validated, so what the invariant is checked against is what
         * the instance will answer with.
         *
         * @param criteria may be empty, and **must be written down to be empty**. An item
         *   with nothing to prove closes with no evidence, which is the rule rather than a
         *   hole in it — the rule is a record *per success criterion*, and zero criteria is
         *   zero records owed. It carries no default precisely because that is the shape a
         *   caller must not reach by omission.
         * @param epicId absent by default: an item belonging to no epic is a whole work
         *   item, the null has no second meaning
         *   (`doc:20-ddd-practices#domain-prohibitions` §8.2), and omitting it cannot weaken
         *   a guard.
         */
        public fun of(
            id: WorkItemId,
            title: WorkItemTitle,
            criteria: List<SuccessCriterion>,
            epicId: EpicId? = null,
        ): WorkItemSpecification {
            val held = criteria.toList()
            require(held.map(SuccessCriterion::id).toSet().size == held.size) {
                "work item '${id.value}' declares duplicate success criterion ids: " + duplicates(held)
            }
            return WorkItemSpecification(id, title, held, epicId)
        }

        private fun duplicates(criteria: List<SuccessCriterion>): String =
            criteria
                .map { it.id }
                .groupBy { it }
                .filterValues { it.size > 1 }
                .keys
                .map(SuccessCriterionId::value)
                .sorted()
                .joinToString(", ")
    }
}
