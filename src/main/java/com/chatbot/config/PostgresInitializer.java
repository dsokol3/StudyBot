package com.chatbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Initializes PostgreSQL extensions (pgvector) BEFORE Hibernate schema generation.
 * 
 * This creates the 'vector' type by running CREATE EXTENSION before Spring
 * creates the DataSource that Hibernate will use.
 * 
 * If pgvector is not available (like on Render free tier), sets a flag to use
 * alternative storage for embeddings.
 */
@Configuration
public class PostgresInitializer {

    private static final Logger log = LoggerFactory.getLogger(PostgresInitializer.class);
    
    // Flag to indicate if pgvector is available
    private static boolean pgvectorAvailable = false;

    /**
     * Creates pgvector extension before the main DataSource is created.
     * This bean runs early and ensures 'vector' type exists.
     */
    @Bean
    public String pgvectorExtensionInitializer(DataSourceProperties properties) {
        log.info("=== PostgreSQL pgvector Extension Initialization ===");
        
        String url = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();
        
        if (url == null || username == null) {
            log.warn("Database URL or username not configured, skipping pgvector initialization");
            System.setProperty("pgvector.available", "false");
            pgvectorAvailable = false;
            log.info("pgvector available: false");
            log.info("=== End pgvector Initialization ===");
            return "unavailable";
        }
        
        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {
            
            log.info("Connected to database: {}", maskUrl(url));
            
            // Check if pgvector extension exists
            ResultSet rs = stmt.executeQuery(
                "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')");
            
            if (rs.next() && rs.getBoolean(1)) {
                log.info("pgvector extension already exists");
                pgvectorAvailable = true;
            } else {
                log.info("Creating pgvector extension...");
                stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
                log.info("pgvector extension created successfully");
                pgvectorAvailable = true;
            }
            
            System.setProperty("pgvector.available", String.valueOf(pgvectorAvailable));
            log.info("pgvector available: {}", pgvectorAvailable);
            
        } catch (Exception e) {
            log.warn("Failed to initialize pgvector extension: {}", e.getMessage());
            log.warn("Embeddings will use alternative storage (JSON)");
            pgvectorAvailable = false;
            System.setProperty("pgvector.available", "false");
        }
        
        log.info("=== End pgvector Initialization ===");
        return pgvectorAvailable ? "available" : "unavailable";
    }
    
    /**
     * Returns whether pgvector extension is available.
     * Used by other components to determine embedding storage strategy.
     */
    public static boolean isPgvectorAvailable() {
        return pgvectorAvailable;
    }
    
    private String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll("password=[^&]*", "password=***");
    }
}
