package com.chatbot.model;

import com.chatbot.service.RetrievalService.Citation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String id;
    private String content;
    private String sender;
    private long timestamp;
    private String conversationId;
    private List<Citation> citations = new ArrayList<>();
    
    // Constructor for backwards compatibility
    public ChatResponse(String message, String conversationId, long timestamp) {
        this.content = message;
        this.conversationId = conversationId;
        this.timestamp = timestamp;
        this.sender = "assistant";
        this.citations = new ArrayList<>();
    }
}
