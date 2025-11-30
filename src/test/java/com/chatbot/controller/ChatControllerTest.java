package com.chatbot.controller;

import com.chatbot.service.ChatService;
import com.chatbot.model.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    private ChatMessage mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new ChatMessage(
            "test-id",
            "Hello! I'm your AI chatbot.",
            "assistant",
            System.currentTimeMillis()
        );
    }

    @Test
    void testSendMessage() throws Exception {
        when(chatService.processMessage(anyString(), anyString()))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Hello\",\"conversationId\":\"test-123\"}"))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk());
    }

    @Test
    void testClearConversation() throws Exception {
        mockMvc.perform(delete("/api/chat/conversation/test-123"))
                .andExpect(status().isOk());
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/chat/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("ChatBot API is running!"));
    }
}
