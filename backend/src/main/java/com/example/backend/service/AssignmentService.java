package com.example.backend.service;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.example.backend.dto.assignment.AssignmentReportResponse;
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
    private final com.example.backend.repository.school.SchoolClassRepository schoolClasses;

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments(String userEmail) {
        User user = getUserByEmail(userEmail);
        String roleName = user.getRole() != null ? user.getRole().getName() : "STUDENT";

        List<Assignment> list;
        if ("STUDENT".equalsIgnoreCase(roleName)) {
            list = assignmentRepository.findAssignmentsForStudent(user.getId());
        } else if ("STAFF".equalsIgnoreCase(roleName)) {
            list = assignmentRepository.findByTeacherIdOrderByCreatedAtDesc(user.getId());
        } else if ("MANAGER".equalsIgnoreCase(roleName)) {
            list = assignmentRepository.findAll();
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "Teaching role is required");
        }

        return list.stream().map(item -> toResponse(item, user)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(UUID id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + id));

        requireReadAccess(user, assignment);
        return toResponse(assignment, user);
    }

    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request, String userEmail) {
        User teacher = getUserByEmail(userEmail);
        requireTeacher(teacher);
        licenses.requireWriteAccess(teacher);
        validateStudents(teacher, request.getStudentIds());
        if (request.getClassId() == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a class before assigning this activity");
        var targetClass = schoolClasses.findById(request.getClassId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Class not found"));
        validateClass(teacher, targetClass, request.getStudentIds());
        Specification spec = specificationRepository.findById(request.getSpecificationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy đặc tả mô phỏng (Specification) với ID: " + request.getSpecificationId()));

        if (!isAdmin(teacher) && (spec.getSubmission() == null
                || !teacher.getId().equals(spec.getSubmission().getOwner().getId())))
            throw new ApiException(HttpStatus.FORBIDDEN, "Specification belongs to another teacher");
        Assignment assignment = new Assignment();
        assignment.setSchoolClass(targetClass);
        assignment.setTitle(request.getTitle().trim());
        assignment.setDescription(request.getDescription());
        assignment.setStatus("ACTIVE");
        assignment.setAssignedAt(OffsetDateTime.now());
        assignment.setDueAt(request.getDueAt());
        assignment.setQuestions(request.getQuestions() != null ? request.getQuestions() : Collections.emptyMap());
        assignment.setGradingCriteria(request.getGradingCriteria());
        assignment.setMaxScore(request.getMaxScore() == null ? BigDecimal.TEN : request.getMaxScore());
        assignment.setAutoGrade(Boolean.TRUE.equals(request.getAutoGrade()));
        validateGrading(assignment);
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

        return toResponse(saved, teacher);
    }

    @Transactional
    public AssignmentResponse updateAssignment(UUID id, UpdateAssignmentRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + id));

        boolean isAdmin = user.getRole() != null && "MANAGER".equalsIgnoreCase(user.getRole().getName());
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
            validateClass(assignment.getTeacher(), assignment.getSchoolClass(), request.getStudentIds());
            assignmentStudentRepository.deleteByIdAssignmentId(id);
            List<AssignmentStudent> students = request.getStudentIds().stream()
                    .map(sId -> new AssignmentStudent(id, sId))
                    .collect(Collectors.toList());
            assignmentStudentRepository.saveAll(students);
        }

        Assignment updated = assignmentRepository.save(assignment);
        return toResponse(updated, user);
    }

    @Transactional
    public AssignmentResponse updateStatus(UUID id, String status, String userEmail) {
        User user = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + id));

        boolean isAdmin = user.getRole() != null && "MANAGER".equalsIgnoreCase(user.getRole().getName());
        if (!assignment.getTeacherId().equals(user.getId()) && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền thay đổi trạng thái bài tập này");
        }

        licenses.requireWriteAccess(user);
        assignment.setStatus(status.toUpperCase());
        Assignment updated = assignmentRepository.save(assignment);
        return toResponse(updated, user);
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
        validateClass(assignment.getTeacher(), assignment.getSchoolClass(), List.of(student.getId()));
        if ("CLOSED".equalsIgnoreCase(assignment.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bài tập đã đóng, không thể nộp thêm");
        }

        AssignmentSubmission submission = assignmentSubmissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElse(new AssignmentSubmission());

        if (submission.getId() != null && submission.getCompletedAt() != null && !submission.isRetryAllowed()) {
            throw new ApiException(HttpStatus.CONFLICT, "The assignment has already been submitted");
        }

        submission.setAssignmentId(assignment.getId());
        submission.setAssignment(assignment);
        submission.setStudentId(student.getId());
        submission.setStudent(student);
        submission.setPredictions(request.getPredictions());
        OffsetDateTime now = OffsetDateTime.now();
        submission.setSubmittedAt(now);
        submission.setCompletedAt(now);
        submission.setMaxScore(assignment.getMaxScore());
        submission.setRetryAllowed(false);
        submission.setGradedBy(null);
        submission.setGradedAt(null);
        submission.setFeedback(null);
        if (assignment.isAutoGrade()) {
            submission.setScore(autoGrade(assignment, request.getPredictions()));
            submission.setGradingStatus("AI_GRADED");
        } else {
            submission.setScore(null);
            submission.setGradingStatus("PENDING");
        }

        AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);
        return toSubmissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getSubmissionsForAssignment(UUID assignmentId, String userEmail) {
        User user = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài tập với ID: " + assignmentId));

        boolean isAdmin = user.getRole() != null && "MANAGER".equalsIgnoreCase(user.getRole().getName());
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

    @Transactional
    public AssignmentSubmissionResponse gradeSubmission(UUID assignmentId, UUID submissionId,
            BigDecimal score, String feedback, boolean confirm, String userEmail) {
        User teacher = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
        requireOwnerOrManager(teacher, assignment);
        licenses.requireWriteAccess(teacher);
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                .filter(item -> assignmentId.equals(item.getAssignmentId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
        BigDecimal maxScore = assignment.getMaxScore() == null ? BigDecimal.TEN : assignment.getMaxScore();
        if (score == null || score.signum() < 0 || score.compareTo(maxScore) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Score must be between 0 and " + maxScore);
        }
        submission.setScore(score);
        submission.setMaxScore(maxScore);
        submission.setFeedback(feedback);
        submission.setGradingStatus(confirm ? "TEACHER_CONFIRMED" : "AI_GRADED");
        submission.setGradedAt(OffsetDateTime.now());
        submission.setGradedBy(teacher.getId());
        submission.setRetryAllowed(false);
        return toSubmissionResponse(assignmentSubmissionRepository.save(submission));
    }

    @Transactional
    public AssignmentSubmissionResponse reopenSubmission(UUID assignmentId, UUID submissionId, String userEmail) {
        User teacher = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
        requireOwnerOrManager(teacher, assignment);
        licenses.requireWriteAccess(teacher);
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
                .filter(item -> assignmentId.equals(item.getAssignmentId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submission not found"));
        submission.setRetryAllowed(true);
        submission.setGradingStatus("RETURNED");
        submission.setGradedAt(OffsetDateTime.now());
        submission.setGradedBy(teacher.getId());
        return toSubmissionResponse(assignmentSubmissionRepository.save(submission));
    }

    @Transactional(readOnly = true)
    public AssignmentReportResponse report(UUID assignmentId, String userEmail) {
        User teacher = getUserByEmail(userEmail);
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
        requireOwnerOrManager(teacher, assignment);
        List<AssignmentSubmission> rows = assignmentSubmissionRepository
                .findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);
        long completed = rows.stream().filter(row -> row.getCompletedAt() != null).count();
        List<AssignmentSubmission> scored = rows.stream().filter(row -> row.getScore() != null).toList();
        BigDecimal total = scored.stream().map(AssignmentSubmission::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long confirmed = rows.stream().filter(row -> "TEACHER_CONFIRMED".equals(row.getGradingStatus())).count();
        int assigned = assignmentStudentRepository.findByIdAssignmentId(assignmentId).size();
        return new AssignmentReportResponse(assigned, completed, Math.max(0, assigned - completed),
                scored.size(), confirmed, rows.stream().filter(AssignmentSubmission::isRetryAllowed).count(),
                scored.isEmpty() ? null : total.divide(BigDecimal.valueOf(scored.size()), 3, RoundingMode.HALF_UP),
                assignment.getMaxScore() == null ? BigDecimal.TEN : assignment.getMaxScore());
    }

    private void requireOwnerOrManager(User user, Assignment assignment) {
        if (!isAdmin(user) && !assignment.getTeacherId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the assigning teacher can manage submissions");
        }
    }

    private void validateGrading(Assignment assignment) {
        if (assignment.getMaxScore() == null || assignment.getMaxScore().signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Maximum score must be greater than zero");
        }
        if (!assignment.isAutoGrade()) return;
        Map<String, Object> criteria = assignment.getGradingCriteria();
        if (criteria == null || number(criteria.get("expectedValue")) == null
                || number(criteria.get("tolerance")) == null || number(criteria.get("tolerance")) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Auto grading requires a numeric expectedValue and a non-negative tolerance");
        }
    }

    private BigDecimal autoGrade(Assignment assignment, Map<String, Object> predictions) {
        Double expected = number(assignment.getGradingCriteria().get("expectedValue"));
        Double tolerance = number(assignment.getGradingCriteria().get("tolerance"));
        Double actual = number(predictions == null ? null : predictions.get("estimatedValue"));
        if (actual == null && predictions != null) actual = number(predictions.get("answerText"));
        return actual != null && Math.abs(actual - expected) <= tolerance
                ? assignment.getMaxScore() : BigDecimal.ZERO;
    }

    private static Double number(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String text) {
            try { return Double.valueOf(text.trim().replace(',', '.')); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u.isEnabled() && Boolean.TRUE.equals(u.getActive()))
                .filter(u -> u.getSchool() == null || u.getSchool().isActive())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && "MANAGER".equalsIgnoreCase(user.getRole().getName());
    }

    private void requireTeacher(User user) {
        if (!isAdmin(user) && (user.getRole() == null || !"STAFF".equalsIgnoreCase(user.getRole().getName())))
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

    private void validateClass(User teacher, com.example.backend.entity.school.SchoolClass target, List<Integer> ids) {
        // Existing assignments have no class link; retain their legacy access checks.
        if (target == null) return;
        if (!Boolean.TRUE.equals(target.getIsActive()) || target.getSchool() == null || !target.getSchool().isActive())
            throw new ApiException(HttpStatus.FORBIDDEN, "Class is not active");
        if (!isAdmin(teacher) && (teacher.getSchool() == null
                || !teacher.getSchool().getId().equals(target.getSchool().getId())
                || !teacherAssignments.existsBySchoolClassIdAndTeacherIdAndIsActiveTrue(target.getId(), teacher.getId())))
            throw new ApiException(HttpStatus.FORBIDDEN, "Teacher is not assigned to the selected class");
        if (ids == null || ids.isEmpty())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select at least one student");
        var members = enrollments.findActiveStudentsByClassId(target.getId()).stream()
                .filter(e -> e.getStudent().getSchool() != null
                        && target.getSchool().getId().equals(e.getStudent().getSchool().getId()))
                .map(e -> e.getStudent().getId()).collect(Collectors.toSet());
        if (!members.containsAll(ids))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Every selected student must belong to the selected class");
    }

    private AssignmentResponse toResponse(Assignment assignment, User viewer) {
        List<AssignmentStudent> assigned = assignmentStudentRepository.findByIdAssignmentId(assignment.getId());
        List<Integer> studentIds = assigned.stream().map(as -> as.getId().getStudentId()).collect(Collectors.toList());
        List<AssignmentSubmission> submissions = assignmentSubmissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignment.getId());
        boolean studentViewer = viewer != null && viewer.getRole() != null
                && "STUDENT".equalsIgnoreCase(viewer.getRole().getName());
        AssignmentSubmission own = studentViewer
                ? assignmentSubmissionRepository.findByAssignmentIdAndStudentId(assignment.getId(), viewer.getId()).orElse(null)
                : null;

        return AssignmentResponse.builder()
                .classId(assignment.getSchoolClass() == null ? null : assignment.getSchoolClass().getId())
                .className(assignment.getSchoolClass() == null ? null : assignment.getSchoolClass().getName())
                .classGradeLevel(assignment.getSchoolClass() == null ? null : assignment.getSchoolClass().getGradeLevel())
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
                .predictionSubmitted(own != null)
                .submissionCompleted(own != null && own.getCompletedAt() != null)
                .completedAt(own == null ? null : own.getCompletedAt())
                .gradingCriteria(studentViewer ? null : assignment.getGradingCriteria())
                .maxScore(assignment.getMaxScore())
                .autoGrade(assignment.isAutoGrade())
                .score(own == null ? null : own.getScore())
                .feedback(own == null ? null : own.getFeedback())
                .gradingStatus(own == null ? null : own.getGradingStatus())
                .retryAllowed(own != null && own.isRetryAllowed())
                .predictions(own == null ? null : own.getPredictions())
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
                .completedAt(sub.getCompletedAt())
                .score(sub.getScore())
                .maxScore(sub.getMaxScore())
                .feedback(sub.getFeedback())
                .gradingStatus(sub.getGradingStatus())
                .gradedAt(sub.getGradedAt())
                .retryAllowed(sub.isRetryAllowed())
                .createdAt(sub.getCreatedAt())
                .build();
    }
}
