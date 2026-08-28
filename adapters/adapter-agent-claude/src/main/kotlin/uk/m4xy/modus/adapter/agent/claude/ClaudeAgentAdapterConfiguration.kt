package uk.m4xy.modus.adapter.agent.claude

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Provisional Spring wiring for the ClaudeAgent adapter.
 */
@Configuration
public class ClaudeAgentAdapterConfiguration {
    @Bean
    public fun provideClaudeAgentAdapter(): ClaudeAgentAdapter = ClaudeAgentAdapter()
}
