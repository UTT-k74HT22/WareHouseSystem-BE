package org.demo.whs.entity.dto.response;

import lombok.*;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.entity.enums.EmailType;

import java.time.LocalDateTime;

/**
 * EmailLogResponse: DTO for email log response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLogResponse {

    private String id;
    private String recipient;
    private String subject;
    private EmailType emailType;
    private EmailStatus status;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime sentAt;
    private Boolean hasAttachment;
    private Integer priority;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String triggeredByUsername;
}
