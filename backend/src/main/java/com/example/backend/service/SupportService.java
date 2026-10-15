package com.example.backend.service;

import com.example.backend.dto.support.SupportContracts.CreateSupportRequest;
import com.example.backend.dto.support.SupportContracts.SupportView;
import com.example.backend.dto.support.SupportContracts.UpdateSupportRequest;
import com.example.backend.entity.SupportItem;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.RoleName;
import com.example.backend.entity.enums.SupportKind;
import com.example.backend.entity.enums.SupportStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.SupportItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportService {
    private final SupportItemRepository repository;
    private final CurrentUserService currentUser;

    @Transactional
    public SupportView create(SupportKind kind, CreateSupportRequest request) {
        if (kind == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Support request kind is required");
        }
        if (request == null || request.subject() == null || request.subject().isBlank()
                || request.content() == null || request.content().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subject and content are required");
        }
        String subject = request.subject().trim();
        String content = request.content().trim();
        if (subject.length() > 180 || content.length() > 10000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Subject or content exceeds the allowed length");
        }

        SupportItem item = new SupportItem();
        item.setKind(kind);
        item.setSender(requireSupportSender());
        item.setSubject(subject);
        item.setContent(content);
        item.setStatus(SupportStatus.OPEN);
        return view(repository.save(item));
    }

    @Transactional(readOnly = true)
    public List<SupportView> mine() {
        Integer senderId = requireSupportSender().getId();
        return repository.findBySenderIdOrderByCreatedAtDesc(senderId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<SupportView> adminList(SupportKind kind) {
        requireManager();
        if (kind == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Support request kind is required");
        }
        return repository.findTop200ByKindOrderByCreatedAtDesc(kind).stream().map(this::view).toList();
    }

    @Transactional
    public SupportView update(UUID id, UpdateSupportRequest request) {
        User manager = requireManager();
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Support update is required");
        }
        SupportItem item = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Support request not found"));
        SupportStatus status = request.status() == null ? item.getStatus() : request.status();
        String response = request.response() == null ? null : request.response().trim();
        if (response != null && response.length() > 10000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Support response exceeds the allowed length");
        }
        if (response != null && !response.isBlank()) {
            item.setAdminResponse(response);
            item.setRespondedBy(manager);
            item.setRespondedAt(Instant.now());
            if (status == SupportStatus.OPEN) status = SupportStatus.READ;
        }
        item.setStatus(status);
        return view(repository.save(item));
    }

    private SupportView view(SupportItem item) {
        User sender = item.getSender();
        return new SupportView(item.getId(), item.getKind(), sender.getId(), sender.getFullName(),
                sender.getEmail(), item.getSubject(), item.getContent(), item.getStatus(),
                item.getAdminResponse(), item.getCreatedAt(), item.getRespondedAt(),
                item.getRespondedBy() == null ? null : item.getRespondedBy().getFullName());
    }

    private User requireSupportSender() {
        User user = currentUser.requireCurrentUser();
        if (user.getRole() == null || !RoleName.STUDENT.matches(user.getRole().getName())
                && !RoleName.SCHOOL.matches(user.getRole().getName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Student or school role is required");
        }
        return user;
    }

    private User requireManager() {
        User user = currentUser.requireCurrentUser();
        if (user.getRole() == null || !RoleName.MANAGER.matches(user.getRole().getName())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Manager role is required");
        }
        return user;
    }
}
