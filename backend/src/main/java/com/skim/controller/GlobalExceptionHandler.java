package com.skim.controller;

import com.skim.util.AiServiceException;
import com.skim.util.DailyLimitReachedException;
import com.skim.util.ServerBusyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central place for translating exceptions into clean, consistent JSON error
 * responses. Keeps stack traces and internal error detail out of client responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Triggered when @Valid bean validation fails (e.g. content too long/short, blank fields)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse("Invalid request");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(message));
    }

    // Triggered by manual validation inside SkimService (e.g. unknown operation/tone)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(ex.getMessage()));
    }

    // Triggered when the global daily quota (protects the shared Gemini free-tier limit) is hit
    @ExceptionHandler(DailyLimitReachedException.class)
    public ResponseEntity<Map<String, String>> handleDailyLimitReached(DailyLimitReachedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorBody(ex.getMessage()));
    }

    // Triggered when too many requests are in-flight to Gemini at once
    @ExceptionHandler(ServerBusyException.class)
    public ResponseEntity<Map<String, String>> handleServerBusy(ServerBusyException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody(ex.getMessage()));
    }

    // Triggered when the AI call itself fails
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, String>> handleAiServiceException(AiServiceException ex) {
        log.error("AI service error", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(errorBody("The AI service is currently unavailable. Please try again shortly."));
    }

    // Catch-all safety net so nothing leaks a raw stack trace to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Something went wrong. Please try again."));
    }

    private Map<String, String> errorBody(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return body;
    }
}