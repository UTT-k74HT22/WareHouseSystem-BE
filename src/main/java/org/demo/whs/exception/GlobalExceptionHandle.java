package org.demo.whs.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
        log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(BaseResponse.error(ex.getErrorCode(), null));
    }

    /**
     * Handle validation errors for method arguments
     * @param ex the MethodArgumentNotValidException
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        log.warn("MethodArgumentNotValidException: {}", ex.getMessage());

        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.COM_001.getCode(), errors));
    }

    /**
     * Handle validation errors for constraint violations
     * @param ex the ConstraintViolationException
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.warn("ConstraintViolationException: {}", ex.getMessage());

        List<FieldError> errors = ex.getConstraintViolations().stream()
                .map(violation -> FieldError.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.COM_001.getCode(), errors));
    }

    /**
     * Handle errors when HTTP message is not readable
     * @param ex the HttpMessageNotReadableException
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        log.warn("HttpMessageNotReadableException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.COM_003.getCode(), null));
    }

    /**
     * Handle unexpected exceptions
     * @param ex the exception
     * @return ResponseEntity with error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception ex) {
        log.error("Unexpected exception: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(ErrorCode.COM_002.getCode(), null));
    }
}
