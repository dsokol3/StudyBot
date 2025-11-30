package com.chatbot.service;

import com.chatbot.entity.Document;
import com.chatbot.entity.DocumentChunk;
import com.chatbot.repository.DocumentChunkRepository;
import com.chatbot.repository.DocumentRepository;
import com.chatbot.service.EmbeddingService.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving relevant document chunks based on semantic similarity.
 * Implements the retrieval part of RAG (Retrieval-Augmented Generation).
 */
@Service
public class RetrievalService {
    
    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);
    
    @Value("${rag.retrieval.top-k:5}")
    private int topK;
    
    @Value("${rag.retrieval.max-distance:0.5}")
    private double maxDistance;
    
    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;
    
    public RetrievalService(
            DocumentChunkRepository chunkRepository,
            DocumentRepository documentRepository,
            EmbeddingService embeddingService) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
    }
    
    /**
     * Find relevant document chunks for a query within a conversation's documents.
     * 
     * @param query The user's query
     * @param conversationId The conversation ID to search within
     * @return List of relevant chunks with metadata
     */
    public List<RetrievedChunk> findRelevantChunks(String query, String conversationId) {
        try {
            // Generate embedding for the query
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String vectorString = embeddingService.embeddingToVectorString(queryEmbedding);
            
            // Search for similar chunks
            List<DocumentChunk> chunks = chunkRepository.findSimilarChunks(
                conversationId,
                vectorString,
                maxDistance,
                topK
            );
            
            log.info("Found {} relevant chunks for query in conversation {}", 
                     chunks.size(), conversationId);
            
            return chunks.stream()
                .map(this::toRetrievedChunk)
                .collect(Collectors.toList());
                
        } catch (EmbeddingException e) {
            log.error("Failed to generate query embedding: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Find relevant chunks across all documents (global knowledge base).
     */
    public List<RetrievedChunk> findRelevantChunksGlobal(String query) {
        try {
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String vectorString = embeddingService.embeddingToVectorString(queryEmbedding);
            
            List<DocumentChunk> chunks = chunkRepository.findSimilarChunksGlobal(
                vectorString,
                maxDistance,
                topK
            );
            
            log.info("Found {} relevant chunks globally for query", chunks.size());
            
            return chunks.stream()
                .map(this::toRetrievedChunk)
                .collect(Collectors.toList());
                
        } catch (EmbeddingException e) {
            log.error("Failed to generate query embedding: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Build context string from retrieved chunks for injection into LLM prompt.
     */
    public String buildContext(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("RELEVANT DOCUMENTS:\n\n");
        
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            context.append(String.format("[Source %d: %s]\n", i + 1, chunk.documentName()));
            context.append(chunk.content());
            context.append("\n\n");
        }
        
        return context.toString();
    }
    
    /**
     * Build citation references for the response.
     */
    public List<Citation> buildCitations(List<RetrievedChunk> chunks) {
        List<Citation> citations = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            citations.add(new Citation(
                i + 1,
                chunk.documentId().toString(),
                chunk.documentName(),
                chunk.chunkOrder()
            ));
        }
        
        return citations;
    }
    
    private RetrievedChunk toRetrievedChunk(DocumentChunk chunk) {
        Document doc = chunk.getDocument();
        return new RetrievedChunk(
            chunk.getId(),
            doc.getId(),
            doc.getOriginalFilename(),
            chunk.getChunkOrder(),
            chunk.getContent(),
            chunk.getTokenCount()
        );
    }
    
    /**
     * Check if a conversation has any indexed documents.
     */
    public boolean hasDocuments(String conversationId) {
        return documentRepository.countByConversationId(conversationId) > 0;
    }
    
    /**
     * Represents a retrieved chunk with document metadata.
     */
    public record RetrievedChunk(
        java.util.UUID chunkId,
        java.util.UUID documentId,
        String documentName,
        int chunkOrder,
        String content,
        int tokenCount
    ) {}
    
    /**
     * Represents a citation reference.
     */
    public record Citation(
        int index,
        String documentId,
        String documentName,
        int chunkOrder
    ) {}
}
