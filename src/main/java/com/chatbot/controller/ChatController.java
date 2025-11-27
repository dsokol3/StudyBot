package com.chatbot.controller;

import com.chatbot.model.ChatMessage;
import com.chatbot.model.ChatRequest;
import com.chatbot.model.ChatResponse;
import com.chatbot.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {
    
    @Autowired
    private ChatService chatService;
    
    @PostMapping("/message")
    public CompletableFuture<ResponseEntity<ChatResponse>> sendMessage(@RequestBody ChatRequest request) {
        // Process message asynchronously
        return CompletableFuture.supplyAsync(() -> {
            ChatMessage response = chatService.processMessage(
                request.getMessage(), 
                request.getConversationId()
            );
            
            ChatResponse chatResponse = new ChatResponse(
                response.getContent(),
                request.getConversationId(),
                response.getTimestamp()
            );
            
            return ResponseEntity.ok(chatResponse);
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
