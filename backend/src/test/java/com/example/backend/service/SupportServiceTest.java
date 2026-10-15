package com.example.backend.service;

import com.example.backend.dto.support.SupportContracts.CreateSupportRequest;
import com.example.backend.dto.support.SupportContracts.UpdateSupportRequest;
import com.example.backend.entity.Role;
import com.example.backend.entity.SupportItem;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.RoleName;
import com.example.backend.entity.enums.SupportKind;
import com.example.backend.entity.enums.SupportStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.SupportItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SupportServiceTest {
    private final SupportItemRepository repository = mock(SupportItemRepository.class);
    private final CurrentUserService currentUser = mock(CurrentUserService.class);
    private final SupportService service = new SupportService(repository, currentUser);

    @ParameterizedTest
    @EnumSource(value = RoleName.class, names = {"STUDENT", "SCHOOL"})
    void studentAndSchoolCanCreateTrimmedSupportRequests(RoleName role) {
        User sender = user(role, 12);
        when(currentUser.requireCurrentUser()).thenReturn(sender);
        when(repository.save(any())).thenAnswer(call -> {
            SupportItem item = call.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        var result = service.create(SupportKind.FEEDBACK,
                new CreateSupportRequest("  Useful feature  ", "  Please add this.  "));

        assertThat(result.kind()).isEqualTo(SupportKind.FEEDBACK);
        assertThat(result.subject()).isEqualTo("Useful feature");
        assertThat(result.content()).isEqualTo("Please add this.");
        assertThat(result.senderId()).isEqualTo(12);
        assertThat(result.status()).isEqualTo(SupportStatus.OPEN);
    }

    @ParameterizedTest
    @EnumSource(value = RoleName.class, names = {"ADMIN", "MANAGER", "REVIEWER", "STAFF"})
    void unrelatedRolesCannotCreateOrReadPersonalSupport(RoleName role) {
        when(currentUser.requireCurrentUser()).thenReturn(user(role, 1));

        assertStatus(HttpStatus.FORBIDDEN, () -> service.create(SupportKind.MESSAGE,
                new CreateSupportRequest("Question", "Content")));
        assertStatus(HttpStatus.FORBIDDEN, service::mine);
        verifyNoInteractions(repository);
    }

    @Test
    void ownMessagesRemainScopedToAuthenticatedSender() {
        User sender = user(RoleName.STUDENT, 12);
        when(currentUser.requireCurrentUser()).thenReturn(sender);
        SupportItem item = item(sender, SupportKind.MESSAGE);
        when(repository.findBySenderIdOrderByCreatedAtDesc(12)).thenReturn(List.of(item));

        var result = service.mine();

        assertThat(result).singleElement().satisfies(view -> {
            assertThat(view.senderId()).isEqualTo(12);
            assertThat(view.subject()).isEqualTo("Question");
        });
        verify(repository).findBySenderIdOrderByCreatedAtDesc(12);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void onlyManagerCanReadPlatformSupportInbox() {
        for (RoleName role : RoleName.values()) {
            if (role == RoleName.MANAGER) continue;
            when(currentUser.requireCurrentUser()).thenReturn(user(role, 1));
            assertStatus(HttpStatus.FORBIDDEN, () -> service.adminList(SupportKind.FEEDBACK));
        }
        verifyNoInteractions(repository);

        when(currentUser.requireCurrentUser()).thenReturn(user(RoleName.MANAGER, 2));
        when(repository.findTop200ByKindOrderByCreatedAtDesc(SupportKind.FEEDBACK)).thenReturn(List.of());
        assertThat(service.adminList(SupportKind.FEEDBACK)).isEmpty();
    }

    @Test
    void managerResponsePreservesOriginalMessageAndMarksItRead() {
        User manager = user(RoleName.MANAGER, 2);
        User sender = user(RoleName.SCHOOL, 12);
        SupportItem item = item(sender, SupportKind.FEEDBACK);
        when(currentUser.requireCurrentUser()).thenReturn(manager);
        when(repository.findById(item.getId())).thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);

        var result = service.update(item.getId(), new UpdateSupportRequest(null, "  Resolved  "));

        assertThat(result.status()).isEqualTo(SupportStatus.READ);
        assertThat(result.adminResponse()).isEqualTo("Resolved");
        assertThat(result.content()).isEqualTo("Original content");
        assertThat(result.respondedAt()).isNotNull();
        assertThat(item.getRespondedBy()).isSameAs(manager);
    }

    @Test
    void validationRejectsMissingAndOversizedInputBeforePersistence() {
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.create(null, new CreateSupportRequest("Subject", "Content")));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.create(SupportKind.FEEDBACK, new CreateSupportRequest(" ", "Content")));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.create(SupportKind.FEEDBACK, new CreateSupportRequest("x".repeat(181), "Content")));
        verify(repository, never()).save(any());
        verifyNoInteractions(currentUser);
    }

    private SupportItem item(User sender, SupportKind kind) {
        SupportItem item = new SupportItem();
        item.setId(UUID.randomUUID());
        item.setSender(sender);
        item.setKind(kind);
        item.setSubject("Question");
        item.setContent("Original content");
        item.setStatus(SupportStatus.OPEN);
        return item;
    }

    private User user(RoleName roleName, int id) {
        Role role = new Role();
        role.setName(roleName.name());
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName("Test user");
        user.setEmail("user" + id + "@example.test");
        user.setEnabled(true);
        user.setActive(true);
        return user;
    }

    private void assertStatus(HttpStatus status, org.junit.jupiter.api.function.Executable action) {
        assertThatThrownBy(action::execute)
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(status));
    }
}
