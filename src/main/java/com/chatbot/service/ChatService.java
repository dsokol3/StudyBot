package com.chatbot.service;

import com.chatbot.model.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {
    
    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaUrl;
    
    @Value("${ollama.api.key:}")
    private String ollamaApiKey;
    
    @Value("${ollama.model:llama3}")
    private String ollamaModel;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Store conversation history per conversation ID
    private final Map<String, List<Map<String, String>>> conversations = new ConcurrentHashMap<>();
    
    public ChatMessage processMessage(String userMessage, String conversationId) {
        // Initialize conversation if needed
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }
        
        conversations.putIfAbsent(conversationId, new ArrayList<>());
        List<Map<String, String>> history = conversations.get(conversationId);
        
        // Add user message to history
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        history.add(userMsg);
        
        String assistantResponse;
        
        try {
            // Call Ollama API
            assistantResponse = callOllama(history);
        } catch (Exception e) {
            assistantResponse = "Sorry, I encountered an error connecting to Ollama: " + e.getMessage() + 
                              ". Make sure Ollama is running on " + ollamaUrl;
        }
        
        // Add assistant response to history
        Map<String, String> assistantMsg = new HashMap<>();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", assistantResponse);
        history.add(assistantMsg);
        
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
        conversations.remove(conversationId);
    }
}
