package org.demo.whs.service;

import org.demo.whs.entity.dto.request.chatbot.ChatBotRequest;
import org.demo.whs.entity.dto.response.chatbot.ChatBotResponse;

public interface ChatBotService {
    ChatBotResponse chat(ChatBotRequest request);
}
