package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SimulationRunResponse {

    @Schema(description = "ID phiên chạy mô phỏng")
    private UUID id;

    @Schema(description = "ID mô phỏng")
    private UUID simulationId;

    @Schema(description = "Thời lượng chạy (giây)")
    private Double durationSeconds;

    @Schema(description = "Kết quả chạy / quỹ đạo tọa độ (JSONB)")
    private Object result;

    @Schema(description = "Loại phiên chạy")
    private String runType;

    @Schema(description = "Kết quả kiểm định nghiệm")
    private boolean validationPassed;

    @Schema(description = "Thời điểm thực hiện")
    private OffsetDateTime createdAt;

    @Schema(description = "Thời điểm cập nhật")
    private OffsetDateTime updatedAt;
}
