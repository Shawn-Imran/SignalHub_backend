package com.realtime.communication.integration.chat;

import com.realtime.communication.chat.adapter.out.persistence.JpaMessageRepository;
import com.realtime.communication.chat.adapter.out.persistence.MessageJpaEntity;
import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for JpaMessageRepository
 * Tests database operations with real PostgreSQL via TestContainers
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("JpaMessageRepository Integration Tests")
class JpaMessageRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JpaMessageRepository messageRepository;

    @Test
    @DisplayName("Should save and retrieve message")
    void shouldSaveAndRetrieveMessage() {
        // Given
        MessageJpaEntity message = createTestMessage();

        // When
        MessageJpaEntity saved = messageRepository.save(message);

        // Then
        assertNotNull(saved.getId());
        assertEquals(message.getContent(), saved.getContent());
    }

    @Test
    @DisplayName("Should find messages by conversation ID with pagination")
    void shouldFindMessagesByConversationId() {
        // Given
        UUID conversationId = UUID.randomUUID();
        messageRepository.save(createTestMessage(conversationId, "Message 1"));
        messageRepository.save(createTestMessage(conversationId, "Message 2"));
        messageRepository.save(createTestMessage(conversationId, "Message 3"));

        // When
        Page<Message> result = messageRepository.findByConversationId(
            new ConversationId(conversationId),
            PageRequest.of(0, 10)
        );

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
    }

    @Test
    @DisplayName("Should update message status")
    void shouldUpdateMessageStatus() {
        // Given
        MessageJpaEntity message = createTestMessage();
        MessageJpaEntity saved = messageRepository.save(message);

        // When
        saved.setStatus("DELIVERED");
        saved.setDeliveredAt(Instant.now());
        MessageJpaEntity updated = messageRepository.save(saved);

        // Then
        assertEquals("DELIVERED", updated.getStatus());
        assertNotNull(updated.getDeliveredAt());
    }

    private MessageJpaEntity createTestMessage() {
        return createTestMessage(UUID.randomUUID(), "Test message");
    }

    private MessageJpaEntity createTestMessage(UUID conversationId, String content) {
        MessageJpaEntity message = new MessageJpaEntity();
        message.setConversationId(conversationId);
        message.setSenderId(UUID.randomUUID());
        message.setContent(content);
        message.setType("TEXT");
        message.setStatus("SENT");
        message.setSentAt(Instant.now());
        return message;
    }
}

