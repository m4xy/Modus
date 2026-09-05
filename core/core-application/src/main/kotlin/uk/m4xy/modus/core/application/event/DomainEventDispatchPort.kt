package uk.m4xy.modus.core.application.event

import uk.m4xy.modus.core.domain.DomainEvent

/**
 * Hands drained domain events over to whatever delivers them.
 *
 * Declared in `core-application` because a use case is its only caller
 * (`doc:20-ddd-practices#ports-and-adapters` §5.2 — a port is declared where it is
 * **used**). It stays here and does not migrate outward with its implementation:
 * `doc:10-architecture#module-system` §7.2 puts ports inside, which is what makes "an
 * adapter port" a thing that does not exist. An aggregate that published its own events is
 * the defect `bean:0066` exists to prevent, so nothing in `core-domain` may reach this type
 * either, and `rule:archunit/domainDependsOnNoOuterLayer` says so mechanically.
 *
 * The signature names only the shared kernel, which is what keeps the seam swappable.
 * `adapter-events-inprocess` satisfies it today, synchronously and without the durable log
 * `doc:20-ddd-practices#domain-events` §4.1.7 requires (`bean:0160`); a conforming durable
 * implementation replaces it without either core module changing a line.
 *
 * Ordering is not this port's business — [WriteThenDispatch] owns it. Delivery semantics on
 * handler failure are not an implementation's private business either: they are stated once
 * in `doc:20-ddd-practices#domain-events` §4.1.8.
 */
public fun interface DomainEventDispatchPort {
    /**
     * Delivers [events] in the order given.
     *
     * Called **after** the write that produced them is durable
     * (`doc:00-constitution` §2.4, `doc:15-repository-layout#cross-cutting-flows` §6.1).
     * An implementation may not assume it is called at most once per aggregate, and may not
     * assume [events] is non-empty: a command that changed nothing drains nothing.
     */
    public fun dispatch(events: List<DomainEvent>)
}
