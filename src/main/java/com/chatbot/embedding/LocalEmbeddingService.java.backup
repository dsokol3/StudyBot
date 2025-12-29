package com.chatbot.embedding;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local embedding service for generating text embeddings.
 * 
 * Features:
 * - Runs fully locally with CPU support
 * - Hash-based deterministic embeddings (fast and consistent)
 * - Caching to avoid re-computing embeddings
 * - No external API calls required
 * 
 * Note: This implementation uses a hash-based approach for speed and simplicity.
 * For production with semantic similarity, integrate a proper embedding model.
 * 
 * Embedding dimension: 384 (compatible with all-MiniLM-L6-v2)
 */
@Service
public class LocalEmbeddingService {
    
    private static final Logger log = LoggerFactory.getLogger(LocalEmbeddingService.class);
    
    // Embedding dimension (compatible with all-MiniLM-L6-v2)
    public static final int EMBEDDING_DIMENSION = 384;
    
    @Value("${rag.embedding.cache-enabled:true}")
    private boolean cacheEnabled;
    
    // In-memory cache: hash -> embedding
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();
    
    // Statistics for logging
    private long totalEmbeddings = 0;
    private long cacheHits = 0;
    
    @PostConstruct
    public void initialize() {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  🚀 Initializing Local Embedding Service                      ║");
        log.info("║  Mode: Hash-based embeddings (384 dimensions)                ║");
        log.info("║  Cache: {}                                                    ║", cacheEnabled ? "Enabled" : "Disabled");
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("🧹 Cleaning up Local Embedding Service...");
        log.info("✅ Stats: {} embeddings generated, {} cache hits ({}% hit rate)",
                totalEmbeddings, cacheHits, 
                totalEmbeddings > 0 ? (cacheHits * 100 / totalEmbeddings) : 0);
    }
    
    /**
     * Generate embedding for a single text.
     * Uses cache if available.
     * 
     * @param text The text to embed
     * @return 384-dimensional float array
     */
    public float[] generateEmbedding(String text) throws EmbeddingException {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Cannot generate embedding for empty text");
        }
        
        totalEmbeddings++;
        
        // Check cache first
        String textHash = computeHash(text);
        if (cacheEnabled) {
            float[] cached = embeddingCache.get(textHash);
            if (cached != null) {
                cacheHits++;
                log.debug("📎 Cache HIT for text hash: {} (hit rate: {}%)", 
                         textHash.substring(0, 8), (cacheHits * 100 / totalEmbeddings));
                return cached;
            }
        }
        
        log.debug("🔄 Generating embedding for text of length {} chars", text.length());
        long startTime = System.currentTimeMillis();
        
        // Generate hash-based embedding
        float[] embedding = generateHashBasedEmbedding(text, textHash);
        
        // Cache the result
        if (cacheEnabled) {
            embeddingCache.put(textHash, embedding);
            log.debug("💾 Cached embedding for hash: {}", textHash.substring(0, 8));
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("✅ Embedding generated in {} ms", elapsed);
        
        return embedding;
    }
    
    /**
     * Generate hash-based embedding.
     * Uses SHA-256 to create deterministic pseudo-random vectors.
     * 
     * This approach:
     * - Is fast (pure CPU, no ML inference)
     * - Is deterministic (same text always produces same embedding)
     * - Works well for exact/near-exact matching
     * 
     * Limitation: Does not capture semantic similarity between different texts.
     */
    private float[] generateHashBasedEmbedding(String text, String textHash) {
        float[] embedding = new float[EMBEDDING_DIMENSION];
        
        // Normalize text for better matching
        String normalizedText = text.toLowerCase().trim()
            .replaceAll("\\s+", " ")
            .replaceAll("[^a-z0-9\\s]", "");
        
        // Split into words and generate word-level features
        String[] words = normalizedText.split("\\s+");
        
        // Use multiple hash seeds for diversity
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // Generate base embedding from full text
            byte[] fullHash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < Math.min(32, EMBEDDING_DIMENSION); i++) {
                embedding[i] = (fullHash[i % 32] & 0xFF) / 127.5f - 1.0f;
            }
            
