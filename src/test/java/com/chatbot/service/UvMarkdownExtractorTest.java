package com.chatbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for UvMarkdownExtractor service.
 */
class UvMarkdownExtractorTest {
    
    private UvMarkdownExtractor uvMarkdownExtractor;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        uvMarkdownExtractor = new UvMarkdownExtractor();
        // Enable UV for tests with 'uv tool run' prefix
        uvMarkdownExtractor.setUvEnabled(true);
        uvMarkdownExtractor.setUvCommandPath("markitdown");
        uvMarkdownExtractor.setUvCommandTimeout(60);
        uvMarkdownExtractor.setUseUvToolRun(true); // Use 'uv tool run markitdown'
    }
    
    @Test
    void testIsUvAvailable() {
        // Test that the availability check doesn't throw exceptions
        boolean available = uvMarkdownExtractor.isUvAvailable();
        // Don't assert true/false as UV may or may not be installed
        // Just verify the method executes without error
        assertNotNull(available);
    }
    
    @Test
    void testIsUvAvailableCaching() {
        // First call
        boolean firstResult = uvMarkdownExtractor.isUvAvailable();
        
        // Second call should use cached value
        boolean secondResult = uvMarkdownExtractor.isUvAvailable();
        
        // Results should be the same (cached)
        assertEquals(firstResult, secondResult);
    }
    
    @Test
    void testResetAvailabilityCache() {
        // Call once to cache
        uvMarkdownExtractor.isUvAvailable();
        
        // Reset cache
        uvMarkdownExtractor.resetAvailabilityCache();
        
        // Should work after reset
        assertDoesNotThrow(() -> uvMarkdownExtractor.isUvAvailable());
    }
    
    @Test
    void testExtractMarkdownWithNonExistentFile() {
        Path nonExistentFile = tempDir.resolve("does-not-exist.txt");
        
        // Should throw exception for non-existent file
        assertThrows(
            UvMarkdownExtractor.MarkdownExtractionException.class,
            () -> uvMarkdownExtractor.extractMarkdown(nonExistentFile)
        );
    }
    
    @Test
    void testExtractMarkdownWithExistingFile() throws IOException {
        // Create a temporary test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "# Test Document\n\nThis is a test.");
        
        // Only test if UV is actually available
        if (uvMarkdownExtractor.isUvAvailable()) {
            assertDoesNotThrow(() -> {
                String markdown = uvMarkdownExtractor.extractMarkdown(testFile);
                assertNotNull(markdown);
                assertFalse(markdown.isEmpty());
            });
        } else {
            // If UV is not available, should throw exception
            assertThrows(
                UvMarkdownExtractor.MarkdownExtractionException.class,
                () -> uvMarkdownExtractor.extractMarkdown(testFile)
            );
        }
    }
}
