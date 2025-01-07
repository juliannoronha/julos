package com.demoproject.demo.services;

import com.demoproject.demo.entity.AuditLog;
import com.demoproject.demo.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    /* --------------------------------------------------------------------------
     * Service Dependencies
     * -------------------------------------------------------------------------- */
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /* --------------------------------------------------------------------------
     * Audit Logging Operations
     * -------------------------------------------------------------------------- */

    /**
     * Creates new audit log entry
     * 
     * @param action Description of the action
     * @param category Action category
     * @param details Additional context
     * @security Automatically captures current user
     * @performance Single transaction scope
     */
    @Transactional
    public void logEvent(String action, String category, String details) {
        logger.debug("Creating audit log entry: action={}, category={}", action, category);
        try {
            String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
            
            AuditLog auditLog = new AuditLog(action, currentUser, category, details);
            auditLogRepository.save(auditLog);
            
            logger.info("Audit log created successfully: action={}, user={}, category={}", 
                       action, currentUser, category);
        } catch (Exception e) {
            logger.error("Failed to create audit log: action={}, category={}", action, category, e);
            throw new RuntimeException("Failed to create audit log", e);
        }
    }

    /**
     * Clears all audit log entries
     * 
     * @security Requires ADMIN role
     * @performance Single transaction scope
     * @audit Self-logging is handled at controller level
     */
    @Transactional
    public void clearAllLogs() {
        try {
            logger.info("Initiating complete audit log clearance");
            auditLogRepository.deleteAll();
            logger.info("Audit logs cleared successfully");
        } catch (Exception e) {
            logger.error("Failed to clear audit logs", e);
            throw new RuntimeException("Failed to clear audit logs", e);
        }
    }

    /* --------------------------------------------------------------------------
     * Audit Retrieval Operations
     * -------------------------------------------------------------------------- */

    /**
     * Retrieves paginated audit logs
     * 
     * @param pageable Pagination parameters
     * @returns Page of audit log entries
     * @security Requires ADMIN role
     * @performance Uses database-level pagination
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAll(Pageable pageable) {
        logger.debug("Retrieving audit logs page: {}", pageable);
        return auditLogRepository.getAll(pageable);
    }

    /**
     * Retrieves audit logs for specific user
     * 
     * @param username Target user's identifier
     * @param pageable Pagination parameters
     * @returns Page of user's audit entries
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getByUser(String username, Pageable pageable) {
        return auditLogRepository.getByUser(username, pageable);
    }

    /**
     * Retrieves logs by category
     * 
     * @param category Action category to filter
     * @param pageable Pagination parameters
     * @returns Page of categorized audit entries
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getByCategory(String category, Pageable pageable) {
        return auditLogRepository.getByCategory(category, pageable);
    }

    /**
     * Retrieves logs within date range
     * 
     * @param startDate Range start (inclusive)
     * @param endDate Range end (inclusive)
     * @returns List of matching audit entries
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.getByDateRange(startDate, endDate);
    }

    /**
     * Retrieves recent actions by user
     * 
     * @param username Username to filter by
     * @param limit Maximum number of records to return
     * @returns List of recent user actions
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentByUser(String username, int limit) {
        return auditLogRepository.getRecentByUser(username, limit);
    }

    /* --------------------------------------------------------------------------
     * Future Enhancements
     * -------------------------------------------------------------------------- */

    /* @todo Implementation needs:
     * - Add support for bulk audit operations
     * - Implement audit log archiving
     * - Add audit log search capabilities
     * - Add support for custom audit fields
     * - Add audit log retention policy
     */
}
