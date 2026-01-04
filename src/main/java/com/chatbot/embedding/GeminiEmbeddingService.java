package com.chatbot.embedding;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Google Gemini API embedding service for generating text embeddings.
 * 
 * Features:
 * - Uses Google Gemini Embeddings API (text-embedding-004)
 * - Supports 768-dimensional embeddings
 * - Caching to reduce API calls
 * - Proper error handling and retries
 * - Secure API key management
 * 
 * API Documentation: https://ai.google.dev/api/embeddings
 */
@Service
public class GeminiEmbeddingService {
    
    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingService.class);
    
    // Gemini text-embedding-004 produces 768-dimensional embeddings
    public static final int EMBEDDING_DIMENSION = 768;
    
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String EMBEDDING_MODEL = "text-embedding-004";
    
    @Value("${gemini.api.key:}")
    private String apiKey;
    
    @Value("${rag.embedding.cache-enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${gemini.api.max-retries:3}")
    private int maxRetries;
    
    @Value("${gemini.api.timeout-seconds:30}")
    private int timeoutSeconds;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // In-memory cache: hash -> embedding
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();
    
    // Statistics for logging
    private long totalEmbeddings = 0;
    private long cacheHits = 0;
    private long apiCalls = 0;
    private long apiErrors = 0;
    
    public GeminiEmbeddingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void initialize() {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  🚀 Initializing Google Gemini Embedding Service             ║");
        log.info("║  Model: {} (768 dimensions)                  ║", EMBEDDING_MODEL);
        log.info("║  Cache: {}                                                   ║", cacheEnabled ? "Enabled" : "Disabled");
        log.info("║  Max Retries: {}                                             ║", maxRetries);
        log.info("╚══════════════════════════════════════════════════════════════╝");
        
        // Enhanced logging to debug environment variable issues
        String envVarValue = System.getenv("GEMINI_API_KEY");
        log.info("🔍 Debug: GEMINI_API_KEY env var = {}", 
            envVarValue == null ? "null" : (envVarValue.isBlank() ? "blank" : "present (length: " + envVarValue.length() + ")"));
        log.info("🔍 Debug: Injected apiKey = {}", 
            apiKey == null ? "null" : (apiKey.isBlank() ? "blank" : "present (length: " + apiKey.length() + ")"));
        
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️  WARNING: Gemini API key is not configured!");
            log.warn("⚠️  Please set 'GEMINI_API_KEY' environment variable");
            log.warn("⚠️  Or set 'gemini.api.key' in application.properties");
        } else {
            log.info("✅ Gemini API key configured (length: {})", apiKey.length());
        }
    }
    
    @PreDestroy
    public void cleanup() {
        log.info("🧹 Cleaning up Google Gemini Embedding Service...");
        log.info("✅ Stats: {} embeddings generated, {} cache hits ({}% hit rate)",
                totalEmbeddings, cacheHits, 
                totalEmbeddings > 0 ? (cacheHits * 100 / totalEmbeddings) : 0);
        log.info("✅ API Stats: {} API calls, {} errors ({}% error rate)",
                apiCalls, apiErrors,
                apiCalls > 0 ? (apiErrors * 100 / apiCalls) : 0);
    }
    
    /**
     * Generate embedding for a single text.
     * Uses cache if available, otherwise calls Gemini API.
     * 
     * @param text The text to embed
     * @return 768-dimensional float array
     */
    public float[] generateEmbedding(String text) throws EmbeddingException {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Cannot generate embedding for empty text");
        }
        
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmbeddingException("Gemini API key is not configured");
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
        
        log.debug("🔄 Generating Gemini embedding for text of length {} chars", text.length());
        long startTime = System.currentTimeMillis();
        
        // Call Gemini API with retries
        float[] embedding = callGeminiAPIWithRetry(text);
        
        // Cache the result
        if (cacheEnabled && embedding != null) {
            embeddingCache.put(textHash, embedding);
            log.debug("💾 Cached embedding for hash: {}", textHash.substring(0, 8));
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        log.debug("✅ Gemini embedding generated in {} ms", elapsed);
        
        return embedding;
    }
    
    /**
     * Call Gemini API with retry logic.
     */
    private float[] callGeminiAPIWithRetry(String text) throws EmbeddingException {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                attempt++;
                if (attempt > 1) {
                    log.debug("🔄 Retry attempt {}/{} for Gemini API", attempt, maxRetries);
                }
                
                float[] embedding = callGeminiAPI(text);
                
                if (attempt > 1) {
                    log.info("✅ Gemini API call succeeded on attempt {}", attempt);
                }
                
                return embedding;
                
            } catch (Exception e) {
                lastException = e;
                apiErrors++;
                log.warn("⚠️  Gemini API call failed (attempt {}/{}): {}", 
                        attempt, maxRetries, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        // Exponential backoff: 1s, 2s, 4s, etc.
                        long backoffMs = (long) Math.pow(2, attempt - 1) * 1000;
                        log.debug("⏳ Waiting {} ms before retry...", backoffMs);
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new EmbeddingException("Interrupted during retry backoff", ie);
                    }
                }
            }
        }
        
        throw new EmbeddingException(
            "Failed to generate embedding after " + maxRetries + " attempts: " + 
            (lastException != null ? lastException.getMessage() : "Unknown error"),
            lastException
        );
    }
    
    /**
     * Call Gemini API to generate embedding.
     */
    private float[] callGeminiAPI(String text) throws Exception {
        apiCalls++;
        
        // Build the API URL
        String url = String.format("%s/%s:embedContent?key=%s", 
                                   GEMINI_API_BASE_URL, EMBEDDING_MODEL, apiKey);
        
        // Build request body
        GeminiEmbedRequest request = new GeminiEmbedRequest();
        request.content = new GeminiContent();
        request.content.parts = new GeminiPart[] { new GeminiPart(text) };
        request.model = "models/" + EMBEDDING_MODEL;
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Make request
        HttpEntity<GeminiEmbedRequest> entity = new HttpEntity<>(request, headers);
        
        try {
            log.debug("📡 Calling Gemini API: {}", url.replace(apiKey, "***"));
            
            ResponseEntity<GeminiEmbedResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                GeminiEmbedResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                GeminiEmbedResponse responseBody = response.getBody();
                
                if (responseBody.embedding != null && responseBody.embedding.values != null) {
                    List<Double> values = responseBody.embedding.values;
                    float[] embedding = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        embedding[i] = values.get(i).floatValue();
                    }
                    
                    log.debug("✅ Received embedding with {} dimensions", embedding.length);
                    return embedding;
                } else {
                    throw new Exception("Invalid response structure from Gemini API");
                }
            } else {
                throw new Exception("Gemini API returned status: " + response.getStatusCode());
            }
            
        } catch (RestClientException e) {
            log.error("❌ Gemini API call failed: {}", e.getMessage());
            throw new Exception("Gemini API call failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate embeddings for multiple texts.
     */
    public List<float[]> generateEmbeddings(List<String> texts) throws EmbeddingException {
        log.info("📦 Generating Gemini embeddings for {} texts...", texts.size());
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
     * Check if the API is configured and ready.
     */
    public boolean isModelLoaded() {
        return apiKey != null && !apiKey.isBlank();
    }
    
    /**
     * Get cache and API statistics.
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", embeddingCache.size());
        stats.put("totalEmbeddings", totalEmbeddings);
        stats.put("cacheHits", cacheHits);
        stats.put("hitRate", totalEmbeddings > 0 ? (double) cacheHits / totalEmbeddings : 0);
        stats.put("apiCalls", apiCalls);
        stats.put("apiErrors", apiErrors);
        stats.put("errorRate", apiCalls > 0 ? (double) apiErrors / apiCalls : 0);
        stats.put("modelLoaded", isModelLoaded());
        stats.put("mode", "gemini-api");
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
    
    // ========== Gemini API Request/Response DTOs ==========
    
    static class GeminiEmbedRequest {
        public String model;
        public GeminiContent content;
    }
    
    static class GeminiContent {
        public GeminiPart[] parts;
    }
    
    static class GeminiPart {
        public String text;
        
        public GeminiPart(String text) {
            this.text = text;
        }
    }
    
    static class GeminiEmbedResponse {
        public GeminiEmbedding embedding;
    }
    
    static class GeminiEmbedding {
        public List<Double> values;
    }
}
