package org.demo.whs.helpers.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.configuration.RabbitMQEmailProperties;
import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.mapper.EmailMapper;
import org.demo.whs.repository.EmailLogRepository;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * EmailProducerService: Service to send emails to RabbitMQ queue
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class EmailProducerService {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQEmailProperties rabbitMQEmailProperties;
    private final EmailMapper emailMapper;
    private final EmailLogRepository emailLogRepository;

    /**
     * Send email log to RabbitMQ queue for async processing
     *
     * @param emailLog Email log to send
     */
    public void sendEmailToQueue(EmailLog emailLog) {
        try {
            log.info("Sending email to queue: {} for recipient: {}", rabbitMQEmailProperties.getQueue(), emailLog.getRecipient());

            rabbitTemplate.convertAndSend(
                    rabbitMQEmailProperties.getExchange(),
                    rabbitMQEmailProperties.getRoutingKey(),
                    emailMapper.toMessageDto(emailLog),
                    message -> {
                        message.getMessageProperties().setMessageId(emailLog.getId());
                        message.getMessageProperties().setCorrelationId(emailLog.getId());
                        message.getMessageProperties().setHeader("emailLogId", emailLog.getId());
                        return message;
                    },
                    new CorrelationData(emailLog.getId())
            );

            log.info("Email successfully sent to queue for recipient: {}", emailLog.getRecipient());
        } catch (Exception e) {
            log.error("Failed to send email to queue with id: {}", emailLog.getId(), e);
            emailLog.setStatus(EmailStatus.FAILED);
            emailLog.setErrorMessage("Queue error: " + e.getMessage());
            emailLogRepository.save(emailLog);
        }
    }
}
