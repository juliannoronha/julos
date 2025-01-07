package com.demoproject.demo.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

@Configuration
@EnableCaching
public class CacheConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);
    
    // Cache configuration constants
    private static final int DEFAULT_EXPIRE_MINUTES = 30;
    private static final int DEFAULT_MAX_SIZE = 100;
    private static final int DEFAULT_INITIAL_CAPACITY = 10;
    
    // Cache timeouts
    private static final int RANGE_DATA_EXPIRE_MINUTES = 15;
    private static final int SERVICE_DETAILS_EXPIRE_MINUTES = 60;
    private static final int USER_PRODUCTIVITY_EXPIRE_MINUTES = 20;
    
    @Bean
    public CacheManager cacheManager() {
        logger.info("Initializing Cache Manager");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Define only the cache names that are actually used
        cacheManager.setCacheNames(Arrays.asList(
            "allUserProductivity",
            "userProductivity",
            "overallProductivity",
            "wellcaData",
            "wellcaRangeData",
            "serviceDetails"
        ));
        
        // Configure caches with optimized specifications
        cacheManager.registerCustomCache("wellcaRangeData", 
            Caffeine.newBuilder()
                .expireAfterWrite(RANGE_DATA_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .maximumSize(50)  // Smaller size as range queries are specific
                .recordStats()
                .build());
            
        cacheManager.registerCustomCache("serviceDetails", 
            Caffeine.newBuilder()
                .expireAfterWrite(SERVICE_DETAILS_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .maximumSize(200) // Larger size for service details
                .recordStats()
                .build());
            
        cacheManager.registerCustomCache("userProductivity", 
            Caffeine.newBuilder()
                .expireAfterAccess(USER_PRODUCTIVITY_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .maximumSize(100) // Medium size for user data
                .recordStats()
                .build());
        
        // Set default cache specification for unnamed caches
        cacheManager.setCaffeine(getDefaultCacheBuilder());
        
        logger.info("Cache Manager initialized with custom specifications");
        return cacheManager;
    }
    
    private Caffeine<Object, Object> getDefaultCacheBuilder() {
        return Caffeine.newBuilder()
            .expireAfterWrite(DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES)
            .initialCapacity(DEFAULT_INITIAL_CAPACITY)
            .maximumSize(DEFAULT_MAX_SIZE)
            .recordStats();
    }
    
    @Bean
    public CacheMetricsCollector cacheMetricsCollector(CacheManager cacheManager) {
        return new CacheMetricsCollector(cacheManager);
    }
}

@Component
class CacheMetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(CacheMetricsCollector.class);
    private final CacheManager cacheManager;
    
    public CacheMetricsCollector(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void reportCacheMetrics() {
        if (cacheManager instanceof CaffeineCacheManager) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache instanceof CaffeineCache) {
                    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = 
                        ((CaffeineCache) cache).getNativeCache();
                    CacheStats stats = nativeCache.stats();
                    
                    logger.info("Cache '{}' statistics: hits={}, misses={}, evictions={}", 
                        cacheName,
                        stats.hitCount(),
                        stats.missCount(),
                        stats.evictionCount());
                }
            });
        }
    }
}
