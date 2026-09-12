package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkProblemRequest {

    @NotBlank(message = "Chủ đề (topic) không được để trống")
    private String topic;

    @NotBlank(message = "Grade scope không được để trống")
    private String gradeScope; // e.g. LỚP 10, LỚP 11, LỚP 12

    @NotBlank(message = "Danh mục nguồn không được để trống")
    private String sourceCategory; // e.g. TEXTBOOK_STANDARD, EXAM_ADVANCED

    @NotBlank(message = "Nội dung bài toán không được để trống")
    private String problemText;

    private Boolean active = true;
}
