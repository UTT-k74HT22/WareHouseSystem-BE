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
     * Lưu key-value với TTL vào Redis
     *
     * @param key      Redis key
     * @param value    Redis value
     * @param ttl      Thời gian sống (timeout)
     * @param timeUnit Đơn vị thời gian (SECONDS, MINUTES, HOURS...)
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
     * Lấy giá trị từ Redis theo key
     *
     * @param key Redis key
     * @return Object hoặc null nếu không tồn tại
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
     * Lấy và convert về kiểu mong muốn
     *
     * @param key   Redis key
     * @param clazz Kiểu dữ liệu mong muốn
     * @return Optional<T>, empty nếu không có dữ liệu
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
     * Kiểm tra key có tồn tại trong Redis hay không
     *
     * @param key Redis key
     * @return true nếu tồn tại, false nếu không
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
     * Xóa key khỏi Redis
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
