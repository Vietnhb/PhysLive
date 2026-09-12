package com.example.backend.physics.model;

import java.math.BigDecimal;

public record PhysicsQuantityDefinition(
        String key,
        String unit,
        QuantityCardinality cardinality,
        boolean required,
        BigDecimal defaultValue,
        String description) {

    public PhysicsQuantityDefinition {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Quantity key is required");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Quantity unit is required");
        }
        if (cardinality == null) {
            throw new IllegalArgumentException("Quantity cardinality is required");
        }
        if (required && defaultValue != null) {
            throw new IllegalArgumentException("A required quantity cannot also have a default value");
        }
    }
}
