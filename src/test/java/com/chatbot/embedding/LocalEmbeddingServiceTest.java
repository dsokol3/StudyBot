package com.chatbot.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocalEmbeddingService.
 * 
 * Tests:
 * - Hash computation for caching
 * - Fallback embedding generation
 * - Vector string conversion
 * - Cache behavior
 */
class LocalEmbeddingServiceTest {
    
    private LocalEmbeddingService embeddingService;
    
    @BeforeEach
    void setUp() {
        embeddingService = new LocalEmbeddingService();
        // Set up for testing - only cacheEnabled field is needed now
        ReflectionTestUtils.setField(embeddingService, "cacheEnabled", true);
    }
    
    @Test
    @DisplayName("Should compute consistent SHA-256 hash for same text")
    void testHashComputation() {
        String text1 = "Hello, world!";
        String text2 = "Hello, world!";
        String text3 = "Different text";
        
        String hash1 = embeddingService.computeHash(text1);
        String hash2 = embeddingService.computeHash(text2);
        String hash3 = embeddingService.computeHash(text3);
        
        // Same text should produce same hash
        assertEquals(hash1, hash2, "Same text should produce same hash");
        
        // Different text should produce different hash
        assertNotEquals(hash1, hash3, "Different text should produce different hash");
        
        // Hash should be 64 characters (SHA-256 hex)
        assertEquals(64, hash1.length(), "SHA-256 hash should be 64 hex characters");
    }
    
    @Test
    @DisplayName("Should convert embedding to vector string correctly")
    void testEmbeddingToVectorString() {
        float[] embedding = {0.1f, 0.2f, 0.3f, -0.4f, 0.5f};
        
        String vectorString = embeddingService.embeddingToVectorString(embedding);
        
        assertNotNull(vectorString);
        assertTrue(vectorString.startsWith("["));
        assertTrue(vectorString.endsWith("]"));
        assertTrue(vectorString.contains("0.1"));
        assertTrue(vectorString.contains("-0.4"));
    }
    
    @Test
    @DisplayName("Should return null for empty embedding")
    void testEmbeddingToVectorStringEmpty() {
        float[] emptyEmbedding = {};
        
        String result = embeddingService.embeddingToVectorString(emptyEmbedding);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Should return null for null embedding")
    void testEmbeddingToVectorStringNull() {
        String result = embeddingService.embeddingToVectorString(null);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Embedding dimension should be 384")
    void testEmbeddingDimension() {
        assertEquals(384, LocalEmbeddingService.EMBEDDING_DIMENSION);
        assertEquals(384, embeddingService.getEmbeddingDimension());
    }
    
    @Test
    @DisplayName("Should throw exception for empty text")
    void testGenerateEmbeddingEmptyText() {
        assertThrows(LocalEmbeddingService.EmbeddingException.class, 
            () -> embeddingService.generateEmbedding(""));
        
        assertThrows(LocalEmbeddingService.EmbeddingException.class, 
            () -> embeddingService.generateEmbedding("   "));
        
        assertThrows(LocalEmbeddingService.EmbeddingException.class, 
            () -> embeddingService.generateEmbedding(null));
    }
    
    @Test
    @DisplayName("Should return cache statistics")
    void testGetCacheStats() {
        var stats = embeddingService.getCacheStats();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("cacheSize"));
        assertTrue(stats.containsKey("totalEmbeddings"));
        assertTrue(stats.containsKey("cacheHits"));
        assertTrue(stats.containsKey("hitRate"));
        assertTrue(stats.containsKey("modelLoaded"));
    }
}
