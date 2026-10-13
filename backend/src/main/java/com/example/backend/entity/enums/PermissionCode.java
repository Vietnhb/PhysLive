package com.example.backend.entity.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Permissions assignable to an individual STAFF account. */
public enum PermissionCode {
    TEACH("Giáo viên"),
    DEPARTMENT_HEAD_PHYSICS("Tổ trưởng bộ môn Vật Lý");

    private final String label;

    PermissionCode(String label) {
        this.label = label;
    }

    public RoleName role() { return RoleName.STAFF; }

    public String label() { return label; }

    public static Optional<PermissionCode> from(String value) {
        if (value == null) return Optional.empty();
        String code = value.trim();
        return Arrays.stream(values()).filter(item -> item.name().equalsIgnoreCase(code)).findFirst();
    }

    public static List<PermissionCode> defaults() {
        return List.of(TEACH);
    }
}
