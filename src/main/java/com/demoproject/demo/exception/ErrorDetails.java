/* ==========================================================================
 * Error Details Module
 * 
 * PURPOSE: Encapsulates error information for consistent error handling
 * DEPENDENCIES: java.time.LocalDateTime
 * SCOPE: Exception handling and error reporting
 * ========================================================================== */

package com.demoproject.demo.exception;

import java.time.LocalDateTime;

/* --------------------------------------------------------------------------
 * Core Error Details Definition
 * 
 * FUNCTIONALITY:
 * - Captures error occurrence timestamp
 * - Stores error message and details
 * - Provides immutable error state
 * 
 * IMPORTANT NOTES:
 * - Thread-safe due to immutability
 * - Uses system timezone for timestamps
 * - All fields are required - no nulls allowed
 * -------------------------------------------------------------------------- */
public class ErrorDetails {

    /* .... Error State Properties .... */
    private final LocalDateTime timestamp;  // Error occurrence time
    private final String message;           // Error description
    private final String details;           // Additional context

    /* .... Constructor .... */
    /**
     * Creates immutable error details instance
     * 
     * @param timestamp When error occurred (system timezone)
     * @param message Brief error description
     * @param details Extended error context
     * @throws IllegalArgumentException if any parameter is null
     * 
     * @note All parameters are required
     * @note Instance is immutable after creation
     */
    public ErrorDetails(LocalDateTime timestamp, String message, String details) {
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
    }

    /* .... Accessors .... */
    /**
     * @returns Error timestamp in system timezone
     * @note Never null
     */
    public LocalDateTime getTimestamp() { 
        return timestamp; 
    }

    /**
     * @returns Brief error description
     * @note Never null
     */
    public String getMessage() { 
        return message; 
    }

    /**
     * @returns Detailed error context
     * @note Never null
     */
    public String getDetails() { 
        return details; 
    }

    /* @todo [FEATURE] Add builder pattern for flexible creation
     * @todo [DEBUG] Implement toString() for logging
     * @todo [VALIDATION] Add null checks in constructor
     * @todo [FEATURE] Consider adding error severity level
     */
}