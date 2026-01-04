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
        
        if (url == null || url.isEmpty()) {
            log.warn("No datasource URL configured, skipping pgvector initialization");
            System.setProperty("pgvector.available", "false");
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
            
            // Check if pgvector extension already exists
            boolean extensionExists = false;
            try (Statement checkStmt = conn.createStatement();
                 ResultSet rs = checkStmt.executeQuery(
                     "SELECT 1 FROM pg_extension WHERE extname = 'vector'")) {
                extensionExists = rs.next();
            }
            
            if (extensionExists) {
                log.info("pgvector extension already exists!");
                pgvectorAvailable = true;
                System.setProperty("pgvector.available", "true");
            } else {
                // Try to create pgvector extension
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("CREATE EXTENSION vector");
                    log.info("SUCCESS: pgvector extension created!");
                    pgvectorAvailable = true;
                    System.setProperty("pgvector.available", "true");
                } catch (Exception createEx) {
                    log.warn("Could not create pgvector extension: {}", createEx.getMessage());
                    log.warn("This is normal on Render free tier PostgreSQL");
                    pgvectorAvailable = false;
                    System.setProperty("pgvector.available", "false");
                }
            }
            
        } catch (Exception e) {
            log.error("FAILED to connect to database for pgvector initialization: {}", e.getMessage());
            log.error("Vector similarity search will NOT work on this database.");
            pgvectorAvailable = false;
            System.setProperty("pgvector.available", "false");
        }
        
        log.info("pgvector available: {}", pgvectorAvailable);
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
