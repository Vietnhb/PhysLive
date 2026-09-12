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
public class TopicResponse {
    private UUID id;
    private String name;
    private String slug;
    private boolean enabled;
    private int sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
