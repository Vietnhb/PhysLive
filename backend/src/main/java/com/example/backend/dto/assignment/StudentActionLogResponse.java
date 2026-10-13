package com.example.backend.dto.assignment;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.example.backend.entity.StudentActionLog;

public record StudentActionLogResponse(
        UUID id,
        Integer studentId,
        UUID assignmentId,
        String action,
        Map<String, Object> payload,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt) {
    public static StudentActionLogResponse from(StudentActionLog log) {
        return new StudentActionLogResponse(log.getId(), log.getStudentId(), log.getAssignmentId(),
                log.getAction(), log.getPayload(), log.getOccurredAt(), log.getCreatedAt());
    }
}
