package org.demo.whs.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails
 */
public class AuthenticationFailedException extends BaseException {

    public AuthenticationFailedException(String message, ErrorCode errorCode) {
        super(errorCode.getCode(), message, HttpStatus.UNAUTHORIZED);
    }

    public AuthenticationFailedException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.UNAUTHORIZED);
    }
}

