package com.realtime.communication.chat.domain.event;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.domain.model.ConversationId;
import com.realtime.communication.chat.domain.model.MessageId;
import com.realtime.communication.chat.domain.model.MessageType;
import com.realtime.communication.shared.application.event.Event;
import lombok.Getter;

/**
 * Domain event emitted when a message is sent
 */
@Getter
public class MessageSentEvent extends Event {
    private final MessageId messageId;
    private final ConversationId conversationId;
    private final UserId senderId;
    private final String content;
    private final MessageType messageType;

    public MessageSentEvent(MessageId messageId, ConversationId conversationId,
                           UserId senderId, String content, MessageType messageType) {
        super();
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.messageType = messageType;
    }
}
