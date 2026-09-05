package uk.m4xy.modus.core.application.event

import uk.m4xy.modus.core.domain.aggregate.RaisesDomainEvents

/**
 * The one place the order of a write and its dispatch is decided: **write, drain, dispatch.**
 *
 * `doc:00-constitution` §2.4 makes every write atomic and
 * `doc:15-repository-layout#cross-cutting-flows` §6.1 puts the durable append before the
 * fan-out. Both are properties of a sequence, and a sequence spelled out at every call site
 * is one that will eventually be spelled the other way round. Spelling it once means a use
 * case cannot get it wrong and a new bounded context does not have to re-derive it. Every
 * other mention of the ordering in this repository cites [write] and states no rule of its
 * own (`doc:05-authoring-for-agents#one-fact-one-place`).
 *
 * The drain is the second half of the contract and is why [write] takes a
 * [RaisesDomainEvents] rather than a lambda: handing the caller a `(T) -> List<DomainEvent>`
 * would let `pendingEvents` be passed where a drain belongs, which compiles, dispatches
 * correctly the first time, and re-publishes everything on the second write — exactly the
 * defect `bean:0066` was raised for.
 *
 * **This is not a mechanism, and it does not make a drain correct.** Nothing forces a use
 * case through this class: `repository.save(root)` followed by
 * `dispatcher.dispatch(root.pendingEvents)` compiles and passes every gate in the build, and
 * so does a `drainEvents()` that copies without clearing. `bean:0133` carries all three
 * gaps.
 */
public class WriteThenDispatch(
    private val dispatcher: DomainEventDispatchPort,
) {
    /**
     * Writes [root] through [save], then hands its events over.
     *
     * Pre: [root] carries the events its command raised. Post: [save] has been called
     * exactly once, [root] carries no events, and everything it carried has been dispatched.
     *
     * If [save] throws, nothing is drained and nothing is dispatched: the events stay on the
     * aggregate and the caller sees the write's failure. That is the ordering guarantee, and
     * it is a guarantee about failure rather than about success — dispatching before the
     * write would announce a fact that is not durable, and no consumer can un-see one.
     *
     * What happens if a **handler** throws, after the write has succeeded, is a different
     * subject and belongs to the dispatcher: `doc:20-ddd-practices#domain-events` §4.1.8.
     *
     * @param save the repository write. A lambda rather than a repository port because every
     *   context declares its own (`PermissionGrantRepository.save`, `DomainRepository.save`,
     *   …) and no common supertype exists or should.
     */
    public fun <T : RaisesDomainEvents> write(
        root: T,
        save: (T) -> Unit,
    ) {
        save(root)
        dispatcher.dispatch(root.drainEvents())
    }
}
