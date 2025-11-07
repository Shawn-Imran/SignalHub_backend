package com.realtime.communication.chat.domain.service;

import com.realtime.communication.auth.domain.model.UserId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for managing typing indicators
 */
@Service
public class TypingIndicatorService {

    private static final Duration TYPING_TIMEOUT = Duration.ofSeconds(5);
    private final RedisTemplate<String, String> redisTemplate;

    public TypingIndicatorService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void startTyping(UserId userId, UUID conversationId) {
        String key = getTypingKey(conversationId, userId);
        redisTemplate.opsForValue().set(key, "true", TYPING_TIMEOUT);
    }

    public void stopTyping(UserId userId, UUID conversationId) {
        String key = getTypingKey(conversationId, userId);
        redisTemplate.delete(key);
    }

    private String getTypingKey(UUID conversationId, UserId userId) {
        return "typing:" + conversationId + ":" + userId.getValue();
    }
}

