package org.demo.whs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.annotation.RateLimit;
import org.demo.whs.entity.dto.RateLimitDTO;
import org.demo.whs.entity.enums.RateLimitType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Service xử lý logic rate limiting sử dụng Redis
 * 
 * Implementation sử dụng Sliding Window Counter algorithm với Redis
 * - Key format: rate_limit:{type}:{key}:{identifier}
 * - Value: số lượng request đã thực hiện
 * - TTL: duration của rate limit window
 * 
 * Algorithm:
 * 1. Tạo key dựa trên type, key và identifier (IP/UserID)
 * 2. Increment counter trong Redis
 * 3. Set TTL nếu là lần đầu tiên
 * 4. Check nếu counter > limit thì reject request
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    
    /**
     * Lua script để thực hiện atomic increment và check limit
     * Script này đảm bảo rằng việc increment và check là atomic
     * 
     * KEYS[1]: Redis key
     * ARGV[1]: limit
     * ARGV[2]: duration (TTL in seconds)
     * 
     * Return:
     * - remaining count nếu allowed
     * - -1 nếu rate limit exceeded
     */
    private static final String RATE_LIMIT_LUA_SCRIPT = 
        "local current = redis.call('get', KEYS[1])\n" +
        "if current and tonumber(current) >= tonumber(ARGV[1]) then\n" +
        "    return -1\n" +
        "end\n" +
        "current = redis.call('incr', KEYS[1])\n" +
        "if tonumber(current) == 1 then\n" +
        "    redis.call('expire', KEYS[1], ARGV[2])\n" +
        "end\n" +
        "return tonumber(ARGV[1]) - tonumber(current)";
    
    /**
     * Check xem request có được phép hay không dựa trên rate limit config
     * 
     * @param rateLimitAnnotation Annotation chứa config
     * @param identifier Identifier để phân biệt request (IP address hoặc username)
     * @return RateLimitInfo chứa thông tin về rate limit state
     */
    public RateLimitDTO checkRateLimit(RateLimit rateLimitAnnotation, String identifier) {
        String key = buildKey(rateLimitAnnotation.type(), rateLimitAnnotation.key(), identifier);
        int limit = rateLimitAnnotation.limit();
        int duration = rateLimitAnnotation.duration();
        
        log.debug("Checking rate limit for key: {}, limit: {}, duration: {}s", key, limit, duration);
        
        try {
            // Execute Lua script
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(RATE_LIMIT_LUA_SCRIPT);
            script.setResultType(Long.class);
            
            Long remaining = redisTemplate.execute(
                script,
                Collections.singletonList(key),
                limit,
                duration
            );
            
            if (remaining == null) {
                log.error("Redis script returned null for key: {}", key);
                // Fallback: allow request nếu Redis có vấn đề
                return new RateLimitDTO(true, limit, 0, System.currentTimeMillis() / 1000 + duration, limit);
            }
            
            // Nếu remaining = -1 nghĩa là rate limit exceeded
            if (remaining == -1) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                long retryAfter = ttl != null && ttl > 0 ? ttl : duration;
                long resetTime = System.currentTimeMillis() / 1000 + retryAfter;
                
                log.warn("Rate limit exceeded for key: {}, retry after: {}s", key, retryAfter);
                
                return new RateLimitDTO(false, 0, retryAfter, resetTime, limit);
            }
            
            // Request được phép
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            long retryAfter = ttl != null && ttl > 0 ? ttl : duration;
            long resetTime = System.currentTimeMillis() / 1000 + retryAfter;
            
            log.debug("Rate limit check passed for key: {}, remaining: {}", key, remaining);
            
            return new RateLimitDTO(true, remaining.intValue(), retryAfter, resetTime, limit);
            
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            // Fallback: allow request nếu có lỗi để tránh block toàn bộ hệ thống
            return new RateLimitDTO(true, limit, 0, System.currentTimeMillis() / 1000 + duration, limit);
        }
    }
    
    /**
     * Build Redis key theo format: rate_limit:{type}:{key}:{identifier}
     * 
     * Examples:
     * - rate_limit:IP:login:192.168.1.1
     * - rate_limit:USER:api:john.doe
     * - rate_limit:GLOBAL:system:global
     * 
     * @param type Rate limit type
     * @param key Rate limit key từ annotation
     * @param identifier IP address hoặc username
     * @return Redis key
     */
    private String buildKey(RateLimitType type, String key, String identifier) {
        return String.format("%s%s:%s:%s", RATE_LIMIT_KEY_PREFIX, type.name(), key, identifier);
    }
    
    /**
     * Reset rate limit cho một key cụ thể (dùng cho testing hoặc admin override)
     * 
     * @param type Rate limit type
     * @param key Rate limit key
     * @param identifier Identifier
     */
    public void resetRateLimit(RateLimitType type, String key, String identifier) {
        String redisKey = buildKey(type, key, identifier);
        redisTemplate.delete(redisKey);
        log.info("Reset rate limit for key: {}", redisKey);
    }
    
    /**
     * Get current rate limit info mà không increment counter
     * 
     * @param type Rate limit type
     * @param key Rate limit key
     * @param identifier Identifier
     * @param limit Limit config
     * @param duration Duration config
     * @return RateLimitInfo
     */
    public RateLimitDTO getRateLimitInfo(RateLimitType type, String key, String identifier, int limit, int duration) {
        String redisKey = buildKey(type, key, identifier);
        
        try {
            Object currentObj = redisTemplate.opsForValue().get(redisKey);
            int current = currentObj != null ? Integer.parseInt(currentObj.toString()) : 0;
            int remaining = Math.max(0, limit - current);
            
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            long retryAfter = ttl != null && ttl > 0 ? ttl : duration;
            long resetTime = System.currentTimeMillis() / 1000 + retryAfter;
            
            return new RateLimitDTO(current < limit, remaining, retryAfter, resetTime, limit);
        } catch (Exception e) {
            log.error("Error getting rate limit info for key: {}", redisKey, e);
            return new RateLimitDTO(true, limit, 0, System.currentTimeMillis() / 1000 + duration, limit);
        }
    }
}
