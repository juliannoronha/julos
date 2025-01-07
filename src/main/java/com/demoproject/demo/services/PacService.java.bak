/* =================================================================
 * PAC (Productivity Activity Counter) Service
 * 
 * PURPOSE: Manages productivity tracking through PAC entries
 * 
 * CORE FUNCTIONALITY:
 * - CRUD operations for productivity records
 * - User-based access control and validation
 * - Integration with productivity calculations
 * 
 * DEPENDENCIES:
 * - Spring Framework (Service, Transactional, Retry)
 * - PAC and User repositories
 * - UserProductivityService
 * 
 * IMPORTANT NOTES:
 * - Uses pagination for large dataset handling
 * - Implements retry logic for transient failures
 * - Requires valid user context for operations
 * ================================================================= */
package com.demoproject.demo.services;

import com.demoproject.demo.entity.Pac;
import com.demoproject.demo.entity.User;
import com.demoproject.demo.repository.PacRepository;
import com.demoproject.demo.repository.UserRepository;
import com.demoproject.demo.pacmedproductivity.UserProductivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service layer for PAC (Productivity Activity Counter) management.
 * 
 * @security Access control per user context
 * @performance Paginated data access, retry mechanisms
 * @note Thread-safe implementation
 */
@Service
public class PacService {
    private static final Logger logger = LoggerFactory.getLogger(PacService.class);
    private final PacRepository pacRepository;
    private final UserRepository userRepository;
    private final UserProductivityService userProductivityService;

    /**
     * Initializes service with required dependencies.
     * 
     * @param pacRepository Data access for PAC records
     * @param userRepository Data access for user records
     * @param userProductivityService Productivity calculation service
     * @note All parameters are required and non-null
     */
    public PacService(PacRepository pacRepository,
                     UserRepository userRepository,
                     UserProductivityService userProductivityService) {
        this.pacRepository = pacRepository;
        this.userRepository = userRepository;
        this.userProductivityService = userProductivityService;
    }

    /* ===== PAC Creation Operations ===== */

    /**
     * Creates new PAC entry with automatic failure recovery.
     *
     * @param pac Entry to submit (must contain all required fields)
     * @param username Associated user identifier
     * @throws RuntimeException User not found
     * @throws IllegalArgumentException Invalid PAC data
     * @note Creates new transaction to avoid deadlocks
     */
    @Retryable(
        value = {TransientDataAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void submitPac(Pac pac, String username) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
            
            pac.setUser(user);
            validatePac(pac);
            pacRepository.save(pac);
            
            userProductivityService.notifyProductivityUpdate();
        } catch (Exception e) {
            logger.error("Failed to submit PAC", e);
            throw new RuntimeException("Failed to submit PAC", e);
        }
    }

    /**
     * Validates PAC entry completeness.
     *
     * @param pac Entry to validate
     * @throws IllegalArgumentException Missing required fields
     * @note Required: store, startTime, endTime, pouchesChecked
     */
    private void validatePac(Pac pac) {
        if (pac.getStore() == null || pac.getStartTime() == null || 
            pac.getEndTime() == null || pac.getPouchesChecked() == null) {
            throw new IllegalArgumentException("Required PAC fields missing");
        }
    }

    /* ===== PAC Query Operations ===== */

    /**
     * Retrieves PAC entries with filtering options.
     *
     * @param pageable Pagination settings
     * @param nameFilter Optional username filter (case-insensitive)
     * @param store Optional store identifier
     * @param month Optional month filter
     * @returns Paginated, filtered PAC entries
     * @note Null filters are ignored
     */
    @Transactional(readOnly = true)
    public Page<Pac> getAllPacsWithFilters(Pageable pageable, String nameFilter, 
                                         String store, Integer month) {
        String lcNameFilter = nameFilter != null && !nameFilter.isEmpty() ? 
                            nameFilter.toLowerCase() : null;
        String validStore = store != null && !store.isEmpty() ? store : null;
        return pacRepository.findAllWithFilters(pageable, lcNameFilter, 
                                              validStore, month);
    }

    /**
     * Retrieves all PAC entries with pagination.
     *
     * @param pageable Pagination settings
     * @returns Paginated PAC entries
     */
    public Page<Pac> getAllPacs(Pageable pageable) {
        return pacRepository.findAll(pageable);
    }

    /* ===== PAC Management Operations ===== */

    /**
     * Removes PAC entry by identifier.
     *
     * @param id Entry identifier
     * @returns true if deleted, false if not found
     * @note Transactional operation
     */
    @Transactional
    public boolean deletePac(Long id) {
        return pacRepository.findById(id)
            .map(pac -> {
                try {
                    pacRepository.delete(pac);
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to delete PAC: {}", id, e);
                    throw new RuntimeException("Failed to delete PAC", e);
                }
            })
            .orElse(false);
    }

    /**
     * Retrieves single PAC entry.
     *
     * @param id Entry identifier
     * @returns PAC entry or null if not found
     */
    public Pac getPacById(Long id) {
        return pacRepository.findById(id).orElse(null);
    }
}