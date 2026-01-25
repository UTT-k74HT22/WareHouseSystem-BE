package org.demo.whs.service;

import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.dto.request.SendEmailRequest;
import org.demo.whs.entity.dto.response.EmailLogResponse;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.entity.enums.EmailType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * EmailService: Service interface for email operations
 */
public interface EmailService {

    /**
     * Send email synchronously
     *
     * @param request Email request
     * @return EmailLog entity
     */
    EmailLog sendEmail(SendEmailRequest request);

    /**
     * Send email asynchronously via RabbitMQ
     *
     * @param request Email request
     * @return EmailLog entity with PENDING status
     */
    EmailLog sendEmailAsync(SendEmailRequest request);

    /**
     * Send simple text email
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param content Email content
     * @param emailType Email type
     */
    void sendSimpleEmail(String to, String subject, String content, EmailType emailType);

    /**
     * Send HTML email
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param htmlContent HTML content
     * @param emailType Email type
     */
    void sendHtmlEmail(String to, String subject, String htmlContent, EmailType emailType);

    /**
     * Send email using Thymeleaf template
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param templateName Thymeleaf template name
     * @param variables Template variables
     * @param emailType Email type
     */
    void sendTemplateEmail(String to, String subject, String templateName,
                           Map<String, Object> variables, EmailType emailType);

    /**
     * Send email with attachment
     *
     * @param to Recipient email
     * @param subject Email subject
     * @param content Email content
     * @param emailType Email type
     * @param attachmentFile Attachment file
     */
    void sendEmailWithAttachment(String to, String subject, String content,
                                  EmailType emailType, File attachmentFile);

    /**
     * Retry failed email
     *
     * @param emailLogId Email log ID
     * @return Updated EmailLog
     */
    EmailLog retryEmail(String emailLogId);

    /**
     * Get email log by ID
     *
     * @param id Email log ID
     * @return EmailLogResponse
     */
    EmailLogResponse getEmailLog(String id);

    /**
     * Get all email logs with pagination
     *
     * @param pageable Pagination
     * @return Page of EmailLogResponse
     */
    Page<EmailLogResponse> getAllEmailLogs(Pageable pageable);

    /**
     * Get email logs by status
     *
     * @param status Email status
     * @param pageable Pagination
     * @return Page of EmailLogResponse
     */
    Page<EmailLogResponse> getEmailLogsByStatus(EmailStatus status, Pageable pageable);

    /**
     * Get email logs by email type
     *
     * @param emailType Email type
     * @param pageable Pagination
     * @return Page of EmailLogResponse
     */
    Page<EmailLogResponse> getEmailLogsByType(EmailType emailType, Pageable pageable);

    /**
     * Get email logs by recipient
     *
     * @param recipient Recipient email
     * @param pageable Pagination
     * @return Page of EmailLogResponse
     */
    Page<EmailLogResponse> getEmailLogsByRecipient(String recipient, Pageable pageable);

    /**
     * Process pending emails (called by scheduled task)
     */
    void processPendingEmails();

    /**
     * Retry failed emails (called by scheduled task)
     */
    void retryFailedEmails();

    /**
     * Cleanup old email logs (called by scheduled task)
     */
    void cleanupOldLogs();

    /**
     * Get email statistics
     *
     * @return Statistics map
     */
    Map<String, Long> getEmailStatistics();
}
