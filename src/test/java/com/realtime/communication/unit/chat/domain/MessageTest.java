package com.realtime.communication.unit.chat.domain;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Message entity
 * Following TDD: These tests verify Message domain logic
 */
@DisplayName("Message Entity Tests")
class MessageTest {

    private MessageId messageId;
    private ConversationId conversationId;
    private UserId senderId;
    private String content;
    private MessageType messageType;

    @BeforeEach
    void setUp() {
        messageId = new MessageId(UUID.randomUUID());
        conversationId = new ConversationId(UUID.randomUUID());
        senderId = new UserId(UUID.randomUUID());
        content = "Hello, this is a test message!";
        messageType = MessageType.TEXT;
    }

    @Nested
    @DisplayName("Message Creation Tests")
    class MessageCreationTests {

        @Test
        @DisplayName("Should create message with valid parameters")
        void shouldCreateMessageWithValidParameters() {
            // When
            Message message = new Message(messageId, conversationId, senderId, content, messageType);

            // Then
            assertNotNull(message);
            assertEquals(messageId, message.getId());
            assertEquals(conversationId, message.getConversationId());
            assertEquals(senderId, message.getSenderId());
            assertEquals(content, message.getContent());
            assertEquals(messageType, message.getType());
            assertEquals(MessageStatus.SENT, message.getStatus());
            assertNotNull(message.getAttachments());
            assertTrue(message.getAttachments().isEmpty());
            assertNotNull(message.getSentAt());
            assertNull(message.getDeliveredAt());
            assertNull(message.getReadAt());
            assertFalse(message.isEdited());
            assertNull(message.getEditedAt());
        }

