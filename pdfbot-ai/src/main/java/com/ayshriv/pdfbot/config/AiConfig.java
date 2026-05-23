package com.ayshriv.pdfbot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class AiConfig {

    /**
     * WebClient Bean
     *
     * Used for:
     * - LM Studio Embedding API
     * - External AI APIs
     */
    @Bean
    public WebClient webClient() {

        log.info("Initializing WebClient");

        return WebClient.builder()
                .build();
    }

    /**
     * Spring AI ChatClient
     *
     * Backed by Azure OpenAI
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}