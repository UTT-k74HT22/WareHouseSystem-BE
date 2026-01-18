package org.demo.whs.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user is not authorized (401)
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message, ErrorCode errorCode) {
        super(errorCode.getCode(), message, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.UNAUTHORIZED);
    }
}

