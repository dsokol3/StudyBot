package com.chatbot.service;

import com.chatbot.model.ChatMessage;
import com.chatbot.repository.ConversationRepository;
import com.chatbot.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private ChatService chatService;
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private RetrievalService retrievalService;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() {
        chatService = new ChatService(conversationRepository, messageRepository, retrievalService);
        // Set local Ollama URL for testing (won't actually call API in unit tests)
        @SuppressWarnings("null")
        Object service = chatService;
        ReflectionTestUtils.setField(service, "ollamaUrl", "http://localhost:11434");
        ReflectionTestUtils.setField(service, "ollamaModel", "llama3");
        ReflectionTestUtils.setField(service, "ollamaApiKey", "");
        
        // Mock repository behavior
        when(conversationRepository.findByConversationId(anyString())).thenReturn(Optional.empty());
        when(conversationRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(messageRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(anyString())).thenReturn(new ArrayList<>());
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
