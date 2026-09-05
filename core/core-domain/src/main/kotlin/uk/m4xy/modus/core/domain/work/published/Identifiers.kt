package uk.m4xy.modus.core.domain.work.published

/**
 * The shape every identifier in this context shares: 1..64 characters of `a-z`, `0-9`,
 * `.`, `_` and `-`, never opening or closing on punctuation.
 *
 * Identical to `identity`'s, and deliberately a second declaration rather than an import.
 * `rule:archunit/publishedLanguageIsLeaf` permits a published package to reference only the
 * Kotlin stdlib, `java.time`, **its own** context's published language and the shared
 * kernel, so a shared regex would have to become a third shared-kernel member — an ADR
 * (`adr:0004-domain-id-shared-kernel#shared-kernel-membership`) for a constant that decides
 * nothing. The reasoning behind the shape is stated once, at
 * [uk.m4xy.modus.core.domain.identity.published.ActorId], and is not restated here: an
 * identifier is authorised to become a path segment, a log field and a file name.
 *
 * It matches what `IdGeneratorPort.newId()` promises to return, so wrapping a freshly
 * generated id never throws. That is a constant which must match an authority, and
 * `WorkPublishedLanguageTest` asserts the correspondence rather than assuming it.
 */
private val OPAQUE_ID = Regex("^[a-z0-9]([a-z0-9._-]{0,62}[a-z0-9])?$")

/**
 * One unit of work: the thing Modus tracks, attributes runs to, and refuses to close
 * without evidence.
 *
 * Invariant: [OPAQUE_ID]. An adapter may use it unencoded as a file name — `.beans/` does
 * exactly that (`doc:00-constitution#workflow` §7.2) — so it survives a URL, a log line and
 * a directory entry without a second encoding.
 */
@JvmInline
public value class WorkItemId(
    public val value: String,
) {
    init {
        require(OPAQUE_ID.matches(value)) {
            "workItemId must be 1-64 characters of a-z, 0-9, '.', '_' or '-', " +
                "starting and ending alphanumeric: '$value'"
        }
    }
}

/**
 * Identifies one epic: a work item's parent, referenced by id and never by object reference
 * (`doc:20-ddd-practices#aggregates` §2.1.3). Same invariant as [WorkItemId].
 */
@JvmInline
public value class EpicId(
    public val value: String,
) {
    init {
        require(OPAQUE_ID.matches(value)) {
            "epicId must be 1-64 characters of a-z, 0-9, '.', '_' or '-', " +
                "starting and ending alphanumeric: '$value'"
        }
    }
}

/**
 * Identifies one success criterion within one work item.
 *
 * Published rather than internal because a refused close names the criteria that carry no
 * evidence, and a caller outside this context has to be able to say which ones. Same
 * invariant as [WorkItemId].
 */
@JvmInline
public value class SuccessCriterionId(
    public val value: String,
) {
    init {
        require(OPAQUE_ID.matches(value)) {
            "successCriterionId must be 1-64 characters of a-z, 0-9, '.', '_' or '-', " +
                "starting and ending alphanumeric: '$value'"
        }
    }
}
