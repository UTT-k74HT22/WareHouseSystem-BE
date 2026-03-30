package org.demo.whs.entity.dto.request.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatBotRequest {
    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;

    @Size(max = 100, message = "Conversation ID must not exceed 100 characters")
    private String conversationId;
}
