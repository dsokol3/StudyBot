package com.chatbot.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

/**
 * Database configuration with resilient connection handling.
 * 
 * Uses environment variables for configuration:
 * - DB_HOST: PostgreSQL hostname
 * - DB_PORT: PostgreSQL port (default: 5432)
 * - DB_NAME: Database name
 * - DB_USERNAME: Database username  
 * - DB_PASSWORD: Database password
 * - DB_SSLMODE: SSL mode (default: prefer, use 'require' for Render)
 * 
 * Key resilience features:
 * - initializationFailTimeout=0: App starts even if DB is unavailable
 * - minimumIdle=0: Pool can be empty when DB is down
 * - Automatic retry when connections are needed
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    /**
     * Creates HikariCP DataSource AFTER pgvector extension is initialized.
     * The @DependsOn ensures pgvector extension exists before Hibernate runs.
     */
    @Bean
    @Primary
    @DependsOn("pgvectorExtensionInitializer")
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        log.info("=== Creating HikariCP DataSource ===");
        log.info("JDBC URL: {}", maskJdbcUrl(properties.getUrl()));
        log.info("Username: {}", properties.getUsername());
        
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        
        // RESILIENCE: Don't fail startup if DB is unavailable
        dataSource.setInitializationFailTimeout(0);
        
        // Connection pool settings optimized for Render free tier
        dataSource.setConnectionTimeout(30000);      // 30s to get a connection
        dataSource.setValidationTimeout(5000);       // 5s for validation
        dataSource.setMaximumPoolSize(5);            // Max 5 connections (free tier limit)
        dataSource.setMinimumIdle(0);                // Allow empty pool when DB is down
        dataSource.setIdleTimeout(300000);           // 5 min idle timeout
        dataSource.setMaxLifetime(600000);           // 10 min max lifetime
        dataSource.setConnectionTestQuery("SELECT 1");
        
        log.info("HikariCP configured - app will start even if DB is temporarily unavailable");
        log.info("=== End HikariCP DataSource Creation ===");
        return dataSource;
    }

    /**
     * Masks password in JDBC URL for safe logging.
     */
    private String maskJdbcUrl(String url) {
        if (url == null) return "null";
        // Hide password if present in URL
        return url.replaceAll("password=[^&]*", "password=***")
                  .replaceAll(":[^:@]+@", ":***@");
    }
}
