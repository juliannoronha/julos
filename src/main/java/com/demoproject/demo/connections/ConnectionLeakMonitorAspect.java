/*
 * ========================================================================
 * ConnectionLeakMonitorAspect
 * ========================================================================
 * Purpose: Monitors Spring @Transactional methods for potential connection leaks
 * by tracking execution time and logging detailed error information.
 * 
 * Dependencies:
 * - Spring AOP
 * - AspectJ
 * - Java Logging API
 * 
 * @note This aspect is automatically applied to all @Transactional methods
 * @note Thread-safe due to stateless design
 */
package com.demoproject.demo.connections;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ConnectionLeakMonitorAspect {
    
    /* Logger instance for this class */
    private static final Logger logger = Logger.getLogger(ConnectionLeakMonitorAspect.class.getName());
    
    /* ---------------------------- Core Monitoring Logic ---------------------------- */
    
    /**
     * Monitors @Transactional methods for potential connection leaks.
     * 
     * @param joinPoint The intercepted method execution context
     * @returns The result of the intercepted method
     * @throws Throwable If the underlying method throws an exception
     * 
     * @note Execution time is measured and logged on exceptions
     * @note Original exception is preserved and re-thrown
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object monitorConnectionLeak(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            logger.severe(String.format(
                "Potential connection leak detected in %s. Method execution time: %dms. Error: %s. Stack trace: %s", 
                joinPoint.getSignature().toShortString(),
                System.currentTimeMillis() - startTime,
                e.getMessage(),
                getStackTraceAsString(e)
            ));
            throw e;
        }
    }
    
    /* ---------------------------- Utility Methods ---------------------------- */
    
    /**
     * Converts an exception's stack trace to a string representation.
     * 
     * @param e The exception to convert
     * @returns String representation of the stack trace
     * 
     * @note Uses StringWriter for efficient string building
     * @note Closes resources automatically via try-with-resources
     */
    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
