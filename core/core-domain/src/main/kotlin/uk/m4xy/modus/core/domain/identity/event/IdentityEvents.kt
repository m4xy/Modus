package uk.m4xy.modus.core.domain.identity.event

import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.ActorKind
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.GrantId
import java.time.Instant

/** A principal now exists. It can reach nothing until a grant says otherwise. */
public data class ActorRegistered(
    public val actorId: ActorId,
    public val kind: ActorKind,
    override val occurredAt: Instant,
) : DomainEvent

/**
 * An actor was given access to one domain, carrying exactly [capabilities].
 *
 * **Not a `data class`, and not by oversight** (`doc:20-ddd-practices#value-objects` §3.1,
 * which §4.1.2 defers to for an event carrying a collection). It published its
 * `Set<Capability>` as a constructor `val` until `bean:0036`. `PermissionGrant.issue` handed
 * the event a copy, so the grant itself was safe — but the event's own set was still an
 * ordinary `LinkedHashSet` a handler could down-cast, adding a capability to a fact that had
 * **already happened**. That is `bean:0030`'s harm, in the context `bean:0009` was supposed to
 * have fixed, and the third occurrence of one defect. It was found by the gate in
 * `DefensiveCopySourceTest`, not by review.
 *
 * Copied on the way in and on the way out. Structural equality is hand-written, because a
 * `data class` can do neither.
 */
public class GrantIssued(
    public val grantId: GrantId,
    public val actorId: ActorId,
    public val domainId: DomainId,
    granted: Set<Capability>,
    override val occurredAt: Instant,
) : DomainEvent {
    private val issued: Set<Capability> = granted.toSet()

    /** A fresh copy every read: mutating it changes no fact this event states. */
    public val capabilities: Set<Capability> get() = issued.toSet()

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is GrantIssued &&
                    grantId == other.grantId &&
                    actorId == other.actorId &&
                    domainId == other.domainId &&
                    occurredAt == other.occurredAt &&
                    issued == other.issued
            )

    override fun hashCode(): Int = listOf(grantId, actorId, domainId, occurredAt, issued).hashCode()

    override fun toString(): String =
        "GrantIssued(grantId=$grantId, actorId=$actorId, domainId=$domainId, " +
            "capabilities=$capabilities, occurredAt=$occurredAt)"
}

/**
 * A grant was withdrawn. `domainmgmt` and `execution` consume this
 * (`doc:10-architecture#bounded-contexts`); the actor's open runs close.
 */
public data class GrantRevoked(
    public val grantId: GrantId,
    public val actorId: ActorId,
    public val domainId: DomainId,
    override val occurredAt: Instant,
) : DomainEvent
