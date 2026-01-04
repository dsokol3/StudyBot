package com.chatbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * Initializes PostgreSQL extensions (pgvector) BEFORE Hibernate schema generation.
 * 
 * This creates the 'vector' type by running CREATE EXTENSION before Spring
 * creates the DataSource that Hibernate will use.
 */
@Configuration
public class PostgresInitializer {

    private static final Logger log = LoggerFactory.getLogger(PostgresInitializer.class);

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
        
        if (url == null || url.isEmpty()) {
            log.warn("No datasource URL configured, skipping pgvector initialization");
            return "skipped";
        }
        
        log.info("Connecting to database to create pgvector extension...");
        log.info("JDBC URL: {}", maskUrl(url));
        log.info("Username: {}", username);
        
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            log.info("Direct JDBC connection successful!");
            log.info("Database: {} {}", 
                    conn.getMetaData().getDatabaseProductName(),
                    conn.getMetaData().getDatabaseProductVersion());
            
            // Create pgvector extension
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
                log.info("SUCCESS: pgvector extension created/verified!");
            }
            
        } catch (Exception e) {
            log.error("FAILED to create pgvector extension: {}", e.getMessage());
            log.error("Vector similarity search will NOT work on this database.");
            log.error("Solutions:");
            log.error("  1. Use PostgreSQL with pgvector support (Supabase, Neon, Railway)");
            log.error("  2. Self-host PostgreSQL with pgvector extension installed");
            log.error("  3. Render's free PostgreSQL does NOT support pgvector");
        }
        
        log.info("=== End pgvector Initialization ===");
        return "initialized";
    }
    
    private String maskUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll("password=[^&]*", "password=***");
    }
}
