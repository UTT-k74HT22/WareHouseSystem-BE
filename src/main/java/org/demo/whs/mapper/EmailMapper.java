package org.demo.whs.mapper;

import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.dto.EmailMessageDTO;
import org.demo.whs.entity.dto.response.EmailLogResponse;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
public class EmailMapper {

    // Dùng cho RabbitMQ message
    public EmailMessageDTO toMessageDto(EmailLog emailLog) {
        if (emailLog == null) {
            return null;
        }

        return EmailMessageDTO.builder()
                .emailLogId(emailLog.getId())
                .recipient(emailLog.getRecipient())
                .cc(splitToList(emailLog.getCc()))
                .bcc(splitToList(emailLog.getBcc()))
                .subject(emailLog.getSubject())
                .content(emailLog.getContent())
                .emailType(emailLog.getEmailType() != null ? emailLog.getEmailType().name() : null)
                .createdAt(emailLog.getCreatedAt())
                .build();
    }

    private List<String> splitToList(String str) {
        if (str == null || str.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .toList();
    }

    // Dùng cho REST response
    public EmailLogResponse mapToResponse(EmailLog emailLog, String triggeredByUsername) {
        if (emailLog == null) {
            return null;
        }

        return EmailLogResponse.builder()
                .id(emailLog.getId())
                .recipient(emailLog.getRecipient())
                .subject(emailLog.getSubject())
                .emailType(emailLog.getEmailType())
                .status(emailLog.getStatus())
                .retryCount(emailLog.getRetryCount())
                .errorMessage(emailLog.getErrorMessage())
                .sentAt(emailLog.getSentAt())
                .hasAttachment(emailLog.getHasAttachment())
                .priority(emailLog.getPriority())
                .scheduledAt(emailLog.getScheduledAt())
                .createdAt(emailLog.getCreatedAt())
                .updatedAt(emailLog.getUpdatedAt())
                .triggeredByUsername(triggeredByUsername)
                .build();
    }
}
