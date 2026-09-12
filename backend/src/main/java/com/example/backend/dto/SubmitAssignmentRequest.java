package com.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAssignmentRequest {

    @Schema(description = "Dự đoán khoa học / câu trả lời trước khi xem mô phỏng (JSONB)", example = "{\"predicted_range_meters\": 25.4, \"predicted_impact_velocity\": 14.2, \"hypothesis\": \"Tầm xa tỉ lệ thuận với căn bậc hai của độ cao\"}")
    @NotNull(message = "predictions không được để trống")
    private java.util.Map<String, Object> predictions;
}
