package uk.m4xy.modus.core.domain.identity.port

import uk.m4xy.modus.core.domain.identity.aggregate.Actor
import uk.m4xy.modus.core.domain.identity.published.ActorId

/**
 * Collection-oriented access to [Actor]s. Declared here, implemented by an adapter
 * (`doc:20-ddd-practices#ports-and-adapters`).
 */
public interface ActorRepository {
    /** Null means no such actor. It never means "could not read": that is thrown. */
    public fun findById(id: ActorId): Actor?

    public fun save(actor: Actor)
}
