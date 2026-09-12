package com.example.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
    private UUID id;
    private String name;
    private String slug;
    private int sortOrder;
    private boolean active;
    private UUID levelId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
