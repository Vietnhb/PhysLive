package com.example.backend.dto.simulation;

import java.math.BigDecimal;

/** Authoritative end of a simulation run after applying its end-condition contract. */
public record ResolvedEnd(BigDecimal time, String reason, boolean conditionReached) {
}
