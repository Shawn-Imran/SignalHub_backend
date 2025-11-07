package com.realtime.communication.chat.application.usecase;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.dto.ConversationDTO;
import com.realtime.communication.chat.application.port.ConversationRepository;
import com.realtime.communication.chat.domain.model.Conversation;
import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.ConversationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use case for creating a conversation
 */
@Service
public class CreateConversationUseCase {
    private final ConversationRepository conversationRepository;

    public CreateConversationUseCase(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public ConversationDTO execute(ConversationType type, Set<UserId> participants) {
        // Create conversation
        ConversationId conversationId = ConversationId.generate();
        Conversation conversation = new Conversation(conversationId, type, participants);

        // Save conversation
        Conversation savedConversation = conversationRepository.save(conversation);

        // Convert to DTO
        return toDTO(savedConversation);
    }

    private ConversationDTO toDTO(Conversation conversation) {
        return new ConversationDTO(
            conversation.getId().getValue(),
            conversation.getType(),
            conversation.getParticipants().stream()
                .map(UserId::getValue)
                .collect(Collectors.toSet()),
            conversation.getCreatedAt(),
            conversation.getLastMessageAt()
        );
    }
}
