package uk.m4xy.modus.core.application.event

import uk.m4xy.modus.core.domain.DomainEvent

/**
 * One bounded context's reaction to one kind of event raised by another.
 *
 * A handler is a use case (`doc:00-constitution` §1.1), which is why this type is in
 * `core-application` and not in `core-domain`: a domain that named its own handlers would
 * depend outwards.
 *
 * **A handler may import the publishing context's `..domain.event..` and `..domain.published..`
 * packages and nothing else of it** — no aggregate, no port, no use case
 * (`doc:10-architecture#bounded-contexts` §3.1). The dispatcher must not become the hole
 * through which one context reaches another's internals. That rule is a review obligation
 * today; `bean:0023` is the ArchUnit rule that will make it mechanical.
 *
 * Deliberately **not** [uk.m4xy.modus.core.application.UseCase]`<E, Unit>`, whose shape is
 * identical. The `E : DomainEvent` bound is the point: it makes registering a handler for
 * something that is not an event a compile error rather than a runtime surprise, and a
 * command is not an event.
 */
public fun interface DomainEventHandler<in E : DomainEvent> {
    /**
     * Reacts to [event].
     *
     * Called synchronously, after the publishing aggregate's write is durable. A handler
     * that throws is not swallowed; what happens to the events behind it is
     * `doc:20-ddd-practices#domain-events` §4.1.8, which every implementation of
     * [DomainEventDispatchPort] is held to.
     *
     * **Write a handler to be idempotent.** `doc:20-ddd-practices#domain-events` §4.1.7 asks
     * for it because it expects a durable event log that can be replayed. That log does not
     * exist and nothing replays anything (`bean:0132`), so idempotency buys nothing today —
     * it is what makes the handler survive the day the log arrives, and the day a
     * re-delivery becomes possible. Nothing enforces it.
     */
    public fun handle(event: E)
}
