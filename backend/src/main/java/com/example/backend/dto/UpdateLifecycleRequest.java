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
public class UpdateLifecycleRequest {

    @NotBlank(message = "Trạng thái vòng đời không được để trống")
    @Pattern(regexp = "DRAFT|APPROVED|RETIRED", message = "Trạng thái phải là DRAFT, APPROVED hoặc RETIRED")
    private String lifecycleStatus;
}
