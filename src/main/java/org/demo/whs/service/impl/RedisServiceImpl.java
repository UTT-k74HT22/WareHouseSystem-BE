package org.demo.whs.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.RedisService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
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
}
