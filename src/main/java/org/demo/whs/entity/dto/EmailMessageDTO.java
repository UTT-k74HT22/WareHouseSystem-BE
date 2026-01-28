package org.demo.whs.entity.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmailMessageDTO {
    private String emailLogId;
    private String recipient;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String content;
    private String emailType;
    private LocalDateTime createdAt;
}
