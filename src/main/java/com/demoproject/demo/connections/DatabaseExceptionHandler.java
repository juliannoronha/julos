/* ==========================================================================
 * Database Exception Handler Module
 *
 * PURPOSE: Centralized handling of database-related exceptions
 * DEPENDENCIES: Spring Framework, Hibernate, Java Logging API
 * SCOPE: Global exception handling for database operations
 * 
 * SECURITY CONSIDERATIONS:
 * - Sanitizes error messages before client exposure
 * - Logs detailed errors for debugging
 * - Maintains consistent error responses
 * ========================================================================== */

package com.demoproject.demo.connections;

import java.util.logging.Logger;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/* --------------------------------------------------------------------------
 * Global Database Exception Handler
 * 
 * FUNCTIONALITY:
 * - Intercepts database connection exceptions
 * - Provides consistent error responses
 * - Logs detailed error information
 * 
 * ERROR HANDLING:
 * - Returns 500 Internal Server Error
 * - Generic client message for security
 * - Detailed server-side logging
 * -------------------------------------------------------------------------- */
@ControllerAdvice
public class DatabaseExceptionHandler {
    
    private static final Logger logger = Logger.getLogger(DatabaseExceptionHandler.class.getName());
    
    /**
     * Handles database connection exceptions globally.
     *
     * @param ex The caught JDBC connection exception
     * @returns ResponseEntity with error status and message
     * 
     * @note Logs full exception details but returns sanitized message
     * @note Always returns 500 status to mask internal errors
     * 
     * SECURITY:
     * - Prevents exposure of internal error details
     * - Maintains consistent error format
     */
    @ExceptionHandler(JDBCConnectionException.class)
    public ResponseEntity<String> handleConnectionException(JDBCConnectionException ex) {
        logger.severe("Database connection error: " + ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Database connection error occurred. Please try again later.");
    }
}
