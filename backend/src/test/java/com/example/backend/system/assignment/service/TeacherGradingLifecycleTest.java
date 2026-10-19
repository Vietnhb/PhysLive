package com.example.backend.system.assignment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.backend.exception.ApiException;
import com.example.backend.system.account.model.entity.Role;
import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.account.repository.UserRepository;
import com.example.backend.system.account.service.CurrentUserService;
import com.example.backend.system.assignment.model.entity.Assignment;
import com.example.backend.system.assignment.model.entity.AssignmentSubmission;
import com.example.backend.system.assignment.model.enums.AssignmentStatus;
import com.example.backend.system.assignment.model.enums.GradingStatus;
import com.example.backend.system.assignment.repository.AssignmentRepository;
import com.example.backend.system.assignment.repository.AssignmentSubmissionRepository;
import com.example.backend.system.library.repository.LibraryItemRepository;
import com.example.backend.system.school.repository.ClassEnrollmentRepository;
import com.example.backend.system.school.repository.ClassTeacherAssignmentRepository;
import com.example.backend.system.simulation.service.SimulationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeacherGradingLifecycleTest {
    private final AssignmentRepository assignments = mock(AssignmentRepository.class);
    private final AssignmentSubmissionRepository submissions = mock(AssignmentSubmissionRepository.class);
    private final CurrentUserService current = mock(CurrentUserService.class);
    private final AssignmentService service = new AssignmentService(assignments, submissions,
            mock(LibraryItemRepository.class), mock(UserRepository.class), current, mock(SimulationService.class),
            mock(ClassEnrollmentRepository.class), mock(ClassTeacherAssignmentRepository.class));
    private final Assignment assignment = new Assignment();
    private final AssignmentSubmission submission = new AssignmentSubmission();
    private final User teacher = new User();

    @BeforeEach void setup() {
        Role role = new Role(); role.setName("STAFF");
        teacher.setId(1); teacher.setRole(role);
        User student = new User(); student.setId(2);
        assignment.setId(UUID.randomUUID()); assignment.setTeacher(teacher); assignment.setStatus(AssignmentStatus.ACTIVE);
        submission.setId(UUID.randomUUID()); submission.setAssignment(assignment); submission.setStudent(student);
        submission.setCompletedAt(Instant.now());
        when(current.requireCurrentUser()).thenReturn(teacher);
        when(assignments.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(submissions.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(submissions.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test void returnAfterGradingClearsGradeForTheNextAttempt() {
        service.grade(assignment.getId(), submission.getId(), new BigDecimal("8"), "Good", true);
        assertEquals(GradingStatus.TEACHER_CONFIRMED, submission.getGradingStatus());
        service.reopen(assignment.getId(), submission.getId());
        assertNull(submission.getScore()); assertNull(submission.getCompletedAt());
        assertNull(submission.getGradedAt()); assertTrue(submission.isRetryAllowed());
        assertEquals(GradingStatus.RETURNED, submission.getGradingStatus());
    }

    @Test void anotherTeacherCannotGradeOrReturnTheSubmission() {
        User stranger = new User(); stranger.setId(99); stranger.setRole(teacher.getRole());
        when(current.requireCurrentUser()).thenReturn(stranger);
        assertThrows(ApiException.class, () -> service.grade(assignment.getId(), submission.getId(), BigDecimal.ONE, "", true));
        assertThrows(ApiException.class, () -> service.reopen(assignment.getId(), submission.getId()));
        verify(submissions, never()).save(any());
    }

    @Test void closedAssignmentCannotReturnAnAttempt() {
        assignment.setStatus(AssignmentStatus.CLOSED);
        assertThrows(ApiException.class, () -> service.reopen(assignment.getId(), submission.getId()));
        verify(submissions, never()).save(any());
    }
}
