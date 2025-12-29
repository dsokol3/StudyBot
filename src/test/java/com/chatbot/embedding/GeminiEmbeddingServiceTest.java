package com.chatbot.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GeminiEmbeddingService.
 * 
 * Tests:
 * - Hash computation for caching
 * - API call with retries
 * - Vector string conversion
 * - Cache behavior
 * - Error handling
 */
class GeminiEmbeddingServiceTest {
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private GeminiEmbeddingService embeddingService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        embeddingService = new GeminiEmbeddingService(restTemplate, objectMapper);
        
        // Set up test configuration
        ReflectionTestUtils.setField(embeddingService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(embeddingService, "cacheEnabled", true);
        ReflectionTestUtils.setField(embeddingService, "maxRetries", 3);
        ReflectionTestUtils.setField(embeddingService, "timeoutSeconds", 30);
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
    @DisplayName("Embedding dimension should be 768")
    void testEmbeddingDimension() {
        assertEquals(768, GeminiEmbeddingService.EMBEDDING_DIMENSION);
        assertEquals(768, embeddingService.getEmbeddingDimension());
    }
    
    @Test
    @DisplayName("Should throw exception for empty text")
    void testGenerateEmbeddingEmptyText() {
        assertThrows(GeminiEmbeddingService.EmbeddingException.class, 
            () -> embeddingService.generateEmbedding(""));
        
        assertThrows(GeminiEmbeddingService.EmbeddingException.class, 
            () -> embeddingService.generateEmbedding("   "));
        
        assertThrows(GeminiEmbeddingService.EmbeddingException.class, 
            () -> embeddingService.generateEmbedding(null));
    }
    
    @Test
    @DisplayName("Should throw exception when API key is not configured")
    void testGenerateEmbeddingNoApiKey() {
        ReflectionTestUtils.setField(embeddingService, "apiKey", "");
        
        assertThrows(GeminiEmbeddingService.EmbeddingException.class,
            () -> embeddingService.generateEmbedding("test text"));
    }
    
    @Test
    @DisplayName("Should successfully generate embedding from API")
    void testGenerateEmbeddingSuccess() throws Exception {
        // Mock successful API response
        GeminiEmbeddingService.GeminiEmbedResponse mockResponse = new GeminiEmbeddingService.GeminiEmbedResponse();
        mockResponse.embedding = new GeminiEmbeddingService.GeminiEmbedding();
        mockResponse.embedding.values = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
        
        when(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(GeminiEmbeddingService.GeminiEmbedResponse.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        float[] embedding = embeddingService.generateEmbedding("test text");
        
        assertNotNull(embedding);
        assertEquals(5, embedding.length);
        assertEquals(0.1f, embedding[0], 0.001f);
        assertEquals(0.5f, embedding[4], 0.001f);
    }
    
    @Test
    @DisplayName("Should cache embeddings and return from cache")
    void testEmbeddingCaching() throws Exception {
        // Mock successful API response
        GeminiEmbeddingService.GeminiEmbedResponse mockResponse = new GeminiEmbeddingService.GeminiEmbedResponse();
        mockResponse.embedding = new GeminiEmbeddingService.GeminiEmbedding();
        mockResponse.embedding.values = Arrays.asList(0.1, 0.2, 0.3);
        
        when(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(GeminiEmbeddingService.GeminiEmbedResponse.class)
        )).thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        
        String testText = "test text";
        
        // First call - should hit API
        float[] embedding1 = embeddingService.generateEmbedding(testText);
        
        // Second call - should hit cache
        float[] embedding2 = embeddingService.generateEmbedding(testText);
        
        // Verify API was called only once
        verify(restTemplate, times(1)).exchange(
            anyString(),
            any(),
            any(),
            eq(GeminiEmbeddingService.GeminiEmbedResponse.class)
        );
        
        assertArrayEquals(embedding1, embedding2);
    }
    
    @Test
    @DisplayName("Should retry on API failure")
    void testApiRetry() throws Exception {
        // First two calls fail, third succeeds
        when(restTemplate.exchange(
            anyString(),
            any(),
            any(),
            eq(GeminiEmbeddingService.GeminiEmbedResponse.class)
        ))
        .thenThrow(new RestClientException("Network error"))
        .thenThrow(new RestClientException("Network error"))
        .thenAnswer(invocation -> {
            GeminiEmbeddingService.GeminiEmbedResponse mockResponse = new GeminiEmbeddingService.GeminiEmbedResponse();
            mockResponse.embedding = new GeminiEmbeddingService.GeminiEmbedding();
            mockResponse.embedding.values = Arrays.asList(0.1, 0.2, 0.3);
            return new ResponseEntity<>(mockResponse, HttpStatus.OK);
        });
        
        float[] embedding = embeddingService.generateEmbedding("test text");
        
        assertNotNull(embedding);
        // Verify API was called 3 times (2 failures + 1 success)
        verify(restTemplate, times(3)).exchange(
            anyString(),
            any(),
            any(),
            eq(GeminiEmbeddingService.GeminiEmbedResponse.class)
        );
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
        assertTrue(stats.containsKey("apiCalls"));
        assertTrue(stats.containsKey("apiErrors"));
        assertTrue(stats.containsKey("errorRate"));
        assertTrue(stats.containsKey("modelLoaded"));
        assertTrue(stats.containsKey("mode"));
        assertEquals("gemini-api", stats.get("mode"));
    }
    
    @Test
    @DisplayName("Should check if model is loaded based on API key")
    void testIsModelLoaded() {
        // With API key
        ReflectionTestUtils.setField(embeddingService, "apiKey", "test-key");
        assertTrue(embeddingService.isModelLoaded());
        
        // Without API key
        ReflectionTestUtils.setField(embeddingService, "apiKey", "");
        assertFalse(embeddingService.isModelLoaded());
        
        ReflectionTestUtils.setField(embeddingService, "apiKey", null);
        assertFalse(embeddingService.isModelLoaded());
    }
}
