/* ==========================================================================
 * RequiresAuthentication Annotation
 * 
 * PURPOSE: Provides method-level authentication enforcement through AOP
 * DEPENDENCY: Requires AuthenticationAspect for runtime validation
 * SCOPE: Method-level only
 * ========================================================================== */

package com.demoproject.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authentication requirement marker for securing method access.
 * 
 * FUNCTIONALITY:
 * - Enforces user authentication via AOP interception
 * - Validates security context before method execution
 * - Fails fast if authentication requirements not met
 * IMPORTANT NOTES:
 * - Must be used with Spring Security configuration
 * - Does not replace URL security - use as additional layer
 * - No caching of authentication status (verified per-call)
 *
 * EDGE CASES:
 * - Handles null security contexts
 * - Rejects anonymous users
 * - Throws IllegalStateException for unauthenticated access
 *
 * @see com.demoproject.demo.aspect.AuthenticationAspect
 *
 * @todo [SECURITY] Add role-based authentication support
 * @todo [ERROR] Implement custom failure handling
 * @todo [PERF] Consider authentication result caching
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAuthentication {
    /* ---- Currently implemented as marker annotation ----
     * Future enhancements may include:
     * - Required roles/permissions
     * - Custom failure handlers
     * - Authentication strength requirements
     */
}