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

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments(String userEmail) {
        User user = getUserByEmail(userEmail);
        String roleName = user.getRole() != null ? user.getRole().getName() : "STUDENT";

        List<Assignment> list;
        if ("STUDENT".equalsIgnoreCase(roleName)) {
            list = assignmentRepository.findAssignmentsForStudent(user.getId());
        } else if ("TEACHER".equalsIgnoreCase(roleName)) {
            list = assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(user.getId());
        } else {
            // ADMIN sees all
            list = assignmentRepository.findAll();
        }

        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(UUID id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + id));

        return toResponse(assignment);
    }

    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, String userEmail) {
        User teacher = getUserByEmail(userEmail);
        Specification spec = specificationRepository.findById(request.getSpecificationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy đặc tả mô phỏng (Specification) với ID: " + request.getSpecificationId()));

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

        assignment.setStatus(status.toUpperCase());
        Assignment updated = assignmentRepository.save(assignment);
        return toResponse(updated);
    }

    @Transactional
    public AssignmentSubmissionResponse submitAssignment(UUID assignmentId, SubmitAssignmentRequest request, String userEmail) {
        User student = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + assignmentId));

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
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));
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
