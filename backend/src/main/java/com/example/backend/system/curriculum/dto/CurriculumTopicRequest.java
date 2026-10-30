package com.example.backend.system.curriculum.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CurriculumTopicRequest(@NotNull @Valid LocalizedName name, String slug,
        @PositiveOrZero Integer sortOrder) { }
