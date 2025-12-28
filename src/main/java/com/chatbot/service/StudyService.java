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

/**
 * Study Tools Service - generates study materials using Groq API.
 * 
 * UPDATED: Now uses GROQ ONLY for text generation (no Ollama dependency).
 * 
 * Features:
 * - Summaries, flashcards, questions, essay prompts
 * - Explanations, diagrams, study plans
 * - JSON response parsing with fallbacks
 */
@Service
public class StudyService {
    
    private static final Logger log = LoggerFactory.getLogger(StudyService.class);
    
    // LLM Configuration (Groq API)
    @Value("${llm.api.url:https://api.groq.com/openai/v1}")
    private String llmApiUrl;
    
    @Value("${llm.api.key:}")
    private String llmApiKey;
    
    @Value("${llm.model:llama-3.1-8b-instant}")
    private String llmModel;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public StudyService(RestTemplateBuilder restTemplateBuilder) {
        // Configure RestTemplate with reasonable timeouts
        this.restTemplate = restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(30))
            .readTimeout(Duration.ofMinutes(2))
            .build();
        
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║  📚 StudyService initialized with Groq API                   ║");
        log.info("║  Model: llama-3.1-8b-instant (fast inference)               ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
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
        
        return callLlmForJson(prompt, createDefaultSummary());
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
        
        return callLlmForJson(prompt, createDefaultFlashcards());
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
        
        return callLlmForJson(prompt, createDefaultQuestions());
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
        
        return callLlmForJson(prompt, createDefaultEssayPrompts());
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
        
        return callLlmForJson(prompt, createDefaultExplanations());
    }
    
    public Map<String, Object> generateDiagram(String content, String diagramType) {
        String prompt = getDiagramPrompt(content, diagramType);
        return callLlmForJson(prompt, createDefaultDiagram(diagramType));
    }
    
    private String getDiagramPrompt(String content, String diagramType) {
        return switch (diagramType) {
            case "timeline" -> getTimelinePrompt(content);
            case "flowchart" -> getFlowchartPrompt(content);
            case "hierarchy" -> getHierarchyPrompt(content);
            case "mind-map" -> getMindMapPrompt(content);
            case "sequence" -> getSequencePrompt(content);
            default -> getConceptMapPrompt(content);
        };
    }
    
    private String getConceptMapPrompt(String content) {
        return """
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
                    {"id": "B", "label": "Sub Concept 1", "type": "concept"}
                ],
                "edges": [
                    {"from": "A", "to": "B", "label": "includes"}
                ],
                "description": "Brief description of what this diagram shows"
            }
            
            IMPORTANT: Use valid Mermaid.js graph TD (top-down) syntax. Use letters A-Z for node IDs. 
            Escape special characters in labels. Use --> for arrows.
            """.formatted(content);
    }
    
    private String getTimelinePrompt(String content) {
        return """
            You are an expert at creating visual timelines. Create a chronological timeline diagram from the following study content using Mermaid.js syntax.
            
            STUDY CONTENT:
            %s
            
            INSTRUCTIONS:
            1. Identify key events, dates, or periods mentioned in the content
            2. Arrange them in chronological order from earliest to latest
            3. Use clear, concise labels with dates/periods if available
            4. Show the progression of events over time
            5. Include 4-8 events for readability
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "mermaidCode": "graph LR\\n    A[1776: Declaration] --> B[1787: Constitution]\\n    B --> C[1791: Bill of Rights]\\n    C --> D[1803: Louisiana Purchase]",
                "nodes": [
                    {"id": "A", "label": "1776: Declaration of Independence", "type": "concept"},
                    {"id": "B", "label": "1787: Constitution Ratified", "type": "concept"}
                ],
                "edges": [
                    {"from": "A", "to": "B", "label": "leads to"}
                ],
                "description": "Timeline showing chronological order of events"
            }
            
            IMPORTANT: Use valid Mermaid.js graph LR (left-to-right) syntax for horizontal timeline flow.
            Put dates/periods at the start of each node label. Use --> for arrows showing progression.
            """.formatted(content);
    }
    
    private String getFlowchartPrompt(String content) {
        return """
            You are an expert at creating flowcharts. Create a process flowchart from the following study content using Mermaid.js syntax.
            
            STUDY CONTENT:
            %s
            
            INSTRUCTIONS:
            1. Identify the main process, steps, or decision points
            2. Create a flowchart showing the sequence of steps
            3. Use diamond shapes {Decision?} for decision points with Yes/No branches
            4. Use rectangular shapes [Step] for actions/steps
            5. Show the flow from start to end
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "mermaidCode": "graph TD\\n    A[Start] --> B[Step 1]\\n    B --> C{Decision?}\\n    C -->|Yes| D[Action A]\\n    C -->|No| E[Action B]\\n    D --> F[End]\\n    E --> F",
                "nodes": [
                    {"id": "A", "label": "Start", "type": "concept"},
                    {"id": "C", "label": "Decision Point", "type": "detail"}
                ],
                "edges": [
                    {"from": "A", "to": "B"},
                    {"from": "C", "to": "D", "label": "Yes"}
                ],
                "description": "Flowchart showing the process flow"
            }
            
            IMPORTANT: Use valid Mermaid.js graph TD syntax. Use {Text} for diamond decision nodes.
            Use |Label| on arrows for decision branches. Start and end nodes should be clearly marked.
            """.formatted(content);
    }
    
