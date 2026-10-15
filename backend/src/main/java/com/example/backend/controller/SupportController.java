package com.example.backend.controller;

import com.example.backend.dto.support.SupportContracts;
import com.example.backend.dto.support.SupportContracts.CreateSupportRequest;
import com.example.backend.dto.support.SupportContracts.UpdateSupportRequest;
import com.example.backend.entity.enums.SupportKind;
import com.example.backend.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {
    private final SupportService service;

    @PostMapping("/feedback")
    @PreAuthorize("hasAnyRole('SCHOOL','STUDENT')")
    public SupportContracts.SupportView feedback(@Valid @RequestBody CreateSupportRequest request) {
        return service.create(SupportKind.FEEDBACK, request);
    }

    @PostMapping("/messages")
    @PreAuthorize("hasAnyRole('SCHOOL','STUDENT')")
    public SupportContracts.SupportView message(@Valid @RequestBody CreateSupportRequest request) {
        return service.create(SupportKind.MESSAGE, request);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('SCHOOL','STUDENT')")
    public List<SupportContracts.SupportView> mine() {
        return service.mine();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('MANAGER')")
    public List<SupportContracts.SupportView> adminList(@RequestParam SupportKind kind) {
        return service.adminList(kind);
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public SupportContracts.SupportView update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateSupportRequest request) {
        return service.update(id, request);
    }
}
