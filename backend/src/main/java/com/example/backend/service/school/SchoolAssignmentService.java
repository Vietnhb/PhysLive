package com.example.backend.service.school;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.assignment.SchoolAssignmentResponse;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.AssignmentRepository;
import com.example.backend.repository.AssignmentStudentRepository;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.account.AccountAccessService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolAssignmentService {
    private final AssignmentRepository assignments;
    private final AssignmentStudentRepository recipients;
    private final CurrentUserService currentUser;
    private final AccountAccessService access;

    @Transactional(readOnly = true)
    public Page<SchoolAssignmentResponse> list(UUID schoolId, int page, int size) {
        User actor = currentUser.requireCurrentUser();
        if (!access.isDepartmentHead(actor) || actor.getSchool() == null
                || !schoolId.equals(actor.getSchool().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only a department head can view assignments in their school");
        }
        if (page < 0 || size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        }
        return assignments.findSchoolAssignments(schoolId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(item -> new SchoolAssignmentResponse(item.getId(), item.getTitle(),
                        item.getSchoolClass() == null ? null : item.getSchoolClass().getId(),
                        item.getSchoolClass() == null ? null : item.getSchoolClass().getName(),
                        item.getSchoolClass() == null ? null : item.getSchoolClass().getSchoolYear(),
                        item.getTeacherId(), item.getTeacher() == null ? null : item.getTeacher().getFullName(),
                        recipients.findByIdAssignmentId(item.getId()).size(),
                        item.getStatus(), item.getAssignedAt(), item.getDueAt()));
    }
}
