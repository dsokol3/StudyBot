package com.chatbot.repository;

import com.chatbot.entity.Document;
import com.chatbot.entity.Document.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    
    List<Document> findByConversationIdOrderByCreatedAtDesc(String conversationId);
    
    List<Document> findByStatus(DocumentStatus status);
    
    Optional<Document> findByFileHashAndConversationId(String fileHash, String conversationId);
    
    @Modifying
    @Query("UPDATE Document d SET d.status = :status, d.processedAt = :processedAt WHERE d.id = :id")
    void updateStatus(@Param("id") UUID id, 
                      @Param("status") DocumentStatus status, 
                      @Param("processedAt") LocalDateTime processedAt);
    
    @Modifying
    @Query("UPDATE Document d SET d.status = :status, d.errorMessage = :errorMessage WHERE d.id = :id")
    void updateStatusWithError(@Param("id") UUID id, 
                               @Param("status") DocumentStatus status, 
                               @Param("errorMessage") String errorMessage);
    
    @Modifying
    @Query("UPDATE Document d SET d.chunkCount = :chunkCount WHERE d.id = :id")
    void updateChunkCount(@Param("id") UUID id, @Param("chunkCount") Integer chunkCount);
    
    @Query("SELECT d FROM Document d WHERE d.status = 'PENDING' ORDER BY d.createdAt ASC")
    List<Document> findPendingDocuments();
    
    long countByConversationId(String conversationId);
}
