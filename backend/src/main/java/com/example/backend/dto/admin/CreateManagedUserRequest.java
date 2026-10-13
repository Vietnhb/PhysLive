package com.example.backend.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateManagedUserRequest(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String fullName,
        @NotBlank String role,
        String institutionId,
        @PastOrPresent LocalDate dateOfBirth,
        @Size(max = 2000) String avatarUrl,
        java.util.List<String> permissions) {
    public CreateManagedUserRequest(String email, String password, String fullName, String role, String institutionId) {
        this(email, password, fullName, role, institutionId, null, null, null);
    }
    public CreateManagedUserRequest(String email, String password, String fullName, String role,
                                    String institutionId, java.util.List<String> permissions) {
        this(email, password, fullName, role, institutionId, null, null, permissions);
    }
}
