package com.demoproject.demo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/* ==========================================================================
 * UserDTO - Data Transfer Object for User Management
 * 
 * PURPOSE: Facilitates secure transfer of user data between application layers
 * DEPENDENCIES: jakarta.validation, lombok
 * USAGE: User registration, authentication, and profile management
 * 
 * @note This DTO excludes sensitive data from entity exposure
 * @note All fields have validation constraints for security
 * ========================================================================== */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    /* ------------------------------------------------------------------------
     * Core User Identifiers
     * ------------------------------------------------------------------------ */
    
    /**
     * @param username Unique identifier for the user
     * @validation NotEmpty, length 3-50 chars
     * @example "john.doe"
     * 
     * @note Username uniqueness should be validated at service layer
     * @note Special characters handling depends on business rules
     */
    @NotEmpty(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /**
     * @param password User's authentication credential
     * @validation NotEmpty, length 6-100 chars
     * 
     * @note Should be encrypted before storage
     * @note Consider adding pattern validation for password strength
     * @todo Add password complexity requirements
     */
    @NotEmpty(message = "Password cannot be empty")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    /**
     * @param role User's system role for authorization
     * @validation NotEmpty
     * @example "ROLE_USER", "ROLE_ADMIN"
     * 
     * @note Role should match predefined system roles
     * @note Consider using enum instead of String
     */
    @NotEmpty(message = "Role cannot be empty")
    private String role;
}