package org.demo.whs.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * EmailProperties: Configuration properties for email functionality
 */
@Configuration
@ConfigurationProperties(prefix = "app.email")
@Data
public class EmailProperties {

    /**
     * Whether email sending is enabled
     */
    private boolean enabled = true;

    /**
     * Default sender email address
     */
    private String from = "noreply@warehouse.com";

    /**
     * Default sender name
     */
    private String fromName = "Warehouse Management System";

    /**
     * Maximum retry attempts for failed emails
     */
    private int maxRetry = 3;

    /**
     * Retry delay in seconds
     */
    private long retryDelaySeconds = 60;

    /**
     * Whether to send emails asynchronously by default
     */
    private boolean asyncByDefault = true;

    /**
     * Path to store email attachments
     */
    private String attachmentPath = "temp/email-attachments";

    /**
     * Maximum attachment size in MB
     */
    private int maxAttachmentSizeMb = 10;

    /**
     * Days to keep email logs before cleanup
     */
    private int logRetentionDays = 90;

    /**
     * RabbitMQ queue name for email sending
     */
    private String queueName = "wms.email.queue";

    /**
     * RabbitMQ exchange name
     */
    private String exchangeName = "wms.email.exchange";

    /**
     * RabbitMQ routing key
     */
    private String routingKey = "wms.email.send";
}
