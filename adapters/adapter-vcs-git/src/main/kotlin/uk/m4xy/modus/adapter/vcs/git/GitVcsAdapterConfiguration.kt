package uk.m4xy.modus.adapter.vcs.git

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Provisional Spring wiring for the GitVcs adapter.
 */
@Configuration
public class GitVcsAdapterConfiguration {
    @Bean
    public fun provideGitVcsAdapter(): GitVcsAdapter = GitVcsAdapter()
}
