package org.demo.whs.service.chatbot;

public record ChatBotCommand(
        ChatBotIntent intent,
        String originalMessage,
        String normalizedMessage,
        String subjectKeyword,
        Integer thresholdDays
) {
}
