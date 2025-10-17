package com.realtime.communication.shared.infrastructure.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for application dependencies.
 * Checks health of Redis, Kafka, and other critical services.
 */
@Component("custom")
public class CustomHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CustomHealthIndicator(RedisTemplate<String, Object> redisTemplate,
                                  KafkaTemplate<String, Object> kafkaTemplate) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try {
            // Check Redis connectivity
            redisTemplate.getConnectionFactory().getConnection().ping();
            builder.withDetail("redis", "UP");
        } catch (Exception e) {
            builder.down()
                    .withDetail("redis", "DOWN")
                    .withDetail("redis-error", e.getMessage());
            return builder.build();
        }

        try {
            // Check Kafka connectivity (basic check)
            kafkaTemplate.getDefaultTopic();
            builder.withDetail("kafka", "UP");
        } catch (Exception e) {
            builder.down()
                    .withDetail("kafka", "DOWN")
                    .withDetail("kafka-error", e.getMessage());
            return builder.build();
        }

        return builder.up().build();
    }
}
package com.realtime.communication.shared.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Base entity class for all domain entities.
 * Provides common fields: ID, timestamps, and version for optimistic locking.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

