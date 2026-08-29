package uk.m4xy.modus.core.domain.domainmgmt.published

/**
 * One move a work item may make, in one domain's process.
 *
 * A pair rather than two fields on [ProcessDefinition], so the set of legal moves is a set
 * — asking whether a move is permitted is a membership test, not a scan, and a duplicate
 * transition is impossible rather than merely ignored.
 *
 * A transition to the state it leaves is refused. A self-transition permits an operation
 * that changes nothing while raising an event saying something happened, which is a way for
 * an audit trail to fill with movement that never occurred.
 */
public data class StateTransition(
    public val from: StateName,
    public val to: StateName,
) {
    init {
        require(from != to) { "stateTransition must move: '${from.value}' to itself is not a transition" }
    }
}
