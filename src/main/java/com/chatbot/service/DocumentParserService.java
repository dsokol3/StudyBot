package com.chatbot.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Service for extracting text content from various document formats.
 * Uses Apache Tika to support PDF, DOCX, TXT, MD, and more.
 */
@Service
public class DocumentParserService {
    
    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);
    
    private final Tika tika;
    private final Parser parser;
    
    // Maximum characters to extract (10MB of text)
    private static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;
    
    public DocumentParserService() {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
    }
    
    /**
     * Extract text content from a document.
     * 
     * @param inputStream The document input stream
     * @param filename Original filename for type detection
     * @return Extracted text content
     * @throws DocumentParseException if parsing fails
     */
    public String extractText(InputStream inputStream, String filename) throws DocumentParseException {
        try {
            // Use BodyContentHandler with limit to prevent memory issues
            BodyContentHandler handler = new BodyContentHandler(MAX_CONTENT_LENGTH);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);

            
            ParseContext context = new ParseContext();
            context.set(Parser.class, parser);
            
            parser.parse(inputStream, handler, metadata, context);
            
            String content = handler.toString();
            log.info("Extracted {} characters from {}", content.length(), filename);
            
            return content.trim();
            
        } catch (IOException e) {
            throw new DocumentParseException("Failed to read document: " + filename, e);
        } catch (SAXException e) {
            throw new DocumentParseException("Failed to parse document content: " + filename, e);
        } catch (TikaException e) {
            throw new DocumentParseException("Tika parsing error for: " + filename, e);
        }
    }
    
    /**
     * Detect the MIME type of a document.
     */
    public String detectMimeType(InputStream inputStream, String filename) throws IOException {
        return tika.detect(inputStream, filename);
    }
    
    /**
     * Check if a file type is supported for text extraction.
     * Uses startsWith for text types to handle charset variations like 'text/plain; charset=utf-8'
     */
    public boolean isSupportedType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        // Normalize content type (remove charset and extra parameters)
        String normalizedType = contentType.split(";")[0].trim().toLowerCase();
        
        return normalizedType.equals("application/pdf") ||
               normalizedType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
               normalizedType.equals("application/msword") ||
               normalizedType.equals("text/plain") ||
               normalizedType.equals("text/markdown") ||
               normalizedType.equals("text/x-markdown") ||
               normalizedType.equals("text/html") ||
               normalizedType.equals("application/rtf") ||
               normalizedType.equals("application/octet-stream"); // Fallback for unknown types - let Tika detect
    }
    
    public static class DocumentParseException extends Exception {
        public DocumentParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
