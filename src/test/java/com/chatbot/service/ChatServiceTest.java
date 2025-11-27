package com.chatbot.service;

import com.chatbot.model.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class ChatServiceTest {

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService();
        // Set local Ollama URL for testing (won't actually call API in unit tests)
        ReflectionTestUtils.setField(chatService, "ollamaUrl", "http://localhost:11434");
        ReflectionTestUtils.setField(chatService, "ollamaModel", "llama3");
        ReflectionTestUtils.setField(chatService, "ollamaApiKey", "");
    }

    @Test
    void testProcessMessage_CreatesNewConversation() {
        String userMessage = "Hello";
        
        ChatMessage response = chatService.processMessage(userMessage, null);
        
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("assistant", response.getSender());
        assertNotNull(response.getContent());
        assertTrue(response.getTimestamp() > 0);
    }

    @Test
    void testProcessMessage_UsesExistingConversation() {
        String conversationId = "test-conversation-123";
        String userMessage = "How are you?";
        
        ChatMessage response = chatService.processMessage(userMessage, conversationId);
        
        assertNotNull(response);
        assertEquals("assistant", response.getSender());
    }

    @Test
    void testClearConversation() {
        String conversationId = "test-conversation-456";
        
        // Create a conversation
        chatService.processMessage("Hello", conversationId);
        
        // Clear it
        assertDoesNotThrow(() -> chatService.clearConversation(conversationId));
    }

    @Test
    void testProcessMessage_HandlesEmptyMessage() {
        String emptyMessage = "";
        
        ChatMessage response = chatService.processMessage(emptyMessage, null);
        
        assertNotNull(response);
        assertNotNull(response.getContent());
    }
}
