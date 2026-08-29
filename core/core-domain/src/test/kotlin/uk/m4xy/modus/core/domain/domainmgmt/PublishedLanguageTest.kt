package uk.m4xy.modus.core.domain.domainmgmt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.domainmgmt.published.DomainName
import uk.m4xy.modus.core.domain.domainmgmt.published.StateName
import kotlin.test.Test

class PublishedLanguageTest {
    @Test
    fun `accepts a domain name a human would write`() {
        DomainName("Modus Core").value shouldBe "Modus Core"
        DomainName("R&D \u2014 Skunkworks (EU)").value shouldBe "R&D \u2014 Skunkworks (EU)"
        DomainName("x").value shouldBe "x"
        DomainName("n".repeat(120)).value shouldBe "n".repeat(120)
    }

    @Test
    fun `refuses a blank domain name`() {
        shouldThrow<IllegalArgumentException> { DomainName("") }.message shouldBe domainNameMessage("")
    }

    /**
     * Untrimmed rather than merely ugly: two names differing only in surrounding whitespace
     * render identically and compare unequal, so a list would show what looks like one
     * domain twice.
     */
    @Test
    fun `refuses a domain name with leading or trailing whitespace`() {
        listOf(" Modus", "Modus ", "\tModus", "Modus\n").forEach { value ->
            shouldThrow<IllegalArgumentException> { DomainName(value) }.message shouldBe domainNameMessage(value)
        }
    }

    /**
     * This string reaches a terminal, a log line and an HTML attribute without the domain
     * controlling the escaping, so a control character is an injection primitive rather than
     * a formatting nuisance. Each of these survives trimming, so the length and trim checks
     * would pass it.
     */
    @Test
    fun `refuses a domain name carrying control characters`() {
        listOf("Mo\tdus", "Mo\u0000dus", "Modus\u001B[31m", "Mo\u007Fdus", "Mo\u000Bdus").forEach { value ->
            shouldThrow<IllegalArgumentException> { DomainName(value) }.message shouldBe domainNameMessage(value)
        }
    }

    @Test
    fun `refuses a domain name longer than a column heading`() {
        val tooLong = "n".repeat(121)

        shouldThrow<IllegalArgumentException> { DomainName(tooLong) }.message shouldBe domainNameMessage(tooLong)
    }

    @Test
    fun `accepts a state name that is lower kebab`() {
        StateName("todo").value shouldBe "todo"
        StateName("in-review").value shouldBe "in-review"
        StateName("blocked-on-evidence").value shouldBe "blocked-on-evidence"
        StateName("s2").value shouldBe "s2"
    }

    /**
     * A state name is rendered in a URL query, a file name and a log field, so it may not
     * carry a separator, a space, or case that folds on a case-insensitive volume — the same
     * reasoning `DomainId` records.
     */
    @Test
    fun `refuses a state name that could not survive a URL, a file name or a log field`() {
        listOf("In Review", "InReview", "in_review", "in/review", "in.review", "-todo", "todo-", "in--review", "")
            .forEach { value ->
                shouldThrow<IllegalArgumentException> { StateName(value) }.message shouldBe stateNameMessage(value)
            }
    }

    private fun domainNameMessage(value: String) = "domainName must be 1-120 trimmed characters with no control characters: '$value'"

    private fun stateNameMessage(value: String) = "stateName must be lower kebab, 1-64 characters: '$value'"
}
