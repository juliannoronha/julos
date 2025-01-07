package com.demoproject.demo.repository;

import com.demoproject.demo.entity.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/* ==========================================================================
 * UserAnswer Repository Interface
 * 
 * PURPOSE: Manages persistence operations for UserAnswer entities
 * EXTENDS: JpaRepository for standard CRUD operations
 * 
 * @note: All methods are transactional by default
 * @dependency: Requires UserAnswer entity and Spring Data JPA
 * ========================================================================== */
@Repository
public interface UserAnswerRepository extends JpaRepository<UserAnswer, Long> {
    
    /* ---------- User-Specific Query Methods ---------- */
    
    /**
     * Finds user answers using Spring Data's method naming convention.
     * 
     * @param username Target user's identifier
     * @return List<UserAnswer> - Empty if no matches found
     * @note Automatically handles null username validation
     */
    List<UserAnswer> findByUser_Username(String username);

    /**
     * Retrieves user answers using explicit JPQL query.
     * 
     * @param username Target user's identifier
     * @return List<UserAnswer> - Empty if no matches found
     * @throws IllegalArgumentException if username is null
     * @note Preferred over findByUser_Username for complex queries
     */
    @Query("SELECT ua FROM UserAnswer ua WHERE ua.user.username = :username")
    List<UserAnswer> findAllByUsername(@Param("username") String username);

    /* ---------- Aggregation Methods ---------- */
    
    /**
     * Counts total answers for a specific user.
     * 
     * @param username Target user's identifier
     * @return long - Total count (0 if no matches)
     * @note Optimized for counting without loading entities
     */
    @Query("SELECT COUNT(ua) FROM UserAnswer ua WHERE ua.user.username = :username")
    long countByUsername(@Param("username") String username);

    /* ---------- Bulk Query Methods ---------- */
    
    /**
     * Retrieves all answers in read-only mode.
     * 
     * @return List<UserAnswer> - All answers in the system
     * @note Consider pagination for large datasets
     * @todo Add pagination support
     * @performance May cause memory issues with large datasets
     */
    @Query("SELECT ua FROM UserAnswer ua")
    List<UserAnswer> findAllReadOnly();

    /* ---------- Future Enhancements ---------- */
    
    /**
     * @todo Implement date range filtering:
     * - Add method: findBySubmissionDateBetween(Date start, Date end)
     * - Consider using Spring's Specification API for dynamic filtering
     * 
     * @todo Add content-based search:
     * - Implement full-text search capabilities
     * - Add support for answer content filtering
     * - Consider using database-specific full-text search
     */
}
