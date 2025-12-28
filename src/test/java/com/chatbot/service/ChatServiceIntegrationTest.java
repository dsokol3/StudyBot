package com.chatbot.service;

import com.chatbot.entity.Conversation;
import com.chatbot.entity.Message;
import com.chatbot.model.ChatResponse;
import com.chatbot.repository.ConversationRepository;
import com.chatbot.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService.
 * 
 * Tests:
 * - Groq API integration (mocked)
 * - Message processing with RAG
 * - Response labeling
 * - Conversation management
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceIntegrationTest {
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private RetrievalService retrievalService;
    
    @Mock
    private RestTemplate restTemplate;
    
    private ChatService chatService;
    
    @BeforeEach
    void setUp() {
        chatService = new ChatService(conversationRepository, messageRepository, retrievalService);
        
        // Set configuration values
        ReflectionTestUtils.setField(chatService, "llmApiUrl", "https://api.groq.com/openai/v1");
        ReflectionTestUtils.setField(chatService, "llmApiKey", "test-groq-api-key");
        ReflectionTestUtils.setField(chatService, "llmModel", "llama-3.1-8b-instant");
        ReflectionTestUtils.setField(chatService, "ragEnabled", true);
        
        // Replace the RestTemplate with our mock
        ReflectionTestUtils.setField(chatService, "restTemplate", restTemplate);
    }
    
    @Test
    @DisplayName("Should process message with Groq API and RAG context")
    void testProcessMessageWithRagContext() {
        // Setup
        String conversationId = "test-conv-123";
        String userMessage = "What is machine learning?";
        
        // Mock conversation repository
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setMessages(new ArrayList<>());
        when(conversationRepository.findByConversationId(conversationId))
            .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
            .thenReturn(conversation);
        
        // Mock message repository
        when(messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(conversationId))
            .thenReturn(new ArrayList<>());
        when(messageRepository.save(any(Message.class)))
            .thenAnswer(i -> i.getArgument(0));
        
        // Mock retrieval service - return some context
        when(retrievalService.hasDocuments(conversationId)).thenReturn(true);
        List<RetrievalService.RetrievedChunk> chunks = List.of(
            new RetrievalService.RetrievedChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "notes.txt",
                0,
                "Machine learning is a subset of AI...",
                100
            )
        );
        when(retrievalService.findRelevantChunks(userMessage, conversationId))
            .thenReturn(chunks);
        when(retrievalService.buildContext(chunks))
            .thenReturn("CONTEXT: Machine learning is a subset of AI...");
        when(retrievalService.buildCitations(chunks))
            .thenReturn(List.of(new RetrievalService.Citation(1, "doc-1", "notes.txt", 0)));
        
        // Mock Groq API response
        Map<String, Object> groqResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "Machine learning is a type of artificial intelligence...");
        choice.put("message", message);
        choices.add(choice);
        groqResponse.put("choices", choices);
        
        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(groqResponse);
        when(restTemplate.exchange(
            contains("/chat/completions"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(org.springframework.core.ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);
        
        // Execute
        ChatResponse response = chatService.processMessageWithRag(userMessage, conversationId);
        
        // Verify
        assertNotNull(response);
        assertNotNull(response.getContent());
        assertTrue(response.getContent().contains("📚 Answer from uploaded notes:"), 
            "Response should be labeled as from notes");
        assertEquals(conversationId, response.getConversationId());
        assertFalse(response.getCitations().isEmpty(), "Should have citations");
        
        // Verify Groq API was called
        verify(restTemplate).exchange(
            contains("/chat/completions"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(org.springframework.core.ParameterizedTypeReference.class)
        );
    }
    
    @Test
    @DisplayName("Should label response as from AI when no context found")
    void testProcessMessageWithoutContext() {
        // Setup
        String conversationId = "test-conv-456";
        String userMessage = "What is the weather?";
        
        // Mock conversation repository
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);
        conversation.setMessages(new ArrayList<>());
        when(conversationRepository.findByConversationId(conversationId))
            .thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class)))
            .thenReturn(conversation);
        
        // Mock message repository
        when(messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(conversationId))
            .thenReturn(new ArrayList<>());
        when(messageRepository.save(any(Message.class)))
            .thenAnswer(i -> i.getArgument(0));
        
        // Mock retrieval service - no documents
        when(retrievalService.hasDocuments(conversationId)).thenReturn(false);
        
        // Mock Groq API response
        Map<String, Object> groqResponse = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> choice = new HashMap<>();
        Map<String, Object> message = new HashMap<>();
        message.put("content", "I can help you check the weather...");
        choice.put("message", message);
        choices.add(choice);
        groqResponse.put("choices", choices);
        
        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(groqResponse);
        when(restTemplate.exchange(
            contains("/chat/completions"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(org.springframework.core.ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);
        
        // Execute
        ChatResponse response = chatService.processMessageWithRag(userMessage, conversationId);
        
        // Verify
        assertNotNull(response);
        assertTrue(response.getContent().contains("🤖 Answer from AI:"), 
            "Response should be labeled as from AI");
        assertTrue(response.getCitations().isEmpty(), "Should have no citations");
    }
    
    @Test
    @DisplayName("Should verify Groq API URL is used correctly")
    void testGroqApiUrlConstruction() {
        // Setup
        String conversationId = "test-conv-789";
        
        // Mock repositories
        when(conversationRepository.findByConversationId(anyString()))
            .thenReturn(Optional.of(new Conversation()));
        when(conversationRepository.save(any()))
            .thenReturn(new Conversation());
        when(messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(anyString()))
            .thenReturn(new ArrayList<>());
        when(messageRepository.save(any()))
            .thenAnswer(i -> i.getArgument(0));
        when(retrievalService.hasDocuments(anyString()))
            .thenReturn(false);
        
        // Mock API response
        Map<String, Object> groqResponse = new HashMap<>();
        groqResponse.put("choices", List.of(Map.of("message", Map.of("content", "Response"))));
        when(restTemplate.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            any(org.springframework.core.ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(groqResponse));
        
        // Execute
        chatService.processMessageWithRag("Test", conversationId);
        
        // Capture the URL used
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(
            urlCaptor.capture(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            any(org.springframework.core.ParameterizedTypeReference.class)
        );
        
        // Verify Groq URL
        String capturedUrl = urlCaptor.getValue();
        assertTrue(capturedUrl.contains("api.groq.com"), "Should use Groq API");
        assertTrue(capturedUrl.contains("/chat/completions"), "Should use chat completions endpoint");
    }
    
    @Test
    @DisplayName("Should verify Authorization header is set")
    void testAuthorizationHeader() {
        // Setup
        when(conversationRepository.findByConversationId(anyString()))
            .thenReturn(Optional.of(new Conversation()));
        when(conversationRepository.save(any()))
            .thenReturn(new Conversation());
        when(messageRepository.findByConversationConversationIdOrderByCreatedAtAsc(anyString()))
            .thenReturn(new ArrayList<>());
        when(messageRepository.save(any()))
            .thenAnswer(i -> i.getArgument(0));
        when(retrievalService.hasDocuments(anyString()))
            .thenReturn(false);
        
        // Mock API response
        Map<String, Object> groqResponse = new HashMap<>();
        groqResponse.put("choices", List.of(Map.of("message", Map.of("content", "Response"))));
        when(restTemplate.exchange(
            anyString(),
            any(HttpMethod.class),
            any(HttpEntity.class),
            any(org.springframework.core.ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(groqResponse));
        
        // Execute
        chatService.processMessageWithRag("Test", "conv-123");
        
        // Capture the HttpEntity used
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = 
            ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
            anyString(),
            any(HttpMethod.class),
            entityCaptor.capture(),
            any(org.springframework.core.ParameterizedTypeReference.class)
        );
        
        // Verify Authorization header
        HttpEntity<Map<String, Object>> entity = entityCaptor.getValue();
        HttpHeaders headers = entity.getHeaders();
        String authHeader = headers.getFirst("Authorization");
        
        assertNotNull(authHeader, "Authorization header should be set");
        assertTrue(authHeader.startsWith("Bearer "), "Should be Bearer token");
        assertEquals("Bearer test-groq-api-key", authHeader);
    }
}
