package org.demo.whs.service.chatbot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatBotConversationContext {
    private String conversationId;
    private ChatBotIntent lastIntent;
    private String lastProductId;
    private String lastProductSku;
    private String lastProductName;
    private Integer lastThresholdDays;
    private String lastResolvedKeyword;
    private LocalDateTime updatedAt;
}
