package uk.m4xy.modus.core.domain.work.published

/**
 * The state one work item occupies, in one domain's process.
 *
 * A **name**, not an enum, and this is the criterion of `bean:0013` most likely to be
 * quietly violated: hardcoding a status enum is the obvious implementation and
 * `doc:00-constitution#domain-scoping` forbids it — every domain defines its own
 * work-item states, and code MUST NOT hardcode a single process. Nothing in this context
 * knows that `todo`, `doing` or `done` exist. The set of legal values is whatever the
 * domain's `ProcessDefinition` declares, and this type only constrains the shape a value
 * may take.
 *
 * ## Why this is a second type beside `domainmgmt`'s `StateName`
 *
 * It has to be. `WorkItemState` appears in this context's events, which publishes it
 * (`doc:10-architecture#bounded-contexts` §3.1), and a published package is a **leaf**: it
 * may reference only the Kotlin stdlib, `java.time`, its own context's published language
 * and the shared kernel. So `work.event.WorkItemTransitioned` cannot name
 * `domainmgmt.published.StateName`, and `WorkItemState` cannot wrap one.
 * `StateName`'s own KDoc states the same conclusion from the other side, and
 * `doc:20-ddd-practices#value-objects` §3.2 lists `WorkItemState` as a required published
 * value object of this context.
 *
 * The mapping runs in `WorkItem`'s internals, where importing `domainmgmt`'s published
 * language is permitted, and it is total **only while this invariant accepts no value
 * `StateName` rejects**. That is a constant which must match an authority
 * (`bean:0090`): widen it and `WorkItem.transitionTo` starts throwing
 * `IllegalArgumentException` from inside a guard. `WorkItemStateMatchesStateNameTest`
 * drives one corpus through both types and asserts they agree, verdict by verdict, rather
 * than comparing the two regexes — two patterns that look alike are not evidence that they
 * decide alike.
 *
 * Invariant: lower kebab, 1..64 characters, each segment alphanumeric — a state name is
 * rendered in a URL query, a file name and a log field.
 */
@JvmInline
public value class WorkItemState(
    public val value: String,
) {
    init {
        require(value.length <= MAX_LENGTH && SHAPE.matches(value)) {
            "workItemState must be lower kebab, 1-$MAX_LENGTH characters: '$value'"
        }
    }

    private companion object {
        /**
         * The length is checked separately because the regex cannot carry it: the segment
         * quantifier bounds each run between hyphens, not the whole string. `StateName`
         * records the same defect, found in its own review; this type inherits the fix
         * rather than the bug, because the two must decide alike.
         */
        private const val MAX_LENGTH = 64
        private val SHAPE = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    }
}
