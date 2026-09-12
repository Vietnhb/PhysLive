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
public class CreateSchemaVersionRequest {

    @NotBlank(message = "Schema ID không được để trống")
    private String schemaId;

    @NotBlank(message = "Chủ đề (topic) không được để trống")
    private String topic;

    @NotBlank(message = "Phiên bản (version) không được để trống")
    private String version;

    @NotBlank(message = "Tên schema không được để trống")
    private String name;

    @NotNull(message = "Định nghĩa JSON schema không được để trống")
    private Map<String, Object> definition;

    private Boolean enabled = true;
}
