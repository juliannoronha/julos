/* ==========================================================================
 * Custom Error Controller Module
 *
 * PURPOSE: Provides centralized error handling and user-friendly error pages
 * DEPENDENCIES: Spring MVC, Jakarta Servlet API
 * SCOPE: Application-wide error handling
 * 
 * SECURITY CONSIDERATIONS:
 * - Sanitizes error messages for client display
 * - Avoids exposing internal error details
 * - Maintains consistent error responses
 * ========================================================================== */

package com.demoproject.demo.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/* --------------------------------------------------------------------------
 * Error Controller Implementation
 * 
 * FUNCTIONALITY:
 * - Intercepts Spring Boot error pages
 * - Maps error codes to user messages
 * - Renders error view templates
 * 
 * IMPORTANT NOTES:
 * - Overrides default Spring Boot error handling
 * - Uses Thymeleaf template resolution
 * - Stateless design pattern
 * -------------------------------------------------------------------------- */
@Controller
public class CustomErrorController implements ErrorController {

    /* .... Error Request Handler .... */
    /**
     * Maps error status codes to appropriate user messages.
     *
     * @param request The HTTP request containing error details
     * @param model Spring MVC model for view attributes
     * @returns String View name for template resolution
     * 
     * ERROR HANDLING:
     * - Handles 404 Not Found gracefully
     * - Manages 403 Forbidden access
     * - Provides generic message for other errors
     * 
     * @note Thread-safe implementation
     * @note Null-safe status code handling
     * @example GET /error?status=404
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMessage = "Error, you cannot access this page!";
        
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                errorMessage = "Sorry, the page you are looking for could not be found.";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                errorMessage = "Sorry, you don't have permission to access this page.";
            }
        }
        
        model.addAttribute("errorMessage", errorMessage);
        return "error";
    }
}