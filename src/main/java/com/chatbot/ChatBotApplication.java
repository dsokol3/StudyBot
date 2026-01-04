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
//@EnableAsync
public class ChatBotApplication {
    
    public static void main(String[] args) {
        System.out.println("=== ChatBot Application Starting ===");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("PORT env: " + System.getenv("PORT"));
        
        // Parse DATABASE_URL if provided (Render format)
        parseDatabaseUrl();
        
        // Log final database configuration
        logDatabaseConfig();
        
        SpringApplication.run(ChatBotApplication.class, args);
    }

    /**
     * Parses Render's DATABASE_URL and sets individual Spring properties.
     * Format: postgresql://user:password@host:port/database
     */
    private static void parseDatabaseUrl() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        System.out.println("=== Database URL Parsing ===");
        System.out.println("DATABASE_URL env present: " + (databaseUrl != null && !databaseUrl.isEmpty()));
        
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            System.out.println("No DATABASE_URL found, checking individual DB_* env vars...");
            System.out.println("  DB_HOST: " + (System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "(not set, using default)"));
            System.out.println("  DB_PORT: " + (System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "(not set, using 5432)"));
            System.out.println("  DB_NAME: " + (System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "(not set, using default)"));
            System.out.println("  DB_USERNAME: " + (System.getenv("DB_USERNAME") != null ? "set" : "(not set)"));
            System.out.println("  DB_PASSWORD: " + (System.getenv("DB_PASSWORD") != null ? "set" : "(not set)"));
            return;
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

            // Build JDBC URL with SSL for Render, disable for localhost
            String sslMode = host.equals("localhost") ? "disable" : "require";
            String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/%s?sslmode=%s",
                host, port, database, sslMode
            );

            // Set as system properties (Spring Boot picks these up)
            System.setProperty("spring.datasource.url", jdbcUrl);
            if (username != null) {
                System.setProperty("spring.datasource.username", username);
            }
            if (password != null) {
                System.setProperty("spring.datasource.password", password);
            }

            System.out.println("Successfully parsed DATABASE_URL:");
            System.out.println("  Host: " + host);
            System.out.println("  Port: " + port);
            System.out.println("  Database: " + database);
            System.out.println("  Username: " + username);
            System.out.println("  Password: " + (password != null ? "[SET]" : "[NOT SET]"));
            System.out.println("  JDBC URL: " + jdbcUrl);
            
        } catch (Exception e) {
            System.err.println("ERROR parsing DATABASE_URL: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=== End Database URL Parsing ===");
    }
    
    /**
     * Logs the final database configuration that will be used.
     */
    private static void logDatabaseConfig() {
        System.out.println("=== Final Database Configuration ===");
        String url = System.getProperty("spring.datasource.url");
        String user = System.getProperty("spring.datasource.username");
        System.out.println("spring.datasource.url: " + (url != null ? url : "(will use application.properties default)"));
        System.out.println("spring.datasource.username: " + (user != null ? user : "(will use application.properties default)"));
        System.out.println("=== End Final Database Configuration ===");
    }
}
