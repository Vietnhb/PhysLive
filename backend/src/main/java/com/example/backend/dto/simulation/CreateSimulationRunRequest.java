package com.example.backend.dto.simulation;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Allowed scalar or series overrides for a new simulation run")
public record CreateSimulationRunRequest(
        Map<String, BigDecimal> values,
        Map<String, List<BigDecimal>> seriesValues) {

    public CreateSimulationRunRequest {
        values = values == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
        if (seriesValues == null || seriesValues.isEmpty()) {
            seriesValues = Map.of();
        } else {
            Map<String, List<BigDecimal>> copy = new LinkedHashMap<>();
            seriesValues.forEach((key, series) -> copy.put(key,
                    series == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(series))));
            seriesValues = Collections.unmodifiableMap(copy);
        }
    }
}
