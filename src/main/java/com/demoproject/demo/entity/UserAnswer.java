/* ==========================================================================
 * UserAnswer Entity Module
 * 
 * PURPOSE: Represents user responses/answers in the system
 * DEPENDENCIES: JPA, Lombok, User Entity
 * SCOPE: Core domain entity for answer tracking
 * ========================================================================== */

package com.demoproject.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/* --------------------------------------------------------------------------
 * Core UserAnswer Entity Definition
 * 
 * FUNCTIONALITY:
 * - Stores user answer submissions
 * - Links answers to users
 * - Tracks submission timing
 * 
 * IMPORTANT NOTES:
 * - Uses lazy loading for user relationship
 * - Requires user association
 * - Name field used as title/description
 * -------------------------------------------------------------------------- */
@Entity
@Table(name = "user_answer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAnswer {

    /* .... Core Identifiers .... */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* .... Answer Details .... */
    /**
     * @param name Title/description of the answer
     * @note Used for quick identification
     * @todo Add validation constraints
     */
    private String name;

    /**
     * @param submissionDate When answer was provided
     * @note Stored as LocalDate for date-only operations
     * @todo Add validation for future dates
     */
    private LocalDate submissionDate;

    /* .... Relationships .... */
    /**
     * @param user The user who submitted this answer
     * @note Uses lazy fetching for performance
     * @note Required field - cannot be null
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /* @todo [FEATURE] Add answer content field with validation
     * @todo [VALIDATION] Implement name/date constraints
     * @todo [AUDIT] Add modification tracking
     * @todo [PERF] Consider indexing frequently queried fields
     */
}