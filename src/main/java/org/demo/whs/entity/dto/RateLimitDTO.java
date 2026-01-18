package org.demo.whs.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin về rate limit state
 * 
 * Semantics:
 * - allowed=true: remaining >= 0, resetTime > 0, retryAfter = null
 * - allowed=false: remaining = 0, retryAfter > 0, resetTime > 0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitDTO {
    
    /**
     * Cho phép request hay không
     */
    private boolean allowed;
    
    /**
     * Số request còn lại trong window hiện tại
     * >= 0 khi allowed=true, = 0 khi allowed=false
     */
    private int remaining;
    
    /**
     * Limit tối đa
     */
    private int limit;
    
    /**
     * Timestamp (epoch seconds) khi rate limit window sẽ được reset
     * Luôn có giá trị, dùng cho X-RateLimit-Reset header
     */
    private long resetTime;
    
    /**
     * Số giây còn lại trước khi có thể retry
     * CHỈ có giá trị khi allowed=false (429 response)
     * null khi allowed=true
     */
    private Long retryAfter;
}
