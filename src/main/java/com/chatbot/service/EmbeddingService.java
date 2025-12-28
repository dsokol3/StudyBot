package com.chatbot.service;

import com.chatbot.embedding.LocalEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for generating text embeddings.
 * 
 * UPDATED: Now uses LOCAL HuggingFace embeddings (all-MiniLM-L6-v2)
 * instead of Ollama API. This provides:
 * - Faster inference (no network latency)
 * - No dependency on external services
 * - Consistent embeddings across restarts (with caching)
 * 
 * The embedding dimension is 384 (all-MiniLM-L6-v2 default).
 */
@Service
public class EmbeddingService {
    
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    
    @Value("${rag.embedding.dimension:384}")
    private int embeddingDimension;
    
    private final LocalEmbeddingService localEmbeddingService;
    
    public EmbeddingService(LocalEmbeddingService localEmbeddingService) {
        this.localEmbeddingService = localEmbeddingService;
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  📦 EmbeddingService initialized with LOCAL embeddings       ║");
        log.info("║  Model: all-MiniLM-L6-v2 (384 dimensions)                    ║");
        log.info("║  Mode: CPU inference (no external API)                       ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Generate embedding for a single text using local model.
     * 
     * @param text The text to embed
     * @return Float array of embedding values (384 dimensions)
     * @throws EmbeddingException if embedding generation fails
     */
    public float[] generateEmbedding(String text) throws EmbeddingException {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Cannot generate embedding for empty text");
        }
        
        try {
            log.debug("🔄 Generating local embedding for text of length {}", text.length());
            long startTime = System.currentTimeMillis();
            
            float[] embedding = localEmbeddingService.generateEmbedding(text);
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("✅ Local embedding generated in {} ms (dimension: {})", 
                      elapsed, embedding.length);
            
            return embedding;
            
        } catch (LocalEmbeddingService.EmbeddingException e) {
            throw new EmbeddingException("Local embedding failed: " + e.getMessage(), e);
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
        log.info("📦 Generating local embeddings for {} texts...", texts.size());
        long startTime = System.currentTimeMillis();
        
        try {
            List<float[]> embeddings = localEmbeddingService.generateEmbeddings(texts);
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ Generated {} local embeddings in {} ms", embeddings.size(), elapsed);
            
            return embeddings;
            
        } catch (LocalEmbeddingService.EmbeddingException e) {
            throw new EmbeddingException("Batch embedding failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert embedding array to pgvector-compatible string format.
     * Format: [0.1, 0.2, 0.3, ...]
     */
    public String embeddingToVectorString(float[] embedding) {
        return localEmbeddingService.embeddingToVectorString(embedding);
    }
    
    /**
     * Compute SHA-256 hash for content (used for caching).
     */
    public String computeContentHash(String content) {
        return localEmbeddingService.computeHash(content);
    }
    
    /**
     * Check if embedding exists in cache for given content hash.
     */
    public boolean hasCachedEmbedding(String contentHash) {
        return localEmbeddingService.hasCachedEmbedding(contentHash);
    }
    
    public int getEmbeddingDimension() {
        return LocalEmbeddingService.EMBEDDING_DIMENSION;
    }
    
    /**
     * Check if the local model is loaded and ready.
     */
    public boolean isModelReady() {
        return localEmbeddingService.isModelLoaded();
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
