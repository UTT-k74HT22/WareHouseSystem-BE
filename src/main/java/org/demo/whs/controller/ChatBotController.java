package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.dto.request.chatbot.ChatBotRequest;
import org.demo.whs.entity.dto.response.chatbot.ChatBotResponse;
import org.demo.whs.service.ChatBotService;
import org.demo.whs.utils.annotation.RateLimit;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    @PostMapping("/chat")
    @RateLimit(key = "chatbot", limit = 5, duration = 60)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatBotResponse> chat(@Valid @RequestBody ChatBotRequest request) {
        return ResponseEntity.ok(chatBotService.chat(request));
    }
}
