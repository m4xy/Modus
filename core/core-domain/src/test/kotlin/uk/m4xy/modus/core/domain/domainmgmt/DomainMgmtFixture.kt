package uk.m4xy.modus.core.domain.domainmgmt

import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.published.DomainName
import uk.m4xy.modus.core.domain.domainmgmt.published.ProcessDefinition
import uk.m4xy.modus.core.domain.domainmgmt.published.StateName
import uk.m4xy.modus.core.domain.domainmgmt.published.StateTransition
import java.time.Instant

/**
 * Immutable, so no test can leak state into another. Time is a constant, never read.
 *
 * Per `doc:35-testing#fixture-variation`, the default process is deliberately **not** the
 * smallest legal one. It carries four states, two terminal states and a branch, so every
 * collection here is at size 2-or-more and the paths a one-element fixture cannot reach —
 * a second terminal state, a state with two outgoing transitions, a diamond in the
 * reachability walk — are exercised by anything that does not name its own process. The
 * one-element and empty shapes are named explicitly by the tests that are about them.
 *
 * `bean:0009` shipped a privilege escalation that 32 tests and 30 verified mutations
 * missed, because every fixture in it carried exactly one capability. This is that lesson.
 */
object DomainMgmtFixture {
    val AT: Instant = Instant.parse("2026-08-29T00:00:00Z")
    val LATER: Instant = Instant.parse("2026-08-30T00:00:00Z")
    val MODUS: DomainId = DomainId("modus-core")
    val SKUNKWORKS: DomainId = DomainId("skunkworks")
    val MODUS_NAME: DomainName = DomainName("Modus Core")

    val TODO: StateName = StateName("todo")
    val DOING: StateName = StateName("doing")
    val DONE: StateName = StateName("done")
    val ABANDONED: StateName = StateName("abandoned")

    /** todo -> doing -> {done, abandoned}, and todo -> abandoned. Two terminals, one branch. */
    val PROCESS: ProcessDefinition =
        ProcessDefinition.of(
            states = setOf(TODO, DOING, DONE, ABANDONED),
            initial = TODO,
            terminal = setOf(DONE, ABANDONED),
            transitions =
                setOf(
                    StateTransition(TODO, DOING),
                    StateTransition(TODO, ABANDONED),
                    StateTransition(DOING, DONE),
                    StateTransition(DOING, ABANDONED),
                ),
        )

    /** The smallest legal process: one move, one terminal state. Size-one collections. */
    val MINIMAL_PROCESS: ProcessDefinition =
        ProcessDefinition.of(
            states = setOf(TODO, DONE),
            initial = TODO,
            terminal = setOf(DONE),
            transitions = setOf(StateTransition(TODO, DONE)),
        )

    fun process(
        states: Set<StateName> = setOf(TODO, DOING, DONE),
        initial: StateName = TODO,
        terminal: Set<StateName> = setOf(DONE),
        transitions: Set<StateTransition> = setOf(StateTransition(TODO, DOING), StateTransition(DOING, DONE)),
    ): ProcessDefinition = ProcessDefinition.of(states, initial, terminal, transitions)
}
