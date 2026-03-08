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
    AUTH_009("AUTH_009", "Account is not active"),
    AUTH_010("AUTH_010", "Account is not found"),

    // Password reset errors
    RESET_001("RESET_001", "Invalid or expired reset token"),
    RESET_002("RESET_002", "Reset token already used"),
    RESET_003("RESET_003", "Password does not meet security requirements"),
    RESET_004("RESET_004", "New password is the same as the old password"),
    RESET_005("RESET_005", "Old password is incorrect"),
    // Role
    ROLE_001("ROLE_001", "Role not found"),

    // OTP errors
    OTP_001("OTP_001", "OTP type is required"),
    OTP_002("OTP_002", "Account not found"),
    OTP_003("OTP_003", "Email already verified - cannot resend REGISTER OTP"),
    OTP_004("OTP_004", "You have exceeded the number of OTP sent per day"),
    OTP_005("OTP_005", "Please wait before resending OTP"),
    OTP_006("OTP_006", "Invalid or expired OTP"),

    // Ware House errors
    WHS_001("WHS_001", "Warehouse not found"),
    WHS_002("WHS_002", "Insufficient stock in warehouse"),
    WHS_003("WHS_003", "Invalid warehouse operation"),
    WHS_004("WHS_004", "Warehouse code already exists"),

    INV_001("INV_001", "Inventory not found"),
    INV_002("INV_002", "Unreserve quantity exceeds reserved quantity"),
    INV_004("INV_004", "Insufficient available stock"),

    STA_001("STA_001", "Invalid stock adjustment request"),
    STA_002("STA_002", "Invalid stock adjustment status transition"),
    STA_003("STA_003", "Rejection reason is required"),
    STA_404("STA_404", "Stock adjustment not found"),
    STF_001("STF_001", "Stock transfer not found"),
    STF_002("STF_002", "Invalid stock transfer status transition"),
    STF_003("STF_003", "Quantity must be greater than 0"),

    // Location errors
    LOC_001("LOC_001", "Location not found"),
    LOC_002("LOC_002", "Invalid location data"),
    LOC_003("LOC_003", "Location code already exists in this warehouse"),
    LOC_004("LOC_004", "Warehouse is not active - cannot create location"),
    LOC_005("LOC_005", "Invalid location status transition"),
    LOC_006("LOC_006", "Cannot change status - location has active inventory"),

    // Product errors
    PROD_001("PROD_001", "Product not found"),
    PROD_002("PROD_002", "Product SKU already exists"),
    PROD_003("PROD_003", "Invalid product data"),
    PROD_004("PROD_004", "Category not found or inactive"),
    PROD_005("PROD_005", "Unit of Measure not found"),
    PROD_006("PROD_006", "Cannot disable batch tracking - batch inventory exists"),
    PROD_007("PROD_007", "Max stock level must be greater than or equal to min stock level"),
    PROD_008("PROD_008", "Reorder point must be between min and max stock levels"),

    // Category errors
    CAT_001("CAT_001", "Category not found"),
    CAT_002("CAT_002", "Category code or name already exists"),

    // UnitsOfMeasure errors
    UOM_001("UOM_001", "Unit of Measure not found"),
    UOM_002("UOM_002", "Unit of Measure code already exists"),
    UOM_003("UOM_003", "Data is not null"),
    UOM_004("UOM_004", "Unit of Measure is in use by products"),

    // Employee errors
    EMP_001("EMP_001", "Employee not found"),
    EMP_002("EMP_002", "Employee code already exists"),
    EMP_003("EMP_003", "Account already linked to an employee"),
    EMP_004("EMP_004", "Account not found"),
    EMP_005("EMP_005", "Invalid employee status transition"),
    EMP_006("EMP_006", "Warehouse not found or inactive"),

    // Business Partner errors
    BP_001("BP_001", "Business partner not found"),
    BP_002("BP_002", "Business partner code already exists"),
    BP_003("BP_003", "Invalid business partner status"),
    BP_004("BP_004", "Business partner is inactive"),

    // Customer errors
    CUST_001("CUST_001", "Customer not found"),
    CUST_002("CUST_002", "Customer code already exists"),
    CUST_003("CUST_003", "Invalid customer status"),
    CUST_004("CUST_004", "Business partner is not a customer type"),

    // Common errors
    COM_001("COM_001", "Validation error - please check your input"),
    COM_002("COM_002", "Internal server error - please contact support"),
    COM_003("COM_003", "Bad request - malformed JSON or invalid data format"),
    COM_004("COM_004", "Resource not found"),
    COM_005("COM_005", "Duplicate entry - resource already exists"),
    COM_006("COM_006", "Page index must not be less than zero"),
    COM_007("COM_007", "Page size must be greater than zero"),
    COM_008("COM_008", "Page size must not exceed 100"),

    BATCH_001("BATCH_001", "Batch not found"),
    BATCH_002("BATCH_002", "Batch number already exists"),
    BATCH_003("BATCH_003", "Invalid batch status"),
    BATCH_004("BATCH_004", "Batch already expired"),
    BATCH_005("BATCH_005", "Manufacturing date is invalid"),


    // Rate limiting errors
    RATE_LIMIT_EXCEEDED("RATE_001", "Rate limit exceeded - too many requests"),

    // Email errors
    EMAIL_NOT_FOUND("EMAIL_001", "Email log not found"),
    EMAIL_002("EMAIL_002", "Email already verified or invalid"),

    // Storage (MinIO) errors
    STORAGE_001("STORAGE_001", "File upload failed"),
    STORAGE_002("STORAGE_002", "File not found in storage"),
    STORAGE_003("STORAGE_003", "File deletion failed"),
    STORAGE_004("STORAGE_004", "Failed to generate presigned URL"),
    STORAGE_005("STORAGE_005", "File type not allowed"),
    STORAGE_006("STORAGE_006", "File size exceeds maximum allowed limit");

    private final String code;
    private final String message;
}
