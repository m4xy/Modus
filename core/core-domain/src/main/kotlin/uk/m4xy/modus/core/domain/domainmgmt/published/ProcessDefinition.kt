package uk.m4xy.modus.core.domain.domainmgmt.published

/**
 * The process one domain imposes on its work: which states exist, where work starts, which
 * states end it, and which moves are legal.
 *
 * `doc:00-constitution#domain-scoping` requires this to be data rather than code — Modus
 * supplies defaults and a domain may override any of them, so nothing here is an enum and
 * nothing is hardcoded.
 *
 * Published language (`doc:10-architecture#bounded-contexts` §3.1): it appears in this
 * context's events, and `doc:20-ddd-practices#aggregates` §2.2's aggregate shape passes it
 * straight into `WorkItem.transitionTo`, so `work` needs the type rather than a copy of the
 * data.
 *
 * The invariants are what make a definition *usable*, not merely well-formed. A process is
 * a graph, and the three ways a graph fails a work item are: it can never finish (no
 * terminal state, or none reachable), it finishes before it starts ([initial] is terminal),
 * or it names moves that go nowhere.
 */
public data class ProcessDefinition(
    public val states: Set<StateName>,
    public val initial: StateName,
    public val terminal: Set<StateName>,
    public val transitions: Set<StateTransition>,
) {
    init {
        require(states.isNotEmpty()) { "processDefinition must declare at least one state" }
        require(initial in states) { "processDefinition initial state '${initial.value}' is not among its states" }
        require(terminal.isNotEmpty()) { "processDefinition must declare at least one terminal state" }
        require(terminal.all { it in states }) {
            "processDefinition terminal states must be among its states: ${unknown(terminal)}"
        }
        require(initial !in terminal) {
            "processDefinition initial state '${initial.value}' is also terminal, so work would be created already finished"
        }
        require(transitions.all { it.from in states && it.to in states }) {
            "processDefinition transitions must name declared states: ${unknownInTransitions()}"
        }
        require(transitions.none { it.from in terminal }) {
            "processDefinition must not permit a transition out of a terminal state: ${leavingTerminal()}"
        }
        require(reachable() == states) {
            "processDefinition states are unreachable from '${initial.value}': ${names(states - reachable())}"
        }
    }

    /**
     * True only for a move this process permits. Absence is a refusal, never an omission.
     *
     * It matches on the pair's parts rather than constructing a [StateTransition] to test
     * membership: `StateTransition` refuses a self-transition in its `init`, so building one
     * here would make `allows(x, x)` throw where it must answer `false`. A query never
     * throws because its argument is uninteresting.
     */
    public fun allows(
        from: StateName,
        to: StateName,
    ): Boolean = transitions.any { it.from == from && it.to == to }

    /** True for a state that ends the work. Nothing transitions out of one. */
    public fun isTerminal(state: StateName): Boolean = state in terminal

    /**
     * The states an item can actually arrive at, walked from [initial].
     *
     * Breadth-first over [transitions]. A state nobody can reach is dead configuration: it
     * will never hold a work item, and if it is terminal the work can never close.
     */
    private fun reachable(): Set<StateName> {
        val seen = mutableSetOf(initial)
        val queue = ArrayDeque(listOf(initial))
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            transitions.filter { it.from == next && seen.add(it.to) }.forEach { queue.addLast(it.to) }
        }
        return seen
    }

    private fun unknown(candidates: Set<StateName>) = names(candidates - states)

    private fun unknownInTransitions() = names(transitions.flatMap { listOf(it.from, it.to) }.toSet() - states)

    private fun leavingTerminal() = names(transitions.filter { it.from in terminal }.map { it.from }.toSet())

    private fun names(states: Set<StateName>) = states.map(StateName::value).sorted().joinToString(", ")
}
