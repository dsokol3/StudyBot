package com.chatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "persons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_person")
    private Integer idPerson;
    
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(name = "date_of_birth", length = 20)
    private String dateOfBirth;
    
    @Column(name = "email", length = 255, unique = true)
    private String email;
    
    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Conversation> conversations = new ArrayList<>();
    
    // Helper method to add a conversation
    public void addConversation(Conversation conversation) {
        conversations.add(conversation);
        conversation.setPerson(this);
    }
    
    // Helper method to remove a conversation
    public void removeConversation(Conversation conversation) {
        conversations.remove(conversation);
        conversation.setPerson(null);
    }
}
