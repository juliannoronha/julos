package com.demoproject.demo.entity;

import com.demoproject.demo.config.TruncatedDateTimeConverter;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/* --------------------------------------------------------------------------
 * Core Audit Log Entity Definition
 * 
 * FUNCTIONALITY:
 * - Records administrative actions
 * - Tracks user operations
 * - Maintains security audit trail
 * 
 * IMPORTANT NOTES:
 * - Timestamps truncated to seconds
 * - Immutable after creation
 * - Requires admin access for viewing
 * -------------------------------------------------------------------------- */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    /* .... Core Identifiers .... */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* .... Audit Details .... */
    /**
     * @param action Description of the performed action
     * @note Required field - cannot be null
     */
    @Column(nullable = false)
    private String action;

    /**
     * @param performedBy Username of the action performer
     * @note Required field - cannot be null
     */
    @Column(nullable = false)
    private String performedBy;

    /**
     * @param timestamp When the action occurred
     * @note Auto-set on creation
     * @note Truncated to seconds
     */
    @Column(nullable = false)
    @Convert(converter = TruncatedDateTimeConverter.class)
    private LocalDateTime timestamp;

    /**
     * @param category Type/category of the action
     * @note Required field - cannot be null
     */
    @Column(nullable = false)
    private String category;

    /**
     * @param details Additional information about the action
     * @note Optional field
     */
    @Column(length = 1000)
    private String details;

    /* .... Entity Relationships .... */
    /**
     * @param user Reference to the user who performed the action
     * @note Uses lazy fetching for performance
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /* .... Lifecycle Hooks .... */
    /**
     * Sets timestamp on entity creation
     * @note Truncates to seconds for consistency
     */
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /* .... Constructors .... */
    /**
     * Creates new audit log entry with basic attributes
     * 
     * @param action Description of the action
     * @param performedBy Username of the performer
     * @param category Action category
     * @param details Additional context
     */
    public AuditLog(String action, String performedBy, String category, String details) {
        this.action = action;
        this.performedBy = performedBy;
        this.category = category;
        this.details = details;
        this.timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /* @todo [FEATURE] Add action type enumeration
     * @todo [PERF] Add indexes for frequent queries
     * @todo [CLEANUP] Implement log rotation/archiving
     * @todo [SECURITY] Add encryption for sensitive details
     */
}
