package com.example.backend.service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.backend.dto.health.SystemHealthResponse;
import com.example.backend.physics.model.PhysicsArchetype;

import lombok.RequiredArgsConstructor;

/**
 * Service kiểm tra trạng thái toàn diện hệ thống:
 * - Database: kết nối, độ trễ phản hồi (ms), hệ quản trị CSDL.
 * - Disk Space: dung lượng đĩa trống, tổng dung lượng.
 * - JVM Memory: lượng RAM đã dùng, tối đa, còn trống.
 * - Physics Engine Core: danh sách archetype và bộ giải được hỗ trợ.
 */
@Service
@RequiredArgsConstructor
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);
    private final DataSource dataSource;

    public SystemHealthResponse checkHealth() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        String now = Instant.now().toString();

        SystemHealthResponse.DatabaseHealth dbHealth = checkDatabase();
        SystemHealthResponse.DiskHealth diskHealth = checkDiskSpace();
        SystemHealthResponse.MemoryHealth memoryHealth = checkMemory();
        SystemHealthResponse.PhysicsEngineHealth physicsHealth = checkPhysicsEngine();

        String overallStatus = "UP";
        if (!"UP".equalsIgnoreCase(dbHealth.getStatus())) {
            overallStatus = "DOWN";
        } else if (!"UP".equalsIgnoreCase(diskHealth.getStatus())) {
            overallStatus = "DEGRADED";
        }

        return SystemHealthResponse.builder()
                .status(overallStatus)
                .timestamp(now)
                .uptimeMs(uptime)
                .database(dbHealth)
                .diskSpace(diskHealth)
                .memory(memoryHealth)
                .physicsEngine(physicsHealth)
                .build();
    }

    private SystemHealthResponse.DatabaseHealth checkDatabase() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(3);
            long latency = System.currentTimeMillis() - start;
            DatabaseMetaData metaData = conn.getMetaData();
            String dbProduct = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();

            return SystemHealthResponse.DatabaseHealth.builder()
                    .status(valid ? "UP" : "DOWN")
                    .database(dbProduct)
                    .latencyMs(latency)
                    .build();
        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;
            log.error("Database health check failed", ex);
            return SystemHealthResponse.DatabaseHealth.builder()
                    .status("DOWN")
                    .database("Unknown")
                    .latencyMs(latency)
                    .error(ex.getMessage())
                    .build();
        }
    }

    private SystemHealthResponse.DiskHealth checkDiskSpace() {
        try {
            File root = new File(".");
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            long usable = root.getUsableSpace();

            double freeGb = (double) usable / (1024 * 1024 * 1024);
            String status = usable > (50L * 1024 * 1024) ? "UP" : "LOW_DISK";

            return SystemHealthResponse.DiskHealth.builder()
                    .status(status)
                    .totalBytes(total)
                    .freeBytes(free)
                    .usableBytes(usable)
                    .freeGb(String.format("%.2f GB", freeGb))
                    .build();
        } catch (Exception ex) {
            log.warn("Disk health check warning", ex);
            return SystemHealthResponse.DiskHealth.builder()
                    .status("UNKNOWN")
                    .freeGb("N/A")
                    .build();
        }
    }

    private SystemHealthResponse.MemoryHealth checkMemory() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long max = runtime.maxMemory();
        long used = total - free;

        double usedMb = (double) used / (1024 * 1024);
        double maxMb = (double) max / (1024 * 1024);

        return SystemHealthResponse.MemoryHealth.builder()
                .status("UP")
                .totalMemoryBytes(total)
                .freeMemoryBytes(free)
                .maxMemoryBytes(max)
                .usedMb(String.format("%.1f MB", usedMb))
                .maxMb(String.format("%.1f MB", maxMb))
                .build();
    }

    private SystemHealthResponse.PhysicsEngineHealth checkPhysicsEngine() {
        List<String> archetypes = Arrays.stream(PhysicsArchetype.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        List<String> solverTypes = List.of("CLOSED_FORM", "NUMERICAL_ODE");

        return SystemHealthResponse.PhysicsEngineHealth.builder()
                .status("UP")
                .supportedArchetypes(archetypes)
                .solverTypes(solverTypes)
                .message("Dual validation (Closed-form & ODE) is active")
                .build();
    }
}
