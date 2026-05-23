package com.ayshriv.pdfbot.repository;

import com.ayshriv.pdfbot.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, Long> {

    /**
     * Search similar chunks using pgvector cosine similarity
     */
    @Query(value = """
            SELECT *
            FROM document_chunks
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    /**
     * Find chunks by document name
     */
    List<DocumentChunk> findByDocumentName(
            String documentName
    );

    /**
     * Delete chunks by document name
     */
    void deleteByDocumentName(
            String documentName
    );
}