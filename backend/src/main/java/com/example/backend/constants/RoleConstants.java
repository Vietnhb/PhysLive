package com.example.backend.constants;

/**
 * Role name constants for B2B system.
 * 
 * Platform roles (school_id = NULL):
 * - ADMIN: System administrator, manages all schools and platform settings
 * - MANAGER: Operates schools and platform workflows
 * - REVIEWER: Reviews shared simulations and manages content quality
 * 
 * School roles (school_id = UUID):
 * - SCHOOL: Manages one school (1 per school), creates classes, assigns staff
 * - STAFF: Creates simulations, teaches classes, assigns work
 * - STUDENT: Takes classes, submits assignments
 */
public final class RoleConstants {
    
    // Platform roles
    public static final String ADMIN = "ADMIN";
    public static final String MANAGER = "MANAGER";
    public static final String REVIEWER = "REVIEWER";
    
    // School roles
    public static final String SCHOOL = "SCHOOL";
    public static final String STAFF = "STAFF";
    public static final String STUDENT = "STUDENT";
    
    // Spring Security role prefix
    public static final String ROLE_PREFIX = "ROLE_";
    
    private RoleConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Check if role is platform-level (school_id must be NULL).
     */
    public static boolean isPlatformRole(String roleName) {
        return ADMIN.equals(roleName) || MANAGER.equals(roleName) || REVIEWER.equals(roleName);
    }
    
    /**
     * Check if role is school-level (school_id must be NOT NULL).
     */
    public static boolean isSchoolRole(String roleName) {
        return SCHOOL.equals(roleName) || STAFF.equals(roleName) || STUDENT.equals(roleName);
    }
    
    /**
     * Get role name with Spring Security prefix.
     */
    public static String withPrefix(String roleName) {
        return ROLE_PREFIX + roleName;
    }
}
