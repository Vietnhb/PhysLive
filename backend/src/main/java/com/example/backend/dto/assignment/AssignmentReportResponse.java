package com.example.backend.dto.assignment;

import java.math.BigDecimal;

public record AssignmentReportResponse(
        long assigned,
        long submitted,
        long pending,
        long graded,
        long confirmed,
        long retryAllowed,
        BigDecimal averageScore,
        BigDecimal maxScore) {
}
