package com.realtime.communication.chat.application.port;

import com.realtime.communication.auth.domain.model.UserId;

import java.util.Set;
import java.util.UUID;

/**
 * Port interface for Presence Gateway
 */
public interface PresenceGateway {
    void setOnline(UserId userId);
    void setOffline(UserId userId);
    boolean isOnline(UserId userId);
    Set<UserId> getOnlineUsers();
    void setTyping(UserId userId, UUID conversationId);
    void stopTyping(UserId userId, UUID conversationId);
    boolean isTyping(UserId userId, UUID conversationId);
}

