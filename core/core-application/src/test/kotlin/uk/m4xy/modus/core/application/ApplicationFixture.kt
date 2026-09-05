package uk.m4xy.modus.core.application

import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.aggregate.Domain
import uk.m4xy.modus.core.domain.domainmgmt.published.DomainName
import uk.m4xy.modus.core.domain.domainmgmt.published.ProcessDefinition
import uk.m4xy.modus.core.domain.domainmgmt.published.StateName
import uk.m4xy.modus.core.domain.domainmgmt.published.StateTransition
import uk.m4xy.modus.core.domain.identity.aggregate.PermissionGrant
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.GrantId
import java.time.Instant

/**
 * Immutable, so no test can leak state into another. Time is a constant, never read.
 *
 * A second copy of `core-domain`'s `IdentityFixture` and `DomainMgmtFixture` rather than a
 * shared one: test source sets are not published between Gradle modules, and publishing
 * them would put a fixture on the unit-test classpath of every module that depends on
 * `core-domain` (`doc:35-testing#unit-classpath`).
 *
 * The grant carries **two** capabilities and the process **four** states, for the reason
 * `doc:35-testing#fixture-variation` gives: a fixture set in which every collection has one
 * element proves nothing about the two-element paths, and that uniformity is what hid
 * `bean:0009`'s privilege escalation.
 */
object ApplicationFixture {
    val AT: Instant = Instant.parse("2026-08-29T00:00:00Z")
    val LATER: Instant = Instant.parse("2026-08-30T00:00:00Z")

    val ALICE: ActorId = ActorId("alice")
    val BOB: ActorId = ActorId("bob")
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

    /** The smallest legal process, so an `adoptProcess` has something different to adopt. */
    val OTHER_PROCESS: ProcessDefinition =
        ProcessDefinition.of(
            states = setOf(TODO_STATE, DONE),
            initial = TODO_STATE,
            terminal = setOf(DONE),
            transitions = setOf(StateTransition(TODO_STATE, DONE)),
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
