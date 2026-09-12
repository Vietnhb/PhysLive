package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDecisionRequest {

    @NotBlank(message = "Câu trả lời phân xử không được để trống")
    private String answer;

    private String comment;

    @NotBlank(message = "Trạng thái phán quyết không được để trống")
    @Pattern(regexp = "RESOLVED|REJECTED", message = "Quyết định phải là RESOLVED hoặc REJECTED")
    private String decisionState;
}
