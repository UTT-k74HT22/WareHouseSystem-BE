package org.demo.whs.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Response trả về khi rate limit bị vượt quá
 * Bao gồm thông tin về thời gian còn lại và số request đã sử dụng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RateLimitErrorResponse extends BaseResponse<Object> {
    
    /**
     * Số giây còn lại trước khi có thể thử lại
     */
    private Long retryAfter;
    
    /**
     * Số request tối đa được phép
     */
    private Integer limit;
    
    /**
     * Số request đã sử dụng
     */
    private Integer remaining;
    
    /**
     * Timestamp khi rate limit sẽ được reset (Unix timestamp)
     */
    private Long resetTime;
    
    public RateLimitErrorResponse(String code, String message, Long retryAfter, Integer limit, Integer remaining, Long resetTime) {
        super(false, code, message, null, null, java.time.LocalDateTime.now());
        this.retryAfter = retryAfter;
        this.limit = limit;
        this.remaining = remaining;
        this.resetTime = resetTime;
    }
}
