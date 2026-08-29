package uk.m4xy.modus.core.domain.identity.published

/**
 * One thing a grant permits, inside the single domain that grant covers.
 *
 * The vocabulary is `<resource>.<action>`: `work.read`, `work.write`,
 * `repositories.read`, `agents.read`, `agents.run`, `memories.read`, `cost.read`,
 * `skills.read`, `settings.read`, `settings.write` are what the backoffice renders today
 * (`backoffice/src/api/types.ts`). The set is open rather than an enum because a Modus
 * Module declares the capabilities it defines (`doc:10-architecture#module-system`); the
 * domain owns the shape, never the membership.
 *
 * Invariant: exactly one `.`, both halves lower-kebab — each half a run of `[a-z0-9]`
 * groups joined by single hyphens, so neither half opens or closes on a hyphen. A
 * capability can therefore never be mistaken for a wildcard, a path, or a role name, and
 * [resource] and [action] always split it the way the vocabulary reads: a second `.` would
 * silently push `read.all` into [action].
 */
@JvmInline
public value class Capability(
    public val value: String,
) {
    init {
        require(SHAPE.matches(value)) { "capability must be '<resource>.<action>': '$value'" }
    }

    /** The resource kind it speaks about, for example `agents`. */
    public val resource: String get() = value.substringBefore('.')

    /** The operation it permits on that resource kind, for example `run`. */
    public val action: String get() = value.substringAfter('.')

    private companion object {
        private val SHAPE = Regex("^[a-z][a-z0-9]*(-[a-z0-9]+)*\\.[a-z][a-z0-9]*(-[a-z0-9]+)*$")
    }
}
