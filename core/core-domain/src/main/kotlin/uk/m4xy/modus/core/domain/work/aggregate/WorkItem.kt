package uk.m4xy.modus.core.domain.work.aggregate

import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.published.ProcessDefinition
import uk.m4xy.modus.core.domain.domainmgmt.published.StateName
import uk.m4xy.modus.core.domain.work.EvidenceRecord
import uk.m4xy.modus.core.domain.work.SuccessCriterion
import uk.m4xy.modus.core.domain.work.UnknownSuccessCriterionException
import uk.m4xy.modus.core.domain.work.WorkItemAlreadyClosedException
import uk.m4xy.modus.core.domain.work.WorkItemNotClosableException
import uk.m4xy.modus.core.domain.work.WorkItemTransitionNotPermittedException
import uk.m4xy.modus.core.domain.work.event.WorkItemClosed
import uk.m4xy.modus.core.domain.work.event.WorkItemCreated
import uk.m4xy.modus.core.domain.work.event.WorkItemTransitioned
import uk.m4xy.modus.core.domain.work.published.EpicId
import uk.m4xy.modus.core.domain.work.published.SuccessCriterionId
import uk.m4xy.modus.core.domain.work.published.WorkItemId
import uk.m4xy.modus.core.domain.work.published.WorkItemState
import uk.m4xy.modus.core.domain.work.published.WorkItemTitle
import java.time.Instant

/**
 * One unit of work. The thing Modus manages, and the root every run, memory and spend
 * figure eventually attributes to.
 *
 * ## The state machine is data this root is handed, never code it contains
 *
 * There is no enum here, no status constant, and no state literal anywhere in this
 * context's `src/main`. Every command that depends on how work moves — [transitionTo],
 * [recordEvidence], and [create] itself — takes the domain's [ProcessDefinition] as a
 * parameter and asks it: where does work start, is this move permitted, does this state end
 * the work. `doc:00-constitution#domain-scoping` requires exactly that: every domain defines
 * its own work-item states and its own definition of done, and code MUST NOT hardcode a
 * single process.
 *
 * The consequence worth stating: **two work items in two domains can be identical in every
 * field and disagree about whether they are finished**, because finishing is a property of
 * the process, not of the item. `WorkItemStateMachineTest` drives one aggregate
 * through two processes with disjoint vocabularies to show it.
 *
 * The process is a parameter rather than a field because it is another aggregate's state
 * (`doc:20-ddd-practices#aggregates` §2.1.3 — reference by identifier, never by object). A
 * `Domain` may adopt a new process at any time; a copy cached here would be a second answer
 * to a question `domainmgmt` owns, and it would be the stale one.
 *
 * ## A close is a transition, and there is no other way to make one
 *
 * `doc:00-constitution#evidence-rule` refuses a work-item transition to done without
 * evidence attached. [transitionTo] is the only writer of the state, and it applies that
 * guard whenever the target is terminal, so there is no second method a caller could use to
 * reach a terminal state without passing it. A separate `close()` beside a permissive
 * `transitionTo()` was rejected for exactly that reason: a guard that a caller can route
 * around is a guard that will be routed around.
 *
 * Entity, not value: two instances carrying the same [id] are the same work item, and
 * [equals]/[hashCode] say so.
 */
