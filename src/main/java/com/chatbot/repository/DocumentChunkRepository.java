package com.chatbot.repository;

import com.chatbot.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    
    List<DocumentChunk> findByDocumentIdOrderByChunkOrderAsc(UUID documentId);
    
    void deleteByDocumentId(UUID documentId);
    
    long countByDocumentId(UUID documentId);
    
    /**
     * Find similar chunks using pgvector cosine distance.
     * Lower distance = more similar (0 = identical, 2 = opposite).
     * Returns chunks from documents in the specified conversation.
     */
    @Query(value = """
        SELECT dc.* FROM document_chunks dc
        JOIN documents d ON dc.document_id = d.id
        WHERE d.conversation_id = :conversationId
        AND d.status = 'COMPLETED'
        AND dc.embedding IS NOT NULL
        AND (dc.embedding <=> cast(:queryEmbedding as vector)) < :maxDistance
        ORDER BY dc.embedding <=> cast(:queryEmbedding as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(
            @Param("conversationId") String conversationId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit);
    
    /**
     * Find similar chunks across all documents (global knowledge base).
     */
    @Query(value = """
        SELECT dc.* FROM document_chunks dc
        JOIN documents d ON dc.document_id = d.id
        WHERE d.status = 'COMPLETED'
        AND dc.embedding IS NOT NULL
        AND (dc.embedding <=> cast(:queryEmbedding as vector)) < :maxDistance
        ORDER BY dc.embedding <=> cast(:queryEmbedding as vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunksGlobal(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit);
}
