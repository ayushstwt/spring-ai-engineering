package com.ayshriv.pdfbot.controller;

import com.ayshriv.pdfbot.service.PdfIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@Slf4j
public class PdfController {

    private final PdfIngestionService pdfIngestionService;

    /**
     * Upload and process PDF
     */
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> uploadPdf(
            @RequestParam("file")
            MultipartFile file) {

        try {

            validatePdf(file);

            log.info(
                    "Uploading PDF: {}",
                    file.getOriginalFilename()
            );

            pdfIngestionService.ingestPdf(file);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "message",
                            "PDF uploaded and processed successfully",
                            "fileName",
                            file.getOriginalFilename()
                    )
            );

        } catch (Exception e) {

            log.error(
                    "Error while uploading PDF",
                    e
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            Map.of(
                                    "success", false,
                                    "message", e.getMessage()
                            )
                    );
        }
    }

    /**
     * Validate uploaded PDF
     */
    private void validatePdf(
            MultipartFile file)
            throws IOException {

        if (file.isEmpty()) {

            throw new RuntimeException(
                    "File is empty"
            );
        }

        if (!file.getOriginalFilename()
                .toLowerCase()
                .endsWith(".pdf")) {

            throw new RuntimeException(
                    "Only PDF files are allowed"
            );
        }

        if (file.getSize() > 20 * 1024 * 1024) {

            throw new RuntimeException(
                    "Maximum file size is 20MB"
            );
        }
    }
}