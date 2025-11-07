package com.realtime.communication.chat.application.usecase;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.dto.MessageDTO;
import com.realtime.communication.chat.application.port.ConversationRepository;
import com.realtime.communication.chat.application.port.MessageRepository;
import com.realtime.communication.chat.domain.model.*;
import com.realtime.communication.shared.domain.exception.NotFoundException;
import com.realtime.communication.shared.domain.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for sending a message
 */
@Service
public class SendMessageUseCase {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public SendMessageUseCase(MessageRepository messageRepository, ConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public MessageDTO execute(ConversationId conversationId, UserId senderId, String content, MessageType type) {
        // Find conversation
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new NotFoundException("Conversation not found"));

        // Verify sender is participant
        if (!conversation.hasParticipant(senderId)) {
            throw new UnauthorizedException("User is not a participant in this conversation");
        }

        // Create message
        MessageId messageId = MessageId.generate();
        Message message = new Message(messageId, conversationId, senderId, content, type);

        // Save message
        Message savedMessage = messageRepository.save(message);

        // Update conversation last message timestamp
        conversation.updateLastMessageTime();
        conversationRepository.save(conversation);

        // Convert to DTO
        return toDTO(savedMessage);
    }

    private MessageDTO toDTO(Message message) {
        return new MessageDTO(
            message.getId().getValue(),
            message.getConversationId().getValue(),
            message.getSenderId().getValue(),
            message.getContent(),
            message.getType(),
            message.getStatus(),
            message.getSentAt(),
            message.getDeliveredAt(),
            message.getReadAt(),
            message.isEdited(),
            message.getEditedAt()
        );
    }
}
