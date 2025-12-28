package com.chatbot.rag;

import com.chatbot.embedding.LocalEmbeddingService;
import com.chatbot.embedding.LocalEmbeddingService.EmbeddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

/**
 * RAG (Retrieval-Augmented Generation) Service.
 * 
 * Features:
 * - Uses LOCAL embeddings for vector search (no external API)
 * - Uses GROQ ONLY for text generation
 * - Implements fallback logic when no relevant context found
 * - Labels responses with source: "from notes" or "from AI"
 * 
 * Flow:
 * 1. User query → embed with local model
 * 2. Search vector store for similar chunks
 * 3. If similar chunks found (score > threshold):
 *    - Send context + query to Groq
 *    - Label: "Answer from uploaded notes:"
 * 4. If no similar chunks:
 *    - Send query alone to Groq
 *    - Label: "Answer not in uploaded notes, generated from AI:"
 */
@Service
public class RagService {
    
    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    
    // Groq API configuration
    @Value("${llm.api.url:https://api.groq.com/openai/v1}")
    private String groqApiUrl;
    
    @Value("${llm.api.key:}")
    private String groqApiKey;
    
    @Value("${llm.model:llama-3.1-8b-instant}")
    private String groqModel;
    
    // RAG configuration
    @Value("${rag.retrieval.top-k:5}")
    private int topK;
    
    @Value("${rag.retrieval.similarity-threshold:0.5}")
    private double similarityThreshold;
    
    private final LocalEmbeddingService embeddingService;
    private final RestTemplate restTemplate;
    
    // Labels for response source
    public static final String LABEL_FROM_NOTES = "📚 Answer from uploaded notes:";
    public static final String LABEL_FROM_AI = "🤖 Answer not in uploaded notes, generated from AI:";
    
