/* =============================================================================
 * User Productivity Query Data Transfer Object
 * =============================================================================
 * PURPOSE: Encapsulates user productivity metrics for database queries
 * USAGE: Used to transfer productivity data between repository and service layers
 * DEPENDENCIES: Lombok for boilerplate code generation
 * 
 * @note All numeric fields use wrapper types to handle null values
 * @note Thread-safe due to immutable design
 */

package com.demoproject.demo.pacmedproductivity;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/* -----------------------------------------------------------------------------
 * Core Data Structure
 * -----------------------------------------------------------------------------
 * @note Lombok annotations eliminate boilerplate code while maintaining clean API
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserProductivityQueryDTO {

    /* ------------------------------------------------------------------------
     * User Identification
     * -------------------------------------------------------------------- */
    
    /**
     * Unique identifier for the pharmacy staff member
     * @note Must not be null or empty
     */
    private String username;

    /* ------------------------------------------------------------------------
     * Productivity Metrics
     * -------------------------------------------------------------------- */
    
    /**
     * Total number of medication verification submissions
     * @note Nullable for new users
     * @note Must be >= 0 when present
     */
    private Long totalSubmissions;

    /**
     * Total count of medication pouches processed
     * @note Nullable for new users
     * @note Must be >= 0 when present
     */
    private Long totalPouchesChecked;

    /**
     * Total time spent on verification tasks in minutes
     * @note Nullable for new users
     * @note Must be >= 0 when present
     * @note Used for calculating hourly productivity rates
     */
    private Long totalMinutes;
}