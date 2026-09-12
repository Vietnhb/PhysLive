package com.example.backend.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PipelineTelemetryResponse {
    private long totalRuns;
    private long succeededRuns;
    private long failedRuns;
    private double successRatePercent;
    private Map<String, Long> providerBreakdown;
    private Map<String, Long> pathBreakdown;
    private List<ExtractionRunResponse> recentFailures;
}
