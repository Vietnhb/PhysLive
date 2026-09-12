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
public class CreateSolverVersionRequest {

    @NotBlank(message = "Solver ID không được để trống")
    private String solverId;

    @NotBlank(message = "Schema ID không được để trống")
    private String schemaId;

    @NotBlank(message = "Phiên bản không được để trống")
    private String version;

    @NotNull(message = "Định nghĩa output solver không được để trống")
    private Map<String, Object> outputDefinition;
}
