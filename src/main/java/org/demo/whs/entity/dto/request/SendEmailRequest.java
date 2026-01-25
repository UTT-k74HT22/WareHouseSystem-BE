package org.demo.whs.entity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.demo.whs.entity.enums.EmailType;

import java.util.List;
import java.util.Map;

/**
 * SendEmailRequest: DTO for sending email request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendEmailRequest {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Recipient email must be valid")
    private String recipient;

    private List<@Email String> cc;

    private List<@Email String> bcc;

    @NotBlank(message = "Subject is required")
    @Size(max = 500, message = "Subject must not exceed 500 characters")
    private String subject;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Email type is required")
    private EmailType emailType;

    /**
     * Template variables for dynamic content
     * e.g., {userName: "John", resetLink: "https://..."}
     */
    private Map<String, Object> templateVariables;

    /**
     * Template name (if using Thymeleaf templates)
     * e.g., "welcome-email", "password-reset"
     */
    private String templateName;

    /**
     * Priority (1 = highest, 10 = lowest)
     */
    private Integer priority;

    /**
     * Whether to send asynchronously via RabbitMQ
     */
    private Boolean async;

    /**
     * Attachment file path (if any)
     */
    private String attachmentPath;

    /**
     * Scheduled time to send email (if null, send immediately)
     */
    private java.time.LocalDateTime scheduledAt;
}
