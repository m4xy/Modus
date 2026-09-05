package uk.m4xy.modus.core.application

import uk.m4xy.modus.core.application.event.DomainEventDispatchPort
import uk.m4xy.modus.core.application.event.DomainEventHandler
import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.aggregate.Domain
import uk.m4xy.modus.core.domain.domainmgmt.port.DomainRepository
import uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant
import uk.m4xy.modus.core.domain.identity.port.PermissionGrantRepository
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.GrantId
import uk.m4xy.modus.core.domain.port.ClockPort
import java.time.Instant

// Hand-written doubles for the ports a use case talks to.
//
// doc:15-repository-layout §8 puts use-case tests here, "domain plus in-memory fakes of
// ports, no Spring context", and forbids a mocking framework in core/ outright.
//
// Every one records what it was GIVEN separately from what it RETURNS — the separation
// core-domain's AmbientCapabilityDoubles already established, and the one criterion 8 of
// bean:0066 turns on: a test that can only see a handler's verdict cannot tell a working
// drain-and-route from a fixture that handed a well-formed event straight to the handler.

/**
 * A clock stopped at [at], counting how often it was read.
 *
 * The count is the input-surface half: a use case that stamped an event from a second read
 * would be indistinguishable, from the event alone, from one that read the clock once.
 */
class FixedClock(
    private val at: Instant,
) : ClockPort {
    private var reads = 0

    /** How many times [now] has been called. */
    val readCount: Int get() = reads

    override fun now(): Instant {
        reads += 1
        return at
    }
}

/**
 * Records every call to [dispatch] without delivering anything.
 *
 * [calls] is one entry per call, so a test can tell "dispatched twice, one event each" from
 * "dispatched once with two events" — the difference between a drain per write and a drain
 * that accumulated. [dispatched] is the flattening, for assertions that do not care.
 */
class RecordingDispatch : DomainEventDispatchPort {
    private val received = mutableListOf<List<DomainEvent>>()

    /** One entry per [dispatch] call, in order. A copy. */
    val calls: List<List<DomainEvent>> get() = received.toList()

    /** Every event handed over, across every call, oldest first. A copy. */
    val dispatched: List<DomainEvent> get() = received.flatten()

    override fun dispatch(events: List<DomainEvent>) {
        received += events.toList()
    }
}

/** Records the events it was handed and does nothing else. */
class RecordingHandler<E : DomainEvent> : DomainEventHandler<E> {
    private val received = mutableListOf<E>()

    /** Every event this handler was given, oldest first. A copy. */
    val handled: List<E> get() = received.toList()

    override fun handle(event: E) {
        received += event
    }
}

/** Thrown by [ThrowingHandler]. Named, so an assertion cannot pass on an unrelated failure. */
class HandlerRefused(
    message: String,
) : IllegalStateException(message)

/**
 * Records the event it was handed, then throws [HandlerRefused].
 *
 * It records **before** throwing on purpose: "the handler ran and then failed" and "the
 * handler was never reached" are different facts, and a double that only threw could not
 * tell them apart.
 */
class ThrowingHandler<E : DomainEvent>(
    private val message: String = "handler refused",
) : DomainEventHandler<E> {
    private val received = mutableListOf<E>()

    /** Every event this handler was given before it threw. A copy. */
    val handled: List<E> get() = received.toList()

    override fun handle(event: E) {
        received += event
        throw HandlerRefused(message)
    }
}

/** Thrown by [InMemoryPermissionGrantRepository] when it is set to fail its write. */
class WriteFailed(
    message: String,
) : IllegalStateException(message)

/**
 * Grants held in a map, with a write that can be made to fail on demand.
 *
 * A fake that throws is how `bean:0066` criterion 6 observes a failed write. It observes it
 * better than a real store would: making a flat-file write fail requires arranging a
 * filesystem condition, and a unit test may not touch a filesystem at all
 * (`doc:35-testing#purity-rules`).
 */
class InMemoryPermissionGrantRepository(
    grants: List<PermissionGrant> = emptyList(),
    private val failWriteWith: String? = null,
) : PermissionGrantRepository {
    private val held = grants.toMutableList()
    private val writes = mutableListOf<GrantId>()

    /** Every grant id this store was asked to write, in order, failed writes included. A copy. */
    val saved: List<GrantId> get() = writes.toList()

    /** What the store holds now. A copy. */
    val contents: List<PermissionGrant> get() = held.toList()

    override fun grantsFor(
        actorId: ActorId,
        domainId: DomainId,
    ): List<PermissionGrant> = held.filter { it.actorId == actorId && it.domainId == domainId }

    override fun grantsFor(actorId: ActorId): List<PermissionGrant> = held.filter { it.actorId == actorId }

    override fun grantsOn(domainId: DomainId): List<PermissionGrant> = held.filter { it.domainId == domainId }

    override fun save(grant: PermissionGrant) {
        writes += grant.id
        if (failWriteWith != null) {
            throw WriteFailed(failWriteWith)
        }
        held.removeAll { it.id == grant.id }
        held += grant
    }
}

/** Domains held in a map, recording every id it was asked about. */
class InMemoryDomainRepository(
    domains: List<Domain> = emptyList(),
) : DomainRepository {
    private val held = domains.toMutableList()
    private val lookedUp = mutableListOf<DomainId>()

    /** Every id this store was asked for, in order, misses included. A copy. */
    val lookups: List<DomainId> get() = lookedUp.toList()

    override fun findById(id: DomainId): Domain? {
        lookedUp += id
        return held.firstOrNull { it.id == id }
    }

    override fun findAllById(ids: Set<DomainId>): List<Domain> {
        lookedUp += ids
        return held.filter { it.id in ids }
    }

    override fun save(domain: Domain) {
        held.removeAll { it.id == domain.id }
        held += domain
    }
}
