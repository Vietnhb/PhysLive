package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.example.backend.entity.enums.PermissionCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @ManyToOne
    @JoinColumn(name = "school_id")
    private com.example.backend.entity.school.School school;

    private java.time.Instant deactivatedAt;
    private Integer deactivatedBy;
    private String deactivationReason;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    /**
     * Primary account-state flag (ADMIN-managed).
     * true = account active, false = account disabled.
     * columnDefinition keeps DEFAULT TRUE so ALTER TABLE works on existing rows.
     */
    @Column(nullable = false, columnDefinition = "boolean NOT NULL DEFAULT TRUE")
    private boolean enabled = true;

    /**
     * Legacy soft-delete / visibility flag (nullable).
     * Defaults to TRUE so newly registered users are active by default.
     */
    @Column
    private Boolean active = true;

    /**
     * School / institution the user belongs to (nullable).
     * Used by ADMIN when assigning teachers to organisations.
     */
    @Column(name = "institution_id")
    private String institutionId;

    /**
     * UTC timestamp of the most recent successful login.
     * Updated by AuthService on every successful authentication.
     */
    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    /**
     * URL of the user's avatar image (nullable).
     */
    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    /**
     * Creation timestamp (auto-set on insert).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Last modification timestamp (auto-updated on modification).
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** Individual capabilities for STAFF accounts, persisted through user_permissions. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private Set<PermissionGrant> permissions = new LinkedHashSet<>();

    public boolean hasPermission(PermissionCode code) {
        return hasPermission(code.name());
    }

    public boolean hasPermission(String code) {
        return permissions.stream().anyMatch(grant -> code.equals(grant.code())
                && grant.getPermission().getRole() != null && role != null
                && role.getName().equals(grant.getPermission().getRole().getName()));
    }

    public List<String> permissionCodes() {
        return Arrays.stream(PermissionCode.values()).filter(this::hasPermission).map(Enum::name).toList();
    }

    public void grantPermission(Permission permission, Integer grantedBy) {
        permissions.add(new PermissionGrant(permission, grantedBy,
                Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)));
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (active == null) {
            active = true;
        }
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
