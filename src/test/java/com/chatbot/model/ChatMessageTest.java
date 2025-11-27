package com.chatbot.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageTest {

    @Test
    void testChatMessageCreation() {
        String id = "test-id";
        String content = "Test message";
        String sender = "user";
        long timestamp = System.currentTimeMillis();

        ChatMessage message = new ChatMessage(id, content, sender, timestamp);

        assertEquals(id, message.getId());
        assertEquals(content, message.getContent());
        assertEquals(sender, message.getSender());
        assertEquals(timestamp, message.getTimestamp());
    }

    @Test
    void testChatMessageSetters() {
        ChatMessage message = new ChatMessage();
        
        message.setId("new-id");
        message.setContent("New content");
        message.setSender("assistant");
        message.setTimestamp(12345L);

        assertEquals("new-id", message.getId());
        assertEquals("New content", message.getContent());
        assertEquals("assistant", message.getSender());
        assertEquals(12345L, message.getTimestamp());
    }
}
