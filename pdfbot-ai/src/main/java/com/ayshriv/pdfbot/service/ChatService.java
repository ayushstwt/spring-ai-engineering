package com.ayshriv.pdfbot.service;

import com.ayshriv.pdfbot.dto.ChatResponse;
import com.ayshriv.pdfbot.entity.DocumentChunk;
import com.ayshriv.pdfbot.repository.DocumentChunkRepository;
import com.ayshriv.pdfbot.service.LmStudioEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final int TOP_K = 5;

    private final LmStudioEmbeddingService embeddingService;

    private final DocumentChunkRepository repository;

    private final ChatClient chatClient;

    /**
     * Chat with uploaded PDFs
     */
    public ChatResponse chat(String question) {

        log.info("Processing question: {}", question);

        /*
         * Step 1:
         * Generate embedding for user question
         */
        List<Double> questionEmbedding =
                embeddingService
                        .generateEmbedding(question);

        /*
         * Step 2:
         * Convert embedding into pgvector format
         */
        String vector =
                embeddingService
                        .convertToPgVector(
                                questionEmbedding
                        );

        /*
         * Step 3:
         * Find most similar chunks
         */
        List<DocumentChunk> similarChunks =
                repository.findSimilarChunks(
                        vector,
                        TOP_K
                );

        /*
         * Step 4:
         * Build RAG context
         */
        String context =
                buildContext(similarChunks);

        /*
         * Step 5:
         * Send context + question to Azure OpenAI
         */
        String answer =
                generateAiResponse(
                        context,
                        question
                );

        /*
         * Step 6:
         * Return final response
         */
        return ChatResponse.builder()
                .question(question)
                .answer(answer)
                .sources(
                        similarChunks.stream()
                                .map(
                                        DocumentChunk::getDocumentName
                                )
                                .distinct()
                                .toList()
                )
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Build RAG context
     */
    private String buildContext(
            List<DocumentChunk> chunks) {

        StringBuilder builder =
                new StringBuilder();

        for (DocumentChunk chunk : chunks) {

            builder.append(chunk.getContent())
                    .append("\n\n");
        }

        return builder.toString();
    }

    /**
     * Generate final AI answer
     */
    private String generateAiResponse(
            String context,
            String question) {

        String prompt = """
                You are an AI assistant for PDF documents.

                Answer ONLY from the provided context.

                If the answer is not present in the context,
                say:
                "I could not find the answer in the uploaded PDF."

                ======================
                CONTEXT:
                ======================

                %s

                ======================
                QUESTION:
                ======================

                %s
                """.formatted(context, question);

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}