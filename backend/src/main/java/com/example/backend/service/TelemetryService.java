package com.example.backend.service;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.backend.dto.ExtractionRunResponse;
import com.example.backend.dto.PipelineTelemetryResponse;
import com.example.backend.entity.ExtractionPath;
import com.example.backend.entity.ExtractionRun;
import com.example.backend.entity.ExtractionRunStatus;
import com.example.backend.repository.ExtractionRunRepository;

import lombok.AllArgsConstructor;

/**
 * Service providing pipeline telemetry and failure monitoring for Administrator.
 */
@Service
@AllArgsConstructor
public class TelemetryService {

    private final ExtractionRunRepository extractionRunRepository;

    /**
     * Calculates high-level telemetry metrics across all extraction runs.
     */
    public PipelineTelemetryResponse getPipelineTelemetry() {
        long total = extractionRunRepository.count();
        long succeeded = extractionRunRepository.countByStatus(ExtractionRunStatus.SUCCEEDED);
        long failed = extractionRunRepository.countByStatus(ExtractionRunStatus.FAILED);

        double successRate = total > 0 ? (double) succeeded / total * 100.0 : 100.0;
        // Round to 2 decimal places
        successRate = Math.round(successRate * 100.0) / 100.0;

        Map<String, Long> providerBreakdown = new HashMap<>();
        providerBreakdown.put("OPENROUTER", extractionRunRepository.countByProviderName("OPENROUTER"));
        providerBreakdown.put("RULE_BASED", extractionRunRepository.countByProviderName("RULE_BASED"));

        Map<String, Long> pathBreakdown = new HashMap<>();
        pathBreakdown.put("OPENROUTER", extractionRunRepository.countByExtractionPath(ExtractionPath.OPENROUTER));
        pathBreakdown.put("RULE_BASED", extractionRunRepository.countByExtractionPath(ExtractionPath.RULE_BASED));

        List<ExtractionRunResponse> recentFailures = extractionRunRepository.findByStatusOrderByCreatedAtDesc(ExtractionRunStatus.FAILED).stream()
                .limit(10)
                .map(this::toResponse)
                .toList();

        return new PipelineTelemetryResponse(
                total,
                succeeded,
                failed,
                successRate,
                providerBreakdown,
                pathBreakdown,
                recentFailures);
    }

    /**
     * Returns top extraction runs, optionally filtered by status (RUNNING, SUCCEEDED, FAILED).
     */
    public List<ExtractionRunResponse> getRecentRuns(String status) {
        List<ExtractionRun> runs;
        if (status != null && !status.trim().isEmpty()) {
            runs = extractionRunRepository.findByStatusOrderByCreatedAtDesc(ExtractionRunStatus.valueOf(status.trim().toUpperCase()));
        } else {
            runs = extractionRunRepository.findTop20ByOrderByCreatedAtDesc();
        }
        return runs.stream().map(this::toResponse).toList();
    }

    private ExtractionRunResponse toResponse(ExtractionRun run) {
        return new ExtractionRunResponse(
                run.getId(),
                run.getSubmissionId(),
                run.getProviderName(),
                run.getExtractionPath() != null ? run.getExtractionPath().name() : null,
                run.getModelVersion(),
                run.getStatus() != null ? run.getStatus().name() : null,
                run.getOutcome() != null ? run.getOutcome().name() : null,
                run.getErrorMessage(),
                run.getCreatedAt() != null ? run.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
    }
}
