package com.chatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "sender", length = 20)
    private String sender = "assistant";
    
    @Column(name = "timestamp", nullable = false)
    private Long timestamp;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @ToString.Exclude
    private Conversation conversation;
    
    @OneToMany(mappedBy = "chatResponse", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Citation> citations = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
    }
    
    // Helper method to add citation
    public void addCitation(Citation citation) {
        citations.add(citation);
        citation.setChatResponse(this);
    }
    
    // Helper method to remove citation
    public void removeCitation(Citation citation) {
        citations.remove(citation);
        citation.setChatResponse(null);
    }
}
