/* ==========================================================================
 * PAC (Pouch Accuracy Check) Entity Module
 * 
 * PURPOSE: Represents a single PAC session with timing and productivity metrics
 * DEPENDENCIES: JPA, Lombok, Custom DateTime Converter
 * SCOPE: Core domain entity for PAC tracking
 * ========================================================================== */

package com.demoproject.demo.entity;

import com.demoproject.demo.config.TruncatedDateTimeConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/* --------------------------------------------------------------------------
 * Core PAC Entity Definition
 * 
 * FUNCTIONALITY:
 * - Tracks individual PAC session details
 * - Records timing and productivity metrics
 * - Links sessions to users and stores
 * 
 * IMPORTANT NOTES:
 * - Uses lazy loading for user relationship
 * - Automatically sets submission timestamp
 * - All timestamps truncated to seconds
 * -------------------------------------------------------------------------- */
@Entity
@Table(name = "pac", indexes = {
    @Index(name = "idx_pac_user", columnList = "user_id"),
    @Index(name = "idx_pac_store", columnList = "store"),
    @Index(name = "idx_pac_submission_date", columnList = "submission_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pac {

    /* .... Core Identifiers .... */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* .... Relationships .... */
    /**
     * @param user The user performing the PAC session
     * @note Uses lazy fetching for performance optimization
     * @note Required field - cannot be null
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /* .... Temporal Data .... */
    /**
     * @param submissionDate When the PAC was recorded
     * @note Auto-set on creation via @PrePersist
     * @note Truncated to seconds for consistency
     */
    @Column(name = "submission_date")
    @Convert(converter = TruncatedDateTimeConverter.class)
    private LocalDateTime submissionDate;

    /**
     * @param startTime Session start timestamp
     * @note Must be before endTime
     */
    @Column(name = "start_time")
    private LocalTime startTime;

    /**
     * @param endTime Session completion timestamp
     * @note Must be after startTime
     */
    @Column(name = "end_time")
    private LocalTime endTime;

    /* .... Metrics .... */
    /**
     * @param pouchesChecked Number of pouches verified
     * @note Used for productivity tracking
     */
    @Column(name = "pouches_checked")
    private Integer pouchesChecked;

    /**
     * @param store Location identifier
     * @note Required field - cannot be null
     */
    @Column(name = "store", nullable = false)
    private String store;

    /* .... Lifecycle Hooks .... */
    /**
     * Sets submission timestamp on entity creation
     * @note Truncates to seconds for consistency
     */
    @PrePersist
    protected void onCreate() {
        submissionDate = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @PreUpdate
    protected void validate() {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalStateException("End time cannot be before start time");
        }
    }

    /* @todo [FEATURE] Add duration calculation method
     * @todo [VALIDATION] Add start/end time validation
     * @todo [PERF] Consider indexing frequently queried fields
     * @todo [SECURITY] Add audit logging for changes
     */
}