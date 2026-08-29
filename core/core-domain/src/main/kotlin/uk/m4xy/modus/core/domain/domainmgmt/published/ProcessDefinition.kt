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
 * **Not a `data class`, and not by oversight** (`doc:20-ddd-practices#value-objects` §3.1).
 * A value object holding a collection cannot be one: the generated constructor binds the
 * caller's collection, Kotlin's `Set` is a read-only *view* rather than an immutable type,
 * and every invariant below would then be enforced exactly once — at construction, on a set
 * the caller still holds a mutable reference to. Review found all four collections
 * published that way, which is `bean:0009`'s privilege escalation in a different type.
 * Copies are taken on the way in and handed out on the way out, as
 * `PermissionGrant.issue` does.
 *
 * The invariants are what make a definition *usable*, not merely well-formed. A process is
 * a graph, and the ways a graph fails a work item are: it can never finish — no terminal
 * state, none reachable, or a state from which none is reachable — it finishes before it
 * starts ([initial] is terminal), or it names moves that go nowhere.
 */
public class ProcessDefinition private constructor(
    private val declaredStates: List<StateName>,
    public val initial: StateName,
    private val declaredTerminal: List<StateName>,
    private val declaredTransitions: List<StateTransition>,
) {
    /** A fresh copy every read: mutating it changes no decision this process makes. */
    public val states: Set<StateName> get() = declaredStates.toSet()

    /** A fresh copy every read, for the same reason as [states]. */
    public val terminal: Set<StateName> get() = declaredTerminal.toSet()

    /** A fresh copy every read, for the same reason as [states]. */
    public val transitions: Set<StateTransition> get() = declaredTransitions.toSet()

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
    ): Boolean = declaredTransitions.any { it.from == from && it.to == to }

    /** True for a state that ends the work. Nothing transitions out of one. */
    public fun isTerminal(state: StateName): Boolean = state in declaredTerminal

    /** Structural equality over the content, since this is a value object with no identity. */
    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is ProcessDefinition &&
                    initial == other.initial &&
                    declaredStates.toSet() == other.declaredStates.toSet() &&
                    declaredTerminal.toSet() == other.declaredTerminal.toSet() &&
                    declaredTransitions.toSet() == other.declaredTransitions.toSet()
            )

    override fun hashCode(): Int = listOf(declaredStates.toSet(), initial, declaredTerminal.toSet(), declaredTransitions.toSet()).hashCode()

    override fun toString(): String = "ProcessDefinition(states=$states, initial=$initial, terminal=$terminal, transitions=$transitions)"

    public companion object {
        /**
         * The only way a [ProcessDefinition] comes into existence. Every argument is copied
         * before it is validated, so what the invariants below are checked against is what
         * the instance will answer with — a caller mutating its own set afterwards changes
         * nothing here.
         */
        public fun of(
            states: Set<StateName>,
            initial: StateName,
            terminal: Set<StateName>,
            transitions: Set<StateTransition>,
        ): ProcessDefinition {
            val s = states.toList()
            val t = terminal.toList()
            val moves = transitions.toList()
            require(s.isNotEmpty()) { "processDefinition must declare at least one state" }
            require(initial in s) { "processDefinition initial state '${initial.value}' is not among its states" }
            require(t.isNotEmpty()) { "processDefinition must declare at least one terminal state" }
            require(t.all { it in s }) {
                "processDefinition terminal states must be among its states: ${names(t.toSet() - s.toSet())}"
            }
            require(initial !in t) {
                "processDefinition initial state '${initial.value}' is also terminal, " +
                    "so work would be created already finished"
            }
            require(moves.all { it.from in s && it.to in s }) {
                "processDefinition transitions must name declared states: " +
                    names(moves.flatMap { listOf(it.from, it.to) }.toSet() - s.toSet())
            }
            require(moves.none { it.from in t }) {
                "processDefinition must not permit a transition out of a terminal state: " +
                    names(moves.filter { it.from in t }.map { it.from }.toSet())
            }
            require(reachableFrom(initial, moves) == s.toSet()) {
                "processDefinition states are unreachable from '${initial.value}': " +
                    names(s.toSet() - reachableFrom(initial, moves))
            }
            require(canFinish(t.toSet(), moves).containsAll(s)) {
                "processDefinition states cannot reach a terminal state, so work entering one never finishes: " +
                    names(s.toSet() - canFinish(t.toSet(), moves))
            }
            return ProcessDefinition(s, initial, t, moves)
        }

        /** Breadth-first forward walk. A state nobody can reach is dead configuration. */
        private fun reachableFrom(
            initial: StateName,
            moves: List<StateTransition>,
        ): Set<StateName> = walk(setOf(initial)) { from -> moves.filter { it.from == from }.map { it.to } }

        /**
         * The states from which some terminal state is reachable — a backward walk from
         * [terminal] over reversed transitions.
         *
         * Forward reachability is not this property and does not imply it. Review supplied
         * two definitions that satisfy every check above and still trap work: a reachable
         * non-terminal state with no outgoing transition, and a cycle with no exit to a
         * terminal state. In both, an item moves legally and can never close, so
         * `doc:00-constitution#evidence-rule`'s close guard never runs — the same harm the
         * `initial !in terminal` check prevents at the other end.
         */
        private fun canFinish(
            terminal: Set<StateName>,
            moves: List<StateTransition>,
        ): Set<StateName> = walk(terminal) { to -> moves.filter { it.to == to }.map { it.from } }

        private fun walk(
            seeds: Set<StateName>,
            next: (StateName) -> List<StateName>,
        ): Set<StateName> {
            val seen = seeds.toMutableSet()
            val queue = ArrayDeque(seeds)
            while (queue.isNotEmpty()) {
                next(queue.removeFirst()).filter(seen::add).forEach(queue::addLast)
            }
            return seen
        }

        private fun names(states: Set<StateName>) = states.map(StateName::value).sorted().joinToString(", ")
    }
}
