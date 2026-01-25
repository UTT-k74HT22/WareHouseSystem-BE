package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.configuration.EmailProperties;
import org.demo.whs.entity.EmailLog;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * EmailProducerService: Service to send emails to RabbitMQ queue
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final EmailProperties emailProperties;

    /**
     * Send email log to RabbitMQ queue for async processing
     *
     * @param emailLog Email log to send
     */
    public void sendEmailToQueue(EmailLog emailLog) {
        try {
            log.info("Sending email to queue: {} for recipient: {}",
                    emailProperties.getQueueName(),
                    emailLog.getRecipient());

            rabbitTemplate.convertAndSend(
                    emailProperties.getExchangeName(),
                    emailProperties.getRoutingKey(),
                    emailLog
            );

            log.info("Email successfully sent to queue for recipient: {}", emailLog.getRecipient());
        } catch (Exception e) {
            log.error("Failed to send email to queue for recipient: {}", emailLog.getRecipient(), e);
            throw new RuntimeException("Failed to send email to queue", e);
        }
    }
}
