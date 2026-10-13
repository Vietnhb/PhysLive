package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentSubmissionResponse {

    @Schema(description = "ID bài nộp")
    private UUID id;

    @Schema(description = "ID bài tập")
    private UUID assignmentId;

    @Schema(description = "Tiêu đề bài tập")
    private String assignmentTitle;

    @Schema(description = "ID học sinh nộp bài")
    private Integer studentId;

    @Schema(description = "Tên học sinh nộp bài")
    private String studentName;

    @Schema(description = "Email học sinh nộp bài")
    private String studentEmail;

    @Schema(description = "Dự đoán khoa học / kết quả câu trả lời đã nộp (JSONB)")
    private Object predictions;

    @Schema(description = "Thời điểm nộp bài")
    private OffsetDateTime submittedAt;

    private OffsetDateTime completedAt;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String feedback;
    private String gradingStatus;
    private OffsetDateTime gradedAt;
    private boolean retryAllowed;

    @Schema(description = "Thời điểm tạo")
    private OffsetDateTime createdAt;
}
