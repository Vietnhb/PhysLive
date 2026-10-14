package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.entity.LibraryItem;
import com.example.backend.entity.LibraryModerationAudit;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.LibraryModerationStatus;
import com.example.backend.entity.school.School;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.LibraryItemRepository;
import com.example.backend.repository.LibraryModerationAuditRepository;
import com.example.backend.service.account.AccountAccessService;

@ExtendWith(MockitoExtension.class)
class LibraryModerationServiceTest {
    @Mock LibraryItemRepository items;
    @Mock LibraryModerationAuditRepository audits;
    @Mock CurrentUserService currentUser;
    @Mock AccountAccessService access;
    @InjectMocks LibraryModerationService service;

    @Test
    void reviewerApprovesPublicItemAndWritesAudit() {
        User reviewer = user(1, null);
        LibraryItem item = item("PUBLIC", user(2, null));
        when(currentUser.requireCurrentUser()).thenReturn(reviewer);
        when(access.canReviewPublic(reviewer)).thenReturn(true);
        when(items.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(items.save(item)).thenReturn(item);

        var response = service.moderatePublic(item.getId(), LibraryModerationStatus.APPROVED, " Valid ");

        assertThat(response.getModerationStatus()).isEqualTo("APPROVED");
        assertThat(item.getModerationComment()).isEqualTo("Valid");
        assertThat(item.getModeratedBy()).isSameAs(reviewer);
        ArgumentCaptor<LibraryModerationAudit> audit = ArgumentCaptor.forClass(LibraryModerationAudit.class);
        verify(audits).save(audit.capture());
        assertThat(audit.getValue().getFromStatus()).isEqualTo(LibraryModerationStatus.PENDING);
        assertThat(audit.getValue().getToStatus()).isEqualTo(LibraryModerationStatus.APPROVED);
    }

    @Test
    void reviewerCannotApproveOwnContent() {
        User reviewer = user(1, null);
        LibraryItem item = item("PUBLIC", reviewer);
        when(currentUser.requireCurrentUser()).thenReturn(reviewer);
        when(access.canReviewPublic(reviewer)).thenReturn(true);
        when(items.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.moderatePublic(
                item.getId(), LibraryModerationStatus.APPROVED, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("own content");
        verifyNoInteractions(audits);
    }

    @Test
    void rejectionRequiresReason() {
        User reviewer = user(1, null);
        LibraryItem item = item("PUBLIC", user(2, null));
        when(currentUser.requireCurrentUser()).thenReturn(reviewer);
        when(access.canReviewPublic(reviewer)).thenReturn(true);
        when(items.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.moderatePublic(
                item.getId(), LibraryModerationStatus.REJECTED, " "))
                .isInstanceOf(ApiException.class).hasMessageContaining("reason");
        verifyNoInteractions(audits);
    }

    @Test
    void departmentHeadCanOnlyModerateOwnSchoolSharedItems() {
        UUID ownSchoolId = UUID.randomUUID();
        User head = user(1, ownSchoolId);
        LibraryItem foreign = item("SHARED", user(2, UUID.randomUUID()));
        when(currentUser.requireCurrentUser()).thenReturn(head);
        when(access.isDepartmentHead(head)).thenReturn(true);
        when(items.findByIdForUpdate(foreign.getId())).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.moderateSchool(ownSchoolId, foreign.getId(),
                LibraryModerationStatus.APPROVED, null)).isInstanceOf(ApiException.class);
        verifyNoInteractions(audits);
    }

    @Test
    void queuesDefaultToPendingStatus() {
        User reviewer = user(1, null);
        LibraryItem pending = item("PUBLIC", user(2, null));
        when(currentUser.requireCurrentUser()).thenReturn(reviewer);
        when(access.canReviewPublic(reviewer)).thenReturn(true);
        when(items.findPublicModerationItems(LibraryModerationStatus.PENDING)).thenReturn(List.of(pending));

        assertThat(service.publicQueue(null)).hasSize(1);
        verify(items).findPublicModerationItems(LibraryModerationStatus.PENDING);
    }

    private static LibraryItem item(String visibility, User owner) {
        LibraryItem item = new LibraryItem();
        item.setId(UUID.randomUUID()); item.setTitle("Simulation"); item.setVisibility(visibility);
        item.setOwner(owner); item.setOwnerId(owner.getId()); item.setActive(true);
        item.setModerationStatus(LibraryModerationStatus.PENDING);
        return item;
    }

    private static User user(int id, UUID schoolId) {
        User user = new User(); user.setId(id); user.setFullName("User " + id); user.setEmail("u" + id + "@test.local");
        if (schoolId != null) {
            School school = new School(); school.setId(schoolId); school.setName("School"); school.setActive(true);
            user.setSchool(school);
        }
        return user;
    }
}
