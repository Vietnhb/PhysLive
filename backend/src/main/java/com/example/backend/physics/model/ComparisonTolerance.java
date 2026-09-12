package com.example.backend.physics.model;

import java.math.BigDecimal;
import java.util.Objects;

public record ComparisonTolerance(BigDecimal absolute, BigDecimal relative) {

    public ComparisonTolerance {
        Objects.requireNonNull(absolute, "Absolute tolerance is required");
        Objects.requireNonNull(relative, "Relative tolerance is required");
        if (absolute.signum() < 0 || relative.signum() < 0) {
            throw new IllegalArgumentException("Tolerances must not be negative");
        }
    }
}
