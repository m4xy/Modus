package uk.m4xy.modus.core.domain.work

import uk.m4xy.modus.core.domain.work.published.SuccessCriterionId
import java.time.Instant

/**
 * The attachment that turns an assertion about one success criterion into a fact
 * (`doc:00-constitution#evidence-rule`).
 *
 * Internal to this context. It is **not** `memory`'s `EvidenceRecord` and cannot be:
 * `doc:10-architecture#bounded-contexts` §3.1 forbids `work` from importing `memory` in any
 * form, in either direction. The two are different artefacts with different lifetimes — a
 * memory outlives the work item that produced it — and `work` needs only enough to answer
 * one question: does this criterion have something attached, and what does that something
 * point at.
 *
 * The payload is deliberately absent. This record holds a [reference], not a command's
 * output tail: an aggregate that carried megabyte-scale transcripts would violate
 * `doc:20-ddd-practices#aggregates` §2.1.6, for the reason `AgentRun` does not carry its
 * own output either.
 */
public data class EvidenceRecord(
    public val criterionId: SuccessCriterionId,
    public val kind: EvidenceKind,
    public val reference: EvidenceReference,
    public val recordedAt: Instant,
)

/**
 * What sort of evidence this is — a test run, a citation, a fetched URL, a diff.
 *
 * A **name**, not an enum, for the reason [uk.m4xy.modus.core.domain.work.published.WorkItemState]
 * is one: `doc:00-constitution#domain-scoping` lists *required evidence kinds* beside
 * work-item states and the definition of done as things every domain defines for itself.
 * An enum here would hardcode one domain's vocabulary into every domain's model.
 *
 * Which kinds a domain **requires** is not modelled yet, and that is a deferral rather than
 * an omission — `bean:0030` deferred the same thing for the same reason. The closed set it
 * would override is `doc:50-memory-and-evidence#evidence-kinds`, owned by `memory`, which
 * `bean:0015` builds; modelling an override of an unbuilt closed set means inventing its
 * vocabulary twice and reconciling later.
 *
 * Invariant: lower kebab, 1..64 characters — it is rendered in a file name and a log field.
 */
@JvmInline
public value class EvidenceKind(
    public val value: String,
) {
    init {
        require(value.length <= MAX_LENGTH && SHAPE.matches(value)) {
            "evidenceKind must be lower kebab, 1-$MAX_LENGTH characters: '$value'"
        }
    }

    private companion object {
        /** The length is checked separately: the segment quantifier bounds each run between hyphens, not the string. */
        private const val MAX_LENGTH = 64
        private val SHAPE = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    }
}

/**
 * Where the evidence is — a `file:line` citation, a command and its exit code, a fetched
 * URL, a commit sha.
 *
 * Invariant: 1..500 characters after trimming, no control characters. The bound is what
 * separates a reference from a payload: `doc:00-constitution#evidence-rule` requires the
 * command, its exit code and its output tail to be *recorded*, and the place it is recorded
 * is the work item's file and a memory — not this aggregate (`doc:20-ddd-practices#aggregates`
 * §2.1.6). Control characters are refused for the same reason as everywhere else in this
 * context: the string reaches a terminal and an HTML attribute unescaped.
 *
 * It is not parsed. What a reference means is the business of whoever follows it, and a
 * domain that understood `file:line` syntax would be a domain that knows about files.
 */
@JvmInline
public value class EvidenceReference(
    public val value: String,
) {
    init {
        require(value == value.trim() && value.length in 1..MAX_LENGTH && value.none(Char::isISOControl)) {
            "evidenceReference must be 1-$MAX_LENGTH trimmed characters with no control characters: '$value'"
        }
    }

    private companion object {
        /** In a companion, never at file scope: `doc:30-code-style#archunit-synthetic-classes`. */
        private const val MAX_LENGTH = 500
    }
}
