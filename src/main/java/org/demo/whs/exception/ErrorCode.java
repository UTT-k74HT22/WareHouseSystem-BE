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
    // OTP errors
    OTP_001("OTP_001", "OTP type is required"),
    OTP_002("OTP_002", "Account not found"),
    OTP_003("OTP_003", "Email already verified - cannot resend REGISTER OTP"),
    OTP_004("OTP_004", "You have exceeded the number of OTP sent per day"),
    OTP_005("OTP_005", "Please wait before resending OTP"),

    // Ware House errors
    WH_001("WHS_001", "Warehouse not found"),
    WH_002("WHS_002", "Insufficient stock in warehouse"),
    WH_003("WHS_003", "Invalid warehouse operation"),
    WH_004("WHS_004", "Warehouse code already exists"),

    // Location errors
    LOC_001("LOC_001", "Location not found"),
    LOC_002("LOC_002", "Invalid location data"),
    LOC_003("LOC_003", "Location code already exists in this warehouse"),
    LOC_004("LOC_004", "Warehouse is not active - cannot create location"),
    LOC_005("LOC_005", "Invalid location status transition"),
    LOC_006("LOC_006", "Cannot change status - location has active inventory"),

    // Product errors
    PROD_001("PROD_001", "Product not found"),
    PROD_002("PROD_002", "Product already exists"),
    PROD_003("PROD_003", "Invalid product data"),


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
