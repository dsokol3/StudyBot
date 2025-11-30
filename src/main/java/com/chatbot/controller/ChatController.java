package com.chatbot.controller;

import com.chatbot.model.ChatRequest;
import com.chatbot.model.ChatResponse;
import com.chatbot.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

// Controller to handle chat-related endpoints
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    // Service to handle chat logic
    @Autowired
    private ChatService chatService;

    // Handle incoming chat messages with RAG support
    @PostMapping("/message")
    public CompletableFuture<ResponseEntity<ChatResponse>> sendMessage(@RequestBody ChatRequest request) {
        // Process message asynchronously with RAG
        return CompletableFuture.supplyAsync(() -> {
            ChatResponse response = chatService.processMessageWithRag(
                request.getMessage(), 
                request.getConversationId()
            );
            
            return ResponseEntity.ok(response);
        });
    }
    
    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Void> clearConversation(@PathVariable String conversationId) {
        chatService.clearConversation(conversationId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ChatBot API is running!");
    }
}
