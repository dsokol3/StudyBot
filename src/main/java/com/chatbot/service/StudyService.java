package com.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class StudyService {
    
    private static final Logger log = LoggerFactory.getLogger(StudyService.class);
    
    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaUrl;
    
    @Value("${ollama.model:llama3}")
    private String ollamaModel;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public Map<String, Object> generateSummary(String content) {
        String prompt = """
            Analyze the following study content and provide a comprehensive summary.
            
            CONTENT:
            %s
            
            Respond with a JSON object in this exact format (no markdown, just JSON):
            {
                "summary": "A comprehensive summary of the content in 2-3 paragraphs",
                "keyPoints": ["Key point 1", "Key point 2", "Key point 3", "...up to 5-7 key points"],
                "wordCount": <number of words in the summary>
            }
            """.formatted(content);
        
        return callOllamaForJson(prompt, createDefaultSummary());
    }
    
    public Map<String, Object> generateFlashcards(String content, int count) {
        String prompt = """
            Create %d flashcards from the following study content.
            
            CONTENT:
            %s
            
            Respond with a JSON object in this exact format (no markdown, just JSON):
            {
                "cards": [
                    {
                        "id": "card-1",
                        "front": "Question or term",
                        "back": "Answer or definition",
                        "difficulty": "easy|medium|hard"
                    }
                ],
                "totalCards": <number>
            }
            """.formatted(count, content);
        
        return callOllamaForJson(prompt, createDefaultFlashcards());
    }
    
    public Map<String, Object> generateQuestions(String content, int count) {
        String prompt = """
            Create %d multiple-choice practice questions from the following study content.
            
            CONTENT:
            %s
            
            Respond with a JSON object in this exact format (no markdown, just JSON):
            {
                "questions": [
                    {
                        "id": "q-1",
                        "question": "The question text",
                        "options": ["Option A", "Option B", "Option C", "Option D"],
                        "correctAnswer": 0,
                        "explanation": "Why this answer is correct"
                    }
                ],
                "totalQuestions": <number>
            }
            
            correctAnswer should be the index (0-3) of the correct option.
            """.formatted(count, content);
        
        return callOllamaForJson(prompt, createDefaultQuestions());
    }
    
    public Map<String, Object> generateEssayPrompts(String content, int count) {
        String prompt = """
            Create %d essay prompts based on the following study content.
            
            CONTENT:
            %s
            
            Respond with a JSON object in this exact format (no markdown, just JSON):
            {
                "prompts": [
                    {
                        "id": "essay-1",
                        "prompt": "The essay question or topic",
                        "suggestedLength": "500-750 words",
                        "keyPointsToAddress": ["Point 1", "Point 2", "Point 3"],
                        "difficulty": "beginner|intermediate|advanced"
                    }
                ],
                "totalPrompts": <number>
            }
            """.formatted(count, content);
        
        return callOllamaForJson(prompt, createDefaultEssayPrompts());
    }
    
    public Map<String, Object> explainText(String content) {
        String prompt = """
            Explain the following content in simpler terms and identify key terminology.
            
            CONTENT:
            %s
            
            Respond with a JSON object in this exact format (no markdown, just JSON):
            {
                "simplifiedText": "The content explained in simple, easy-to-understand language",
                "explanations": [
                    {
                        "term": "Technical term or concept",
                        "definition": "Simple definition",
                        "example": "Optional real-world example",
                        "relatedTerms": ["related term 1", "related term 2"]
                    }
                ]
            }
            """.formatted(content);
        
        return callOllamaForJson(prompt, createDefaultExplanations());
    }
    
    public Map<String, Object> generateDiagram(String content) {
        String prompt = """
            Create a concept map/diagram from the following study content.
            
            CONTENT:
            %s
            
            Respond with a JSON object in this exact format (no markdown, just JSON):
            {
                "mermaidCode": "graph TD\\n    A[Main Concept] --> B[Sub Concept 1]\\n    A --> C[Sub Concept 2]",
                "nodes": [
                    {"id": "A", "label": "Main Concept", "type": "concept"},
                    {"id": "B", "label": "Sub Concept 1", "type": "detail"}
                ],
                "edges": [
                    {"from": "A", "to": "B", "label": "relates to"}
                ],
                "description": "Brief description of the diagram"
            }
            
            Use valid Mermaid.js flowchart syntax with graph TD (top-down).
            """.formatted(content);
        
        return callOllamaForJson(prompt, createDefaultDiagram());
    }
    
    public Map<String, Object> generateStudyPlan(String content, String examDate, int hoursPerDay) {
        String prompt = """
            Create a study plan for the following content with exam date %s and %d hours available per day.
            
            CONTENT:
            %s
            
            Respond with a JSON object in this exact format (no markdown, just JSON):
            {
                "sessions": [
                    {
                        "id": "session-1",
                        "date": "2024-01-15",
                        "topic": "Topic name",
                        "duration": 120,
                        "activities": ["Activity 1", "Activity 2"]
                    }
                ],
                "totalHours": <total study hours>,
                "examDate": "%s",
                "recommendations": ["Study tip 1", "Study tip 2", "Study tip 3"]
            }
            
            duration is in minutes. Create a realistic schedule leading up to the exam.
            """.formatted(examDate, hoursPerDay, content, examDate);
        
        return callOllamaForJson(prompt, createDefaultStudyPlan(examDate));
    }
    
    private Map<String, Object> callOllamaForJson(String prompt, Map<String, Object> fallback) {
        try {
            String url = ollamaUrl + "/api/generate";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("format", "json");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("response")) {
                String jsonResponse = (String) responseBody.get("response");
                return objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            }
            
            return fallback;
        } catch (Exception e) {
            log.error("Error calling Ollama for study generation: {}", e.getMessage());
            return fallback;
        }
    }
    
    // Fallback response generators
    private Map<String, Object> createDefaultSummary() {
        Map<String, Object> result = new HashMap<>();
        result.put("summary", "Unable to generate summary. Please try again.");
        result.put("keyPoints", List.of("Content analysis pending"));
        result.put("wordCount", 0);
        return result;
    }
    
    private Map<String, Object> createDefaultFlashcards() {
        Map<String, Object> result = new HashMap<>();
        result.put("cards", List.of());
        result.put("totalCards", 0);
        return result;
    }
    
    private Map<String, Object> createDefaultQuestions() {
        Map<String, Object> result = new HashMap<>();
        result.put("questions", List.of());
        result.put("totalQuestions", 0);
        return result;
    }
    
    private Map<String, Object> createDefaultEssayPrompts() {
        Map<String, Object> result = new HashMap<>();
        result.put("prompts", List.of());
        result.put("totalPrompts", 0);
        return result;
    }
    
    private Map<String, Object> createDefaultExplanations() {
        Map<String, Object> result = new HashMap<>();
        result.put("simplifiedText", "Unable to generate explanation. Please try again.");
        result.put("explanations", List.of());
        return result;
    }
    
    private Map<String, Object> createDefaultDiagram() {
        Map<String, Object> result = new HashMap<>();
        result.put("mermaidCode", "graph TD\n    A[Content] --> B[Analysis Pending]");
        result.put("nodes", List.of(
            Map.of("id", "A", "label", "Content", "type", "concept"),
            Map.of("id", "B", "label", "Analysis Pending", "type", "detail")
        ));
        result.put("edges", List.of(Map.of("from", "A", "to", "B")));
        result.put("description", "Diagram generation pending");
        return result;
    }
    
    private Map<String, Object> createDefaultStudyPlan(String examDate) {
        Map<String, Object> result = new HashMap<>();
        result.put("sessions", List.of());
        result.put("totalHours", 0);
        result.put("examDate", examDate);
        result.put("recommendations", List.of("Create a study schedule", "Review materials regularly"));
        return result;
    }
}
