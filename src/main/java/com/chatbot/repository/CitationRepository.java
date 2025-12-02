package com.chatbot.repository;

import com.chatbot.entity.Citation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitationRepository extends JpaRepository<Citation, Long> {
    
    List<Citation> findByChatResponseId(Long chatResponseId);
    
    List<Citation> findByDocumentId(String documentId);
}
