package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * EmailScheduledService: Scheduled tasks for email processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailScheduledService {

    private final EmailService emailService;

    /**
     * Process pending emails every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void processPendingEmails() {
        log.info("Scheduled task: Processing pending emails");
        try {
            emailService.processPendingEmails();
        } catch (Exception e) {
            log.error("Error in scheduled task - processPendingEmails", e);
        }
    }

    /**
     * Retry failed emails every 30 minutes
     */
    @Scheduled(fixedDelay = 1800000) // 30 minutes
    public void retryFailedEmails() {
        log.info("Scheduled task: Retrying failed emails");
        try {
            emailService.retryFailedEmails();
        } catch (Exception e) {
            log.error("Error in scheduled task - retryFailedEmails", e);
        }
    }

    /**
     * Cleanup old email logs every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldLogs() {
        log.info("Scheduled task: Cleaning up old email logs");
        try {
            emailService.cleanupOldLogs();
        } catch (Exception e) {
            log.error("Error in scheduled task - cleanupOldLogs", e);
        }
    }
}
