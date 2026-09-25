package org.demo.whs.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.FieldError;
import org.demo.whs.entity.dto.response.RateLimitErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST controllers
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandle {

    private final PublicErrorMessageResolver publicErrorMessageResolver;

    private static final Map<String, String> FIELD_NAMES = Map.ofEntries(
            Map.entry("username", "Tên đăng nhập"),
            Map.entry("password", "Mật khẩu"),
            Map.entry("email", "Email"),
            Map.entry("name", "Tên"),
            Map.entry("code", "Mã"),
            Map.entry("status", "Trạng thái"),
            Map.entry("quantity", "Số lượng"),
            Map.entry("warehouseId", "Kho"),
            Map.entry("locationId", "Vị trí kho"),
            Map.entry("productId", "Sản phẩm"),
            Map.entry("categoryId", "Danh mục"),
            Map.entry("roleIds", "Danh sách vai trò"),
            Map.entry("permissionIds", "Danh sách quyền")
    );

    /**
     * Handle custom BaseException
     * @param ex the BaseException
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<BaseResponse<Void>> handleBaseException(BaseException ex) {
        log.warn("{}: {} - errorCode={}", ex.getClass().getSimpleName(), ex.getMessage(), ex.getErrorCode());
        String publicMessage = publicErrorMessageResolver.resolve(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(BaseResponse.error(ex.getErrorCode(), publicMessage, null));
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
                        .message(toVietnameseValidationMessage(error.getField(), error.getDefaultMessage()))
                        .rejectedValue(isSensitiveField(error.getField()) ? null : error.getRejectedValue())
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
                        .message(toVietnameseValidationMessage(
                                violation.getPropertyPath().toString(), violation.getMessage()))
                        .rejectedValue(isSensitiveField(violation.getPropertyPath().toString())
                                ? null : violation.getInvalidValue())
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
        FieldError fieldError = FieldError.builder()
                .field(ex.getName())
                .message("Giá trị không đúng định dạng yêu cầu")
                .rejectedValue(ex.getValue())
                .build();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.COM_001.getCode(), ErrorCode.COM_001.getMessage(), List.of(fieldError)));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<BaseResponse<Void>> handleMissingRequestValue(Exception ex) {
        log.warn("Missing required request value: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.COM_013.getCode(), ErrorCode.COM_013.getMessage(), null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Resource not found: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error(ErrorCode.COM_004.getCode(), ErrorCode.COM_004.getMessage(), null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(BaseResponse.error(ErrorCode.COM_011.getCode(), ErrorCode.COM_011.getMessage(), null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Media type not supported: {}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(BaseResponse.error(ErrorCode.COM_012.getCode(), ErrorCode.COM_012.getMessage(), null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<BaseResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Upload exceeds maximum allowed size");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(BaseResponse.error(ErrorCode.STORAGE_006.getCode(), ErrorCode.STORAGE_006.getMessage(), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(ErrorCode.COM_005.getCode(), ErrorCode.COM_005.getMessage(), null));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<BaseResponse<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: entity={}", ex.getPersistentClassName());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(ErrorCode.COM_009.getCode(), ErrorCode.COM_009.getMessage(), null));
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

    private String toVietnameseValidationMessage(String field, String message) {
        String fieldName = FIELD_NAMES.getOrDefault(simpleFieldName(field), "Trường " + simpleFieldName(field));
        if (message == null || message.isBlank()) {
            return fieldName + " không hợp lệ";
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("must not be blank") || normalized.contains("must not be empty")
                || normalized.contains("must not be null") || normalized.contains("is required")
                || normalized.contains("cannot be blank") || normalized.contains("cannot be empty")) {
            return fieldName + " là bắt buộc";
        }
        if (normalized.contains("must be a well-formed email") || normalized.contains("invalid email")) {
            return fieldName + " không đúng định dạng email";
        }
        if (normalized.contains("must be greater than or equal to")) {
            return fieldName + " phải lớn hơn hoặc bằng giá trị tối thiểu cho phép";
        }
        if (normalized.contains("must be greater than") || normalized.contains("must be positive")) {
            return fieldName + " phải lớn hơn 0";
        }
        if (normalized.contains("must be less than or equal to")) {
            return fieldName + " vượt quá giá trị tối đa cho phép";
        }
        if (normalized.contains("size must be between") || normalized.contains("length must be between")) {
            return fieldName + " có độ dài không hợp lệ";
        }
        if (normalized.contains("size must be") || normalized.contains("must be at least")
                || normalized.contains("must not exceed") || normalized.contains("too long")) {
            return fieldName + " có độ dài không hợp lệ";
        }
        if (normalized.contains("must match") || normalized.contains("invalid format")
                || normalized.contains("invalid pattern")) {
            return fieldName + " không đúng định dạng";
        }
        if (containsVietnameseCharacter(message)) {
            return message;
        }
        return fieldName + " không hợp lệ";
    }

    private String simpleFieldName(String field) {
        if (field == null || field.isBlank()) {
            return "dữ liệu";
        }
        int dotIndex = field.lastIndexOf('.');
        return dotIndex >= 0 ? field.substring(dotIndex + 1) : field;
    }

    private boolean isSensitiveField(String field) {
        String normalized = field == null ? "" : field.toLowerCase(Locale.ROOT);
        return normalized.contains("password") || normalized.contains("token")
                || normalized.contains("otp") || normalized.contains("secret");
    }

    private boolean containsVietnameseCharacter(String value) {
        return value.matches(".*[À-ỹ].*");
    }
}
