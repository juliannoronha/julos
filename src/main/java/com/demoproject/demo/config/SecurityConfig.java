/* ==========================================================================
 * Security Configuration Module
 * 
 * PURPOSE: Centralizes Spring Security configuration and authentication logic
 * DEPENDENCIES: Spring Security, BCrypt, UserRepository, SLF4J
 * SCOPE: Application-wide security settings
 * ========================================================================== */

package com.demoproject.demo.config;

import com.demoproject.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.io.IOException;
import java.io.PrintWriter;

/* --------------------------------------------------------------------------
 * Core Security Configuration
 * 
 * FEATURES:
 * - BCrypt password hashing
 * - Role-based access control
 * - HTTPS channel security
 * - Custom login/logout handling
 * - Session management
 * 
 * SECURITY CONSIDERATIONS:
 * - All passwords hashed with BCrypt
 * - Session fixation protection
 * - HTTPS required for all requests
 * - Access denied handling
 * -------------------------------------------------------------------------- */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableAspectJAutoProxy
public class SecurityConfig {

    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /* .... Core Security Beans .... */
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            try {
                return userRepository.findByUsername(username)
                    .map(user -> User.withUsername(user.getUsername())
                                   .password(user.getPassword())
                                   .roles(user.getRole().name())
                                   .build())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            } catch (Exception e) {
                logger.error("Error during user authentication for username: {}", username, e);
                throw new UsernameNotFoundException("Authentication error", e);
            }
        };
    }

    /* .... HTTP Security Configuration .... */
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .requiresChannel(channel -> channel.anyRequest().requiresSecure())
            
            .authorizeHttpRequests(authz -> authz
                // Static resources and public pages
                .requestMatchers("/css/**", "/js/**", "/images/**", "/*.png", "/*.ico", "/h2-console/**").permitAll()
                .requestMatchers("/", "/login").permitAll()
                
                // Admin and moderator access
                .requestMatchers("/api/overall-productivity", "/view-responses", "/user-productivity", "/api/user-productivity/**").hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers("/api/checker/**").hasRole("CHECKER")
                .requestMatchers("/api/shipping/**").hasRole("SHIPPING")
                .requestMatchers("/api/inventory/**").hasRole("INVENTORY")
                .requestMatchers("/packmed", "/api/packmed/**").hasAnyRole("CHECKER", "MODERATOR", "ADMIN")
                .requestMatchers("/api/user-productivity-stream").hasAnyRole("ADMIN", "MODERATOR")
                
                // Audit log access - Admin only
                .requestMatchers("/audit/**", "/api/audit/**").hasRole("ADMIN")
                .requestMatchers("/api/audit/logs", "/api/audit/download").hasRole("ADMIN")
                .requestMatchers("/api/audit/search/**").hasRole("ADMIN")
                
                .requestMatchers(HttpMethod.POST, "/submit-questions").authenticated()
                .anyRequest().authenticated()
            )
            
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home", true)
                .failureHandler((request, response, exception) -> {
                    logger.warn("Failed login attempt: {}", exception.getMessage());
                    response.sendRedirect("/login?error");
                })
                .permitAll()
            )
            
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    try (PrintWriter writer = response.getWriter()) {
                        logger.warn("Access denied: {}", accessDeniedException.getMessage());
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        writer.write("{\"error\": \"ACCESS_DENIED\", \"message\": \"You do not have permission to access this resource\"}");
                        writer.flush();
                    } catch (IOException e) {
                        logger.error("Error writing access denied response", e);
                    }
                })
            )
            
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
                .maximumSessions(1)
                .expiredUrl("/login?expired")
            );

        return http.build();
    }

    /* .... Authentication Configuration .... */
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
    }

    /* @todo [SECURITY] Add rate limiting for login attempts
     * @todo [AUDIT] Implement security event logging
     * @todo [PERF] Consider caching authenticated user details
     * @todo [RESILIENCE] Add circuit breaker for auth failures
     */
}
