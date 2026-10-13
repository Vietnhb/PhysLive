package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.entity.Assignment;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.AssignmentStudentRepository;
import com.example.backend.repository.AssignmentSubmissionRepository;
import com.example.backend.repository.SpecificationRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.school.ClassEnrollmentRepository;
import com.example.backend.repository.school.ClassTeacherAssignmentRepository;
import com.example.backend.repository.school.SchoolClassRepository;
import com.example.backend.service.school.LicenseCheckService;

@ExtendWith(MockitoExtension.class)
class AssignmentResponsePrivacyTest {
    @Mock AssignmentRepository assignments;
    @Mock AssignmentStudentRepository recipients;
    @Mock AssignmentSubmissionRepository submissions;
    @Mock SpecificationRepository specifications;
    @Mock UserRepository users;
    @Mock ClassEnrollmentRepository enrollments;
    @Mock ClassTeacherAssignmentRepository teacherAssignments;
    @Mock LicenseCheckService licenses;
    @Mock SchoolClassRepository classes;
    @InjectMocks AssignmentService service;

    @Test
    void studentResponseNeverLeaksAutoGradeAnswer() {
        Role role = new Role(); role.setName("STUDENT");
        User student = new User(); student.setId(9); student.setRole(role);
        UUID assignmentId = UUID.randomUUID();
        Assignment assignment = new Assignment(); assignment.setId(assignmentId);
        assignment.setTeacherId(9); assignment.setMaxScore(BigDecimal.TEN);
        assignment.setGradingCriteria(Map.of("expectedValue", 42, "tolerance", 0.1));
        when(users.findByEmail("student@example.com")).thenReturn(Optional.of(student));
        when(assignments.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(recipients.findByIdAssignmentId(assignmentId)).thenReturn(List.of());
        when(submissions.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId)).thenReturn(List.of());
        when(submissions.findByAssignmentIdAndStudentId(assignmentId, 9)).thenReturn(Optional.empty());

        var response = service.getAssignmentById(assignmentId, "student@example.com");

        assertThat(response.getGradingCriteria()).isNull();
        assertThat(response.getMaxScore()).isEqualByComparingTo("10");
    }
}
