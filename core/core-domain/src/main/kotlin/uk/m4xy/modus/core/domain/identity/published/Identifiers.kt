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
 * the file system accepts). Lower case is part of the rule for the same reason [DomainId]
 * requires it — see the note there.
 */
private val OPAQUE_ID = Regex("^[a-z0-9]([a-z0-9._-]{0,62}[a-z0-9])?$")

/** Lower kebab, 3..64 characters, no leading or trailing hyphen. */
private val SLUG = Regex("^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$")

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

/**
 * The tenant identifier that scopes every other bounded context.
 *
 * Invariant: a slug, because it is the first path segment after `/domains`
 * (`doc:10-architecture#domain-root-convention`) and a directory name in the store.
 *
 * Lower case is load-bearing, not cosmetic. The store is a flat file tree, and on a
 * case-insensitive volume — APFS on the development platform, NTFS in CI containers —
 * `Modus-Core/` and `modus-core/` are one directory. Two ids unequal in the domain would
 * be one tenant on disk, and the 404-not-403 rule cannot help when both ids resolve.
 */
@JvmInline
public value class DomainId(
    public val value: String,
) {
    init {
        require(SLUG.matches(value)) { "domainId must be a slug: '$value'" }
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
