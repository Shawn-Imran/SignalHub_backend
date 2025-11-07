package com.realtime.communication.chat.domain.model;

import com.realtime.communication.auth.domain.model.UserId;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Message entity representing a chat message
 */
@Getter
public class Message {
    private final MessageId id;
    private final ConversationId conversationId;
    private final UserId senderId;
    private String content;
    private final MessageType type;
    private MessageStatus status;
    private final List<Attachment> attachments;
    private final Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private boolean edited;
    private Instant editedAt;

    // Constructor for creating a new message
    public Message(MessageId id, ConversationId conversationId, UserId senderId,
                   String content, MessageType type) {
        this.id = Objects.requireNonNull(id, "Message ID cannot be null");
        this.conversationId = Objects.requireNonNull(conversationId, "Conversation ID cannot be null");
        this.senderId = Objects.requireNonNull(senderId, "Sender ID cannot be null");
        this.content = Objects.requireNonNull(content, "Content cannot be null");
        this.type = Objects.requireNonNull(type, "Message type cannot be null");
        this.status = MessageStatus.SENT;
        this.attachments = new ArrayList<>();
        this.sentAt = Instant.now();
        this.edited = false;
    }

    // Full constructor for reconstitution from persistence
    public Message(MessageId id, ConversationId conversationId, UserId senderId,
                   String content, MessageType type, MessageStatus status,
                   List<Attachment> attachments, Instant sentAt, Instant deliveredAt,
                   Instant readAt, boolean edited, Instant editedAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.type = type;
        this.status = status;
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
        this.sentAt = sentAt;
        this.deliveredAt = deliveredAt;
        this.readAt = readAt;
        this.edited = edited;
        this.editedAt = editedAt;
    }

    // Domain methods
    public void markAsDelivered() {
        if (this.status == MessageStatus.SENT) {
            this.status = MessageStatus.DELIVERED;
            this.deliveredAt = Instant.now();
        }
    }

    public void markAsRead() {
        if (this.status != MessageStatus.READ) {
            this.status = MessageStatus.READ;
            this.readAt = Instant.now();
            if (this.deliveredAt == null) {
                this.deliveredAt = this.readAt;
            }
        }
    }

    public void editContent(String newContent) {
        this.content = Objects.requireNonNull(newContent, "Content cannot be null");
        this.edited = true;
        this.editedAt = Instant.now();
    }

    public void addAttachment(Attachment attachment) {
        this.attachments.add(Objects.requireNonNull(attachment, "Attachment cannot be null"));
    }

    public List<Attachment> getAttachments() {
        return new ArrayList<>(attachments);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", conversationId=" + conversationId +
                ", senderId=" + senderId +
                ", type=" + type +
                ", status=" + status +
                ", sentAt=" + sentAt +
                '}';
    }
}
