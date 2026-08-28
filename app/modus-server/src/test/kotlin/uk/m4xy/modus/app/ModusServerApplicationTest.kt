package uk.m4xy.modus.app

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import kotlin.test.Test
import kotlin.test.assertTrue

@SpringBootTest
class ModusServerApplicationTest(
    private val context: ApplicationContext,
) {
    @Test
    fun `the application context starts with every adapter and module on the classpath`() {
        assertTrue(context.beanDefinitionCount > 0)
    }
}
