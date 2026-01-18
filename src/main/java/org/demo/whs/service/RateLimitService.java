package org.demo.whs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.utils.annotation.RateLimit;
import org.demo.whs.entity.dto.RateLimitDTO;
import org.demo.whs.entity.enums.RateLimitType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Service xử lý logic rate limiting sử dụng Redis
 * 
 * Implementation sử dụng Sliding Window Counter algorithm với Redis + Lua script
 * - Key format: rate_limit:{type}:{key}:{identifier}
 * - Value: số lượng request đã thực hiện
 * - TTL: duration của rate limit window
 * 
 * Improvements:
 * 1. Lua script returns TTL để tránh extra round-trip
 * 2. Hardened TTL logic - ALWAYS set expire, kể cả khi INCR fail
 * 3. Proper fallback mode per endpoint (fail-open vs fail-closed)
 * 4. Semantically correct retryAfter (chỉ khi 429)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    
    /**
     * Improved Lua script với hardened TTL và return TTL
     * 
     * KEYS[1]: Redis key
     * ARGV[1]: limit (int)
     * ARGV[2]: duration (TTL in seconds)
     * 
     * Return array: {remaining, ttl}
     * - remaining: số request còn lại (-1 nếu exceeded)
     * - ttl: số giây còn lại của window (luôn > 0)
     * 
     * Hardened TTL logic:
     * - Luôn EXPIRE sau INCR để tránh key tồn tại vĩnh viễn
     * - Nếu key mới (INCR = 1), set TTL = duration
     * - Nếu key cũ nhưng chưa có TTL (edge case), set TTL = duration
     */
    private static final String RATE_LIMIT_LUA_SCRIPT = 
        "local current = redis.call('GET', KEYS[1])\n" +
        "local limit = tonumber(ARGV[1])\n" +
        "local duration = tonumber(ARGV[2])\n" +
        "\n" +
        "-- Check if exceeded BEFORE increment\n" +
        "if current and tonumber(current) >= limit then\n" +
        "    local ttl = redis.call('TTL', KEYS[1])\n" +
        "    if ttl < 0 then\n" +
        "        -- Edge case: key exists but no TTL, fix it\n" +
        "        redis.call('EXPIRE', KEYS[1], duration)\n" +
        "        ttl = duration\n" +
        "    end\n" +
        "    return {-1, ttl}\n" +
        "end\n" +
        "\n" +
        "-- Increment counter\n" +
        "current = redis.call('INCR', KEYS[1])\n" +
        "\n" +
        "-- ALWAYS ensure TTL is set (hardened)\n" +
        "local ttl = redis.call('TTL', KEYS[1])\n" +
        "if ttl < 0 then\n" +
        "    redis.call('EXPIRE', KEYS[1], duration)\n" +
        "    ttl = duration\n" +
        "end\n" +
        "\n" +
        "local remaining = limit - current\n" +
        "return {remaining, ttl}";
    
    /**
     * Check xem request có được phép hay không dựa trên rate limit config
     * 
     * @param rateLimitAnnotation Annotation chứa config
     * @param identifier Identifier để phân biệt request (IP address hoặc username)
     * @param routeKey Route key (METHOD:pattern) để tránh cardinality explosion
     * @return RateLimitDTO chứa thông tin về rate limit state
     */
    public RateLimitDTO checkRateLimit(RateLimit rateLimitAnnotation, String identifier, String routeKey) {
        String key = buildKey(rateLimitAnnotation.type(), rateLimitAnnotation.key(), identifier, routeKey);
        int limit = rateLimitAnnotation.limit();
        int duration = rateLimitAnnotation.duration();
        boolean failClosed = rateLimitAnnotation.failClosed();
        
        log.debug("Checking rate limit for key: {}, limit: {}, duration: {}s, failClosed: {}", 
            key, limit, duration, failClosed);
        
        try {
            // Execute Lua script - returns {remaining, ttl}
            DefaultRedisScript<java.util.List> script = new DefaultRedisScript<>();
            script.setScriptText(RATE_LIMIT_LUA_SCRIPT);
            script.setResultType(java.util.List.class);
            
            java.util.List<Long> result = redisTemplate.execute(
                script,
                Arrays.asList(key),
                limit,
                duration
            );
            
            if (result == null || result.size() != 2) {
                log.error("Redis script returned invalid result for key: {}, result: {}", key, result);
                return handleRedisFallback(limit, duration, failClosed);
            }
            
            long remaining = result.get(0);
            long ttl = result.get(1);
            long now = System.currentTimeMillis() / 1000;
            long resetTime = now + ttl;
            
            // Rate limit exceeded
            if (remaining == -1) {
                log.warn("Rate limit exceeded for key: {}, retry after: {}s", key, ttl);
                return new RateLimitDTO(false, 0, limit, resetTime, ttl);
            }
            
            // Request allowed
            log.debug("Rate limit check passed for key: {}, remaining: {}, resetTime: {}", 
                key, remaining, resetTime);
            return new RateLimitDTO(true, (int) remaining, limit, resetTime, null);
            
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            return handleRedisFallback(limit, duration, failClosed);
        }
    }
    
    /**
     * Handle fallback khi Redis unavailable
     * 
     * @param limit Rate limit
     * @param duration Duration
     * @param failClosed Fail-closed mode (true = block, false = allow)
     * @return RateLimitDTO
     */
    private RateLimitDTO handleRedisFallback(int limit, int duration, boolean failClosed) {
        if (failClosed) {
            // Fail-closed: Block requests khi Redis down (cho sensitive endpoints)
            log.warn("Redis unavailable - FAIL CLOSED mode, blocking request");
            long now = System.currentTimeMillis() / 1000;
            return new RateLimitDTO(false, 0, limit, now + duration, (long) duration);
        } else {
            // Fail-open: Allow requests khi Redis down (default)
            log.warn("Redis unavailable - FAIL OPEN mode, allowing request");
            long now = System.currentTimeMillis() / 1000;
            return new RateLimitDTO(true, limit, limit, now + duration, null);
        }
    }
    
    /**
     * Build Redis key theo format: rate_limit:{type}:{key}:{routeKey}:{identifier}
     * 
     * Route key sử dụng METHOD + best matching pattern để tránh cardinality explosion
     * 
     * Examples:
     * - rate_limit:IP:login:POST:/api/v1/auth/login:192.168.1.1
     * - rate_limit:USER:api:GET:/api/v1/products/**:john.doe
     * - rate_limit:GLOBAL:system:*:*:global
     * 
     * @param type Rate limit type
     * @param key Rate limit key từ annotation
     * @param identifier IP address hoặc username
     * @param routeKey Route key (METHOD:pattern)
     * @return Redis key
     */
    private String buildKey(RateLimitType type, String key, String identifier, String routeKey) {
        return String.format("%s%s:%s:%s:%s", 
            RATE_LIMIT_KEY_PREFIX, type.name(), key, routeKey, identifier);
    }
    
    /**
     * Reset rate limit cho một key cụ thể (dùng cho testing hoặc admin override)
     * 
     * @param type Rate limit type
     * @param key Rate limit key
     * @param identifier Identifier
     * @param routeKey Route key
     */
    public void resetRateLimit(RateLimitType type, String key, String identifier, String routeKey) {
        String redisKey = buildKey(type, key, identifier, routeKey);
        redisTemplate.delete(redisKey);
        log.info("Reset rate limit for key: {}", redisKey);
    }
    
    /**
     * Get current rate limit info mà không increment counter
     * 
     * @param type Rate limit type
     * @param key Rate limit key
     * @param identifier Identifier
     * @param routeKey Route key
     * @param limit Limit config
     * @param duration Duration config
     * @return RateLimitDTO
     */
    public RateLimitDTO getRateLimitInfo(RateLimitType type, String key, String identifier, 
                                         String routeKey, int limit, int duration) {
        String redisKey = buildKey(type, key, identifier, routeKey);
        
        try {
            Object currentObj = redisTemplate.opsForValue().get(redisKey);
            int current = currentObj != null ? Integer.parseInt(currentObj.toString()) : 0;
            int remaining = Math.max(0, limit - current);
            
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            long ttlSeconds = ttl != null && ttl > 0 ? ttl : duration;
            long now = System.currentTimeMillis() / 1000;
            long resetTime = now + ttlSeconds;
            
            boolean allowed = current < limit;
            Long retryAfter = allowed ? null : ttlSeconds;
            
            return new RateLimitDTO(allowed, remaining, limit, resetTime, retryAfter);
        } catch (Exception e) {
            log.error("Error getting rate limit info for key: {}", redisKey, e);
            long now = System.currentTimeMillis() / 1000;
            return new RateLimitDTO(true, limit, limit, now + duration, null);
        }
    }
}
