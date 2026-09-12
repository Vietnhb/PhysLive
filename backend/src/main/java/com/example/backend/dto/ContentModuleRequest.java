package com.example.backend.dto;

import java.util.UUID;

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
public class ContentModuleRequest {

    @NotBlank(message = "Tên module không được để trống")
    private String name;

    @NotBlank(message = "Slug module không được để trống")
    private String slug;

    private Integer sortOrder = 0;
    private Boolean active = true;

    @NotNull(message = "Topic ID không được để trống")
    private UUID topicId;
}
