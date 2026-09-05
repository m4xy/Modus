package uk.m4xy.modus.core.application.event

import uk.m4xy.modus.core.domain.DomainEvent

/**
 * Delivers each event to every subscription that accepts it, in order, on the calling thread.
 *
 * The walking skeleton's whole delivery mechanism. It is deliberately not durable, not
 * replayable and not asynchronous: choosing an event log or a broker before there is a
 * consumer is a decision without evidence (`bean:0066`, "Not owned").
 *
 * **On a handler that throws: the exception propagates, and delivery stops there.** Nothing
 * is caught, nothing is logged and nothing is swallowed. Two consequences a caller must
 * plan for, stated rather than discovered:
 *
 * 1. The write has already happened — [WriteThenDispatch] guarantees that ordering — so the
 *    aggregate is durable and its events have already left it. The failure surfaces to the
 *    use case's caller with the write standing.
 * 2. Subscriptions after the failing one, and events after the failing one, are not
 *    delivered. Delivery is all-or-a-prefix, never all-or-nothing.
 *
 * That is a choice, not an accident. Running every handler and reporting the failures
 * afterwards requires catching `Exception` or `RuntimeException` around a call that may throw
 * anything, and `config/detekt/detekt.yml` activates `TooGenericExceptionCaught` everywhere
 * outside the two process-supervising adapters. Given "propagate" and "swallow" as the
 * reachable options, criterion 7 of `bean:0066` picks propagate. Delivery that survives one
 * handler's failure is a property of a durable dispatcher with a retry, which is the same
 * work item as replay and is out of scope here.
 *
 * This lives in `core-application` rather than in an adapter, which is a departure from
 * `doc:00-constitution` §1.2's "the adapter implements it" and is flagged as such. The
 * reason: it names no technology — it is a loop over a list of application-layer handlers —
 * and moving it outward would put the routing of application-layer types outside the
 * application layer while everything it routes to stays inside. The port above is what keeps
 * the seam honest: the day dispatch has to be durable or asynchronous, an adapter implements
 * [DomainEventDispatchPort] and this class is deleted or kept for tests, and neither core
 * module changes.
 */
public class SynchronousDomainEventDispatch(
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
