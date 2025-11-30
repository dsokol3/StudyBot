package com.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for generating text embeddings using Ollama's embedding API.
 * Uses the nomic-embed-text model for high-quality embeddings.
 */
@Service
public class EmbeddingService {
    
    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    
    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaUrl;
    
    @Value("${rag.embedding.model:nomic-embed-text}")
    private String embeddingModel;
    
    @Value("${rag.embedding.dimension:768}")
    private int embeddingDimension;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Generate embedding for a single text.
     * 
     * @param text The text to embed
     * @return Float array of embedding values
     * @throws EmbeddingException if embedding generation fails
     */
    public float[] generateEmbedding(String text) throws EmbeddingException {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Cannot generate embedding for empty text");
        }
        
        try {
            String url = ollamaUrl + "/api/embeddings";
            
            Map<String, Object> requestBody = Map.of(
                "model", embeddingModel,
                "prompt", text
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("embedding")) {
                throw new EmbeddingException("Invalid response from Ollama embedding API");
            }
            
            @SuppressWarnings("unchecked")
            List<Double> embeddingList = (List<Double>) responseBody.get("embedding");
            
            if (embeddingList == null || embeddingList.isEmpty()) {
                throw new EmbeddingException("Empty embedding returned from Ollama");
            }
            
            float[] embedding = new float[embeddingList.size()];
            for (int i = 0; i < embeddingList.size(); i++) {
                embedding[i] = embeddingList.get(i).floatValue();
            }
            
            log.debug("Generated embedding of dimension {} for text of length {}", 
                      embedding.length, text.length());
            
            return embedding;
            
        } catch (Exception e) {
            if (e instanceof EmbeddingException) {
                throw (EmbeddingException) e;
            }
            throw new EmbeddingException("Failed to generate embedding: " + e.getMessage(), e);
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
        List<float[]> embeddings = new ArrayList<>();
        
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        
        log.info("Generated {} embeddings", embeddings.size());
        return embeddings;
    }
    
    /**
     * Convert embedding array to pgvector-compatible string format.
     * Format: [0.1, 0.2, 0.3, ...]
     */
    public String embeddingToVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    public int getEmbeddingDimension() {
        return embeddingDimension;
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
