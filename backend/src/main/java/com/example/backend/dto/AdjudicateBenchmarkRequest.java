package com.example.backend.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdjudicateBenchmarkRequest {

    @NotBlank(message = "Reviewer reference không được để trống")
    private String reviewerReference;

    @NotBlank(message = "Trạng thái bất đồng không được để trống")
    private String disagreementState;

    @NotNull(message = "Đặc tả giải quyết không được để trống")
    private Map<String, Object> resolvedSpecification;
}
