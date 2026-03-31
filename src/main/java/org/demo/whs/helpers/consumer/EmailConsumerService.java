package org.demo.whs.helpers.consumer;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.configuration.EmailProperties;
import org.demo.whs.configuration.RabbitMQEmailProperties;
import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.dto.EmailMessageDTO;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.EmailLogRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.time.LocalDateTime;
import static org.demo.whs.exception.ErrorCode.EMAIL_NOT_FOUND;

/**
 * EmailConsumerService: Service to consume emails from RabbitMQ queue and send them
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class EmailConsumerService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final EmailProperties emailProperties;
    private final RabbitMQEmailProperties rabbitMQEmailProperties;

    /**
     * Listen to email queue and process emails
     *
     * @param messageDTO Email log from queue
     */
    @RabbitListener(queues = "#{rabbitMQEmailProperties.queue}",
            containerFactory = "emailListenerContainerFactory")
    @Transactional
    public void consumeEmail(EmailMessageDTO messageDTO) {
        log.info("Consuming email from queue for recipient: {}", messageDTO.getRecipient());


        // Fetch the email log from DB
        EmailLog emailLog = emailLogRepository.findById(messageDTO.getEmailLogId())
                .orElseThrow(() -> new NotFoundException(EMAIL_NOT_FOUND.getCode(), EMAIL_NOT_FOUND));

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
            log.error("Failed to send email to: {}", messageDTO.getRecipient(), e);
            emailLog.setRetryCount(emailLog.getRetryCount() + 1);

            if (emailLog.getRetryCount() >= emailLog.getMaxRetry()) {
                emailLog.setStatus(EmailStatus.FAILED);
            } else {
                emailLog.setStatus(EmailStatus.RETRY);
            }
            emailLog.setErrorMessage(e.getMessage());
            emailLogRepository.save(emailLog);
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
