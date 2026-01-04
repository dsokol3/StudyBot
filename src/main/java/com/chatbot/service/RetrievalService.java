package com.chatbot.service;

import com.chatbot.config.PostgresInitializer;
import com.chatbot.entity.Document;
import com.chatbot.entity.Document.DocumentStatus;
import com.chatbot.entity.DocumentChunk;
import com.chatbot.repository.DocumentChunkRepository;
import com.chatbot.repository.DocumentRepository;
import com.chatbot.service.EmbeddingService.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving relevant document chunks based on semantic similarity.
 * Implements the retrieval part of RAG (Retrieval-Augmented Generation).
 * 
 * Supports multiple retrieval strategies:
 * - pgvector similarity search (when available)
 * - In-memory similarity calculation (fallback when pgvector unavailable)
 * - Dev mode fallback (returns all chunks)
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
    private final Environment environment;
    
    public RetrievalService(
            DocumentChunkRepository chunkRepository,
            DocumentRepository documentRepository,
            EmbeddingService embeddingService,
            Environment environment) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.embeddingService = embeddingService;
        this.environment = environment;
    }
    
    /**
     * Check if running in dev mode (H2 database without vector support).
     */
    private boolean isDevMode() {
        String[] activeProfiles = environment.getActiveProfiles();
        return Arrays.asList(activeProfiles).contains("dev");
    }
    
    /**
     * Check if pgvector is available for native similarity search.
     */
    private boolean isPgvectorAvailable() {
        return PostgresInitializer.isPgvectorAvailable();
    }
    
    /**
     * Find relevant document chunks for a query within a conversation's documents.
     * 
     * @param query The user's query
     * @param conversationId The conversation ID to search within
     * @return List of relevant chunks with metadata
     */
    public List<RetrievedChunk> findRelevantChunks(String query, String conversationId) {
        // In dev mode (H2), use fallback that returns all chunks
        if (isDevMode()) {
            log.info("Dev mode: Using fallback chunk retrieval (no vector search)");
            return findAllChunksForConversation(conversationId);
        }
        
        // If pgvector is available, use native vector search
        if (isPgvectorAvailable()) {
            return findRelevantChunksWithPgvector(query, conversationId);
        } else {
            // Fallback: Load chunks and calculate similarity in memory
            log.info("pgvector unavailable: Using in-memory similarity calculation");
            return findRelevantChunksInMemory(query, conversationId);
        }
    }
    
    /**
     * Find relevant chunks using pgvector native similarity search.
     */
    private List<RetrievedChunk> findRelevantChunksWithPgvector(String query, String conversationId) {
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
            
            log.info("Found {} relevant chunks for query in conversation {} (pgvector)", 
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
     * Find relevant chunks by loading all chunks and calculating similarity in memory.
     * This is used when pgvector is not available (e.g., Render free tier).
     */
    private List<RetrievedChunk> findRelevantChunksInMemory(String query, String conversationId) {
        try {
            // Generate embedding for the query
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            
            // Get all completed documents for this conversation
            List<Document> documents = documentRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
            List<ScoredChunk> scoredChunks = new ArrayList<>();
            
            for (Document doc : documents) {
                if (doc.getStatus() == DocumentStatus.COMPLETED) {
                    // Get all chunks for this document
                    List<DocumentChunk> chunks = chunkRepository.findByDocumentIdOrderByChunkOrderAsc(doc.getId());
                    
                    for (DocumentChunk chunk : chunks) {
                        float[] chunkEmbedding = chunk.getEmbedding();
                        if (chunkEmbedding != null && chunkEmbedding.length > 0) {
                            double similarity = cosineSimilarity(queryEmbedding, chunkEmbedding);
                            scoredChunks.add(new ScoredChunk(chunk, similarity));
                        }
                    }
                }
            }
            
            // Sort by similarity (highest first) and take top K
            List<RetrievedChunk> relevantChunks = scoredChunks.stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .map(sc -> toRetrievedChunk(sc.chunk()))
                .collect(Collectors.toList());
            
            log.info("Found {} relevant chunks for query in conversation {} (in-memory)", 
                     relevantChunks.size(), conversationId);
            
            return relevantChunks;
                
        } catch (EmbeddingException e) {
            log.error("Failed to generate query embedding: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Fallback for dev mode: Returns all chunks from completed documents in the conversation.
     * Limited to topK chunks to avoid overwhelming the context.
     */
    private List<RetrievedChunk> findAllChunksForConversation(String conversationId) {
        List<RetrievedChunk> allChunks = new ArrayList<>();
        
        List<Document> documents = documentRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
        log.info("Found {} documents for conversation {} in dev mode", documents.size(), conversationId);
        
        for (Document doc : documents) {
            if (doc.getStatus() == DocumentStatus.COMPLETED) {
                List<String> contents = chunkRepository.findContentByDocumentIdOrderByChunkOrderAsc(doc.getId());
                log.info("Document {} has {} chunks", doc.getOriginalFilename(), contents.size());
                
                for (int i = 0; i < contents.size() && allChunks.size() < topK; i++) {
                    allChunks.add(new RetrievedChunk(
                        null, // No chunk ID needed for dev mode
                        doc.getId(),
                        doc.getOriginalFilename(),
                        i,
                        contents.get(i),
                        0 // Token count not tracked in dev mode
                    ));
                }
            }
            
            if (allChunks.size() >= topK) {
                break;
            }
        }
        
        log.info("Returning {} chunks in dev mode fallback", allChunks.size());
        return allChunks;
    }
    
    /**
     * Find relevant chunks across all documents (global knowledge base).
     */
    public List<RetrievedChunk> findRelevantChunksGlobal(String query) {
        // In dev mode (H2), return empty list for global search
        if (isDevMode()) {
            log.info("Dev mode: Global vector search not supported with H2.");
            return List.of();
        }
        
        // If pgvector is available, use native vector search
        if (isPgvectorAvailable()) {
            return findRelevantChunksGlobalWithPgvector(query);
        } else {
            // Fallback: This would require loading ALL chunks from ALL documents
            // For now, return empty list to avoid performance issues
            log.info("pgvector unavailable: Global search not supported in fallback mode");
            return List.of();
        }
    }
    
    /**
     * Find relevant chunks globally using pgvector.
     */
    private List<RetrievedChunk> findRelevantChunksGlobalWithPgvector(String query) {
        try {
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            String vectorString = embeddingService.embeddingToVectorString(queryEmbedding);
            
            List<DocumentChunk> chunks = chunkRepository.findSimilarChunksGlobal(
                vectorString,
                maxDistance,
                topK
            );
            
            log.info("Found {} relevant chunks globally for query (pgvector)", chunks.size());
            
            return chunks.stream()
                .map(this::toRetrievedChunk)
                .collect(Collectors.toList());
                
        } catch (EmbeddingException e) {
            log.error("Failed to generate query embedding: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Calculate cosine similarity between two vectors.
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
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
    
    /**
     * Internal record for scoring chunks during in-memory similarity calculation.
     */
    private record ScoredChunk(DocumentChunk chunk, double score) {}
}
