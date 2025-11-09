package com.realtime.communication.unit.chat.domain;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.domain.model.Conversation;
import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.ConversationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Conversation entity
 * Following TDD: These tests verify Conversation domain logic
 */
@DisplayName("Conversation Entity Tests")
class ConversationTest {

    private ConversationId conversationId;
    private UserId user1Id;
    private UserId user2Id;
    private UserId user3Id;

    @BeforeEach
    void setUp() {
        conversationId = new ConversationId(UUID.randomUUID());
        user1Id = new UserId(UUID.randomUUID());
        user2Id = new UserId(UUID.randomUUID());
        user3Id = new UserId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("Conversation Creation Tests")
    class ConversationCreationTests {

        @Test
        @DisplayName("Should create one-to-one conversation with two participants")
        void shouldCreateOneToOneConversationWithTwoParticipants() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);

            // When
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );

            // Then
            assertNotNull(conversation);
            assertEquals(conversationId, conversation.getId());
            assertEquals(ConversationType.ONE_TO_ONE, conversation.getType());
            assertEquals(2, conversation.getParticipants().size());
            assertTrue(conversation.hasParticipant(user1Id));
            assertTrue(conversation.hasParticipant(user2Id));
            assertNotNull(conversation.getCreatedAt());
            assertNull(conversation.getLastMessageAt());
        }

        @Test
        @DisplayName("Should create group conversation with multiple participants")
        void shouldCreateGroupConversationWithMultipleParticipants() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            participants.add(user3Id);

            // When
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.GROUP,
                participants
            );

            // Then
            assertEquals(ConversationType.GROUP, conversation.getType());
            assertEquals(3, conversation.getParticipants().size());
            assertTrue(conversation.hasParticipant(user1Id));
            assertTrue(conversation.hasParticipant(user2Id));
            assertTrue(conversation.hasParticipant(user3Id));
        }

        @Test
        @DisplayName("Should throw exception when conversationId is null")
        void shouldThrowExceptionWhenConversationIdIsNull() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);

            // When & Then
            assertThrows(NullPointerException.class, () ->
                new Conversation(null, ConversationType.ONE_TO_ONE, participants)
            );
        }

        @Test
        @DisplayName("Should throw exception when conversation type is null")
        void shouldThrowExceptionWhenConversationTypeIsNull() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);

            // When & Then
            assertThrows(NullPointerException.class, () ->
                new Conversation(conversationId, null, participants)
            );
        }

        @Test
        @DisplayName("Should throw exception when participants is null")
        void shouldThrowExceptionWhenParticipantsIsNull() {
            // When & Then
            assertThrows(NullPointerException.class, () ->
                new Conversation(conversationId, ConversationType.ONE_TO_ONE, null)
            );
        }

        @Test
        @DisplayName("Should throw exception when participants is empty")
        void shouldThrowExceptionWhenParticipantsIsEmpty() {
            // Given
            Set<UserId> participants = new HashSet<>();

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Conversation(conversationId, ConversationType.ONE_TO_ONE, participants)
            );
            assertTrue(exception.getMessage().contains("at least one participant"));
        }

        @Test
        @DisplayName("Should throw exception when one-to-one conversation has less than 2 participants")
        void shouldThrowExceptionWhenOneToOneConversationHasLessThan2Participants() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Conversation(conversationId, ConversationType.ONE_TO_ONE, participants)
            );
            assertTrue(exception.getMessage().contains("exactly 2 participants"));
        }

        @Test
        @DisplayName("Should throw exception when one-to-one conversation has more than 2 participants")
        void shouldThrowExceptionWhenOneToOneConversationHasMoreThan2Participants() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            participants.add(user3Id);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new Conversation(conversationId, ConversationType.ONE_TO_ONE, participants)
            );
            assertTrue(exception.getMessage().contains("exactly 2 participants"));
        }
    }

    @Nested
    @DisplayName("Last Message Time Tests")
    class LastMessageTimeTests {

        @Test
        @DisplayName("Should update last message time")
        void shouldUpdateLastMessageTime() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );
            assertNull(conversation.getLastMessageAt());
            Instant beforeUpdate = Instant.now();

            // When
            conversation.updateLastMessageTime();

            // Then
            assertNotNull(conversation.getLastMessageAt());
            assertTrue(conversation.getLastMessageAt().isAfter(beforeUpdate) ||
                      conversation.getLastMessageAt().equals(beforeUpdate));
        }

        @Test
        @DisplayName("Should update last message time on subsequent updates")
        void shouldUpdateLastMessageTimeOnSubsequentUpdates() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );
            conversation.updateLastMessageTime();
            Instant firstUpdate = conversation.getLastMessageAt();

            // When
            try {
                Thread.sleep(10); // Small delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            conversation.updateLastMessageTime();

            // Then
            assertTrue(conversation.getLastMessageAt().isAfter(firstUpdate) ||
                      conversation.getLastMessageAt().equals(firstUpdate));
        }
    }

    @Nested
    @DisplayName("Participant Management Tests - Group Conversations")
    class GroupParticipantManagementTests {

        @Test
        @DisplayName("Should add participant to group conversation")
        void shouldAddParticipantToGroupConversation() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.GROUP,
                participants
            );
            UserId newUser = new UserId(UUID.randomUUID());

            // When
            conversation.addParticipant(newUser);

            // Then
            assertEquals(3, conversation.getParticipants().size());
            assertTrue(conversation.hasParticipant(newUser));
        }

        @Test
        @DisplayName("Should not add duplicate participant to group conversation")
        void shouldNotAddDuplicateParticipantToGroupConversation() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.GROUP,
                participants
            );

            // When
            conversation.addParticipant(user1Id);

            // Then
            assertEquals(2, conversation.getParticipants().size());
        }

        @Test
        @DisplayName("Should throw exception when adding null participant")
        void shouldThrowExceptionWhenAddingNullParticipant() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.GROUP,
                participants
            );

            // When & Then
            assertThrows(NullPointerException.class, () ->
                conversation.addParticipant(null)
            );
        }

        @Test
        @DisplayName("Should remove participant from group conversation")
        void shouldRemoveParticipantFromGroupConversation() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            participants.add(user3Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.GROUP,
                participants
            );

            // When
            conversation.removeParticipant(user3Id);

            // Then
            assertEquals(2, conversation.getParticipants().size());
            assertFalse(conversation.hasParticipant(user3Id));
            assertTrue(conversation.hasParticipant(user1Id));
            assertTrue(conversation.hasParticipant(user2Id));
        }

        @Test
        @DisplayName("Should not throw when removing non-existent participant")
        void shouldNotThrowWhenRemovingNonExistentParticipant() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.GROUP,
                participants
            );
            UserId nonExistentUser = new UserId(UUID.randomUUID());

            // When & Then
            assertDoesNotThrow(() -> conversation.removeParticipant(nonExistentUser));
            assertEquals(2, conversation.getParticipants().size());
        }
    }

    @Nested
    @DisplayName("Participant Management Tests - One-to-One Conversations")
    class OneToOneParticipantManagementTests {

        @Test
        @DisplayName("Should throw exception when adding participant to one-to-one conversation")
        void shouldThrowExceptionWhenAddingParticipantToOneToOneConversation() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );
            UserId newUser = new UserId(UUID.randomUUID());

            // When & Then
            IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                conversation.addParticipant(newUser)
            );
            assertTrue(exception.getMessage().contains("Cannot add participants to one-to-one conversation"));
        }

        @Test
        @DisplayName("Should throw exception when removing participant from one-to-one conversation")
        void shouldThrowExceptionWhenRemovingParticipantFromOneToOneConversation() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );

            // When & Then
            IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                conversation.removeParticipant(user1Id)
            );
            assertTrue(exception.getMessage().contains("Cannot remove participants from one-to-one conversation"));
        }
    }

    @Nested
    @DisplayName("Participant Query Tests")
    class ParticipantQueryTests {

        @Test
        @DisplayName("Should correctly identify if user is participant")
        void shouldCorrectlyIdentifyIfUserIsParticipant() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );

            // Then
            assertTrue(conversation.hasParticipant(user1Id));
            assertTrue(conversation.hasParticipant(user2Id));
            assertFalse(conversation.hasParticipant(user3Id));
        }

        @Test
        @DisplayName("Should return unmodifiable set of participants")
        void shouldReturnUnmodifiableSetOfParticipants() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );

            // When
            Set<UserId> returnedParticipants = conversation.getParticipants();

            // Then
            assertThrows(UnsupportedOperationException.class, () ->
                returnedParticipants.add(user3Id)
            );
        }
    }

    @Nested
    @DisplayName("Equality and HashCode Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal when same conversationId")
        void shouldBeEqualWhenSameConversationId() {
            // Given
            Set<UserId> participants1 = new HashSet<>();
            participants1.add(user1Id);
            participants1.add(user2Id);
            Conversation conversation1 = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants1
            );

            Set<UserId> participants2 = new HashSet<>();
            participants2.add(user1Id);
            participants2.add(user3Id);
            Conversation conversation2 = new Conversation(
                conversationId,
                ConversationType.GROUP,
                participants2
            );

            // Then
            assertEquals(conversation1, conversation2);
            assertEquals(conversation1.hashCode(), conversation2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when different conversationId")
        void shouldNotBeEqualWhenDifferentConversationId() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation1 = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );
            Conversation conversation2 = new Conversation(
                new ConversationId(UUID.randomUUID()),
                ConversationType.ONE_TO_ONE,
                participants
            );

            // Then
            assertNotEquals(conversation1, conversation2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );

            // Then
            assertEquals(conversation, conversation);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );

            // Then
            assertNotEquals(null, conversation);
        }
    }

    @Nested
    @DisplayName("Full Constructor Tests")
    class FullConstructorTests {

        @Test
        @DisplayName("Should reconstitute conversation from persistence with full constructor")
        void shouldReconstituteConversationFromPersistence() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            participants.add(user3Id);
            Instant createdAt = Instant.now().minusSeconds(3600);
            Instant lastMessageAt = Instant.now().minusSeconds(300);

            // When
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.GROUP,
                participants,
                createdAt,
                lastMessageAt
            );

            // Then
            assertEquals(conversationId, conversation.getId());
            assertEquals(ConversationType.GROUP, conversation.getType());
            assertEquals(3, conversation.getParticipants().size());
            assertEquals(createdAt, conversation.getCreatedAt());
            assertEquals(lastMessageAt, conversation.getLastMessageAt());
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should contain key conversation information in toString")
        void shouldContainKeyConversationInformationInToString() {
            // Given
            Set<UserId> participants = new HashSet<>();
            participants.add(user1Id);
            participants.add(user2Id);
            Conversation conversation = new Conversation(
                conversationId,
                ConversationType.ONE_TO_ONE,
                participants
            );

            // When
            String toString = conversation.toString();

            // Then
            assertTrue(toString.contains("Conversation{"));
            assertTrue(toString.contains("id="));
            assertTrue(toString.contains("type="));
            assertTrue(toString.contains("participantCount="));
            assertTrue(toString.contains("createdAt="));
        }
    }
}

