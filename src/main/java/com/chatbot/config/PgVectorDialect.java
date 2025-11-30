package com.chatbot.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;

import java.sql.Types;

/**
 * Custom PostgreSQL dialect that adds support for pgvector types.
 */
public class PgVectorDialect extends PostgreSQLDialect {
    
    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        
        // Vector type handling is done through entity annotations and converters
    }
    
    @Override
    protected String columnType(int sqlTypeCode) {
        if (sqlTypeCode == Types.ARRAY) {
            return "vector";
        }
        return super.columnType(sqlTypeCode);
    }
}
