package org.demo.whs.exception;

import org.springframework.http.HttpStatus;

public class BadRequest extends BaseException {
    public BadRequest(String message, ErrorCode errorCode) {
        super(message, errorCode.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
