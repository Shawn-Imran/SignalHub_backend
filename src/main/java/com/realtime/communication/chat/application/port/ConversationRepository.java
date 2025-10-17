package com.realtime.communication.chat.application.port;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.domain.model.Conversation;
import com.realtime.communication.chat.domain.model.ConversationId;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for Conversation repository
 */
public interface ConversationRepository {
    Conversation save(Conversation conversation);
    Optional<Conversation> findById(ConversationId conversationId);
    List<Conversation> findByParticipant(UserId userId);
    void delete(ConversationId conversationId);
}

