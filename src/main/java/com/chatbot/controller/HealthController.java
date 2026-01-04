package com.chatbot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Health check controller providing multiple endpoints for different health check scenarios.
 * - /api/health - Simple liveness check (always returns 200 if app is running)
 * - /health - Root-level health check for load balancers
 * - / - Basic status with API documentation
 * - Spring Actuator provides /actuator/health for detailed health with DB status
 */
@RestController
public class HealthController {
    
    /**
     * Simple liveness probe - returns 200 if the application is running.
     * Does NOT check database to prevent startup failures.
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> apiHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        ));
    }
    
    /**
     * Root-level health endpoint for cloud platforms like Render.
     * Many platforms check / or /health for container health.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> rootHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "chatbot-api",
            "timestamp", Instant.now().toString()
        ));
    }
    
    /**
     * API documentation endpoint (not root path to allow static file serving).
     * Provides basic status and API documentation.
     */
    @GetMapping("/api")
    public ResponseEntity<Map<String, Object>> apiInfo() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "ChatBot API is running",
            "version", "1.0-SNAPSHOT",
            "timestamp", Instant.now().toString(),
            "endpoints", Map.of(
                "health", List.of(
                    "GET / - Basic status and API info",
                    "GET /health - Health check for load balancers", 
                    "GET /api/health - Simple liveness probe",
                    "GET /actuator/health - Detailed health (Spring Boot Actuator)"
                ),
                "chat", List.of(
                    "POST /api/chat/message - Send chat message with optional document context"
                ),
                "documents", List.of(
                    "POST /api/documents/upload - Upload document for processing",
                    "GET /api/documents/{id} - Get document info",
                    "GET /api/documents/{id}/status - Get processing status",
                    "GET /api/documents/{id}/content - Get document content",
                    "GET /api/documents/conversation/{conversationId} - List documents in conversation",
                    "GET /api/documents/conversation/{conversationId}/content - Get all content in conversation"
                ),
                "study_tools", List.of(
                    "POST /api/study/generate/summary - Generate document summary",
                    "POST /api/study/generate/flashcards - Generate flashcards",
                    "POST /api/study/generate/questions - Generate practice questions", 
                    "POST /api/study/generate/essay-prompts - Generate essay prompts",
                    "POST /api/study/generate/explain - Explain concepts",
                    "POST /api/study/generate/diagram - Generate diagrams",
                    "POST /api/study/generate/study-plan - Create study plan"
                )
            ),
            "features", List.of(
                "RAG (Retrieval-Augmented Generation) with document context",
                "Vector similarity search for relevant content retrieval",
                "Multiple AI models (Groq for chat, Google Gemini for embeddings)",
                "Document processing with chunking and embedding",
                "Study tools: summaries, flashcards, questions, diagrams",
                "Conversation-based document organization"
            )
        ));
    }
}
