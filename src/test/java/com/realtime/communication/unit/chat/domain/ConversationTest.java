package com.realtime.communication.unit.chat.domain;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.domain.model.*;
import com.realtime.communication.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Conversation entity (Domain Layer)
 * TDD: These tests are written BEFORE implementation
 */
@DisplayName("Conversation Entity Tests")
class ConversationTest {

    @Test
    @DisplayName("Should create one-to-one conversation with two participants")
    void shouldCreateOneToOneConversation() {
        // Given
        ConversationId conversationId = new ConversationId(UUID.randomUUID());
        UserId user1 = new UserId(UUID.randomUUID());
        UserId user2 = new UserId(UUID.randomUUID());
        Set<UserId> participants = Set.of(user1, user2);

        // When
        Conversation conversation = new Conversation(conversationId, ConversationType.ONE_TO_ONE, participants);

        // Then
        assertNotNull(conversation);
        assertEquals(conversationId, conversation.getId());
        assertEquals(ConversationType.ONE_TO_ONE, conversation.getType());
        assertEquals(2, conversation.getParticipants().size());
        assertTrue(conversation.hasParticipant(user1));
        assertTrue(conversation.hasParticipant(user2));
        assertNotNull(conversation.getCreatedAt());
    }

    @Test
    @DisplayName("Should throw exception for one-to-one conversation without exactly 2 participants")
    void shouldThrowExceptionForInvalidOneToOneParticipantCount() {
        // Given
        ConversationId conversationId = new ConversationId(UUID.randomUUID());
        UserId user1 = new UserId(UUID.randomUUID());

        // When/Then - Only 1 participant
        assertThrows(ValidationException.class, 
            () -> new Conversation(conversationId, ConversationType.ONE_TO_ONE, Set.of(user1)));

        // When/Then - 3 participants
        UserId user2 = new UserId(UUID.randomUUID());
        UserId user3 = new UserId(UUID.randomUUID());
        assertThrows(ValidationException.class, 
            () -> new Conversation(conversationId, ConversationType.ONE_TO_ONE, Set.of(user1, user2, user3)));
    }

    @Test
    @DisplayName("Should create group conversation with multiple participants")
    void shouldCreateGroupConversation() {
        // Given
        ConversationId conversationId = new ConversationId(UUID.randomUUID());
        UserId user1 = new UserId(UUID.randomUUID());
        UserId user2 = new UserId(UUID.randomUUID());
        UserId user3 = new UserId(UUID.randomUUID());
        Set<UserId> participants = Set.of(user1, user2, user3);

        // When
        Conversation conversation = new Conversation(conversationId, ConversationType.GROUP, participants);

        // Then
        assertNotNull(conversation);
        assertEquals(ConversationType.GROUP, conversation.getType());
        assertEquals(3, conversation.getParticipants().size());
    }

    @Test
    @DisplayName("Should add participant to group conversation")
    void shouldAddParticipantToGroup() {
        // Given
        Conversation conversation = createGroupConversation();
        UserId newUser = new UserId(UUID.randomUUID());
        int initialSize = conversation.getParticipants().size();

        // When
        conversation.addParticipant(newUser);

        // Then
        assertEquals(initialSize + 1, conversation.getParticipants().size());
        assertTrue(conversation.hasParticipant(newUser));
    }

    @Test
    @DisplayName("Should not allow adding participant to one-to-one conversation")
    void shouldNotAddParticipantToOneToOne() {
        // Given
        Conversation conversation = createOneToOneConversation();
        UserId newUser = new UserId(UUID.randomUUID());

        // When/Then
        assertThrows(IllegalStateException.class, 
            () -> conversation.addParticipant(newUser));
    }

    @Test
    @DisplayName("Should remove participant from group conversation")
    void shouldRemoveParticipantFromGroup() {
        // Given
        UserId user1 = new UserId(UUID.randomUUID());
        UserId user2 = new UserId(UUID.randomUUID());
        UserId user3 = new UserId(UUID.randomUUID());
        Conversation conversation = new Conversation(
            new ConversationId(UUID.randomUUID()), 
            ConversationType.GROUP, 
            Set.of(user1, user2, user3)
        );

        // When
        conversation.removeParticipant(user3);

        // Then
        assertEquals(2, conversation.getParticipants().size());
        assertFalse(conversation.hasParticipant(user3));
    }

    @Test
    @DisplayName("Should update last message timestamp")
    void shouldUpdateLastMessageTimestamp() {
        // Given
        Conversation conversation = createOneToOneConversation();
        assertNull(conversation.getLastMessageAt());

        // When
        conversation.updateLastMessage();

        // Then
        assertNotNull(conversation.getLastMessageAt());
    }

