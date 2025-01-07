/* =============================================================================
 * Productivity Controller Module
 * =============================================================================
 * PURPOSE: Handles all productivity-related HTTP endpoints and data streaming
 * DEPENDENCIES: Spring Framework, UserProductivityService, PacRepository
 * AUTHOR: DemoProject Team
 * LAST UPDATED: Current Version
 */

package com.demoproject.demo.pacmedproductivity;

import com.demoproject.demo.repository.PacRepository;
import com.demoproject.demo.services.UserService;

import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Random;

import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/* -----------------------------------------------------------------------------
 * Core Controller Definition
 * -----------------------------------------------------------------------------
 * FUNCTIONALITY:
 * - REST endpoints for productivity data
 * - Server-sent events for real-time updates
 * - Caching and pagination support
 * - Security and role-based access
 */
@Controller
public class ProductivityController {
    private static final Logger logger = LoggerFactory.getLogger(ProductivityController.class);

    private final UserProductivityService userProductivityService;
    private final PacRepository pacRepository;
    private final ThreadPoolTaskExecutor taskExecutor;

    /* .... Constructor .... */
    /**
     * @param userProductivityService Handles productivity calculations
     * @param userService User management operations
     * @param pacRepository Data access for PAC records
     * @param taskExecutor Manages async SSE connections
     * @note All parameters are required and non-null
     */
    public ProductivityController(UserProductivityService userProductivityService, 
                                UserService userService, 
                                PacRepository pacRepository, 
                                ThreadPoolTaskExecutor taskExecutor) {
        this.userProductivityService = userProductivityService;
        this.pacRepository = pacRepository;
        this.taskExecutor = taskExecutor;
    }

    /* .... API Endpoints .... */
    
