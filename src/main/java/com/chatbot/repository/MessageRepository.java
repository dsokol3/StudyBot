package com.chatbot.repository;

import com.chatbot.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByConversationConversationIdOrderByCreatedAtAsc(String conversationId);
}
