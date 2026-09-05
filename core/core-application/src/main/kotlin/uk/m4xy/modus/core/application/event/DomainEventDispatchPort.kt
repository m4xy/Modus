package uk.m4xy.modus.core.application.event

import uk.m4xy.modus.core.domain.DomainEvent

/**
 * Hands drained domain events over to whatever delivers them.
 *
 * Declared in `core-application` because a use case is its only caller
 * (`doc:20-ddd-practices#ports-and-adapters` §5.2 — a port is declared where it is
 * **used**). An aggregate that published its own events is the defect `bean:0066` exists
 * to prevent, so nothing in `core-domain` may reach this type, and
 * `rule:archunit/domainDependsOnNoOuterLayer` says so mechanically.
 *
 * The signature names only the shared kernel, which is what keeps the seam swappable: the
 * synchronous in-process [SynchronousDomainEventDispatch] satisfies it today, and a durable
 * or asynchronous implementation in an adapter satisfies it later without either core module
 * changing a line. Ordering is not this port's business — [WriteThenDispatch] owns it — and
 * neither is delivery semantics beyond what the one implementation documents.
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
