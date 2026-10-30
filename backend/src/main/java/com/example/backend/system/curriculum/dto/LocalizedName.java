package com.example.backend.system.curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Stored in the single topics.name JSONB column; separate from the stable topic slug. */
public record LocalizedName(@NotBlank @Size(max = 80) String en, @NotBlank @Size(max = 80) String vi) {
    public LocalizedName {
        en = en == null ? null : en.trim();
        vi = vi == null ? null : vi.trim();
    }
}