    private String getHierarchyPrompt(String content) {
        return """
            You are an expert at creating organizational hierarchies. Create a hierarchical structure diagram from the following study content using Mermaid.js syntax.
            
            STUDY CONTENT:
            %s
            
            INSTRUCTIONS:
            1. Identify the top-level category or main topic
            2. Find subcategories that belong under the main topic
            3. Add details or examples under each subcategory
            4. Create a clear parent-child structure (tree format)
            5. Keep it to 3-4 levels deep maximum
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "mermaidCode": "graph TD\\n    A[Main Topic] --> B[Category 1]\\n    A --> C[Category 2]\\n    A --> D[Category 3]\\n    B --> E[Sub-item 1.1]\\n    B --> F[Sub-item 1.2]\\n    C --> G[Sub-item 2.1]",
                "nodes": [
                    {"id": "A", "label": "Main Topic", "type": "concept"},
                    {"id": "B", "label": "Category 1", "type": "concept"},
                    {"id": "E", "label": "Sub-item 1.1", "type": "detail"}
                ],
                "edges": [
                    {"from": "A", "to": "B"},
                    {"from": "B", "to": "E"}
                ],
                "description": "Hierarchical structure showing organization"
            }
            
            IMPORTANT: Use valid Mermaid.js graph TD (top-down) syntax for hierarchical tree structure.
            Each node should only have ONE parent. Use clear indentation in the logical structure.
            """.formatted(content);
    }
    
    private String getMindMapPrompt(String content) {
        return """
            You are an expert at creating mind maps. Create a radial mind map from the following study content using Mermaid.js syntax.
            
            STUDY CONTENT:
            %s
            
            INSTRUCTIONS:
            1. Identify the central idea or main topic
            2. Find 4-6 major branches/themes that extend from the center
            3. Add 1-2 sub-branches or details for each major branch
            4. Use descriptive but concise labels
            5. Create a balanced structure radiating from the center
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "mermaidCode": "graph TD\\n    Center[Central Idea] --> A[Branch 1]\\n    Center --> B[Branch 2]\\n    Center --> C[Branch 3]\\n    Center --> D[Branch 4]\\n    A --> A1[Detail 1.1]\\n    A --> A2[Detail 1.2]\\n    B --> B1[Detail 2.1]\\n    C --> C1[Detail 3.1]",
                "nodes": [
                    {"id": "Center", "label": "Central Idea", "type": "concept"},
                    {"id": "A", "label": "Branch 1", "type": "concept"},
                    {"id": "A1", "label": "Detail 1.1", "type": "detail"}
                ],
                "edges": [
                    {"from": "Center", "to": "A"},
                    {"from": "A", "to": "A1"}
                ],
                "description": "Mind map with central topic and branching ideas"
            }
            
            IMPORTANT: Use valid Mermaid.js graph TD syntax. The central node should connect to all major branches.
            Each branch can have its own sub-nodes. Keep labels short for readability.
            """.formatted(content);
    }
    
    private String getSequencePrompt(String content) {
        return """
            You are an expert at creating sequence diagrams. Create a sequence diagram showing interactions from the following study content using Mermaid.js syntax.
            
            STUDY CONTENT:
            %s
            
            INSTRUCTIONS:
            1. Identify the participants/actors involved (people, systems, components)
            2. Find the sequence of interactions or communications between them
            3. Show the order of steps from top to bottom
            4. Label each interaction clearly
            5. Include 4-8 interactions for readability
            
            Respond with ONLY a valid JSON object (no markdown, no code blocks, just pure JSON):
            {
                "mermaidCode": "sequenceDiagram\\n    participant A as Actor 1\\n    participant B as Actor 2\\n    participant C as Actor 3\\n    A->>B: Step 1 action\\n    B->>C: Step 2 action\\n    C-->>B: Response\\n    B-->>A: Final result",
                "nodes": [
                    {"id": "A", "label": "Actor 1", "type": "concept"},
                    {"id": "B", "label": "Actor 2", "type": "concept"}
                ],
                "edges": [
                    {"from": "A", "to": "B", "label": "Step 1 action"}
                ],
                "description": "Sequence diagram showing step-by-step interactions"
            }
            
            IMPORTANT: Use valid Mermaid.js sequenceDiagram syntax.
            Use ->> for solid arrows and -->> for dashed response arrows.
            Define participants first, then show interactions in order.
            """.formatted(content);
    }
    
