package com.example.backend.dto.library;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Request and response contracts for a published library item's discussion. */
public final class LibraryDiscussion {
    private LibraryDiscussion() {
    }

    public record CommentRequest(@NotBlank String body) {
    }

    public record ReactionRequest(@NotNull Boolean liked) {
    }

    public record Comment(UUID id, Integer authorId, String authorName, String avatarUrl,
                          String body, OffsetDateTime createdAt, boolean canDelete) {
    }

    public record Reaction(long likes, boolean liked) {
    }

    public record Discussion(long likes, boolean liked, long commentCount, List<Comment> comments,
                             int page, boolean hasMore, boolean canInteract, int commentMaxLength) {
    }
}
