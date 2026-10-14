package com.example.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.LibraryItemResponse;
import com.example.backend.dto.library.LibraryModerationContracts;
import com.example.backend.entity.enums.LibraryModerationStatus;
import com.example.backend.service.LibraryModerationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reviewer/library")
@PreAuthorize("hasAnyRole('REVIEWER', 'MANAGER')")
@RequiredArgsConstructor
public class LibraryModerationController {
    private final LibraryModerationService service;

    @GetMapping
    public List<LibraryItemResponse> queue(@RequestParam(required = false) LibraryModerationStatus status) {
        return service.publicQueue(status);
    }

    @PutMapping("/{id}")
    public LibraryItemResponse moderate(@PathVariable UUID id,
            @Valid @RequestBody LibraryModerationContracts.Request request) {
        return service.moderatePublic(id, request.status(), request.comment());
    }

    @GetMapping("/{id}/history")
    public List<LibraryModerationContracts.Audit> history(@PathVariable UUID id) {
        return service.history(id);
    }
}
