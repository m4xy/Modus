package uk.m4xy.modus.core.domain.aggregate

import uk.m4xy.modus.core.domain.DomainEvent

/**
 * An aggregate root that accumulates [DomainEvent]s and can hand them over exactly once.
 *
 * **The contract every aggregate root in Modus adopts**, and the whole of what a new root
 * has to do to participate in dispatch: implement [drainEvents]. `Domain`, `Actor` and
 * `PermissionGrant` do today; `WorkItem`, `Memory`, `AgentRun` and `SpendLedger` will as
 * their contexts land (`bean:0013` to `bean:0016`). None of them restates the ordering or
 * the once-only guarantee — those live in `core-application`'s `WriteThenDispatch`, which
 * takes this type and can therefore reach nothing but the drain.
 *
 * Why a type and not a convention: without it, the caller that writes an aggregate and
 * then hands its events over has to be given a `(T) -> List<DomainEvent>` at every call
 * site, and passing `pendingEvents` there instead of a drain compiles and reintroduces
 * the exact defect `bean:0066` exists to fix. With it, the drain is the only thing the
 * caller can reach.
 *
 * It belongs to no bounded context, so it sits beside the shared kernel rather than in it
 * — `adr:0004-domain-id-shared-kernel#shared-kernel-membership` test 2 asks whether a type
 * appears in more than one context's *published language*, and this one appears in none:
 * an aggregate is internals (`doc:10-architecture#bounded-contexts` §3.1). The precedent
 * for the placement is `uk.m4xy.modus.core.domain.port`, which is a subpackage of the
 * kernel's package and not a member of the kernel for the same reason.
 *
 * It is deliberately **not** in `..domain.<ctx>.aggregate`: that package is scoped by
 * `rule:archunit/aggregatesAreSealedOrFinal`, which every aggregate root must satisfy and
 * an interface never can.
 */
public interface RaisesDomainEvents {
    /**
     * Every event raised since the last drain, oldest first, leaving this root with none.
     *
     * Pre: none. Post: the returned events are the caller's, and a second call with no
     * intervening command returns an empty list.
     *
     * The return is a **copy**, so mutating it puts nothing back into the root
     * (`bean:0036`). Draining is what makes an event a fact that has been handed over: a
     * read that only copies leaves the root able to re-publish everything it has ever
     * raised the next time it is written, which is the defect `bean:0066` was raised for.
     *
     * The domain never decides *who* receives them, and never decides *when*.
     * `doc:20-ddd-practices#domain-events` §4.1.4 puts dispatch in the application layer;
     * the ordering is `core-application`'s `WriteThenDispatch.write`, and this signature is
     * the whole of what the domain knows about either.
     *
     * **Implementing this interface does not make a drain correct.** A body that copies
     * without clearing satisfies the compiler, every Detekt and ArchUnit rule, and every
     * test in this repository, while reinstating the defect above. `bean:0133` carries the
     * gate that would catch it.
     */
    public fun drainEvents(): List<DomainEvent>
}
