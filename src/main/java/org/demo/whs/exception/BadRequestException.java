package org.demo.whs.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException {
    public BadRequestException(String message, ErrorCode errorCode) {
        super(message, errorCode.getMessage(), HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.BAD_REQUEST);
    }
}
