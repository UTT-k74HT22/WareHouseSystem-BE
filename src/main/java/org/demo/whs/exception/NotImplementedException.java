package org.demo.whs.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for code paths that are intentionally scaffolded but not implemented yet.
 */
public class NotImplementedException extends BaseException {

    public NotImplementedException(String message) {
        super("COM_999", message, HttpStatus.NOT_IMPLEMENTED);
    }
}