    /**
     * @returns Overall productivity metrics with chart data
     * @throws Exception on data access errors
     * @note Cached response unless null
     * @security Requires ADMIN or MODERATOR role
     */
    @Operation(summary = "Get overall productivity metrics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved productivity data"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/overall-productivity")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    @Cacheable(value = "overallProductivity", key = "'overall'", unless = "#result == null")
    public ResponseEntity<UserProductivityDTO> getOverallProductivity() {
        logger.info("Fetching overall productivity");
        try {
            UserProductivityDTO overallProductivity = userProductivityService.getOverallProductivity();
            Map<String, Object> chartData = generateChartData();
            
            logger.debug("Overall productivity: {}", overallProductivity);
            logger.debug("Chart data: {}", chartData);
            
            UserProductivityDTO productivityWithChartData = new UserProductivityDTO(
                overallProductivity.getUsername(),
                overallProductivity.getTotalSubmissions(),
                overallProductivity.getTotalPouchesChecked(),
                overallProductivity.getAvgTimePerPouch(),
                overallProductivity.getAvgPouchesPerHour(),
                chartData
            );
            
            logger.debug("Final productivity DTO: {}", productivityWithChartData);
            return ResponseEntity.ok(productivityWithChartData);
        } catch (Exception e) {
            logger.error("Error fetching overall productivity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new UserProductivityDTO("Error", 0, 0, 0, 0, null));
        }
    }

    /**
     * @param model View model for Thymeleaf template
     * @param page Zero-based page index
     * @param size Results per page
     * @returns Template name for view resolution
     * @note Supports pagination with default size of 10
     */
    @GetMapping("/user-productivity")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public String userProductivity(Model model, 
                                 @RequestParam(defaultValue = "0") int page, 
                                 @RequestParam(defaultValue = "10") int size) {
        logger.info("Fetching user productivity for all users. Page: {}, Size: {}", page, size);
        Page<UserProductivityDTO> usersPage = userProductivityService.getAllUserProductivity(page, size);
        logger.debug("Retrieved {} user productivity records", usersPage.getContent().size());
        model.addAttribute("usersPage", usersPage);
        return "user-productivity";
    }

    /**
     * @param username Target user (alphanumeric + underscore/hyphen, 3-50 chars)
     * @returns User's productivity metrics
     * @throws 404 if user not found
     * @security Requires ADMIN or MODERATOR role
     */
    @GetMapping("/api/user-productivity/{username}")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Map<String, Object>> getUserProductivity(
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]{3,50}$") String username) {
        if (!userProductivityService.userExists(username)) {
            logger.warn("Attempted to access non-existent user: {}", username);
            return ResponseEntity.notFound().build();
        }
        logger.info("Fetching user productivity for username: {}", username);
        Map<String, Object> productivity = userProductivityService.getUserProductivity(username);
        logger.debug("User productivity for {}: {}", username, productivity);
        return ResponseEntity.ok(productivity);
    }

    /* .... SSE Streaming Endpoints .... */

    /**
     * @returns SSE emitter for real-time productivity updates
     * @note Manages emitter lifecycle with cleanup
     * @performance Uses ThreadPoolTaskExecutor for async handling
     */
    @GetMapping(value = "/api/user-productivity-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamUserProductivity() {
        SseEmitter emitter = new SseEmitter(UserProductivityService.SSE_TIMEOUT);
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    emitter.onCompletion(() -> cleanupEmitter(emitter));
                    emitter.onTimeout(() -> cleanupEmitter(emitter));
                    emitter.onError(ex -> cleanupEmitter(emitter));
                    userProductivityService.subscribeToProductivityUpdates(emitter);
                } catch (Exception e) {
                    logger.error("Error in SSE stream: ", e);
                    emitter.completeWithError(e);
                    cleanupEmitter(emitter);
                }
            }
        });
        return emitter;
    }

    private void cleanupEmitter(SseEmitter emitter) {
        userProductivityService.removeEmitter(emitter);
        logger.info("SSE connection cleaned up");
    }

    /**
     * @param page Zero-based page index
     * @param size Results per page
     * @returns Paginated productivity data for all users
     * @security Requires ADMIN or MODERATOR role
     */
    @GetMapping("/api/all-user-productivity")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Page<UserProductivityDTO>> getAllUserProductivity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        logger.info("Fetching all user productivity data. Page: {}, Size: {}", page, size);
        Page<UserProductivityDTO> productivityData = userProductivityService.getAllUserProductivity(page, size);
        logger.debug("Retrieved {} user productivity records", productivityData.getContent().size());
        return ResponseEntity.ok(productivityData);
    }

    /**
     * @returns SSE emitter for overall productivity updates
     * @note Managed by UserProductivityService
     */
    @GetMapping(value = "/api/overall-productivity-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOverallProductivity() {
        logger.info("New SSE connection established for overall productivity");
        return userProductivityService.subscribeToOverallProductivityUpdates();
    }

    /* .... Helper Methods .... */

    /**
     * @returns Chart data for last 7 days
     * @throws RuntimeException on data generation failure
     * @note Generates sample data if no real data available
     */
    private Map<String, Object> generateChartData() {
        try {
            Map<String, Object> chartData = new HashMap<>();
            List<String> labels = new ArrayList<>();
            List<Integer> pouchesChecked = new ArrayList<>();

            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(6).withHour(0).withMinute(0).withSecond(0);

            logger.debug("Fetching data from {} to {}", startDate, endDate);
            
            List<Object[]> results = pacRepository.getPouchesCheckedLast7Days(startDate, endDate);

            if (results.isEmpty()) {
                logger.info("No data available for the last 7 days. Generating sample data.");
                Random random = new Random();
                for (LocalDate date = startDate.toLocalDate(); !date.isAfter(endDate.toLocalDate()); date = date.plusDays(1)) {
                    labels.add(date.format(DateTimeFormatter.ofPattern("MMM dd")));
                    pouchesChecked.add(random.nextInt(100) + 1);
                }
            } else {
                Map<LocalDate, Long> resultMap = new HashMap<>();
                for (Object[] row : results) {
                    if (row[0] instanceof java.sql.Date) {
                        LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
                        Long count = (Long) row[1];
                        resultMap.put(date, count);
                    }
                }

                for (LocalDate date = startDate.toLocalDate(); !date.isAfter(endDate.toLocalDate()); date = date.plusDays(1)) {
                    labels.add(date.format(DateTimeFormatter.ofPattern("MMM dd")));
                    pouchesChecked.add(resultMap.getOrDefault(date, 0L).intValue());
                }
            }

            chartData.put("labels", labels);
            chartData.put("pouchesChecked", pouchesChecked);
            logger.debug("Generated chart data: {}", chartData);
            return chartData;
        } catch (Exception e) {
            logger.error("Error generating chart data", e);
            throw new RuntimeException("Failed to generate chart data", e);
        }
    }

    /**
     * Scheduled cache cleanup every 5 minutes
     * @note Prevents stale productivity data
     */
    @CacheEvict(value = "overallProductivity", allEntries = true)
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void clearProductivityCache() {
        logger.info("Clearing productivity cache");
    }
}
