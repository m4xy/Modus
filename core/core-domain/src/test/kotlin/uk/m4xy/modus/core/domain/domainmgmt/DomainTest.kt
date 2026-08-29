package uk.m4xy.modus.core.domain.domainmgmt

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import uk.m4xy.modus.core.domain.DomainId
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.AT
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.LATER
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.MINIMAL_PROCESS
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.MODUS
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.MODUS_NAME
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.PROCESS
import uk.m4xy.modus.core.domain.domainmgmt.DomainMgmtFixture.SKUNKWORKS
import uk.m4xy.modus.core.domain.domainmgmt.aggregate.Domain
import uk.m4xy.modus.core.domain.domainmgmt.event.DomainCreated
import uk.m4xy.modus.core.domain.domainmgmt.event.ProcessDefinitionChanged
import kotlin.test.Test

class DomainTest {
    private fun modus() = Domain.create(MODUS, MODUS_NAME, PROCESS, AT)

    @Test
    fun `a created domain carries the id, name and process it was created with`() {
        val domain = modus()

        domain.id shouldBe MODUS
        domain.name shouldBe MODUS_NAME
        domain.processDefinition shouldBe PROCESS
    }

    /**
     * The event carries the process, so a consumer that has only ever seen `DomainCreated`
     * can already guard a transition. That is why there is no `ProcessDefinitionChanged` at
     * creation: nothing changed.
     */
    @Test
    fun `creating a domain raises exactly one event, carrying the process`() {
        val raised = modus().pendingEvents.single() as DomainCreated

        raised.domainId shouldBe MODUS
        raised.name shouldBe MODUS_NAME
        raised.process shouldBe PROCESS
        raised.occurredAt shouldBe AT
    }

    @Test
    fun `adopting a different process replaces it and raises the change`() {
        val domain = modus().adoptProcess(MINIMAL_PROCESS, LATER)

        domain.processDefinition shouldBe MINIMAL_PROCESS
        domain.pendingEvents shouldBe
            listOf(
                DomainCreated(MODUS, MODUS_NAME, PROCESS, AT),
                ProcessDefinitionChanged(MODUS, MINIMAL_PROCESS, LATER),
            )

        val changed = domain.pendingEvents.last() as ProcessDefinitionChanged
        changed.domainId shouldBe MODUS
        changed.process shouldBe MINIMAL_PROCESS
        changed.occurredAt shouldBe LATER
    }

    /**
     * A replayed command or an idempotent import adopts the process already in force. It is
     * not an error, and it is not an event: nothing happened.
     */
    @Test
    fun `adopting the process already in force raises nothing`() {
        val domain = modus().adoptProcess(PROCESS, LATER)

        domain.processDefinition shouldBe PROCESS
        domain.pendingEvents shouldBe listOf(DomainCreated(MODUS, MODUS_NAME, PROCESS, AT))
    }

    /** Equal by value, not by reference: a rebuilt definition is the same process. */
    @Test
    fun `adopting an equal but distinct process instance raises nothing`() {
        val rebuilt = PROCESS.copy()

        val domain = modus().adoptProcess(rebuilt, LATER)

        domain.pendingEvents.size shouldBe 1
    }

    /**
     * Two events on purpose, and this test is why `doc:35-testing#fixture-variation` exists.
     * At size one `toList()` returns `Collections.singletonList`, whose `clear()` throws
     * `UnsupportedOperationException` before it can prove anything about copying — the
     * one-element fixture passed for the wrong reason. At size two it is an `ArrayList` the
     * cast genuinely reaches and empties, so what survives is the root's own list.
     */
    @Test
    fun `pendingEvents is a copy, so draining it cannot empty the root`() {
        val domain = modus().adoptProcess(MINIMAL_PROCESS, LATER)

        val drained = domain.pendingEvents as MutableList
        drained.clear()

        drained.size shouldBe 0
        domain.pendingEvents.size shouldBe 2
    }

    /**
     * Entity identity. Without it a `Set<Domain>` holds a stale copy beside a current one
     * and both answer — the defect `bean:0009` found in `PermissionGrant`.
     */
    @Test
    fun `two instances of one domain id are the same domain, whatever their process`() {
        val current = modus()
        val stale = Domain.create(MODUS, MODUS_NAME, MINIMAL_PROCESS, AT)

        current shouldBe stale
        current.hashCode() shouldBe stale.hashCode()
        setOf(current, stale).size shouldBe 1
    }

    @Test
    fun `a domain equals itself`() {
        val domain = modus()

        domain.equals(domain) shouldBe true
    }

    @Test
    fun `domains with different ids are different domains`() {
        modus() shouldNotBe Domain.create(SKUNKWORKS, MODUS_NAME, PROCESS, AT)
    }

    @Test
    fun `a domain is never equal to something that is not a domain`() {
        modus() shouldNotBe MODUS
        modus() shouldNotBe null
    }

    @Test
    fun `hashCode is the id's, so a rename cannot lose a domain in a set`() {
        modus().hashCode() shouldBe DomainId("modus-core").hashCode()
    }
}
