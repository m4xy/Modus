package uk.m4xy.modus.core.domain.work.aggregate

import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.published.ProcessDefinition
import uk.m4xy.modus.core.domain.domainmgmt.published.StateName
import uk.m4xy.modus.core.domain.work.EvidenceRecord
import uk.m4xy.modus.core.domain.work.UnknownSuccessCriterionException
import uk.m4xy.modus.core.domain.work.WorkItemAlreadyClosedException
import uk.m4xy.modus.core.domain.work.WorkItemNotClosableException
import uk.m4xy.modus.core.domain.work.WorkItemSpecification
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
    public val specification: WorkItemSpecification,
    public val domainId: DomainId,
    // JustifiedVar: the state machine is this root's reason to exist; transitionTo is its
    // only writer, and it validates against the domain's ProcessDefinition first.
    private var currentState: WorkItemState,
    private val events: MutableList<DomainEvent>,
) {
    /** This item's identity, fixed for its life. */
    public val id: WorkItemId get() = specification.id

    /** What this item is called. Retitling is not modelled; it would be a new specification. */
    public val title: WorkItemTitle get() = specification.title

    /** The epic this item belongs to, or null for one that belongs to none. */
    public val epicId: EpicId? get() = specification.epicId

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
        requireGoverning(process)
        if (process.isTerminal(currentState.asStateName())) {
            throw WorkItemAlreadyClosedException(id, currentState)
        }
        if (specification.criteria.none { it.id == record.criterionId }) {
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
        requireGoverning(process)
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
            events += WorkItemClosed(id, domainId, target, specification.criteria.size, at)
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
        return specification.criteria
            .map { it.id }
            .filterNot { it in evidenced }
            .toSet()
    }

    /**
     * Refuses a process that does not govern this item, before any guard reads it.
     *
     * **A partial check, and the part it does not cover is `bean:0157`.** It refuses a
     * process that cannot describe this item at all. It does **not** refuse one that
     * declares the item's state and disagrees about what that state means: an item at
     * `doing` with unevidenced criteria can still be moved to `shipped` under a legally
     * constructible process in which `shipped` is an ordinary intermediate, landing in a
     * state its own domain calls terminal with nothing proved and no [WorkItemClosed]
     * raised. Membership cannot catch that — any process permitting a move out of `doing`
     * must declare `doing`, so the check is implied by the move being permitted.
     *
     * Closing it needs something binding this item to *its* domain's process, and nothing
     * here can be that: caching the process would be caching another aggregate's state, and
     * it would go stale the moment the domain adopts a new one. The obligation is the use
     * case's — load the process for [domainId], never accept one from the caller — which is
     * `bean:0153`'s to discharge and `bean:0157`'s to state.
     *
     * A `require`, not a domain exception: handing an aggregate a process that cannot
     * describe it is a programming error rather than a business rule a caller is expected to
     * surface (`doc:20-ddd-practices#invariants` §7.2).
     */
    private fun requireGoverning(process: ProcessDefinition) {
        require(currentState.asStateName() in process.states) {
            "work item '${id.value}' is in state '${currentState.value}', which the process " +
                "supplied does not declare: that process does not govern this item"
        }
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
         * Everything an item is born with arrives as one [WorkItemSpecification], which is
         * where the duplicate-criterion-id invariant lives and which makes its criteria
         * mandatory. An earlier version took the criteria here with a default of
         * `emptyList()`, to get under Detekt's `LongParameterList`; that made the shortest
         * path to a `WorkItem` the one producing an item able to close having proved
         * nothing — a hole in the rule this whole context exists to enforce, reachable by
         * writing less code. The lint was reporting a missing concept, not asking for a
         * default.
         */
        public fun create(
            specification: WorkItemSpecification,
            domainId: DomainId,
            process: ProcessDefinition,
            at: Instant,
        ): WorkItem {
            val initial = WorkItemState(process.initial.value)
            return WorkItem(
                specification = specification,
                domainId = domainId,
                currentState = initial,
                events =
                    mutableListOf(
                        WorkItemCreated(
                            specification.id,
                            domainId,
                            specification.epicId,
                            specification.title,
                            initial,
                            at,
                        ),
                    ),
            )
        }
    }
}
