package com.realtime.communication.auth.adapter.out.messaging;

import com.realtime.communication.auth.domain.event.UserLoggedInEvent;
import com.realtime.communication.auth.domain.event.UserRegisteredEvent;
import com.realtime.communication.shared.application.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Kafka adapter for publishing auth-related domain events.
 * Publishes events to the auth-events topic for consumption by other services.
 */
@Component
public class KafkaAuthEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaAuthEventPublisher.class);
    private static final String AUTH_EVENTS_TOPIC = "auth-events";

    private final EventPublisher eventPublisher;

    public KafkaAuthEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publishes UserRegisteredEvent to Kafka
     */
    public void publishUserRegistered(UserRegisteredEvent event) {
        logger.info("Publishing UserRegisteredEvent for userId: {}", event.getUserId());
        eventPublisher.publish(AUTH_EVENTS_TOPIC, event);
    }

    /**
     * Publishes UserLoggedInEvent to Kafka
     */
    public void publishUserLoggedIn(UserLoggedInEvent event) {
        logger.info("Publishing UserLoggedInEvent for userId: {}", event.getUserId());
        eventPublisher.publish(AUTH_EVENTS_TOPIC, event);
    }
}

