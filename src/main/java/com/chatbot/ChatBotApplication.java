package com.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.net.URI;

/**
 * Main Spring Boot application class.
 * 
 * Supports two configuration methods:
 * 
 * 1. DATABASE_URL (Render format): 
 *    postgresql://user:pass@host/database
 *    - Set this single env var and the app parses it automatically
 * 
 * 2. Individual env vars:
 *    - DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD, DB_SSLMODE
 */
@SpringBootApplication
@EnableAsync
public class ChatBotApplication {
    
    public static void main(String[] args) {
        // Parse DATABASE_URL if provided (Render format)
        parseDatabaseUrl();
        SpringApplication.run(ChatBotApplication.class, args);
    }

    /**
     * Parses Render's DATABASE_URL and sets individual Spring properties.
     * Format: postgresql://user:password@host:port/database
     */
    private static void parseDatabaseUrl() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            return; // Use individual DB_* env vars instead
        }

        try {
            // Handle postgresql:// or postgres:// prefix
            String uriString = databaseUrl;
            if (uriString.startsWith("postgresql://")) {
                uriString = "postgres" + uriString.substring(10); // normalize to postgres://
            }
            
            URI uri = new URI(uriString);
            
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String database = uri.getPath();
            if (database != null && database.startsWith("/")) {
                database = database.substring(1);
            }

            // Extract credentials from userInfo (user:password)
            String userInfo = uri.getUserInfo();
            String username = null;
            String password = null;
            if (userInfo != null) {
                int colonIdx = userInfo.indexOf(':');
                if (colonIdx > 0) {
                    username = userInfo.substring(0, colonIdx);
                    password = userInfo.substring(colonIdx + 1);
                } else {
                    username = userInfo;
                }
            }

            // Build JDBC URL with SSL for Render
            String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/%s?sslmode=require",
                host, port, database
            );

            // Set as system properties (Spring Boot picks these up)
            System.setProperty("spring.datasource.url", jdbcUrl);
            if (username != null) {
                System.setProperty("spring.datasource.username", username);
            }
            if (password != null) {
                System.setProperty("spring.datasource.password", password);
            }

            System.out.println("Parsed DATABASE_URL:");
            System.out.println("  JDBC URL: jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=require");
            System.out.println("  Username: " + username);
            
        } catch (Exception e) {
            System.err.println("Failed to parse DATABASE_URL: " + e.getMessage());
        }
    }
}
