/* ==========================================================================
 * Page Controller Module
 *
 * PURPOSE: Manages page navigation and routing for web application
 * DEPENDENCIES: Spring MVC, Spring Security
 * SCOPE: Web page endpoints and view resolution
 * 
 * SECURITY CONSIDERATIONS:
 * - Authentication required for most pages
 * - Role-based access control
 * - Session validation
 * ========================================================================== */

package com.demoproject.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* --------------------------------------------------------------------------
 * Page Controller Implementation
 * 
 * FUNCTIONALITY:
 * - Root URL redirection
 * - View resolution for authenticated pages
 * - Role-based page access control
 * 
 * IMPORTANT NOTES:
 * - Thread-safe implementation
 * - Stateless design pattern
 * - Centralized logging
 * -------------------------------------------------------------------------- */
@Controller
public class PageController {
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    /* .... Root Navigation .... */
    /**
     * Handles root URL redirection to home page.
     * 
     * @returns String View redirect path
     * @example GET /
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }

    /* .... Home Page Handler .... */
    /**
     * Manages home page access and authentication.
     *
     * @param model Spring MVC model for view attributes
     * @returns String View name or login redirect
     * 
     * SECURITY:
     * - Validates authentication state
     * - Handles anonymous users
     * - Sets user context
     */
    @GetMapping("/home")
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || 
            auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }
        model.addAttribute("username", auth.getName());
        return "home";
    }

    /* .... Internal Tools Section .... */
    /**
     * Packmed page access controller.
     *
     * @param model Spring MVC model for view attributes
     * @param authentication Current user's authentication context
     * @returns String View name for template resolution
     * @secure Requires CHECKER, ADMIN, or MODERATOR role
     */
    @GetMapping("/packmed")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MODERATOR')")
    public String packmed(Model model, Authentication authentication) {
        String username = authentication.getName();
        logger.info("User {} accessed packmed page", username);
        model.addAttribute("username", username);
        return "packmed";
    }

    /**
     * Wellca page access controller.
     *
     * @param model Spring MVC model for view attributes
     * @param authentication Current user's authentication context
     * @returns String View name for template resolution
     * @secure Requires CHECKER, ADMIN, or MODERATOR role
     */
    @GetMapping("/wellca")
    @PreAuthorize("hasAnyRole('CHECKER', 'ADMIN', 'MODERATOR')")
    public String wellca(Model model, Authentication authentication) {
        String username = authentication.getName();
        logger.info("User {} accessed wellca page", username);
        model.addAttribute("username", username);
        return "wellca";
    }

    /* .... NBA Statistics Section .... */
    /**
     * NBA player statistics page controller.
     *
     * @param model Spring MVC model for view attributes
     * @param authentication Current user's authentication context
     * @returns String View name for template resolution
     * @secure Requires USER, ADMIN, or MODERATOR role
     */
    @GetMapping("/nbaplayers")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public String nbaPlayers(Model model, Authentication authentication) {
        String username = authentication.getName();
        logger.info("User {} accessed NBA player stats page", username);
        model.addAttribute("username", username);
        return "nbaplayers";
    }

    /**
     * NBA team statistics page controller.
     *
     * @param model Spring MVC model for view attributes
     * @param authentication Current user's authentication context
     * @returns String View name for template resolution
     * @secure Requires USER, ADMIN, or MODERATOR role
     */
    @GetMapping("/nbateams")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public String nbaTeams(Model model, Authentication authentication) {
        String username = authentication.getName();
        logger.info("User {} accessed NBA team stats page", username);
        model.addAttribute("username", username);
        return "nbateams";
    }

    /**
     * NBA betting statistics page controller.
     *
     * @param model Spring MVC model for view attributes
     * @param authentication Current user's authentication context
     * @returns String View name for template resolution
     * @secure Requires USER, ADMIN, or MODERATOR role
     */
    @GetMapping("/nbabets")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MODERATOR')")
    public String nbaBets(Model model, Authentication authentication) {
        String username = authentication.getName();
        logger.info("User {} accessed NBA betting stats page", username);
        model.addAttribute("username", username);
        return "nbabets";
    }
}