package org.demo.whs.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisServiceImpl implements RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Store a key-value pair in Redis with a TTL.
     *
     * @param key      Redis key
     * @param value    Redis value
     * @param ttl      timeout value
     * @param timeUnit timeout unit
     */
    @Override
    public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl, timeUnit);
            log.debug("Set Redis key: {} with TTL: {} {}", key, ttl, timeUnit);
        } catch (Exception e) {
            log.error("Error setting Redis key: {}", key, e);
            throw new RuntimeException("Failed to set Redis key", e);
        }
    }

    /**
     * Read a value from Redis by key.
     *
     * @param key Redis key
     * @return value or null when the key does not exist
     */
    @Override
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Get Redis key: {} -> {}", key, value != null ? "found" : "not found");
            return value;
        } catch (Exception e) {
            log.error("Error getting Redis key: {}", key, e);
            throw new RuntimeException("Failed to get Redis key", e);
        }
    }

    /**
     * Read a value from Redis and convert it to the requested type.
     *
     * @param key   Redis key
     * @param clazz expected Java type
     * @return Optional.empty when no value exists or conversion fails
     */
    @Override
    public <T> Optional<T> get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }

            if (clazz.isInstance(value)) {
                return Optional.of((T) value);
            }

            if (value instanceof String && !clazz.equals(String.class)) {
                T convertedValue = objectMapper.readValue((String) value, clazz);
                return Optional.of(convertedValue);
            }

            return Optional.of((T) value);
        } catch (Exception e) {
            log.error("Error getting and converting Redis key: {} to class: {}", key, clazz.getSimpleName(), e);
            return Optional.empty();
        }
    }

    /**
     * Check whether a Redis key exists.
     *
     * @param key Redis key
     * @return true if the key exists, otherwise false
     */
    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            boolean result = exists != null && exists;
            log.debug("Check Redis key exists: {} -> {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Error checking Redis key exists: {}", key, e);
            return false;
        }
    }

    /**
     * Delete a key from Redis.
     *
     * @param key Redis key
     */
    @Override
    public void delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Delete Redis key: {} -> {}", key, deleted != null && deleted);
        } catch (Exception e) {
            log.error("Error deleting Redis key: {}", key, e);
            throw new RuntimeException("Failed to delete Redis key", e);
        }
    }

    /**
     * 🔥 Safe get với TypeReference + Optional
     * - Fix: avoid silent failure, log deserialize error
     * - Fix: đồng bộ return Optional<T>
     * - Caller có thể phân biệt cache miss vs deserialize error qua log
     */
    @Override
    public <T> Optional<T> getOptional(String key, TypeReference<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Cache MISS for key {}", key);
                return Optional.empty();
            }

            T result;
            if (value instanceof String json) {
                result = objectMapper.readValue(json, type);
            } else {
                result = objectMapper.convertValue(value, type);
            }

            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.error("DESERIALIZE ERROR for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 🔥 Save với TTL linh hoạt
     * - Fix: tránh hardcode TTL 1h, cho phép caller set TTL
     */
    @Override
    public void saveWithTTL(String key, Object value, long ttl, TimeUnit unit) {
        if (key == null || value == null) {
            log.warn("[REDIS SAVE] key or value is null -> skip");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl, unit);

            log.debug("[REDIS SET] key={} ttl={} {}", key, ttl, unit);
        } catch (Exception e) {
            log.error("[REDIS SET ERROR] key={} message={}", key, e.getMessage(), e);
            throw new RuntimeException("Failed to save Redis key", e);
        }
    }

    @Override
    public Set<String> getAllKeys(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            log.warn("[REDIS KEYS] pattern is null/blank");
            return Collections.emptySet();
        }

        try {
            Set<String> keys = redisTemplate.keys(pattern);

            int size = (keys != null) ? keys.size() : 0;
            log.debug("[REDIS KEYS] pattern={} -> {} keys", pattern, size);

            return keys != null ? keys : Collections.emptySet();
        } catch (Exception e) {
            log.error("[REDIS KEYS ERROR] pattern={} message={}", pattern, e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    @Override
    public void delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            log.debug("[REDIS DELETE] empty keys -> skip");
            return;
        }

        try {
            Long deleted = redisTemplate.delete(keys);

            log.debug("[REDIS DELETE] requested={} deleted={}", keys.size(), deleted);
        } catch (Exception e) {
            log.error("[REDIS DELETE ERROR] keys={} message={}", keys, e.getMessage(), e);
        }
    }
}
