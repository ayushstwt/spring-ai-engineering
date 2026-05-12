package com.ayshriv.chat_memory.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/api")
public class ChatMemoryController {

    private final ChatClient chatClient;

    public ChatMemoryController(@Qualifier("chatMemoryChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/chat-memory")
    public ResponseEntity<String> getChatMemory(@RequestParam("message") String message,
                                                @RequestParam("username") String username) {
        return ResponseEntity.ok(chatClient.
                prompt()
                .user(message)
                        .advisors(advisorSpec -> {
                            advisorSpec.param(CONVERSATION_ID, username);
                        })
                .call()
                .content());
    }
}
