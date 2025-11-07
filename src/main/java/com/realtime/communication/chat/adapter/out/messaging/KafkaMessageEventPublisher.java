package com.realtime.communication.chat.adapter.out.messaging;

import com.realtime.communication.chat.domain.event.MessageDeliveredEvent;
import com.realtime.communication.chat.domain.event.MessageReadEvent;
import com.realtime.communication.chat.domain.event.MessageSentEvent;
import com.realtime.communication.shared.application.event.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka implementation for publishing message events
 */
@Component
public class KafkaMessageEventPublisher {

    private static final String MESSAGE_EVENTS_TOPIC = "message-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaMessageEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        String key = determineKey(event);

        kafkaTemplate.send(MESSAGE_EVENTS_TOPIC, key, event);
    }

    private String determineKey(Event event) {
        if (event instanceof MessageSentEvent) {
            return ((MessageSentEvent) event).getConversationId().toString();
        } else if (event instanceof MessageDeliveredEvent) {
            return ((MessageDeliveredEvent) event).getMessageId().getValue().toString();
        } else if (event instanceof MessageReadEvent) {
            return ((MessageReadEvent) event).getMessageId().getValue().toString();
        }
        return event.getEventId();
    }
}
