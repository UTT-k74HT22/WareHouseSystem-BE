package org.demo.whs.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.configuration.EmailProperties;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.dto.request.SendEmailRequest;
import org.demo.whs.entity.dto.response.EmailLogResponse;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.entity.enums.EmailType;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.EmailLogRepository;
import org.demo.whs.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * EmailServiceImpl: Implementation of EmailService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final AccountRepository accountRepository;
    private final EmailProperties emailProperties;
    private final SpringTemplateEngine templateEngine;
    private final Optional<EmailProducerService> emailProducerService;

    @Override
    @Transactional
    public EmailLog sendEmail(SendEmailRequest request) {
        log.info("Sending email synchronously to: {}", request.getRecipient());

        // Create email log
        EmailLog emailLog = createEmailLog(request);
        emailLog.setStatus(EmailStatus.SENDING);
        emailLog = emailLogRepository.save(emailLog);

        try {
            // Determine content (template or direct content)
            String content = request.getContent();
            if (request.getTemplateName() != null && !request.getTemplateName().isEmpty()) {
                content = processTemplate(request.getTemplateName(), request.getTemplateVariables());
            }

            // Send email
            sendMimeMessage(
                    request.getRecipient(),
                    request.getCc(),
                    request.getBcc(),
                    request.getSubject(),
                    content,
                    request.getAttachmentPath()
            );

            // Update status to SENT
            emailLog.setStatus(EmailStatus.SENT);
            emailLog.setSentAt(LocalDateTime.now());
            log.info("Email sent successfully to: {}", request.getRecipient());

        } catch (Exception e) {
            log.error("Failed to send email to: {}", request.getRecipient(), e);
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
            emailLog.setRetryCount(emailLog.getRetryCount() + 1);
        }

        return emailLogRepository.save(emailLog);
    }

    @Override
    @Transactional
    public EmailLog sendEmailAsync(SendEmailRequest request) {
        log.info("Queuing email for async sending to: {}", request.getRecipient());

        // Create email log with PENDING status
        EmailLog emailLog = createEmailLog(request);
        emailLog.setStatus(EmailStatus.PENDING);
        emailLog = emailLogRepository.save(emailLog);

        // Send to RabbitMQ queue if available, otherwise send synchronously
        try {
            if (emailProducerService.isPresent()) {
                emailProducerService.get().sendEmailToQueue(emailLog);
                log.info("Email queued successfully for: {}", request.getRecipient());
            } else {
                log.warn("EmailProducerService not available, sending email synchronously instead");
                // Send synchronously as fallback
                return sendEmail(request);
            }
        } catch (Exception e) {
            log.error("Failed to queue email for: {}", request.getRecipient(), e);
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage("Failed to queue email: " + e.getMessage());
            emailLog = emailLogRepository.save(emailLog);
        }

        return emailLog;
    }

    @Override
    @Transactional
    public void sendSimpleEmail(String to, String subject, String content, EmailType emailType) {
        SendEmailRequest request = SendEmailRequest.builder()
                .recipient(to)
                .subject(subject)
                .content(content)
                .emailType(emailType)
                .async(emailProperties.isAsyncByDefault())
                .build();

        if (emailProperties.isAsyncByDefault()) {
            sendEmailAsync(request);
        } else {
            sendEmail(request);
        }
    }

    @Override
    @Transactional
    public void sendHtmlEmail(String to, String subject, String htmlContent, EmailType emailType) {
        sendSimpleEmail(to, subject, htmlContent, emailType);
    }

    @Override
    @Transactional
    public void sendTemplateEmail(String to, String subject, String templateName,
                                   Map<String, Object> variables, EmailType emailType) {
        SendEmailRequest request = SendEmailRequest.builder()
                .recipient(to)
                .subject(subject)
                .templateName(templateName)
                .templateVariables(variables)
                .emailType(emailType)
                .async(emailProperties.isAsyncByDefault())
                .build();

        if (emailProperties.isAsyncByDefault()) {
            sendEmailAsync(request);
        } else {
            sendEmail(request);
        }
    }

    @Override
    @Transactional
    public void sendEmailWithAttachment(String to, String subject, String content,
                                         EmailType emailType, File attachmentFile) {
        SendEmailRequest request = SendEmailRequest.builder()
                .recipient(to)
                .subject(subject)
                .content(content)
                .emailType(emailType)
                .attachmentPath(attachmentFile.getAbsolutePath())
                .async(emailProperties.isAsyncByDefault())
                .build();

        if (emailProperties.isAsyncByDefault()) {
            sendEmailAsync(request);
        } else {
            sendEmail(request);
        }
    }

    @Override
    @Transactional
    public EmailLog retryEmail(String emailLogId) {
        log.info("Retrying email with ID: {}", emailLogId);

        EmailLog emailLog = emailLogRepository.findById(emailLogId)
                .orElseThrow(() -> new NotFoundException("Email log not found with ID: " + emailLogId, ErrorCode.EMAIL_NOT_FOUND));

        if (emailLog.getRetryCount() >= emailLog.getMaxRetry()) {
            log.warn("Email has reached maximum retry attempts: {}", emailLogId);
            throw new IllegalStateException("Email has reached maximum retry attempts");
        }

        emailLog.setStatus(EmailStatus.RETRY);
        emailLog.setRetryCount(emailLog.getRetryCount() + 1);
        emailLog = emailLogRepository.save(emailLog);

        // Queue for retry if producer is available
        if (emailProducerService.isPresent()) {
            emailProducerService.get().sendEmailToQueue(emailLog);
        } else {
            log.warn("EmailProducerService not available, cannot retry email asynchronously");
            throw new IllegalStateException("Email producer service not available");
        }

        return emailLog;
    }

    @Override
    @Transactional(readOnly = true)
    public EmailLogResponse getEmailLog(String id) {
        EmailLog emailLog = emailLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Email log not found with ID: " + id, ErrorCode.EMAIL_NOT_FOUND));
        return mapToResponse(emailLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailLogResponse> getAllEmailLogs(Pageable pageable) {
        return emailLogRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailLogResponse> getEmailLogsByStatus(EmailStatus status, Pageable pageable) {
        return emailLogRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailLogResponse> getEmailLogsByType(EmailType emailType, Pageable pageable) {
        return emailLogRepository.findByEmailType(emailType, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmailLogResponse> getEmailLogsByRecipient(String recipient, Pageable pageable) {
        return emailLogRepository.findByRecipient(recipient, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void processPendingEmails() {
        log.info("Processing pending emails...");

        if (emailProducerService.isEmpty()) {
            log.warn("EmailProducerService not available, cannot process pending emails");
            return;
        }

        List<EmailStatus> statuses = Arrays.asList(EmailStatus.PENDING, EmailStatus.RETRY);
        List<EmailLog> pendingEmails = emailLogRepository.findPendingEmails(statuses, LocalDateTime.now());

        log.info("Found {} pending emails to process", pendingEmails.size());

        for (EmailLog emailLog : pendingEmails) {
            try {
                emailProducerService.get().sendEmailToQueue(emailLog);
            } catch (Exception e) {
                log.error("Failed to queue pending email: {}", emailLog.getId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void retryFailedEmails() {
        log.info("Retrying failed emails...");

        List<EmailLog> failedEmails = emailLogRepository.findFailedEmailsForRetry(EmailStatus.FAILED);

        log.info("Found {} failed emails to retry", failedEmails.size());

        for (EmailLog emailLog : failedEmails) {
            try {
                retryEmail(emailLog.getId());
            } catch (Exception e) {
                log.error("Failed to retry email: {}", emailLog.getId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void cleanupOldLogs() {
        log.info("Cleaning up old email logs...");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(emailProperties.getLogRetentionDays());
        emailLogRepository.deleteByCreatedAtBefore(cutoffDate);

        log.info("Email logs older than {} days have been deleted", emailProperties.getLogRetentionDays());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getEmailStatistics() {
        Map<String, Long> stats = new HashMap<>();

        stats.put("total", emailLogRepository.count());
        stats.put("pending", emailLogRepository.countByStatus(EmailStatus.PENDING));
        stats.put("sent", emailLogRepository.countByStatus(EmailStatus.SENT));
        stats.put("failed", emailLogRepository.countByStatus(EmailStatus.FAILED));
        stats.put("retry", emailLogRepository.countByStatus(EmailStatus.RETRY));

        return stats;
    }

    // ==================== Private Helper Methods ====================

    private EmailLog createEmailLog(SendEmailRequest request) {
        EmailLog emailLog = EmailLog.builder()
                .recipient(request.getRecipient())
                .cc(request.getCc() != null ? String.join(",", request.getCc()) : null)
                .bcc(request.getBcc() != null ? String.join(",", request.getBcc()) : null)
                .subject(request.getSubject())
                .content(request.getContent())
                .emailType(request.getEmailType())
                .status(EmailStatus.PENDING)
                .retryCount(0)
                .maxRetry(emailProperties.getMaxRetry())
                .hasAttachment(request.getAttachmentPath() != null)
                .attachmentPath(request.getAttachmentPath())
                .priority(request.getPriority() != null ? request.getPriority() : 5)
                .scheduledAt(request.getScheduledAt())
                .build();

        // Set triggered by user (store account ID only)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            accountRepository.findByUsername(username).ifPresent(account ->
                emailLog.setTriggeredBy(account.getId())
            );
        }

        return emailLog;
    }

    private void sendMimeMessage(String to, List<String> cc, List<String> bcc,
                                  String subject, String content, String attachmentPath)
            throws MessagingException {

        if (!emailProperties.isEnabled()) {
            log.warn("Email sending is disabled. Skipping email to: {}", to);
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(emailProperties.getFrom(), emailProperties.getFromName());
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to set from address", e);
            helper.setFrom(emailProperties.getFrom());
        }

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true = HTML

        if (cc != null && !cc.isEmpty()) {
            helper.setCc(cc.toArray(new String[0]));
        }

        if (bcc != null && !bcc.isEmpty()) {
            helper.setBcc(bcc.toArray(new String[0]));
        }

        if (attachmentPath != null && !attachmentPath.isEmpty()) {
            File file = new File(attachmentPath);
            if (file.exists()) {
                helper.addAttachment(file.getName(), file);
            }
        }

        mailSender.send(message);
    }

    private String processTemplate(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }
        return templateEngine.process(templateName, context);
    }

    private EmailLogResponse mapToResponse(EmailLog emailLog) {
        // Fetch username if triggeredBy ID exists
        String triggeredByUsername = null;
        if (emailLog.getTriggeredBy() != null) {
            triggeredByUsername = accountRepository.findById(emailLog.getTriggeredBy())
                    .map(Account::getUsername)
                    .orElse(null);
        }

        return EmailLogResponse.builder()
                .id(emailLog.getId())
                .recipient(emailLog.getRecipient())
                .subject(emailLog.getSubject())
                .emailType(emailLog.getEmailType())
                .status(emailLog.getStatus())
                .retryCount(emailLog.getRetryCount())
                .errorMessage(emailLog.getErrorMessage())
                .sentAt(emailLog.getSentAt())
                .hasAttachment(emailLog.getHasAttachment())
                .priority(emailLog.getPriority())
                .scheduledAt(emailLog.getScheduledAt())
                .createdAt(emailLog.getCreatedAt())
                .updatedAt(emailLog.getUpdatedAt())
                .triggeredByUsername(triggeredByUsername)
                .build();
    }
}
