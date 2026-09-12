package com.example.backend.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateClassSessionRequest {

    @Schema(description = "Tiêu đề buổi học / phiên tương tác", example = "Buổi thực hành: Khảo sát định luật II Newton trên lớp")
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @Schema(description = "ID đặc tả mô phỏng", example = "11111111-1111-1111-1111-111111111111")
    @NotNull(message = "specificationId không được để trống")
    private UUID specificationId;

    @Schema(description = "Thời lượng biểu diễn mô phỏng (giây)", example = "180.5")
    private Double durationSeconds = 0.0;

    @Schema(description = "Đặc tả ban đầu trước khi thay đổi tham số (JSONB)")
    @NotNull(message = "initialSpecification không được để trống")
    private java.util.Map<String, Object> initialSpecification;

    @Schema(description = "Dòng thời gian các thao tác kéo slider và thay đổi tham số (JSONB)", example = "[{\"time\": 0.0, \"param\": \"v0\", \"value\": 10.0}]")
    @NotNull(message = "parameterTimeline không được để trống")
    private java.util.List<java.util.Map<String, Object>> parameterTimeline;

    @Schema(description = "Đường dẫn lưu trữ video hoặc export dữ liệu (tùy chọn)")
    private String storageUrl;
}
