package uk.m4xy.modus.core.domain.identity.event

import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.ActorKind
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.DomainId
import uk.m4xy.modus.core.domain.identity.published.GrantId
import java.time.Instant

/** A principal now exists. It can reach nothing until a grant says otherwise. */
public data class ActorRegistered(
    public val actorId: ActorId,
    public val kind: ActorKind,
    override val occurredAt: Instant,
) : DomainEvent

/** An actor was given access to one domain, carrying exactly [capabilities]. */
public data class GrantIssued(
    public val grantId: GrantId,
    public val actorId: ActorId,
    public val domainId: DomainId,
    public val capabilities: Set<Capability>,
    override val occurredAt: Instant,
) : DomainEvent

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
