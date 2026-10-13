package com.example.backend.entity.enums;

import java.util.Arrays;
import java.util.Optional;

/** Roles supported by the authorization and school-membership model. */
public enum RoleName {
    ADMIN(1, Scope.PLATFORM),
    MANAGER(2, Scope.PLATFORM),
    REVIEWER(3, Scope.PLATFORM, "CONTENT_REVIEWER"),
    SCHOOL(4, Scope.SCHOOL, "SCHOOL_MANAGER"),
    STAFF(5, Scope.SCHOOL, "TEACHER"),
    STUDENT(6, Scope.SCHOOL);

    private final int id;
    private final Scope scope;
    private final String[] legacyNames;

    RoleName(int id, Scope scope, String... legacyNames) {
        this.id = id;
        this.scope = scope;
        this.legacyNames = legacyNames;
    }

    public int id() { return id; }

    public boolean matches(String value) {
        if (value == null) return false;
        String normalized = value.trim();
        return name().equalsIgnoreCase(normalized)
                || Arrays.stream(legacyNames).anyMatch(name -> name.equalsIgnoreCase(normalized));
    }

    public boolean isPlatformRole() {
        return scope == Scope.PLATFORM;
    }

    public boolean isSchoolRole() {
        return scope == Scope.SCHOOL;
    }

    public String authority() {
        return "ROLE_" + name();
    }

    public static Optional<RoleName> from(String value) {
        return Arrays.stream(values()).filter(role -> role.matches(value)).findFirst();
    }

    private enum Scope {
        PLATFORM,
        SCHOOL
    }
}
