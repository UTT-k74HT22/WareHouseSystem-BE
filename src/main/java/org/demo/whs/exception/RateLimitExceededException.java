package org.demo.whs.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception được throw khi request vượt quá rate limit
 */
@Getter
public class RateLimitExceededException extends BaseException {
    
    private final long retryAfter;
    
    public RateLimitExceededException(String message, long retryAfter) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfter = retryAfter;
    }
    
    public RateLimitExceededException(long retryAfter) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfter = retryAfter;
    }
}
