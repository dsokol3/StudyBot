package com.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.net.URI;
import java.net.URISyntaxException;

@SpringBootApplication
@EnableAsync
public class ChatBotApplication {
    public static void main(String[] args) {
        // Convert Render's DATABASE_URL format to JDBC format
        convertDatabaseUrl();
        SpringApplication.run(ChatBotApplication.class, args);
    }

    /**
     * Converts Render's DATABASE_URL (postgres://user:pass@host:port/db)
     * to Spring's expected JDBC format and sets individual properties.
     */
    private static void convertDatabaseUrl() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            return;
        }

        // If already in JDBC format, no conversion needed
        if (databaseUrl.startsWith("jdbc:")) {
            System.setProperty("spring.datasource.url", databaseUrl);
            return;
        }

        try {
            // Parse postgres://user:password@host:port/database
            URI uri = new URI(databaseUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String database = uri.getPath();
            if (database != null && database.startsWith("/")) {
                database = database.substring(1);
            }

            // Extract credentials
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                System.setProperty("spring.datasource.username", parts[0]);
                System.setProperty("spring.datasource.password", parts[1]);
            }

            // Build JDBC URL with SSL mode for Render
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?sslmode=require",
                    host, port, database);
            System.setProperty("spring.datasource.url", jdbcUrl);

            System.out.println("Converted DATABASE_URL to JDBC format: jdbc:postgresql://" 
                    + host + ":" + port + "/" + database);
        } catch (URISyntaxException e) {
            System.err.println("Failed to parse DATABASE_URL: " + e.getMessage());
        }
    }
}
