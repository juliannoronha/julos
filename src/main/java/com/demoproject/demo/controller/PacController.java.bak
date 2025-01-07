/* ==========================================================================
 * PAC (Pouch Accuracy Check) Controller Module
 *
 * PURPOSE: Manages PAC system interactions including submissions and responses
 * DEPENDENCIES: Spring MVC, Spring Security, PAC Service
 * SCOPE: Web endpoints for PAC functionality
 * 
 * SECURITY CONSIDERATIONS:
 * - Authentication required for submissions
 * - Role-based access control for admin functions
 * - Input validation and sanitization
 * ========================================================================== */

package com.demoproject.demo.controller;

import com.demoproject.demo.entity.Pac;
import com.demoproject.demo.services.PacService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.demoproject.demo.annotation.RequiresAuthentication;
import org.springframework.ui.Model;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/* --------------------------------------------------------------------------
 * PAC Controller Implementation
 * 
 * FUNCTIONALITY:
 * - Question submission handling
 * - Response management and viewing
 * - Access control enforcement
 * 
 * IMPORTANT NOTES:
 * - Thread-safe implementation
 * - Stateless design pattern
 * - Centralized logging
 * -------------------------------------------------------------------------- */
@Controller
public class PacController {

    private static final Logger logger = LoggerFactory.getLogger(PacController.class);
    private final PacService pacService;

    public PacController(PacService pacService) {
        this.pacService = pacService;
    }

    /* .... Question Display Handler .... */
    /**
     * Renders the PAC questions interface for authenticated users.
     *
     * @returns String View name for template resolution
     * @secure Requires valid authentication
     * @example GET /questions
     */
    @GetMapping("/questions")
    @RequiresAuthentication
    public String showQuestions() {
        return "packmed";
    }

    /* .... Question Submission Handler .... */
    /**
     * Processes PAC data submissions from authenticated users.
     *
     * @param pacData Map of PAC form data (store, times, counts)
     * @param authentication Current user's authentication context
     * @returns ResponseEntity with submission status
     * 
     * @secure Requires valid authentication
     * @throws Exception on parsing or validation errors
     * 
     * VALIDATION:
     * - Authentication state verification
     * - Required field presence
     * - Time format validation
     * - Numeric value parsing
     */
    @PostMapping("/submit-questions")
    @RequiresAuthentication
    @ResponseBody
    public ResponseEntity<String> submitQuestions(@RequestBody Map<String, String> pacData, 
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized access attempt to submit questions");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                               .body("User not authenticated");
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            Pac pac = new Pac();
            pac.setStore(pacData.get("store"));
            pac.setStartTime(LocalTime.parse(pacData.get("startTime").split("T")[1], 
                    formatter));
            pac.setEndTime(LocalTime.parse(pacData.get("endTime").split("T")[1], 
                    formatter));
            pac.setPouchesChecked(Integer.parseInt(pacData.get("pouchesChecked")));

            pacService.submitPac(pac, authentication.getName());

            return ResponseEntity.ok("Questions submitted successfully");
        } catch (Exception e) {
            logger.error("Error submitting questions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error submitting questions: " + e.getMessage());
        }
    }

    /* .... Response Management .... */
    /**
     * Deletes a PAC response record.
     *
     * @param id Response identifier
     * @returns ResponseEntity with deletion status
     * 
     * @secure Requires ADMIN or MODERATOR role
     * @throws Exception on deletion errors
     * 
     * VALIDATION:
     * - Record existence check
     * - Permission verification
     */
    @PostMapping("/delete-response")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<String> deleteResponse(@RequestParam Long id) {
        try {
            boolean deleted = pacService.deletePac(id);
            if (deleted) {
                return ResponseEntity.ok("Response deleted successfully.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                   .body("Response not found with id: " + id);
            }
        } catch (Exception e) {
            logger.error("Error deleting response", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error deleting response: " + e.getMessage());
        }
    }

    /* .... Response Viewing Interface .... */
    /**
     * Displays filtered and paginated PAC responses.
     *
     * @param model View model for template attributes
     * @param page Page number (zero-based)
     * @param size Results per page
     * @param nameFilter Optional username filter
     * @param store Optional store filter
     * @param month Optional month filter
     * @returns String View name for template resolution
     * 
     * @secure Requires ADMIN or MODERATOR role
     * @throws NumberFormatException on invalid numeric input
     * 
     * PERFORMANCE:
     * - Paginated results
     * - Optimized sorting
     * - Filtered queries
     */
    @GetMapping("/view-responses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public String viewResponses(Model model, 
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String nameFilter,
            @RequestParam(required = false) String store,
            @RequestParam(required = false) String month) {
        try {
            Integer monthValue = (month != null && !month.isEmpty() && 
                    !month.equals("null")) ? Integer.parseInt(month) : null;

            Pageable pageable = PageRequest.of(page, size, 
                    Sort.by(Sort.Direction.DESC, "submissionDate"));
            
            Page<Pac> responsesPage = pacService.getAllPacsWithFilters(
                pageable, nameFilter, store, monthValue);
            
            model.addAttribute("responses", responsesPage);
            model.addAttribute("nameFilter", nameFilter);
            model.addAttribute("selectedStore", store);
            model.addAttribute("selectedMonth", month);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", responsesPage.getTotalPages());
            model.addAttribute("totalItems", responsesPage.getTotalElements());
            
            return "responses";
        } catch (NumberFormatException e) {
            logger.warn("Invalid number format: {}", e.getMessage());
            return "error";
        }
    }

    /* TODO: Future Enhancements
     * - Add CSV/Excel export functionality
     * - Implement response caching
     * - Add batch operations for admins
     * - Enhance filtering capabilities
     */
}