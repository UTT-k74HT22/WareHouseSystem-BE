package org.demo.whs.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.configuration.EmailProperties;
import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.repository.EmailLogRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;

/**
 * EmailConsumerService: Service to consume emails from RabbitMQ queue and send them
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumerService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final EmailProperties emailProperties;

    /**
     * Listen to email queue and process emails
     *
     * @param emailLog Email log from queue
     */
    @RabbitListener(queues = "#{emailProperties.queueName}")
    @Transactional
    public void consumeEmail(EmailLog emailLog) {
        log.info("Consuming email from queue for recipient: {}", emailLog.getRecipient());

        try {
            // Update status to SENDING
            emailLog.setStatus(EmailStatus.SENDING);
            emailLog = emailLogRepository.save(emailLog);

            // Send the email
            sendMimeMessage(emailLog);

            // Update status to SENT
            emailLog.setStatus(EmailStatus.SENT);
            emailLog.setSentAt(LocalDateTime.now());
            emailLogRepository.save(emailLog);

            log.info("Email sent successfully to: {}", emailLog.getRecipient());

        } catch (Exception e) {
            log.error("Failed to send email to: {}", emailLog.getRecipient(), e);

            // Update status to FAILED
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
            emailLog.setRetryCount(emailLog.getRetryCount() + 1);
            emailLogRepository.save(emailLog);

            // Re-queue for retry if not exceeded max attempts
            if (emailLog.getRetryCount() < emailLog.getMaxRetry()) {
                log.info("Re-queueing email for retry (attempt {}/{})",
                        emailLog.getRetryCount(), emailLog.getMaxRetry());
                emailLog.setStatus(EmailStatus.RETRY);
                emailLogRepository.save(emailLog);
                // Could implement retry logic here with delay
            } else {
                log.error("Email permanently failed after {} attempts", emailLog.getMaxRetry());
            }
        }
    }

    /**
     * Send MIME message via JavaMailSender
     *
     * @param emailLog Email log containing email details
     * @throws MessagingException If email sending fails
     */
    private void sendMimeMessage(EmailLog emailLog) throws MessagingException {
        if (!emailProperties.isEnabled()) {
            log.warn("Email sending is disabled. Skipping email to: {}", emailLog.getRecipient());
            return;
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(emailProperties.getFrom(), emailProperties.getFromName());
        } catch (Exception e) {
            log.error("Failed to set from address", e);
            helper.setFrom(emailProperties.getFrom());
        }

        helper.setTo(emailLog.getRecipient());
        helper.setSubject(emailLog.getSubject());
        helper.setText(emailLog.getContent(), true); // true = HTML

        // CC recipients
        if (emailLog.getCc() != null && !emailLog.getCc().isEmpty()) {
            String[] ccArray = emailLog.getCc().split(",");
            helper.setCc(ccArray);
        }

        // BCC recipients
        if (emailLog.getBcc() != null && !emailLog.getBcc().isEmpty()) {
            String[] bccArray = emailLog.getBcc().split(",");
            helper.setBcc(bccArray);
        }

        // Attachment
        if (emailLog.getHasAttachment() && emailLog.getAttachmentPath() != null) {
            File file = new File(emailLog.getAttachmentPath());
            if (file.exists()) {
                helper.addAttachment(file.getName(), file);
            } else {
                log.warn("Attachment file not found: {}", emailLog.getAttachmentPath());
            }
        }

        mailSender.send(message);
    }
}
