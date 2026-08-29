package uk.m4xy.modus.core.domain.identity

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.ActorKind
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.GrantId
import kotlin.test.Test

class PublishedLanguageTest {
    @Test
    fun `accepts an actor id that is an opaque lower-case token`() {
        ActorId("agent-supervisor").value shouldBe "agent-supervisor"
        ActorId("a").value shouldBe "a"
        ActorId("agent.supervisor_2").value shouldBe "agent.supervisor_2"
        ActorId("a".repeat(64)).value shouldBe "a".repeat(64)
    }

    @Test
    fun `refuses a blank actor id`() {
        shouldThrow<IllegalArgumentException> { ActorId("") }.message shouldBe actorIdMessage("")
    }

    @Test
    fun `refuses an actor id containing whitespace`() {
        shouldThrow<IllegalArgumentException> { ActorId("max holman") }.message shouldBe actorIdMessage("max holman")
    }

    /**
     * The KDoc authorises an adapter to use an [ActorId] unencoded as a path segment or a
     * file name, so each of these is a real primitive and not a style preference: `../`
     * traverses, NUL and the zero-width space are invisible aliases of another principal,
     * upper case collides with lower case on a case-insensitive volume, and an unbounded
     * id is a name the file system will refuse or silently truncate.
     */
    @Test
    fun `refuses an actor id that could not survive being a path segment or a file name`() {
        val refused =
            listOf(
                "../../etc/passwd",
                "alice/../bob",
                "alice/bob",
                "alice\u0000",
                "alice\u200B",
                "Alice",
                "-alice",
                "alice-",
                "a".repeat(65),
            )

        refused.forEach { value ->
            shouldThrow<IllegalArgumentException> { ActorId(value) }.message shouldBe actorIdMessage(value)
        }
    }

    @Test
    fun `accepts a grant id that is an opaque lower-case token`() {
        GrantId("grant-1").value shouldBe "grant-1"
    }

    @Test
    fun `refuses a grant id containing whitespace`() {
        shouldThrow<IllegalArgumentException> { GrantId("grant 1") }.message shouldBe grantIdMessage("grant 1")
    }

    @Test
    fun `refuses a blank grant id`() {
        shouldThrow<IllegalArgumentException> { GrantId("") }.message shouldBe grantIdMessage("")
    }

    @Test
    fun `refuses a grant id that could not survive being a path segment or a file name`() {
        val refused = listOf("../g1", "g1/g2", "g1\u0000", "g1\u200B", "G1", "a".repeat(65))

        refused.forEach { value ->
            shouldThrow<IllegalArgumentException> { GrantId(value) }.message shouldBe grantIdMessage(value)
        }
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
    fun `accepts a capability whose halves are lower kebab`() {
        val capability = Capability("work-items.bulk-read")

        capability.resource shouldBe "work-items"
        capability.action shouldBe "bulk-read"
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

    /**
     * `resource` and `action` split on the first `.`, so a second one is not a wider
     * capability — it is a capability whose action is silently `read.all`.
     */
    @Test
    fun `refuses a capability carrying more than one dot`() {
        shouldThrow<IllegalArgumentException> { Capability("work.read.all") }
            .message shouldBe "capability must be '<resource>.<action>': 'work.read.all'"
    }

    /** A trailing or leading hyphen is not kebab, and it round-trips differently through a URL. */
    @Test
    fun `refuses a capability whose halves are not kebab`() {
        listOf("agents-.run", "agents.run-", "-agents.run", "agents.-run", "agents--x.run").forEach { value ->
            shouldThrow<IllegalArgumentException> { Capability(value) }
                .message shouldBe "capability must be '<resource>.<action>': '$value'"
        }
    }

    @Test
    fun `distinguishes the two kinds of principal`() {
        ActorKind.entries shouldBe listOf(ActorKind.HUMAN, ActorKind.AGENT)
        ActorKind.valueOf("AGENT") shouldBe ActorKind.AGENT
    }

    private fun actorIdMessage(value: String) =
        "actorId must be 1-64 characters of a-z, 0-9, '.', '_' or '-', " +
            "starting and ending alphanumeric: '$value'"

    private fun grantIdMessage(value: String) =
        "grantId must be 1-64 characters of a-z, 0-9, '.', '_' or '-', " +
            "starting and ending alphanumeric: '$value'"
}
