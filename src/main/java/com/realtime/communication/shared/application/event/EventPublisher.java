package com.realtime.communication.shared.application.event;

/**
 * Port interface for publishing domain events.
 * Implementations can use Kafka, RabbitMQ, or in-memory event bus.
 */
public interface EventPublisher {

    /**
     * Publish a domain event to the event bus.
     *
     * @param event The event to publish
     */
    void publish(Event event);

    /**
     * Publish a domain event to a specific topic/channel.
     *
     * @param topic The topic to publish to
     * @param event The event to publish
     */
    void publish(String topic, Event event);
}

