/* ==========================================================================
 * Authentication Aspect Module
 * 
 * PURPOSE: Provides AOP-based authentication verification for annotated methods
 * DEPENDENCIES: Spring Security, Micrometer, SLF4J
 * SCOPE: Method-level authentication enforcement
 * ========================================================================== */

package com.demoproject.demo.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* --------------------------------------------------------------------------
 * Core Authentication Aspect Implementation
 * Provides method-level authentication verification via AOP interception
 * -------------------------------------------------------------------------- */
@Aspect
@Component
public class AuthenticationAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationAspect.class);
    private final MeterRegistry meterRegistry;

    /* .... Constructor .... */
    /**
     * @param meterRegistry Metrics registry for tracking auth stats
     * @note Initializes aspect with required monitoring capabilities
     */
    public AuthenticationAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        logger.info("AuthenticationAspect initialized with metrics registry");
    }

    /* .... Authentication Verification .... */
    /**
     * Verifies user authentication before method execution
     * 
     * @throws IllegalStateException for unauthenticated access
     * 
     * FLOW:
     * 1. Start metrics timer
     * 2. Check security context
     * 3. Verify authentication status
     * 4. Record metrics
     * 
     * EDGE CASES:
     * - Null security context
     * - Anonymous users
     * - Context holder exceptions
     */
    @Before("@annotation(com.demoproject.demo.annotation.RequiresAuthentication)")
    public void checkAuthentication() {
        logger.debug("Starting authentication check");
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            logger.trace("Retrieved authentication context: {}", 
                auth != null ? auth.getName() : "null");
            
            if (auth == null || auth instanceof AnonymousAuthenticationToken) {
                meterRegistry.counter("auth.check.failures").increment();
                logger.warn("Authentication check failed - User not authenticated");
                throw new IllegalStateException("User is not authenticated");
            }
            
            meterRegistry.counter("auth.check.success").increment();
            logger.debug("Authentication check passed for user: {}", auth.getName());
            
        } catch (Exception e) {
            meterRegistry.counter("auth.check.errors", 
                "error", e.getClass().getSimpleName()).increment();
            logger.error("Authentication check error: {}", e.getMessage());
            throw e;
            
        } finally {
            sample.stop(meterRegistry.timer("auth.check.time"));
            logger.trace("Authentication check completed");
        }
    }

    /* TODO: [SECURITY] Add role-based authentication support
     * TODO: [ERROR] Implement custom failure handlers
     * TODO: [PERF] Add circuit breaker for repeated failures
     * TODO: [CACHE] Consider caching authentication results
     */
}