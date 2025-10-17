package com.realtime.communication.shared.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Redis-based cache manager for application-level caching.
 * Provides simple key-value caching with TTL support.
 */
@Component
public class RedisCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheManager.class);
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisCacheManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Store a value in cache with default TTL.
     */
    public void put(String key, Object value) {
        put(key, value, DEFAULT_TTL);
    }

    /**
     * Store a value in cache with custom TTL.
     */
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
            logger.debug("Cached value for key: {} with TTL: {}", key, ttl);
        } catch (Exception e) {
            logger.error("Failed to cache value for key: {}", key, e);
        }
    }

    /**
     * Retrieve a value from cache.
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            logger.error("Failed to retrieve cached value for key: {}", key, e);
            return null;
        }
    }

    /**
     * Remove a value from cache.
     */
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
            logger.debug("Evicted cache for key: {}", key);
        } catch (Exception e) {
            logger.error("Failed to evict cache for key: {}", key, e);
        }
    }

    /**
     * Remove all values matching a pattern.
     */
    public void evictPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.debug("Evicted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            logger.error("Failed to evict cache for pattern: {}", pattern, e);
        }
    }

    /**
     * Check if a key exists in cache.
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            logger.error("Failed to check existence for key: {}", key, e);
            return false;
        }
    }
}

