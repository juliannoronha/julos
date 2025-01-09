package com.demoproject.demo.services;

import com.demoproject.demo.entity.Wellca;
import com.demoproject.demo.repository.WellcaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@CacheConfig(cacheNames = {"wellcaData"})
public class WellcaService {
    private static final Logger logger = LoggerFactory.getLogger(WellcaService.class);
    private final WellcaRepository wellcaRepository;
    private final CacheManager cacheManager;

    public WellcaService(WellcaRepository wellcaRepository, CacheManager cacheManager) {
        this.wellcaRepository = wellcaRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Save or update a Wellca entry
     * @param wellca The entry to save
     * @return The saved entry
     */
    @Transactional
    @CacheEvict(value = {"wellcaRangeData", "wellcaData", "serviceDetails"}, allEntries = true)
    public Wellca saveEntry(Wellca wellca) {
        logger.debug("Saving Wellca entry for date: {} with service type: {} and cost: {}", 
            wellca.getDate(), wellca.getServiceType(), wellca.getServiceCost());
        
        if (wellca.getServiceType() != null) {
            logger.debug("Service details - Patient: {}, Pharmacist: {}", 
                wellca.getPatientName(), wellca.getPharmacistName());
        }
        
        if (wellcaRepository.existsByDate(wellca.getDate())) {
            logger.info("Updating existing entry for date: {}", wellca.getDate());
        }
        
        Wellca savedEntry = wellcaRepository.save(wellca);
        logger.debug("Successfully saved entry with ID: {}. Service type: {}, cost: {}", 
            savedEntry.getId(), savedEntry.getServiceType(), savedEntry.getServiceCost());
        
        // Clear all related caches
        clearCaches();
        
        return savedEntry;
    }

    private void clearCaches() {
        logger.debug("Clearing all Wellca related caches");
        cacheManager.getCacheNames().stream()
            .filter(name -> name.startsWith("wellca"))
            .forEach(cacheName -> {
                logger.debug("Clearing cache: {}", cacheName);
                cacheManager.getCache(cacheName).clear();
            });
    }

    /**
     * Retrieve entry by date
     * @param date The date to search for
     * @return Optional containing the entry if found
     */
    @Cacheable(value = "wellcaData", key = "#date")
    public Optional<Wellca> getEntryByDate(LocalDate date) {
        logger.debug("Fetching Wellca entry for date: {}", date);
        Optional<Wellca> entry = wellcaRepository.findByDate(date);
        entry.ifPresent(e -> logger.debug("Found entry with service type: {} and cost: {}", 
            e.getServiceType(), e.getServiceCost()));
        return entry;
    }

    /**
     * Get entries within a date range
     * @param startDate Start of the range
     * @param endDate End of the range
     * @return List of entries
     */
    @Cacheable(value = "wellcaRangeData", key = "#startDate.toString() + '-' + #endDate.toString()")
    public List<Wellca> getEntriesInRange(LocalDate startDate, LocalDate endDate) {
        logger.debug("Fetching Wellca entries between {} and {}", startDate, endDate);
        List<Wellca> entries = wellcaRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);
        logger.debug("Found {} entries in date range", entries.size());
        entries.forEach(e -> logger.debug("Entry date: {}, service type: {}, cost: {}", 
            e.getDate(), e.getServiceType(), e.getServiceCost()));
        return entries;
    }

