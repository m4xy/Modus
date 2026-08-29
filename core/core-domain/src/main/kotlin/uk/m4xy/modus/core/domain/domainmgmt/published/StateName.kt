package uk.m4xy.modus.core.domain.domainmgmt.published

/**
 * One state a work item may occupy, in one domain's process.
 *
 * A **name**, not an enum. `doc:00-constitution#domain-scoping` says every domain defines
 * its own work-item states and that code MUST NOT hardcode a single process, so membership
 * is per-domain data and the domain model owns only the shape.
 *
 * It is not `work`'s `WorkItemState` either, and cannot be: `domainmgmt` MUST NOT import
 * `work` in any form (`doc:10-architecture#bounded-contexts` §3.1). The dependency runs the
 * other way — `work` may import this context's published language and map its own states
 * onto these names.
 *
 * Invariant: lower kebab, 1..64 characters, each segment alphanumeric. The same reasoning
 * as every other identifier here — a state name is rendered in a URL query, a file name and
 * a log field, so it may not carry a separator, a space, or case that folds on a
 * case-insensitive volume.
 */
@JvmInline
public value class StateName(
    public val value: String,
) {
    init {
        require(SHAPE.matches(value)) { "stateName must be lower kebab, 1-64 characters: '$value'" }
    }

    private companion object {
        private val SHAPE = Regex("^[a-z0-9]{1,64}(-[a-z0-9]{1,64})*$")
    }
}
