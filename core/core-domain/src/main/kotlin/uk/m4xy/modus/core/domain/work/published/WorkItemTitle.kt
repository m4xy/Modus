package uk.m4xy.modus.core.domain.work.published

/**
 * What a work item or an epic is called, for a human reading a screen or a backlog.
 *
 * Distinct from [WorkItemId], and deliberately unconstrained by comparison, for the reason
 * `DomainName` gives: the id is a slug because it is a path segment and a file name, the
 * title is prose because it is neither. Retitling a work item must never move it on disk
 * or change a URL, which is only true while the two are separate types.
 *
 * Invariant: 1..200 characters after trimming, and no control characters. The upper bound
 * is a rendering constraint — a title that cannot fit a backlog row is a description, and
 * the description is the body of the file. Control characters are excluded because this
 * string reaches a terminal, a log line and an HTML attribute unescaped by anything the
 * domain controls.
 */
@JvmInline
public value class WorkItemTitle(
    public val value: String,
) {
    init {
        require(value == value.trim() && value.length in 1..MAX_LENGTH && value.none(Char::isISOControl)) {
            "workItemTitle must be 1-$MAX_LENGTH trimmed characters with no control characters: '$value'"
        }
    }

    private companion object {
        /** In a companion, never at file scope: `doc:30-code-style#archunit-synthetic-classes`. */
        private const val MAX_LENGTH = 200
    }
}
