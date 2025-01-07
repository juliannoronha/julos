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

@Configuration
@EnableCaching
public class CacheConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheConfig.class);
    
    @Bean
    public CacheManager cacheManager() {
        logger.info("Initializing Cache Manager");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure default cache settings
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(100)
            .recordStats());
        
        // Define specific caches
        cacheManager.setCacheNames(Arrays.asList(
            "allUserProductivity",
            "userProductivity",
            "overallProductivity",
            "wellcaData",
            "wellcaRangeData",
            "serviceDetails"
        ));
        
        logger.info("Cache Manager initialized with caches: {}", cacheManager.getCacheNames());
        return cacheManager;
    }
    
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        logger.debug("Configuring Caffeine cache specifications");
        return Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .initialCapacity(10)
            .maximumSize(100)
            .recordStats();
    }

    // Optional: Add cache event listener for debugging
    @Bean
    public CacheEventLogger cacheEventLogger() {
        return new CacheEventLogger();
    }
}

// Cache event listener for debugging
class CacheEventLogger {
    private static final Logger logger = LoggerFactory.getLogger(CacheEventLogger.class);

    public void onEvent(Object key, Object value, String eventType) {
        logger.debug("Cache event: type={}, key={}, value={}", eventType, key, value);
    }
}
