package com.realtime.communication.chat.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Metrics collector for message operations
 */
@Component
public class MessageMetricsCollector {

    private final Counter messagesSentCounter;
    private final Counter messagesDeliveredCounter;
    private final Counter messagesReadCounter;
    private final Timer messageDeliveryTimer;

    public MessageMetricsCollector(MeterRegistry meterRegistry) {
        this.messagesSentCounter = Counter.builder("messages.sent")
            .description("Total number of messages sent")
            .register(meterRegistry);

        this.messagesDeliveredCounter = Counter.builder("messages.delivered")
            .description("Total number of messages delivered")
            .register(meterRegistry);

        this.messagesReadCounter = Counter.builder("messages.read")
            .description("Total number of messages read")
            .register(meterRegistry);

        this.messageDeliveryTimer = Timer.builder("messages.delivery.time")
            .description("Time taken for message delivery")
            .register(meterRegistry);
    }

    public void incrementMessagesSent() {
        messagesSentCounter.increment();
    }

    public void incrementMessagesDelivered() {
        messagesDeliveredCounter.increment();
    }

    public void incrementMessagesRead() {
        messagesReadCounter.increment();
    }

    public Timer.Sample startDeliveryTimer() {
        return Timer.start();
    }

    public void recordDeliveryTime(Timer.Sample sample) {
        sample.stop(messageDeliveryTimer);
    }
}
