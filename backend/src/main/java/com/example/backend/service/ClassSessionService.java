package com.example.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.ClassSessionResponse;
import com.example.backend.dto.CreateClassSessionRequest;
import com.example.backend.entity.ClassSession;
import com.example.backend.entity.Specification;
import com.example.backend.entity.User;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.ClassSessionRepository;
import com.example.backend.repository.SpecificationRepository;
import com.example.backend.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final SpecificationRepository specificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> getSessions(String userEmail) {
        User user = getUserByEmail(userEmail);
        String roleName = user.getRole() != null ? user.getRole().getName() : "STUDENT";

        List<ClassSession> list;
        if ("STAFF".equalsIgnoreCase(roleName)) {
            list = classSessionRepository.findByTeacherIdOrderByCreatedAtDesc(user.getId());
        } else {
            // Students and Admin can view all recorded classroom sessions
            list = classSessionRepository.findAll().stream().filter(session -> canView(user, session)).toList();
        }

        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClassSessionResponse getSessionById(UUID id, String userEmail) {
        User user = getUserByEmail(userEmail);
        ClassSession session = classSessionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên tương tác / buổi học với ID: " + id));
        if (!canView(user, session)) throw new ApiException(HttpStatus.NOT_FOUND, "Session not found");
        return toResponse(session);
    }

    @Transactional
    public ClassSessionResponse createSession(CreateClassSessionRequest request, String userEmail) {
        User teacher = getUserByEmail(userEmail);
        Specification spec = specificationRepository.findById(request.getSpecificationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy đặc tả mô phỏng (Specification) với ID: " + request.getSpecificationId()));

        if (!isAdmin(teacher) && (spec.getSubmission() == null || !teacher.getId().equals(spec.getSubmission().getOwner().getId())))
            throw new ApiException(HttpStatus.FORBIDDEN, "Specification belongs to another teacher");
        ClassSession session = new ClassSession();
        session.setTitle(request.getTitle().trim());
        session.setDurationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 0.0);
        session.setInitialSpecification(request.getInitialSpecification());
        session.setParameterTimeline(request.getParameterTimeline());
        session.setStorageUrl(request.getStorageUrl());
        session.setTeacherId(teacher.getId());
        session.setTeacher(teacher);
        session.setSpecificationId(spec.getId());
        session.setSpecification(spec);

        ClassSession saved = classSessionRepository.save(session);
        return toResponse(saved);
    }

    @Transactional
    public void deleteSession(UUID id, String userEmail) {
        User user = getUserByEmail(userEmail);
        ClassSession session = classSessionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy phiên tương tác / buổi học với ID: " + id));

        boolean isAdmin = user.getRole() != null && "MANAGER".equalsIgnoreCase(user.getRole().getName());
        if (!session.getTeacherId().equals(user.getId()) && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Bạn không có quyền xoá buổi học này");
        }

        classSessionRepository.delete(session);
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && "MANAGER".equalsIgnoreCase(user.getRole().getName());
    }

    private boolean canView(User user, ClassSession session) {
        if (isAdmin(user) || user.getId().equals(session.getTeacherId())) return true;
        return user.getSchool() != null && session.getTeacher() != null && session.getTeacher().getSchool() != null
                && user.getSchool().getId().equals(session.getTeacher().getSchool().getId());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));
    }

    private ClassSessionResponse toResponse(ClassSession session) {
        return ClassSessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .durationSeconds(session.getDurationSeconds())
                .initialSpecification(session.getInitialSpecification())
                .parameterTimeline(session.getParameterTimeline())
                .storageUrl(session.getStorageUrl())
                .teacherId(session.getTeacherId())
                .teacherName(session.getTeacher() != null ? session.getTeacher().getFullName() : null)
                .specificationId(session.getSpecificationId())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
