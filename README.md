# 🤖 Intelligent ChatBot with RAG & Study Tools

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.5-4FC08D.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A production-ready full-stack application featuring **Retrieval-Augmented Generation (RAG)** for context-aware conversational AI, plus seven AI-powered study tools for automated learning material generation. Built with Spring Boot, Vue 3, and PostgreSQL.

## 🎯 Overview

This project demonstrates advanced AI integration and modern full-stack development. It combines conversational AI with intelligent document analysis to provide contextual responses and automated study material generation from uploaded documents.

**Key Technical Achievements:**
- Multi-layered Spring Boot architecture with async processing
- RAG implementation with vector embeddings and semantic search  
- Vue 3 Composition API with TypeScript strict mode
- Real-time document processing pipeline with chunking and deduplication
- RESTful API design with comprehensive error handling

---

## ✨ Core Features

### 🤖 Conversational AI with RAG
- Context-aware responses using document knowledge base with vector similarity search
- Multi-turn conversation management with citation tracking
- Streaming responses for real-time interaction

### 📚 AI Study Tools (7 Generators)
- **Summaries**: Multi-paragraph summaries with key points
- **Flashcards**: Q&A cards with difficulty ratings
- **Practice Questions**: Multiple-choice with explanations  
- **Essay Prompts**: Thought-provoking topics with grading criteria
- **Concept Diagrams**: Mermaid.js visualizations
- **Study Plans**: Personalized schedules based on exam dates
- **Text Explanations**: Simplification with examples

### 📄 Document Processing
- Multi-format support (PDF, DOCX, TXT, Markdown)
- Asynchronous upload with progress tracking
- Intelligent chunking for optimal retrieval
- SHA-256 hash-based duplicate detection

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Vue 3 + TypeScript                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Chat View   │  │  Study Tools │  │   Document   │      │
│  │              │  │   Dashboard  │  │   Manager    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                  │                  │              │
│         └──────────────────┴──────────────────┘              │
│                            │                                 │
│                     API Service Layer                        │
└────────────────────────────┬────────────────────────────────┘
                             │ REST API
┌────────────────────────────┴────────────────────────────────┐
│              Spring Boot REST Controllers                    │
│  ┌──────────┐  ┌────────────┐  ┌──────────┐  ┌──────────┐ │
│  │   Chat   │  │   Study    │  │Document  │  │   Home   │ │
│  │Controller│  │ Controller │  │Controller│  │Controller│ │
│  └─────┬────┘  └──────┬─────┘  └────┬─────┘  └──────────┘ │
├────────┴───────────────┴─────────────┴────────────────────  │
│                    Service Layer                             │
│  ┌────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐ │
│  │  Chat  │  │  Study   │  │ Document │  │  Retrieval   │ │
│  │Service │  │ Service  │  │  Service │  │   Service    │ │
│  └────┬───┘  └────┬─────┘  └─────┬────┘  └───────┬──────┘ │
│       │           │               │                │         │
│  ┌────┴───────────┴───────────────┴────────────────┴──────┐ │
│  │        Repository Layer (Spring Data JPA)              │ │
│  └────────────────────────────┬────────────────────────────┘ │
└───────────────────────────────┼──────────────────────────────┘
                                │
┌───────────────────────────────┴──────────────────────────────┐
│                    PostgreSQL Database                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │Conversations │  │   Documents  │  │Document Chunks│       │
│  │   Messages   │  │   Citations  │  │   Embeddings │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└──────────────────────────────────────────────────────────────┘
                                │
                         External APIs
                    ┌────────────┴────────────┐
                    │                         │
            ┌───────┴───────┐         ┌──────┴──────┐
            │  Ollama LLM   │         │  Embedding  │
            │     API       │         │   Service   │
            └───────────────┘         └─────────────┘
```

---

## 🛠️ Technology Stack

**Backend:** Java 17 • Spring Boot 3.5 • PostgreSQL 16 • Hibernate • Maven • Apache POI • PDFBox  
**Frontend:** Vue 3 • TypeScript 5.6 • Vite 6 • Pinia • TailwindCSS • shadcn-vue  
**AI/ML:** Ollama (llama3.2:1b) • Vector Embeddings • RAG Architecture

---

## 🚀 Quick Start

### Prerequisites
- Java 17+, Node.js 18+, PostgreSQL 16+, Maven 3.6+, Ollama

### Setup

1. **Start Database**
```bash
docker-compose up -d postgres
```

2. **Configure Application**
spring.datasource.url=jdbc:postgresql://localhost:5432/chatbot_db
ollama.api.url=http://localhost:11434
ollama.model=llama3.2:1b
```

3. **Install Ollama & Model**
```bash
ollama pull llama3.2:1b
```

4. **Run Backend**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

5. **Run Frontend**
```bash
cd frontend
npm install
npm run dev
```

6. **Access Application**
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api

---

## 📡 API Examples

### Send Chat Message
```http
POST /api/chat/message
Content-Type: application/json

{
  "message": "Explain machine learning",
  "conversationId": "user-123"
}
```

### Upload Document
```http
POST /api/documents/upload
Content-Type: multipart/form-data

file: <binary>
conversationId: "user-123"
```

### Generate Study Materials
```http
POST /api/study/generate/summary
Content-Type: application/json

{
  "content": "Text to summarize..."
}
```

---

## 🚢 Deployment

### Production Build
```bash
# Backend
mvn clean package
java -jar target/chatbot-1.0-SNAPSHOT.jar

# Frontend
cd frontend
npm run build
# Serve dist/ with nginx
```

### Docker
```bash
docker-compose up -d
```

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Devora Sokol**  
GitHub: [@dsokol3](https://github.com/dsokol3) • LinkedIn: [devorasokol](https://linkedin.com/in/devorasokol)
