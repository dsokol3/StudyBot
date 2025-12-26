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

@Service
@Transactional
public class ChatService {
    
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    
    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaUrl;
    
    @Value("${ollama.api.key:}")
    private String ollamaApiKey;
    
    @Value("${ollama.model:llama3}")
    private String ollamaModel;
    
    @Value("${rag.enabled:true}")
    private boolean ragEnabled;
    
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
    }
    
    public ChatResponse processMessageWithRag(String userMessage, String conversationIdParam) {
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
        
        // RAG: Retrieve relevant document chunks
        List<RetrievedChunk> relevantChunks = new ArrayList<>();
        List<Citation> citations = new ArrayList<>();
        String ragContext = "";
        
        if (ragEnabled && retrievalService.hasDocuments(conversationId)) {
            relevantChunks = retrievalService.findRelevantChunks(userMessage, conversationId);
            if (!relevantChunks.isEmpty()) {
                ragContext = retrievalService.buildContext(relevantChunks);
                citations = retrievalService.buildCitations(relevantChunks);
                log.info("RAG: Found {} relevant chunks for query", relevantChunks.size());
            }
        }
        
        // Load conversation history
        List<Message> dbMessages = messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(conversationId);
        List<Map<String, String>> history = new ArrayList<>();
        
        // Add system message with RAG context if available
        if (!ragContext.isEmpty()) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", buildSystemPrompt(ragContext));
            history.add(systemMsg);
        }
        
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
            // Call Ollama API
            assistantResponse = callOllama(history);
        } catch (Exception e) {
            assistantResponse = "Sorry, I encountered an error connecting to Ollama: " + e.getMessage() + 
                              ". Make sure Ollama is running on " + ollamaUrl;
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
        
        return new ChatResponse(
            UUID.randomUUID().toString(),
            assistantResponse,
            "assistant",
            System.currentTimeMillis(),
            conversationId,
            citations
        );
    }
    
    private String buildSystemPrompt(String ragContext) {
        return """
            You are a helpful AI assistant. Answer questions based on the provided context documents when relevant.
            
            %s
            
            INSTRUCTIONS:
            - Use the provided documents to answer questions when relevant
            - Cite sources using [Source 1], [Source 2], etc. when referencing specific documents
            - If the documents don't contain relevant information, you can still answer using your general knowledge
            - Be accurate and helpful
            """.formatted(ragContext);
    }
    
    public ChatMessage processMessage(String userMessage, String conversationIdParam) {
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
            // Call Ollama API
            assistantResponse = callOllama(history);
        } catch (Exception e) {
            assistantResponse = "Sorry, I encountered an error connecting to Ollama: " + e.getMessage() + 
                              ". Make sure Ollama is running on " + ollamaUrl;
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
    
    private String callOllama(List<Map<String, String>> history) {
        // Detect if using OpenAI-compatible API (Groq, OpenAI, etc.) or local Ollama
        boolean isOpenAiCompatible = ollamaApiKey != null && !ollamaApiKey.isEmpty() && !ollamaUrl.contains("localhost:11434");
        
        String url;
        Map<String, Object> requestBody = new HashMap<>();
        
        if (isOpenAiCompatible) {
            // OpenAI-compatible API format (Groq, OpenAI, Azure, etc.)
            url = ollamaUrl + "/chat/completions";
            requestBody.put("model", ollamaModel);
            requestBody.put("messages", history);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2048);
            log.info("⚡ Using OpenAI-compatible API: {}", ollamaUrl);
        } else {
            // Local Ollama API format
            url = ollamaUrl + "/api/chat";
            requestBody.put("model", ollamaModel);
            requestBody.put("messages", history);
            requestBody.put("stream", false);
            log.info("🦙 Using local Ollama API: {}", ollamaUrl);
        }
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (ollamaApiKey != null && !ollamaApiKey.isEmpty()) {
            headers.set("Authorization", "Bearer " + ollamaApiKey);
        }
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // Make API call using exchange method
        @SuppressWarnings("null")
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url, 
            HttpMethod.POST, 
            entity, 
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null) {
            if (isOpenAiCompatible) {
                // Parse OpenAI-compatible response format
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            } else {
                // Parse Ollama response format
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        }
        
        return "I received an empty response from the AI.";
    }
    
    @SuppressWarnings("null")
    public void clearConversation(String conversationId) {
        // Delete all messages in the conversation
        List<Message> messages = messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(conversationId);
        if (messages != null && !messages.isEmpty()) {
            messageRepository.deleteAll(messages);
        }
        
        // Delete the conversation
        conversationRepository.findByConversationId(conversationId)
                .ifPresent(conversationRepository::delete);
    }
}
