package com.realtime.communication.chat.domain.model;

/**
 * Enum representing the delivery status of a message
 */
public enum MessageStatus {
    SENT,       // Message sent from sender
    DELIVERED,  // Message delivered to recipient's device
    READ        // Message read by recipient
}

