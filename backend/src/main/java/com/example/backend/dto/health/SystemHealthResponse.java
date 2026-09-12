package com.example.backend.dto.health;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemHealthResponse {

    private String status;
    private String timestamp;
    private long uptimeMs;
    private DatabaseHealth database;
    private DiskHealth diskSpace;
    private MemoryHealth memory;
    private PhysicsEngineHealth physicsEngine;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseHealth {
        private String status;
        private String database;
        private long latencyMs;
        private String error;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskHealth {
        private String status;
        private long totalBytes;
        private long freeBytes;
        private long usableBytes;
        private String freeGb;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryHealth {
        private String status;
        private long totalMemoryBytes;
        private long freeMemoryBytes;
        private long maxMemoryBytes;
        private String usedMb;
        private String maxMb;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhysicsEngineHealth {
        private String status;
        private List<String> supportedArchetypes;
        private List<String> solverTypes;
        private String message;
    }
}
