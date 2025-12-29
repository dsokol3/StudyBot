package com.chatbot.service;

import com.chatbot.entity.Conversation;
import com.chatbot.entity.Message;
import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatResponse;
import com.chatbot.repository.ConversationRepository;
import com.chatbot.repository.MessageRepository;
import com.chatbot.service.RetrievalService.Citation;
import com.chatbot.service.RetrievalService.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Chat Service - handles conversation processing with RAG support.
 * 
 * UPDATED: Now uses:
 * - GOOGLE GEMINI for embeddings (via EmbeddingService -> GeminiEmbeddingService)
 * - GROQ API for text generation (llama-3.1-8b-instant)
 * 
 * Response labeling:
 * - "📚 Answer from uploaded notes:" when context is found
 * - "🤖 Answer from AI:" when using general knowledge
 */
@Service
@Transactional
public class ChatService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    
    // LLM Configuration (Groq API)
    @Value("${llm.api.url:https://api.groq.com/openai/v1}")
    private String llmApiUrl;
    
    @Value("${llm.api.key:}")
    private String llmApiKey;
    
    @Value("${llm.model:llama-3.1-8b-instant}")
    private String llmModel;
    
    @Value("${rag.enabled:true}")
    private boolean ragEnabled;
    
    // Response labels
    private static final String LABEL_FROM_NOTES = "📚 Answer from uploaded notes:";
    private static final String LABEL_FROM_AI = "🤖 Answer from AI:";
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final RetrievalService retrievalService;
    
    public ChatService(
            ConversationRepository conversationRepository, 
            MessageRepository messageRepository,
            RetrievalService retrievalService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.retrievalService = retrievalService;
        
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  🤖 ChatService initialized                                  ║");
        log.info("║  Text Generation: Groq API (llama-3.1-8b-instant)           ║");
        log.info("║  Embeddings: Google Gemini (text-embedding-004)             ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
    }
    
    public ChatResponse processMessageWithRag(String userMessage, String conversationIdParam) {
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  💬 Processing Chat Message with RAG                          ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        log.info("📝 Message: {}", truncate(userMessage, 100));
        
        long startTime = System.currentTimeMillis();
        
        // Initialize conversation if needed
        final String conversationId;
        if (conversationIdParam == null || conversationIdParam.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
            log.info("🆕 New conversation: {}", conversationId);
        } else {
            conversationId = conversationIdParam;
            log.info("📂 Existing conversation: {}", conversationId);
        }
        
        // Find or create conversation
        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();
                    newConv.setConversationId(conversationId);
                    newConv.setCreatedAt(LocalDateTime.now());
                    newConv.setUpdatedAt(LocalDateTime.now());
                    newConv.setMessages(new ArrayList<>());
                    return conversationRepository.save(newConv);
                });
        
        // RAG: Retrieve relevant document chunks
        List<RetrievedChunk> relevantChunks = new ArrayList<>();
        List<Citation> citations = new ArrayList<>();
        String ragContext = "";
        boolean hasContext = false;
        
        if (ragEnabled && retrievalService.hasDocuments(conversationId)) {
            log.info("🔍 RAG enabled - searching for relevant context...");
            relevantChunks = retrievalService.findRelevantChunks(userMessage, conversationId);
            if (!relevantChunks.isEmpty()) {
                ragContext = retrievalService.buildContext(relevantChunks);
                citations = retrievalService.buildCitations(relevantChunks);
                hasContext = true;
                log.info("✅ Found {} relevant chunks from uploaded notes", relevantChunks.size());
            } else {
                log.info("⚠️  No relevant chunks found in uploaded notes");
            }
        } else {
            log.info("📝 RAG disabled or no documents - using general AI knowledge");
        }
        
        // Load conversation history
        List<Message> dbMessages = messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, String>> history = new ArrayList<>();
        
        // Add system message with appropriate prompt
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        if (hasContext) {
            systemMsg.put("content", buildRagSystemPrompt(ragContext));
            log.info("📚 Using RAG system prompt with context");
        } else {
            systemMsg.put("content", buildFallbackSystemPrompt());
            log.info("🤖 Using fallback system prompt (no context)");
        }
        history.add(systemMsg);
        
        for (Message msg : dbMessages) {
            Map<String, String> historyEntry = new HashMap<>();
            historyEntry.put("role", msg.getRole());
            historyEntry.put("content", msg.getContent());
            history.add(historyEntry);
        }
        
        // Add user message to history
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        history.add(userMsg);
        
        // Save user message to database
        Message userMessage_db = new Message();
        userMessage_db.setConversation(conversation);
        userMessage_db.setRole("user");
        userMessage_db.setContent(userMessage);
        userMessage_db.setCreatedAt(LocalDateTime.now());
        messageRepository.save(userMessage_db);
        
        String assistantResponse;
        String label = hasContext ? LABEL_FROM_NOTES : LABEL_FROM_AI;
        
        try {
            log.info("🚀 Calling Groq API for text generation...");
            assistantResponse = callGroqApi(history);
            log.info("✅ Groq response received");
        } catch (Exception e) {
            log.error("❌ Groq API error: {}", e.getMessage());
            assistantResponse = "Sorry, I encountered an error connecting to the AI service: " + e.getMessage();
            label = "⚠️ Error:";
        }
        
        // Add label to response
        String labeledResponse = label + "\n\n" + assistantResponse;
        
        // Save assistant response to database
        Message assistantMessage_db = new Message();
        assistantMessage_db.setConversation(conversation);
        assistantMessage_db.setRole("assistant");
        assistantMessage_db.setContent(labeledResponse);
        assistantMessage_db.setCreatedAt(LocalDateTime.now());
        messageRepository.save(assistantMessage_db);
        
        // Update conversation timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  ✅ Chat Response Generated in {} ms                         ║", elapsed);
        log.info("║  Source: {}                                                  ║", hasContext ? "Uploaded Notes" : "AI Knowledge");
        log.info("║  Citations: {}                                               ║", citations.size());
        log.info("╚══════════════════════════════════════════════════════════════╝");
        
        return new ChatResponse(
            UUID.randomUUID().toString(),
            labeledResponse,
            "assistant",
            System.currentTimeMillis(),
            conversationId,
            citations
        );
    }
    
    /**
     * System prompt when context from uploaded notes is available.
     */
    private String buildRagSystemPrompt(String ragContext) {
        return """
            You are a helpful AI assistant. Answer questions based on the provided context documents when relevant.
            
            %s
            
            INSTRUCTIONS:
            - Use the provided documents to answer questions when relevant
            - Cite sources using [Source 1], [Source 2], etc. when referencing specific documents
            - If the documents don't contain the answer, say "I don't see that information in your notes"
            - Be accurate and helpful
            - Do NOT make up information not present in the context
            """.formatted(ragContext);
    }
    
    /**
     * System prompt when no context is available (fallback).
     */
    private String buildFallbackSystemPrompt() {
        return """
            You are a helpful AI assistant. The user asked a question but no relevant information was found in their uploaded notes.
            
            INSTRUCTIONS:
            - Answer the question using your general knowledge
            - Be helpful and informative
            - If you're not sure about something, say so
            - The response will be labeled as coming from AI, not from their notes
            """;
    }
    
    public ChatMessage processMessage(String userMessage, String conversationIdParam) {
        log.info("💬 Processing simple chat message");
        
        // Initialize conversation if needed
        final String conversationId;
        if (conversationIdParam == null || conversationIdParam.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        } else {
            conversationId = conversationIdParam;
        }
        
        // Find or create conversation
        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();
                    newConv.setConversationId(conversationId);
                    newConv.setCreatedAt(LocalDateTime.now());
                    newConv.setUpdatedAt(LocalDateTime.now());
                    newConv.setMessages(new ArrayList<>());
                    return conversationRepository.save(newConv);
                });
        
        // Load conversation history
        List<Message> dbMessages = messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, String>> history = new ArrayList<>();
        for (Message msg : dbMessages) {
            Map<String, String> historyEntry = new HashMap<>();
            historyEntry.put("role", msg.getRole());
            historyEntry.put("content", msg.getContent());
            history.add(historyEntry);
        }
        
        // Add user message to history
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        history.add(userMsg);
        
        // Save user message to database
        Message userMessage_db = new Message();
        userMessage_db.setConversation(conversation);
        userMessage_db.setRole("user");
        userMessage_db.setContent(userMessage);
        userMessage_db.setCreatedAt(LocalDateTime.now());
        messageRepository.save(userMessage_db);
        
        String assistantResponse;
        
        try {
            log.info("🚀 Calling Groq API...");
            assistantResponse = callGroqApi(history);
        } catch (Exception e) {
            log.error("❌ Groq API error: {}", e.getMessage());
            assistantResponse = "Sorry, I encountered an error connecting to the AI service: " + e.getMessage();
        }
        
        // Save assistant response to database
        Message assistantMessage_db = new Message();
        assistantMessage_db.setConversation(conversation);
        assistantMessage_db.setRole("assistant");
        assistantMessage_db.setContent(assistantResponse);
        assistantMessage_db.setCreatedAt(LocalDateTime.now());
        messageRepository.save(assistantMessage_db);
        
        // Update conversation timestamp
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        
        return new ChatMessage(
            UUID.randomUUID().toString(),
            assistantResponse,
            "assistant",
            System.currentTimeMillis()
        );
    }
    
    /**
     * Call Groq API for text generation.
     * Groq uses OpenAI-compatible API format.
     */
    private String callGroqApi(List<Map<String, String>> history) {
        log.info("📡 Groq API call started");
        log.debug("   URL: {}", llmApiUrl);
        log.debug("   Model: {}", llmModel);
        log.debug("   Messages: {}", history.size());
        
        long startTime = System.currentTimeMillis();
        
        String url = llmApiUrl + "/chat/completions";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmModel);
        requestBody.put("messages", history);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 2048);
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (llmApiKey != null && !llmApiKey.isEmpty()) {
            headers.set("Authorization", "Bearer " + llmApiKey);
        } else {
            log.warn("⚠️  No API key configured! Set LLM_API_KEY environment variable.");
        }
        
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
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null) {
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
    }
    
    @SuppressWarnings("null")
    public void clearConversation(String conversationId) {
        log.info("🗑️  Clearing conversation: {}", conversationId);
        
        // Delete all messages in the conversation
        List<Message> messages = messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(conversationId);
        if (messages != null && !messages.isEmpty()) {
            messageRepository.deleteAll(messages);
        }
        
        // Delete the conversation
        conversationRepository.findByConversationId(conversationId)
                .ifPresent(conversationRepository::delete);
        
        log.info("✅ Conversation cleared");
    }
    
    /**
     * Truncate string for logging.
     */
    private String truncate(String s, int maxLength) {
        if (s == null) return "";
        if (s.length() <= maxLength) return s;
        return s.substring(0, maxLength) + "...";
    }
}
