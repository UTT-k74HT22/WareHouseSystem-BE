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

    WH_001("WHS_001", "Warehouse not found"),
    WH_002("WHS_002", "Insufficient stock in warehouse"),
    WH_003("WHS_003", "Invalid warehouse operation"),
    WH_004("WHS_004", "Warehouse code already exists"),
    WH_005("WHS_005", "Warehouse cannot be deleted because it has active locations"),
    WH_006("WHS_006", "Warehouse cannot be deleted because inventory still exists"),
    WH_007("WHS_007", "Warehouse is already inactive"),
    WH_008("WHS_008", "Warehouse is referenced by other resources"),
    INV_001("INV_001", "Inventory not found"),
    INV_002("INV_002", "Unreserve quantity exceeds reserved quantity"),
    INV_003("INV_003", "Invalid inventory dimension"),
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

    // Purchase order errors
    PO_001("PO_001", "Purchase order not found"),
    PO_002("PO_002", "Purchase order status is not DRAFT - cannot modify"),
    PO_003("PO_003", "Purchase order status transition is not allowed"),

    // Purchase order line errors
    POL_001("POL_001", "Purchase order line not found"),
    POL_002("POL_002", "Purchase order line quantity must be greater than zero"),
    POL_003("POL_003", "Purchase order line product not found"),
    POL_004("POL_004", "Purchase order line product is not active"),
    POL_005("POL_005","Product already exists in this purchase order"),
    POL_006("POL_006", "Quantity received cannot be greater than quantity ordered"),
    POL_007("POL_007", "Unit price must be greater than or equal to zero"),
    POL_008("POL_008", "Quantity ordered must be greater than zero"),

    // Common errors
    COM_001("COM_001", "Validation error - please check your input"),
    COM_002("COM_002", "Internal server error - please contact support"),
    COM_003("COM_003", "Bad request - malformed JSON or invalid data format"),
    COM_004("COM_004", "Resource not found"),
    COM_005("COM_005", "Duplicate entry - resource already exists"),
    COM_006("COM_006", "Page index must not be less than zero"),
    COM_007("COM_007", "Page size must be greater than zero"),
    COM_008("COM_008", "Page size must not exceed 100"),

    // Batch errors
    BATCH_001("BATCH_001", "Batch not found"),
    BATCH_002("BATCH_002", "Batch number already exists for this product"),
    BATCH_003("BATCH_003", "Invalid batch status transition"),
    BATCH_004("BATCH_004", "Batch already expired"),
    BATCH_005("BATCH_005", "Manufacturing date cannot be in the future"),
    BATCH_006("BATCH_006", "Expiry date must be after manufacturing date"),
    BATCH_007("BATCH_007", "Batch cannot be quarantined because reserved inventory exists"),
    BATCH_008("BATCH_008", "Batch cannot be released because it is expired"),
    BATCH_009("BATCH_009", "Product does not support batch tracking"),
    BATCH_010("BATCH_010", "Batch cannot change product once created"),
    BATCH_011("BATCH_011", "Batch status cannot be updated through this endpoint"),
    BATCH_012("BATCH_012", "Batch is not available for stock operations"),
    BATCH_013("BATCH_013", "Batch is already quarantined"),
    BATCH_014("BATCH_014", "Recalled batch cannot be quarantined"),
    BATCH_015("BATCH_015", "Only AVAILABLE batch can be quarantined"),
    BATCH_016("BATCH_016", "Batch cannot be released because it is not in quarantine"),
    BATCH_017("BATCH_017", "Batch cannot be released because it is expired"),
    BATCH_018("BATCH_018", "Recalled batch cannot be released"),
    BATCH_019("BATCH_019", "Batch date filter range is invalid"),

    // Inbound Receipt Line errors
    IRL_001("IRL_001", "Inbound receipt line not found"),
    IRL_002("IRL_002", "Inbound receipt not found"),
    IRL_003("IRL_003", "Inbound receipt is not in DRAFT status - cannot modify lines"),
    IRL_004("IRL_004", "Purchase order line does not belong to this receipt"),
    IRL_005("IRL_005", "Product is inactive"),
    IRL_006("IRL_006", "Location does not belong to receipt warehouse"),
    IRL_007("IRL_007", "Location is not usable - status is INACTIVE or MAINTENANCE"),
    IRL_008("IRL_008", "Batch is required for batch-tracked product"),
    IRL_009("IRL_009", "Batch is not allowed for non-batch tracked product"),
    IRL_010("IRL_010", "Batch is not compatible with quality status"),
    IRL_011("IRL_011", "Batch is EXPIRED or RECALLED - cannot be used"),
    IRL_012("IRL_012", "Quantity exceeds remaining quantity available for this purchase order line"),
    IRL_013("IRL_013", "Duplicate split dimension already exists in this receipt"),
    IRL_014("IRL_014", "Quarantine status requires notes"),
    IRL_015("IRL_015", "Invalid quality status"),
    IRL_016("IRL_016", "Purchase order line not found"),

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
    STORAGE_006("STORAGE_006", "File size exceeds maximum allowed limit"),

    // Permission errors
    PERM_001("PERM_001", "Permission not found"),
    PERM_002("PERM_002", "Permission name already exists"),
    PERM_003("PERM_003", "Permission already assigned to role"),
    PERM_004("PERM_004", "Invalid permission data"),

    PERM_005("PERM_005", "Permission code already exists"),
    PERM_006("PERM_006", "Permission already exists for resource and action"),
    PERM_007("PERM_007", "Permission is currently used by roles"),
    PERM_008("PERM_008", "Permission not assigned to role"),
    PERM_009("PERM_009", "Invalid resource"),
    PERM_010("PERM_010", "Invalid action"),
    PERM_011("PERM_011", "Permission code cannot be changed"),
    PERM_012("PERM_012", "Permission resource cannot be changed");

    private final String code;
    private final String message;
}
