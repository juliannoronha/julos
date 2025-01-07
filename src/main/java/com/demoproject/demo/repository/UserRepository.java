/* =============================================================================
 * User Repository Interface
 * =============================================================================
 * PURPOSE: Manages persistence operations for User entities
 * EXTENDS: JpaRepository for standard CRUD operations
 * 
 * @dependency Spring Data JPA
 * @dependency User entity
 * @note All methods are transactional by default
 * @performance Consider caching for frequently accessed users
 */
package com.demoproject.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.demoproject.demo.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /* -----------------------------------------------------------------------------
     * Core User Queries
     * -------------------------------------------------------------------------- */
    
    /**
     * Finds user by exact username match
     * 
     * @param username Target username (case-sensitive)
     * @returns Optional<User> - Empty if not found
     * @note Consider adding case-insensitive variant if needed
     * @performance Indexed lookup, O(1) expected
     */
    Optional<User> findByUsername(String username);
    
    /* -----------------------------------------------------------------------------
     * Aggregate Queries
     * -------------------------------------------------------------------------- */
    
    /**
     * Retrieves active users with submissions
     * 
     * @returns List<User> - Users with at least one answer
     * @note Uses DISTINCT to prevent duplicates
     * @performance May need pagination for large datasets
     * @todo Consider adding date range parameter
     */
    @Query("SELECT DISTINCT ua.user FROM UserAnswer ua")
    List<User> findUsersWithSubmissions();

    /* -----------------------------------------------------------------------------
     * Future Enhancements
     * -------------------------------------------------------------------------- */
    
    /**
     * @todo Role-based queries:
     * - Add findByRole(Role role)
     * - Consider hierarchical role support
     * 
     * @todo Activity tracking:
     * - Add findInactiveUsers(Duration duration)
     * - Track last login timestamps
     * - Implement session management
     */
}