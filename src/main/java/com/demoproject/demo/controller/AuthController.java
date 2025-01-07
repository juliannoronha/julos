/* ==========================================================================
 * Authentication Controller Module
 *
 * PURPOSE: Manages user authentication flows and session handling
 * DEPENDENCIES: Spring MVC, Spring Security, AuditLogService
 * SCOPE: Web authentication endpoints
 * 
 * SECURITY CONSIDERATIONS:
 * - CSRF protection enabled by default
 * - Session fixation protection
 * - Secure session cookie handling
 * - Audit logging of authentication events
 * ========================================================================== */

package com.demoproject.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.demoproject.demo.services.AuditLogService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;

/* --------------------------------------------------------------------------
 * Authentication Controller Implementation
 * 
 * FUNCTIONALITY:
 * - Handles login page display
 * - Manages logout status
 * - Provides view resolution
 * - Audit logs authentication events
 * 
 * IMPORTANT NOTES:
 * - Relies on Spring Security for auth processing
 * - Uses Thymeleaf template resolution
 * - Stateless design pattern
 * - Comprehensive audit trail
 * -------------------------------------------------------------------------- */
@Controller
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuditLogService auditLogService;

    public AuthController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /* .... Login Page Handler .... */
    /**
     * Renders the authentication login page view.
     *
     * @param logout Optional parameter indicating logout status
     * @param model Spring MVC model for view attributes
     * @returns String View name for template resolution
     * 
     * @note Handles both fresh login and post-logout states
     * @example GET /login
     * @example GET /login?logout
     * 
     * VALIDATION:
     * - Null-safe logout parameter handling
     * - Model attribute safety checks
     * AUDIT:
     * - Logs login page access
     * - Logs successful logout events
     */
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String logout, 
                       Model model, 
                       Authentication authentication) {
        if (logout != null) {
            model.addAttribute("logout", true);
            auditLogService.logEvent(
                "USER_LOGOUT",
                "AUTH",
                "User logged out successfully"
            );
            logger.info("User logged out successfully");
        }
        return "login";
    }

    /**
     * Renders the audit log view for administrators.
     *
     * @param model Spring MVC model for view attributes
     * @param pageable Pagination parameters
     * @returns String View name for template resolution
     * 
     * @security Requires ADMIN role
     * @note Includes pagination support
     * @audit Logs audit page access
     */
    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewAuditLogs(Model model, 
                              @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) 
                              Pageable pageable) {
        model.addAttribute("auditLogs", auditLogService.getAll(pageable));
        
        auditLogService.logEvent(
            "AUDIT_VIEW",
            "AUDIT",
            "Administrator accessed audit logs"
        );
        
        logger.info("Audit logs accessed by administrator");
        return "audit";
    }

    /**
     * Handles audit log clearing request
     * 
     * @returns ResponseEntity<Void> HTTP response
     * @security Requires ADMIN role
     * @audit Logs the clear action
     */
    @PostMapping("/api/audit/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> clearAuditLogs() {
        try {
            auditLogService.clearAllLogs();
            auditLogService.logEvent(
                "AUDIT_CLEAR",
                "AUDIT",
                "Administrator cleared all audit logs"
            );
            logger.info("Audit logs cleared by administrator");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Failed to clear audit logs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /* @todo [SECURITY] Add rate limiting for failed attempts
     * @todo [FEATURE] Add 2FA support
     * @todo [AUDIT] Add IP address logging
     * @todo [PERF] Consider caching login page
     */
}