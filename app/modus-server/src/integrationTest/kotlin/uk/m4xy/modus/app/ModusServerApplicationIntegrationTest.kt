package uk.m4xy.modus.app

import io.kotest.matchers.ints.shouldBeGreaterThan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import kotlin.test.Test

/**
 * An integration test by definition (`doc:35-testing#definitions`): it starts a
 * context. `src/integrationTest` is the only source set where
 * `org.springframework..` resolves at all.
 */
@SpringBootTest
class ModusServerApplicationIntegrationTest(
    private val context: ApplicationContext,
) {
    @Test
    fun `the application context starts with every adapter and module on the classpath`() {
        context.beanDefinitionCount shouldBeGreaterThan 0
    }
}
