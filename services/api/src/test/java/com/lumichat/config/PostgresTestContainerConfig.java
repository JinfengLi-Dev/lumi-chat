package com.lumichat.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base configuration for integration tests that require PostgreSQL.
 * Uses Testcontainers to spin up a real PostgreSQL instance.
 *
 * Required for tests involving PostgreSQL-specific features like:
 * - Array types (bigint[]) used in Conversation and Message entities
 * - Native queries with PostgreSQL syntax
 * - JSONB columns
 *
 * Uses singleton pattern to share the container across all test classes
 * for better performance and to avoid container lifecycle issues.
 *
 * @see <a href="https://github.com/testcontainers/testcontainers-java/issues/11212">
 *      Testcontainers Docker API version fix</a>
 */
public abstract class PostgresTestContainerConfig {

    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "false");
    }
}
