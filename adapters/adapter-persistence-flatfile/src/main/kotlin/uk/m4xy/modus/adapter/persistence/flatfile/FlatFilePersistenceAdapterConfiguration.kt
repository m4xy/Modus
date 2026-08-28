package uk.m4xy.modus.adapter.persistence.flatfile

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Provisional Spring wiring for the FlatFilePersistence adapter.
 */
@Configuration
public class FlatFilePersistenceAdapterConfiguration {
    @Bean
    public fun provideFlatFilePersistenceAdapter(): FlatFilePersistenceAdapter = FlatFilePersistenceAdapter()
}
