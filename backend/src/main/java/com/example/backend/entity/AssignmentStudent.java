package com.example.backend.entity;

import java.util.UUID;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assignment_students", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentStudent {

    @EmbeddedId
    private AssignmentStudentId id;

    public AssignmentStudent(UUID assignmentId, Integer studentId) {
        this.id = new AssignmentStudentId(assignmentId, studentId);
    }
}
