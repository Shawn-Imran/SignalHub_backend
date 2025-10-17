package com.realtime.communication.chat.application.usecase;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.dto.MessageDTO;
import com.realtime.communication.chat.application.port.ConversationRepository;
import com.realtime.communication.chat.application.port.MessageRepository;
import com.realtime.communication.chat.domain.model.Conversation;
import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.Message;
import com.realtime.communication.shared.domain.exception.NotFoundException;
import com.realtime.communication.shared.domain.exception.UnauthorizedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for loading conversation history
 */
@Service
@Transactional(readOnly = true)
public class LoadConversationHistoryUseCase {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public LoadConversationHistoryUseCase(MessageRepository messageRepository,
                                         ConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    public Page<MessageDTO> execute(ConversationId conversationId, UserId userId, int page, int size) {
        // Find conversation
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new NotFoundException("Conversation not found"));

        // Verify user is participant
        if (!conversation.hasParticipant(userId)) {
            throw new UnauthorizedException("User is not a participant in this conversation");
        }

        // Load messages with pagination (sorted by sent time descending)
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        Page<Message> messages = messageRepository.findByConversationId(conversationId, pageable);

        // Convert to DTOs
        return messages.map(this::toDTO);
    }

    private MessageDTO toDTO(Message message) {
        return new MessageDTO(
            message.getId().value(),
            message.getConversationId().value(),
            message.getSenderId().value(),
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

