package com.demoproject.demo.repository;

import com.demoproject.demo.entity.Wellca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface WellcaRepository extends JpaRepository<Wellca, Long> {
    
    /**
     * Find entry by date
     * @param date The date to search for
     * @return Optional containing the entry if found
     */
    Optional<Wellca> findByDate(LocalDate date);

    /**
     * Find entries within a date range
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return List of entries within range
     */
    List<Wellca> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);

    /**
     * Get weekly aggregated data
     * @param startDate Beginning of the week
     * @param endDate End of the week
     * @return List of entries for the week
     */
    @Query("SELECT w FROM Wellca w WHERE w.date >= :startDate AND w.date <= :endDate ORDER BY w.date")
    List<Wellca> findWeeklyData(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Calculate monthly delivery counts
     * @param startDate Start of the month
     * @param endDate End of the month
     * @return List of entries with delivery counts
     */
    @Query("SELECT NEW map(w.date as date, " +
           "SUM(w.purolator) as purolatorCount, " +
           "SUM(w.fedex) as fedexCount, " +
           "SUM(w.oneCourier) as oneCourierCount, " +
           "SUM(w.goBolt) as goBoltCount) " +
           "FROM Wellca w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.date")
    List<java.util.Map<String, Object>> getMonthlyDeliveryCounts(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get service type statistics
     * @param startDate Beginning of period
     * @param endDate End of period
     * @return List of service type counts and costs
     */
    @Query("SELECT NEW map(w.serviceType as type, " +
           "COUNT(w) as count, " +
           "SUM(w.serviceCost) as totalCost) " +
           "FROM Wellca w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.serviceType")
    List<java.util.Map<String, Object>> getServiceTypeStats(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    /**
     * Check if an entry exists for a specific date
     * @param date Date to check
     * @return true if entry exists
     */
    boolean existsByDate(LocalDate date);

    /**
     * Find entries by service type and date range
     * @param serviceType Service type to search for
     * @param startDate Start of the date range
     * @param endDate End of the date range
     * @return List of entries within range
     */
    List<Wellca> findByServiceTypeAndDateBetweenOrderByDateDesc(
        String serviceType, 
        LocalDate startDate, 
        LocalDate endDate
    );

    /**
     * Get comprehensive monthly statistics for charts
     * @param startDate Start of the month
     * @param endDate End of the month
     * @return Monthly aggregated data for charts
     */
    @Query(value = "SELECT /*+ INDEX(w date_idx) */ " +
           "w.date as date, " +
           "SUM(w.purolator + w.fedex + w.oneCourier + w.goBolt) as totalDeliveries, " +
           "SUM(w.newRx + w.refill + w.reAuth) as totalRx, " +
           "COUNT(CASE WHEN w.serviceType IS NOT NULL THEN 1 END) as totalServices " +
           "FROM wellca_entries w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY w.date " +
           "ORDER BY w.date",
           nativeQuery = true)
    List<Map<String, Object>> getMonthlyChartStats(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get quarterly statistics for charts
     * @param year The year
     * @param quarter The quarter (1-4)
     * @return Quarterly aggregated data for charts
     */
    @Query(value = "SELECT /*+ INDEX(w date_idx) */ " +
           "MONTH(w.date) as month, " +
           "SUM(w.purolator + w.fedex + w.oneCourier + w.goBolt) as totalDeliveries, " +
           "SUM(w.newRx + w.refill + w.reAuth) as totalRx, " +
           "COUNT(CASE WHEN w.serviceType IS NOT NULL THEN 1 END) as totalServices " +
           "FROM wellca_entries w " +
           "WHERE YEAR(w.date) = :year " +
           "AND QUARTER(w.date) = :quarter " +
           "GROUP BY MONTH(w.date) " +
           "ORDER BY month",
           nativeQuery = true)
    List<Map<String, Object>> getQuarterlyChartStats(
        @Param("year") int year,
        @Param("quarter") int quarter
    );

    /**
     * Get weekly statistics for charts with progressive loading
     * @param startDate Start of the week
     * @param endDate End of the week
     * @return Weekly aggregated data for charts
     */
    @Query(value = "SELECT /*+ INDEX(w date_idx) */ " +
           "MAX(w.date) as weekEndDate, " +
           "SUM(w.purolator + w.fedex + w.oneCourier + w.goBolt) as totalDeliveries, " +
           "SUM(w.newRx + w.refill + w.reAuth) as totalRx, " +
           "COUNT(CASE WHEN w.serviceType IS NOT NULL THEN 1 END) as totalServices " +
           "FROM wellca_entries w " +
           "WHERE w.date BETWEEN :startDate AND :endDate " +
           "GROUP BY YEARWEEK(w.date) " +
           "ORDER BY weekEndDate",
           nativeQuery = true)
    List<Map<String, Object>> getWeeklyChartStats(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );
}
