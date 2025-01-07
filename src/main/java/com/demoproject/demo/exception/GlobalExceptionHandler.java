/*
 * =============================================================================
 * Global Exception Handler
 * =============================================================================
 * Purpose: Provides centralized exception handling for the entire application
 * Dependencies: Spring Framework (Web, Security), SLF4J Logger
 * Author: DemoProject Team
 * Last Updated: Current Version
 */

package com.demoproject.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import org.springframework.http.MediaType;

/**
 * Global exception handler that centralizes error handling across the application.
 * Implements Spring's @ControllerAdvice for cross-cutting exception management.
 * 
 * @note All handlers return standardized ErrorDetails for consistent client responses
 * @note Logging is implemented for critical errors to aid debugging
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ---------------------------- Client Error Handlers ---------------------------- */

    /**
     * Handles invalid input parameters or business rule violations.
     * 
     * @param ex      The IllegalArgumentException containing validation details
     * @param request The current web request context
     * @return ResponseEntity<ErrorDetails> with 400 BAD_REQUEST
     * @note Commonly triggered by invalid user input or parameter validation
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    /**
     * Manages security access violations.
     * 
     * @param ex      The security exception indicating unauthorized access
     * @param request The current web request context
     * @return ResponseEntity<ErrorDetails> with 403 FORBIDDEN
     * @note Triggered when authenticated users lack required permissions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "Access denied", request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles database constraint violations and data conflicts.
     * 
     * @param ex      The database constraint violation exception
     * @param request The current web request context
     * @return ResponseEntity<ErrorDetails> with 409 CONFLICT
     * @note Common cases: unique constraint violations, foreign key conflicts
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "Data integrity violation", request.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.CONFLICT);
    }

    /* ---------------------------- Server Error Handlers ---------------------------- */

    /**
     * Catches all unhandled exceptions as a last resort.
     * 
     * @param ex      The uncaught exception
     * @param request The current web request context
     * @return ResponseEntity<ErrorDetails> with 500 INTERNAL_SERVER_ERROR
     * @note Logs full stack trace for debugging while returning safe message to client
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("An unexpected error occurred", ex);

        ErrorDetails errorDetails = new ErrorDetails(
            LocalDateTime.now(),
            ex.getMessage(),
            request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errorDetails);
    }

    /**
     * Handles database-specific exceptions.
     * 
     * @param ex The SQL exception from database operations
     * @return ResponseEntity<String> with 500 INTERNAL_SERVER_ERROR
     * @note Logs detailed DB error while returning generic message to client
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<String> handleSQLException(SQLException ex) {
        logger.error("Database error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while processing your request. Please try again later.");
    }

    /* ---------------------------- Resource Handlers ---------------------------- */

    /**
     * Manages missing resource requests, including special handling for favicon.
     * 
     * @param ex The resource not found exception
     * @return ResponseEntity with appropriate status (200 OK for favicon, 404 otherwise)
     * @note Special case handling for favicon.ico to prevent unnecessary 404 logs
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFoundException(NoResourceFoundException ex) {
        if (ex.getMessage().contains("favicon.ico")) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /* 
     * @todo Future Enhancements:
     * - Add handlers for other common exceptions (MethodNotAllowed, UnsupportedMediaType)
     * - Implement admin notification system for critical errors
     * - Consider rate limiting for repeated error responses
     * - Add metrics collection for error monitoring
     */
}