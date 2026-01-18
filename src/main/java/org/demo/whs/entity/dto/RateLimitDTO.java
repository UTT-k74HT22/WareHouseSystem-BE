package org.demo.whs.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin về rate limit state
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
     */
    private int remaining;
    
    /**
     * Số giây còn lại trước khi rate limit reset
     */
    private long retryAfter;
    
    /**
     * Timestamp khi rate limit sẽ được reset
     */
    private long resetTime;
    
    /**
     * Limit tối đa
     */
    private int limit;
}