    /**
     * Get weekly statistics
     * @param weekStartDate First day of the week
     * @return Weekly aggregated data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWeeklyStats(LocalDate weekStartDate) {
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        List<Wellca> weeklyData = wellcaRepository.findWeeklyData(weekStartDate, weekEndDate);
        logger.debug("Fetching weekly stats for period {} to {}", weekStartDate, weekEndDate);
        return calculateWeeklyStats(weeklyData);
    }

    /**
     * Get service type statistics
     * @param startDate Start of period
     * @param endDate End of period
     * @return Service statistics
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getServiceTypeStats(LocalDate startDate, LocalDate endDate) {
        logger.debug("Fetching service type stats for period {} to {}", startDate, endDate);
        return wellcaRepository.getServiceTypeStats(startDate, endDate);
    }

    /**
     * Calculate weekly statistics from data
     * @param weeklyData List of entries for the week
     * @return Map of calculated statistics
     */
    private Map<String, Object> calculateWeeklyStats(List<Wellca> weeklyData) {
        double avgProfilesEntered = weeklyData.stream()
            .mapToInt(Wellca::getProfilesEntered)
            .average()
            .orElse(0.0);

        long totalRx = weeklyData.stream()
            .mapToInt(Wellca::getTotalFilled)
            .sum();

        Map<String, Object> stats = Map.of(
            "averageProfilesEntered", avgProfilesEntered,
            "totalRxFilled", totalRx,
            "entriesCount", weeklyData.size()
        );
        
        logger.debug("Calculated weekly stats: {}", stats);
        return stats;
    }

    /**
     * Delete an entry
     * @param id Entry ID to delete
     */
    @Transactional
    @CacheEvict(value = "wellcaData", allEntries = true)
    public void deleteEntry(Long id) {
        logger.debug("Deleting Wellca entry with ID: {}", id);
        wellcaRepository.deleteById(id);
    }

    /**
     * Get monthly delivery counts
     * @param yearMonth Year and month for the report
     * @return Monthly delivery statistics
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthlyDeliveryCounts(LocalDate yearMonth) {
        LocalDate startDate = yearMonth.withDayOfMonth(1);
        LocalDate endDate = yearMonth.withDayOfMonth(yearMonth.lengthOfMonth());
        
        logger.debug("Fetching monthly delivery counts for period: {} to {}", startDate, endDate);
        return wellcaRepository.getMonthlyDeliveryCounts(startDate, endDate);
    }

    /**
     * Get detailed service entries by type and date range
     */
    @Cacheable(value = "serviceDetails", key = "#serviceType + '-' + #startDate + '-' + #endDate")
    public List<Wellca> getServiceDetailsByType(String serviceType, LocalDate startDate, LocalDate endDate) {
        logger.debug("Fetching service details for type: {} between {} and {}", 
            serviceType, startDate, endDate);
        
        List<Wellca> entries = wellcaRepository.findByServiceTypeAndDateBetweenOrderByDateDesc(
            serviceType, startDate, endDate);
        
        logger.debug("Found {} entries for service type: {}", entries.size(), serviceType);
        return entries;
    }

    /**
     * Get monthly service statistics
     */
    @Cacheable(value = "serviceDetails", key = "'monthly-' + #yearMonth")
    public Map<String, Object> getMonthlyServiceStats(LocalDate yearMonth) {
        LocalDate startDate = yearMonth.withDayOfMonth(1);
        LocalDate endDate = yearMonth.withDayOfMonth(yearMonth.lengthOfMonth());
        
        List<Wellca> monthlyServices = wellcaRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);
        
        Map<String, Object> stats = calculateMonthlyServiceStats(monthlyServices);
        logger.debug("Calculated monthly service stats: {}", stats);
        
