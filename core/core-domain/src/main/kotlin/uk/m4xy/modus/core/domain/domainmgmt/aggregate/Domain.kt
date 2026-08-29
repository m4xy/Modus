package uk.m4xy.modus.core.domain.domainmgmt.aggregate

import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.event.DomainCreated
import uk.m4xy.modus.core.domain.domainmgmt.event.ProcessDefinitionChanged
import uk.m4xy.modus.core.domain.domainmgmt.published.DomainName
import uk.m4xy.modus.core.domain.domainmgmt.published.ProcessDefinition
import java.time.Instant

/**
 * A tenant. The thing every other bounded context is scoped by, and the thing `identity`'s
 * grants have been naming since `bean:0009` without anything creating one.
 *
 * Invariant: a domain always has a process. There is no state in which it exists and its
 * work cannot move, because [create] takes one and [adoptProcess] can only replace it —
 * which is what lets `work` treat `DomainCreated` as the complete answer to "how does this
 * domain's work behave" (`doc:00-constitution#domain-scoping`).
 *
 * Entity, not value: two instances carrying the same [id] are the same domain, and
 * [equals]/[hashCode] say so. Without it a `Set<Domain>` could hold a stale copy beside a
 * current one and both would answer — the defect `bean:0009` found in `PermissionGrant`,
 * not repeated here.
 */
public class Domain private constructor(
    public val id: DomainId,
    public val name: DomainName,
    // JustifiedVar: the process is the only state this root owns, and adoptProcess is its
    // only writer. It is a var rather than a new instance so a held reference cannot keep
    // answering with a process the domain has replaced.
    private var process: ProcessDefinition,
    private val events: MutableList<DomainEvent>,
) {
    /** Raised, not dispatched: the application layer drains these after the write. */
    public val pendingEvents: List<DomainEvent> get() = events.toList()

    /** The process this domain imposes right now. A value object, so handing it out is safe. */
    public val processDefinition: ProcessDefinition get() = process

    /**
     * Pre: none. Post: the domain imposes [definition], and [ProcessDefinitionChanged] is
     * pending **only if** that differs from what it imposed before.
     *
     * Adopting the process already in force is not an error and not a no-op worth refusing:
     * it is what a replayed command or an idempotent import does. It raises nothing, because
     * an event is a claim that something happened (`doc:20-ddd-practices#domain-events`).
     */
    public fun adoptProcess(
        definition: ProcessDefinition,
        at: Instant,
    ): Domain {
        if (definition == process) {
            return this
        }
        process = definition
        events += ProcessDefinitionChanged(id, definition, at)
        return this
    }

    /** Entity identity: the [id] alone, never the process hanging off it. */
    override fun equals(other: Any?): Boolean = this === other || (other is Domain && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    public companion object {
        /**
         * The only way a [Domain] comes into existence.
         *
         * Every creation invariant it could own already belongs to a value object —
         * `DomainId` is a slug, `DomainName` is renderable, `ProcessDefinition` is a usable
         * graph — so this factory adds none. That is the intended end state of
         * `doc:20-ddd-practices#value-objects`'s no-primitive-obsession rule, not an
         * omission: there is no argument here that could be wrong and still typecheck.
         */
        public fun create(
            id: DomainId,
            name: DomainName,
            process: ProcessDefinition,
            at: Instant,
        ): Domain = Domain(id, name, process, mutableListOf(DomainCreated(id, name, process, at)))
    }
}
