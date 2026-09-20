package com.example.backend.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.AssignmentResponse;
import com.example.backend.dto.AssignmentSubmissionResponse;
import com.example.backend.dto.CreateAssignmentRequest;
import com.example.backend.dto.SubmitAssignmentRequest;
import com.example.backend.dto.UpdateAssignmentRequest;
import com.example.backend.entity.Assignment;
import com.example.backend.entity.AssignmentStudent;
import com.example.backend.entity.AssignmentSubmission;
import com.example.backend.entity.Specification;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.AssignmentStudentRepository;
import com.example.backend.repository.AssignmentSubmissionRepository;
import com.example.backend.repository.SpecificationRepository;
import com.example.backend.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentStudentRepository assignmentStudentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final SpecificationRepository specificationRepository;
    private final UserRepository userRepository;
    private final com.example.backend.repository.school.ClassEnrollmentRepository enrollments;
    private final com.example.backend.repository.school.ClassTeacherAssignmentRepository teacherAssignments;
    private final com.example.backend.service.school.LicenseCheckService licenses;

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments(String userEmail) {
        User user = getUserByEmail(userEmail);
        String roleName = user.getRole() != null ? user.getRole().getName() : "STUDENT";

        List<Assignment> list;
        if ("STUDENT".equalsIgnoreCase(roleName)) {
            list = assignmentRepository.findAssignmentsForStudent(user.getId());
        } else if ("TEACHER".equalsIgnoreCase(roleName)) {
            list = assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(user.getId());
        } else if ("ADMIN".equalsIgnoreCase(roleName)) {
            list = assignmentRepository.findAll();
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "Teaching role is required");
        }

        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(UUID id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + id));

        requireReadAccess(user, assignment);
        return toResponse(assignment);
    }

    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, String userEmail) {
        User teacher = getUserByEmail(userEmail);
        requireTeacher(teacher);
        licenses.requireWriteAccess(teacher);
        validateStudents(teacher, request.getStudentIds());
        Specification spec = specificationRepository.findById(request.getSpecificationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy đặc tả mô phỏng (Specification) với ID: " + request.getSpecificationId()));

        if (!isAdmin(teacher) && (spec.getSubmission() == null
                || !teacher.getId().equals(spec.getSubmission().getOwner().getId())))
            throw new ApiException(HttpStatus.FORBIDDEN, "Specification belongs to another teacher");
        Assignment assignment = new Assignment();
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(request.getDescription());
        assignment.setStatus("ACTIVE");
        assignment.setAssignedAt(OffsetDateTime.now());
        assignment.setDueAt(request.getDueAt());
        assignment.setQuestions(request.getQuestions() != null ? request.getQuestions() : Collections.emptyMap());
        assignment.setTeacherId(teacher.getId());
        assignment.setTeacher(teacher);
        assignment.setSpecificationId(spec.getId());
        assignment.setSpecification(spec);

        Assignment saved = assignmentRepository.save(assignment);

        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            List<AssignmentStudent> students = new ArrayList<>();
            for (Integer studentId : request.getStudentIds()) {
                students.add(new AssignmentStudent(saved.getId(), studentId));
            }
            assignmentStudentRepository.saveAll(students);
        }

        return toResponse(saved);
    }

    @Transactional
    public AssignmentResponse updateAssignment(UUID id, UpdateAssignmentRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + id));

        boolean isAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName());
        if (!assignment.getTeacherId().equals(user.getId()) && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật bài tập này");
        }

        licenses.requireWriteAccess(user);
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            assignment.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            assignment.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            assignment.setStatus(request.getStatus().toUpperCase());
        }
        if (request.getDueAt() != null) {
            assignment.setDueAt(request.getDueAt());
        }
        if (request.getQuestions() != null) {
            assignment.setQuestions(request.getQuestions());
        }

        if (request.getStudentIds() != null) {
            validateStudents(assignment.getTeacher(), request.getStudentIds());
            assignmentStudentRepository.deleteByIdAssignmentId(id);
            List<AssignmentStudent> students = request.getStudentIds().stream()
                    .map(sId -> new AssignmentStudent(id, sId))
                    .collect(Collectors.toList());
            assignmentStudentRepository.saveAll(students);
        }

        Assignment updated = assignmentRepository.save(assignment);
        return toResponse(updated);
    }

    @Transactional
    public AssignmentResponse updateStatus(UUID id, String status, String userEmail) {
        User user = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + id));

        boolean isAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName());
        if (!assignment.getTeacherId().equals(user.getId()) && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền thay đổi trạng thái bài tập này");
        }

        licenses.requireWriteAccess(user);
        assignment.setStatus(status.toUpperCase());
        Assignment updated = assignmentRepository.save(assignment);
        return toResponse(updated);
    }

    @Transactional
    public AssignmentSubmissionResponse submitAssignment(UUID assignmentId, SubmitAssignmentRequest request, String userEmail) {
        User student = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + assignmentId));

        licenses.requireWriteAccess(student);
        if (student.getRole() == null || !"STUDENT".equalsIgnoreCase(student.getRole().getName())
                || !assignmentStudentRepository.existsByIdAssignmentIdAndIdStudentId(assignmentId, student.getId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Assignment is not assigned to this student");
        validateStudents(assignment.getTeacher(), List.of(student.getId()));
        if ("CLOSED".equalsIgnoreCase(assignment.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bài tập đã đóng, không thể nộp thêm");
        }

        AssignmentSubmission submission = assignmentSubmissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElse(new AssignmentSubmission());

        submission.setAssignmentId(assignment.getId());
        submission.setAssignment(assignment);
        submission.setStudentId(student.getId());
        submission.setStudent(student);
        submission.setPredictions(request.getPredictions());
        submission.setSubmittedAt(OffsetDateTime.now());

        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);
        return toSubmissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getSubmissionsForAssignment(UUID assignmentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + assignmentId));

        boolean isAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName());
        if (!assignment.getTeacherId().equals(user.getId()) && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem danh sách bài nộp của bài tập này");
        }

        return assignmentSubmissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId).stream()
                .map(this::toSubmissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getMySubmissions(String userEmail) {
        User student = getUserByEmail(userEmail);
        return assignmentSubmissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId()).stream()
                .map(this::toSubmissionResponse)
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u.isEnabled() && Boolean.TRUE.equals(u.getActive()))
                .filter(u -> u.getSchool() == null || u.getSchool().isActive())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName());
    }

    private void requireTeacher(User user) {
        if (!isAdmin(user) && (user.getRole() == null || !"TEACHER".equalsIgnoreCase(user.getRole().getName())))
            throw new ApiException(HttpStatus.FORBIDDEN, "Teacher role is required");
    }

    private void requireReadAccess(User user, Assignment assignment) {
        if (isAdmin(user) || user.getId().equals(assignment.getTeacherId())) return;
        if (user.getRole() != null && "STUDENT".equalsIgnoreCase(user.getRole().getName())
                && assignmentStudentRepository.existsByIdAssignmentIdAndIdStudentId(assignment.getId(), user.getId())) {
            validateStudents(assignment.getTeacher(), List.of(user.getId()));
            return;
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Assignment access denied");
    }

    private void validateStudents(User teacher, List<Integer> ids) {
        if (ids == null) return;
        for (Integer id : ids.stream().distinct().toList()) {
            User student = userRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Student not found"));
            if (!student.isEnabled() || !Boolean.TRUE.equals(student.getActive())
                    || student.getRole() == null || !"STUDENT".equalsIgnoreCase(student.getRole().getName())
                    || student.getSchool() == null || !student.getSchool().isActive())
                throw new ApiException(HttpStatus.BAD_REQUEST, "Student is not active in a school");
            if (isAdmin(teacher)) continue;
            if (teacher.getSchool() == null || !teacher.getSchool().getId().equals(student.getSchool().getId()))
                throw new ApiException(HttpStatus.FORBIDDEN, "Teacher and student must belong to the same school");
            boolean assigned = enrollments.findActiveEnrollmentsByStudentId(id).stream()
                    .filter(e -> Boolean.TRUE.equals(e.getSchoolClass().getIsActive()))
                    .filter(e -> teacher.getSchool().getId().equals(e.getSchoolClass().getSchool().getId()))
                    .anyMatch(e -> teacherAssignments.existsBySchoolClassIdAndTeacherIdAndIsActiveTrue(e.getSchoolClass().getId(), teacher.getId()));
            if (!assigned) throw new ApiException(HttpStatus.FORBIDDEN, "Teacher is not assigned to the student's class");
        }
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        List<AssignmentStudent> assigned = assignmentStudentRepository.findByIdAssignmentId(assignment.getId());
        List<Integer> studentIds = assigned.stream().map(as -> as.getId().getStudentId()).collect(Collectors.toList());
        List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignment.getId());

        return AssignmentResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .status(assignment.getStatus())
                .assignedAt(assignment.getAssignedAt())
                .dueAt(assignment.getDueAt())
                .questions(assignment.getQuestions())
                .teacherId(assignment.getTeacherId())
                .teacherName(assignment.getTeacher() != null ? assignment.getTeacher().getFullName() : null)
                .specificationId(assignment.getSpecificationId())
                .assignedStudentCount(studentIds.size())
                .assignedStudentIds(studentIds)
                .submissionCount(submissions.size())
                .createdAt(assignment.getCreatedAt())
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    private AssignmentSubmissionResponse toSubmissionResponse(AssignmentSubmission sub) {
        return AssignmentSubmissionResponse.builder()
                .id(sub.getId())
                .assignmentId(sub.getAssignmentId())
                .assignmentTitle(sub.getAssignment() != null ? sub.getAssignment().getTitle() : null)
                .studentId(sub.getStudentId())
                .studentName(sub.getStudent() != null ? sub.getStudent().getFullName() : null)
                .studentEmail(sub.getStudent() != null ? sub.getStudent().getEmail() : null)
                .predictions(sub.getPredictions())
                .submittedAt(sub.getSubmittedAt())
                .createdAt(sub.getCreatedAt())
                .build();
    }
}
