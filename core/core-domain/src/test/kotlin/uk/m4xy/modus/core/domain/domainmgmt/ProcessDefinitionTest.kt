package uk.m4xy.modus.core.domain.domainmgmt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
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
            ProcessDefinition.of(states = emptySet(), initial = TODO, terminal = setOf(DONE), transitions = emptySet())
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

    /**
     * `bean:0009`'s escalation, in a different type. All four collections were `public val`
     * constructor parameters with nothing copied either way, so every invariant here was
     * enforced once — at construction, against a set the caller still held.
     *
     * Size two-or-more on purpose (`doc:35-testing#fixture-variation`): `setOf(x)` is
     * `Collections.singleton`, whose `add` throws, so this test written against
     * [MINIMAL_PROCESS] would pass while proving nothing. `PROCESS` backs its sets with
     * `LinkedHashSet`, where the cast succeeds and the mutation lands.
     */
    @Test
    fun `mutating a published collection changes no decision the process makes`() {
        val extra = StateName("extra")

        (PROCESS.states as MutableSet).add(extra)
        (PROCESS.terminal as MutableSet).add(DOING)
        (PROCESS.transitions as MutableSet).add(StateTransition(DONE, TODO))

        PROCESS.states shouldBe setOf(TODO, DOING, DONE, ABANDONED)
        PROCESS.isTerminal(DOING) shouldBe false
        PROCESS.allows(DONE, TODO) shouldBe false
    }

    /** The other half: the caller's own set, mutated after construction. */
    @Test
    fun `mutating the caller's collection after construction changes nothing`() {
        val transitions = mutableSetOf(StateTransition(TODO, DOING), StateTransition(DOING, DONE))
        val process = DomainMgmtFixture.process(transitions = transitions)

        transitions.add(StateTransition(TODO, DONE))

        process.allows(TODO, DONE) shouldBe false
        process.transitions.size shouldBe 2
    }

    /**
     * A reachable non-terminal state with no way out. Every state is reachable from
     * `initial`, so forward reachability accepts it; an item that moves to `blocked` can
     * never move again and can never close.
     */
    @Test
    fun `refuses a process with a non-terminal state that cannot reach a terminal state`() {
        val blocked = StateName("blocked")

        shouldThrow<IllegalArgumentException> {
            ProcessDefinition.of(
                states = setOf(TODO, blocked, DONE),
                initial = TODO,
                terminal = setOf(DONE),
                transitions = setOf(StateTransition(TODO, blocked), StateTransition(TODO, DONE)),
            )
        }.message shouldBe
            "processDefinition states cannot reach a terminal state, " +
            "so work entering one never finishes: blocked"
    }

    /**
     * A cycle with no exit. All four states are reachable, so the forward walk is satisfied;
     * from `a` the item can move forever and never finish. `doc:00-constitution#evidence-rule`'s
     * close guard would never run — the same harm the `initial !in terminal` check prevents at
     * the other end of the process.
     */
    @Test
    fun `refuses a process with a cycle that cannot escape to a terminal state`() {
        val a = StateName("a")
        val b = StateName("b")

        shouldThrow<IllegalArgumentException> {
            ProcessDefinition.of(
                states = setOf(TODO, a, b, DONE),
                initial = TODO,
                terminal = setOf(DONE),
                transitions =
                    setOf(
                        StateTransition(TODO, a),
                        StateTransition(a, b),
                        StateTransition(b, a),
                        StateTransition(TODO, DONE),
                    ),
            )
        }.message shouldBe
            "processDefinition states cannot reach a terminal state, " +
            "so work entering one never finishes: a, b"
    }

    /** A cycle is legal when it can still get out. */
    @Test
    fun `accepts a cycle that can reach a terminal state`() {
        val review = StateName("in-review")

        val process =
            ProcessDefinition.of(
                states = setOf(TODO, DOING, review, DONE),
                initial = TODO,
                terminal = setOf(DONE),
                transitions =
                    setOf(
                        StateTransition(TODO, DOING),
                        StateTransition(DOING, review),
                        StateTransition(review, DOING),
                        StateTransition(review, DONE),
                    ),
            )

        process.allows(review, DOING) shouldBe true
        process.allows(review, DONE) shouldBe true
    }

    /**
     * `ProcessDefinition` cannot be a `data class` — it holds collections and must copy them
     * (`doc:20-ddd-practices#value-objects` §3.1) — so structural equality is hand-written
     * and is pinned here. A value object with no identity is equal by content or it is not a
     * value object.
     */
    @Test
    fun `two processes are equal when their content is, whatever the instance`() {
        val rebuilt = ProcessDefinition.of(PROCESS.states, PROCESS.initial, PROCESS.terminal, PROCESS.transitions)

        rebuilt shouldBe PROCESS
        rebuilt.hashCode() shouldBe PROCESS.hashCode()
        setOf(PROCESS, rebuilt).size shouldBe 1
        PROCESS.equals(PROCESS) shouldBe true
    }

    /**
     * One variant per field `equals` compares. They are not minimal edits, because a process
     * is a graph and the parts are coupled — adding a state without a transition to it is
     * unreachable, and making a state terminal while something leaves it is refused. Each
     * variant is therefore the smallest **legal** difference in that field, and each is
     * chosen so the fields `equals` checks before it are equal.
     */
    @Test
    fun `processes with different content are not equal`() {
        val base = DomainMgmtFixture.process()

        // initial differs; checked first
        base shouldNotBe
            ProcessDefinition.of(setOf(DOING, DONE), DOING, setOf(DONE), setOf(StateTransition(DOING, DONE)))
        // same initial, states differ
        base shouldNotBe PROCESS
        // same initial and states, terminal differs
        base shouldNotBe
            ProcessDefinition.of(
                setOf(TODO, DOING, DONE),
                TODO,
                setOf(DOING, DONE),
                setOf(StateTransition(TODO, DOING), StateTransition(TODO, DONE)),
            )
        // same initial, states and terminal; transitions differ
        base shouldNotBe
            ProcessDefinition.of(
                setOf(TODO, DOING, DONE),
                TODO,
                setOf(DONE),
                setOf(StateTransition(TODO, DOING), StateTransition(DOING, DONE), StateTransition(TODO, DONE)),
            )
    }

    @Test
    fun `a process is never equal to something that is not one`() {
        PROCESS shouldNotBe TODO
        PROCESS shouldNotBe null
    }

    /** The rendering an agent reads in a failure message, so it names every part. */
    @Test
    fun `renders its states, initial state, terminal states and transitions`() {
        val rendered = MINIMAL_PROCESS.toString()

        rendered shouldContain "states="
        rendered shouldContain "initial=StateName(value=todo)"
        rendered shouldContain "terminal="
        rendered shouldContain "transitions="
    }

    @Test
    fun `distinguishes terminal states from every other state`() {
        PROCESS.isTerminal(DONE) shouldBe true
        PROCESS.isTerminal(ABANDONED) shouldBe true
        PROCESS.isTerminal(TODO) shouldBe false
        PROCESS.isTerminal(DOING) shouldBe false
    }
}
