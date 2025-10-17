package com.realtime.communication.shared.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * Base class for Kafka event consumers.
 * Provides common logging and error handling for event consumption.
 */
public abstract class KafkaEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventConsumer.class);

    /**
     * Handle incoming event with metadata.
     *
     * @param event The event payload
     * @param topic The topic the event came from
     * @param partition The partition number
     * @param offset The offset in the partition
     */
    protected void handleEvent(@Payload Object event,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            logger.debug("Received event from topic: {}, partition: {}, offset: {}",
                    topic, partition, offset);
            processEvent(event);
        } catch (Exception e) {
            logger.error("Error processing event from topic: {}", topic, e);
            handleError(event, e);
        }
    }

    /**
     * Process the event. Must be implemented by subclasses.
     *
     * @param event The event to process
     */
    protected abstract void processEvent(Object event);

    /**
     * Handle errors during event processing.
     * Can be overridden for custom error handling (e.g., DLQ).
     *
     * @param event The event that failed
     * @param error The error that occurred
     */
    protected void handleError(Object event, Exception error) {
        logger.error("Event processing failed: {}", event, error);
        // Default implementation logs the error
        // Subclasses can override to send to DLQ, retry, etc.
    }
}

