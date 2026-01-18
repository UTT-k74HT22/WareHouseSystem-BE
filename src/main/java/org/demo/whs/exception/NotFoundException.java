package org.demo.whs.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {
    public NotFoundException(String message, ErrorCode errorCode) {
        super(message, errorCode.getMessage(), HttpStatus.NOT_FOUND);
    }
}
