package org.demo.whs.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {
    public NotFoundException(String message, ErrorCode errorCode) {
        super(errorCode.getCode(), message, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.NOT_FOUND);
    }
}
