package com.realtime.communication.shared.infrastructure.messaging;

import com.realtime.communication.shared.application.event.Event;
import com.realtime.communication.shared.application.event.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka implementation of the EventPublisher port.
 * Publishes domain events to Kafka topics.
 */
@Component
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String DEFAULT_TOPIC = "domain-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(Event event) {
        publish(DEFAULT_TOPIC, event);
    }

    @Override
    public void publish(String topic, Event event) {
        try {
            kafkaTemplate.send(topic, event.getEventId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.debug("Event published successfully: {} to topic: {}",
                                    event.getEventType(), topic);
                        } else {
                            logger.error("Failed to publish event: {} to topic: {}",
                                    event.getEventType(), topic, ex);
                        }
                    });
        } catch (Exception e) {
            logger.error("Error publishing event: {}", event.getEventType(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}

