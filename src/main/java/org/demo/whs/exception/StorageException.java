package org.demo.whs.exception;

import org.springframework.http.HttpStatus;

/**
 * StorageException – thrown when any MinIO / file-storage operation fails.
 */
public class StorageException extends BaseException {

    public StorageException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public StorageException(ErrorCode errorCode, HttpStatus status) {
        super(errorCode, status);
    }

    public StorageException(String message) {
        super(ErrorCode.STORAGE_001.getCode(), message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
