package org.demo.whs.entity.dto.request.chatbot;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.demo.whs.service.chatbot.ChatBotIntent;

import java.util.Map;

@Data
public class ChatBotRequest {
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;

    @Size(max = 100, message = "Conversation ID must not exceed 100 characters")
    private String conversationId;

    private ChatBotIntent intent;

    private Map<String, Object> payload;
}
