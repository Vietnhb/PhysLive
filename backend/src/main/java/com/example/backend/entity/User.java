package com.example.backend.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}
