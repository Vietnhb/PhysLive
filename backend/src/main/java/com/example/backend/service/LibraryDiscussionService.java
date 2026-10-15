package com.example.backend.service;

import com.example.backend.dto.library.LibraryDiscussion;
import com.example.backend.entity.LibraryComment;
import com.example.backend.entity.LibraryItem;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.RoleName;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.LibraryCommentRepository;
import com.example.backend.repository.LibraryItemRepository;
import com.example.backend.service.account.AccountAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LibraryDiscussionService {
    private final LibraryItemRepository items;
    private final LibraryCommentRepository comments;
    private final CurrentUserService currentUser;
    private final AccountAccessService access;

    @Value("${physlive.community.comment-max-characters:2000}")
    private int maxCommentCharacters = 2000;

    @Value("${physlive.community.max-page-size:50}")
    private int maxPageSize = 50;

    @Transactional(readOnly = true)
    public LibraryDiscussion.Discussion discussion(UUID itemId, int page, int size) {
        if (page < 0 || size < 1 || size > maxPageSize) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid comment pagination");
        }
        User viewer = currentUser.currentUserOrNull();
        requirePublishedItem(itemId, viewer, false);
        var slice = comments.findByItemIdOrderByCreatedAtDescIdDesc(itemId, PageRequest.of(page, size));
        return new LibraryDiscussion.Discussion(items.countLikes(itemId),
                viewer != null && items.hasLiked(itemId, viewer.getId()), comments.countByItemId(itemId),
                slice.stream().map(comment -> toResponse(comment, viewer)).toList(), page, slice.hasNext(),
                canInteract(viewer), maxCommentCharacters);
    }

    @Transactional
    public LibraryDiscussion.Comment comment(UUID itemId, String body) {
        User author = requireParticipant();
        String text = body == null ? "" : body.strip();
        if (text.isBlank() || text.length() > maxCommentCharacters) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Comment must contain between 1 and " + maxCommentCharacters + " characters");
        }
        LibraryItem item = requirePublishedItem(itemId, author, true);
        LibraryComment comment = new LibraryComment();
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setBody(text);
        return toResponse(comments.save(comment), author);
    }

    @Transactional
    public void removeComment(UUID itemId, UUID commentId) {
        User viewer = requireParticipant();
        requirePublishedItem(itemId, viewer, true);
        LibraryComment comment = comments.findByIdAndItemId(commentId, itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!canDelete(comment, viewer)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot delete this comment");
        }
        comments.delete(comment);
    }

    @Transactional
    public LibraryDiscussion.Reaction react(UUID itemId, boolean liked) {
        User viewer = requireParticipant();
        requirePublishedItem(itemId, viewer, true);
        if (liked) items.addLike(itemId, viewer.getId());
        else items.removeLike(itemId, viewer.getId());
        return new LibraryDiscussion.Reaction(items.countLikes(itemId), liked);
    }

    private LibraryItem requirePublishedItem(UUID id, User viewer, boolean forUpdate) {
        return (forUpdate ? items.findByIdForUpdate(id) : items.findById(id))
                .filter(item -> LibraryService.isPublishedFor(viewer, item))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Community resource not found"));
    }

    private User requireParticipant() {
        User user = currentUser.requireCurrentUser();
        if (!canInteract(user)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account cannot interact with community resources");
        }
        return user;
    }

    private boolean canInteract(User user) {
        return user != null && user.isEnabled() && Boolean.TRUE.equals(user.getActive())
                && !user.isMustChangePassword() && user.getRole() != null
                && RoleName.from(user.getRole().getName()).filter(role -> role != RoleName.ADMIN).isPresent();
    }

    private boolean canDelete(LibraryComment comment, User viewer) {
        if (!canInteract(viewer)) return false;
        LibraryItem item = comment.getItem();
        return comment.getAuthor().getId().equals(viewer.getId())
                || RoleName.MANAGER.matches(viewer.getRole().getName())
                || "PUBLIC".equalsIgnoreCase(item.getVisibility()) && access.canReviewPublic(viewer)
                || "SHARED".equalsIgnoreCase(item.getVisibility()) && access.isDepartmentHead(viewer)
                    && LibraryService.sharedWithSchool(viewer, item);
    }

    private LibraryDiscussion.Comment toResponse(LibraryComment comment, User viewer) {
        User author = comment.getAuthor();
        return new LibraryDiscussion.Comment(comment.getId(), author.getId(), author.getFullName(),
                author.getAvatarUrl(), comment.getBody(), comment.getCreatedAt(), canDelete(comment, viewer));
    }
}
