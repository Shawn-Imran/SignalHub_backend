package com.realtime.communication.shared.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * PostgreSQL database configuration.
 * Enables JPA repositories, entity scanning, and transaction management.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.realtime.communication")
@EntityScan(basePackages = "com.realtime.communication")
@EnableJpaAuditing
@EnableTransactionManagement
public class PostgresConfig {
    // Additional PostgreSQL-specific configurations can be added here
}

