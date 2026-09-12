package com.example.backend.physics.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record SimulationFrame(
        BigDecimal time,
        Map<String, BigDecimal> state) {

    public SimulationFrame {
        Objects.requireNonNull(time, "Frame time is required");
        state = state == null || state.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(state));
    }
}