    public RagService(
            LocalEmbeddingService embeddingService,
            RestTemplateBuilder restTemplateBuilder) {
        this.embeddingService = embeddingService;
        this.restTemplate = restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(2))
            .build();
    }
    
    /**
     * Generate a RAG response for a user query.
     * 
     * @param query User's question
     * @param chunks List of document chunks to search
     * @return RagResponse with answer and source label
     */
    public RagResponse generateResponse(String query, List<DocumentChunk> chunks) {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  🔍 RAG Query Processing Started                              ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        log.info("📝 Query: {}", truncate(query, 100));
        log.info("📦 Available chunks: {}", chunks.size());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Generate query embedding
            log.info("🔄 Step 1: Generating query embedding...");
            long embedStart = System.currentTimeMillis();
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            log.info("✅ Query embedding generated in {} ms", System.currentTimeMillis() - embedStart);
            
            // Step 2: Find relevant chunks
            log.info("🔄 Step 2: Searching for relevant chunks...");
            List<ScoredChunk> scoredChunks = findRelevantChunks(queryEmbedding, chunks);
            
            // Step 3: Check if we have relevant context
            List<ScoredChunk> relevantChunks = scoredChunks.stream()
                .filter(sc -> sc.score >= similarityThreshold)
                .limit(topK)
                .toList();
            
            log.info("📊 Found {} chunks above threshold {} (out of {} total)", 
                    relevantChunks.size(), similarityThreshold, scoredChunks.size());
            
            // Step 4: Generate response with appropriate prompt
            String response;
            String label;
            List<String> citations = new ArrayList<>();
            
            if (!relevantChunks.isEmpty()) {
                // Context found - use RAG prompt
                log.info("✅ Relevant context found! Using RAG generation...");
                response = generateWithContext(query, relevantChunks);
                label = LABEL_FROM_NOTES;
                
                // Build citations
                for (int i = 0; i < relevantChunks.size(); i++) {
                    ScoredChunk sc = relevantChunks.get(i);
                    citations.add(String.format("[%d] %s (similarity: %.2f)", 
                            i + 1, sc.chunk.fileName(), sc.score));
                }
                
                log.info("📚 Response generated from {} sources", relevantChunks.size());
            } else {
                // No relevant context - use fallback prompt
                log.info("⚠️  No relevant context found! Using fallback generation...");
                response = generateWithoutContext(query);
                label = LABEL_FROM_AI;
                
                log.info("🤖 Response generated using general AI knowledge");
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║  ✅ RAG Query Completed in {} ms                             ║", elapsed);
            log.info("║  Source: {}                                                  ║", 
                    relevantChunks.isEmpty() ? "General AI" : "Uploaded Notes");
            log.info("╚══════════════════════════════════════════════════════════════╝");
            
            return new RagResponse(
                label + "\n\n" + response,
                !relevantChunks.isEmpty(),
                citations,
                elapsed
            );
            
        } catch (EmbeddingException e) {
            log.error("❌ Embedding error: {}", e.getMessage());
            // Fallback to Groq without RAG
            String response = generateWithoutContext(query);
            return new RagResponse(
                LABEL_FROM_AI + "\n\n" + response,
                false,
                List.of(),
                System.currentTimeMillis() - startTime
            );
        } catch (Exception e) {
            log.error("❌ RAG error: {}", e.getMessage(), e);
            return new RagResponse(
                "Sorry, I encountered an error processing your request: " + e.getMessage(),
                false,
                List.of(),
                System.currentTimeMillis() - startTime
            );
        }
    }
    
    /**
     * Find relevant chunks using cosine similarity.
     */
    private List<ScoredChunk> findRelevantChunks(float[] queryEmbedding, List<DocumentChunk> chunks) {
        log.debug("🔍 Computing similarity for {} chunks...", chunks.size());
        
        List<ScoredChunk> scored = new ArrayList<>();
        
        for (DocumentChunk chunk : chunks) {
            if (chunk.embedding() != null) {
                double similarity = cosineSimilarity(queryEmbedding, chunk.embedding());
                scored.add(new ScoredChunk(chunk, similarity));
            }
        }
        
        // Sort by similarity (highest first)
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        
        // Log top results
        if (!scored.isEmpty()) {
            log.debug("📊 Top similarity scores:");
            for (int i = 0; i < Math.min(3, scored.size()); i++) {
                ScoredChunk sc = scored.get(i);
                log.debug("   {}. {} (score: {:.3f})", i + 1, 
                        truncate(sc.chunk.content(), 50), sc.score);
            }
        }
        
        return scored;
    }
    
    /**
     * Compute cosine similarity between two vectors.
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0;
        }
        
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0 || normB == 0) {
            return 0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * Generate response using Groq with context from documents.
     */
    private String generateWithContext(String query, List<ScoredChunk> chunks) {
        log.info("🚀 Calling Groq API with context...");
        
        // Build context from chunks
        StringBuilder context = new StringBuilder();
        context.append("RELEVANT CONTEXT FROM UPLOADED NOTES:\n\n");
        
        for (int i = 0; i < chunks.size(); i++) {
            ScoredChunk sc = chunks.get(i);
            context.append(String.format("[Source %d: %s]\n", i + 1, sc.chunk.fileName()));
            context.append(sc.chunk.content());
            context.append("\n\n---\n\n");
        }
        
        String systemPrompt = """
            You are a helpful AI assistant. Answer the user's question based ONLY on the provided context from their uploaded notes.
            
            INSTRUCTIONS:
            - Use ONLY information from the provided context to answer
            - If the answer is not in the context, say "I don't see information about that in your notes"
            - Cite sources using [Source 1], [Source 2], etc. when referencing specific documents
            - Be accurate and concise
            - Do NOT make up information not present in the context
            
            %s
            """.formatted(context.toString());
        
        return callGroqApi(systemPrompt, query);
    }
    
    /**
     * Generate response using Groq without context (fallback).
     */
    private String generateWithoutContext(String query) {
        log.info("🚀 Calling Groq API without context (fallback)...");
        
        String systemPrompt = """
            You are a helpful AI assistant. The user asked a question but no relevant information was found in their uploaded notes.
            
            INSTRUCTIONS:
            - Answer the question using your general knowledge
            - Be helpful but indicate that this answer is from general AI knowledge, not from their uploaded documents
            - Be accurate and informative
            - If you're not sure about something, say so
            """;
        
        return callGroqApi(systemPrompt, query);
    }
    
    /**
     * Call the Groq API for text generation.
     */
    private String callGroqApi(String systemPrompt, String userMessage) {
        log.info("📡 Groq API call started");
        log.debug("   Model: {}", groqModel);
        log.debug("   URL: {}", groqApiUrl);
        
        long startTime = System.currentTimeMillis();
        
        try {
            String url = groqApiUrl + "/chat/completions";
            
            // Build messages
            List<Map<String, String>> messages = new ArrayList<>();
            
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", groqModel);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2048);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.debug("📤 Sending request to Groq...");
            
            @SuppressWarnings("null")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ Groq response received in {} ms", elapsed);
            
            // Parse response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        String content = (String) message.get("content");
                        log.info("📦 Response length: {} chars", content.length());
                        return content;
                    }
                }
            }
            
            log.warn("⚠️  Empty response from Groq");
            return "I received an empty response from the AI.";
            
        } catch (Exception e) {
            log.error("❌ Groq API error: {}", e.getMessage());
            return "Sorry, I encountered an error calling the AI service: " + e.getMessage();
        }
    }
    
    /**
     * Truncate string for logging.
     */
    private String truncate(String s, int maxLength) {
        if (s == null) return "";
        if (s.length() <= maxLength) return s;
        return s.substring(0, maxLength) + "...";
    }
    
    // ==================== Records ====================
    
    /**
     * Represents a document chunk with embedding.
     */
    public record DocumentChunk(
        String id,
        String fileName,
        int chunkIndex,
        String content,
        float[] embedding,
        String contentHash
    ) {}
    
    /**
     * Scored chunk for similarity ranking.
     */
    public record ScoredChunk(
        DocumentChunk chunk,
        double score
    ) {}
    
    /**
     * RAG response with metadata.
     */
    public record RagResponse(
        String response,
        boolean fromContext,
        List<String> citations,
        long processingTimeMs
    ) {}
}
