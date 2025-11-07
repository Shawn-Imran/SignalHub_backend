package com.realtime.communication.chat.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * Value object representing a file attachment in a message
 */
@Getter
@AllArgsConstructor
public class Attachment {
    private final AttachmentId id;
    private final MessageId messageId;
    private final String fileName;
    private final String fileType;
    private final Long fileSize;
    private final String storageUrl;
    private final Instant uploadedAt;

    public Attachment(AttachmentId id, MessageId messageId, String fileName,
                      String fileType, Long fileSize, String storageUrl) {
        this.id = id;
        this.messageId = messageId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.storageUrl = storageUrl;
        this.uploadedAt = Instant.now();
    }
}