            // Add word-level features (bag of words style)
            for (int w = 0; w < words.length; w++) {
                String word = words[w];
                byte[] wordHash = md.digest((word + "_" + w).getBytes(StandardCharsets.UTF_8));
                for (int i = 0; i < 32; i++) {
                    int idx = (32 + w * 32 + i) % EMBEDDING_DIMENSION;
                    embedding[idx] += (wordHash[i] & 0xFF) / 255.0f / words.length;
                }
            }
            
            // Fill remaining dimensions with character n-gram features
            for (int i = 0; i < normalizedText.length() - 2; i++) {
                String trigram = normalizedText.substring(i, i + 3);
                int idx = Math.abs(trigram.hashCode()) % EMBEDDING_DIMENSION;
                embedding[idx] += 0.1f;
            }
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback: use simple hash
            Random random = new Random(text.hashCode());
            for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                embedding[i] = (random.nextFloat() * 2) - 1;
            }
        }
        
        // L2 normalize
        return l2Normalize(embedding);
    }
    
    /**
     * L2 normalize the embedding vector.
     */
    private float[] l2Normalize(float[] embedding) {
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
        }
        
        return embedding;
    }
    
    /**
     * Generate embeddings for multiple texts.
     */
    public List<float[]> generateEmbeddings(List<String> texts) throws EmbeddingException {
        log.info("📦 Generating embeddings for {} texts...", texts.size());
        long startTime = System.currentTimeMillis();
        
        List<float[]> embeddings = new ArrayList<>();
        int newEmbeddings = 0;
        int cachedEmbeddings = 0;
        
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            String textHash = computeHash(text);
            
            if (cacheEnabled && embeddingCache.containsKey(textHash)) {
                embeddings.add(embeddingCache.get(textHash));
                cachedEmbeddings++;
            } else {
                embeddings.add(generateEmbedding(text));
                newEmbeddings++;
            }
            
            if ((i + 1) % 10 == 0) {
                log.debug("   Progress: {}/{} embeddings", i + 1, texts.size());
            }
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("✅ Generated {} embeddings in {} ms ({} cached, {} new)", 
                 texts.size(), elapsed, cachedEmbeddings, newEmbeddings);
        
        return embeddings;
    }
    
    /**
     * Check if embedding exists in cache for given hash.
     */
    public boolean hasCachedEmbedding(String contentHash) {
        return embeddingCache.containsKey(contentHash);
    }
    
    /**
     * Get cached embedding by content hash.
     */
    public Optional<float[]> getCachedEmbedding(String contentHash) {
        return Optional.ofNullable(embeddingCache.get(contentHash));
    }
    
    /**
     * Clear the embedding cache.
     */
    public void clearCache() {
        int size = embeddingCache.size();
        embeddingCache.clear();
        log.info("🗑️  Cleared {} cached embeddings", size);
    }
    
    /**
     * Compute SHA-256 hash for text (used for caching).
     */
    public String computeHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return Integer.toHexString(text.hashCode());
        }
    }
    
    /**
     * Convert embedding to vector string format for database storage.
     */
    public String embeddingToVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Get the embedding dimension.
     */
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSION;
    }
    
    /**
     * Check if the model is loaded and ready.
     */
    public boolean isModelLoaded() {
        return true; // Hash-based embeddings are always ready
    }
    
    /**
     * Get cache statistics.
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", embeddingCache.size());
        stats.put("totalEmbeddings", totalEmbeddings);
        stats.put("cacheHits", cacheHits);
        stats.put("hitRate", totalEmbeddings > 0 ? (double) cacheHits / totalEmbeddings : 0);
        stats.put("modelLoaded", true);
        stats.put("mode", "hash-based");
        return stats;
    }
    
    /**
     * Exception for embedding generation failures.
     */
    public static class EmbeddingException extends Exception {
        public EmbeddingException(String message) {
            super(message);
        }
        
        public EmbeddingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
