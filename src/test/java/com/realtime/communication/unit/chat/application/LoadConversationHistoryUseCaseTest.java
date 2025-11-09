package com.realtime.communication.unit.chat.application;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.dto.MessageDTO;
import com.realtime.communication.chat.application.port.ConversationRepository;
import com.realtime.communication.chat.application.port.MessageRepository;
import com.realtime.communication.chat.application.usecase.LoadConversationHistoryUseCase;
import com.realtime.communication.chat.domain.model.*;
import com.realtime.communication.shared.domain.exception.NotFoundException;
import com.realtime.communication.shared.domain.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoadConversationHistoryUseCase
 * Following TDD: These tests verify conversation history loading logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoadConversationHistoryUseCase Tests")
class LoadConversationHistoryUseCaseTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    private LoadConversationHistoryUseCase loadConversationHistoryUseCase;

    private ConversationId conversationId;
    private UserId user1Id;
    private UserId user2Id;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        loadConversationHistoryUseCase = new LoadConversationHistoryUseCase(
            messageRepository,
            conversationRepository
        );

        conversationId = new ConversationId(UUID.randomUUID());
        user1Id = new UserId(UUID.randomUUID());
        user2Id = new UserId(UUID.randomUUID());

        Set<UserId> participants = new HashSet<>();
        participants.add(user1Id);
        participants.add(user2Id);

        conversation = new Conversation(conversationId, ConversationType.ONE_TO_ONE, participants);
    }

    @Test
    @DisplayName("Should successfully load conversation history")
    void shouldSuccessfullyLoadConversationHistory() {
        // Given
        int page = 0;
        int size = 20;
        List<Message> messages = createSampleMessages(3);
        Page<Message> messagePage = new PageImpl<>(messages);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(messagePage);

        // When
        Page<MessageDTO> result = loadConversationHistoryUseCase.execute(conversationId, user1Id, page, size);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        verify(conversationRepository).findById(conversationId);
        verify(messageRepository).findByConversationId(eq(conversationId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return messages sorted by sentAt descending")
    void shouldReturnMessagesSortedBySentAtDescending() {
        // Given
        int page = 0;
        int size = 20;
        List<Message> messages = createSampleMessages(3);
        Page<Message> messagePage = new PageImpl<>(messages);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(messagePage);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // When
        loadConversationHistoryUseCase.execute(conversationId, user1Id, page, size);

        // Then
        verify(messageRepository).findByConversationId(eq(conversationId), pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();

        assertEquals(page, capturedPageable.getPageNumber());
        assertEquals(size, capturedPageable.getPageSize());
        assertTrue(capturedPageable.getSort().toString().contains("sentAt"));
        assertTrue(capturedPageable.getSort().toString().contains("DESC"));
    }

    @Test
    @DisplayName("Should throw NotFoundException when conversation does not exist")
    void shouldThrowNotFoundExceptionWhenConversationDoesNotExist() {
        // Given
        int page = 0;
        int size = 20;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            loadConversationHistoryUseCase.execute(conversationId, user1Id, page, size)
        );

        assertTrue(exception.getMessage().contains("Conversation not found"));
        verify(conversationRepository).findById(conversationId);
        verify(messageRepository, never()).findByConversationId(any(), any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not a participant")
    void shouldThrowUnauthorizedExceptionWhenUserIsNotParticipant() {
        // Given
        UserId unauthorizedUserId = new UserId(UUID.randomUUID());
        int page = 0;
        int size = 20;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            loadConversationHistoryUseCase.execute(conversationId, unauthorizedUserId, page, size)
        );

        assertTrue(exception.getMessage().contains("not a participant"));
        verify(conversationRepository).findById(conversationId);
        verify(messageRepository, never()).findByConversationId(any(), any());
    }

    @Test
    @DisplayName("Should allow both participants to load history")
    void shouldAllowBothParticipantsToLoadHistory() {
        // Given
        int page = 0;
        int size = 20;
        List<Message> messages = createSampleMessages(3);
        Page<Message> messagePage = new PageImpl<>(messages);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(messagePage);

        // When - User 1 loads history
        Page<MessageDTO> result1 = loadConversationHistoryUseCase.execute(conversationId, user1Id, page, size);

        // When - User 2 loads history
        Page<MessageDTO> result2 = loadConversationHistoryUseCase.execute(conversationId, user2Id, page, size);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(3, result1.getContent().size());
        assertEquals(3, result2.getContent().size());
        verify(messageRepository, times(2)).findByConversationId(eq(conversationId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty page when no messages exist")
    void shouldReturnEmptyPageWhenNoMessagesExist() {
        // Given
        int page = 0;
        int size = 20;
        Page<Message> emptyPage = new PageImpl<>(Collections.emptyList());

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When
        Page<MessageDTO> result = loadConversationHistoryUseCase.execute(conversationId, user1Id, page, size);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Should support pagination with different page sizes")
    void shouldSupportPaginationWithDifferentPageSizes() {
        // Given
        int page = 1;
        int size = 10;
        List<Message> messages = createSampleMessages(10);
        Page<Message> messagePage = new PageImpl<>(messages);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(messagePage);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        // When
        loadConversationHistoryUseCase.execute(conversationId, user1Id, page, size);

        // Then
        verify(messageRepository).findByConversationId(eq(conversationId), pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();

        assertEquals(page, capturedPageable.getPageNumber());
        assertEquals(size, capturedPageable.getPageSize());
    }

    @Test
    @DisplayName("Should convert messages to DTOs correctly")
    void shouldConvertMessagesToDtosCorrectly() {
        // Given
        int page = 0;
        int size = 20;
        List<Message> messages = createSampleMessages(1);
        Message message = messages.get(0);
        Page<Message> messagePage = new PageImpl<>(messages);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(messagePage);

        // When
        Page<MessageDTO> result = loadConversationHistoryUseCase.execute(conversationId, user1Id, page, size);

        // Then
        MessageDTO dto = result.getContent().get(0);
        assertEquals(message.getId().getValue(), dto.id());
        assertEquals(message.getConversationId().getValue(), dto.conversationId());
        assertEquals(message.getSenderId().getValue(), dto.senderId());
        assertEquals(message.getContent(), dto.content());
        assertEquals(message.getType(), dto.type());
        assertEquals(message.getStatus(), dto.status());
        assertEquals(message.getSentAt(), dto.sentAt());
        assertEquals(message.getDeliveredAt(), dto.deliveredAt());
        assertEquals(message.getReadAt(), dto.readAt());
        assertEquals(message.isEdited(), dto.edited());
        assertEquals(message.getEditedAt(), dto.editedAt());
    }

    @Test
    @DisplayName("Should handle pagination metadata correctly")
    void shouldHandlePaginationMetadataCorrectly() {
        // Given
        int page = 2;
        int size = 5;
        int totalElements = 23;
        List<Message> messages = createSampleMessages(5);
        Page<Message> messagePage = new PageImpl<>(messages,
            org.springframework.data.domain.PageRequest.of(page, size),
            totalElements);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(messagePage);

        // When
        Page<MessageDTO> result = loadConversationHistoryUseCase.execute(conversationId, user1Id, page, size);

        // Then
        assertEquals(page, result.getNumber());
        assertEquals(size, result.getSize());
        assertEquals(totalElements, result.getTotalElements());
        assertEquals(5, result.getTotalPages()); // 23 / 5 = 5 pages
        assertFalse(result.isFirst());
        assertFalse(result.isLast());
    }

    @Test
    @DisplayName("Should work with group conversations")
    void shouldWorkWithGroupConversations() {
        // Given
        UserId user3Id = new UserId(UUID.randomUUID());
        Set<UserId> groupParticipants = new HashSet<>();
        groupParticipants.add(user1Id);
        groupParticipants.add(user2Id);
        groupParticipants.add(user3Id);

        Conversation groupConversation = new Conversation(
            conversationId,
            ConversationType.GROUP,
            groupParticipants
        );

        int page = 0;
        int size = 20;
        List<Message> messages = createSampleMessages(5);
        Page<Message> messagePage = new PageImpl<>(messages);

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(groupConversation));
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(messagePage);

        // When
        Page<MessageDTO> result = loadConversationHistoryUseCase.execute(conversationId, user3Id, page, size);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
    }

    @Test
    @DisplayName("Should verify participant before loading messages")
    void shouldVerifyParticipantBeforeLoadingMessages() {
        // Given
        UserId unauthorizedUserId = new UserId(UUID.randomUUID());
        int page = 0;
        int size = 20;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When & Then
        assertThrows(UnauthorizedException.class, () ->
            loadConversationHistoryUseCase.execute(conversationId, unauthorizedUserId, page, size)
        );

        verify(conversationRepository).findById(conversationId);
        verify(messageRepository, never()).findByConversationId(any(), any());
    }

    // Helper method to create sample messages
    private List<Message> createSampleMessages(int count) {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MessageId messageId = new MessageId(UUID.randomUUID());
            UserId senderId = (i % 2 == 0) ? user1Id : user2Id;
            Message message = new Message(
                messageId,
                conversationId,
                senderId,
                "Message " + i,
                MessageType.TEXT
            );
            messages.add(message);
        }
        return messages;
    }
}

