package uk.m4xy.modus.core.domain.domainmgmt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.ABANDONED
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.DOING
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.DONE
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.MINIMAL_PROCESS
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.PROCESS
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.TODO
import uk.m4xy.modus.core.domain.domainmgmt.published.ProcessDefinition
import uk.m4xy.modus.core.domain.domainmgmt.published.StateName
import uk.m4xy.modus.core.domain.domainmgmt.published.StateTransition
import kotlin.test.Test

class ProcessDefinitionTest {
    @Test
    fun `accepts a process with a branch and more than one terminal state`() {
        PROCESS.states shouldBe setOf(TODO, DOING, DONE, ABANDONED)
        PROCESS.initial shouldBe TODO
        PROCESS.terminal shouldBe setOf(DONE, ABANDONED)
        PROCESS.transitions shouldBe
            setOf(
                StateTransition(TODO, DOING),
                StateTransition(TODO, ABANDONED),
                StateTransition(DOING, DONE),
                StateTransition(DOING, ABANDONED),
            )
    }

    @Test
    fun `accepts the smallest legal process — one move, one terminal state`() {
        MINIMAL_PROCESS.allows(TODO, DONE) shouldBe true
        MINIMAL_PROCESS.isTerminal(DONE) shouldBe true
    }

    @Test
    fun `refuses a process with no states at all`() {
        shouldThrow<IllegalArgumentException> {
            ProcessDefinition(states = emptySet(), initial = TODO, terminal = setOf(DONE), transitions = emptySet())
        }.message shouldBe "processDefinition must declare at least one state"
    }

    @Test
    fun `refuses a process whose initial state is not among its states`() {
        shouldThrow<IllegalArgumentException> {
            DomainMgmtFixture.process(states = setOf(DOING, DONE), initial = TODO)
        }.message shouldBe "processDefinition initial state 'todo' is not among its states"
    }

    @Test
    fun `refuses a process with no terminal state, because its work could never finish`() {
        shouldThrow<IllegalArgumentException> {
            DomainMgmtFixture.process(terminal = emptySet())
        }.message shouldBe "processDefinition must declare at least one terminal state"
    }

    @Test
    fun `refuses a process whose terminal states are not among its states`() {
        shouldThrow<IllegalArgumentException> {
            DomainMgmtFixture.process(terminal = setOf(DONE, ABANDONED))
        }.message shouldBe "processDefinition terminal states must be among its states: abandoned"
    }

    /**
     * The degenerate process: work is created in a state that already ends it. Every work
     * item would be born closed, and `doc:00-constitution#evidence-rule`'s close guard would
     * never run.
     */
    @Test
    fun `refuses a process whose initial state is also terminal`() {
        shouldThrow<IllegalArgumentException> {
            DomainMgmtFixture.process(terminal = setOf(TODO, DONE))
        }.message shouldBe
            "processDefinition initial state 'todo' is also terminal, so work would be created already finished"
    }

    @Test
    fun `refuses a transition whose target the process never declared`() {
        shouldThrow<IllegalArgumentException> {
            DomainMgmtFixture.process(
                transitions = setOf(StateTransition(TODO, DOING), StateTransition(DOING, ABANDONED)),
            )
        }.message shouldBe "processDefinition transitions must name declared states: abandoned"
    }

    /** The other half of the same guard: an undeclared state on the *leaving* side. */
    @Test
    fun `refuses a transition whose source the process never declared`() {
        shouldThrow<IllegalArgumentException> {
            DomainMgmtFixture.process(
                transitions = setOf(StateTransition(TODO, DOING), StateTransition(ABANDONED, DONE)),
            )
        }.message shouldBe "processDefinition transitions must name declared states: abandoned"
    }

    /**
     * A terminal state that can be left is not terminal. Permitting it would let a closed
     * work item reopen through the definition rather than through a decision.
     */
    @Test
    fun `refuses a transition out of a terminal state`() {
        shouldThrow<IllegalArgumentException> {
            DomainMgmtFixture.process(
                transitions =
                    setOf(StateTransition(TODO, DOING), StateTransition(DOING, DONE), StateTransition(DONE, TODO)),
            )
        }.message shouldBe "processDefinition must not permit a transition out of a terminal state: done"
    }

    /**
     * Dead configuration: the state exists, nothing reaches it. If it is the only terminal
     * state the work can never close, and the definition would still be well-formed by every
     * other check here.
     */
    @Test
    fun `refuses a process with a state unreachable from its initial state`() {
        shouldThrow<IllegalArgumentException> {
            DomainMgmtFixture.process(
                states = setOf(TODO, DOING, DONE, ABANDONED),
                terminal = setOf(DONE, ABANDONED),
            )
        }.message shouldBe "processDefinition states are unreachable from 'todo': abandoned"
    }

    @Test
    fun `reports every unreachable state, not the first one it finds`() {
        val orphan = StateName("blocked")
        shouldThrow<IllegalArgumentException> {
            DomainMgmtFixture.process(
                states = setOf(TODO, DOING, DONE, ABANDONED, orphan),
                terminal = setOf(DONE, ABANDONED),
            )
        }.message shouldBe "processDefinition states are unreachable from 'todo': abandoned, blocked"
    }

    @Test
    fun `permits exactly the moves it declares`() {
        PROCESS.allows(TODO, DOING) shouldBe true
        PROCESS.allows(TODO, ABANDONED) shouldBe true
        PROCESS.allows(DOING, DONE) shouldBe true
        PROCESS.allows(TODO, DONE) shouldBe false
        PROCESS.allows(DONE, TODO) shouldBe false
    }

    /** A move to the state you are already in is refused by the type, and by `allows`. */
    @Test
    fun `refuses a self-transition, in the pair and in the query`() {
        PROCESS.allows(DOING, DOING) shouldBe false
        shouldThrow<IllegalArgumentException> { StateTransition(DOING, DOING) }
            .message shouldBe "stateTransition must move: 'doing' to itself is not a transition"
    }

    @Test
    fun `distinguishes terminal states from every other state`() {
        PROCESS.isTerminal(DONE) shouldBe true
        PROCESS.isTerminal(ABANDONED) shouldBe true
        PROCESS.isTerminal(TODO) shouldBe false
        PROCESS.isTerminal(DOING) shouldBe false
    }
}
