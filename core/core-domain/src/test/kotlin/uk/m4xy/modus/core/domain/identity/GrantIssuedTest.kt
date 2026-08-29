package uk.m4xy.modus.core.domain.identity

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.identity.IdentityFixture.AGENTS_READ
import uk.m4xy.modus.core.domain.identity.IdentityFixture.AGENTS_RUN
import uk.m4xy.modus.core.domain.identity.IdentityFixture.ALICE
import uk.m4xy.modus.core.domain.identity.IdentityFixture.AT
import uk.m4xy.modus.core.domain.identity.IdentityFixture.BOB
import uk.m4xy.modus.core.domain.identity.IdentityFixture.COST_READ
import uk.m4xy.modus.core.domain.identity.IdentityFixture.MODUS
import uk.m4xy.modus.core.domain.identity.IdentityFixture.SECRET
import uk.m4xy.modus.core.domain.identity.event.GrantIssued
import uk.m4xy.modus.core.domain.identity.published.ActorId
import uk.m4xy.modus.core.domain.identity.published.Capability
import uk.m4xy.modus.core.domain.identity.published.GrantId
import java.time.Instant
import kotlin.test.Test

/**
 * [GrantIssued] is the third type in which this repository published a collection it owned
 * (`bean:0036`), and the first found by a gate rather than by review. Every fixture here
 * carries **two** capabilities: at size one `toSet()` returns Kotlin's immutable singleton and
 * the down-cast throws, so the same test would pass while proving nothing
 * (`doc:35-testing#fixture-variation`).
 */
class GrantIssuedTest {
    @Test
    fun `carries exactly the capabilities it was issued with`() {
        event().capabilities shouldBe setOf(AGENTS_READ, AGENTS_RUN)
    }

    /** Copy in: a caller mutating the set it supplied changes nothing this event states. */
    @Test
    fun `does not alias the set it was constructed from`() {
        val supplied = mutableSetOf(AGENTS_READ, AGENTS_RUN)
        val issued = GrantIssued(GrantId("g1"), ALICE, MODUS, supplied, AT)

        supplied += COST_READ

        issued.capabilities shouldBe setOf(AGENTS_READ, AGENTS_RUN)
    }

    /**
     * Copy out: the escape itself. Without the copying getter this cast succeeds against the
     * backing collection and a handler adds a capability to a fact that already happened.
     */
    @Test
    fun `hands out a copy, so a handler cannot add a capability to a fact already stated`() {
        val issued = event()
        val taken = issued.capabilities

        @Suppress("UNCHECKED_CAST")
        (taken as MutableSet<Capability>) += COST_READ

        issued.capabilities shouldBe setOf(AGENTS_READ, AGENTS_RUN)
    }

    /**
     * It cannot be a `data class` — it holds a collection (`doc:20-ddd-practices` §3.1) — so
     * equality is hand-written, and an event is a value: equal by content or not at all.
     */
    @Test
    fun `two events stating the same fact are equal, whatever the instance`() {
        val rebuilt = event()

        rebuilt shouldBe event()
        rebuilt.hashCode() shouldBe event().hashCode()
        setOf(event(), rebuilt).size shouldBe 1
        rebuilt.equals(rebuilt) shouldBe true
    }

    /** One variant per field `equals` compares, each differing only in that field. */
    @Test
    fun `events differing in any one field are not equal`() {
        event() shouldNotBe event(id = "g2")
        event() shouldNotBe event(actor = BOB)
        event() shouldNotBe event(domain = SECRET)
        event() shouldNotBe event(at = AT.plusSeconds(1))
        event() shouldNotBe event(capabilities = setOf(AGENTS_READ, COST_READ))
    }

    @Test
    fun `an event is never equal to something that is not one`() {
        event() shouldNotBe GrantId("g1")
        event() shouldNotBe null
    }

    /** The rendering an agent reads in a log line or a failure message, so it names every part. */
    @Test
    fun `renders the grant, the actor, the domain, the capabilities and the instant`() {
        val rendered = event().toString()

        rendered shouldContain "grantId=GrantId(value=g1)"
        rendered shouldContain "actorId=ActorId(value=alice)"
        rendered shouldContain "domainId=DomainId(value=modus-core)"
        rendered shouldContain "capabilities="
        rendered shouldContain "occurredAt=$AT"
    }

    private fun event(
        id: String = "g1",
        actor: ActorId = ALICE,
        domain: DomainId = MODUS,
        capabilities: Set<Capability> = setOf(AGENTS_READ, AGENTS_RUN),
        at: Instant = AT,
    ): GrantIssued = GrantIssued(GrantId(id), actor, domain, capabilities, at)
}
