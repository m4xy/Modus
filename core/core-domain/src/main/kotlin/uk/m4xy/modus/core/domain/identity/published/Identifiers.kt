package uk.m4xy.modus.core.domain.identity.published

/**
 * The shape every identifier in this context shares: 1..64 characters of `a-z`, `0-9`,
 * `.`, `_` and `-`, never opening or closing on punctuation.
 *
 * It is deliberately narrower than "non-blank and whitespace-free". An identifier here is
 * authorised to become a path segment, a log field and a file name
 * (`doc:10-architecture#domain-root-convention`), so the shape has to exclude what those
 * three cannot survive: `/` and `..` (traversal), NUL and the zero-width characters
 * (invisible aliases of a different principal), and unbounded length (a name longer than
 * the file system accepts). Lower case is part of the rule for the same reason
 * [uk.m4xy.modus.core.domain.DomainId] requires it — see the note there.
 */
private val OPAQUE_ID = Regex("^[a-z0-9]([a-z0-9._-]{0,62}[a-z0-9])?$")

/**
 * A principal — human or agent — stable across every domain it can reach.
 *
 * Invariant: [OPAQUE_ID]. It is the subject of every audit line, every spend entry and
 * every grant, and an adapter may use it unencoded as a path segment or a file name, so
 * it survives a URL, a log line or a file name without a second encoding — and it cannot
 * name anything but itself.
 */
@JvmInline
public value class ActorId(
    public val value: String,
) {
    init {
        require(OPAQUE_ID.matches(value)) {
            "actorId must be 1-64 characters of a-z, 0-9, '.', '_' or '-', " +
                "starting and ending alphanumeric: '$value'"
        }
    }
}

/** Identifies one grant of an actor's access within one domain. Same invariant as [ActorId]. */
@JvmInline
public value class GrantId(
    public val value: String,
) {
    init {
        require(OPAQUE_ID.matches(value)) {
            "grantId must be 1-64 characters of a-z, 0-9, '.', '_' or '-', " +
                "starting and ending alphanumeric: '$value'"
        }
    }
}
