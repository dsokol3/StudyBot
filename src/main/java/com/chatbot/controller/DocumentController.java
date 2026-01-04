package com.chatbot.controller;

import com.chatbot.entity.Document;
import com.chatbot.service.DocumentService;
import com.chatbot.service.DocumentService.DocumentUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for document upload and management.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    
    private final DocumentService documentService;
    
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    /**
     * Upload a document for a conversation.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversationId") String conversationId) {
        
        log.info("Uploading document: {} for conversation: {}", 
                 file.getOriginalFilename(), conversationId);
        
        // Validate conversationId
        if (conversationId == null || conversationId.trim().isEmpty()) {
            log.error("Upload failed: conversationId is required");
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Conversation ID is required"
            ));
        }
        
        try {
            Document document = documentService.uploadDocument(file, conversationId);
            return ResponseEntity.ok(toDocumentDto(document));
        } catch (DocumentUploadException e) {
            log.error("Upload failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error during upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error"
            ));
        }
    }
    
    /**
     * Get document status by ID.
     */
    @GetMapping("/{documentId}/status")
    public ResponseEntity<?> getDocumentStatus(@PathVariable UUID documentId) {
        return documentService.getDocument(documentId)
            .map(doc -> ResponseEntity.ok(Map.of(
                "id", doc.getId(),
                "status", doc.getStatus().name(),
                "chunkCount", doc.getChunkCount() != null ? doc.getChunkCount() : 0,
                "errorMessage", doc.getErrorMessage() != null ? doc.getErrorMessage() : ""
            )))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get document details by ID.
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocument(@PathVariable UUID documentId) {
        return documentService.getDocument(documentId)
            .map(doc -> ResponseEntity.ok(toDocumentDto(doc)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * List all documents for a conversation.
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<DocumentDto>> getDocumentsByConversation(
            @PathVariable String conversationId) {
        
        List<Document> documents = documentService.getDocumentsByConversation(conversationId);
        List<DocumentDto> dtos = documents.stream()
            .map(this::toDocumentDto)
            .toList();
        
        return ResponseEntity.ok(dtos);
    }
    
    /**
     * Get the full text content of a document.
     */
    @GetMapping("/{documentId}/content")
    public ResponseEntity<?> getDocumentContent(@PathVariable UUID documentId) {
        return documentService.getDocumentContent(documentId)
            .map(content -> ResponseEntity.ok(Map.of("content", content)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all document contents for a conversation (for study tools).
     */
    @GetMapping("/conversation/{conversationId}/content")
    public ResponseEntity<?> getAllDocumentContents(@PathVariable String conversationId) {
        String content = documentService.getAllDocumentContents(conversationId);
        return ResponseEntity.ok(Map.of("content", content));
    }

    /**
     * Delete a document.
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable UUID documentId) {
        try {
            documentService.deleteDocument(documentId);
            return ResponseEntity.ok(Map.of("deleted", true));
        } catch (Exception e) {
            log.error("Delete failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    private DocumentDto toDocumentDto(Document doc) {
        return new DocumentDto(
            doc.getId().toString(),
            doc.getOriginalFilename(),
            doc.getContentType(),
            doc.getFileSizeBytes(),
            doc.getStatus().name(),
            doc.getChunkCount() != null ? doc.getChunkCount() : 0,
            doc.getErrorMessage(),
            doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null,
            doc.getProcessedAt() != null ? doc.getProcessedAt().toString() : null
        );
    }
    
    /**
     * DTO for document responses.
     */
    public record DocumentDto(
        String id,
        String filename,
        String contentType,
        long fileSizeBytes,
        String status,
        int chunkCount,
        String errorMessage,
        String createdAt,
        String processedAt
    ) {}
}
