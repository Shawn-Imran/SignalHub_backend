package com.realtime.communication.chat.adapter.out.messaging;

import com.realtime.communication.auth.domain.model.UserId;
import com.realtime.communication.chat.application.port.PresenceGateway;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Redis implementation of PresenceGateway
 */
@Component
public class RedisPresenceAdapter implements PresenceGateway {

    private static final String ONLINE_USERS_KEY = "presence:online";
    private static final String TYPING_KEY_PREFIX = "presence:typing:";
    private static final Duration TYPING_TIMEOUT = Duration.ofSeconds(5);

    private final RedisTemplate<String, String> redisTemplate;

    public RedisPresenceAdapter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void setOnline(UserId userId) {
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.getValue().toString());
    }

    @Override
    public void setOffline(UserId userId) {
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.getValue().toString());
    }

    @Override
    public boolean isOnline(UserId userId) {
        Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId.getValue().toString());
        return Boolean.TRUE.equals(isMember);
    }

    @Override
    public Set<UserId> getOnlineUsers() {
        Set<String> members = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        if (members == null) {
            return Set.of();
        }
        return members.stream()
            .map(id -> new UserId(UUID.fromString(id)))
            .collect(Collectors.toSet());
    }

    @Override
    public void setTyping(UserId userId, UUID conversationId) {
        String key = TYPING_KEY_PREFIX + conversationId + ":" + userId.getValue();
        redisTemplate.opsForValue().set(key, "typing", TYPING_TIMEOUT);
    }

    @Override
    public void stopTyping(UserId userId, UUID conversationId) {
        String key = TYPING_KEY_PREFIX + conversationId + ":" + userId.getValue();
        redisTemplate.delete(key);
    }

    @Override
    public boolean isTyping(UserId userId, UUID conversationId) {
        String key = TYPING_KEY_PREFIX + conversationId + ":" + userId.getValue();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}

