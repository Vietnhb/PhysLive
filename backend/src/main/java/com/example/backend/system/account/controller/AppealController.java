package com.example.backend.system.account.controller;

import com.example.backend.exception.ApiException;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppealController {
    private final AccountAppealRepository appealRepository;
    private final UserRepository userRepository;

    public record CreateAppealRequest(@NotBlank String reason, @NotBlank String content) { }
    public record ResolveAppealRequest(@NotBlank String status, String resolutionNote) { }
    public record AppealResponse(UUID id, Integer userId, String userEmail, String userName,
                                  String reason, String content, String status, String resolutionNote,
                                  Instant createdAt, Instant resolvedAt) {
        public static AppealResponse from(AccountAppeal item) {
            return new AppealResponse(item.getId(), item.getUser().getId(), item.getUser().getEmail(),
                    item.getUser().getFullName(), item.getReason(), item.getContent(),
                    item.getStatus(), item.getResolutionNote(), item.getCreatedAt(), item.getResolvedAt());
        }
    }

    @PostMapping("/appeals")
    @Transactional
    public AppealResponse createAppeal(Authentication authentication, @Valid @RequestBody CreateAppealRequest request) {
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

    @GetMapping("/appeals/my")
    public List<AppealResponse> myAppeals(Authentication authentication) {
        if (authentication == null) throw ApiException.unauthorized("Vui lòng đăng nhập để xem khiếu nại");
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
        return appealRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(AppealResponse::from).toList();
    }

    @GetMapping("/admin/appeals")
    public List<AppealResponse> allAppeals() {
        return appealRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(AppealResponse::from).toList();
    }

    @PutMapping("/admin/appeals/{id}/resolve")
    @Transactional
    public AppealResponse resolveAppeal(@PathVariable UUID id, @Valid @RequestBody ResolveAppealRequest request) {
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
