package com.ayshriv.pdfbot.service;

import com.ayshriv.pdfbot.dto.EmbeddingRequest;
import com.ayshriv.pdfbot.dto.EmbeddingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LmStudioEmbeddingService {

    @Value("${lmstudio.embedding.url}")
    private String embeddingUrl;

    @Value("${lmstudio.embedding.model}")
    private String embeddingModel;

    private final WebClient webClient;

    /**
     * Generate embeddings using LM Studio
     */
    public List<Double> generateEmbedding(String text) {

        log.info("Generating embedding from LM Studio");

        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(embeddingModel)
                .input(text)
                .build();

        EmbeddingResponse response = webClient.post()
                .uri(embeddingUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();

        validateResponse(response);

        return response.getData()
                .get(0)
                .getEmbedding();
    }

    /**
     * Validate response
     */
    private void validateResponse(EmbeddingResponse response) {

        if (response == null ||
                response.getData() == null ||
                response.getData().isEmpty()) {

            throw new RuntimeException("Invalid LM Studio embedding response");
        }
    }

    /**
     * FIXED: Convert embedding to pgvector format
     */
    public String convertToPgVector(List<Double> embedding) {

        return "[" +
                embedding.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")) +
                "]";
    }
}