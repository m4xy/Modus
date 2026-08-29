package uk.m4xy.modus.core.domain.identity.published

/**
 * A principal — human or agent — stable across every domain it can reach.
 *
 * Invariant: opaque, non-blank and whitespace-free, so it survives a URL, a log line or a
 * file name without a second encoding.
 */
@JvmInline
public value class ActorId(
    public val value: String,
) {
    init {
        require(value.isNotBlank() && value.none { it.isWhitespace() }) {
            "actorId must be non-blank and whitespace-free: '$value'"
        }
    }
}

/**
 * The tenant identifier that scopes every other bounded context.
 *
 * Invariant: a slug, because it is the first path segment after `/domains`
 * (`doc:10-architecture#domain-root-convention`) and a directory name in the store.
 */
@JvmInline
public value class DomainId(
    public val value: String,
) {
    init {
        require(SLUG.matches(value)) { "domainId must be a slug: '$value'" }
    }

    private companion object {
        private val SLUG = Regex("^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$")
    }
}

/** Identifies one grant of an actor's access within one domain. Same invariant as [ActorId]. */
@JvmInline
public value class GrantId(
    public val value: String,
) {
    init {
        require(value.isNotBlank() && value.none { it.isWhitespace() }) {
            "grantId must be non-blank and whitespace-free: '$value'"
        }
    }
}
