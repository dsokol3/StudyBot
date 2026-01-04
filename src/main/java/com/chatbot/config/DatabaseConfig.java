package com.chatbot.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Database configuration that provides resilient connection handling.
 * Handles Render's DATABASE_URL format (postgres://user:pass@host:port/db)
 * and converts it to JDBC format if needed.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:chatbot}")
    private String dbName;

    @Value("${DB_USERNAME:}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        // Handle Render's postgres:// URL format
        configureFromDatabaseUrl(properties);
        
        log.info("Configuring datasource - Host: {}, Port: {}, Database: {}, User: {}",
                properties.getUrl() != null ? extractHost(properties.getUrl()) : dbHost,
                dbPort, dbName, 
                properties.getUsername() != null ? properties.getUsername() : dbUsername);
        
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        
        // Configure for resilience - don't fail startup if DB is unavailable
        dataSource.setInitializationFailTimeout(0); // Don't fail on init
        dataSource.setConnectionTimeout(30000); // 30 seconds to get a connection
        dataSource.setValidationTimeout(5000); // 5 seconds for validation
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(0); // Allow pool to be empty
        dataSource.setIdleTimeout(300000); // 5 minutes idle timeout
        dataSource.setMaxLifetime(600000); // 10 minutes max lifetime
        dataSource.setLeakDetectionThreshold(60000); // 1 minute leak detection
        dataSource.setConnectionTestQuery("SELECT 1");
        
        log.info("HikariCP configured for resilient database connections");
        return dataSource;
    }

    /**
     * Convert Render's DATABASE_URL format (postgres://user:pass@host:port/db)
     * to JDBC format (jdbc:postgresql://host:port/db) and set credentials
     */
    private void configureFromDatabaseUrl(DataSourceProperties properties) {
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            // Use individual env vars
            String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);
            properties.setUrl(jdbcUrl);
            if (dbUsername != null && !dbUsername.isEmpty()) {
                properties.setUsername(dbUsername);
            }
            if (dbPassword != null && !dbPassword.isEmpty()) {
                properties.setPassword(dbPassword);
            }
            log.info("Using individual env vars for database connection");
            return;
        }

        // Check if it's already a JDBC URL
        if (databaseUrl.startsWith("jdbc:")) {
            log.info("DATABASE_URL is already in JDBC format");
            properties.setUrl(databaseUrl);
            return;
        }

        // Convert postgres:// or postgresql:// to jdbc:postgresql://
        try {
            URI uri = new URI(databaseUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            String database = path != null && path.startsWith("/") ? path.substring(1) : path;
            
            // Extract user info
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                properties.setUsername(parts[0]);
                properties.setPassword(parts[1]);
            }
            
            // Build JDBC URL with SSL for Render
            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?sslmode=require", 
                    host, port, database);
            properties.setUrl(jdbcUrl);
            
            log.info("Converted DATABASE_URL to JDBC format: jdbc:postgresql://{}:{}/{}", 
                    host, port, database);
        } catch (URISyntaxException e) {
            log.error("Failed to parse DATABASE_URL: {}", e.getMessage());
            // Fall back to using the URL as-is
            properties.setUrl(databaseUrl.replace("postgres://", "jdbc:postgresql://")
                    .replace("postgresql://", "jdbc:postgresql://"));
        }
    }

    private String extractHost(String url) {
        if (url == null) return "unknown";
        try {
            // Remove jdbc: prefix and parse
            String cleanUrl = url.replace("jdbc:", "");
            URI uri = new URI(cleanUrl);
            return uri.getHost();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
