package org.demo.whs.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * BaseException: A custom runtime exception that includes an error code and HTTP status.
 */
@Getter
public class BaseException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    protected BaseException(ErrorCode errorCode, HttpStatus status) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getCode();
        this.status = status;
    }

    protected BaseException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
