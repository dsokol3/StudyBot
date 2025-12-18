package com.chatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Convert;
import com.chatbot.converter.FloatArrayToStringConverter;

@Entity
@Table(name = "document_chunks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Column(name = "chunk_order", nullable = false)
    private Integer chunkOrder;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "token_count", nullable = false)
    private Integer tokenCount;
    
    // Embedding stored as float array. Column definition is configurable
    // so we can use Postgres pgvector in production and a TEXT column for H2 in dev.
    @Column(name = "embedding", columnDefinition = "TEXT")
    @Convert(converter = FloatArrayToStringConverter.class)
    private float[] embedding;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
