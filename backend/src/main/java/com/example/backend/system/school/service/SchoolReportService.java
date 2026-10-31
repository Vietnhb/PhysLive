package com.example.backend.system.school.service;

import com.example.backend.exception.ApiException;
import com.example.backend.system.account.model.enums.RoleName;
import com.example.backend.system.account.repository.UserRepository;
import com.example.backend.system.account.service.CurrentUserService;
import com.example.backend.system.account.service.RoleValidationService;
import com.example.backend.system.school.dto.SchoolReportContracts.ClassRow;
import com.example.backend.system.school.dto.SchoolReportContracts.Summary;
import com.example.backend.system.school.dto.SchoolReportContracts.TokenRow;
import com.example.backend.system.school.dto.SchoolReportContracts.TokenPage;
import com.example.backend.system.school.dto.SchoolReportContracts.TokenOptions;
import com.example.backend.system.school.dto.SchoolContracts;
import com.example.backend.system.school.model.entity.TokenUsageAudit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.time.YearMonth;
import java.time.LocalDate;
import java.util.Locale;
import com.example.backend.system.school.model.entity.School;
import com.example.backend.system.school.repository.ClassEnrollmentRepository;
import com.example.backend.system.school.repository.ClassTeacherAssignmentRepository;
import com.example.backend.system.school.repository.SchoolClassRepository;
import com.example.backend.system.school.repository.SchoolRepository;
import com.example.backend.system.school.repository.TokenUsageAuditRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolReportService {
    private final SchoolRepository schools;
    private final AiQuotaService aiQuota;
    private final SchoolClassRepository classes;
    private final ClassEnrollmentRepository enrollments;
    private final ClassTeacherAssignmentRepository teacherAssignments;
    private final UserRepository users;
    private final CurrentUserService currentUser;
    private final RoleValidationService roles;
    private final TokenUsageAuditRepository tokenAudits;

    public Summary summary(UUID schoolId) {
        requireAccess(schoolId); School school = school(schoolId);
        long teachers = users.findBySchoolIdAndRoleName(schoolId, RoleName.STAFF.name()).size();
        long students = users.countActiveStudents(schoolId);
        long managers = users.findBySchoolIdAndRoleName(schoolId, RoleName.SCHOOL.name()).size();
        long enrolled = enrollments.countActiveStudentsBySchoolId(schoolId);
        return new Summary(schoolId, school.getName(), students, teachers, managers,
                classes.findBySchoolIdAndIsActiveTrue(schoolId).size(), enrolled,
                aiQuota.usedThisMonth(school), school.getMonthlyTokenQuota(), school.getLicenseEnd(),
                aiQuota.currentMonth(), school.getStudentQuota(), school.getPlanCode(), school.getLicenseStart(),
                SchoolContracts.Response.from(school).subscriptionStatus());
    }

    public List<ClassRow> classes(UUID schoolId) {
        requireAccess(schoolId);
        return classes.findBySchoolIdAndIsActiveTrue(schoolId).stream().map(item -> new ClassRow(item.getId(), item.getName(), item.getGradeLevel(), item.getSchoolYear(),
                teacherAssignments.findByClassIdAndIsActiveTrue(item.getId()).size(), enrollments.findActiveStudentsByClassId(item.getId()).size())).toList();
    }

    public List<TokenRow> tokenAudit(UUID schoolId) {
        requireAccess(schoolId);
        return tokenAudits.findTop200BySchoolIdOrderByRecordedAtDesc(schoolId).stream()
                .map(row -> new TokenRow(row.getUser().getEmail(), row.getOperation(), row.getTokens(), row.getUsageMonth(), row.getRecordedAt())).toList();
    }

    public List<ClassRow> filteredClasses(UUID schoolId, Integer grade, String year, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return classes(schoolId).stream().filter(row -> grade == null || grade.equals(row.gradeLevel()))
                .filter(row -> year == null || year.isBlank() || year.equals(row.schoolYear()))
                .filter(row -> row.name().toLowerCase(Locale.ROOT).contains(needle)).toList();
    }

    public TokenOptions tokenOptions(UUID schoolId) {
        requireAccess(schoolId);
        return new TokenOptions(tokenAudits.operations(schoolId), tokenAudits.months(schoolId));
    }

    public TokenPage tokenPage(UUID schoolId, int page, int size, String query, String operation, String month) {
        requireAccess(schoolId);
        if (page < 0 || size < 1 || size > 100) throw ApiException.badRequest("Phân trang không hợp lệ (1–100 dòng/trang).");
        if ((query != null && query.length() > 200) || (operation != null && operation.length() > 64))
            throw ApiException.badRequest("Bộ lọc quá dài.");
        LocalDate usageMonth;
        try { usageMonth = month == null || month.isBlank() ? null : YearMonth.parse(month).atDay(1); }
        catch (java.time.format.DateTimeParseException invalid) { throw ApiException.badRequest("Tháng cần có dạng yyyy-MM."); }
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Specification<TokenUsageAudit> filter = (root, criteria, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("school").get("id"), schoolId));
            if (usageMonth != null) predicates.add(cb.equal(root.get("usageMonth"), usageMonth));
            if (operation != null && !operation.isBlank()) predicates.add(cb.equal(root.get("operation"), operation));
            if (!needle.isEmpty()) {
                String escaped = needle.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.get("user").get("email")), "%" + escaped + "%", '\\'));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Sort sort = Sort.by(Sort.Order.desc("recordedAt"), Sort.Order.desc("id"));
        var result = tokenAudits.findAll(filter, PageRequest.of(page, size, sort));
        if (result.getTotalElements() > 0 && page >= result.getTotalPages())
            result = tokenAudits.findAll(filter, PageRequest.of(result.getTotalPages() - 1, size, sort));
        var items = result.getContent().stream().map(row -> new TokenRow(row.getUser().getEmail(), row.getOperation(),
                row.getTokens(), row.getUsageMonth(), row.getRecordedAt())).toList();
        return new TokenPage(items, result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    private School school(UUID schoolId) { return schools.findById(schoolId).orElseThrow(() -> ApiException.notFound("Không tìm thấy trường")); }
    private void requireAccess(UUID schoolId) {
        if (!roles.canManageSchool(currentUser.requireCurrentUser(), schoolId)) {
            throw ApiException.forbidden("Bạn không có quyền truy cập trường này");
        }
    }

}
