package uk.m4xy.modus.core.application.event

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import uk.m4xy.modus.core.application.ApplicationFixture.AT
import uk.m4xy.modus.core.application.ApplicationFixture.LATER
import uk.m4xy.modus.core.application.ApplicationFixture.MODUS
import uk.m4xy.modus.core.application.ApplicationFixture.OTHER_PROCESS
import uk.m4xy.modus.core.application.ApplicationFixture.domain
import uk.m4xy.modus.core.application.RecordingDispatch
import uk.m4xy.modus.core.domain.DomainEvent
import uk.m4xy.modus.core.domain.aggregate.RaisesDomainEvents
import uk.m4xy.modus.core.domain.domainmgmt.event.DomainCreated
import uk.m4xy.modus.core.domain.domainmgmt.event.ProcessDefinitionChanged
import kotlin.test.Test

/**
 * The ordering contract: **write, drain, dispatch** — and nothing dispatched when the write
 * fails (`bean:0066` criteria 6 and 8).
 *
 * Every assertion here reads [RecordingDispatch], which delivers nothing to anybody. That
 * is deliberate: this file is about what the dispatcher was **given**, and no handler is
 * involved in deciding it. `SynchronousDomainEventDispatchTest` is the other half.
 */
class WriteThenDispatchTest {
    @Test
    fun `writes before it dispatches`() {
        val order = mutableListOf<String>()
        val dispatch =
            DomainEventDispatchPort { events ->
                order += "dispatch(${events.size})"
            }
        val root = domain().adoptProcess(OTHER_PROCESS, LATER)

        WriteThenDispatch(dispatch).write(root) { order += "save" }

        order shouldBe listOf("save", "dispatch(2)")
    }

    @Test
    fun `hands over exactly what the aggregate raised, in the order it raised it`() {
        val dispatch = RecordingDispatch()
        val root = domain().adoptProcess(OTHER_PROCESS, LATER)

        WriteThenDispatch(dispatch).write(root) { }

        dispatch.calls.size shouldBe 1
        dispatch.dispatched.size shouldBe 2
        dispatch.dispatched[0].shouldBeInstanceOf<DomainCreated>()
        dispatch.dispatched[1].shouldBeInstanceOf<ProcessDefinitionChanged>()
    }

    @Test
    fun `passes the aggregate itself to the write, once`() {
        val written = mutableListOf<RaisesDomainEvents>()
        val root = domain()

        WriteThenDispatch(RecordingDispatch()).write(root) { written += it }

        written shouldBe listOf(root)
    }

    /**
     * The defect `bean:0066` was raised for, at the layer that would have shipped it.
     *
     * Before the drain existed, `pendingEvents` was the only way out and it left the events
     * on the aggregate; a second write of the same instance handed `DomainCreated` over for
     * a second time. `domainmgmt`'s consumers treat `DomainCreated` as "this tenant now
     * exists", so a redelivery creates a tenant that already exists.
     */
    @Test
    fun `a second write of the same aggregate dispatches nothing`() {
        val dispatch = RecordingDispatch()
        val write = WriteThenDispatch(dispatch)
        val root = domain()

        write.write(root) { }
        write.write(root) { }

        dispatch.calls.size shouldBe 2
        dispatch.calls[0].size shouldBe 1
        dispatch.calls[1] shouldBe emptyList()
        dispatch.dispatched.size shouldBe 1
    }

    @Test
    fun `a second write dispatches only what the second command raised`() {
        val dispatch = RecordingDispatch()
        val write = WriteThenDispatch(dispatch)
        val root = domain()

        write.write(root) { }
        root.adoptProcess(OTHER_PROCESS, LATER)
        write.write(root) { }

        dispatch.calls[0].single().shouldBeInstanceOf<DomainCreated>()
        dispatch.calls[1].single().shouldBeInstanceOf<ProcessDefinitionChanged>()
    }

    /**
     * Criterion 6. The healthy case above is what makes this one mean something: a
     * dispatcher that was never called for any input would pass this assertion identically
     * (`doc:00-constitution#observed-failing`).
     */
    @Test
    fun `dispatches nothing when the write fails, and leaves the events on the aggregate`() {
        val dispatch = RecordingDispatch()
        val root = domain().adoptProcess(OTHER_PROCESS, LATER)

        shouldThrow<IllegalStateException> {
            WriteThenDispatch(dispatch).write(root) { error("the store is unwritable") }
        }.message shouldBe "the store is unwritable"

        dispatch.calls shouldBe emptyList()
        dispatch.dispatched shouldBe emptyList()
        root.pendingEvents.size shouldBe 2
    }

    @Test
    fun `a write that raised nothing dispatches an empty list rather than skipping the call`() {
        val dispatch = RecordingDispatch()
        val root = domain()
        root.drainEvents()

        WriteThenDispatch(dispatch).write(root) { }

        dispatch.calls shouldBe listOf(emptyList<DomainEvent>())
    }

    @Test
    fun `the events retried after a failed write are dispatched by the write that succeeds`() {
        val dispatch = RecordingDispatch()
        val write = WriteThenDispatch(dispatch)
        val root = domain().adoptProcess(OTHER_PROCESS, LATER)

        shouldThrow<IllegalStateException> { write.write(root) { error("the store is unwritable") } }
        write.write(root) { }

        dispatch.dispatched.size shouldBe 2
        dispatch.dispatched[0].occurredAt shouldBe AT
        dispatch.dispatched[1].occurredAt shouldBe LATER
        root.id shouldBe MODUS
    }
}
