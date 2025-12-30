package com.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for extracting markdown from documents using the 'uv' tool.
 * Falls back to Tika extraction if 'uv' is not available or fails.
 */
@Service
public class UvMarkdownExtractor {
    
    private static final Logger log = LoggerFactory.getLogger(UvMarkdownExtractor.class);
    
    @Value("${uv.command.path:markitdown}")
    private String uvCommandPath;
    
    @Value("${uv.command.timeout:60}")
    private long uvCommandTimeout;
    
    @Value("${uv.enabled:true}")
    private boolean uvEnabled;
    
    @Value("${uv.use.tool.run:false}")
    private boolean useUvToolRun;
    
    private Boolean uvAvailable = null;
    
    // Default values for when Spring doesn't inject (e.g., in tests)
    public UvMarkdownExtractor() {
        // Defaults will be set by Spring or can be set manually
    }
    
    // Package-private setters for testing
    void setUvCommandPath(String path) {
        this.uvCommandPath = path;
    }
    
    void setUvCommandTimeout(long timeout) {
        this.uvCommandTimeout = timeout;
    }
    
    void setUvEnabled(boolean enabled) {
        this.uvEnabled = enabled;
    }
    
    void setUseUvToolRun(boolean use) {
        this.useUvToolRun = use;
    }
    
    /**
     * Extract markdown content from a file using 'uv' tool.
     * 
     * @param filePath Path to the file to extract markdown from
     * @return Extracted markdown content
     * @throws MarkdownExtractionException if extraction fails and no fallback is available
     */
    public String extractMarkdown(Path filePath) throws MarkdownExtractionException {
        // Initialize defaults if not set by Spring
        if (uvCommandPath == null) {
            uvCommandPath = "markitdown";
        }
        if (uvCommandTimeout == 0) {
            uvCommandTimeout = 60;
        }
        
        if (!uvEnabled) {
            log.debug("Markdown extraction disabled, will use fallback");
            throw new MarkdownExtractionException("Markdown extraction is disabled", null);
        }
        
        // Check if uv is available (cached check)
        if (!isUvAvailable()) {
            log.warn("'uv' command is not available. Please install it or ensure it's in PATH.");
            throw new MarkdownExtractionException("'uv' command not found", null);
        }
        
        try {
            return runUvCommand(filePath);
        } catch (IOException | InterruptedException e) {
            log.error("Failed to extract markdown using 'uv' for file {}: {}", filePath, e.getMessage());
            throw new MarkdownExtractionException("UV extraction failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if 'uv' command is available in the system.
     * Result is cached after first check.
     * 
     * @return true if 'uv' is available, false otherwise
     */
    public boolean isUvAvailable() {
        // Initialize defaults if not set by Spring
        if (uvCommandPath == null) {
            uvCommandPath = "markitdown";
        }
        
        if (uvAvailable != null) {
            return uvAvailable;
        }
        
        try {
            List<String> command = buildCommand("--version");
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                uvAvailable = false;
                return false;
            }
            
            uvAvailable = process.exitValue() == 0;
            log.info("UV availability check: {}", uvAvailable);
            return uvAvailable;
            
        } catch (IOException | InterruptedException e) {
            log.debug("UV not available: {}", e.getMessage());
            uvAvailable = false;
            return false;
        }
    }
    
    /**
     * Run the 'uv' command to extract markdown from a file.
     * 
     * @param filePath Path to the file
     * @return Extracted markdown content
     * @throws IOException if file operations fail
     * @throws InterruptedException if process is interrupted
     * @throws MarkdownExtractionException if extraction fails
     */
    private String runUvCommand(Path filePath) throws IOException, InterruptedException, MarkdownExtractionException {
        // Initialize defaults if not set by Spring
        if (uvCommandPath == null) {
            uvCommandPath = "markitdown";
        }
        if (uvCommandTimeout == 0) {
            uvCommandTimeout = 60;
        }
        
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }
        
        // Build command: markitdown <file-path>
        List<String> command = buildCommand(filePath.toString());
        
        log.info("Executing UV command: {}", String.join(" ", command));
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);
        
        Process process = processBuilder.start();
        
        // Read output streams
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        // Read stdout
        try (BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        // Read stderr
        try (BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = stderrReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }
        
        // Wait for process to complete with timeout
        boolean completed = process.waitFor(uvCommandTimeout, TimeUnit.SECONDS);
        
        if (!completed) {
            process.destroyForcibly();
            throw new MarkdownExtractionException(
                "UV command timed out after " + uvCommandTimeout + " seconds", null);
        }
        
        int exitCode = process.exitValue();
        
        if (exitCode != 0) {
            String errorMsg = errorOutput.length() > 0 ? errorOutput.toString() : "Unknown error";
            log.error("UV command failed with exit code {}: {}", exitCode, errorMsg);
            throw new MarkdownExtractionException(
                "UV command failed with exit code " + exitCode + ": " + errorMsg, null);
        }
        
        String markdownContent = output.toString().trim();
        
        if (markdownContent.isEmpty()) {
            throw new MarkdownExtractionException("UV command produced no output", null);
        }
        
        log.info("Successfully extracted {} characters of markdown using UV", markdownContent.length());
        return markdownContent;
    }
    
    /**
     * Reset the cached availability status (useful for testing).
     */
    public void resetAvailabilityCache() {
        uvAvailable = null;
    }
    
    /**
     * Build command list with optional 'uv tool run' prefix.
     */
    private List<String> buildCommand(String... args) {
        List<String> command = new ArrayList<>();
        
        if (useUvToolRun) {
            command.add("uv");
            command.add("tool");
            command.add("run");
        }
        
        command.add(uvCommandPath);
        command.addAll(Arrays.asList(args));
        
        return command;
    }
    
    public static class MarkdownExtractionException extends Exception {
        public MarkdownExtractionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
