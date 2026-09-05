package uk.m4xy.modus.adapter.events.inprocess

import uk.m4xy.modus.core.application.event.DomainEventDispatchPort
import uk.m4xy.modus.core.application.event.EventSubscription
import uk.m4xy.modus.core.domain.DomainEvent

/**
 * Delivers each event to every subscription that accepts it, in order, on the calling thread.
 *
 * The walking skeleton's whole delivery mechanism, and an **incomplete** implementation of
 * the port it satisfies. `doc:20-ddd-practices#domain-events` §4.1.7 requires that every
 * event be appended to the durable event log before any handler runs; there is no durable
 * event log, and nothing in this repository appends a domain event anywhere. `bean:0160`
 * carries it. So this class is not a different concern from the durable dispatcher that
 * replaces it — it is the same concern with the durable half missing, which is exactly why
 * it is an adapter rather than an application-layer collaborator: a conforming
 * implementation must touch durable storage, and storage is an adapter's business
 * (`doc:00-constitution` §1.2).
 *
 * It is deliberately not replayable, not asynchronous and not cross-process either:
 * choosing an event log or a broker before there is a consumer is a decision without
 * evidence (`bean:0066`, "Not owned").
 *
 * **On a handler that throws: the exception propagates, and delivery stops there.** Nothing
 * is caught, nothing is logged and nothing is swallowed. The contract is stated once, in
 * `doc:20-ddd-practices#domain-events` §4.1.8, and this class implements it rather than
 * restating it. Three consequences a caller must plan for:
 *
 * 1. The write has already happened — `WriteThenDispatch` guarantees that ordering — so the
 *    aggregate is durable and its events have already left it. The failure surfaces to the
 *    use case's caller with the write standing.
 * 2. Subscriptions after the failing one, and events after the failing one, are not
 *    delivered. Delivery is all-or-a-prefix, never all-or-nothing.
 * 3. **The undelivered suffix is lost, permanently.** Nothing recorded that those events
 *    were due, so nothing can replay them — a direct consequence of §4.1.7's missing log,
 *    not of the fail-fast choice. Running every handler and reporting the failures
 *    afterwards would discard them just as permanently; only the log fixes it.
 *
 * Fail-fast rather than collect-and-report is also the option the toolchain leaves
 * reachable: running every handler around a call that may throw anything needs a broad
 * `catch`, and `config/detekt/detekt.yml` activates `TooGenericExceptionCaught` everywhere
 * outside the two process-supervising adapters. Given "propagate" and "swallow", criterion
 * 7 of `bean:0066` picks propagate.
 */
public class InProcessDomainEventDispatch(
    subscriptions: List<EventSubscription<*>>,
) : DomainEventDispatchPort {
    // Copied on the way in, so a caller that keeps its list cannot register a handler after
    // wiring is done. Registration is a wiring act (doc:00-constitution §1.2), and a
    // dispatcher whose subscriber set can change under it has no stable delivery contract.
    private val registered: List<EventSubscription<*>> = subscriptions.toList()

    /**
     * How many subscriptions this dispatcher will consult.
     *
     * A count rather than the list: the list is what it must not hand out. It exists so the
     * copy above is observable — a caller that adds to its own list after construction can
     * be asserted not to have changed this.
     */
    public val subscriptionCount: Int get() = registered.size

    override fun dispatch(events: List<DomainEvent>) {
        for (event in events) {
            for (subscription in registered) {
                subscription.deliver(event)
            }
        }
    }
}
