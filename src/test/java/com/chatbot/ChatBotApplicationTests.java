package com.chatbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
class ChatBotApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context loads successfully
        // Uses H2 in-memory database for testing
    }
}

