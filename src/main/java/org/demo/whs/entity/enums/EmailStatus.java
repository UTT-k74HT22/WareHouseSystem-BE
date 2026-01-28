package org.demo.whs.entity.enums;

/**
 * EmailStatus: Status of email sending
 */
public enum EmailStatus {
    PENDING,    // Email is queued and waiting to be sent
    SENDING,    // Email is being sent
    SENT,       // Email was sent successfully
    FAILED,     // Email failed to send
    RETRY       // Email is scheduled for retry
}
