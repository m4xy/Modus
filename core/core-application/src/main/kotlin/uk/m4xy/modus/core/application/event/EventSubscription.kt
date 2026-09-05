package uk.m4xy.modus.core.application.event

import uk.m4xy.modus.core.domain.DomainEvent

/**
 * A [DomainEventHandler] bound to the events it accepts.
 *
 * The binding is a narrowing function rather than a class token, for two reasons. It keeps
 * the dispatcher free of reflection and of an unchecked cast — `KClass.safeCast` would put
 * `kotlin.reflect` on the path a domain event travels, for a decision the compiler can make.
 * And it lets a handler accept a family of events (`{ it as? AgentRunEvent }`) without the
 * dispatcher learning that families exist.
 *
 * Mismatching the two arguments is not representable: [accepts] produces an `E` and
 * [handler] consumes one, so a selector for `GrantIssued` paired with a handler for
 * `GrantRevoked` fails to unify.
 *
 * @param accepts returns the event narrowed to [E], or null when this subscription is not
 *   interested in it. The idiomatic form is `{ it as? GrantRevoked }`.
 */
public class EventSubscription<E : DomainEvent>(
    private val accepts: (DomainEvent) -> E?,
    private val handler: DomainEventHandler<E>,
) {
    /**
     * Delivers [event] to the handler if this subscription accepts it.
     *
     * Returns true when it was delivered — reported rather than inferred, so a test can
     * assert what the routing decided separately from what a handler concluded
     * (`doc:35-testing#load-bearing-evidence`: what a mechanism perceives is a separate
     * subject from what it decides).
     */
    public fun deliver(event: DomainEvent): Boolean {
        val accepted = accepts(event) ?: return false
        handler.handle(accepted)
        return true
    }
}
