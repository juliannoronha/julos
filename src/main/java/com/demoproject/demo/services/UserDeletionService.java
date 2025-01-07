/* =============================================================================
 * User Deletion Service
 * =============================================================================
 * PURPOSE: Manages secure deletion of user accounts from the system
 * 
 * CORE FUNCTIONALITY:
 * - Safe user deletion with role validation
 * - Transactional integrity
 * - Admin protection
 * 
 * DEPENDENCIES:
 * - Spring Framework (Service, Transactional)
 * - UserRepository for persistence
 * - User entity
 * 
 * SECURITY NOTES:
 * - Prevents deletion of admin accounts
 * - Validates user existence before deletion
 * ============================================================================= */
package com.demoproject.demo.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.demoproject.demo.repository.UserRepository;
import com.demoproject.demo.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserDeletionService {

    /* --------------------------------------------------------------------------
     * Service Dependencies
     * -------------------------------------------------------------------------- */
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private static final Logger logger = LoggerFactory.getLogger(UserDeletionService.class);

    /**
     * Initializes deletion service with required data access.
     * 
     * @param userRepository Data access for user operations
     * @param auditLogService Data access for audit logging
     * @note Repository must be non-null
     */
    public UserDeletionService(UserRepository userRepository, 
                             AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    /* --------------------------------------------------------------------------
     * Deletion Operations
     * -------------------------------------------------------------------------- */

    /**
     * Executes user deletion with security validation.
     * 
     * @param username Target user identifier
     * @throws IllegalStateException Admin deletion attempted
     * @throws IllegalArgumentException User not found
     * @security Prevents admin deletion
     * @performance Single transaction scope
     * @note Consider audit logging before deletion
     */
    @Transactional
    public void deleteUser(String username) {
        logger.debug("Attempting to delete user: {}", username);
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found for deletion: {}", username);
                    return new IllegalArgumentException("User not found: " + username);
                });
            
            if ("ADMIN".equals(user.getRole())) {
                logger.warn("Attempted to delete admin user: {}", username);
                throw new IllegalStateException("Admin users cannot be deleted");
            }
            
            auditLogService.logEvent("USER_DELETION", "USER", 
                "User deleted: " + username);
            
            userRepository.delete(user);
            logger.info("Successfully deleted user: {}", username);
        } catch (Exception e) {
            logger.error("Failed to delete user: {}", username, e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    /* --------------------------------------------------------------------------
     * Future Enhancements
     * -------------------------------------------------------------------------- */

    /* @todo Implementation needs:
     * - Cascading deletion support
     * - Audit logging integration
     * - Soft deletion option
     * - Backup before deletion
     * - Rate limiting for bulk operations
     */
}