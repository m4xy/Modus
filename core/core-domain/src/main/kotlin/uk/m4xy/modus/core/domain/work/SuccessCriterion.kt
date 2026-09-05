package uk.m4xy.modus.core.domain.work

import uk.m4xy.modus.core.domain.work.published.SuccessCriterionId

/**
 * One binary, checkable thing a work item must be true of before it can close.
 *
 * Internal to this context, not published: it appears in no event
 * (`doc:10-architecture#bounded-contexts` §3.1 makes an event's signature the thing that
 * publishes a type). Its [id] **is** published, because a refused close names the criteria
 * that carry no evidence and the caller has to be able to say which.
 *
 * A criterion is the unit the evidence guard counts over. That is the whole reason it is a
 * type rather than a line of prose in the item's body: `doc:00-constitution#evidence-rule`
 * refuses a close without an evidence record **per success criterion**, and a rule counted
 * per criterion needs criteria that can be counted.
 */
public data class SuccessCriterion(
    public val id: SuccessCriterionId,
    public val statement: CriterionStatement,
)

/**
 * What the criterion says, in words a reader can check the work against.
 *
 * Invariant: 1..500 characters after trimming, no control characters. `doc:80-agent-operating-procedure`
 * step 2 requires a criterion to be *binary and checkable*; that is a property of the
 * sentence, which no type can enforce, so the type enforces what it can — that the sentence
 * exists, fits a table cell, and survives a terminal and an HTML attribute unescaped.
 *
 * Newlines are control characters and are therefore refused. A criterion that needs one is
 * two criteria, and splitting it is what makes each half countable by the evidence guard.
 */
@JvmInline
public value class CriterionStatement(
    public val value: String,
) {
    init {
        require(value == value.trim() && value.length in 1..MAX_LENGTH && value.none(Char::isISOControl)) {
            "criterionStatement must be 1-$MAX_LENGTH trimmed characters with no control characters: '$value'"
        }
    }

    private companion object {
        /** In a companion, never at file scope: `doc:30-code-style#archunit-synthetic-classes`. */
        private const val MAX_LENGTH = 500
    }
}
