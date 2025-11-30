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
     */
    public boolean isSupportedType(String contentType) {
        return contentType != null && (
            contentType.equals("application/pdf") ||
            contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
            contentType.equals("application/msword") ||
            contentType.equals("text/plain") ||
            contentType.equals("text/markdown") ||
            contentType.equals("text/html") ||
            contentType.equals("application/rtf")
        );
    }
    
    public static class DocumentParseException extends Exception {
        public DocumentParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
