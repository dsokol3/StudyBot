package com.chatbot.service;

import com.chatbot.entity.Document;
import com.chatbot.entity.Document.DocumentStatus;
import com.chatbot.entity.DocumentChunk;
import com.chatbot.repository.DocumentChunkRepository;
import com.chatbot.repository.DocumentRepository;
import com.chatbot.service.ChunkingService.TextChunk;
import com.chatbot.service.DocumentParserService.DocumentParseException;
import com.chatbot.service.EmbeddingService.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Main service for document upload, processing, and management.
 * Handles file validation, storage, text extraction, chunking, and embedding generation.
 */
@Service
public class DocumentService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    
    @Value("${rag.upload.storage-path:./uploads}")
    private String storagePath;
    
    @Value("${rag.upload.max-file-size:52428800}")
    private long maxFileSize;
    
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentParserService parserService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    
    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            DocumentParserService parserService,
            ChunkingService chunkingService,
            EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.parserService = parserService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
    }
    
    /**
     * Upload and save a document, then trigger async processing.
     */
    @Transactional
    public Document uploadDocument(MultipartFile file, String conversationId) throws DocumentUploadException {
        // Validate file
        validateFile(file);
        
        try {
            // Generate safe filename and hash
            String originalFilename = file.getOriginalFilename();
            String safeFilename = UUID.randomUUID().toString() + getExtension(originalFilename);
            String fileHash = calculateHash(file.getInputStream());
            
            // Check for duplicate
            Optional<Document> existing = documentRepository.findByFileHashAndConversationId(fileHash, conversationId);
            if (existing.isPresent()) {
                log.info("Duplicate document detected: {}", originalFilename);
                return existing.get();
            }
            
            // Save file to storage
            Path uploadDir = Path.of(storagePath);
            Files.createDirectories(uploadDir);
            Path filePath = uploadDir.resolve(safeFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create document entity
            Document document = new Document();
            document.setConversationId(conversationId);
            document.setOriginalFilename(originalFilename);
            document.setSafeFilename(safeFilename);
            document.setContentType(file.getContentType());
            document.setFileSizeBytes(file.getSize());
            document.setFileHash(fileHash);
            document.setStatus(DocumentStatus.PENDING);
            
            document = documentRepository.save(document);
            log.info("Saved document: {} ({})", originalFilename, document.getId());
            
            // Trigger async processing
            processDocumentAsync(document.getId());
            
            return document;
            
        } catch (IOException e) {
            throw new DocumentUploadException("Failed to save file: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new DocumentUploadException("Failed to calculate file hash", e);
        }
    }
    
    /**
     * Process document asynchronously - extract text, chunk, and generate embeddings.
     */
    @Async
    @Transactional
    public void processDocumentAsync(UUID documentId) {
        log.info("Starting async processing for document: {}", documentId);
        
        @SuppressWarnings("null")
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.error("Document not found: {}", documentId);
            return;
        }
        
        try {
            // Update status to processing
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);
            
            // Extract text
            Path filePath = Path.of(storagePath, document.getSafeFilename());
            String text;
            try (InputStream is = Files.newInputStream(filePath)) {
                text = parserService.extractText(is, document.getOriginalFilename());
            }
            
            if (text == null || text.isBlank()) {
                throw new DocumentParseException("No text content extracted", null);
            }
            
            // Chunk text
            List<TextChunk> chunks = chunkingService.chunkText(text);
            log.info("Document {} chunked into {} pieces", documentId, chunks.size());
            
            // Generate embeddings and save chunks
            for (TextChunk chunk : chunks) {
                float[] embedding = embeddingService.generateEmbedding(chunk.content());
                
                DocumentChunk chunkEntity = new DocumentChunk();
                chunkEntity.setDocument(document);
                chunkEntity.setChunkOrder(chunk.order());
                chunkEntity.setContent(chunk.content());
                chunkEntity.setTokenCount(chunk.tokenCount());
                chunkEntity.setEmbedding(embedding);
                
                chunkRepository.save(chunkEntity);
            }
            
            // Update document status
            document.setStatus(DocumentStatus.COMPLETED);
            document.setChunkCount(chunks.size());
            document.setProcessedAt(LocalDateTime.now());
            documentRepository.save(document);
            
            log.info("Document {} processed successfully with {} chunks", documentId, chunks.size());
            
        } catch (DocumentParseException | EmbeddingException | IOException e) {
            log.error("Failed to process document {}: {}", documentId, e.getMessage());
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
        }
    }
    
    /**
     * Get document by ID.
     */
    public Optional<Document> getDocument(UUID documentId) {
        @SuppressWarnings("null")
        var result = documentRepository.findById(documentId);
        return result;
    }
    
    /**
     * Get all documents for a conversation.
     */
    public List<Document> getDocumentsByConversation(String conversationId) {
        return documentRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
    }
    
    /**
     * Get the full text content of a document by combining all its chunks.
     */
    @Transactional(readOnly = true)
    public Optional<String> getDocumentContent(UUID documentId) {
        try {
            @SuppressWarnings("null")
            var docOptional = documentRepository.findById(documentId);
            return docOptional
                .filter(doc -> doc.getStatus() == DocumentStatus.COMPLETED)
                .map(doc -> {
                    List<String> contents = chunkRepository.findContentByDocumentIdOrderByChunkOrderAsc(documentId);
                    log.info("Found {} chunks for document {}", contents.size(), documentId);
                    return String.join("\n\n", contents).trim();
                });
        } catch (Exception e) {
            log.error("Error getting document content for {}: {}", documentId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Get all document contents for a conversation.
     */
    @Transactional(readOnly = true)
    public String getAllDocumentContents(String conversationId) {
        try {
            List<Document> documents = getDocumentsByConversation(conversationId);
            log.info("Found {} documents for conversation {}", documents.size(), conversationId);
            StringBuilder content = new StringBuilder();
            
            for (Document doc : documents) {
                if (doc.getStatus() == DocumentStatus.COMPLETED) {
                    List<String> chunkContents = chunkRepository.findContentByDocumentIdOrderByChunkOrderAsc(doc.getId());
                    log.info("Document {} has {} chunks", doc.getId(), chunkContents.size());
                    for (String chunkContent : chunkContents) {
                        content.append(chunkContent).append("\n\n");
                    }
                    content.append("---\n\n");
                }
            }
            
            return content.toString().trim();
        } catch (Exception e) {
            log.error("Error getting document contents for conversation {}: {}", conversationId, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Delete a document and its chunks.
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        @SuppressWarnings("null")
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document != null) {
            // Delete file from storage
            try {
                Path filePath = Path.of(storagePath, document.getSafeFilename());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Failed to delete file: {}", e.getMessage());
            }
            
            // Delete from database (chunks will cascade delete)
            documentRepository.delete(document);
            log.info("Deleted document: {}", documentId);
        }
    }
    
    private void validateFile(MultipartFile file) throws DocumentUploadException {
        if (file == null || file.isEmpty()) {
            throw new DocumentUploadException("No file provided");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new DocumentUploadException(
                String.format("File too large. Maximum size is %d MB", maxFileSize / (1024 * 1024))
            );
        }
        
        String contentType = file.getContentType();
        if (!parserService.isSupportedType(contentType)) {
            throw new DocumentUploadException(
                "Unsupported file type: " + contentType + 
                ". Supported types: PDF, DOCX, TXT, MD"
            );
        }
    }
    
    private String calculateHash(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    
    private String getExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(dotIndex);
        }
        return "";
    }
    
    public static class DocumentUploadException extends Exception {
        public DocumentUploadException(String message) {
            super(message);
        }
        
        public DocumentUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
