package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAssignmentRequest {

    @Schema(description = "Tiêu đề bài tập", example = "Dự đoán vận tốc chạm đất và quỹ đạo ném ngang")
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @Schema(description = "Mô tả hướng dẫn cho học sinh", example = "Hãy điều chỉnh vận tốc ban đầu và dự đoán tầm xa trước khi chạy mô phỏng.")
    private String description;

    @Schema(description = "ID đặc tả mô phỏng được giao kèm", example = "11111111-1111-1111-1111-111111111111")
    @NotNull(message = "specificationId không được để trống")
    private UUID specificationId;

    @Schema(description = "Hạn nộp bài tập (UTC)")
    private OffsetDateTime dueAt;

    @Schema(description = "Danh sách câu hỏi dự đoán hoặc khảo sát dành cho học sinh (JSONB)")
    private java.util.Map<String, Object> questions;

    @Schema(description = "Danh sách ID của các học sinh được giao bài tập", example = "[7, 18]")
    private List<Integer> studentIds;
}
