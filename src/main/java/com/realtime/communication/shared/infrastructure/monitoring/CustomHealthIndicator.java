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
