package com.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for splitting text into chunks suitable for embedding.
 * Uses a recursive character-based splitting strategy that respects
 * natural text boundaries (paragraphs, sentences, words).
 */
@Service
public class ChunkingService {
    
    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);
    
    @Value("${rag.chunking.size:500}")
    private int chunkSize;
    
    @Value("${rag.chunking.overlap:50}")
    private int chunkOverlap;
    
    // Separators in order of preference (try to split on larger units first)
    private static final String[] SEPARATORS = {
        "\n\n",     // Paragraph breaks
        "\n",       // Line breaks
        ". ",       // Sentence endings
        "! ",       // Exclamation endings
        "? ",       // Question endings
        "; ",       // Semicolon breaks
        ", ",       // Comma breaks
        " ",        // Word breaks
        ""          // Character-level (last resort)
    };
    
    /**
     * Split text into overlapping chunks.
     * 
     * @param text The full text to chunk
     * @return List of text chunks
     */
    public List<TextChunk> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        
        // Normalize whitespace
        text = normalizeWhitespace(text);
        
        List<String> chunks = splitRecursively(text, SEPARATORS, 0);
        List<TextChunk> result = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i).trim();
            if (!content.isEmpty()) {
                result.add(new TextChunk(
                    i,
                    content,
                    estimateTokenCount(content)
                ));
            }
        }
        
        log.info("Split text into {} chunks (target size: {}, overlap: {})", 
                 result.size(), chunkSize, chunkOverlap);
        
        return result;
    }
    
    private List<String> splitRecursively(String text, String[] separators, int sepIndex) {
        if (text.length() <= chunkSize) {
            return List.of(text);
        }
        
        if (sepIndex >= separators.length) {
            // Last resort: hard split at chunkSize
            return hardSplit(text);
        }
        
        String separator = separators[sepIndex];
        List<String> splits;
        
        if (separator.isEmpty()) {
            // Character-level split
            return hardSplit(text);
        } else {
            splits = List.of(text.split(Pattern.quote(separator), -1));
        }
        
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < splits.size(); i++) {
            String split = splits.get(i);
            String withSep = i < splits.size() - 1 ? split + separator : split;
            
            if (current.length() + withSep.length() <= chunkSize) {
                current.append(withSep);
            } else {
                if (current.length() > 0) {
                    // Save current chunk
                    result.add(current.toString());
                    
                    // Start new chunk with overlap
                    current = new StringBuilder();
                    if (chunkOverlap > 0 && !result.isEmpty()) {
                        String lastChunk = result.get(result.size() - 1);
                        String overlap = getOverlap(lastChunk);
                        current.append(overlap);
                    }
                }
                
                if (withSep.length() > chunkSize) {
                    // This segment is too big, recurse with next separator
                    List<String> subChunks = splitRecursively(withSep, separators, sepIndex + 1);
                    result.addAll(subChunks);
                } else {
                    current.append(withSep);
                }
            }
        }
        
        if (current.length() > 0) {
            result.add(current.toString());
        }
        
        return result;
    }
    
    private List<String> hardSplit(String text) {
        List<String> result = new ArrayList<>();
        int pos = 0;
        
        while (pos < text.length()) {
            int end = Math.min(pos + chunkSize, text.length());
            result.add(text.substring(pos, end));
            pos = end - chunkOverlap;
            if (pos <= 0 && end < text.length()) {
                pos = end;
            }
        }
        
        return result;
    }
    
    private String getOverlap(String text) {
        if (text.length() <= chunkOverlap) {
            return text;
        }
        return text.substring(text.length() - chunkOverlap);
    }
    
    private String normalizeWhitespace(String text) {
        // Replace multiple spaces/tabs with single space
        // Keep paragraph breaks (double newlines)
        return text
            .replaceAll("[ \\t]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }
    
    /**
     * Rough estimate of token count (approximately 4 characters per token for English).
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }
    
    /**
     * Represents a text chunk with metadata.
     */
    public record TextChunk(
        int order,
        String content,
        int tokenCount
    ) {}
}
