package org.demo.whs.entity.enums;

/**
 * EmailType: Types of emails supported in the system
 */
public enum EmailType {
    WELCOME,                // Welcome email for new users
    PASSWORD_RESET,         // Password reset email
    PASSWORD_CHANGED,       // Password changed confirmation
    ORDER_CONFIRMATION,     // Order confirmation email
    INVENTORY_ALERT,        // Inventory low stock alert
    REPORT_EXPORT,          // Export report email with attachment
    NOTIFICATION,           // General notification email
    VERIFICATION            // Account verification email
}