        @Test
        @DisplayName("Should throw exception when messageId is null")
        void shouldThrowExceptionWhenMessageIdIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                new Message(null, conversationId, senderId, content, messageType)
            );
        }

        @Test
        @DisplayName("Should throw exception when conversationId is null")
        void shouldThrowExceptionWhenConversationIdIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                new Message(messageId, null, senderId, content, messageType)
            );
        }

        @Test
        @DisplayName("Should throw exception when senderId is null")
        void shouldThrowExceptionWhenSenderIdIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                new Message(messageId, conversationId, null, content, messageType)
            );
        }

        @Test
        @DisplayName("Should throw exception when content is null")
        void shouldThrowExceptionWhenContentIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                new Message(messageId, conversationId, senderId, null, messageType)
            );
        }

        @Test
        @DisplayName("Should throw exception when messageType is null")
        void shouldThrowExceptionWhenMessageTypeIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                new Message(messageId, conversationId, senderId, content, null)
            );
        }
    }

    @Nested
    @DisplayName("Message Status Transition Tests")
    class MessageStatusTransitionTests {

        @Test
        @DisplayName("Should mark message as delivered from SENT status")
        void shouldMarkMessageAsDeliveredFromSentStatus() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);
            assertEquals(MessageStatus.SENT, message.getStatus());
            Instant beforeDelivery = Instant.now();

            // When
            message.markAsDelivered();

            // Then
            assertEquals(MessageStatus.DELIVERED, message.getStatus());
            assertNotNull(message.getDeliveredAt());
            assertTrue(message.getDeliveredAt().isAfter(beforeDelivery) ||
                      message.getDeliveredAt().equals(beforeDelivery));
            assertNull(message.getReadAt());
        }

        @Test
        @DisplayName("Should not mark as delivered when already delivered")
        void shouldNotMarkAsDeliveredWhenAlreadyDelivered() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);
            message.markAsDelivered();
            Instant firstDeliveredAt = message.getDeliveredAt();

            // When
            message.markAsDelivered();

            // Then
            assertEquals(MessageStatus.DELIVERED, message.getStatus());
            assertEquals(firstDeliveredAt, message.getDeliveredAt());
        }

        @Test
        @DisplayName("Should mark message as read and update readAt")
        void shouldMarkMessageAsReadAndUpdateReadAt() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);
            message.markAsDelivered();
            Instant beforeRead = Instant.now();

            // When
            message.markAsRead();

            // Then
            assertEquals(MessageStatus.READ, message.getStatus());
            assertNotNull(message.getReadAt());
            assertTrue(message.getReadAt().isAfter(beforeRead) ||
                      message.getReadAt().equals(beforeRead));
        }

        @Test
        @DisplayName("Should mark as read from SENT status and set both deliveredAt and readAt")
        void shouldMarkAsReadFromSentStatusAndSetBothTimestamps() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);
            assertEquals(MessageStatus.SENT, message.getStatus());
            assertNull(message.getDeliveredAt());

            // When
            message.markAsRead();

            // Then
            assertEquals(MessageStatus.READ, message.getStatus());
            assertNotNull(message.getDeliveredAt());
            assertNotNull(message.getReadAt());
            assertEquals(message.getReadAt(), message.getDeliveredAt());
        }

        @Test
        @DisplayName("Should not update readAt when already read")
        void shouldNotUpdateReadAtWhenAlreadyRead() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);
            message.markAsRead();
            Instant firstReadAt = message.getReadAt();

            // When
            message.markAsRead();

            // Then
            assertEquals(MessageStatus.READ, message.getStatus());
            assertEquals(firstReadAt, message.getReadAt());
        }
    }

    @Nested
    @DisplayName("Message Editing Tests")
    class MessageEditingTests {

        @Test
        @DisplayName("Should edit message content")
        void shouldEditMessageContent() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);
            String newContent = "This is the edited content";
            assertFalse(message.isEdited());
            Instant beforeEdit = Instant.now();

            // When
            message.editContent(newContent);

            // Then
            assertEquals(newContent, message.getContent());
            assertTrue(message.isEdited());
            assertNotNull(message.getEditedAt());
            assertTrue(message.getEditedAt().isAfter(beforeEdit) ||
                      message.getEditedAt().equals(beforeEdit));
        }

        @Test
        @DisplayName("Should throw exception when editing with null content")
        void shouldThrowExceptionWhenEditingWithNullContent() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);

            // When & Then
            assertThrows(NullPointerException.class, () ->
                message.editContent(null)
            );
        }

        @Test
        @DisplayName("Should update editedAt on subsequent edits")
        void shouldUpdateEditedAtOnSubsequentEdits() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);
            message.editContent("First edit");
            Instant firstEditTime = message.getEditedAt();

            // When
            try {
                Thread.sleep(10); // Small delay to ensure different timestamp
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            message.editContent("Second edit");

            // Then
            assertEquals("Second edit", message.getContent());
            assertTrue(message.isEdited());
            assertTrue(message.getEditedAt().isAfter(firstEditTime) ||
                      message.getEditedAt().equals(firstEditTime));
        }
    }

    @Nested
    @DisplayName("Attachment Tests")
    class AttachmentTests {

        @Test
        @DisplayName("Should add attachment to message")
        void shouldAddAttachmentToMessage() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);
            Attachment attachment = new Attachment(
                new AttachmentId(UUID.randomUUID()),
                messageId,
                "image.jpg",
                "image/jpeg",
                1024L,
                "https://example.com/image.jpg"
            );

            // When
            message.addAttachment(attachment);

            // Then
            assertEquals(1, message.getAttachments().size());
            assertTrue(message.getAttachments().contains(attachment));
        }

        @Test
        @DisplayName("Should add multiple attachments")
        void shouldAddMultipleAttachments() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);
            Attachment attachment1 = new Attachment(
                new AttachmentId(UUID.randomUUID()),
                messageId,
                "image1.jpg",
                "image/jpeg",
                1024L,
                "https://example.com/image1.jpg"
            );
            Attachment attachment2 = new Attachment(
                new AttachmentId(UUID.randomUUID()),
                messageId,
                "document.pdf",
                "application/pdf",
                2048L,
                "https://example.com/document.pdf"
            );

            // When
            message.addAttachment(attachment1);
            message.addAttachment(attachment2);

            // Then
            assertEquals(2, message.getAttachments().size());
            assertTrue(message.getAttachments().contains(attachment1));
            assertTrue(message.getAttachments().contains(attachment2));
        }

        @Test
        @DisplayName("Should throw exception when adding null attachment")
        void shouldThrowExceptionWhenAddingNullAttachment() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);

            // When & Then
            assertThrows(NullPointerException.class, () ->
                message.addAttachment(null)
            );
        }

        @Test
        @DisplayName("Should return immutable copy of attachments")
        void shouldReturnImmutableCopyOfAttachments() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);
            Attachment attachment = new Attachment(
                new AttachmentId(UUID.randomUUID()),
                messageId,
                "image.jpg",
                "image/jpeg",
                1024L,
                "https://example.com/image.jpg"
            );
            message.addAttachment(attachment);

            // When
            List<Attachment> attachments = message.getAttachments();
            int originalSize = attachments.size();

            // Try to modify the returned list (should not affect internal state)
            attachments.add(new Attachment(
                new AttachmentId(UUID.randomUUID()),
                messageId,
                "another.jpg",
                "image/jpeg",
                1024L,
                "https://example.com/another.jpg"
            ));

            // Then
            assertEquals(originalSize, message.getAttachments().size());
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same messageId")
        void shouldBeEqualWhenSameMessageId() {
            // Given
            Message message1 = new Message(messageId, conversationId, senderId, content, messageType);
            Message message2 = new Message(messageId,
                new ConversationId(UUID.randomUUID()),
                new UserId(UUID.randomUUID()),
                "Different content",
                messageType);

            // Then
            assertEquals(message1, message2);
            assertEquals(message1.hashCode(), message2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when different messageId")
        void shouldNotBeEqualWhenDifferentMessageId() {
            // Given
            Message message1 = new Message(messageId, conversationId, senderId, content, messageType);
            Message message2 = new Message(
                new MessageId(UUID.randomUUID()),
                conversationId,
                senderId,
                content,
                messageType);

            // Then
            assertNotEquals(message1, message2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);

            // Then
            assertEquals(message, message);
            assertEquals(message.hashCode(), message.hashCode());
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);

            // Then
            assertNotEquals(null, message);
        }
    }

    @Nested
    @DisplayName("Full Constructor Tests")
    class FullConstructorTests {

        @Test
        @DisplayName("Should reconstitute message from persistence with full constructor")
        void shouldReconstituteMessageFromPersistence() {
            // Given
            MessageStatus status = MessageStatus.READ;
            List<Attachment> attachments = new ArrayList<>();
            attachments.add(new Attachment(
                new AttachmentId(UUID.randomUUID()),
                messageId,
                "file.pdf",
                "application/pdf",
                2048L,
                "https://example.com/file.pdf"
            ));
            Instant sentAt = Instant.now().minusSeconds(300);
            Instant deliveredAt = Instant.now().minusSeconds(200);
            Instant readAt = Instant.now().minusSeconds(100);
            boolean edited = true;
            Instant editedAt = Instant.now().minusSeconds(50);

            // When
            Message message = new Message(messageId, conversationId, senderId, content,
                messageType, status, attachments, sentAt, deliveredAt, readAt, edited, editedAt);

            // Then
            assertEquals(messageId, message.getId());
            assertEquals(conversationId, message.getConversationId());
            assertEquals(senderId, message.getSenderId());
            assertEquals(content, message.getContent());
            assertEquals(messageType, message.getType());
            assertEquals(status, message.getStatus());
            assertEquals(1, message.getAttachments().size());
            assertEquals(sentAt, message.getSentAt());
            assertEquals(deliveredAt, message.getDeliveredAt());
            assertEquals(readAt, message.getReadAt());
            assertEquals(edited, message.isEdited());
            assertEquals(editedAt, message.getEditedAt());
        }

        @Test
        @DisplayName("Should handle null attachments in full constructor")
        void shouldHandleNullAttachmentsInFullConstructor() {
            // When
            Message message = new Message(messageId, conversationId, senderId, content,
                messageType, MessageStatus.SENT, null, Instant.now(), null, null, false, null);

            // Then
            assertNotNull(message.getAttachments());
            assertTrue(message.getAttachments().isEmpty());
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should contain key message information in toString")
        void shouldContainKeyMessageInformationInToString() {
            // Given
            Message message = new Message(messageId, conversationId, senderId, content, messageType);

            // When
            String toString = message.toString();

            // Then
            assertTrue(toString.contains("Message{"));
            assertTrue(toString.contains("id="));
            assertTrue(toString.contains("conversationId="));
            assertTrue(toString.contains("senderId="));
            assertTrue(toString.contains("type="));
            assertTrue(toString.contains("status="));
            assertTrue(toString.contains("sentAt="));
        }
    }
}

