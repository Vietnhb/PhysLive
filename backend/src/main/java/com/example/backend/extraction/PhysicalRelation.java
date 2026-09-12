package com.example.backend.extraction;

import java.math.BigDecimal;

public record PhysicalRelation(
        String type,
        String subject,
        String object,
        BigDecimal value,
        String unit,
        String sourceText) {
}
