package uk.m4xy.modus.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class BoundedContextsTest {
    @Test
    fun `every bounded context is registered exactly once`() {
        assertEquals(BoundedContexts.names.distinct(), BoundedContexts.names)
        assertEquals(6, BoundedContexts.names.size)
    }

    /**
     * `listOf` over more than one element returns a `java.util.Arrays$ArrayList`, whose `set`
     * works even though `add` does not — so publishing the backing list from a singleton let
     * any caller rename a context for every other caller
     * (`doc:20-ddd-practices#value-objects` §3.1). Six elements, never one:
     * `doc:35-testing#fixture-variation`.
     */
    @Test
    fun `hands out a copy, so a caller cannot rename a context for everyone else`() {
        val taken = BoundedContexts.names

        assertNotSame(taken, BoundedContexts.names)

        @Suppress("UNCHECKED_CAST")
        (taken as MutableList<String>)[0] = "hijacked"

        assertEquals("identity", BoundedContexts.names[0])
    }
}
