package com.realtime.communication.chat.application.port;

import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.Message;
import com.realtime.communication.chat.domain.model.MessageId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Port interface for Message repository
 */
public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(MessageId messageId);
    Page<Message> findByConversationId(ConversationId conversationId, Pageable pageable);
    void delete(MessageId messageId);
}

