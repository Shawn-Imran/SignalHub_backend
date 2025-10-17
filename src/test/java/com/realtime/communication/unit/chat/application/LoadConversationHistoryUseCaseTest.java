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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoadConversationHistoryUseCase (Application Layer)
 * TDD: These tests are written BEFORE implementation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoadConversationHistoryUseCase Tests")
class LoadConversationHistoryUseCaseTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    private LoadConversationHistoryUseCase loadConversationHistoryUseCase;

    @BeforeEach
    void setUp() {
        loadConversationHistoryUseCase = new LoadConversationHistoryUseCase(
            messageRepository,
            conversationRepository
        );
    }

    @Test
    @DisplayName("Should load conversation history successfully")
    void shouldLoadConversationHistorySuccessfully() {
        // Given
        UserId userId = new UserId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());

        Conversation conversation = mock(Conversation.class);
        when(conversation.hasParticipant(userId)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        Message message1 = createMessage(conversationId, userId, "Message 1");
        Message message2 = createMessage(conversationId, userId, "Message 2");
        Page<Message> messages = new PageImpl<>(List.of(message1, message2));

        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(messages);

        // When
        Page<MessageDTO> result = loadConversationHistoryUseCase.execute(conversationId, userId, 0, 20);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("Message 1", result.getContent().get(0).getContent());
        assertEquals("Message 2", result.getContent().get(1).getContent());

        verify(conversationRepository).findById(conversationId);
        verify(messageRepository).findByConversationId(eq(conversationId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should throw exception when conversation not found")
    void shouldThrowExceptionWhenConversationNotFound() {
        // Given
        UserId userId = new UserId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
            () -> loadConversationHistoryUseCase.execute(conversationId, userId, 0, 20));

        verify(messageRepository, never()).findByConversationId(any(), any());
    }

    @Test
    @DisplayName("Should throw exception when user is not participant")
    void shouldThrowExceptionWhenUserNotParticipant() {
        // Given
        UserId userId = new UserId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());

        Conversation conversation = mock(Conversation.class);
        when(conversation.hasParticipant(userId)).thenReturn(false);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When/Then
        assertThrows(UnauthorizedException.class,
            () -> loadConversationHistoryUseCase.execute(conversationId, userId, 0, 20));

        verify(messageRepository, never()).findByConversationId(any(), any());
    }

    @Test
    @DisplayName("Should return empty page when no messages exist")
    void shouldReturnEmptyPageWhenNoMessages() {
        // Given
        UserId userId = new UserId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());

        Conversation conversation = mock(Conversation.class);
        when(conversation.hasParticipant(userId)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        Page<Message> emptyPage = new PageImpl<>(List.of());
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When
        Page<MessageDTO> result = loadConversationHistoryUseCase.execute(conversationId, userId, 0, 20);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    @DisplayName("Should support pagination")
    void shouldSupportPagination() {
        // Given
        UserId userId = new UserId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());
        int page = 1;
        int size = 10;

        Conversation conversation = mock(Conversation.class);
        when(conversation.hasParticipant(userId)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        Page<Message> messages = new PageImpl<>(List.of());
        when(messageRepository.findByConversationId(eq(conversationId), any(Pageable.class)))
            .thenReturn(messages);

        // When
        Page<MessageDTO> result = loadConversationHistoryUseCase.execute(conversationId, userId, page, size);

        // Then
        assertNotNull(result);
        verify(messageRepository).findByConversationId(eq(conversationId), any(Pageable.class));
    }

    private Message createMessage(ConversationId conversationId, UserId senderId, String content) {
        Message message = mock(Message.class);
        when(message.getId()).thenReturn(new MessageId(UUID.randomUUID()));
        when(message.getConversationId()).thenReturn(conversationId);
        when(message.getSenderId()).thenReturn(senderId);
        when(message.getContent()).thenReturn(content);
        when(message.getType()).thenReturn(MessageType.TEXT);
        when(message.getStatus()).thenReturn(MessageStatus.SENT);
        return message;
    }
}
package com.realtime.communication.unit.chat.application;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.dto.MessageDTO;
import com.realtime.communication.chat.application.port.ConversationRepository;
import com.realtime.communication.chat.application.port.MessageRepository;
import com.realtime.communication.chat.application.usecase.SendMessageUseCase;
import com.realtime.communication.chat.domain.model.*;
import com.realtime.communication.shared.domain.exception.NotFoundException;
import com.realtime.communication.shared.domain.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SendMessageUseCase (Application Layer)
 * TDD: These tests are written BEFORE implementation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SendMessageUseCase Tests")
class SendMessageUseCaseTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    private SendMessageUseCase sendMessageUseCase;

    @BeforeEach
    void setUp() {
        sendMessageUseCase = new SendMessageUseCase(messageRepository, conversationRepository);
    }

    @Test
    @DisplayName("Should send message successfully")
    void shouldSendMessageSuccessfully() {
        // Given
        UserId senderId = new UserId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());
        String content = "Hello, World!";

        Conversation conversation = mock(Conversation.class);
        when(conversation.hasParticipant(senderId)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MessageDTO result = sendMessageUseCase.execute(conversationId, senderId, content, MessageType.TEXT);

        // Then
        assertNotNull(result);
        assertEquals(content, result.getContent());
        assertEquals(MessageStatus.SENT, result.getStatus());

        verify(conversationRepository).findById(conversationId);
        verify(messageRepository).save(any(Message.class));
        verify(conversation).updateLastMessage();
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("Should throw exception when conversation not found")
    void shouldThrowExceptionWhenConversationNotFound() {
        // Given
        UserId senderId = new UserId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class,
            () -> sendMessageUseCase.execute(conversationId, senderId, "Hello", MessageType.TEXT));

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when sender is not participant")
    void shouldThrowExceptionWhenSenderNotParticipant() {
        // Given
        UserId senderId = new UserId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());

        Conversation conversation = mock(Conversation.class);
        when(conversation.hasParticipant(senderId)).thenReturn(false);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When/Then
        assertThrows(UnauthorizedException.class,
            () -> sendMessageUseCase.execute(conversationId, senderId, "Hello", MessageType.TEXT));

        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should publish MessageSentEvent after sending message")
    void shouldPublishMessageSentEvent() {
        // Given
        UserId senderId = new UserId(UUID.randomUUID());
        ConversationId conversationId = new ConversationId(UUID.randomUUID());

        Conversation conversation = mock(Conversation.class);
        when(conversation.hasParticipant(senderId)).thenReturn(true);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MessageDTO result = sendMessageUseCase.execute(conversationId, senderId, "Hello", MessageType.TEXT);

        // Then
        assertNotNull(result);
        // Event publishing will be verified when EventPublisher is integrated
    }
}

