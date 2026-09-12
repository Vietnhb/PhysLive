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
public class TopicRequest {

    @NotBlank(message = "Tên chủ đề không được để trống")
    private String name;

    @NotBlank(message = "Slug không được để trống")
    private String slug;

    private Integer sortOrder = 0;
    private Boolean enabled = true;
}