public class WorkItem private constructor(
    public val id: WorkItemId,
    public val domainId: DomainId,
    public val epicId: EpicId?,
    public val title: WorkItemTitle,
    // JustifiedVar: the state machine is this root's reason to exist; transitionTo is its
    // only writer, and it validates against the domain's ProcessDefinition first.
    private var currentState: WorkItemState,
    private val criteria: List<SuccessCriterion>,
    private val events: MutableList<DomainEvent>,
) {
    /**
     * Not a constructor parameter, unlike [events]. A work item is created with nothing
     * proved, always — there is no caller that could legitimately supply evidence at
     * creation, because evidence is recorded against a criterion the item does not have
     * until it exists.
     */
    private val evidence: MutableList<EvidenceRecord> = mutableListOf()

    /** Raised, not dispatched: the application layer drains these after the write. */
    public val pendingEvents: List<DomainEvent> get() = events.toList()

    /** Where this item stands right now, in its own domain's vocabulary. */
    public val state: WorkItemState get() = currentState

    /** A fresh copy every read: mutating it adds no criterion this item must satisfy. */
    public val successCriteria: List<SuccessCriterion> get() = criteria.toList()

    /** A fresh copy every read: mutating it evidences nothing. */
    public val evidenceRecords: List<EvidenceRecord> get() = evidence.toList()

    /**
     * Every event raised since the last drain, oldest first, leaving this root with none.
     *
     * Pre: none. Post: the returned events are the caller's, and a second call with no
     * intervening command returns an empty list. The return is a copy, so mutating it puts
     * nothing back into the root.
     *
     * `bean:0066` makes this the contract every aggregate root in Modus adopts, as
     * `uk.m4xy.modus.core.domain.aggregate.RaisesDomainEvents`. That interface is not
     * implemented here because it does not exist on `main` yet: this branch is cut from
     * `main` and `bean:0066` is open. Adopting it is `bean:0153`, which is `blocked_by` it,
     * and is a two-token change — the body is already the one the other three roots use.
     */
    public fun drainEvents(): List<DomainEvent> {
        val drained = events.toList()
        events.clear()
        return drained
    }

    /**
     * Attaches evidence to one of this item's success criteria.
     *
     * Pre: [record] names a criterion this item carries, and this item is not in a state
     * [process] declares terminal. Post: the record is held, and the criterion it names
     * counts as evidenced for the purposes of [transitionTo]'s close guard.
     *
     * Raises no event. The three events this context publishes are named by
     * `doc:10-architecture#bounded-contexts` §3, and adding a fourth extends a published
     * contract; nothing outside this context needs to know that a close became possible,
     * only that it happened.
     *
     * Duplicate evidence for one criterion is accepted and kept. A criterion may be
     * evidenced twice — a test run and a citation — and refusing the second would make the
     * order in which evidence arrived decide what is recorded.
     *
     * @param process this domain's process, which is the only thing that knows whether this
     *   item's current state ends the work.
     */
    public fun recordEvidence(
        record: EvidenceRecord,
        process: ProcessDefinition,
    ): WorkItem {
        if (process.isTerminal(currentState.asStateName())) {
            throw WorkItemAlreadyClosedException(id, currentState)
        }
        if (criteria.none { it.id == record.criterionId }) {
            throw UnknownSuccessCriterionException(id, record.criterionId)
        }
        evidence += record
        return this
    }

    /**
     * Moves this item to [target], if [process] permits the move and — when [target] ends
     * the work — every success criterion carries evidence.
     *
     * Pre: `process.allows(state, target)`, and if `process.isTerminal(target)` then every
     * criterion has at least one [EvidenceRecord]. Post: the item is in [target],
     * [WorkItemTransitioned] is pending, and [WorkItemClosed] follows it when [target] is
     * terminal.
     *
     * Both guards run **before** the state changes, so a refused transition leaves the item
     * exactly as it was and raises nothing (`doc:20-ddd-practices#aggregates` §2.1.5).
     *
     * @throws WorkItemTransitionNotPermittedException when this domain's process does not
     *   declare the move. An item already in a terminal state fails here for every target:
     *   `ProcessDefinition` refuses to declare a transition out of one.
     * @throws WorkItemNotClosableException when [target] ends the work and some criterion
     *   has no evidence. It names every such criterion, not the first
     *   (`doc:00-constitution#evidence-rule`).
     */
    public fun transitionTo(
        target: WorkItemState,
        process: ProcessDefinition,
        at: Instant,
    ): WorkItem {
        if (!process.allows(currentState.asStateName(), target.asStateName())) {
            throw WorkItemTransitionNotPermittedException(id, currentState, target)
        }
        val closing = process.isTerminal(target.asStateName())
        if (closing) {
            val unmet = criteriaWithoutEvidence()
            if (unmet.isNotEmpty()) {
                throw WorkItemNotClosableException(id, unmet)
            }
        }
        val from = currentState
        currentState = target
        events += WorkItemTransitioned(id, domainId, from, target, at)
        if (closing) {
            events += WorkItemClosed(id, domainId, target, criteria.size, at)
        }
        return this
    }

    /**
     * The criteria with no evidence record against them, in declaration order.
     *
     * Per criterion, not "any evidence at all": an item with three criteria and three
     * records against the same one is unmet twice over. That distinction is the whole rule
     * — `doc:00-constitution#evidence-rule` counts per assertion — and it is the one a
     * naive implementation loses.
     */
    private fun criteriaWithoutEvidence(): Set<SuccessCriterionId> {
        val evidenced = evidence.map { it.criterionId }.toSet()
        return criteria.map { it.id }.filterNot { it in evidenced }.toSet()
    }

    /**
     * This context's state name, as the name `domainmgmt`'s process speaks.
     *
     * Total only while [WorkItemState] accepts nothing `StateName` rejects — the two
     * invariants are written to agree and `WorkItemStateMatchesStateNameTest` drives one
     * corpus through both to prove they do. `WorkItemState`'s own KDoc carries why the
     * duplication is forced rather than chosen.
     */
    private fun WorkItemState.asStateName(): StateName = StateName(value)

    /** Entity identity: the [id] alone, never the state or the evidence hanging off it. */
    override fun equals(other: Any?): Boolean = this === other || (other is WorkItem && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    public companion object {
        /**
         * The only way a [WorkItem] comes into existence.
         *
         * It takes the domain's [process] rather than a starting state, so an item cannot be
         * created anywhere but where that domain's process says work begins. A `state`
         * parameter would let a caller create an item already terminal, closing it without
         * ever passing the evidence guard — the guard is on the transition, and an item
         * created in a terminal state never makes one.
         *
         * Criterion ids must be distinct. Two criteria sharing an id are one criterion the
         * evidence guard would count once and a reader would count twice, so the count in
         * [WorkItemClosed] would disagree with the list that produced it. That is a
         * malformed argument rather than a business rule, so it is a `require`
         * (`doc:20-ddd-practices#invariants` §7.2).
         *
         * The last two parameters are the two a work item may genuinely be without, and
         * they are the two the store this models already treats as optional: a bean in
         * `.beans/` carries `parent:` only when it has one, and plenty carry no success
         * criteria at all. Everything before them is required, and deliberately so —
         * `process` most of all, because it is what stops an item being created in a state
         * of the caller's choosing.
         *
         * @param criteria may be empty. An item with nothing to prove closes with no
         *   evidence, and that is correct rather than a hole in the guard: the rule is a
         *   record **per success criterion**, and zero criteria is zero records. Stating a
         *   criterion is what makes evidence owed, and `doc:80-agent-operating-procedure`
         *   step 2 is where an item is required to state one — a procedural rule about
         *   authoring, which this aggregate is not the place to enforce.
         * @param epicId absent by default: an item belonging to no epic is a whole work
         *   item, and the null has no second meaning
         *   (`doc:20-ddd-practices#domain-prohibitions` §8.2).
         */
        public fun create(
            id: WorkItemId,
            domainId: DomainId,
            title: WorkItemTitle,
            process: ProcessDefinition,
            at: Instant,
            criteria: List<SuccessCriterion> = emptyList(),
            epicId: EpicId? = null,
        ): WorkItem {
            val declared = criteria.toList()
            require(declared.map { it.id }.toSet().size == declared.size) {
                "work item '${id.value}' declares duplicate success criterion ids: " +
                    declared
                        .map { it.id.value }
                        .groupBy { it }
                        .filterValues { it.size > 1 }
                        .keys
                        .sorted()
                        .joinToString(", ")
            }
            val initial = WorkItemState(process.initial.value)
            return WorkItem(
                id = id,
                domainId = domainId,
                epicId = epicId,
                title = title,
                currentState = initial,
                criteria = declared,
                events = mutableListOf(WorkItemCreated(id, domainId, epicId, title, initial, at)),
            )
        }
    }
}
