package com.chatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

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

    // Embedding stored as JSON string - supports both pgvector and fallback storage
    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embeddingJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Sets the embedding as a float array, stored as JSON.
     */
    public void setEmbedding(float[] embedding) {
        this.embeddingJson = arrayToJson(embedding);
    }

    /**
     * Gets the embedding as a float array from JSON storage.
     */
    public float[] getEmbedding() {
        return jsonToArray(this.embeddingJson);
    }

    private String arrayToJson(float[] array) {
        if (array == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] jsonToArray(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        String clean = json.trim();
        if (!clean.startsWith("[") || !clean.endsWith("]")) return null;

        String[] parts = clean.substring(1, clean.length() - 1).split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return result;
    }
}
