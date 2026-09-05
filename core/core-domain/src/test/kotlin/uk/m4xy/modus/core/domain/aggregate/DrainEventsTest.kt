package uk.m4xy.modus.core.domain.aggregate

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.AT
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.LATER
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.MINIMAL_PROCESS
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.MODUS
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.MODUS_NAME
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.PROCESS
import uk.m4xy.modus.core.domain.domainmgmt.aggregate.Domain
import uk.m4xy.modus.core.domain.domainmgmt.event.DomainCreated
import uk.m4xy.modus.core.domain.domainmgmt.event.ProcessDefinitionChanged
import uk.m4xy.modus.core.domain.identity.IdentityFixture
import uk.m4xy.modus.core.domain.identity.aggregate.Actor
import uk.m4xy.modus.core.domain.identity.event.ActorRegistered
import uk.m4xy.modus.core.domain.identity.event.GrantIssued
import uk.m4xy.modus.core.domain.identity.event.GrantRevoked
import uk.m4xy.modus.core.domain.identity.published.ActorKind
import kotlin.test.Test

/**
 * The drain contract of [RaisesDomainEvents], asserted once against every root that
 * implements it (`bean:0066`).
 *
 * It is one file rather than three additions to three aggregate tests because the contract
 * is one thing: a fourth root — `bean:0013`'s `WorkItem` — is adopted by adding a case here,
 * and a root that implements the interface without appearing in this file is visible as an
 * absence.
 *
 * Collection sizes follow `doc:35-testing#fixture-variation`. `Domain` and `PermissionGrant`
 * carry two-or-more; `Actor` carries exactly one, because `register` is its only factory and
 * it has no command that raises a second — the size-one shape is a fact about that root, not
 * a uniform fixture. Every root is asserted at size zero after a drain.
 */
class DrainEventsTest {
    // --- criterion 1: the drain hands over and empties -----------------------------------

    @Test
    fun `Domain hands over everything it raised and keeps none of it`() {
        val domain = Domain.create(MODUS, MODUS_NAME, PROCESS, AT).adoptProcess(MINIMAL_PROCESS, LATER)

        val drained = domain.drainEvents()

        drained.size shouldBe 2
        drained[0].shouldBeInstanceOf<DomainCreated>()
        drained[1].shouldBeInstanceOf<ProcessDefinitionChanged>()
        domain.pendingEvents shouldBe emptyList()
    }

    @Test
    fun `PermissionGrant hands over everything it raised and keeps none of it`() {
        val grant = IdentityFixture.grant("g1").revoke(LATER)

        val drained = grant.drainEvents()

        drained.size shouldBe 2
        drained[0].shouldBeInstanceOf<GrantIssued>()
        drained[1].shouldBeInstanceOf<GrantRevoked>()
        grant.pendingEvents shouldBe emptyList()
    }

    @Test
    fun `Actor hands over the one event it raises and keeps none of it`() {
        val actor = Actor.register(IdentityFixture.ALICE, ActorKind.AGENT, AT)

        val drained = actor.drainEvents()

        drained.size shouldBe 1
        drained.single().shouldBeInstanceOf<ActorRegistered>()
        actor.pendingEvents shouldBe emptyList()
    }

    // --- criterion 1: draining twice yields the second call an empty list ----------------
    //
    // This is the whole defect bean:0066 names. Before the drain existed, a second write of
    // the same instance handed the same events over again: a redelivered ProcessDefinitionChanged
    // moves a domain's work back onto a process it has left, and a redelivered GrantRevoked
    // closes an actor's runs a second time.

    @Test
    fun `a second drain of Domain yields nothing`() {
        val domain = Domain.create(MODUS, MODUS_NAME, PROCESS, AT).adoptProcess(MINIMAL_PROCESS, LATER)
        domain.drainEvents()

        domain.drainEvents() shouldBe emptyList()
    }

    @Test
    fun `a second drain of PermissionGrant yields nothing`() {
        val grant = IdentityFixture.grant("g1").revoke(LATER)
        grant.drainEvents()

        grant.drainEvents() shouldBe emptyList()
    }

    @Test
    fun `a second drain of Actor yields nothing`() {
        val actor = Actor.register(IdentityFixture.ALICE, ActorKind.HUMAN, AT)
        actor.drainEvents()

        actor.drainEvents() shouldBe emptyList()
    }

    @Test
    fun `a command raised after a drain hands over only what it raised`() {
        val domain = Domain.create(MODUS, MODUS_NAME, PROCESS, AT)
        domain.drainEvents()

        domain.adoptProcess(MINIMAL_PROCESS, LATER)

        domain.drainEvents().single().shouldBeInstanceOf<ProcessDefinitionChanged>()
    }

    // --- criterion 2: the drain returns a copy -------------------------------------------
    //
    // Asserted at size two, per doc:35-testing#fixture-variation: kotlin's toList() returns
    // an immutable singleton at size one, so the down-cast an exploit needs throws there and
    // the assertion would pass on a broken implementation. That is exactly how bean:0009's
    // privilege escalation reached main.

    @Test
    fun `the list Domain hands over cannot be mutated back into it`() {
        val domain = Domain.create(MODUS, MODUS_NAME, PROCESS, AT).adoptProcess(MINIMAL_PROCESS, LATER)

        val drained = domain.drainEvents()
        drained.size shouldBe 2
        (drained as MutableList).add(drained[0])

        drained.size shouldBe 3
        domain.pendingEvents shouldBe emptyList()
        domain.drainEvents() shouldBe emptyList()
    }

    @Test
    fun `the list PermissionGrant hands over cannot be mutated back into it`() {
        val grant = IdentityFixture.grant("g1").revoke(LATER)

        val drained = grant.drainEvents()
        drained.size shouldBe 2
        val stolen = drained[0]
        (drained as MutableList).add(stolen)

        drained.size shouldBe 3
        grant.pendingEvents shouldBe emptyList()
        grant.drainEvents() shouldBe emptyList()
    }

    @Test
    fun `pendingEvents still reads without draining, and the drain still finds them`() {
        val grant = IdentityFixture.grant("g1").revoke(LATER)

        grant.pendingEvents.size shouldBe 2
        grant.pendingEvents.size shouldBe 2

        grant.drainEvents().size shouldBe 2
    }
}
