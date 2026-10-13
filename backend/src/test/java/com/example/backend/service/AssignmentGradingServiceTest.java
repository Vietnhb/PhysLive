package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.entity.Assignment;
import com.example.backend.entity.AssignmentSubmission;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
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
class AssignmentGradingServiceTest {
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

    private User teacher;
    private Assignment assignment;
    private AssignmentSubmission submission;

    @BeforeEach
    void setup() {
        Role role = new Role(); role.setName("STAFF");
        teacher = new User(); teacher.setId(4); teacher.setRole(role);
        assignment = new Assignment(); assignment.setId(UUID.randomUUID());
        assignment.setTeacherId(4); assignment.setMaxScore(BigDecimal.TEN);
        submission = new AssignmentSubmission(); submission.setId(UUID.randomUUID());
        submission.setAssignmentId(assignment.getId()); submission.setAssignment(assignment);
        when(users.findByEmail("teacher@example.com")).thenReturn(Optional.of(teacher));
        when(assignments.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(submissions.findById(submission.getId())).thenReturn(Optional.of(submission));
    }

    @Test
    void teacherCanConfirmScoreAndFeedback() {
        when(submissions.save(submission)).thenReturn(submission);
        var result = service.gradeSubmission(assignment.getId(), submission.getId(),
                new BigDecimal("8.5"), "Good reasoning", true, "teacher@example.com");

        assertThat(result.getScore()).isEqualByComparingTo("8.5");
        assertThat(result.getGradingStatus()).isEqualTo("TEACHER_CONFIRMED");
        assertThat(submission.getGradedBy()).isEqualTo(4);
    }

    @Test
    void scoreCannotExceedAssignmentsMaximum() {
        assertThatThrownBy(() -> service.gradeSubmission(assignment.getId(), submission.getId(),
                new BigDecimal("10.001"), null, true, "teacher@example.com"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("between 0 and 10");
    }

    @Test
    void reopeningMarksSubmissionForRetry() {
        when(submissions.save(submission)).thenReturn(submission);
        var result = service.reopenSubmission(assignment.getId(), submission.getId(), "teacher@example.com");
        assertThat(result.isRetryAllowed()).isTrue();
        assertThat(result.getGradingStatus()).isEqualTo("RETURNED");
    }
}
