package org.demo.whs.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.FieldError;
import org.demo.whs.entity.dto.response.RateLimitErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandle {

    /**
     * Handle custom BaseException
     * @param ex the BaseException
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException ex) {
        log.warn("{}: {} - errorCode={}", ex.getClass().getSimpleName(), ex.getMessage(), ex.getErrorCode());
        return ResponseEntity
                .status(ex.getStatus())
                .body(BaseResponse.error(ex.getErrorCode(), ex.getMessage(), null));
    }

    /**
     * Handle validation errors for method arguments
     * @param ex the MethodArgumentNotValidException
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {} field errors", ex.getBindingResult().getFieldErrorCount());

        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.COM_001.getCode(), ErrorCode.COM_001.getMessage(), errors));
    }

    /**
     * Handle validation errors for constraint violations
     * @param ex the ConstraintViolationException
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.warn("Constraint violation: {} violations", ex.getConstraintViolations().size());

        List<FieldError> errors = ex.getConstraintViolations().stream()
                .map(violation -> FieldError.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.COM_001.getCode(), ErrorCode.COM_001.getMessage(), errors));
    }

    /**
     * Handle errors when HTTP message is not readable
     * @param ex the HttpMessageNotReadableException
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.COM_003.getCode(), ErrorCode.COM_003.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid request parameter: parameter={}, value={}", ex.getName(), ex.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.COM_001.getCode(), ErrorCode.COM_001.getMessage(), null));
    }

    /**
     * Handle access denied exceptions
     * @param ex the exception
     * @return ResponseEntity with error response
     */
    @ExceptionHandler({
            org.springframework.security.access.AccessDeniedException.class,
            org.springframework.security.authorization.AuthorizationDeniedException.class
    })
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(Exception ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN) // Trả về 403
                .body(BaseResponse.error(ErrorCode.AUTH_003.getCode(),  ErrorCode.AUTH_003.getMessage(), null));
    }

    /**
     * Handle rate limit exceeded exceptions
     * @param ex the RateLimitExceededException
     * @return ResponseEntity with rate limit error response
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<RateLimitErrorResponse> handleRateLimitExceededException(
            RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {} - retry after: {}s", ex.getMessage(), ex.getRetryAfter());
        
        long resetTime = System.currentTimeMillis() / 1000 + ex.getRetryAfter();
        
        RateLimitErrorResponse response = new RateLimitErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getRetryAfter(),
            null,  // limit will be set by interceptor via headers
            0,     // remaining = 0 when exceeded
            resetTime
        );
        
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS) // 429
                .header("Retry-After", String.valueOf(ex.getRetryAfter()))
                .body(response);
    }

    /**
     * Handle unexpected exceptions
     * @param ex the exception
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(ErrorCode.COM_002.getCode(), ErrorCode.COM_002.getMessage(), null));
    }
}
