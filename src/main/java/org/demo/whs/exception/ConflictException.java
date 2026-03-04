package org.demo.whs.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.CONFLICT);
    }

    public ConflictException(String message, ErrorCode errorCode) {
        super(errorCode.getCode(), message, HttpStatus.CONFLICT);
    }
}
