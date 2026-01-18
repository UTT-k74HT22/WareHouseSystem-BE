package org.demo.whs.annotation;

import org.demo.whs.entity.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation để áp dụng rate limiting cho API endpoints
 * 
 * Usage:
 * @RateLimit(key = "login", limit = 5, duration = 60, type = RateLimitType.IP)
 * 
 * Giải thích:
 * - key: Tên duy nhất để phân biệt rate limit rule
 * - limit: Số request tối đa được phép trong khoảng thời gian
 * - duration: Thời gian tính bằng giây (window time)
 * - type: Loại rate limit strategy (IP, USER, API, GLOBAL)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Key duy nhất để identify rate limit rule
     * Ví dụ: "login", "register", "api:v1:user"
     */
    String key();
    
    /**
     * Số lượng request tối đa được phép trong duration
     * Default: 100 requests
     */
    int limit() default 100;
    
    /**
     * Thời gian window tính bằng giây
     * Default: 60 seconds (1 phút)
     */
    int duration() default 60;
    
    /**
     * Loại rate limit strategy
     * Default: IP-based rate limiting
     */
    RateLimitType type() default RateLimitType.IP;
    
    /**
     * Message hiển thị khi rate limit exceeded
     * Default: "Rate limit exceeded. Please try again later."
     */
    String message() default "Rate limit exceeded. Please try again later.";
    
    /**
     * Fallback mode khi Redis unavailable
     * - FAIL_OPEN (false): Allow requests nếu Redis down (default cho read endpoints)
     * - FAIL_CLOSED (true): Block requests nếu Redis down (cho sensitive endpoints như login)
     */
    boolean failClosed() default false;
}
