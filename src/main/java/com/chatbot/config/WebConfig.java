package com.chatbot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

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
    
    /**
     * Configure resource handlers for Vue.js SPA.
     * This ensures that all non-API routes are handled by index.html (Vue Router).
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // If the requested resource exists and is readable, serve it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // For API, health, and actuator endpoints, don't serve index.html
                        if (resourcePath.startsWith("api/") || 
                            resourcePath.startsWith("actuator/") ||
                            resourcePath.equals("health")) {
                            return null;
                        }
                        
                        // For everything else (Vue Router routes), serve index.html
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
