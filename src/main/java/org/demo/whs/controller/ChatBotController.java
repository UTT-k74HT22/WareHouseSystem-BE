package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.dto.chatbot.ChatBotRequest;
import org.demo.whs.entity.dto.chatbot.ChatBotResponse;
import org.demo.whs.service.ChatBotService;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ChatBotResponse> chat(@RequestBody ChatBotRequest request) {
        return ResponseEntity.ok(chatBotService.chat(request));
    }
}