    @Test
    @DisplayName("Should check if user is participant")
    void shouldCheckIfUserIsParticipant() {
        // Given
        UserId user1 = new UserId(UUID.randomUUID());
        UserId user2 = new UserId(UUID.randomUUID());
        UserId nonParticipant = new UserId(UUID.randomUUID());
        Conversation conversation = new Conversation(
            new ConversationId(UUID.randomUUID()), 
            ConversationType.ONE_TO_ONE, 
            Set.of(user1, user2)
        );

        // Then
        assertTrue(conversation.hasParticipant(user1));
        assertTrue(conversation.hasParticipant(user2));
        assertFalse(conversation.hasParticipant(nonParticipant));
    }

    private Conversation createOneToOneConversation() {
        UserId user1 = new UserId(UUID.randomUUID());
        UserId user2 = new UserId(UUID.randomUUID());
        return new Conversation(
            new ConversationId(UUID.randomUUID()), 
            ConversationType.ONE_TO_ONE, 
            Set.of(user1, user2)
        );
    }

    private Conversation createGroupConversation() {
        UserId user1 = new UserId(UUID.randomUUID());
        UserId user2 = new UserId(UUID.randomUUID());
        UserId user3 = new UserId(UUID.randomUUID());
        return new Conversation(
            new ConversationId(UUID.randomUUID()), 
            ConversationType.GROUP, 
            Set.of(user1, user2, user3)
        );
    }
}
package com.realtime.communication.unit.chat.domain;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.domain.model.*;
import com.realtime.communication.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Message entity (Domain Layer)
 * TDD: These tests are written BEFORE implementation
 */
@DisplayName("Message Entity Tests")
class MessageTest {

    @Test
    @DisplayName("Should create valid message with all required fields")
    void shouldCreateValidMessage() {
        // Given
        MessageId messageId = new MessageId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());
        UserId senderId = new UserId(UUID.randomUUID());
        String content = "Hello, World!";

        // When
        Message message = new Message(messageId, conversationId, senderId, content, MessageType.TEXT);

        // Then
        assertNotNull(message);
        assertEquals(messageId, message.getId());
        assertEquals(conversationId, message.getConversationId());
        assertEquals(senderId, message.getSenderId());
        assertEquals(content, message.getContent());
        assertEquals(MessageType.TEXT, message.getType());
        assertEquals(MessageStatus.SENT, message.getStatus());
        assertNotNull(message.getSentAt());
        assertNull(message.getDeliveredAt());
        assertNull(message.getReadAt());
    }

    @Test
    @DisplayName("Should throw exception when content is empty")
    void shouldThrowExceptionForEmptyContent() {
        // Given
        MessageId messageId = new MessageId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());
        UserId senderId = new UserId(UUID.randomUUID());

        // When/Then
        assertThrows(ValidationException.class, 
            () -> new Message(messageId, conversationId, senderId, "", MessageType.TEXT));
        assertThrows(ValidationException.class, 
            () -> new Message(messageId, conversationId, senderId, null, MessageType.TEXT));
    }

    @Test
    @DisplayName("Should mark message as delivered")
    void shouldMarkAsDelivered() {
        // Given
        Message message = createValidMessage();
        Instant beforeDelivery = Instant.now();

        // When
        message.markAsDelivered();

        // Then
        assertEquals(MessageStatus.DELIVERED, message.getStatus());
        assertNotNull(message.getDeliveredAt());
        assertTrue(message.getDeliveredAt().isAfter(beforeDelivery) || 
                   message.getDeliveredAt().equals(beforeDelivery));
    }

    @Test
    @DisplayName("Should mark message as read")
    void shouldMarkAsRead() {
        // Given
        Message message = createValidMessage();
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
    @DisplayName("Should not allow marking as read before delivered")
    void shouldNotMarkAsReadBeforeDelivered() {
        // Given
        Message message = createValidMessage();

        // When/Then
        assertThrows(IllegalStateException.class, message::markAsRead);
    }

    @Test
    @DisplayName("Should update message content for editable messages")
    void shouldUpdateContent() {
        // Given
        Message message = createValidMessage();
        String newContent = "Updated content";

        // When
        message.updateContent(newContent);

        // Then
        assertEquals(newContent, message.getContent());
        assertTrue(message.isEdited());
        assertNotNull(message.getEditedAt());
    }

    @Test
    @DisplayName("Should delete message")
    void shouldDeleteMessage() {
        // Given
        Message message = createValidMessage();

        // When
        message.delete();

        // Then
        assertTrue(message.isDeleted());
        assertNotNull(message.getDeletedAt());
    }

    private Message createValidMessage() {
        return new Message(
            new MessageId(UUID.randomUUID()),
            new ConversationId(UUID.randomUUID()),
            new UserId(UUID.randomUUID()),
            "Test message content",
            MessageType.TEXT
        );
    }
}

