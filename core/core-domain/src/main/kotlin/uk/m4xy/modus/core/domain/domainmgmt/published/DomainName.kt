package uk.m4xy.modus.core.domain.domainmgmt.published

/**
 * What a domain is called, for a human reading a screen.
 *
 * Distinct from `DomainId`, and deliberately unconstrained by comparison: the id is a slug
 * because it is a path segment and a directory name, the name is prose because it is
 * neither. Renaming a domain must never move it on disk or change a URL, which is only
 * true while the two are separate types.
 *
 * Invariant: 1..120 characters after trimming, and no control characters. The upper bound
 * is a rendering constraint rather than a storage one — a name that cannot fit a column
 * heading is a description. Control characters are excluded because this string reaches a
 * terminal, a log line and an HTML attribute unescaped by anything the domain controls.
 */
@JvmInline
public value class DomainName(
    public val value: String,
) {
    init {
        require(value == value.trim() && value.length in 1..MAX_LENGTH && value.none(Char::isISOControl)) {
            "domainName must be 1-$MAX_LENGTH trimmed characters with no control characters: '$value'"
        }
    }

    private companion object {
        /** In a companion, never at file scope: `doc:30-code-style#archunit-synthetic-classes`. */
        private const val MAX_LENGTH = 120
    }
}
