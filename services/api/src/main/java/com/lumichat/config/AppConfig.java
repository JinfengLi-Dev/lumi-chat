package com.lumichat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Application-wide configuration for common beans.
 */
@Configuration
public class AppConfig {

    /**
     * Provides a system Clock bean for production use.
     * This can be overridden in tests for deterministic time-based testing.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
