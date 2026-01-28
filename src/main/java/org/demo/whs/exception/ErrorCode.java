package org.demo.whs.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Authentication errors
    AUTH_001("AUTH_001", "Invalid username or password"),
    AUTH_002("AUTH_002", "User not found"),
    AUTH_003("AUTH_003", "Access denied - insufficient permissions"),
    AUTH_004("AUTH_004", "Please activate your account before logging in"),
    AUTH_005("AUTH_005", "Invalid or expired token"),
    AUTH_006("AUTH_006", "Refresh token is invalid or expired"),
    AUTH_007("AUTH_007", "Account is locked due to suspicious activity"),
    AUTH_008("AUTH_008", "Too many failed login attempts - please try again later"),
    AUTH_009("AUTH_009", "Please register with new credentials"),
    AUTH_010("AUTH_010", "Please input new username and password"),
    AUTH_011("AUTH_011", "Please input password confirmation"),
    AUTH_012("AUTH_012", "Password and confirmation do not match"),

    // Common errors
    COM_001("COM_001", "Validation error - please check your input"),
    COM_002("COM_002", "Internal server error - please contact support"),
    COM_003("COM_003", "Bad request - malformed JSON or invalid data format"),
    COM_004("COM_004", "Resource not found"),
    COM_005("COM_005", "Duplicate entry - resource already exists"),

    // Rate limiting errors
    RATE_LIMIT_EXCEEDED("RATE_001", "Rate limit exceeded - too many requests"),

    // Email errors
    EMAIL_NOT_FOUND("EMAIL_001", "Email log not found");

    private final String code;
    private final String message;
}