    private Map<String, Object> createDefaultDiagram(String diagramType) {
        String defaultCode = switch (diagramType) {
            case "timeline" -> "graph LR\\n    A[Event 1] --> B[Event 2]";
            case "flowchart" -> "graph TD\\n    A[Start] --> B{Decision}\\n    B -->|Yes| C[End]";
            case "hierarchy" -> "graph TD\\n    A[Main] --> B[Sub 1]\\n    A --> C[Sub 2]";
            case "mind-map" -> "graph TD\\n    A[Center] --> B[Branch 1]\\n    A --> C[Branch 2]";
            case "sequence" -> "sequenceDiagram\\n    A->>B: Action";
            default -> "graph TD\\n    A[Content] --> B[Analysis Pending]";
        };
        
        Map<String, Object> result = new HashMap<>();
        result.put("mermaidCode", defaultCode);
        result.put("nodes", List.of(
            Map.of("id", "A", "label", "Content", "type", "concept"),
            Map.of("id", "B", "label", "Analysis Pending", "type", "detail")
        ));
        result.put("edges", List.of(Map.of("from", "A", "to", "B")));
        result.put("description", "Diagram generation pending");
        return result;
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
        
        return callLlmForJson(prompt, createDefaultStudyPlan(examDate));
    }
    
    /**
     * Calls the Groq API for JSON response generation.
     * Uses OpenAI-compatible API format.
     */
    private Map<String, Object> callLlmForJson(String prompt, Map<String, Object> fallback) {
        try {
            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║  🚀 Groq API Call (Study Tools)                              ║");
            log.info("╚══════════════════════════════════════════════════════════════╝");
            
            // Truncate content if too large
            String truncatedPrompt = prompt;
            if (prompt.length() > 12000) {
                log.warn("⚠️  Prompt too long ({} chars), truncating to 12000", prompt.length());
                truncatedPrompt = prompt.substring(0, 12000) + "\n\n[Content truncated for processing...]";
            }
            
            log.info("🤖 Model: {}", llmModel);
            log.info("📝 Prompt size: {} characters", truncatedPrompt.length());
            log.info("🌐 API URL: {}", llmApiUrl);
            log.info("🔑 Using API key: {}", llmApiKey != null && !llmApiKey.isEmpty() ? "Yes" : "No");
            
            long startTime = System.currentTimeMillis();
            
            String url = llmApiUrl + "/chat/completions";
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "You are a helpful AI assistant that always responds with valid JSON. Never include markdown code blocks, just raw JSON.");
            messages.add(systemMsg);
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", truncatedPrompt);
            messages.add(userMsg);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", llmModel);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2048);
            
            // Groq supports JSON mode
            Map<String, String> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);
            
            log.info("⏳ Waiting for Groq response...");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (llmApiKey != null && !llmApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + llmApiKey);
            } else {
                log.warn("⚠️  No API key configured! Set LLM_API_KEY environment variable.");
            }
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            @SuppressWarnings("null")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("✅ Response received in {} ms", elapsed);
            
            Map<String, Object> responseBody = response.getBody();
            String jsonResponse = null;
            
            // Parse OpenAI-compatible response format (Groq uses this)
            if (responseBody != null && responseBody.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        jsonResponse = (String) message.get("content");
                    }
                }
            }
            
            if (jsonResponse != null) {
                log.info("📦 Response size: {} characters", jsonResponse.length());
                log.debug("Raw response: {}", jsonResponse.substring(0, Math.min(300, jsonResponse.length())));
                
                // Clean up response - remove markdown code blocks if present
                jsonResponse = jsonResponse.trim();
                if (jsonResponse.startsWith("```json")) {
                    jsonResponse = jsonResponse.substring(7);
                }
                if (jsonResponse.startsWith("```")) {
                    jsonResponse = jsonResponse.substring(3);
                }
                if (jsonResponse.endsWith("```")) {
                    jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
                }
                jsonResponse = jsonResponse.trim();
                
                try {
                    Map<String, Object> parsed = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
                    log.info("✅ JSON parsed successfully");
                    return parsed;
                } catch (Exception parseEx) {
                    log.error("❌ Failed to parse JSON response: {}", parseEx.getMessage());
                    log.error("📄 Raw response: {}", jsonResponse.substring(0, Math.min(500, jsonResponse.length())));
                    return fallback;
                }
            }
            
            log.warn("⚠️  Response did not contain expected content field");
            return fallback;
        } catch (Exception e) {
            log.error("❌ Error calling Groq API: {}", e.getMessage(), e);
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
        return createDefaultDiagram("concept-map");
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
