package uk.m4xy.modus.core.domain.work.event

import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.work.published.EpicId
import uk.m4xy.modus.core.domain.work.published.WorkItemId
import uk.m4xy.modus.core.domain.work.published.WorkItemState
import uk.m4xy.modus.core.domain.work.published.WorkItemTitle
import java.time.Instant

/**
 * A unit of work now exists, in whatever state its domain's process starts work in.
 *
 * [state] is carried rather than left implicit because a consumer cannot otherwise know
 * where the item began without a port back into `domainmgmt` to read the process — which is
 * the coupling `doc:10-architecture#bounded-contexts` §3.1's published-language split exists
 * to prevent. It is never a constant: `WorkItem.create` takes it from the process it is
 * handed (`doc:00-constitution#domain-scoping`).
 *
 * [epicId] is null for an item that belongs to no epic. That is modelled absence and not a
 * flag standing in for `false` (`doc:20-ddd-practices#domain-prohibitions` §8.2): a work
 * item genuinely may have no parent, and there is no second meaning for the null.
 *
 * **No actor.** `doc:20-ddd-practices#domain-events` §4.1's worked example of this context's
 * events carries an `actorId: ActorId`, and it cannot be built:
 * `rule:archunit/publishedLanguageIsLeaf` permits a type in `..domain.<ctx>.event..` to
 * reference only the Kotlin stdlib, `java.time`, **its own** context's published language
 * and the shared kernel, so no event here may name `identity.published.ActorId`. §3.1's
 * allowlist does permit `work` to import `identity`'s published language — from this
 * context's *internals*, which is a different package. Attribution's whole value is in the
 * event, so it is left out rather than modelled where it cannot be published;
 * `bean:0154` carries correcting the document and deciding whether `ActorId` becomes a
 * shared-kernel member, which needs an ADR.
 */
public data class WorkItemCreated(
    public val workItemId: WorkItemId,
    public val domainId: DomainId,
    public val epicId: EpicId?,
    public val title: WorkItemTitle,
    public val state: WorkItemState,
    override val occurredAt: Instant,
) : DomainEvent

/**
 * A unit of work moved. `execution` consumes this
 * (`doc:10-architecture#bounded-contexts` §3).
 *
 * [from] and [to] are names this domain's process declared, never members of an enum. A
 * consumer that wants to know whether [to] ends the work asks the domain's process, because
 * that is the only thing that knows — `done` in one domain is an intermediate state in
 * another, and both are correct.
 *
 * Raised for a close as well as for an ordinary move, with [WorkItemClosed] following it.
 * A close **is** a transition; publishing it as one keeps a consumer that only tracks
 * movement from having to special-case the last one.
 */
public data class WorkItemTransitioned(
    public val workItemId: WorkItemId,
    public val domainId: DomainId,
    public val from: WorkItemState,
    public val to: WorkItemState,
    override val occurredAt: Instant,
) : DomainEvent

/**
 * A unit of work reached a state its domain's process declares terminal, with an evidence
 * record against every success criterion it carried. `memory` consumes this
 * (`doc:10-architecture#bounded-contexts` §3).
 *
 * The event is the claim that `doc:00-constitution#evidence-rule` was satisfied, which is
 * why it exists beside [WorkItemTransitioned] rather than being inferrable from it: a
 * consumer would otherwise need the domain's process in hand to tell a close from a move,
 * and inferring an assertion is how an unevidenced one gets recorded as true.
 *
 * [evidencedCriteria] is a count, not the records. The records are this aggregate's
 * internals and `memory` may not import them (§3.1); the count is what makes the claim
 * falsifiable at the edge — an item with three criteria that closes reporting two did not
 * pass the guard that raised this.
 */
public data class WorkItemClosed(
    public val workItemId: WorkItemId,
    public val domainId: DomainId,
    public val finalState: WorkItemState,
    public val evidencedCriteria: Int,
    override val occurredAt: Instant,
) : DomainEvent