        return stats;
    }

    private Map<String, Object> calculateMonthlyServiceStats(List<Wellca> services) {
        Map<String, Integer> serviceCounts = new HashMap<>();
        Map<String, BigDecimal> serviceRevenue = new HashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Wellca service : services) {
            if (service.getServiceType() != null && service.getServiceCost() != null) {
                serviceCounts.merge(service.getServiceType(), 1, Integer::sum);
                serviceRevenue.merge(service.getServiceType(), 
                    service.getServiceCost(), BigDecimal::add);
                totalRevenue = totalRevenue.add(service.getServiceCost());
            }
        }

        return Map.of(
            "serviceCounts", serviceCounts,
            "serviceRevenue", serviceRevenue,
            "totalRevenue", totalRevenue,
            "totalServices", services.size()
        );
    }

    

    /**
     * Get monthly statistics for chart display
     * @param yearMonth The month to get statistics for
     * @return Map containing chart data and statistics
     */
    @Cacheable(value = "chartData", key = "'monthly-' + #yearMonth")
    public Map<String, Object> getMonthlyChartStats(LocalDate yearMonth) {
        LocalDate startDate = yearMonth.withDayOfMonth(1);
        LocalDate endDate = yearMonth.withDayOfMonth(yearMonth.lengthOfMonth());
        
        logger.debug("Fetching monthly chart stats for period {} to {}", startDate, endDate);
        
        List<Map<String, Object>> monthlyStats = wellcaRepository.getMonthlyChartStats(startDate, endDate);
        
        // Prepare chart data structure
        List<String> labels = new ArrayList<>();
        List<Number> rxCounts = new ArrayList<>();
        List<Number> deliveryCounts = new ArrayList<>();
        List<Number> rxPerDelivery = new ArrayList<>();
        List<Number> servicesCounts = new ArrayList<>();
        
        monthlyStats.forEach(stat -> {
            LocalDate date = (LocalDate) stat.get("date");
            labels.add(date.format(DateTimeFormatter.ofPattern("MMM dd")));
            
            Long totalRx = (Long) stat.get("totalRx");
            Long totalDeliveries = (Long) stat.get("totalDeliveries");
            Long totalServices = (Long) stat.get("professionalServices");
            
            rxCounts.add(totalRx);
            deliveryCounts.add(totalDeliveries);
            rxPerDelivery.add(totalDeliveries > 0 ? 
                (double) totalRx / totalDeliveries : 0);
            servicesCounts.add(totalServices);
        });
        
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Map.of(
            "rxCount", rxCounts,
            "deliveries", deliveryCounts,
            "rxPerDelivery", rxPerDelivery,
            "services", servicesCounts
        ));
        
        logger.debug("Generated chart data with {} data points", labels.size());
        return chartData;
    }

    /**
     * Get quarterly statistics for chart display
     * @param year The year to get statistics for
     * @param quarter The quarter (1-4)
     * @return Map containing chart data and statistics
     */
    @Cacheable(value = "chartData", key = "'quarterly-' + #year + '-Q' + #quarter")
    public Map<String, Object> getQuarterlyChartStats(int year, int quarter) {
        logger.debug("Fetching quarterly chart stats for Q{} {}", quarter, year);
        
        if (quarter < 1 || quarter > 4) {
            throw new IllegalArgumentException("Quarter must be between 1 and 4");
        }
        
        List<Map<String, Object>> quarterlyStats = wellcaRepository.getQuarterlyChartStats(year, quarter);
        
        // Prepare chart data structure
        List<String> labels = new ArrayList<>();
        List<Number> rxCounts = new ArrayList<>();
        List<Number> deliveryCounts = new ArrayList<>();
        List<Number> rxPerDelivery = new ArrayList<>();
        List<Number> servicesCounts = new ArrayList<>();
        
        // Get month names for the quarter
        String[] monthNames = getMonthsForQuarter(quarter);
        
        // Initialize data for all three months of the quarter
        for (String month : monthNames) {
            labels.add(month + " " + year);
        }
        
        // Map the data to the correct months
        quarterlyStats.forEach(stat -> {
            int monthIndex = ((Integer) stat.get("month") - 1) % 3;
            
            Long totalRx = (Long) stat.get("totalRx");
            Long totalDeliveries = (Long) stat.get("totalDeliveries");
            Long totalServices = (Long) stat.get("totalServices");
            
            rxCounts.add(monthIndex, totalRx);
            deliveryCounts.add(monthIndex, totalDeliveries);
            rxPerDelivery.add(monthIndex, totalDeliveries > 0 ? 
                (double) totalRx / totalDeliveries : 0);
            servicesCounts.add(monthIndex, totalServices);
        });
        
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Map.of(
            "rxCount", rxCounts,
            "deliveries", deliveryCounts,
            "rxPerDelivery", rxPerDelivery,
            "services", servicesCounts
        ));
        
        logger.debug("Generated quarterly chart data with {} data points", labels.size());
        return chartData;
    }
    
    /**
     * Helper method to get month names for a quarter
     */
    private String[] getMonthsForQuarter(int quarter) {
        String[] allMonths = {"January", "February", "March", "April", "May", "June", 
                             "July", "August", "September", "October", "November", "December"};
        int startMonth = (quarter - 1) * 3;
        return new String[]{
            allMonths[startMonth],
            allMonths[startMonth + 1],
            allMonths[startMonth + 2]
        };
    }

    /**
     * Get weekly statistics for charts with progressive loading
     * @param startDate Start of the period
     * @param endDate End of the period
     * @param page Page number (1-based) for progressive loading
     * @return Map containing chart data and statistics
     */
    @Cacheable(value = "chartData", key = "'weekly-' + #startDate + '-' + #endDate + '-page-' + #page")
    public Map<String, Object> getWeeklyChartStats(LocalDate startDate, LocalDate endDate, int page) {
        logger.debug("Fetching weekly chart stats for period {} to {}, page {}", startDate, endDate, page);
        
        final int WEEKS_PER_PAGE = 4;
        LocalDate adjustedStartDate = startDate.plusWeeks((page - 1) * WEEKS_PER_PAGE);
        LocalDate adjustedEndDate = adjustedStartDate.plusWeeks(WEEKS_PER_PAGE);
        
        // Ensure we don't exceed the original end date
        if (adjustedEndDate.isAfter(endDate)) {
            adjustedEndDate = endDate;
        }
        
        List<Map<String, Object>> weeklyStats = wellcaRepository.getWeeklyChartStats(
            adjustedStartDate, 
            adjustedEndDate
        );
        
        // Calculate total number of weeks for pagination
        long totalWeeks = ChronoUnit.WEEKS.between(startDate, endDate);
        int totalPages = (int) Math.ceil((double) totalWeeks / WEEKS_PER_PAGE);
        
        // Prepare chart data structure
        List<String> labels = new ArrayList<>();
        List<Number> rxCounts = new ArrayList<>();
        List<Number> deliveryCounts = new ArrayList<>();
        List<Number> rxPerDelivery = new ArrayList<>();
        List<Number> servicesCounts = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        
        weeklyStats.forEach(stat -> {
            LocalDate weekEnd = (LocalDate) stat.get("weekEndDate");
            labels.add(weekEnd.format(formatter));
            
            Long totalRx = (Long) stat.get("totalRx");
            Long totalDeliveries = (Long) stat.get("totalDeliveries");
            Long totalServices = (Long) stat.get("totalServices");
            
            rxCounts.add(totalRx);
            deliveryCounts.add(totalDeliveries);
            rxPerDelivery.add(totalDeliveries > 0 ? 
                (double) totalRx / totalDeliveries : 0);
            servicesCounts.add(totalServices);
        });
        
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("labels", labels);
        chartData.put("datasets", Map.of(
            "rxCount", rxCounts,
            "deliveries", deliveryCounts,
            "rxPerDelivery", rxPerDelivery,
            "services", servicesCounts
        ));
        
        // Add pagination metadata
        chartData.put("pagination", Map.of(
            "currentPage", page,
            "totalPages", totalPages,
            "hasMore", page < totalPages
        ));
        
        logger.debug("Generated weekly chart data with {} data points for page {}", 
            labels.size(), page);
        
        return chartData;
    }
    
    /**
     * Get total number of weeks between dates
     * @param startDate Start of period
     * @param endDate End of period
     * @return Total number of pages for weekly data
     */
    public int getTotalWeeklyPages(LocalDate startDate, LocalDate endDate) {
        final int WEEKS_PER_PAGE = 4;
        long totalWeeks = ChronoUnit.WEEKS.between(startDate, endDate);
        return (int) Math.ceil((double) totalWeeks / WEEKS_PER_PAGE);
    }
}
