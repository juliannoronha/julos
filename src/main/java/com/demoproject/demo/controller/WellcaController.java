package com.demoproject.demo.controller;

import com.demoproject.demo.dto.WellcaDTO;
import com.demoproject.demo.entity.Wellca;
import com.demoproject.demo.services.WellcaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Controller
@RequestMapping("/wellca-management")
public class WellcaController {
    
    private static final Logger logger = LoggerFactory.getLogger(WellcaController.class);
    private final WellcaService wellcaService;

    public WellcaController(WellcaService wellcaService) {
        this.wellcaService = wellcaService;
    }

    /**
     * Display the main Wellca form
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public String showWellcaForm(Model model) {
        model.addAttribute("wellcaDTO", new WellcaDTO());
        return "wellca";
    }

    /**
     * Submit new entry
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> submitEntry(@Valid @RequestBody WellcaDTO wellcaDTO, 
                                       BindingResult bindingResult) {
        logger.info("=== Starting Data Submission ===");
        logger.info("Received submission request for date: {}", wellcaDTO.getDate());
        
        try {
            if (bindingResult.hasErrors()) {
                logger.error("Validation errors: {}", bindingResult.getAllErrors());
                return ResponseEntity.badRequest()
                    .body(bindingResult.getAllErrors());
            }

            logger.debug("Converting DTO to entity");
            Wellca entity = convertToEntity(wellcaDTO);
            
            if (entity.getDate() == null) {
                logger.error("Date is null after conversion");
                return ResponseEntity.badRequest()
                    .body("Date cannot be null");
            }

            // Log RX Sales specific validation
            if (entity.getTotalFilled() < 0) {
                logger.error("Invalid total filled RX count: {}", entity.getTotalFilled());
                return ResponseEntity.badRequest()
                    .body("Total filled RX count cannot be negative");
            }

            logger.debug("Saving entity to database");
            Wellca savedEntry = wellcaService.saveEntry(entity);
            
            logger.info("Successfully saved entry for date: {}", savedEntry.getDate());
            logger.info("RX Sales Summary - Total Filled: {}, Total Entered: {}", 
                savedEntry.getTotalFilled(), savedEntry.getTotalEntered());
            logger.info("=== Completed Data Submission ===");
            
            return ResponseEntity.ok(convertToDTO(savedEntry));
        } catch (Exception e) {
            logger.error("Error saving Wellca entry: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body("Error saving entry: " + e.getMessage());
        }
    }

    /**
     * Get entry by date
     */
    @GetMapping("/entry/{date}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> getEntryByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return wellcaService.getEntryByDate(date)
            .map(entry -> ResponseEntity.ok(convertToDTO(entry)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get entries within date range
     */
    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WellcaDTO>> getEntriesInRange(
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.info("Fetching entries between {} and {}", startDate, endDate);
        
        // Validate date range
        if (startDate == null || endDate == null) {
            logger.error("Invalid date parameters: startDate={}, endDate={}", startDate, endDate);
            return ResponseEntity.badRequest().build();
        }
        
        if (endDate.isBefore(startDate)) {
            logger.error("End date {} is before start date {}", endDate, startDate);
            return ResponseEntity.badRequest().build();
        }
        
        try {
            List<Wellca> entries = wellcaService.getEntriesInRange(startDate, endDate);
            logger.debug("Found {} entries in date range", entries.size());
            
            List<WellcaDTO> dtos = entries.stream()
                .map(this::convertToDTO)
                .toList();
                
            return ResponseEntity.ok(dtos);
        } catch (DataAccessException e) {
            logger.error("Database error while fetching entries: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            logger.error("Error fetching entries in range: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } finally {
            // Log completion of request
            logger.debug("Completed processing range request for {} to {}", startDate, endDate);
        }
    }

    /**
     * Get weekly statistics
     */
    @GetMapping("/weekly-stats/{weekStartDate}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getWeeklyStats(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate) {
        return ResponseEntity.ok(wellcaService.getWeeklyStats(weekStartDate));
    }

    /**
     * Get monthly delivery statistics
     */
    @GetMapping("/monthly-delivery/{yearMonth}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyDeliveryCounts(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") LocalDate yearMonth) {
        return ResponseEntity.ok(wellcaService.getMonthlyDeliveryCounts(yearMonth));
    }

    /**
     * Delete entry
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        try {
            wellcaService.deleteEntry(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting Wellca entry", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Convert DTO to Entity
     */
    private Wellca convertToEntity(WellcaDTO dto) {
        logger.debug("Starting DTO to Entity conversion");
        
        if (dto == null) {
            logger.error("Received null DTO");
            throw new IllegalArgumentException("DTO cannot be null");
        }

        // Validate date first
        if (dto.getDate() == null) {
            logger.error("Date is null in DTO");
            throw new IllegalArgumentException("Date cannot be null");
        }

        Wellca entity = new Wellca();
        entity.setId(dto.getId());
        entity.setDate(dto.getDate());
        
        // Determine submission type based on non-zero values
        boolean isDeliverySubmission = dto.getPurolator() > 0 || dto.getFedex() > 0 || 
                                      dto.getOneCourier() > 0 || dto.getGoBolt() > 0;
        boolean isRxSubmission = dto.getNewRx() > 0 || dto.getRefill() > 0 || 
                                dto.getReAuth() > 0 || dto.getHold() > 0;
        boolean isProfileSubmission = dto.getProfilesEntered() > 0 || dto.getWhoFilledRx() > 0;
        boolean isServiceSubmission = dto.getServiceType() != null && dto.getServiceCost() != null;

        // Set all fields as normal
        entity.setPurolator(dto.getPurolator());
        entity.setFedex(dto.getFedex());
        entity.setOneCourier(dto.getOneCourier());
        entity.setGoBolt(dto.getGoBolt());
        entity.setNewRx(dto.getNewRx());
        entity.setRefill(dto.getRefill());
        entity.setReAuth(dto.getReAuth());
        entity.setHold(dto.getHold());
        entity.setProfilesEntered(dto.getProfilesEntered());
        entity.setWhoFilledRx(dto.getWhoFilledRx());
        entity.setActivePercentage(dto.getActivePercentage() != null ? 
            dto.getActivePercentage() : BigDecimal.ZERO);

        // Handle service fields explicitly
        logger.debug("Processing service data - Type: {}, Cost: {}, Name: {}, DOB: {}, Pharmacist: {}", dto.getServiceType(), dto.getServiceCost(), dto.getPatientName(), dto.getPatientDob(), dto.getPharmacistName());
        entity.setServiceType(dto.getServiceType());
        entity.setServiceCost(dto.getServiceCost());
        entity.setPatientName(dto.getPatientName());
        entity.setPatientDob(dto.getPatientDob());
        entity.setPharmacistName(dto.getPharmacistName());

        logger.debug("Converted entity service data: type={}, cost={}", entity.getServiceType(), entity.getServiceCost());
        return entity;
    }

    /**
     * Convert Entity to DTO
     */
    private WellcaDTO convertToDTO(Wellca entity) {
        logger.debug("Converting entity to DTO - ID: {}", entity.getId());
        logger.debug("Entity values - Patient: {}, DOB: {}, Pharmacist: {}", 
            entity.getPatientName(), entity.getPatientDob(), entity.getPharmacistName());
        
        WellcaDTO dto = new WellcaDTO();
        // Map entity fields to DTO
        dto.setId(entity.getId());
        dto.setDate(entity.getDate());
        dto.setPurolator(entity.getPurolator());
        dto.setFedex(entity.getFedex());
        dto.setOneCourier(entity.getOneCourier());
        dto.setGoBolt(entity.getGoBolt());
        dto.setNewRx(entity.getNewRx());
        dto.setRefill(entity.getRefill());
        dto.setReAuth(entity.getReAuth());
        dto.setHold(entity.getHold());
        dto.setProfilesEntered(entity.getProfilesEntered());
        dto.setWhoFilledRx(entity.getWhoFilledRx());
        dto.setActivePercentage(entity.getActivePercentage());
        
        // Explicitly set service fields
        dto.setServiceType(entity.getServiceType());
        dto.setServiceCost(entity.getServiceCost());
        
        // Explicitly set patient information
        dto.setPatientName(entity.getPatientName());
        dto.setPatientDob(entity.getPatientDob());
        dto.setPharmacistName(entity.getPharmacistName());
        
        // Set other fields...
        
        logger.debug("Converted DTO values - Patient: {}, DOB: {}, Pharmacist: {}", 
            dto.getPatientName(), dto.getPatientDob(), dto.getPharmacistName());
        
        return dto;
    }

    @GetMapping("/services/{type}")
    public ResponseEntity<List<Wellca>> getServiceDetails(
            @PathVariable String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        logger.debug("Fetching service details for type: {} between {} and {}", 
            type, startDate, endDate);
        
        List<Wellca> details = wellcaService.getServiceDetailsByType(type, startDate, endDate);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/services/monthly-stats")
    public ResponseEntity<Map<String, Object>> getMonthlyServiceStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate yearMonth) {
        
        logger.debug("Fetching monthly service stats for: {}", yearMonth);
        
        Map<String, Object> stats = wellcaService.getMonthlyServiceStats(yearMonth);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get monthly chart statistics
     */
    @GetMapping("/monthly-chart-stats/{yearMonth}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getMonthlyChartStats(
            @PathVariable String yearMonth) {
        try {
            logger.debug("Fetching monthly chart stats for: {}", yearMonth);
            
            // Parse yearMonth string (format: "yyyy-MM")
            String[] parts = yearMonth.split("-");
            if (parts.length != 2) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid date format. Use yyyy-MM"));
            }
            
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            
            // Create start and end dates for the month
            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(
                startDate.lengthOfMonth());
            
            Map<String, Object> chartData = wellcaService.getMonthlyChartStats(startDate, endDate);
            return ResponseEntity.ok(chartData);
            
        } catch (Exception e) {
            logger.error("Error fetching monthly chart stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate monthly report"));
        }
    }

    /**
     * Get quarterly chart statistics
     */
    @GetMapping("/quarterly-chart-stats/{year}/{quarter}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getQuarterlyChartStats(
            @PathVariable int year,
            @PathVariable int quarter) {
        try {
            logger.debug("Fetching quarterly chart stats for Q{} {}", quarter, year);
            Map<String, Object> chartData = wellcaService.getQuarterlyChartStats(year, quarter);
            return ResponseEntity.ok(chartData);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid quarter value: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching quarterly chart stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get weekly chart statistics with progressive loading
     */
    @GetMapping("/weekly-chart-stats")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getWeeklyChartStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int page) {
        try {
            logger.debug("Fetching weekly chart stats for period {} to {}, page {}", 
                startDate, endDate, page);

            // Validate date range
            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Start date must be before end date"));
            }

            // Validate page number
            int totalPages = wellcaService.getTotalWeeklyPages(startDate, endDate);
            if (page < 1 || page > totalPages) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid page number", "totalPages", totalPages));
            }

            Map<String, Object> chartData = wellcaService.getWeeklyChartStats(
                startDate, endDate, page);

            return ResponseEntity.ok(chartData);

        } catch (Exception e) {
            logger.error("Error fetching weekly chart stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to fetch weekly chart statistics"));
        }
    }

    /**
     * Get total pages for weekly chart data
     */
    @GetMapping("/weekly-chart-stats/pages")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getWeeklyChartPages(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            logger.debug("Calculating total pages for weekly chart stats between {} and {}", 
                startDate, endDate);

            if (startDate.isAfter(endDate)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Start date must be before end date"));
            }

            int totalPages = wellcaService.getTotalWeeklyPages(startDate, endDate);
            
            return ResponseEntity.ok(Map.of(
                "totalPages", totalPages,
                "weeksPerPage", 4
            ));

        } catch (Exception e) {
            logger.error("Error calculating weekly chart pages: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to calculate total pages"));
        }
    }
}
