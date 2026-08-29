package uk.m4xy.modus.core.domain.identity.aggregate

import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.identity.event.ActorRegistered
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.ActorKind
import java.time.Instant

/**
 * A principal. Stable identity, independent of any domain.
 *
 * Invariant: an actor holds no authority of its own. Everything it may do is a
 * [PermissionGrant] inside one domain, so registering an actor can never widen anyone's
 * access — which is what makes the bootstrap path of
 * `doc:10-architecture#domain-root-convention` §5.5 safe to run at first start.
 */
public class Actor private constructor(
    public val id: ActorId,
    public val kind: ActorKind,
    private val events: MutableList<DomainEvent>,
) {
    /** Raised, not dispatched: the application layer drains these after the write. */
    public val pendingEvents: List<DomainEvent> get() = events.toList()

    public companion object {
        /** The only way an [Actor] comes into existence. */
        public fun register(
            id: ActorId,
            kind: ActorKind,
            at: Instant,
        ): Actor = Actor(id, kind, mutableListOf(ActorRegistered(id, kind, at)))
    }
}
