package com.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
public class StudyService {
    
    private static final Logger log = LoggerFactory.getLogger(StudyService.class);
    
    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaUrl;
    
    @Value("${ollama.model:llama3}")
    private String ollamaModel;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public StudyService(RestTemplateBuilder restTemplateBuilder) {
        // Configure RestTemplate with longer timeouts for AI generation
        this.restTemplate = restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofMinutes(3)) // AI generation can take a while
            .build();
    }
    
    public Map<String, Object> generateSummary(String content) {
        String prompt = """
            You are an expert educator creating study materials. Analyze the following content carefully and provide a comprehensive, well-structured summary.
            
            STUDY CONTENT TO SUMMARIZE:
            %s
            
            INSTRUCTIONS:
            1. Read and understand the entire content thoroughly
            2. Identify the main topics, themes, and concepts
            3. Create a clear, organized summary that captures all essential information
            4. Extract 5-7 key points that a student must remember
            5. Make the summary educational and easy to understand
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "summary": "A comprehensive 2-3 paragraph summary that explains the main concepts clearly. Include important details, relationships between ideas, and any critical information a student needs to understand the material.",
                "keyPoints": ["Key point 1 - be specific and informative", "Key point 2", "Key point 3", "Key point 4", "Key point 5"],
                "wordCount": <number of words in the summary>
            }
            """.formatted(content);
        
        return callOllamaForJson(prompt, createDefaultSummary());
    }
    
    public Map<String, Object> generateFlashcards(String content, int count) {
        String prompt = """
            You are an expert educator creating flashcards for effective studying. Create exactly %d high-quality flashcards from the following study content.
            
            STUDY CONTENT:
            %s
            
            INSTRUCTIONS:
            1. Create flashcards that test understanding of key concepts, definitions, and relationships
            2. Front of card should be a clear question or term
            3. Back of card should be a complete, accurate answer or definition
            4. Vary the difficulty levels (easy, medium, hard) appropriately
            5. Focus on the most important information students need to memorize
            6. Make questions specific and answers concise but complete
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "cards": [
                    {
                        "id": "card-1",
                        "front": "What is [specific concept/term from the content]?",
                        "back": "Complete, accurate answer that explains the concept clearly",
                        "difficulty": "easy"
                    },
                    {
                        "id": "card-2", 
                        "front": "Explain the relationship between [concept A] and [concept B]",
                        "back": "Clear explanation of the relationship",
                        "difficulty": "medium"
                    }
                ],
                "totalCards": %d
            }
            
            Create exactly %d cards with varied difficulty levels.
            """.formatted(count, content, count, count);
        
        return callOllamaForJson(prompt, createDefaultFlashcards());
    }
    
    public Map<String, Object> generateQuestions(String content, int count) {
        String prompt = """
            You are an expert educator creating practice exam questions. Create exactly %d high-quality multiple-choice questions from the following study content.
            
            STUDY CONTENT:
            %s
            
            INSTRUCTIONS:
            1. Create questions that test comprehension, not just memorization
            2. Each question should have exactly 4 options (A, B, C, D)
            3. Make wrong answers plausible but clearly incorrect
            4. Provide a clear explanation for why the correct answer is right
            5. Cover different aspects of the content
            6. Make questions clear and unambiguous
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "questions": [
                    {
                        "id": "q-1",
                        "question": "Clear, specific question about the content?",
                        "options": ["Option A - plausible answer", "Option B - correct answer", "Option C - plausible wrong answer", "Option D - plausible wrong answer"],
                        "correctAnswer": 1,
                        "explanation": "This is correct because [clear explanation based on the content]"
                    }
                ],
                "totalQuestions": %d
            }
            
            IMPORTANT: correctAnswer is the index (0-3) of the correct option in the options array.
            Create exactly %d questions.
            """.formatted(count, content, count, count);
        
        return callOllamaForJson(prompt, createDefaultQuestions());
    }
    
    public Map<String, Object> generateEssayPrompts(String content, int count) {
        String prompt = """
            You are an expert educator creating essay prompts for students. Create exactly %d thought-provoking essay prompts from the following study content.
            
            STUDY CONTENT:
            %s
            
            INSTRUCTIONS:
            1. Create prompts that require critical thinking and synthesis of ideas
            2. Vary difficulty levels (beginner, intermediate, advanced)
            3. Provide 3-5 specific key points students should address
            4. Suggest appropriate essay lengths
            5. Make prompts open-ended but focused
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "prompts": [
                    {
                        "id": "essay-1",
                        "prompt": "Analyze [specific topic from content]. Discuss [specific aspects to cover] and explain [what students should demonstrate understanding of].",
                        "suggestedLength": "500-750 words",
                        "keyPointsToAddress": [
                            "First specific point to discuss",
                            "Second key aspect to analyze", 
                            "Third element to include",
                            "Fourth consideration"
                        ],
                        "difficulty": "intermediate"
                    }
                ],
                "totalPrompts": %d
            }
            
            Create exactly %d essay prompts with varied difficulty.
            """.formatted(count, content, count, count);
        
        return callOllamaForJson(prompt, createDefaultEssayPrompts());
    }
    
    public Map<String, Object> explainText(String content) {
        String prompt = """
            You are an expert educator helping students understand complex material. Explain the following content in simpler terms and identify key terminology.
            
            STUDY CONTENT TO EXPLAIN:
            %s
            
            INSTRUCTIONS:
            1. Rewrite the content in clear, simple language that a student can easily understand
            2. Identify 3-5 key terms or concepts that need explanation
            3. Provide clear definitions and real-world examples for each term
            4. Show how terms relate to each other
            5. Keep explanations accessible but accurate
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "simplifiedText": "A clear, easy-to-understand explanation of the content. Use simple language, short sentences, and concrete examples. Break down complex ideas into digestible parts. This should be 2-4 paragraphs that help a student grasp the main concepts.",
                "explanations": [
                    {
                        "term": "Key term or concept from the content",
                        "definition": "Clear, simple definition of this term",
                        "example": "A concrete, relatable example that illustrates this concept",
                        "relatedTerms": ["related concept 1", "related concept 2"]
                    }
                ]
            }
            
            Include 3-5 term explanations covering the most important concepts.
            """.formatted(content);
        
        return callOllamaForJson(prompt, createDefaultExplanations());
    }
    
    public Map<String, Object> generateDiagram(String content) {
        String prompt = """
            You are an expert at creating visual concept maps. Create a clear concept diagram from the following study content using Mermaid.js flowchart syntax.
            
            STUDY CONTENT:
            %s
            
            INSTRUCTIONS:
            1. Identify the main concept and 3-6 related sub-concepts
            2. Create a hierarchical or relational diagram showing how concepts connect
            3. Use clear, concise labels for each node
            4. Show meaningful relationships between concepts
            5. Keep the diagram simple enough to be readable
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "mermaidCode": "graph TD\\n    A[Main Concept] --> B[Sub Concept 1]\\n    A --> C[Sub Concept 2]\\n    B --> D[Detail 1]\\n    C --> E[Detail 2]",
                "nodes": [
                    {"id": "A", "label": "Main Concept", "type": "concept"},
                    {"id": "B", "label": "Sub Concept 1", "type": "concept"},
                    {"id": "C", "label": "Sub Concept 2", "type": "concept"},
                    {"id": "D", "label": "Detail 1", "type": "detail"},
                    {"id": "E", "label": "Detail 2", "type": "detail"}
                ],
                "edges": [
                    {"from": "A", "to": "B", "label": "includes"},
                    {"from": "A", "to": "C", "label": "includes"},
                    {"from": "B", "to": "D"},
                    {"from": "C", "to": "E"}
                ],
                "description": "Brief description of what this diagram shows"
            }
            
            IMPORTANT: Use valid Mermaid.js graph TD (top-down) syntax. Use letters A-Z for node IDs. 
            Escape special characters in labels. Use --> for arrows.
            """.formatted(content);
        
        return callOllamaForJson(prompt, createDefaultDiagram());
    }
    
    public Map<String, Object> generateStudyPlan(String content, String examDate, int hoursPerDay) {
        String prompt = """
            You are an expert study coach. Create a detailed, realistic study plan for the following content.
            
            STUDY CONTENT TO COVER:
            %s
            
            EXAM DATE: %s
            AVAILABLE STUDY TIME: %d hours per day
            
            INSTRUCTIONS:
            1. Divide the content into logical study topics
            2. Create a day-by-day study schedule leading to the exam
            3. Allocate appropriate time for each topic based on complexity
            4. Include review sessions and practice activities
            5. Provide 3-5 practical study tips
            6. Make the plan realistic and achievable
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "sessions": [
                    {
                        "id": "session-1",
                        "date": "2024-01-15",
                        "topic": "Specific topic to study",
                        "duration": 120,
                        "activities": [
                            "Read and take notes on [specific section]",
                            "Create flashcards for key terms",
                            "Practice with sample problems"
                        ]
                    }
                ],
                "totalHours": 20,
                "examDate": "%s",
                "recommendations": [
                    "Study in 45-minute focused sessions with 10-minute breaks",
                    "Review previous material before starting new topics",
                    "Use active recall techniques rather than passive reading",
                    "Get adequate sleep before the exam"
                ]
            }
            
            IMPORTANT: 
            - duration is in MINUTES
            - Create realistic sessions that fit within the daily hour limit
            - Date format should be YYYY-MM-DD
            - Include 3-7 study sessions depending on time until exam
            """.formatted(content, examDate, hoursPerDay, examDate);
        
        return callOllamaForJson(prompt, createDefaultStudyPlan(examDate));
    }
    
    private Map<String, Object> callOllamaForJson(String prompt, Map<String, Object> fallback) {
        try {
            String url = ollamaUrl + "/api/generate";
            
            // Truncate content if too large to prevent very long generation times
            String truncatedPrompt = prompt;
            if (prompt.length() > 8000) {
                log.warn("Prompt too long ({} chars), truncating to 8000", prompt.length());
                truncatedPrompt = prompt.substring(0, 8000) + "\n\n[Content truncated for processing...]";
            }
            
            log.info("Calling Ollama model {} with prompt of {} chars", ollamaModel, truncatedPrompt.length());
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", truncatedPrompt);
            requestBody.put("stream", false);
            requestBody.put("format", "json");
            // Add options for better generation
            Map<String, Object> options = new HashMap<>();
            options.put("num_predict", 4096); // Allow longer responses for detailed content
            options.put("temperature", 0.7);  // Balance creativity and consistency
            options.put("top_p", 0.9);        // Nucleus sampling for quality
            requestBody.put("options", options);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Ollama response received in {} ms", elapsed);
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("response")) {
                String jsonResponse = (String) responseBody.get("response");
                log.debug("Ollama JSON response: {}", jsonResponse.substring(0, Math.min(200, jsonResponse.length())));
                return objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            }
            
            log.warn("Ollama response did not contain 'response' field");
            return fallback;
        } catch (Exception e) {
            log.error("Error calling Ollama for study generation: {}", e.getMessage(), e);
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
