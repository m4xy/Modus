package uk.m4xy.modus.core.domain.work

import uk.m4xy.modus.core.domain.work.published.SuccessCriterionId
import uk.m4xy.modus.core.domain.work.published.WorkItemId
import uk.m4xy.modus.core.domain.work.published.WorkItemState

/**
 * A business rule of the `work` context that the caller is expected to handle and surface
 * (`doc:20-ddd-practices#invariants` §7.2).
 *
 * `require` is wrong for every one of these. It produces an `IllegalArgumentException`,
 * which a REST adapter can only map to one status for every rule in the system; these are
 * distinguishable outcomes a user has to be told apart — "that move is not in your
 * process" and "you have not evidenced criterion 3" are not the same answer.
 *
 * **Sealed within this context, not repository-wide.** §7.2 names a sealed `DomainException`
 * hierarchy; a root spanning every context would belong to no context, which makes it a
 * third shared-kernel member and needs an ADR
 * (`adr:0004-domain-id-shared-kernel#shared-kernel-membership`). Sealing per context still
 * gives the adapter what the rule is for — an exhaustive `when` with no `else ->` branch —
 * and costs no ADR. `bean:0154` carries the wider question.
 *
 * Structural failures stay on `require`: a malformed id or a blank title is a programming
 * error, not a business rule, and it is refused in a value object's `init` before any
 * aggregate sees it.
 */
public sealed class WorkException(
    message: String,
) : RuntimeException(message)

/**
 * The domain's process does not permit this move.
 *
 * Which moves exist is per-domain data and this exception says nothing about them beyond
 * naming the pair that was refused — there is no list of "the legal states" here, because
 * there is no such list in this context (`doc:00-constitution#domain-scoping`).
 *
 * A work item already in a terminal state produces this too, and not a separate "already
 * closed" outcome: `ProcessDefinition` refuses to declare a transition out of a terminal
 * state, so `allows(terminal, anything)` is false for every process that can be built. The
 * closed item is immovable as a consequence of the process's own invariant rather than of a
 * second check that could disagree with it.
 */
public class WorkItemTransitionNotPermittedException(
    public val workItemId: WorkItemId,
    public val from: WorkItemState,
    public val to: WorkItemState,
) : WorkException(
        "work item '${workItemId.value}' cannot move from '${from.value}' to '${to.value}': " +
            "its domain's process does not permit that transition",
    )

/**
 * The close was refused because [unmetCriteria] carry no evidence
 * (`doc:00-constitution#evidence-rule`).
 *
 * It names every unmet criterion rather than the first one found. A caller told about one
 * missing record at a time has to attempt the close once per criterion to discover what is
 * outstanding, and each attempt is an assertion that the work is done.
 *
 * **Not a `data class`, and not by oversight** (`doc:20-ddd-practices#value-objects` §3.1).
 * It owns a collection, so the set is copied on the way in and on the way out: a handler
 * that could add an id to this set would be editing the reason a close was refused, which
 * is `GrantIssued`'s defect in a different type.
 */
public class WorkItemNotClosableException(
    public val workItemId: WorkItemId,
    unmet: Set<SuccessCriterionId>,
) : WorkException(
        "work item '${workItemId.value}' cannot close: no evidence recorded for " +
            unmet.map(SuccessCriterionId::value).sorted().joinToString(", "),
    ) {
    private val withoutEvidence: Set<SuccessCriterionId> = unmet.toSet()

    /** A fresh copy every read: mutating it changes no criterion this refusal names. */
    public val unmetCriteria: Set<SuccessCriterionId> get() = withoutEvidence.toSet()
}

/**
 * Evidence was offered for a criterion this work item does not have.
 *
 * Refused rather than ignored. Silently dropping it would let a caller believe a criterion
 * is evidenced when the close will later say otherwise, and the close is the point at which
 * that is most expensive to discover.
 */
public class UnknownSuccessCriterionException(
    public val workItemId: WorkItemId,
    public val criterionId: SuccessCriterionId,
) : WorkException(
        "work item '${workItemId.value}' has no success criterion '${criterionId.value}'",
    )

/**
 * Evidence was offered to a work item that has already reached a terminal state.
 *
 * The evidence set of a closed item is what its close was justified by. Appending to it
 * afterwards would make the record of a past decision change — the same harm the defensive
 * copy on `GrantIssued` prevents, arriving through the aggregate's own API instead of
 * through a down-cast. An observation is amended, never edited
 * (`doc:00-constitution#workflow` §7.2.5); the amendment is a new work item, not a
 * retrospective edit of this one's justification.
 *
 * Which states end the work is per-domain data, so this refusal is decided by the process
 * passed in and not by anything this context knows.
 */
public class WorkItemAlreadyClosedException(
    public val workItemId: WorkItemId,
    public val state: WorkItemState,
) : WorkException(
        "work item '${workItemId.value}' is closed in state '${state.value}': " +
            "its evidence is what the close was justified by and cannot be added to",
    )
