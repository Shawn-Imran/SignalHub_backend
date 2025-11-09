package com.realtime.communication.integration.chat;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.port.MessageRepository;
import com.realtime.communication.chat.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JpaMessageRepository
 * Tests actual database operations using Testcontainers
 */
@SpringBootTest
@Testcontainers
@DisplayName("JpaMessageRepository Integration Tests")
class JpaMessageRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MessageRepository messageRepository;

    private ConversationId conversationId;
    private UserId senderId;
    private UserId recipientId;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        conversationId = new ConversationId(UUID.randomUUID());
        senderId = new UserId(UUID.randomUUID());
        recipientId = new UserId(UUID.randomUUID());

        testMessage = new Message(
            new MessageId(UUID.randomUUID()),
            conversationId,
            senderId,
            "Test message content",
            MessageType.TEXT
        );
    }

    @Test
    @DisplayName("Should save and retrieve message by ID")
    void shouldSaveAndRetrieveMessageById() {
        // When
        Message savedMessage = messageRepository.save(testMessage);
        Optional<Message> retrievedMessage = messageRepository.findById(savedMessage.getId());

        // Then
        assertTrue(retrievedMessage.isPresent());
        assertEquals(testMessage.getId(), retrievedMessage.get().getId());
        assertEquals(testMessage.getContent(), retrievedMessage.get().getContent());
        assertEquals(testMessage.getSenderId(), retrievedMessage.get().getSenderId());
    }

    @Test
    @DisplayName("Should find messages by conversation ID")
    void shouldFindMessagesByConversationId() {
        // Given
        Message message1 = new Message(
            new MessageId(UUID.randomUUID()),
            conversationId,
            senderId,
            "Message 1",
            MessageType.TEXT
        );
        Message message2 = new Message(
            new MessageId(UUID.randomUUID()),
            conversationId,
            recipientId,
            "Message 2",
            MessageType.TEXT
        );

        messageRepository.save(message1);
        messageRepository.save(message2);

        // When
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("sentAt").descending());
        Page<Message> messages = messageRepository.findByConversationId(conversationId, pageable);

        // Then
        assertEquals(2, messages.getContent().size());
    }

    @Test
    @DisplayName("Should find messages by conversation ID with pagination")
    void shouldFindMessagesByConversationIdWithPagination() {
        // Given - Create 5 messages
        for (int i = 0; i < 5; i++) {
            Message message = new Message(
                new MessageId(UUID.randomUUID()),
                conversationId,
                senderId,
                "Message " + i,
                MessageType.TEXT
            );
            messageRepository.save(message);
        }

        // When - Request first page with size 2
        PageRequest pageable = PageRequest.of(0, 2, Sort.by("sentAt").descending());
        Page<Message> firstPage = messageRepository.findByConversationId(conversationId, pageable);

        // Then
        assertEquals(2, firstPage.getContent().size());
        assertEquals(5, firstPage.getTotalElements());
        assertEquals(3, firstPage.getTotalPages());
        assertTrue(firstPage.hasNext());
    }

    @Test
    @DisplayName("Should update message status to DELIVERED")
    void shouldUpdateMessageStatusToDelivered() {
        // Given
        Message savedMessage = messageRepository.save(testMessage);
        assertEquals(MessageStatus.SENT, savedMessage.getStatus());

        // When
        savedMessage.markAsDelivered();
        Message updatedMessage = messageRepository.save(savedMessage);
        Message retrievedMessage = messageRepository.findById(updatedMessage.getId()).orElseThrow();

        // Then
        assertEquals(MessageStatus.DELIVERED, retrievedMessage.getStatus());
        assertNotNull(retrievedMessage.getDeliveredAt());
    }

    @Test
    @DisplayName("Should update message status to READ")
    void shouldUpdateMessageStatusToRead() {
        // Given
        Message savedMessage = messageRepository.save(testMessage);

        // When
        savedMessage.markAsRead();
        Message updatedMessage = messageRepository.save(savedMessage);
        Message retrievedMessage = messageRepository.findById(updatedMessage.getId()).orElseThrow();

        // Then
        assertEquals(MessageStatus.READ, retrievedMessage.getStatus());
        assertNotNull(retrievedMessage.getReadAt());
        assertNotNull(retrievedMessage.getDeliveredAt());
    }

    @Test
    @DisplayName("Should edit message content")
    void shouldEditMessageContent() {
        // Given
        Message savedMessage = messageRepository.save(testMessage);
        String newContent = "Edited message content";

        // When
        savedMessage.editContent(newContent);
        Message updatedMessage = messageRepository.save(savedMessage);
        Message retrievedMessage = messageRepository.findById(updatedMessage.getId()).orElseThrow();

        // Then
        assertEquals(newContent, retrievedMessage.getContent());
        assertTrue(retrievedMessage.isEdited());
        assertNotNull(retrievedMessage.getEditedAt());
    }

    @Test
    @DisplayName("Should add attachment to message")
    void shouldAddAttachmentToMessage() {
        // Given
        Message savedMessage = messageRepository.save(testMessage);
        Attachment attachment = new Attachment(
            new AttachmentId(UUID.randomUUID()),
            testMessage.getId(),
            "image.jpg",
            "image/jpeg",
            1024L,
            "https://example.com/image.jpg"
        );

        // When
        savedMessage.addAttachment(attachment);
        Message updatedMessage = messageRepository.save(savedMessage);
        Message retrievedMessage = messageRepository.findById(updatedMessage.getId()).orElseThrow();

        // Then
        assertEquals(1, retrievedMessage.getAttachments().size());
        assertEquals(attachment.getFileName(), retrievedMessage.getAttachments().get(0).getFileName());
    }

    @Test
    @DisplayName("Should delete message")
    void shouldDeleteMessage() {
        // Given
        Message savedMessage = messageRepository.save(testMessage);
        assertTrue(messageRepository.findById(savedMessage.getId()).isPresent());

        // When
        messageRepository.delete(savedMessage.getId());

        // Then
        assertFalse(messageRepository.findById(savedMessage.getId()).isPresent());
    }

    @Test
    @DisplayName("Should handle different message types")
    void shouldHandleDifferentMessageTypes() {
        // Given
        Message textMessage = new Message(
            new MessageId(UUID.randomUUID()),
            conversationId,
            senderId,
            "Text message",
            MessageType.TEXT
        );
        Message imageMessage = new Message(
            new MessageId(UUID.randomUUID()),
            conversationId,
            senderId,
            "image.jpg",
            MessageType.IMAGE
        );
        Message videoMessage = new Message(
            new MessageId(UUID.randomUUID()),
            conversationId,
            senderId,
            "video.mp4",
            MessageType.VIDEO
        );

        // When
        messageRepository.save(textMessage);
        messageRepository.save(imageMessage);
        messageRepository.save(videoMessage);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Message> messages = messageRepository.findByConversationId(conversationId, pageable);

        // Then
        assertEquals(3, messages.getContent().size());
        assertTrue(messages.getContent().stream().anyMatch(m -> m.getType() == MessageType.TEXT));
        assertTrue(messages.getContent().stream().anyMatch(m -> m.getType() == MessageType.IMAGE));
        assertTrue(messages.getContent().stream().anyMatch(m -> m.getType() == MessageType.VIDEO));
    }

    @Test
    @DisplayName("Should maintain message order by sentAt timestamp")
    void shouldMaintainMessageOrderBySentAtTimestamp() {
        // Given - Create messages with slight delay to ensure different timestamps
        for (int i = 0; i < 3; i++) {
            Message message = new Message(
                new MessageId(UUID.randomUUID()),
                conversationId,
                senderId,
                "Message " + i,
                MessageType.TEXT
            );
            messageRepository.save(message);
            try {
                Thread.sleep(10); // Small delay to ensure different timestamps
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // When - Query with descending order
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("sentAt").descending());
        Page<Message> messages = messageRepository.findByConversationId(conversationId, pageable);

        // Then - Most recent message should be first
        List<Message> messageList = messages.getContent();
        assertEquals(3, messageList.size());
        assertTrue(messageList.get(0).getSentAt().isAfter(messageList.get(1).getSentAt()) ||
                  messageList.get(0).getSentAt().equals(messageList.get(1).getSentAt()));
    }

    @Test
    @DisplayName("Should return empty page when no messages in conversation")
    void shouldReturnEmptyPageWhenNoMessagesInConversation() {
        // Given
        ConversationId emptyConversationId = new ConversationId(UUID.randomUUID());

        // When
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Message> messages = messageRepository.findByConversationId(emptyConversationId, pageable);

        // Then
        assertTrue(messages.getContent().isEmpty());
        assertEquals(0, messages.getTotalElements());
    }

    @Test
    @DisplayName("Should isolate messages by conversation")
    void shouldIsolateMessagesByConversation() {
        // Given
        ConversationId conversation1 = new ConversationId(UUID.randomUUID());
        ConversationId conversation2 = new ConversationId(UUID.randomUUID());

        Message message1 = new Message(
            new MessageId(UUID.randomUUID()),
            conversation1,
            senderId,
            "Message for conversation 1",
            MessageType.TEXT
        );
        Message message2 = new Message(
            new MessageId(UUID.randomUUID()),
            conversation2,
            senderId,
            "Message for conversation 2",
            MessageType.TEXT
        );

        messageRepository.save(message1);
        messageRepository.save(message2);

        // When
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Message> conv1Messages = messageRepository.findByConversationId(conversation1, pageable);
        Page<Message> conv2Messages = messageRepository.findByConversationId(conversation2, pageable);

        // Then
        assertEquals(1, conv1Messages.getContent().size());
        assertEquals(1, conv2Messages.getContent().size());
        assertEquals(conversation1, conv1Messages.getContent().get(0).getConversationId());
        assertEquals(conversation2, conv2Messages.getContent().get(0).getConversationId());
    }

    @Test
    @DisplayName("Should persist all message fields correctly")
    void shouldPersistAllMessageFieldsCorrectly() {
        // Given
        testMessage.markAsDelivered();
        testMessage.markAsRead();
        testMessage.editContent("Edited content");
        testMessage.addAttachment(new Attachment(
            new AttachmentId(UUID.randomUUID()),
            testMessage.getId(),
            "file.pdf",
            "application/pdf",
            2048L,
            "https://example.com/file.pdf"
        ));

        // When
        Message savedMessage = messageRepository.save(testMessage);
        Message retrievedMessage = messageRepository.findById(savedMessage.getId()).orElseThrow();

        // Then
        assertEquals(testMessage.getId(), retrievedMessage.getId());
        assertEquals(testMessage.getConversationId(), retrievedMessage.getConversationId());
        assertEquals(testMessage.getSenderId(), retrievedMessage.getSenderId());
        assertEquals(testMessage.getContent(), retrievedMessage.getContent());
        assertEquals(testMessage.getType(), retrievedMessage.getType());
        assertEquals(testMessage.getStatus(), retrievedMessage.getStatus());
        assertEquals(testMessage.getAttachments().size(), retrievedMessage.getAttachments().size());
        assertEquals(testMessage.isEdited(), retrievedMessage.isEdited());
        assertNotNull(retrievedMessage.getSentAt());
        assertNotNull(retrievedMessage.getDeliveredAt());
        assertNotNull(retrievedMessage.getReadAt());
        assertNotNull(retrievedMessage.getEditedAt());
    }
}

