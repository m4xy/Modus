package uk.m4xy.modus.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class BoundedContextsTest {
    @Test
    fun `every bounded context is registered exactly once`() {
        assertEquals(BoundedContexts.names.distinct(), BoundedContexts.names)
        assertEquals(6, BoundedContexts.names.size)
    }
}
