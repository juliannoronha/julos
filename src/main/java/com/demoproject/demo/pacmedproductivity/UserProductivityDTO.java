package com.demoproject.demo.pacmedproductivity;

import lombok.Value;
import java.util.Map;

/* ==========================================================================
 * User Productivity Data Transfer Object
 * 
 * PURPOSE: Encapsulates productivity metrics for pharmacy staff members
 * USAGE: Used to transfer productivity data between service layers
 * 
 * @note This is an immutable value object using Lombok @Value
 * @note All numeric fields use primitive types to optimize performance
 * ========================================================================== */
@Value
public class UserProductivityDTO {
    
    /* ------------------------------------------------------------------------
     * Core User Identification
     * -------------------------------------------------------------------- */
    
    /**
     * Unique identifier for the pharmacy staff member
     * @note Must not be null or empty
     */
    String username;

    /* ------------------------------------------------------------------------
     * Productivity Metrics
     * -------------------------------------------------------------------- */
    
    /**
     * Total number of medication verification submissions
     * @note Represents completed verification actions
     * @note Must be >= 0
     */
    long totalSubmissions;

    /**
     * Total count of medication pouches processed
     * @note Each pouch may contain multiple medications
     * @note Must be >= 0
     */
    long totalPouchesChecked;

    /* ------------------------------------------------------------------------
     * Performance Indicators
     * -------------------------------------------------------------------- */
    
    /**
     * Average processing duration per medication pouch
     * @note Measured in seconds
     * @note Must be > 0
     */
    double avgTimePerPouch;

    /**
     * Hourly productivity rate
     * @note Calculated as: (totalPouchesChecked / total hours worked)
     * @note Must be >= 0
     */
    double avgPouchesPerHour;

    /* ------------------------------------------------------------------------
     * Visualization Data
     * -------------------------------------------------------------------- */
    
    /**
     * Structured data for generating productivity charts
     * @note Format depends on visualization requirements
     * @note May contain time series, aggregates, or comparison data
     */
    Map<String, Object> chartData;
}
