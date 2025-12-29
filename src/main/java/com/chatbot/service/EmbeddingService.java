package com.chatbot.service;

import com.chatbot.embedding.GeminiEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for generating text embeddings.
 * 
 * UPDATED: Now uses Google Gemini Embeddings API (text-embedding-004)
 * This provides:
 * - High-quality semantic embeddings (768 dimensions)
 * - Fast cloud inference
 * - No local model management required
 * - Consistent embeddings across restarts (with caching)
 * 
 * The embedding dimension is 768 (Gemini text-embedding-004).
 */
@Service
public class EmbeddingService {
    
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    
    @Value("${rag.embedding.dimension:768}")
    private int embeddingDimension;
    
    private final GeminiEmbeddingService geminiEmbeddingService;
    
    public EmbeddingService(GeminiEmbeddingService geminiEmbeddingService) {
        this.geminiEmbeddingService = geminiEmbeddingService;
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  📦 EmbeddingService initialized with Gemini API             ║");
        log.info("║  Model: text-embedding-004 (768 dimensions)                  ║");
        log.info("║  Mode: Cloud API (Google Gemini)                             ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Generate embedding for a single text using Gemini API.
     * 
     * @param text The text to embed
     * @return Float array of embedding values (768 dimensions)
     * @throws EmbeddingException if embedding generation fails
     */
    public float[] generateEmbedding(String text) throws EmbeddingException {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Cannot generate embedding for empty text");
        }
        
        try {
            log.debug("🔄 Generating Gemini embedding for text of length {}", text.length());
            long startTime = System.currentTimeMillis();
            
            float[] embedding = geminiEmbeddingService.generateEmbedding(text);
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("✅ Gemini embedding generated in {} ms (dimension: {})", 
                      elapsed, embedding.length);
            
            return embedding;
            
        } catch (GeminiEmbeddingService.EmbeddingException e) {
            throw new EmbeddingException("Gemini embedding failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate embeddings for multiple texts in batch.
     * 
     * @param texts List of texts to embed
     * @return List of embedding arrays
     * @throws EmbeddingException if embedding generation fails
     */
    public List<float[]> generateEmbeddings(List<String> texts) throws EmbeddingException {
        log.info("📦 Generating Gemini embeddings for {} texts...", texts.size());
        long startTime = System.currentTimeMillis();
        
        try {
            List<float[]> embeddings = geminiEmbeddingService.generateEmbeddings(texts);
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ Generated {} Gemini embeddings in {} ms", embeddings.size(), elapsed);
            
            return embeddings;
            
        } catch (GeminiEmbeddingService.EmbeddingException e) {
            throw new EmbeddingException("Batch embedding failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert embedding array to pgvector-compatible string format.
     * Format: [0.1, 0.2, 0.3, ...]
     */
    public String embeddingToVectorString(float[] embedding) {
        return geminiEmbeddingService.embeddingToVectorString(embedding);
    }
    
    /**
     * Compute SHA-256 hash for content (used for caching).
     */
    public String computeContentHash(String content) {
        return geminiEmbeddingService.computeHash(content);
    }
    
    /**
     * Check if embedding exists in cache for given content hash.
     */
    public boolean hasCachedEmbedding(String contentHash) {
        return geminiEmbeddingService.hasCachedEmbedding(contentHash);
    }
    
    public int getEmbeddingDimension() {
        return GeminiEmbeddingService.EMBEDDING_DIMENSION;
    }
    
    /**
     * Check if the Gemini API is configured and ready.
     */
    public boolean isModelReady() {
        return geminiEmbeddingService.isModelLoaded();
    }
    
    public static class EmbeddingException extends Exception {
        public EmbeddingException(String message) {
            super(message);
        }
        
        public EmbeddingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
