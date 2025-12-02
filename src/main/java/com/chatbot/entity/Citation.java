package com.chatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "citations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Citation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "source", length = 500)
    private String source;
    
    @Column(name = "url", length = 1000)
    private String url;
    
    @Column(name = "document_id", length = 100)
    private String documentId;
    
    @Column(name = "chunk_index")
    private Integer chunkIndex;
    
    @Column(name = "relevance_score")
    private Double relevanceScore;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_response_id")
    @ToString.Exclude
    private ChatResponseEntity chatResponse;
    
    // Constructor for quick creation from RetrievalService citation
    public Citation(String source, String url, String documentId) {
        this.source = source;
        this.url = url;
        this.documentId = documentId;
    }
}
