package com.example.backend.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateSimulationRunRequest {

    @Schema(description = "ID mô phỏng (Simulation)", example = "11111111-1111-1111-1111-111111111111")
    @NotNull(message = "simulationId không được để trống")
    private UUID simulationId;

    @Schema(description = "Thời lượng chạy mô phỏng (giây)", example = "15.0")
    private Double durationSeconds = 0.0;

    @Schema(description = "Kết quả tính toán / dữ liệu tọa độ thời gian (JSONB)", example = "{\"t\": [0, 1, 2], \"x\": [0, 5, 10], \"y\": [20, 15.1, 0.4]}")
    @NotNull(message = "result không được để trống")
    private java.util.Map<String, Object> result;

    @Schema(description = "Loại phiên chạy: INTERACTIVE, BENCHMARK, hoặc VALIDATION", example = "INTERACTIVE")
    private String runType = "INTERACTIVE";

    @Schema(description = "Mô phỏng có vượt qua kiểm tra nghiệm đóng vs ODE hay không", example = "true")
    private Boolean validationPassed = true;
}
