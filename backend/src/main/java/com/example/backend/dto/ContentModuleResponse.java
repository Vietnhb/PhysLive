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
public class ContentModuleResponse {
    private UUID id;
    private String name;
    private String slug;
    private boolean active;
    private int sortOrder;
    private UUID topicId;
    private String topicName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
