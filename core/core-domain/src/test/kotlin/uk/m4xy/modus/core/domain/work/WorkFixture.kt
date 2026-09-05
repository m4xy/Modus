package uk.m4xy.modus.core.domain.work

import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.published.ProcessDefinition
import uk.m4xy.modus.core.domain.domainmgmt.published.StateName
import uk.m4xy.modus.core.domain.domainmgmt.published.StateTransition
import uk.m4xy.modus.core.domain.work.aggregate.WorkItem
import uk.m4xy.modus.core.domain.work.published.EpicId
import uk.m4xy.modus.core.domain.work.published.SuccessCriterionId
import uk.m4xy.modus.core.domain.work.published.WorkItemId
import uk.m4xy.modus.core.domain.work.published.WorkItemState
import uk.m4xy.modus.core.domain.work.published.WorkItemTitle
import java.time.Instant

/**
 * Immutable, so no test can leak state into another. Time is a constant, never read.
 *
 * ## Three processes, and the reason there are three
 *
 * `doc:00-constitution#domain-scoping` makes the state machine per-domain data, and a
 * fixture set carrying one process cannot tell a model that reads the process from a model
 * that hardcoded that one process's vocabulary — both pass
 * (`doc:35-testing#fixture-variation`). So:
 *
 * - [ENGINEERING] and [RESEARCH] share **no** state name. An aggregate that knew any word of
 *   either could not drive both.
 * - [EDITORIAL] shares one name with [ENGINEERING] and gives it the opposite meaning:
 *   `shipped` is where [ENGINEERING] ends and where [EDITORIAL] **begins**. Any
 *   implementation carrying a notion of which names are terminal disagrees with one of them.
 *
 * Per `doc:35-testing#fixture-variation` the criteria fixtures also vary across 0, 1 and
 * 2-or-more; [THREE_CRITERIA] is the default because the partly-evidenced case the guard
 * exists for is unreachable below size two.
 */
object WorkFixture {
    val AT: Instant = Instant.parse("2026-09-05T00:00:00Z")
    val LATER: Instant = Instant.parse("2026-09-06T00:00:00Z")

    val MODUS: DomainId = DomainId("modus-core")
    val SKUNKWORKS: DomainId = DomainId("skunkworks")

    val ITEM: WorkItemId = WorkItemId("modus-0152")
    val OTHER_ITEM: WorkItemId = WorkItemId("modus-0153")
    val EPIC: EpicId = EpicId("modus-0013")
    val OTHER_EPIC: EpicId = EpicId("modus-0011")
    val TITLE: WorkItemTitle = WorkItemTitle("The WorkItem aggregate")

    // ---- ENGINEERING -------------------------------------------------------------------
    val BACKLOG: WorkItemState = WorkItemState("backlog")
    val DOING: WorkItemState = WorkItemState("doing")
    val SHIPPED: WorkItemState = WorkItemState("shipped")
    val ABANDONED: WorkItemState = WorkItemState("abandoned")

    /** backlog -> doing -> {shipped, abandoned}, and backlog -> abandoned. Two terminals, one branch. */
    val ENGINEERING: ProcessDefinition =
        ProcessDefinition.of(
            states = setOf(name(BACKLOG), name(DOING), name(SHIPPED), name(ABANDONED)),
            initial = name(BACKLOG),
            terminal = setOf(name(SHIPPED), name(ABANDONED)),
            transitions =
                setOf(
                    StateTransition(name(BACKLOG), name(DOING)),
                    StateTransition(name(BACKLOG), name(ABANDONED)),
                    StateTransition(name(DOING), name(SHIPPED)),
                    StateTransition(name(DOING), name(ABANDONED)),
                ),
        )

    // ---- RESEARCH: no name in common with ENGINEERING ----------------------------------
    val QUESTION: WorkItemState = WorkItemState("question")
    val INVESTIGATING: WorkItemState = WorkItemState("investigating")
    val ANSWERED: WorkItemState = WorkItemState("answered")

    /** question -> investigating -> answered. The smallest interesting process, disjoint from [ENGINEERING]. */
    val RESEARCH: ProcessDefinition =
        ProcessDefinition.of(
            states = setOf(name(QUESTION), name(INVESTIGATING), name(ANSWERED)),
            initial = name(QUESTION),
            terminal = setOf(name(ANSWERED)),
            transitions =
                setOf(
                    StateTransition(name(QUESTION), name(INVESTIGATING)),
                    StateTransition(name(INVESTIGATING), name(ANSWERED)),
                ),
        )

    // ---- EDITORIAL: starts where ENGINEERING ends --------------------------------------
    val SUBEDIT: WorkItemState = WorkItemState("subedit")
    val PRINTED: WorkItemState = WorkItemState("printed")

    /**
     * shipped -> subedit -> printed. `shipped` is this process's **initial** state and
     * [ENGINEERING]'s terminal one, which is what makes "is this item finished" a question
     * only the process can answer.
     */
    val EDITORIAL: ProcessDefinition =
        ProcessDefinition.of(
            states = setOf(name(SHIPPED), name(SUBEDIT), name(PRINTED)),
            initial = name(SHIPPED),
            terminal = setOf(name(PRINTED)),
            transitions =
                setOf(
                    StateTransition(name(SHIPPED), name(SUBEDIT)),
                    StateTransition(name(SUBEDIT), name(PRINTED)),
                ),
        )

    // ---- criteria, at sizes 0, 1 and 3 -------------------------------------------------
    val FIRST: SuccessCriterionId = SuccessCriterionId("c1")
    val SECOND: SuccessCriterionId = SuccessCriterionId("c2")
    val THIRD: SuccessCriterionId = SuccessCriterionId("c3")

    val NO_CRITERIA: List<SuccessCriterion> = emptyList()

    val ONE_CRITERION: List<SuccessCriterion> = listOf(criterion(FIRST))

    val THREE_CRITERIA: List<SuccessCriterion> = listOf(criterion(FIRST), criterion(SECOND), criterion(THIRD))

    val TEST_RUN: EvidenceKind = EvidenceKind("test-run")
    val CITATION: EvidenceKind = EvidenceKind("citation")

    fun criterion(id: SuccessCriterionId): SuccessCriterion = SuccessCriterion(id, CriterionStatement("criterion ${id.value} holds"))

    fun evidence(
        id: SuccessCriterionId,
        kind: EvidenceKind = TEST_RUN,
        at: Instant = AT,
    ): EvidenceRecord = EvidenceRecord(id, kind, EvidenceReference("build/reports/${id.value}.txt:1"), at)

    /** The name `domainmgmt`'s process speaks, for a state this context names. */
    fun name(state: WorkItemState): StateName = StateName(state.value)

    fun item(
        criteria: List<SuccessCriterion> = THREE_CRITERIA,
        process: ProcessDefinition = ENGINEERING,
        epicId: EpicId? = EPIC,
        id: WorkItemId = ITEM,
        domainId: DomainId = MODUS,
    ): WorkItem = WorkItem.create(id, domainId, TITLE, process, AT, criteria, epicId)

    /** An [ENGINEERING] item at `doing`, fully evidenced, one move from a legal close. */
    fun readyToClose(criteria: List<SuccessCriterion> = THREE_CRITERIA): WorkItem {
        val subject = item(criteria).transitionTo(DOING, ENGINEERING, AT)
        criteria.forEach { subject.recordEvidence(evidence(it.id), ENGINEERING) }
        return subject
    }
}
