package org.demo.whs.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    AUTH_001("AUTH_001", "Invalid credentials"),
    AUTH_002("AUTH_002", "User not found"),
    AUTH_003("AUTH_003", "Access denied"),


    // Common errors
    COM_001("COM_001", "Validation error"),
    COM_002("COM_002", "Internal server error"),
    COM_003("COM_003", "Bad request");

    private final String code;
    private final String message;
}
