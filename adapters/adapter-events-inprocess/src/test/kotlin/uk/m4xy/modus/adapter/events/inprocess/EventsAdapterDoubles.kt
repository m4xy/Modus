package uk.m4xy.modus.adapter.events.inprocess

import uk.m4xy.modus.core.application.event.DomainEventDispatchPort
import uk.m4xy.modus.core.application.event.DomainEventHandler
import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.aggregate.Domain
import uk.m4xy.modus.core.domain.domainmgmt.port.DomainRepository
import uk.m4xy.modus.core.domain.domainmgmt.published.DomainName
import uk.m4xy.modus.core.domain.domainmgmt.published.ProcessDefinition
import uk.m4xy.modus.core.domain.domainmgmt.published.StateName
import uk.m4xy.modus.core.domain.domainmgmt.published.StateTransition
import uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant
import uk.m4xy.modus.core.domain.identity.port.PermissionGrantRepository
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.GrantId
import uk.m4xy.modus.core.domain.port.ClockPort
import java.time.Instant

// Hand-written doubles and constants for this adapter's tests.
//
// A near-copy of core-application's ApplicationDoubles and ApplicationFixture, and
// deliberately a copy: a test source set is not published between Gradle modules, and
// publishing one would put a fixture on the unit-test classpath of every module that
// depends on core-application (doc:35-testing#unit-classpath). The alternative — a
// java-test-fixtures variant — is a build-logic change, and modus.kotlin-base is the one
// home for test dependencies (doc:35-testing#source-sets). bean:0134 carries the choice.
//
// Every one records what it was GIVEN separately from what it RETURNS, the separation
// core-domain's AmbientCapabilityDoubles established and bean:0066 criterion 8 turns on.

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

/** Records every call to [dispatch] without delivering anything. */
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

/** A clock stopped at [at], counting how often it was read. */
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

/** Thrown by [InMemoryPermissionGrantRepository] when it is set to fail its write. */
class WriteFailed(
    message: String,
) : IllegalStateException(message)

/** Grants held in a list, with a write that can be made to fail on demand. */
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

/** Domains held in a list, recording every id it was asked about. */
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

/**
 * Immutable, so no test can leak state into another. Time is a constant, never read.
 *
 * The grant carries **two** capabilities and the process **four** states, for the reason
 * `doc:35-testing#fixture-variation` gives: a fixture set in which every collection has one
 * element proves nothing about the two-element paths, and that uniformity is what hid
 * `bean:0009`'s privilege escalation.
 */
object EventsFixture {
    val AT: Instant = Instant.parse("2026-08-29T00:00:00Z")
    val LATER: Instant = Instant.parse("2026-08-30T00:00:00Z")

    val ALICE: ActorId = ActorId("alice")
    val MODUS: DomainId = DomainId("modus-core")
    val SKUNKWORKS: DomainId = DomainId("skunkworks")
    val AGENTS_READ: Capability = Capability("agents.read")
    val COST_READ: Capability = Capability("cost.read")

    private val TODO_STATE: StateName = StateName("todo")
    private val DOING: StateName = StateName("doing")
    private val DONE: StateName = StateName("done")
    private val ABANDONED: StateName = StateName("abandoned")

    /** todo -> doing -> {done, abandoned}, and todo -> abandoned. Two terminals, one branch. */
    val PROCESS: ProcessDefinition =
        ProcessDefinition.of(
            states = setOf(TODO_STATE, DOING, DONE, ABANDONED),
            initial = TODO_STATE,
            terminal = setOf(DONE, ABANDONED),
            transitions =
                setOf(
                    StateTransition(TODO_STATE, DOING),
                    StateTransition(TODO_STATE, ABANDONED),
                    StateTransition(DOING, DONE),
                    StateTransition(DOING, ABANDONED),
                ),
        )

    fun grant(
        id: String = "g1",
        actor: ActorId = ALICE,
        domain: DomainId = MODUS,
        capabilities: Set<Capability> = setOf(AGENTS_READ, COST_READ),
    ): PermissionGrant = PermissionGrant.issue(GrantId(id), actor, domain, capabilities, AT)

    fun domain(
        id: DomainId = MODUS,
        name: String = "Modus Core",
    ): Domain = Domain.create(id, DomainName(name), PROCESS, AT)
}
