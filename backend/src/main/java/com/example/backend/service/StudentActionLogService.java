package com.example.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.assignment.StudentActionLogRequest;
import com.example.backend.dto.assignment.StudentActionLogResponse;
import com.example.backend.entity.StudentActionLog;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.AssignmentStudentRepository;
import com.example.backend.repository.StudentActionLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentActionLogService {
    private final StudentActionLogRepository logs;
    private final AssignmentStudentRepository assignmentStudents;
    private final CurrentUserService currentUser;

    @Transactional
    public StudentActionLogResponse create(StudentActionLogRequest request) {
        User student = currentUser.requireAnyRole("STUDENT");
        if (!assignmentStudents.existsByIdAssignmentIdAndIdStudentId(request.assignmentId(), student.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Assignment is not assigned to this student");
        }
        StudentActionLog log = new StudentActionLog();
        log.setStudentId(student.getId());
        log.setAssignmentId(request.assignmentId());
        log.setAction(request.action().trim());
        log.setPayload(request.payload());
        return StudentActionLogResponse.from(logs.save(log));
    }

    @Transactional(readOnly = true)
    public List<StudentActionLogResponse> mine() {
        User student = currentUser.requireAnyRole("STUDENT");
        return logs.findTop200ByStudentIdOrderByOccurredAtDesc(student.getId()).stream()
                .map(StudentActionLogResponse::from).toList();
    }
}
