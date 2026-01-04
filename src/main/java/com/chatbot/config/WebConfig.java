package com.chatbot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Configure CORS settings - allow cloud deployments and local development
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                    "http://localhost:5173",
                    "http://localhost:3000",
                    "http://localhost:*",
                    "https://*.onrender.com",
                    "https://*.render.com",
                    "https://*.vercel.app",
                    "https://*.netlify.app"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
        
        // Also allow CORS for actuator health endpoints
        registry.addMapping("/actuator/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET")
                .allowedHeaders("*");
    }
}
