package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.entity.enums.EmailType;

/**
 * EmailLog: Entity to track email sending history
 * Stores information about all emails sent by the system
 */
@Entity
@Table(name = "email_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog extends BaseEntity {

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Column(name = "cc", length = 1000)
    private String cc; // Comma-separated email addresses

    @Column(name = "bcc", length = 1000)
    private String bcc; // Comma-separated email addresses

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type", nullable = false, length = 50)
    private EmailType emailType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailStatus status;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Builder.Default
    @Column(name = "max_retry", nullable = false)
    private Integer maxRetry = 3;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    private java.time.LocalDateTime sentAt;

    @Builder.Default
    @Column(name = "has_attachment")
    private Boolean hasAttachment = false;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private Integer priority = 5; // 1 = highest, 10 = lowest

    @Column(name = "scheduled_at")
    private java.time.LocalDateTime scheduledAt;

    /**
     * Account ID who triggered this email (optional)
     * Stored as String to avoid foreign key constraint complexity
     */
    @Column(
            name = "triggered_by",
            length = 36,
            columnDefinition = "char(36)"
    )
    private String triggeredBy;
}
