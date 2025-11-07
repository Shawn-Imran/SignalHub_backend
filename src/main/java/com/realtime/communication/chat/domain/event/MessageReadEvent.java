package com.realtime.communication.chat.domain.event;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.MessageId;
import com.realtime.communication.shared.application.event.Event;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event emitted when a message is read by recipient
 */
@Getter
public class MessageReadEvent extends Event {
    private final MessageId messageId;
    private final ConversationId conversationId;
    private final UserId readerId;
    private final Instant readAt;

    public MessageReadEvent(MessageId messageId, ConversationId conversationId,
                           UserId readerId, Instant readAt) {
        super();
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.readerId = readerId;
        this.readAt = readAt;
    }
}
