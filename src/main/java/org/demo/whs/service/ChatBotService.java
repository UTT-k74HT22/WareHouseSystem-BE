package org.demo.whs.service;

import org.demo.whs.entity.dto.chatbot.ChatBotRequest;
import org.demo.whs.entity.dto.chatbot.ChatBotResponse;

public interface ChatBotService {
    ChatBotResponse chat(ChatBotRequest request);
}
