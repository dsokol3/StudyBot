package com.chatbot.controller;

import com.chatbot.service.StudyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for AI-powered study guide generation.
 */
@RestController
@RequestMapping("/api/study")
public class StudyController {
    
    private static final Logger log = LoggerFactory.getLogger(StudyController.class);
    
    private final StudyService studyService;
    
    public StudyController(StudyService studyService) {
        this.studyService = studyService;
    }
    
    /**
     * Generate a summary of the provided content.
     */
    @PostMapping("/generate/summary")
    public ResponseEntity<Map<String, Object>> generateSummary(@RequestBody Map<String, Object> request) {
        log.info("Generating summary");
        String content = (String) request.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        
        Map<String, Object> result = studyService.generateSummary(content);
        result.put("type", "summary");
        return ResponseEntity.ok(result);
    }
    
    /**
     * Generate flashcards from the provided content.
     */
    @PostMapping("/generate/flashcards")
    public ResponseEntity<Map<String, Object>> generateFlashcards(@RequestBody Map<String, Object> request) {
        log.info("Generating flashcards");
        String content = (String) request.get("content");
        int count = request.containsKey("count") ? ((Number) request.get("count")).intValue() : 10;
        
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        
        Map<String, Object> result = studyService.generateFlashcards(content, count);
        result.put("type", "flashcards");
        return ResponseEntity.ok(result);
    }
    
    /**
     * Generate practice questions from the provided content.
     */
    @PostMapping("/generate/questions")
    public ResponseEntity<Map<String, Object>> generateQuestions(@RequestBody Map<String, Object> request) {
        log.info("Generating practice questions");
        String content = (String) request.get("content");
        int count = request.containsKey("count") ? ((Number) request.get("count")).intValue() : 5;
        
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        
        Map<String, Object> result = studyService.generateQuestions(content, count);
        result.put("type", "questions");
        return ResponseEntity.ok(result);
    }
    
    /**
     * Generate essay prompts from the provided content.
     */
    @PostMapping("/generate/essay-prompts")
    public ResponseEntity<Map<String, Object>> generateEssayPrompts(@RequestBody Map<String, Object> request) {
        log.info("Generating essay prompts");
        String content = (String) request.get("content");
        int count = request.containsKey("count") ? ((Number) request.get("count")).intValue() : 3;
        
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        
        Map<String, Object> result = studyService.generateEssayPrompts(content, count);
        result.put("type", "essay-prompts");
        return ResponseEntity.ok(result);
    }
    
    /**
     * Explain complex text in simpler terms.
     */
    @PostMapping("/generate/explain")
    public ResponseEntity<Map<String, Object>> explainText(@RequestBody Map<String, Object> request) {
        log.info("Generating explanations");
        String content = (String) request.get("content");
        
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        
        Map<String, Object> result = studyService.explainText(content);
        result.put("type", "explanations");
        return ResponseEntity.ok(result);
    }
    
    /**
     * Generate a concept diagram from the provided content.
     */
    @PostMapping("/generate/diagram")
    public ResponseEntity<Map<String, Object>> generateDiagram(@RequestBody Map<String, Object> request) {
        log.info("Generating diagram");
        String content = (String) request.get("content");
        
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        
        Map<String, Object> result = studyService.generateDiagram(content);
        result.put("type", "diagrams");
        return ResponseEntity.ok(result);
    }
    
    /**
     * Generate a study plan based on content and exam date.
     */
    @PostMapping("/generate/study-plan")
    public ResponseEntity<Map<String, Object>> generateStudyPlan(@RequestBody Map<String, Object> request) {
        log.info("Generating study plan");
        String content = (String) request.get("content");
        String examDate = (String) request.get("examDate");
        int hoursPerDay = request.containsKey("hoursPerDay") ? ((Number) request.get("hoursPerDay")).intValue() : 2;
        
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
        if (examDate == null || examDate.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Exam date is required"));
        }
        
        Map<String, Object> result = studyService.generateStudyPlan(content, examDate, hoursPerDay);
        result.put("type", "study-plan");
        return ResponseEntity.ok(result);
    }
    
    /**
     * Upload study notes endpoint (delegates to document controller).
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadNotes() {
        // This is a placeholder - the actual upload is handled by DocumentController
        // This endpoint exists for API completeness
        return ResponseEntity.ok(Map.of(
            "message", "Use /api/documents/upload for file uploads"
        ));
    }
}
