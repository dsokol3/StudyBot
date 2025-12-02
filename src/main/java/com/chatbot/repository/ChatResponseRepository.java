package com.chatbot.repository;

import com.chatbot.entity.ChatResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatResponseRepository extends JpaRepository<ChatResponseEntity, Long> {
    
    List<ChatResponseEntity> findByConversationConversationIdOrderByCreatedAtAsc(String conversationId);
    
    List<ChatResponseEntity> findByConversationConversationId(String conversationId);
}
