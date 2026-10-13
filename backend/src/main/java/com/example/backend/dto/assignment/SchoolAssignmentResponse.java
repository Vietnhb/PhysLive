package com.example.backend.dto.assignment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolAssignmentResponse(
        UUID id,
        String title,
        UUID classId,
        String className,
        String schoolYear,
        Integer teacherId,
        String teacherName,
        int studentCount,
        String status,
        OffsetDateTime assignedAt,
        OffsetDateTime dueAt) {
}
