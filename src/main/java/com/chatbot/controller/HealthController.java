package com.chatbot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health check controller providing multiple endpoints for different health check scenarios.
 * - /api/health - Simple liveness check (always returns 200 if app is running)
 * - /health - Root-level health check for load balancers
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
     * Root path - useful for basic connectivity checks.
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> root() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "ChatBot API is running"
        ));
    }
}
