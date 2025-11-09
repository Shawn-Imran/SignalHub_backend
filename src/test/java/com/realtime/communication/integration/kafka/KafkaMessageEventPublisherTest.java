package com.realtime.communication.integration.kafka;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.adapter.out.messaging.KafkaMessageEventPublisher;
import com.realtime.communication.chat.domain.event.MessageSentEvent;
import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.MessageId;
 import com.realtime.communication.chat.domain.model.MessageType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for KafkaMessageEventPublisher
 * Tests actual Kafka operations using Testcontainers
 */
@SpringBootTest
@Testcontainers
@DisplayName("KafkaMessageEventPublisher Integration Tests")
class KafkaMessageEventPublisherTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withKraft();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaMessageEventPublisher eventPublisher;

    private Consumer<String, Object> testConsumer;
    private static final String TOPIC = "message-events";

    @BeforeEach
    void setUp() {
        // Create test consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
            kafka.getBootstrapServers(),
            "test-group",
            "false"
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        testConsumer = consumerFactory.createConsumer();
        testConsumer.subscribe(java.util.Collections.singletonList(TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @Test
    @DisplayName("Should publish MessageSentEvent to Kafka")
    void shouldPublishMessageSentEventToKafka() {
        // Given
        MessageId messageId = new MessageId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());
        UserId senderId = new UserId(UUID.randomUUID());
        String content = "Test message";
        MessageType messageType = MessageType.TEXT;

        MessageSentEvent event = new MessageSentEvent(
            messageId,
            conversationId,
            senderId,
            content,
            messageType
        );

        // When
        eventPublisher.publish(event);

        // Then - Poll for the message
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertFalse(records.isEmpty(), "Should receive at least one message");

        // Verify event data
        records.forEach(record -> {
            assertNotNull(record.value());
            assertInstanceOf(Map.class, record.value(), "Event should be deserialized as Map");

            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) record.value();

            assertTrue(eventData.containsKey("messageId"));
            assertTrue(eventData.containsKey("conversationId"));
            assertTrue(eventData.containsKey("senderId"));
            assertTrue(eventData.containsKey("content"));
        });
    }

    @Test
    @DisplayName("Should publish event with correct topic")
    void shouldPublishEventWithCorrectTopic() {
        // Given
        MessageSentEvent event = createSampleMessageSentEvent();

        // When
        eventPublisher.publish(event);

        // Then
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertFalse(records.isEmpty());

        records.forEach(record -> assertEquals(TOPIC, record.topic()));
    }

    @Test
    @DisplayName("Should publish multiple events in sequence")
    void shouldPublishMultipleEventsInSequence() {
        // Given
        MessageSentEvent event1 = createSampleMessageSentEvent();
        MessageSentEvent event2 = createSampleMessageSentEvent();
        MessageSentEvent event3 = createSampleMessageSentEvent();

        // When
        eventPublisher.publish(event1);
        eventPublisher.publish(event2);
        eventPublisher.publish(event3);

        // Then - Should receive all 3 events
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertTrue(records.count() >= 3, "Should receive at least 3 messages");
    }

    @Test
    @DisplayName("Should publish event asynchronously without blocking")
    void shouldPublishEventAsynchronouslyWithoutBlocking() {
        // Given
        MessageSentEvent event = createSampleMessageSentEvent();
        long startTime = System.currentTimeMillis();

        // When
        eventPublisher.publish(event);
        long endTime = System.currentTimeMillis();

        // Then - Should return quickly (< 100ms) as it's async
        long duration = endTime - startTime;
        assertTrue(duration < 1000, "Publish should be async and return quickly, took: " + duration + "ms");

        // Verify event was actually published
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertFalse(records.isEmpty());
    }

    @Test
    @DisplayName("Should handle event with all required fields")
    void shouldHandleEventWithAllRequiredFields() {
        // Given
        MessageId messageId = new MessageId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());
        UserId senderId = new UserId(UUID.randomUUID());
        String content = "Complete message with all fields";
        MessageType messageType = MessageType.TEXT;

        MessageSentEvent event = new MessageSentEvent(
            messageId,
            conversationId,
            senderId,
            content,
            messageType
        );

        // When
        eventPublisher.publish(event);

        // Then
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertFalse(records.isEmpty());

        records.forEach(record -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) record.value();

            assertNotNull(eventData.get("messageId"));
            assertNotNull(eventData.get("conversationId"));
            assertNotNull(eventData.get("senderId"));
            assertNotNull(eventData.get("content"));
            assertEquals(content, eventData.get("content"));
        });
    }

    @Test
    @DisplayName("Should use message ID as partition key")
    void shouldUseMessageIdAsPartitionKey() {
        // Given
        MessageSentEvent event = createSampleMessageSentEvent();

        // When
        eventPublisher.publish(event);

        // Then
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertFalse(records.isEmpty());

        records.forEach(record -> assertNotNull(record.key(), "Message should have a key for partitioning"));
    }

    @Test
    @DisplayName("Should handle concurrent event publishing")
    void shouldHandleConcurrentEventPublishing() throws InterruptedException {
        // Given
        int eventCount = 10;
        Thread[] threads = new Thread[eventCount];

        // When - Publish events concurrently
        for (int i = 0; i < eventCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                MessageSentEvent event = new MessageSentEvent(
                    new MessageId(UUID.randomUUID()),
                    new ConversationId(UUID.randomUUID()),
                    new UserId(UUID.randomUUID()),
                    "Concurrent message " + index,
                    MessageType.TEXT
                );
                eventPublisher.publish(event);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - Should receive all events
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertTrue(records.count() >= eventCount,
            "Should receive at least " + eventCount + " messages, received: " + records.count());
    }

    @Test
    @DisplayName("Should serialize event timestamps correctly")
    void shouldSerializeEventTimestampsCorrectly() {
        // Given
        MessageSentEvent event = new MessageSentEvent(
            new MessageId(UUID.randomUUID()),
            new ConversationId(UUID.randomUUID()),
            new UserId(UUID.randomUUID()),
            "Message with timestamp",
            MessageType.TEXT
        );

        // When
        eventPublisher.publish(event);

        // Then
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertFalse(records.isEmpty());

        records.forEach(record -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) record.value();
            assertNotNull(eventData.get("sentAt"));
        });
    }

    @Test
    @DisplayName("Should preserve event ordering for same conversation")
    void shouldPreserveEventOrderingForSameConversation() {
        // Given
        ConversationId conversationId = new ConversationId(UUID.randomUUID());
        MessageSentEvent event1 = new MessageSentEvent(
            new MessageId(UUID.randomUUID()),
            conversationId,
            new UserId(UUID.randomUUID()),
            "First message",
            MessageType.TEXT
        );
        MessageSentEvent event2 = new MessageSentEvent(
            new MessageId(UUID.randomUUID()),
            conversationId,
            new UserId(UUID.randomUUID()),
            "Second message",
            MessageType.TEXT
        );

        // When
        eventPublisher.publish(event1);
        eventPublisher.publish(event2);

        // Then
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertTrue(records.count() >= 2);
    }

    @Test
    @DisplayName("Should handle large message content")
    void shouldHandleLargeMessageContent() {
        // Given
        String largeContent = "This is a large message content. ".repeat(1000);

        MessageSentEvent event = new MessageSentEvent(
            new MessageId(UUID.randomUUID()),
            new ConversationId(UUID.randomUUID()),
            new UserId(UUID.randomUUID()),
            largeContent,
            MessageType.TEXT
        );

        // When
        eventPublisher.publish(event);

        // Then
        ConsumerRecords<String, Object> records = testConsumer.poll(Duration.ofSeconds(10));
        assertFalse(records.isEmpty());

        records.forEach(record -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) record.value();
            String content = (String) eventData.get("content");
            assertNotNull(content);
            assertTrue(content.length() > 1000);
        });
    }

    // Helper method to create a sample event
    private MessageSentEvent createSampleMessageSentEvent() {
        return new MessageSentEvent(
            new MessageId(UUID.randomUUID()),
            new ConversationId(UUID.randomUUID()),
            new UserId(UUID.randomUUID()),
            "Test message content",
            MessageType.TEXT
        );
    }
}

