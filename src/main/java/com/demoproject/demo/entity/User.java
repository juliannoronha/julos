/* ==========================================================================
 * User Entity Module
 * 
 * PURPOSE: Core domain entity representing system users and their permissions
 * DEPENDENCIES: JPA, Lombok, Custom Entity Relationships
 * SCOPE: Primary user management and authentication
 * ========================================================================== */

package com.demoproject.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.ArrayList;

/* --------------------------------------------------------------------------
 * Core User Entity Definition
 * 
 * FUNCTIONALITY:
 * - Manages user identity and authentication
 * - Handles role-based access control
 * - Maintains relationships with user activities
 * 
 * IMPORTANT NOTES:
 * - Uses "users" table to avoid SQL keyword conflicts
 * - Implements cascading deletion of child entities
 * - Requires unique usernames
 * -------------------------------------------------------------------------- */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /* .... Core Identifiers .... */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    /* .... Access Control .... */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /* .... Entity Relationships .... */
    /**
     * @note Cascading delete of associated answers
     * @note Orphan removal ensures cleanup
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAnswer> userAnswers;


    /* Temporarily Disabled */
    /**
     * @note Initialized to prevent NPE
     * @note Maintains PAC session history
     */
    // @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Pac> pacs = new ArrayList<>();

    
    /* .... Role Enumeration .... */
    /**
     * System roles defining user permissions
     * 
     * HIERARCHY (highest to lowest):
     * - ADMIN: Full system access
     * - MODERATOR: User management
     * - CHECKER: PAC operations
     * - SHIPPING: Shipping operations
     * - INVENTORY: Stock management
     * - USER: Basic access
     */
    public enum Role {
        ADMIN, MODERATOR, USER, CHECKER, SHIPPING, INVENTORY
    }

    /* .... Constructors .... */
    /**
     * Creates new user with basic attributes
     * 
     * @param username Unique identifier (case-sensitive)
     * @param password Unhashed password (encrypt before storage)
     * @param role Case-insensitive role name
     * @throws IllegalArgumentException for invalid role
     */
    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = Role.valueOf(role.toUpperCase());
    }

    /* .... Role Management .... */
    /**
     * @returns Current user role
     */
    public Role getRole() {
        return this.role;
    }

    /**
     * @param role New role to assign
     * @note Consider adding role transition validation
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /* @todo [SECURITY] Add password hashing and validation
     * @todo [AUDIT] Implement user activity tracking
     * @todo [PERF] Consider lazy loading for large collections
     * @todo [VALIDATION] Add username format constraints
     */
}