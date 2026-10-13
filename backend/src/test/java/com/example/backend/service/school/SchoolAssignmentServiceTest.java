package com.example.backend.service.school;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example.backend.entity.Assignment;
import com.example.backend.entity.AssignmentStudent;
import com.example.backend.entity.User;
import com.example.backend.entity.school.School;
import com.example.backend.entity.school.SchoolClass;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.AssignmentStudentRepository;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.account.AccountAccessService;

@ExtendWith(MockitoExtension.class)
class SchoolAssignmentServiceTest {
    @Mock AssignmentRepository assignments;
    @Mock AssignmentStudentRepository recipients;
    @Mock CurrentUserService currentUser;
    @Mock AccountAccessService access;
    @InjectMocks SchoolAssignmentService service;

    @Test
    void departmentHeadGetsPagedAssignmentsFromOwnSchool() {
        UUID schoolId = UUID.randomUUID();
        School school = new School(); school.setId(schoolId);
        User head = new User(); head.setSchool(school);
        User teacher = new User(); teacher.setId(8); teacher.setFullName("Teacher");
        SchoolClass schoolClass = new SchoolClass(); schoolClass.setId(UUID.randomUUID());
        schoolClass.setName("10A1"); schoolClass.setSchoolYear("2026-2027");
        Assignment assignment = new Assignment(); assignment.setId(UUID.randomUUID());
        assignment.setTitle("Motion"); assignment.setTeacherId(8); assignment.setTeacher(teacher);
        assignment.setSchoolClass(schoolClass); assignment.setStatus("ACTIVE");
        var paging = PageRequest.of(0, 30, Sort.by("createdAt").descending());
        when(currentUser.requireCurrentUser()).thenReturn(head);
        when(access.isDepartmentHead(head)).thenReturn(true);
        when(assignments.findSchoolAssignments(schoolId, paging))
                .thenReturn(new PageImpl<>(List.of(assignment), paging, 1));
        when(recipients.findByIdAssignmentId(assignment.getId())).thenReturn(List.of(
                new AssignmentStudent(assignment.getId(), 11),
                new AssignmentStudent(assignment.getId(), 12)));

        var result = service.list(schoolId, 0, 30);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().studentCount()).isEqualTo(2);
        assertThat(result.getContent().getFirst().className()).isEqualTo("10A1");
    }

    @Test
    void ordinaryStaffCannotInspectSchoolAssignments() {
        UUID schoolId = UUID.randomUUID();
        School school = new School(); school.setId(schoolId);
        User staff = new User(); staff.setSchool(school);
        when(currentUser.requireCurrentUser()).thenReturn(staff);

        assertThatThrownBy(() -> service.list(schoolId, 0, 30)).isInstanceOf(ApiException.class);
        verifyNoInteractions(assignments, recipients);
    }
}
