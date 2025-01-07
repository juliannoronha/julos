/* =============================================================================
 * PAC (Pouch Accuracy Check) Repository
 * =============================================================================
 * PURPOSE: Provides data access and querying capabilities for PAC entities
 * DEPENDENCIES: 
 * - Spring Data JPA
 * - Hibernate
 * - PostgreSQL (for native queries)
 * 
 * @author DemoProject Team
 * @version 1.0
 */
package com.demoproject.demo.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.demoproject.demo.entity.Pac;
import java.time.LocalDateTime;

@Repository
public interface PacRepository extends JpaRepository<Pac, Long> {

    /* -----------------------------------------------------------------------------
     * Basic Queries
     * -------------------------------------------------------------------------- */

    /**
     * Retrieves all PAC entries for a specific user
     * @param username Target user's username
     * @returns List of matching PAC records
     */
    List<Pac> findByUser_Username(String username);

    /* -----------------------------------------------------------------------------
     * Productivity Analytics
     * -------------------------------------------------------------------------- */

    /**
     * Calculates paginated productivity metrics per user
     * @param pageable Pagination parameters
     * @returns Page of metrics arrays: [username, submissions, pouches, avgTime, rate]
     * @note Uses native query for optimal performance
     */
    @Query(value = """
        SELECT 
            u.username,
            COUNT(p.id) as totalSubmissions,
            COALESCE(SUM(p.pouches_checked), 0) as totalPouchesChecked,
            CASE 
                WHEN COUNT(p.id) > 0 THEN 
                    AVG(EXTRACT(EPOCH FROM (p.end_time - p.start_time))) / COUNT(p.id)
                ELSE 0 
            END as avgTimePerPouch,
            CASE 
                WHEN SUM(EXTRACT(EPOCH FROM (p.end_time - p.start_time))) > 0 THEN 
                    (SUM(p.pouches_checked) * 3600.0) / SUM(EXTRACT(EPOCH FROM (p.end_time - p.start_time)))
                ELSE 0 
            END as avgPouchesPerHour
        FROM users u
        INNER JOIN pac p ON p.user_id = u.id
        GROUP BY u.username
        ORDER BY totalSubmissions DESC
        """,
        countQuery = "SELECT COUNT(DISTINCT u.username) FROM users u INNER JOIN pac p ON p.user_id = u.id",
        nativeQuery = true)
    Page<Object[]> getUserProductivityDataPaginated(Pageable pageable);

    /* -----------------------------------------------------------------------------
     * Filtered Queries
     * -------------------------------------------------------------------------- */

    /**
     * Counts PAC entries matching specified filters
     * @param name Username filter (partial, case-insensitive)
     * @param store Store identifier
     * @param month Month number (1-12)
     * @returns Total matching records
     */
    @Query("SELECT COUNT(p) FROM Pac p WHERE " +
           "(:name IS NULL OR LOWER(p.user.username) LIKE %:name%) AND " +
           "(:store IS NULL OR p.store = :store) AND " +
           "(:month IS NULL OR MONTH(p.startTime) = :month)")
    long countAllWithFilters(@Param("name") String name, 
                           @Param("store") String store, 
                           @Param("month") Integer month);

    /**
     * Retrieves filtered PAC entries with pagination
     * @param pageable Pagination parameters
     * @param nameFilter Username filter (partial, case-insensitive)
     * @param store Store identifier
     * @param month Month number (1-12)
     * @returns Page of matching PAC records with user data
     * @note Uses JOIN FETCH to avoid N+1 queries
     */
    @Query("SELECT p FROM Pac p JOIN FETCH p.user u WHERE " +
           "(:nameFilter IS NULL OR LOWER(u.username) LIKE %:nameFilter%) AND " +
           "(:store IS NULL OR p.store = :store) AND " +
           "(:month IS NULL OR MONTH(p.submissionDate) = :month)")
    Page<Pac> findAllWithFilters(Pageable pageable, 
                               @Param("nameFilter") String nameFilter, 
                               @Param("store") String store, 
                               @Param("month") Integer month);

    /* -----------------------------------------------------------------------------
     * Aggregate Analytics
     * -------------------------------------------------------------------------- */

    /**
     * Calculates comprehensive productivity metrics per user
     * @returns List of metric arrays: [username, count, pouches, avgTime, rate]
     * @note Uses JPQL for database agnostic implementation
     */
    @Query("SELECT p.user.username, " +
           "COUNT(p), " +
           "COALESCE(SUM(p.pouchesChecked), 0), " +
           "CASE WHEN SUM(p.pouchesChecked) > 0 " +
           "     THEN CAST(SUM(FUNCTION('TIMESTAMPDIFF', SECOND, p.startTime, p.endTime)) AS double) / SUM(p.pouchesChecked) " +
           "     ELSE 0.0 END, " +
           "CASE WHEN SUM(FUNCTION('TIMESTAMPDIFF', HOUR, p.startTime, p.endTime)) > 0 " +
           "     THEN CAST(SUM(p.pouchesChecked) AS double) / SUM(FUNCTION('TIMESTAMPDIFF', HOUR, p.startTime, p.endTime)) " +
           "     ELSE 0.0 END " +
           "FROM Pac p " +
           "GROUP BY p.user.username")
    List<Object[]> getUserProductivityData();

    /**
     * Retrieves daily pouch check counts for date range
     * @param startDate Range start (inclusive)
     * @param endDate Range end (inclusive)
     * @returns List of arrays: [date, count]
     */
    @Query("SELECT DATE(p.submissionDate) as date, COUNT(p) as count " +
           "FROM Pac p " +
           "WHERE p.submissionDate >= :startDate AND p.submissionDate <= :endDate " +
           "GROUP BY DATE(p.submissionDate) " +
           "ORDER BY DATE(p.submissionDate)")
    List<Object[]> getPouchesCheckedLast7Days(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate);

    /* -----------------------------------------------------------------------------
     * User Existence Checks
     * -------------------------------------------------------------------------- */

    /**
     * Checks if a user has any PAC entries
     * @param username Username to check
     * @returns True if user exists with PAC entries
     */
    @Query("SELECT COUNT(p) > 0 FROM Pac p WHERE p.user.username = :username")
    boolean existsByUsername(@Param("username") String username);

    /* -----------------------------------------------------------------------------
     * User Productivity Metrics
     * -------------------------------------------------------------------------- */

    /**
     * Retrieves detailed productivity metrics for a specific user
     * @param username Target username
     * @returns Array containing [totalSubmissions, totalPouchesChecked, totalSeconds]
     */
    @Query("""
        SELECT 
            COUNT(p) as totalSubmissions,
            COALESCE(SUM(p.pouchesChecked), 0) as totalPouchesChecked,
            COALESCE(SUM(FUNCTION('TIMESTAMPDIFF', SECOND, p.startTime, p.endTime)), 0) as totalSeconds
        FROM Pac p 
        WHERE p.user.username = :username
        """)
    Object[] getUserProductivityMetrics(@Param("username") String username);

    /* -----------------------------------------------------------------------------
     * TODO: Future Enhancements
     * -------------------------------------------------------------------------- */
    // - Add date range filtering capabilities
    // - Implement user-specific hourly metrics
    // - Add error rate tracking
    // - Consider caching frequently accessed metrics
}
