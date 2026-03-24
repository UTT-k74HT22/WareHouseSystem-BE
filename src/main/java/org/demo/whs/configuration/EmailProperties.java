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
    private boolean enabled = true;
    private String from = "noreply@warehouse.com";
    private String fromName = "Warehouse Management System";
    private int maxRetry = 3;
    private long retryDelaySeconds = 60;
    private boolean asyncByDefault = true;
    private String attachmentPath = "temp/email-attachments";
    private int maxAttachmentSizeMb = 10;
    private int logRetentionDays = 90;
}