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
public class CreateModuleReleaseRequest {

    @NotBlank(message = "Chủ đề (topic) không được để trống")
    private String topic;

    @NotBlank(message = "Tên module không được để trống")
    private String moduleName;

    @NotBlank(message = "Schema ID không được để trống")
    private String schemaId;

    @NotBlank(message = "Schema version không được để trống")
    private String schemaVersion;
}
