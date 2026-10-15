package com.example.backend.service;

import com.example.backend.entity.LibraryComment;
import com.example.backend.entity.LibraryItem;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.LibraryModerationStatus;
import com.example.backend.entity.school.School;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.LibraryCommentRepository;
import com.example.backend.repository.LibraryItemRepository;
import com.example.backend.service.account.AccountAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LibraryDiscussionServiceTest {
    private final LibraryItemRepository items = mock(LibraryItemRepository.class);
    private final LibraryCommentRepository comments = mock(LibraryCommentRepository.class);
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final AccountAccessService access = mock(AccountAccessService.class);
    private final LibraryDiscussionService service =
            new LibraryDiscussionService(items, comments, currentUser, access);
    private final UUID schoolId = UUID.randomUUID();

    @Test
    void anonymousCanReadOnlyPublishedPublicDiscussion() {
        LibraryItem item = item("PUBLIC");
        LibraryComment comment = comment(item, user(7, "STAFF", schoolId));
        comment.getAuthor().setAvatarUrl("https://example.com/avatar.jpg");
        when(items.findById(item.getId())).thenReturn(Optional.of(item));
        var page = PageRequest.of(0, 20);
        when(comments.findByItemIdOrderByCreatedAtDescIdDesc(item.getId(), page))
                .thenReturn(new SliceImpl<>(List.of(comment), page, false));
        when(items.countLikes(item.getId())).thenReturn(3L);
        when(comments.countByItemId(item.getId())).thenReturn(8L);

        var result = service.discussion(item.getId(), 0, 20);

        assertThat(result.likes()).isEqualTo(3);
        assertThat(result.commentCount()).isEqualTo(8);
        assertThat(result.canInteract()).isFalse();
        assertThat(result.comments()).singleElement().satisfies(value -> {
            assertThat(value.authorId()).isEqualTo(7);
            assertThat(value.avatarUrl()).isEqualTo("https://example.com/avatar.jpg");
            assertThat(value.canDelete()).isFalse();
        });
        verify(items, never()).hasLiked(any(), any());

        item.setVisibility("SHARED");
        assertStatus(HttpStatus.NOT_FOUND, () -> service.discussion(item.getId(), 0, 20));
    }

    @Test
    void studentCanCommentOnPublishedContentInOwnSchool() {
        User student = user(4, "STUDENT", schoolId);
        LibraryItem item = item("SHARED");
        when(currentUser.requireCurrentUser()).thenReturn(student);
        when(items.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(comments.save(any())).thenAnswer(call -> {
            LibraryComment saved = call.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(OffsetDateTime.now());
            return saved;
        });

        var result = service.comment(item.getId(), "  Question about force?  ");

        assertThat(result.body()).isEqualTo("Question about force?");
        assertThat(result.authorId()).isEqualTo(student.getId());
        assertThat(result.canDelete()).isTrue();
    }

    @Test
    void otherSchoolAndUnpublishedResourcesAreHidden() {
        User student = user(4, "STUDENT", UUID.randomUUID());
        LibraryItem item = item("SHARED");
        when(currentUser.requireCurrentUser()).thenReturn(student);
        when(items.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        assertStatus(HttpStatus.NOT_FOUND, () -> service.comment(item.getId(), "Valid"));

        item.setVisibility("PUBLIC");
        item.setActive(false);
        assertStatus(HttpStatus.NOT_FOUND, () -> service.react(item.getId(), true));
        verifyNoInteractions(comments);
        verify(items, never()).addLike(any(), any());
    }

    @Test
    void paginationAndCommentLimitsAreBounded() {
        ReflectionTestUtils.setField(service, "maxPageSize", 25);
        ReflectionTestUtils.setField(service, "maxCommentCharacters", 12);
        when(currentUser.requireCurrentUser()).thenReturn(user(4, "STUDENT", schoolId));

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.discussion(UUID.randomUUID(), -1, 20));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.discussion(UUID.randomUUID(), 0, 26));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.comment(UUID.randomUUID(), " "));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.comment(UUID.randomUUID(), "x".repeat(13)));
        verifyNoInteractions(items, comments);
    }

    @Test
    void desiredReactionStateIsIdempotent() {
        User student = user(4, "STUDENT", schoolId);
        LibraryItem item = item("PUBLIC");
        when(currentUser.requireCurrentUser()).thenReturn(student);
        when(items.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(items.countLikes(item.getId())).thenReturn(1L, 1L, 0L, 0L);

        assertThat(service.react(item.getId(), true).liked()).isTrue();
        assertThat(service.react(item.getId(), true).likes()).isEqualTo(1);
        assertThat(service.react(item.getId(), false).liked()).isFalse();
        assertThat(service.react(item.getId(), false).likes()).isZero();
        verify(items, times(2)).addLike(item.getId(), student.getId());
        verify(items, times(2)).removeLike(item.getId(), student.getId());
    }

    @Test
    void onlyOwnerOrAuthorizedModeratorCanDeleteComment() {
        User student = user(4, "STUDENT", schoolId);
        LibraryItem item = item("PUBLIC");
        LibraryComment own = comment(item, student);
        when(currentUser.requireCurrentUser()).thenReturn(student);
        when(items.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(comments.findByIdAndItemId(own.getId(), item.getId())).thenReturn(Optional.of(own));

        service.removeComment(item.getId(), own.getId());
        verify(comments).delete(own);

        LibraryComment another = comment(item, user(7, "STAFF", schoolId));
        when(comments.findByIdAndItemId(another.getId(), item.getId())).thenReturn(Optional.of(another));
        assertStatus(HttpStatus.FORBIDDEN, () -> service.removeComment(item.getId(), another.getId()));
    }

    @Test
    void adminAndInitialPasswordAccountsCannotInteract() {
        for (User user : List.of(user(1, "ADMIN", schoolId), user(2, "STUDENT", schoolId))) {
            if (user.getId() == 2) user.setMustChangePassword(true);
            when(currentUser.requireCurrentUser()).thenReturn(user);
            assertStatus(HttpStatus.FORBIDDEN, () -> service.react(UUID.randomUUID(), true));
        }
        verifyNoInteractions(items, comments);
    }

    private User user(int id, String roleName, UUID membershipId) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setFullName("User " + id);
        user.setRole(role);
        user.setEnabled(true);
        user.setActive(true);
        if (membershipId != null) {
            School school = new School();
            school.setId(membershipId);
            user.setSchool(school);
        }
        return user;
    }

    private LibraryItem item(String visibility) {
        LibraryItem item = new LibraryItem();
        item.setId(UUID.randomUUID());
        item.setOwner(user(7, "STAFF", schoolId));
        item.setOwnerId(7);
        item.setVisibility(visibility);
        item.setSharedInstitutionId("SHARED".equals(visibility) ? schoolId.toString() : null);
        item.setModerationStatus(LibraryModerationStatus.APPROVED);
        item.setActive(true);
        return item;
    }

    private LibraryComment comment(LibraryItem item, User author) {
        LibraryComment comment = new LibraryComment();
        comment.setId(UUID.randomUUID());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setBody("Comment");
        comment.setCreatedAt(OffsetDateTime.now());
        return comment;
    }

    private void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertThatThrownBy(action::execute)
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getStatus()).isEqualTo(status));
    }
}
