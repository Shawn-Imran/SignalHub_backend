package com.realtime.communication.chat.domain.event;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.MessageId;
import com.realtime.communication.shared.application.event.Event;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event emitted when a message is delivered to recipient
 */
@Getter
public class MessageDeliveredEvent extends Event {
    private final MessageId messageId;
    private final ConversationId conversationId;
    private final UserId recipientId;
    private final Instant deliveredAt;

    public MessageDeliveredEvent(MessageId messageId, ConversationId conversationId,
                                UserId recipientId, Instant deliveredAt) {
        super();
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.recipientId = recipientId;
        this.deliveredAt = deliveredAt;
    }
}
