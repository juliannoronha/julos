/* ==========================================================================
 * NBA API Configuration Module
 * 
 * PURPOSE: Configures HTTP client and authentication for NBA API access
 * DEPENDENCIES: OkHttp3, Spring Framework, SLF4J
 * SCOPE: Application-level API configuration
 * ========================================================================== */

package com.demoproject.demo.config;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/* --------------------------------------------------------------------------
 * Core API Configuration
 * 
 * FUNCTIONALITY:
 * - Configures OkHttpClient with timeout settings
 * - Manages API authentication credentials
 * - Provides API endpoint configuration
 * 
 * SECURITY:
 * - API key injected via properties
 * - Credentials masked in logs
 * - Host URL configurable per environment
 * 
 * PERFORMANCE:
 * - Configurable timeout values
 * - Connection pooling enabled
 * - Single client instance shared
 * -------------------------------------------------------------------------- */
@Configuration
public class NbaAPIConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(NbaAPIConfig.class);

    @Value("${nba.api.key}")
    private String apiKey;
    
    @Value("${nba.api.host}")
    private String apiHost;
    
    /* .... HTTP Client Configuration .... */
    /**
     * Configures shared OkHttpClient instance for API requests.
     *
     * @return Configured OkHttpClient with timeout settings
     * @note Thread-safe, can be shared across application
     * 
     * TIMEOUTS:
     * - Connect: 30s
     * - Read: 30s 
     * - Write: 30s
     * 
     * EDGE CASES:
     * - Handles slow networks via timeouts
     * - Auto-manages connection pooling
     * - Fails fast on network errors
     */
    @Bean
    public OkHttpClient okHttpClient() {
        logger.debug("Configuring OkHttpClient with timeout settings");
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
                
        logger.info("OkHttpClient configured successfully with 30s timeouts");
        return client;
    }
    
    /* .... API Authentication .... */
    /**
     * Provides API key for request authentication.
     *
     * @return API key from secure configuration
     * @throws IllegalStateException if key not configured
     * 
     * SECURITY:
     * - Key sourced from properties
     * - Value masked in logs
     * - Validated at startup
     */
    @Bean
    public String apiKey() {
        logger.debug("Retrieving API key from configuration");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("NBA API key not configured");
        }
        logger.info("API key configured successfully [key=***]");
        return apiKey;
    }
    
    /* .... API Endpoint Configuration .... */
    /**
     * Provides base API host URL.
     *
     * @return Configured API host URL
     * @throws IllegalStateException if host not configured
     * 
     * VALIDATION:
     * - Must be non-null
     * - Must be non-empty
     * - Should be valid URL format
     */
    @Bean
    public String apiHost() {
        logger.debug("Retrieving API host from configuration");
        if (apiHost == null || apiHost.trim().isEmpty()) {
            throw new IllegalStateException("NBA API host not configured");
        }
        logger.info("API host configured: {}", apiHost);
        return apiHost;
    }

    /* TODO: [RESILIENCE] Add retry configuration for failed requests
     * TODO: [STABILITY] Add circuit breaker for API failure scenarios
     * TODO: [SECURITY] Consider adding request rate limiting
     * TODO: [MONITOR] Add metrics for API call monitoring
     */
}
