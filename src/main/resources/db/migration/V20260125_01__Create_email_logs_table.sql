-- Create email_logs table
-- This table stores all email sending history and status

CREATE TABLE IF NOT EXISTS email_logs (
    id CHAR(36) NOT NULL PRIMARY KEY COMMENT 'UUID primary key',
    recipient VARCHAR(255) NOT NULL COMMENT 'Email recipient',
    cc VARCHAR(1000) NULL COMMENT 'CC recipients (comma-separated)',
    bcc VARCHAR(1000) NULL COMMENT 'BCC recipients (comma-separated)',
    subject VARCHAR(500) NOT NULL COMMENT 'Email subject',
    content TEXT NOT NULL COMMENT 'Email content (HTML or plain text)',
    email_type VARCHAR(50) NOT NULL COMMENT 'Type of email: WELCOME, PASSWORD_RESET, etc.',
    status VARCHAR(20) NOT NULL COMMENT 'Status: PENDING, SENDING, SENT, FAILED, RETRY',
    retry_count INT NOT NULL DEFAULT 0 COMMENT 'Number of retry attempts',
    max_retry INT NOT NULL DEFAULT 3 COMMENT 'Maximum retry attempts',
    error_message TEXT NULL COMMENT 'Error message if failed',
    sent_at DATETIME NULL COMMENT 'Timestamp when email was successfully sent',
    has_attachment BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether email has attachment',
    attachment_path VARCHAR(500) NULL COMMENT 'Path to attachment file',
    priority INT NOT NULL DEFAULT 5 COMMENT 'Priority (1=highest, 10=lowest)',
    scheduled_at DATETIME NULL COMMENT 'Scheduled time to send email',
    triggered_by CHAR(36) NULL COMMENT 'Account ID who triggered this email',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
    created_by VARCHAR(50) NULL COMMENT 'User who created this record',
    updated_by VARCHAR(50) NULL COMMENT 'User who last updated this record',

    INDEX idx_recipient (recipient),
    INDEX idx_status (status),
    INDEX idx_email_type (email_type),
    INDEX idx_created_at (created_at),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_triggered_by (triggered_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Email sending history and logs';
