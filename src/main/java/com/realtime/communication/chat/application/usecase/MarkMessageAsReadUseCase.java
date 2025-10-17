package com.realtime.communication.chat.application.usecase;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.port.MessageRepository;
import com.realtime.communication.chat.domain.model.Message;
import com.realtime.communication.chat.domain.model.MessageId;
import com.realtime.communication.shared.domain.exception.NotFoundException;
import com.realtime.communication.shared.domain.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for marking a message as read
 */
@Service
public class MarkMessageAsReadUseCase {
    private final MessageRepository messageRepository;

    public MarkMessageAsReadUseCase(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void execute(MessageId messageId, UserId userId) {
        // Find message
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new NotFoundException("Message not found"));

        // Verify user is not the sender (can't mark own message as read)
        if (message.getSenderId().equals(userId)) {
            throw new UnauthorizedException("Cannot mark own message as read");
        }

        // Mark as read
        message.markAsRead();
        messageRepository.save(message);
    }
}
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

