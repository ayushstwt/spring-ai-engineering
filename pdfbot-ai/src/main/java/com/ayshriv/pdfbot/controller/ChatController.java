package com.ayshriv.pdfbot.controller;

import com.ayshriv.pdfbot.dto.ChatRequest;
import com.ayshriv.pdfbot.dto.ChatResponse;
import com.ayshriv.pdfbot.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * Chat with uploaded PDFs
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid
            @RequestBody
            ChatRequest request) {

        log.info(
                "Received question: {}",
                request.getQuestion()
        );

        ChatResponse response =
                chatService.chat(
                        request.getQuestion()
                );

        return ResponseEntity.ok(response);
    }
}