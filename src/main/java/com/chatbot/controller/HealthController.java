package com.chatbot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        // Try a lightweight DB check to ensure DB is up (optional, comment out if not desired)
        try {
            // If you want to check DB, inject a JdbcTemplate and run a simple query here
            // jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.status(503).body("DB unavailable");
        }
    }
}
