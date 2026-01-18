package org.demo.whs.entity.enums;

/**
 * Enum định nghĩa các loại rate limit strategy
 */
public enum RateLimitType {
    /**
     * Rate limit theo IP address của client
     */
    IP,
    
    /**
     * Rate limit theo user đã authenticated
     */
    USER,
    
    /**
     * Rate limit theo API endpoint cụ thể
     */
    API,
    
    /**
     * Rate limit global cho toàn bộ hệ thống
     */
    GLOBAL
}
