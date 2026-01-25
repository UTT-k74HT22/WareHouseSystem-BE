package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.dto.request.SendEmailRequest;
import org.demo.whs.entity.dto.response.EmailLogResponse;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.entity.enums.EmailType;
import org.demo.whs.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * EmailController: REST API endpoints for email operations
 */
@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Management", description = "APIs for email sending and management")
@SecurityRequirement(name = "Bearer Authentication")
public class EmailController {

    private final EmailService emailService;

    /**
     * Send email (sync or async based on request)
     *
     * POST /api/v1/emails/send
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send email", description = "Send email synchronously or asynchronously")
    public ResponseEntity<EmailLogResponse> sendEmail(@Valid @RequestBody SendEmailRequest request) {
        log.info("Received request to send email to: {}", request.getRecipient());

        EmailLog emailLog;
        if (Boolean.TRUE.equals(request.getAsync())) {
            emailLog = emailService.sendEmailAsync(request);
        } else {
            emailLog = emailService.sendEmail(request);
        }

        EmailLogResponse response = emailService.getEmailLog(emailLog.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get email log by ID
     *
     * GET /api/v1/emails/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get email log", description = "Get email log details by ID")
    public ResponseEntity<EmailLogResponse> getEmailLog(@PathVariable String id) {
        log.info("Fetching email log with ID: {}", id);
        EmailLogResponse response = emailService.getEmailLog(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all email logs
     *
     * GET /api/v1/emails
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all email logs", description = "Get all email logs with pagination")
    public ResponseEntity<Page<EmailLogResponse>> getAllEmailLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        log.info("Fetching all email logs - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<EmailLogResponse> emailLogs = emailService.getAllEmailLogs(pageable);
        return ResponseEntity.ok(emailLogs);
    }

    /**
     * Get email logs by status
     *
     * GET /api/v1/emails/status/{status}
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get emails by status", description = "Get email logs filtered by status")
    public ResponseEntity<Page<EmailLogResponse>> getEmailLogsByStatus(
            @PathVariable EmailStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching email logs with status: {}", status);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EmailLogResponse> emailLogs = emailService.getEmailLogsByStatus(status, pageable);
        return ResponseEntity.ok(emailLogs);
    }

    /**
     * Get email logs by type
     *
     * GET /api/v1/emails/type/{type}
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get emails by type", description = "Get email logs filtered by email type")
    public ResponseEntity<Page<EmailLogResponse>> getEmailLogsByType(
            @PathVariable EmailType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching email logs with type: {}", type);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EmailLogResponse> emailLogs = emailService.getEmailLogsByType(type, pageable);
        return ResponseEntity.ok(emailLogs);
    }

    /**
     * Get email logs by recipient
     *
     * GET /api/v1/emails/recipient/{email}
     */
    @GetMapping("/recipient/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get emails by recipient", description = "Get email logs for specific recipient")
    public ResponseEntity<Page<EmailLogResponse>> getEmailLogsByRecipient(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching email logs for recipient: {}", email);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EmailLogResponse> emailLogs = emailService.getEmailLogsByRecipient(email, pageable);
        return ResponseEntity.ok(emailLogs);
    }

    /**
     * Retry failed email
     *
     * POST /api/v1/emails/{id}/retry
     */
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retry failed email", description = "Retry sending a failed email")
    public ResponseEntity<EmailLogResponse> retryEmail(@PathVariable String id) {
        log.info("Retrying email with ID: {}", id);
        EmailLog emailLog = emailService.retryEmail(id);
        EmailLogResponse response = emailService.getEmailLog(emailLog.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Get email statistics
     *
     * GET /api/v1/emails/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get email statistics", description = "Get statistics about email sending")
    public ResponseEntity<Map<String, Long>> getEmailStatistics() {
        log.info("Fetching email statistics");
        Map<String, Long> statistics = emailService.getEmailStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Process pending emails manually (for testing)
     *
     * POST /api/v1/emails/process-pending
     */
    @PostMapping("/process-pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process pending emails", description = "Manually trigger processing of pending emails")
    public ResponseEntity<String> processPendingEmails() {
        log.info("Manually triggering pending email processing");
        emailService.processPendingEmails();
        return ResponseEntity.ok("Pending emails processing initiated");
    }

    /**
     * Retry failed emails manually (for testing)
     *
     * POST /api/v1/emails/retry-failed
     */
    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retry failed emails", description = "Manually trigger retry of failed emails")
    public ResponseEntity<String> retryFailedEmails() {
        log.info("Manually triggering failed email retry");
        emailService.retryFailedEmails();
        return ResponseEntity.ok("Failed emails retry initiated");
    }
}
