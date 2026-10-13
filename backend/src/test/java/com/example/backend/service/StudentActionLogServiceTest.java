package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.dto.assignment.StudentActionLogRequest;
import com.example.backend.entity.StudentActionLog;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.AssignmentStudentRepository;
import com.example.backend.repository.StudentActionLogRepository;

@ExtendWith(MockitoExtension.class)
class StudentActionLogServiceTest {
    @Mock StudentActionLogRepository logs;
    @Mock AssignmentStudentRepository recipients;
    @Mock CurrentUserService currentUser;
    @InjectMocks StudentActionLogService service;

    @Test
    void rejectsActionFromStudentWhoWasNotAssigned() {
        User student = new User(); student.setId(7);
        UUID assignmentId = UUID.randomUUID();
        when(currentUser.requireAnyRole("STUDENT")).thenReturn(student);

        assertThatThrownBy(() -> service.create(
                new StudentActionLogRequest(assignmentId, "OPENED", null)))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(logs);
    }

    @Test
    void storesAndReturnsOnlyCurrentStudentsActions() {
        User student = new User(); student.setId(7);
        UUID assignmentId = UUID.randomUUID();
        when(currentUser.requireAnyRole("STUDENT")).thenReturn(student);
        when(recipients.existsByIdAssignmentIdAndIdStudentId(assignmentId, 7)).thenReturn(true);
        when(logs.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new StudentActionLogRequest(
                assignmentId, " OPENED ", Map.of("frame", 3)));

        assertThat(response.studentId()).isEqualTo(7);
        assertThat(response.action()).isEqualTo("OPENED");
        assertThat(response.payload()).containsEntry("frame", 3);

        StudentActionLog stored = new StudentActionLog(); stored.setStudentId(7);
        stored.setAssignmentId(assignmentId); stored.setAction("OPENED");
        when(logs.findTop200ByStudentIdOrderByOccurredAtDesc(7)).thenReturn(List.of(stored));
        assertThat(service.mine()).hasSize(1);
        verify(logs).findTop200ByStudentIdOrderByOccurredAtDesc(7);
    }
}
