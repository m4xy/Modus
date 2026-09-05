package uk.m4xy.modus.core.domain.work

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import uk.m4xy.modus.core.domain.work.WorkFixture.EPIC
import uk.m4xy.modus.core.domain.work.WorkFixture.MODUS
import uk.m4xy.modus.core.domain.work.WorkFixture.OTHER_EPIC
import uk.m4xy.modus.core.domain.work.WorkFixture.SKUNKWORKS
import uk.m4xy.modus.core.domain.work.WorkFixture.TITLE
import uk.m4xy.modus.core.domain.work.aggregate.Epic
import uk.m4xy.modus.core.domain.work.published.WorkItemTitle
import kotlin.test.Test

class EpicTest {
    private fun epic(
        id: uk.m4xy.modus.core.domain.work.published.EpicId = EPIC,
        title: WorkItemTitle = TITLE,
    ) = Epic.create(id, MODUS, title)

    @Test
    fun `a created epic carries the id, domain and title it was created with`() {
        val subject = epic()

        subject.id shouldBe EPIC
        subject.domainId shouldBe MODUS
        subject.title shouldBe TITLE
    }

    /**
     * An epic raises nothing. `doc:10-architecture#bounded-contexts` §3 names the three
     * events this context publishes and `EpicCreated` is not among them; adding one extends
     * a published contract five other contexts read. Asserted rather than left implicit, so
     * that adding an event is a test change and therefore a visible decision.
     */
    @Test
    fun `creating an epic raises no domain event`() {
        Epic::class.java.methods
            .map { it.name }
            .contains("getPendingEvents") shouldBe false
    }

    /**
     * Entity, not value: the title is not part of identity. Without this a `Set<Epic>` could
     * hold a renamed copy beside the original and both would answer — `bean:0009`'s defect
     * in `PermissionGrant`.
     */
    @Test
    fun `two instances of one epic id are the same epic, whatever they are called`() {
        val subject = epic()
        val retitled = epic(title = WorkItemTitle("The work bounded context"))

        subject shouldBe retitled
        subject.hashCode() shouldBe retitled.hashCode()
        subject shouldBe subject
    }

    @Test
    fun `epics with different ids are different, and an epic is not some other type`() {
        epic() shouldNotBe epic(id = OTHER_EPIC)
        epic() shouldNotBe EPIC
    }

    /**
     * Two epics may share an id across domains only if the id itself does; identity here is
     * the id alone, which is what `WorkItem.epicId` resolves against.
     */
    @Test
    fun `an epic knows which domain it belongs to`() {
        Epic.create(EPIC, SKUNKWORKS, TITLE).domainId shouldBe SKUNKWORKS
    }
}
