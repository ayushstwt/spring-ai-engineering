package com.ayshriv.pdfbot.service;

import com.ayshriv.pdfbot.entity.DocumentChunk;
import com.ayshriv.pdfbot.repository.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfIngestionService {

    private static final int CHUNK_SIZE = 1000;

    private final LmStudioEmbeddingService embeddingService;

    private final DocumentChunkRepository repository;

    /**
     * Main PDF ingestion flow
     */
    public void ingestPdf(MultipartFile file)
            throws IOException {

        log.info("Starting PDF ingestion: {}",
                file.getOriginalFilename());

        String extractedText = extractText(file);

        List<String> chunks =
                splitIntoChunks(extractedText);

        saveChunks(
                chunks,
                file.getOriginalFilename()
        );

        log.info("PDF ingestion completed");
    }

    /**
     * Extract text from PDF
     */
    private String extractText(
            MultipartFile file)
            throws IOException {

        try (PDDocument document =
                     Loader.loadPDF(file.getBytes())) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            return stripper.getText(document);
        }
    }

    /**
     * Split large text into chunks
     */
    private List<String> splitIntoChunks(
            String text) {

        List<String> chunks =
                new ArrayList<>();

        int start = 0;

        while (start < text.length()) {

            int end = Math.min(
                    start + CHUNK_SIZE,
                    text.length()
            );

            chunks.add(
                    text.substring(start, end)
            );

            start = end;
        }

        return chunks;
    }

    /**
     * Generate embeddings and save chunks
     */
    private void saveChunks(
            List<String> chunks,
            String documentName) {

        for (int i = 0; i < chunks.size(); i++) {

            String chunk = chunks.get(i);

            log.info(
                    "Processing chunk {} of {}",
                    i + 1,
                    chunks.size()
            );

            List<Double> embedding =
                    embeddingService
                            .generateEmbedding(chunk);

            String vector =
                    embeddingService
                            .convertToPgVector(
                                    embedding
                            );

            DocumentChunk documentChunk =
                    DocumentChunk.builder()
                            .documentName(documentName)
                            .content(chunk)
                            .chunkIndex(i)
                            .createdAt(LocalDateTime.now())
                            .build();

            repository.save(documentChunk);
        }
    }
}