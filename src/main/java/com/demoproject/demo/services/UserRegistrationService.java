/* =============================================================================
 * User Registration Service
 * =============================================================================
 * PURPOSE: Manages secure user registration and account creation
 * 
 * CORE FUNCTIONALITY:
 * - New user registration with validation
 * - Password encoding and security
 * - Role management and conversion
 * - Duplicate prevention
 * 
 * DEPENDENCIES:
 * - Spring Framework (Service, Transactional)
 * - Spring Security (PasswordEncoder)
 * - UserRepository for persistence
 * - User entity and DTO
 * 
 * SECURITY NOTES:
 * - Passwords are cryptographically hashed
 * - Input validation prevents injection
 * - Role conversion is strictly controlled
 * ============================================================================= */
package com.demoproject.demo.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.demoproject.demo.dto.UserDTO;
import com.demoproject.demo.entity.User;
import com.demoproject.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserRegistrationService {

    /* --------------------------------------------------------------------------
     * Service Dependencies
     * -------------------------------------------------------------------------- */
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private static final Logger logger = LoggerFactory.getLogger(UserRegistrationService.class);

    /**
     * Initializes registration service with required components.
     * 
     * @param userRepository Data access for user operations
     * @param passwordEncoder Security component for password hashing
     * @param auditLogService Service for audit logging
     * @note Both dependencies must be non-null
     */
    public UserRegistrationService(UserRepository userRepository, 
                                 PasswordEncoder passwordEncoder,
                                 AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /* --------------------------------------------------------------------------
     * Registration Operations
     * -------------------------------------------------------------------------- */

    /**
     * Processes new user registration with validation.
     * 
     * @param userDTO Registration data transfer object
     * @returns Created user entity
     * @throws IllegalArgumentException Username exists or invalid input
     * @security Ensures password hashing
     * @performance Single transaction scope
     */
    @Transactional
    public User registerNewUser(UserDTO userDTO) {
        logger.debug("Registering new user: {}", userDTO.getUsername());
        try {
            if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
                logger.warn("Username already exists: {}", userDTO.getUsername());
                throw new IllegalArgumentException("Username already exists");
            }

            validateUserInput(userDTO);

            // Create and configure new user
            User newUser = new User();
            newUser.setUsername(userDTO.getUsername());
            newUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            newUser.setRole(convertStringToRole(userDTO.getRole()));

            User savedUser = userRepository.save(newUser);
            logger.info("Successfully registered new user: {}", userDTO.getUsername());
            
            // Add audit log entry
            auditLogService.logEvent(
                "USER_REGISTRATION",
                "USER",
                "New user registered: " + userDTO.getUsername() + " with role " + userDTO.getRole()
            );

            return savedUser;
        } catch (Exception e) {
            logger.error("Failed to register user: {}", userDTO.getUsername(), e);
            throw new RuntimeException("Failed to register user", e);
        }
    }

    /* --------------------------------------------------------------------------
     * Validation Operations
     * -------------------------------------------------------------------------- */

    /**
     * Validates registration input completeness.
     * 
     * @param userDTO Input data to validate
     * @throws IllegalArgumentException Missing or invalid fields
     * @note Required: username, password, role
     */
    private void validateUserInput(UserDTO userDTO) {
        if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (userDTO.getRole() == null || userDTO.getRole().trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be empty");
        }

        /* @todo Enhanced validation needs:
         * - Password strength requirements
         * - Username format rules
         * - Email validation if added
         * - Input sanitization
         */
    }

    /**
     * Converts role string to enum with validation.
     * 
     * @param roleString Role identifier to convert
     * @returns Validated Role enum value
     * @throws IllegalArgumentException Invalid role specified
     * @security Ensures role value integrity
     */
    private User.Role convertStringToRole(String roleString) {
        try {
            return User.Role.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleString);
        }
    }

    /* --------------------------------------------------------------------------
     * Future Enhancements
     * -------------------------------------------------------------------------- */

    /* @todo Implementation needs:
     * - Email verification flow
     * - Password strength validation
     * - Rate limiting for registrations
     * - Audit logging
     * - CAPTCHA integration
     */
}
