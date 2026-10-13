package com.example.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.entity.school.*;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.*;
import com.example.backend.repository.school.*;
import com.example.backend.service.school.LicenseCheckService;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssignmentClassPolicyTest {
    @Mock AssignmentRepository assignments;
    @Mock AssignmentStudentRepository recipients;
    @Mock AssignmentSubmissionRepository submissions;
    @Mock SpecificationRepository specs;
    @Mock UserRepository users;
    @Mock ClassEnrollmentRepository enrollments;
    @Mock ClassTeacherAssignmentRepository teachers;
    @Mock LicenseCheckService licenses;
    @Mock SchoolClassRepository classes;
    @InjectMocks AssignmentService service;
    User teacher;
    SchoolClass target;
    CreateAssignmentRequest request;

    @BeforeEach void setup() {
        School school = new School(); school.setId(UUID.randomUUID()); school.setActive(true);
        Role role = new Role(); role.setName("STAFF");
        teacher = new User(); teacher.setId(1); teacher.setRole(role); teacher.setSchool(school);
        target = new SchoolClass(); target.setId(UUID.randomUUID()); target.setSchool(school); target.setIsActive(true);
        request = new CreateAssignmentRequest(); request.setClassId(target.getId()); request.setTitle("Test");
        request.setSpecificationId(UUID.randomUUID());
        when(users.findByEmail("teacher")).thenReturn(Optional.of(teacher));
    }
    @Test void creationRequiresClass() {
        request.setClassId(null);
        assertThatThrownBy(() -> service.createAssignment(request, "teacher")).isInstanceOf(ApiException.class)
                .hasMessageContaining("Select a class");
        verifyNoInteractions(assignments);
    }
    @Test void creationRejectsUnassignedTeacher() {
        when(classes.findById(target.getId())).thenReturn(Optional.of(target));
        assertThatThrownBy(() -> service.createAssignment(request, "teacher")).isInstanceOf(ApiException.class)
                .hasMessageContaining("not assigned");
        verifyNoInteractions(assignments);
    }
    @Test void creationRejectsOtherSchoolEvenWithTeacherAssignment() {
        target.setSchool(new School()); target.getSchool().setId(UUID.randomUUID());
        when(classes.findById(target.getId())).thenReturn(Optional.of(target));
        assertThatThrownBy(() -> service.createAssignment(request, "teacher")).isInstanceOf(ApiException.class)
                .hasMessageContaining("not assigned");
    }
    @Test void creationRequiresRecipients() {
        when(classes.findById(target.getId())).thenReturn(Optional.of(target));
        when(teachers.existsBySchoolClassIdAndTeacherIdAndIsActiveTrue(target.getId(), teacher.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.createAssignment(request, "teacher")).isInstanceOf(ApiException.class)
                .hasMessageContaining("at least one");
    }

    private ClassEnrollment recipient() {
        User student = new User(); student.setId(2); student.setSchool(teacher.getSchool());
        Role role = new Role(); role.setName("STUDENT"); student.setRole(role);
        ClassEnrollment enrollment = new ClassEnrollment(); enrollment.setStudent(student); enrollment.setSchoolClass(target);
        request.setStudentIds(List.of(2));
        when(users.findById(2)).thenReturn(Optional.of(student));
        when(enrollments.findActiveEnrollmentsByStudentId(2)).thenReturn(List.of(enrollment));
        when(teachers.existsBySchoolClassIdAndTeacherIdAndIsActiveTrue(target.getId(), teacher.getId())).thenReturn(true);
        return enrollment;
    }

    @Test void creationPersistsClassAndReturnsClassMetadata() {
        var enrollment = recipient();
        when(classes.findById(target.getId())).thenReturn(Optional.of(target));
        when(enrollments.findActiveStudentsByClassId(target.getId())).thenReturn(List.of(enrollment));
        ProblemSubmission problem = new ProblemSubmission(); problem.setOwner(teacher);
        Specification spec = new Specification(); spec.setId(request.getSpecificationId()); spec.setSubmission(problem);
        when(specs.findById(spec.getId())).thenReturn(Optional.of(spec));
        when(assignments.save(any())).thenAnswer(invocation -> {
            Assignment saved = invocation.getArgument(0); saved.setId(UUID.randomUUID()); return saved;
        });
        assertThat(service.createAssignment(request, "teacher").getClassId()).isEqualTo(target.getId());
        verify(assignments).save(argThat(a -> a.getSchoolClass() == target));
    }

    @Test void creationRejectsStudentOutsideSelectedClass() {
        recipient();
        when(classes.findById(target.getId())).thenReturn(Optional.of(target));
        when(enrollments.findActiveStudentsByClassId(target.getId())).thenReturn(List.of());
        assertThatThrownBy(() -> service.createAssignment(request, "teacher")).isInstanceOf(ApiException.class)
                .hasMessageContaining("selected class");
        verifyNoInteractions(assignments);
    }

    @Test void updateCannotAddStudentOutsideOriginalClass() {
        recipient();
        Assignment assignment = new Assignment(); assignment.setId(UUID.randomUUID());
        assignment.setTeacher(teacher); assignment.setTeacherId(teacher.getId()); assignment.setSchoolClass(target);
        when(assignments.findById(assignment.getId())).thenReturn(Optional.of(assignment));
        when(enrollments.findActiveStudentsByClassId(target.getId())).thenReturn(List.of());
        UpdateAssignmentRequest update = new UpdateAssignmentRequest(); update.setStudentIds(List.of(2));
        assertThatThrownBy(() -> service.updateAssignment(assignment.getId(), update, "teacher"))
                .isInstanceOf(ApiException.class).hasMessageContaining("selected class");
        verifyNoInteractions(recipients);
    }
}
