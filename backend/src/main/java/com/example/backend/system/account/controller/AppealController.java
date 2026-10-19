package com.example.backend.system.account.controller;

import com.example.backend.system.account.dto.AppealContracts.*;
import com.example.backend.system.account.service.AccountAppealService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppealController {
    private final AccountAppealService appeals;

    @PostMapping("/appeals")
    public AppealResponse createAppeal(Authentication authentication, @Valid @RequestBody CreateAppealRequest request) {
        return appeals.createAppeal(authentication, request);
    }

    @GetMapping("/appeals/my")
    public List<AppealResponse> myAppeals(Authentication authentication) {
        return appeals.myAppeals(authentication);
    }

    @GetMapping("/admin/appeals")
    public List<AppealResponse> allAppeals() { return appeals.allAppeals(); }

    @PutMapping("/admin/appeals/{id}/resolve")
    public AppealResponse resolveAppeal(@PathVariable UUID id, @Valid @RequestBody ResolveAppealRequest request) {
        return appeals.resolveAppeal(id, request);
    }
}
