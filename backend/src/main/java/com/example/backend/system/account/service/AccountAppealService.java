package com.example.backend.system.account.service;

import com.example.backend.exception.ApiException;
import com.example.backend.system.account.dto.AppealContracts.*;
import com.example.backend.system.account.model.entity.AccountAppeal;
import com.example.backend.system.account.model.entity.User;
import com.example.backend.system.account.repository.AccountAppealRepository;
import com.example.backend.system.account.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountAppealService {
    private final AccountAppealRepository appealRepository;
    private final UserRepository userRepository;

    @Transactional
    public AppealResponse createAppeal(Authentication authentication, CreateAppealRequest request) {
        if (authentication == null) throw ApiException.unauthorized("Vui lòng đăng nhập để gửi khiếu nại");
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
        AccountAppeal appeal = new AccountAppeal();
        appeal.setUser(user);
        appeal.setReason(request.reason().trim());
        appeal.setContent(request.content().trim());
        appeal.setStatus("PENDING");
        appeal.setCreatedAt(Instant.now());
        return AppealResponse.from(appealRepository.save(appeal));
    }

    public List<AppealResponse> myAppeals(Authentication authentication) {
        if (authentication == null) throw ApiException.unauthorized("Vui lòng đăng nhập để xem khiếu nại");
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
        return appealRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(AppealResponse::from).toList();
    }

    public List<AppealResponse> allAppeals() {
        return appealRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(AppealResponse::from).toList();
    }

    @Transactional
    public AppealResponse resolveAppeal(UUID id, ResolveAppealRequest request) {
        AccountAppeal appeal = appealRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy khiếu nại"));
        String status = request.status().toUpperCase();
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw ApiException.badRequest("Trạng thái xử lý phải là APPROVED hoặc REJECTED");
        }
        appeal.setStatus(status);
        appeal.setResolutionNote(request.resolutionNote());
        appeal.setResolvedAt(Instant.now());

        if ("APPROVED".equals(status)) {
            User user = appeal.getUser();
            user.setAccountStatus("ACTIVE");
            user.setActive(true);
            user.setSuspensionReason(null);
            user.setSuspendedUntil(null);
            userRepository.save(user);
        }
        return AppealResponse.from(appealRepository.save(appeal));
    }
}
