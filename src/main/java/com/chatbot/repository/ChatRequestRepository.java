package com.chatbot.repository;

import com.chatbot.entity.ChatRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRequestRepository extends JpaRepository<ChatRequestEntity, Long> {
    
    List<ChatRequestEntity> findByConversationConversationIdOrderByCreatedAtAsc(String conversationId);
    
    List<ChatRequestEntity> findByConversationConversationId(String conversationId);
}
