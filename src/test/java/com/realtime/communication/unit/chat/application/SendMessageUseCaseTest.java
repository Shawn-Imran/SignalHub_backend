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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SendMessageUseCase
 * Following TDD: These tests verify message sending logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SendMessageUseCase Tests")
class SendMessageUseCaseTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    private SendMessageUseCase sendMessageUseCase;

    private ConversationId conversationId;
    private UserId senderId;
    private UserId recipientId;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        sendMessageUseCase = new SendMessageUseCase(messageRepository, conversationRepository);

        conversationId = new ConversationId(UUID.randomUUID());
        senderId = new UserId(UUID.randomUUID());
        recipientId = new UserId(UUID.randomUUID());

        Set<UserId> participants = new HashSet<>();
        participants.add(senderId);
        participants.add(recipientId);

        conversation = new Conversation(conversationId, ConversationType.ONE_TO_ONE, participants);
    }

    @Test
    @DisplayName("Should successfully send text message")
    void shouldSuccessfullySendTextMessage() {
        // Given
        String content = "Hello, this is a test message!";
        MessageType messageType = MessageType.TEXT;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        // When
        MessageDTO result = sendMessageUseCase.execute(conversationId, senderId, content, messageType);

        // Then
        assertNotNull(result);
        assertEquals(content, result.content());
        assertEquals(messageType, result.type());
        assertEquals(MessageStatus.SENT, result.status());
        assertNotNull(result.sentAt());

        verify(conversationRepository).findById(conversationId);
        verify(messageRepository).save(any(Message.class));
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("Should save message with correct conversation and sender")
    void shouldSaveMessageWithCorrectConversationAndSender() {
        // Given
        String content = "Test message";
        MessageType messageType = MessageType.TEXT;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        when(messageRepository.save(messageCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        sendMessageUseCase.execute(conversationId, senderId, content, messageType);

        // Then
        Message savedMessage = messageCaptor.getValue();
        assertEquals(conversationId, savedMessage.getConversationId());
        assertEquals(senderId, savedMessage.getSenderId());
        assertEquals(content, savedMessage.getContent());
        assertEquals(messageType, savedMessage.getType());
    }

    @Test
    @DisplayName("Should update conversation last message timestamp")
    void shouldUpdateConversationLastMessageTimestamp() {
        // Given
        String content = "Test message";
        MessageType messageType = MessageType.TEXT;
        Instant beforeSend = Instant.now();

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        when(conversationRepository.save(conversationCaptor.capture())).thenReturn(conversation);

        // When
        sendMessageUseCase.execute(conversationId, senderId, content, messageType);

        // Then
        Conversation savedConversation = conversationCaptor.getValue();
        assertNotNull(savedConversation.getLastMessageAt());
        assertTrue(savedConversation.getLastMessageAt().isAfter(beforeSend) ||
                  savedConversation.getLastMessageAt().equals(beforeSend));
    }

    @Test
    @DisplayName("Should throw NotFoundException when conversation does not exist")
    void shouldThrowNotFoundExceptionWhenConversationDoesNotExist() {
        // Given
        String content = "Test message";
        MessageType messageType = MessageType.TEXT;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When & Then
        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            sendMessageUseCase.execute(conversationId, senderId, content, messageType)
        );

        assertTrue(exception.getMessage().contains("Conversation not found"));
        verify(conversationRepository).findById(conversationId);
        verify(messageRepository, never()).save(any(Message.class));
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when sender is not a participant")
    void shouldThrowUnauthorizedExceptionWhenSenderIsNotParticipant() {
        // Given
        UserId nonParticipantId = new UserId(UUID.randomUUID());
        String content = "Test message";
        MessageType messageType = MessageType.TEXT;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When & Then
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            sendMessageUseCase.execute(conversationId, nonParticipantId, content, messageType)
        );

        assertTrue(exception.getMessage().contains("not a participant"));
        verify(conversationRepository).findById(conversationId);
        verify(messageRepository, never()).save(any(Message.class));
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("Should create message with SENT status")
    void shouldCreateMessageWithSentStatus() {
        // Given
        String content = "Test message";
        MessageType messageType = MessageType.TEXT;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        when(messageRepository.save(messageCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        sendMessageUseCase.execute(conversationId, senderId, content, messageType);

        // Then
        Message savedMessage = messageCaptor.getValue();
        assertEquals(MessageStatus.SENT, savedMessage.getStatus());
    }

    @Test
    @DisplayName("Should return DTO with all message details")
    void shouldReturnDtoWithAllMessageDetails() {
        // Given
        String content = "Test message";
        MessageType messageType = MessageType.TEXT;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        // When
        MessageDTO result = sendMessageUseCase.execute(conversationId, senderId, content, messageType);

        // Then
        assertNotNull(result.id());
        assertEquals(conversationId.getValue(), result.conversationId());
        assertEquals(senderId.getValue(), result.senderId());
        assertEquals(content, result.content());
        assertEquals(messageType, result.type());
        assertEquals(MessageStatus.SENT, result.status());
        assertNotNull(result.sentAt());
        assertNull(result.deliveredAt());
        assertNull(result.readAt());
        assertFalse(result.edited());
        assertNull(result.editedAt());
    }

    @Test
    @DisplayName("Should allow both participants to send messages")
    void shouldAllowBothParticipantsToSendMessages() {
        // Given
        String content = "Test message";
        MessageType messageType = MessageType.TEXT;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        // When - sender sends message
        MessageDTO result1 = sendMessageUseCase.execute(conversationId, senderId, content, messageType);

        // When - recipient sends message
        MessageDTO result2 = sendMessageUseCase.execute(conversationId, recipientId, "Reply", messageType);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(senderId.getValue(), result1.senderId());
        assertEquals(recipientId.getValue(), result2.senderId());
    }

    @Test
    @DisplayName("Should handle different message types")
    void shouldHandleDifferentMessageTypes() {
        // Given
        String content = "Test message";

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        // When & Then - TEXT message
        MessageDTO textResult = sendMessageUseCase.execute(conversationId, senderId, content, MessageType.TEXT);
        assertEquals(MessageType.TEXT, textResult.type());

        // When & Then - IMAGE message
        MessageDTO imageResult = sendMessageUseCase.execute(conversationId, senderId, "image.jpg", MessageType.IMAGE);
        assertEquals(MessageType.IMAGE, imageResult.type());

        // When & Then - VIDEO message
        MessageDTO videoResult = sendMessageUseCase.execute(conversationId, senderId, "video.mp4", MessageType.VIDEO);
        assertEquals(MessageType.VIDEO, videoResult.type());

        // When & Then - FILE message
        MessageDTO fileResult = sendMessageUseCase.execute(conversationId, senderId, "document.pdf", MessageType.FILE);
        assertEquals(MessageType.FILE, fileResult.type());
    }

    @Test
    @DisplayName("Should work in group conversations")
    void shouldWorkInGroupConversations() {
        // Given
        UserId user3 = new UserId(UUID.randomUUID());
        Set<UserId> groupParticipants = new HashSet<>();
        groupParticipants.add(senderId);
        groupParticipants.add(recipientId);
        groupParticipants.add(user3);

        Conversation groupConversation = new Conversation(
            conversationId,
            ConversationType.GROUP,
            groupParticipants
        );

        String content = "Group message";
        MessageType messageType = MessageType.TEXT;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(groupConversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(groupConversation);

        // When
        MessageDTO result = sendMessageUseCase.execute(conversationId, senderId, content, messageType);

        // Then
        assertNotNull(result);
        assertEquals(content, result.content());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("Should verify participant before creating message")
    void shouldVerifyParticipantBeforeCreatingMessage() {
        // Given
        UserId unauthorizedUser = new UserId(UUID.randomUUID());
        String content = "Test message";
        MessageType messageType = MessageType.TEXT;

        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When & Then
        assertThrows(UnauthorizedException.class, () ->
            sendMessageUseCase.execute(conversationId, unauthorizedUser, content, messageType)
        );

        // Message should not be created
        verify(messageRepository, never()).save(any(Message.class));
    }
}

