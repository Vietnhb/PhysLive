package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateAssignmentRequest {

    @Schema(description = "Tiêu đề mới cho bài tập")
    private String title;

    @Schema(description = "Mô tả hướng dẫn bài tập")
    private String description;

    @Schema(description = "Trạng thái: ACTIVE hoặc CLOSED", example = "ACTIVE")
    @Pattern(regexp = "ACTIVE|CLOSED", message = "Status phải là ACTIVE hoặc CLOSED")
    private String status;

    @Schema(description = "Hạn nộp bài tập mới (UTC)")
    private OffsetDateTime dueAt;

    @Schema(description = "Nội dung câu hỏi dự đoán (JSONB)")
    private java.util.Map<String, Object> questions;

    @Schema(description = "Cập nhật danh sách học sinh được giao")
    private List<Integer> studentIds;
}
