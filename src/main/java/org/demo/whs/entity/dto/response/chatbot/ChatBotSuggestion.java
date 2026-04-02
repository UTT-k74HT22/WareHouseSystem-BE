package org.demo.whs.entity.dto.response.chatbot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.demo.whs.service.chatbot.ChatBotIntent;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotSuggestion {
    private String label;
    private ChatBotIntent intent;
    private String sku;
    private String query;
    private Boolean requiresInput;
}
