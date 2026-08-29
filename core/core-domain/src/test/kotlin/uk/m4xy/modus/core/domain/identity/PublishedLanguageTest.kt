package uk.m4xy.modus.core.domain.identity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.ActorKind
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.DomainId
import uk.m4xy.modus.core.domain.identity.published.GrantId
import kotlin.test.Test

class PublishedLanguageTest {
    @Test
    fun `accepts an actor id that is non-blank and whitespace-free`() {
        ActorId("agent-supervisor").value shouldBe "agent-supervisor"
    }

    @Test
    fun `refuses a blank actor id`() {
        shouldThrow<IllegalArgumentException> { ActorId("") }
            .message shouldBe "actorId must be non-blank and whitespace-free: ''"
    }

    @Test
    fun `refuses an actor id containing whitespace`() {
        shouldThrow<IllegalArgumentException> { ActorId("max holman") }
            .message shouldBe "actorId must be non-blank and whitespace-free: 'max holman'"
    }

    @Test
    fun `accepts a domain id that is a slug`() {
        DomainId("modus-core").value shouldBe "modus-core"
    }

    @Test
    fun `refuses a domain id that is not a slug`() {
        shouldThrow<IllegalArgumentException> { DomainId("Modus Core") }
            .message shouldBe "domainId must be a slug: 'Modus Core'"
    }

    @Test
    fun `accepts a grant id that is non-blank and whitespace-free`() {
        GrantId("grant-1").value shouldBe "grant-1"
    }

    @Test
    fun `refuses a grant id containing whitespace`() {
        shouldThrow<IllegalArgumentException> { GrantId("grant 1") }
            .message shouldBe "grantId must be non-blank and whitespace-free: 'grant 1'"
    }

    @Test
    fun `refuses a blank grant id`() {
        shouldThrow<IllegalArgumentException> { GrantId("") }
            .message shouldBe "grantId must be non-blank and whitespace-free: ''"
    }

    @Test
    fun `accepts every capability the backoffice renders today`() {
        val rendered =
            listOf(
                "work.read",
                "work.write",
                "repositories.read",
                "agents.read",
                "agents.run",
                "memories.read",
                "cost.read",
                "skills.read",
                "settings.read",
                "settings.write",
            )

        rendered.map { Capability(it).value } shouldBe rendered
    }

    @Test
    fun `splits a capability into its resource and its action`() {
        val capability = Capability("agents.run")

        capability.resource shouldBe "agents"
        capability.action shouldBe "run"
    }

    @Test
    fun `refuses a capability that is not resource dot action`() {
        shouldThrow<IllegalArgumentException> { Capability("agents") }
            .message shouldBe "capability must be '<resource>.<action>': 'agents'"
    }

    @Test
    fun `refuses a capability that tries to be a wildcard`() {
        shouldThrow<IllegalArgumentException> { Capability("agents.*") }
            .message shouldBe "capability must be '<resource>.<action>': 'agents.*'"
    }

    @Test
    fun `distinguishes the two kinds of principal`() {
        ActorKind.entries shouldBe listOf(ActorKind.HUMAN, ActorKind.AGENT)
        ActorKind.valueOf("AGENT") shouldBe ActorKind.AGENT
    }
}
