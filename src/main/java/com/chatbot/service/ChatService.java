package com.chatbot.service;

import com.chatbot.model.ChatMessage;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    private OpenAiService openAiService;
    
    // Store conversation history per conversation ID
    private final Map<String, List<com.theokanning.openai.completion.chat.ChatMessage>> conversations = new ConcurrentHashMap<>();
    
    public ChatMessage processMessage(String userMessage, String conversationId) {
        // Initialize conversation if needed
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }
        
        conversations.putIfAbsent(conversationId, new ArrayList<>());
        List<com.theokanning.openai.completion.chat.ChatMessage> history = conversations.get(conversationId);
        
        // Add user message to history
        com.theokanning.openai.completion.chat.ChatMessage userMsg = 
            new com.theokanning.openai.completion.chat.ChatMessage("user", userMessage);
        history.add(userMsg);
        
        String assistantResponse;
        
        // Check if API key is configured
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            // Mock response for demo
            assistantResponse = generateMockResponse(userMessage);
        } else {
            // Use OpenAI API
            assistantResponse = callOpenAI(history);
        }
        
        // Add assistant response to history
        com.theokanning.openai.completion.chat.ChatMessage assistantMsg = 
            new com.theokanning.openai.completion.chat.ChatMessage("assistant", assistantResponse);
        history.add(assistantMsg);
        
        return new ChatMessage(
            UUID.randomUUID().toString(),
            assistantResponse,
            "assistant",
            System.currentTimeMillis()
        );
    }
    
    private String callOpenAI(List<com.theokanning.openai.completion.chat.ChatMessage> history) {
        try {
            if (openAiService == null) {
                openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30));
            }
            
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(history)
                .maxTokens(500)
                .temperature(0.7)
                .build();
            
            ChatCompletionResult result = openAiService.createChatCompletion(request);
            return result.getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            return "Sorry, I encountered an error: " + e.getMessage();
        }
    }
    
    private String generateMockResponse(String userMessage) {
        // Simple mock responses for demo
        String msg = userMessage.toLowerCase();
        
        if (msg.contains("hello") || msg.contains("hi")) {
            return "Hello! I'm your AI chatbot. How can I help you today?";
        } else if (msg.contains("how are you")) {
            return "I'm doing great, thank you! I'm here to assist you with any questions.";
        } else if (msg.contains("weather")) {
            return "I don't have access to real-time weather data, but I can help with other questions!";
        } else if (msg.contains("help")) {
            return "I'm an AI chatbot. You can ask me questions about various topics. To use full AI capabilities, configure your OpenAI API key in application.properties.";
        } else {
            return "That's an interesting question! I'm currently running in demo mode. To get AI-powered responses, please configure your OpenAI API key.";
        }
    }
    
    public void clearConversation(String conversationId) {
        conversations.remove(conversationId);
    }
}
