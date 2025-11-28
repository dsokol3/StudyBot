package com.chatbot.service;

import com.chatbot.entity.Conversation;
import com.chatbot.entity.Message;
import com.chatbot.model.ChatMessage;
import com.chatbot.repository.ConversationRepository;
import com.chatbot.repository.MessageRepository;
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
    
    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaUrl;
    
    @Value("${ollama.api.key:}")
    private String ollamaApiKey;
    
    @Value("${ollama.model:llama3}")
    private String ollamaModel;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    
    public ChatService(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
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
        String url = ollamaUrl + "/api/chat";
        
        // Prepare request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel);
        requestBody.put("messages", history);
        requestBody.put("stream", false);
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (ollamaApiKey != null && !ollamaApiKey.isEmpty()) {
            headers.set("Authorization", "Bearer " + ollamaApiKey);
        }
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // Make API call using exchange method
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url, 
            HttpMethod.POST, 
            entity, 
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        
        Map<String, Object> responseBody = response.getBody();
        if (responseBody != null) {
            // Parse Ollama's response format
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) responseBody.get("message");
            if (message != null) {
                return (String) message.get("content");
            }
        }
        
        return "I received an empty response from Ollama.";
    }
    
    public void clearConversation(String conversationId) {
        // Delete all messages in the conversation
        List<Message> messages = messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(conversationId);
        messageRepository.deleteAll(messages);
        
        // Delete the conversation
        conversationRepository.findByConversationId(conversationId)
                .ifPresent(conversationRepository::delete);
    }
}
